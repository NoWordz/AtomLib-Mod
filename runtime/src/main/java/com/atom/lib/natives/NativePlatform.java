// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

record NativePlatform(String os, String arch) {
    static NativePlatform detect() throws IOException {
        return detect(System.getProperty("os.name", ""), System.getProperty("os.arch", ""),
                System.getProperty("java.runtime.name", ""), System.getProperty("atomlib.natives.platform", ""),
                System.getenv("POJAV_NATIVEDIR") != null || System.getenv("ANDROID_ROOT") != null
                        && Files.isRegularFile(Path.of("/system/bin/app_process64")));
    }

    static NativePlatform detect(String name, String architecture, String runtime, String override,
                                 boolean androidLauncher) throws IOException {
        String host = name.toLowerCase(Locale.ROOT);
        String os;
        if (override.equalsIgnoreCase("android") || host.contains("android")
                || runtime.toLowerCase(Locale.ROOT).contains("android") || androidLauncher) os = "android";
        else if (!override.isBlank()) throw new IOException("Unknown atomlib.natives.platform: " + override);
        else if (host.startsWith("windows")) os = "windows";
        else if (host.startsWith("mac") || host.startsWith("darwin")) os = "macos";
        else if (host.startsWith("linux")) os = "linux";
        else throw new IOException("Unsupported AtomLib native operating system: " + name);
        String arch = switch (architecture.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64", "x64" -> "x64";
            case "aarch64", "arm64" -> "arm64";
            default -> throw new IOException("Unsupported AtomLib JVM architecture: " + architecture);
        };
        return new NativePlatform(os, arch);
    }

    String id() { return os + "-" + arch; }
}
