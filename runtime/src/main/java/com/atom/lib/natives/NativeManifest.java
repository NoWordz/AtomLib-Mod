// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class NativeManifest {
    record Blob(long size, String sha256) {
        Blob {
            if (size <= 0 || !sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid native digest");
        }
    }

    record Entry(String jarPath, String packPath, Blob blob) {
        Entry {
            if (!safeName(jarPath) || !safeName(packPath)) throw new IllegalArgumentException("Invalid native entry");
        }
        String library() { return packPath.substring(packPath.lastIndexOf('/') + 1); }
        boolean isSkija() { return packPath.startsWith("skija/"); }
        Path cachePath(Path root) {
            return root.resolve(packPath.substring(0, packPath.lastIndexOf('/')))
                    .resolve(blob.sha256()).resolve(library());
        }
    }

    record Archive(Blob blob, List<URI> sources, List<Entry> entries) {
        Archive {
            sources = List.copyOf(sources);
            entries = List.copyOf(entries);
        }
    }

    record Plan(List<Archive> artifacts, Archive bundle) {}

    private final Properties properties = new Properties();

    static Plan load(NativePlatform platform, boolean skijaProvided, boolean yogaProvided) throws IOException {
        var manifest = new NativeManifest();
        try (var input = NativeManifest.class.getResourceAsStream("/META-INF/atomlib/native-artifacts.properties")) {
            if (input == null) throw new IOException("AtomLib native manifest is missing");
            manifest.properties.load(input);
        }
        return manifest.plan(platform, skijaProvided, yogaProvided);
    }

    private Plan plan(NativePlatform platform, boolean skijaProvided, boolean yogaProvided) throws IOException {
        String prefix = platform.id();
        String skija = prefix + ".skija";
        String yoga = prefix + ".yoga." + required("yogaVersion");
        if (!properties.containsKey(skija + ".path") || !properties.containsKey(yoga + ".path")) {
            throw new IOException("No AtomLib native distribution for " + platform.id() + " (Yoga "
                    + required("yogaVersion") + ")");
        }
        var artifacts = new ArrayList<Archive>();
        if (!skijaProvided) artifacts.add(artifact(skija));
        if (!yogaProvided) artifacts.add(artifact(yoga));
        List<Entry> bundled = List.of(required(prefix + ".bundle.entries").split(",")).stream()
                .map(this::entry).toList();
        String bundle = prefix + ".bundle";
        return new Plan(List.copyOf(artifacts), new Archive(blob(bundle), sources(bundle), bundled));
    }

    private Archive artifact(String key) {
        // Android entries are distributed together in the versioned bundle.
        return new Archive(properties.containsKey(key + ".size") ? blob(key) : null, sources(key), List.of(entry(key)));
    }

    private Entry entry(String key) {
        return new Entry(required(key + ".path"), required(key + ".packPath"), blob(key + ".file"));
    }

    private Blob blob(String key) {
        return new Blob(Long.parseLong(required(key + ".size")), required(key + ".sha256"));
    }

    private List<URI> sources(String key) {
        String value = properties.getProperty(key + ".urls", "");
        if (value.isEmpty()) return List.of();
        return List.of(value.split(",")).stream().map(URI::create).peek(uri -> {
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null) throw new IllegalArgumentException("Invalid native source: " + uri);
        }).toList();
    }

    private String required(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing native manifest key: " + key);
        return value;
    }

    static boolean safeName(String name) {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) return false;
        for (String part : name.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }
}
