package org.pistonmc.build.gradle.util;

import org.gradle.api.tasks.Input;

import java.util.Map;

public record AssetIndex(Map<String, AssetEntry> objects) {
    public record AssetEntry(String hash, int size) {
        @Input
        public String getHash() {
            return hash;
        }

        @Input
        public int getSize() {
            return size;
        }
    }
}