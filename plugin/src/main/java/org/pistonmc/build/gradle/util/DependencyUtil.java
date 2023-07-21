package org.pistonmc.build.gradle.util;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

import java.util.Objects;

public class DependencyUtil {
    public static boolean groupAndNameEquals(Dependency a, Dependency b) {
        return Objects.equals(a.getGroup(), b.getGroup()) && a.getName().equals(b.getName());
    }

    public static boolean equals(Dependency a, Dependency b) {
        if (groupAndNameEquals(a, b) && Objects.equals(a.getVersion(), b.getVersion())) {
            if (a instanceof ModuleDependency am) {
                if (b instanceof ModuleDependency bm) {
                    return am.getArtifacts().equals(bm.getArtifacts());
                }
            } else if (b instanceof ModuleDependency) {
            } else return true;
        }
        return false;
    }
}