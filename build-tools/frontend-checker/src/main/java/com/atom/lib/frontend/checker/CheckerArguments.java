package com.atom.lib.frontend.checker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Pass complete Gradle catalogs without expanding hundreds of paths on the process command line. */
final class CheckerArguments {
    private static final String FILE_OPTION = "--arguments-file";

    private CheckerArguments() {}

    static List<String> write(Path file, List<String> arguments) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, String.join("\0", arguments));
        return List.of(FILE_OPTION, file.toString());
    }

    static String[] expand(String[] invocation) throws IOException {
        if (invocation.length == 2 && FILE_OPTION.equals(invocation[0])) {
            return Files.readString(Path.of(invocation[1])).split("\0", -1);
        }
        return invocation;
    }
}
