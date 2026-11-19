// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import com.atom.lib.NativePreparation.Source;
import com.atom.lib.NativePreparation.State;
import com.atom.lib.NativePreparation.Status;
import io.github.humbleui.skija.impl.Library;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Loader and runtime integration for the process-wide client native paths. */
public final class ClientNatives {
    private static final System.Logger LOGGER = System.getLogger("AtomLib");
    private static Client client;
    private static volatile Status status = new Status(State.NOT_STARTED, "", "", null);

    private ClientNatives() {}

    private record Client(Path cache, BooleanSupplier hostYoga, Consumer<Path> configureYoga, Runnable loadYoga) {}

    public static synchronized void start(Path gameDirectory, BooleanSupplier hostYoga, Consumer<Path> configureYoga,
                                          Runnable loadYoga) {
        if (client != null) return;
        client = new Client(gameDirectory.resolve("atomlib").resolve("natives"), hostYoga, configureYoga, loadYoga);
        try {
            NativePlatform platform = NativePlatform.detect();
            if (platform.os().equals("windows")) {
                status = new Status(State.AVAILABLE, platform.id(), "", null);
                return;
            }
            schedule(platform, null, null);
        } catch (IOException | RuntimeException failure) {
            failed("", failure, true);
        }
    }

    public static Status status() { return status; }

    public static synchronized void reportLoadFailure(LinkageError failure) {
        if (status.state() == State.AVAILABLE) failed(status.platform(), failure, false);
    }

    public static void requireAvailable() {
        Status current = status;
        // Standalone hosts choose their own classpath and explicitly open the runtime.
        if (current.state() == State.NOT_STARTED || current.state() == State.AVAILABLE) return;
        throw new IllegalStateException(current.state() == State.PREPARING
                ? "AtomLib native preparation is still running"
                : "AtomLib native preparation failed: " + current.detail(), current.failure());
    }

    public static synchronized boolean retry(Source source) {
        return restart(Objects.requireNonNull(source), null);
    }

    public static synchronized boolean retryConfiguredSource() {
        return restart(null, null);
    }

    public static synchronized boolean importBundle(Path zip) {
        return restart(Source.OFFLINE, Objects.requireNonNull(zip));
    }

    private static boolean restart(Source source, Path zip) {
        if (client == null || !status.retryable()) return false;
        try {
            schedule(NativePlatform.detect(), source, zip);
            return true;
        } catch (IOException | RuntimeException failure) {
            failed("", failure, true);
            return false;
        }
    }

    private static void schedule(NativePlatform platform, Source source, Path zip) {
        Client configuration = client;
        status = new Status(State.PREPARING, platform.id(), "", null);
        Thread worker = new Thread(() -> prepare(configuration, platform, source, zip), "AtomLib native preparation");
        worker.setDaemon(true);
        worker.start();
    }

    private static void prepare(Client configuration, NativePlatform platform, Source selection, Path zip) {
        boolean initializing = false;
        try {
            Source source = selection == null ? Source.valueOf(
                    System.getProperty("atomlib.natives.source", "auto").strip().toUpperCase(Locale.ROOT)) : selection;
            boolean skijaProvided = hasHostSkija();
            boolean yogaProvided = configuration.hostYoga().getAsBoolean();
            if (!skijaProvided || !yogaProvided || zip != null) {
                var plan = NativeManifest.load(platform, skijaProvided, yogaProvided);
                var cache = new NativeCache(configuration.cache(), platform.id());
                var libraries = cache.prepare(plan.artifacts(), plan.bundle(), zip, source, Duration.ofSeconds(60),
                        detail -> status = new Status(State.PREPARING, platform.id(), detail, null));
                for (var library : libraries.entrySet()) {
                    Path directory = library.getValue().getParent();
                    if (library.getKey().isSkija()) {
                        if (!hasHostSkija()) System.setProperty("skija.library.path", directory.toString());
                    } else {
                        configuration.configureYoga().accept(directory);
                    }
                }
            }
            status = new Status(State.PREPARING, platform.id(), "Loading Skija and Yoga", null);
            initializing = true;
            Library.load();
            configuration.loadYoga().run();
            status = new Status(State.AVAILABLE, platform.id(), "", null);
        } catch (IOException | RuntimeException | LinkageError failure) {
            failed(platform.id(), failure, !initializing);
        }
    }

    private static boolean hasHostSkija() {
        return Library._loaded || System.getProperty("skija.library.path") != null
                || "true".equals(System.getProperty("skija.loadFromLibraryPath"));
    }

    private static void failed(String platform, Throwable failure, boolean retryable) {
        status = new Status(State.FAILED, platform, failure.getMessage(), failure, retryable);
        LOGGER.log(System.Logger.Level.WARNING, "AtomLib native preparation failed for " + platform, failure);
    }
}
