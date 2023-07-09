package org.pistonmc.build.gradle.util.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pistonmc.build.gradle.util.OSName;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public record VersionJson(Map<String, List<Argument>> arguments, AssetIndex assetIndex, String assets, int complianceLevel, Map<String, GameDownload> downloads,
                          String id, JavaVersion javaVersion, List<Library> libraries, Map<String, LoggingConfig> logging, String mainClass,
                          String minecraftArguments, String minimumLauncherVersion, ZonedDateTime releaseTime, ZonedDateTime time, Type type) {
    public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {}

    public record JavaVersion(String component, int majorVersion) {}

    public record GameDownload(String sha1, int size, String url) {}

    public record LoggingConfig(String argument, File file, String type) {
        public record File(String id, String sha1, int size, String url) {}
    }

    public record Library(@NotNull Downloads downloads, @NotNull String name, @Nullable Map<OSName, String> natives, @Nullable List<Rule> rules, @Nullable Extract extract) {
        public record Downloads(@Nullable Artifact artifact, @Nullable Map<String, Artifact> classifiers) {
            public record Artifact(@NotNull String path, @NotNull String sha1, int size, @NotNull String url) {}
        }

        public record Extract(@NotNull List<String> exclude) {}
    }
}