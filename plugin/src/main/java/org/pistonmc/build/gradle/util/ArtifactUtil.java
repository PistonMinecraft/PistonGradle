package org.pistonmc.build.gradle.util;

public class ArtifactUtil {
    public static String getPath(String group, String artifactId, String version) {
        String replacedGroup = group.replace('.', '/');
        String replacedArtifact = artifactId.replace('.', '/');
        return String.join("/", replacedGroup, replacedArtifact, version, artifactId + '-' + version + ".jar");
    }
}