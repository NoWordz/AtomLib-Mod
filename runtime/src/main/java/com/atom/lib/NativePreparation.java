// SPDX-License-Identifier: Apache-2.0
package com.atom.lib;

import com.atom.lib.natives.ClientNatives;

import java.nio.file.Path;

/** Status and recovery operations for AtomLib's client native preparation. */
public final class NativePreparation {
    private NativePreparation() {}

    public enum State { NOT_STARTED, PREPARING, AVAILABLE, FAILED }
    public enum Source { AUTO, MIRROR, CENTRAL, OFFLINE }

    /**
     * A snapshot that can be read from the render thread without waiting.
     * Failure and transfer details are intended for diagnostics.
     */
    public record Status(State state, String platform, String detail, Throwable failure, boolean retryable) {
        public Status(State state, String platform, String detail, Throwable failure) {
            this(state, platform, detail, failure, state == State.FAILED);
        }
    }

    public static Status status() { return ClientNatives.status(); }

    /** Retries with the configured source, including an explicit offline choice. */
    public static boolean retry() { return ClientNatives.retryConfiguredSource(); }

    /** Retries a failed client preparation with an explicitly selected source. */
    public static boolean retry(Source source) { return ClientNatives.retry(source); }

    /** Starts offline import when preparation has failed. The path is a local ZIP. */
    public static boolean importBundle(Path zip) { return ClientNatives.importBundle(zip); }
}
