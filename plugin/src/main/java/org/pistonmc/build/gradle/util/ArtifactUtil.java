package org.pistonmc.build.gradle.util;

import java.util.StringJoiner;

public class ArtifactUtil {
    public static String getPath(String group, String artifactId, String version) {
        return getPath(group, artifactId, version, null);
    }

    public static String getPath(String group, String artifactId, String version, String classifier) {
        return getPath(group, artifactId, version, classifier, ".jar");
    }

    public static String getPath(String group, String artifactId, String version, String classifier, String extension) {
        String replacedGroup = group.replace('.', '/');
        StringJoiner fileName = new StringJoiner("-", "", extension == null ? ".jar" : extension);
        fileName.add(artifactId);
        fileName.add(version);
        if (classifier != null) fileName.add(classifier);
        return String.join("/", replacedGroup, artifactId, version, fileName.toString());
    }
}