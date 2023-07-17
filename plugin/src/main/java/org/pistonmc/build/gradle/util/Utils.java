package org.pistonmc.build.gradle.util;

import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static Charset forName(@Nullable String charsetName) {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    public static JavaLanguageVersion of(@Nullable Integer version, JavaLanguageVersion defaultVersion) {
        return version == null ? defaultVersion : JavaLanguageVersion.of(version);
    }
}