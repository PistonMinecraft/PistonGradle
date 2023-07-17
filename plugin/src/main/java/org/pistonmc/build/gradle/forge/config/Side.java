package org.pistonmc.build.gradle.forge.config;

import java.util.Locale;

public enum Side {
    CLIENT, JOINED, SERVER;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static Side of(String s) {
        return valueOf(s.toUpperCase(Locale.ENGLISH));
    }
}
