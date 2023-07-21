package org.pistonmc.build.gradle.util;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Utils {
    public static Charset forName(@Nullable String charsetName) {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    public static JavaLanguageVersion of(@Nullable Integer version, JavaLanguageVersion defaultVersion) {
        return version == null ? defaultVersion : JavaLanguageVersion.of(version);
    }

    public static boolean hashEquals(Object a, Object b) {
        return a.hashCode() == b.hashCode();
    }

    public static String joinAbsoluteFiles(FileCollection files) {
        return files.getFiles().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    }

    public static String mapToAbsolutePath(FileSystemLocation l) {
        return l.getAsFile().getAbsolutePath();
    }
}