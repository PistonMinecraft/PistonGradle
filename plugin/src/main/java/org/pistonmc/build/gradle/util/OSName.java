package org.pistonmc.build.gradle.util;

import java.util.Locale;

public enum OSName {
    WINDOWS, LINUX, OSX;
    private static OSName current;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static OSName getCurrent() {
        if (current != null) return current;
        String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (name.contains("windows")) current = OSName.WINDOWS;
        else if (name.contains("mac") || name.contains("darwin")) current = OSName.OSX;
        else if (name.contains("linux")) current = OSName.LINUX;
        else throw new IllegalArgumentException("Unknown OS \"" + System.getProperty("os.name") +
                    "\". It seems that Minecraft doesn't support this OS. If you believe this is an error, " +
                    "please report to PistonGradle GitHub Issues");
        return current;
    }
}