package org.pistonmc.build.gradle.util;

import org.gradle.api.JavaVersion;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static Charset forName(@Nullable String charsetName) {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    public static JavaVersion of(Integer version) {
        return version == null ? JavaVersion.current() : JavaVersion.toVersion(version);
    }
}