package org.pistonmc.build.gradle.util;

import org.gradle.api.artifacts.Dependency;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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

    public static boolean artifactEquals(Dependency a, Dependency b) {
        return Objects.equals(a.getGroup(), b.getGroup()) && a.getName().equals(b.getName());
    }
}