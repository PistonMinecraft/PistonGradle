package org.pistonmc.build.gradle.util;

import java.util.Map;

public class AssetIndex {
    private Map<String, AssetEntry> objects;

    public Map<String, AssetEntry> getObjects() {
        return objects;
    }

    public record AssetEntry(String hash, int size) {}
}