package org.pistonmc.build.gradle.util;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.FileCollection;

import java.util.Objects;

public class DependencyUtil {
    public static boolean groupAndNameEquals(Dependency a, Dependency b) {
        return Objects.equals(a.getGroup(), b.getGroup()) && a.getName().equals(b.getName());
    }

    public static boolean groupAndNameEquals(ComponentIdentifier a, Dependency b) {
        return (a instanceof ModuleComponentIdentifier m) &&
                Objects.equals(m.getGroup(), b.getGroup()) &&
                m.getModule().equals(b.getName());
    }

    public static boolean equals(ComponentIdentifier a, Dependency b) {
        return groupAndNameEquals(a, b) && Objects.equals(((ModuleComponentIdentifier) a).getVersion(), b.getVersion());
    }

    public static Action<ArtifactView.ViewConfiguration> filter(Dependency dep) {
        return view -> view.componentFilter(id -> equals(id, dep));
    }

    public static Action<ArtifactView.ViewConfiguration> filterWithoutVersion(Dependency dep) {
        return view -> view.componentFilter(id -> groupAndNameEquals(id, dep));
    }

    public static FileCollection fileCollection(Configuration c, Action<ArtifactView.ViewConfiguration> viewConfig) {
        return c.getIncoming().artifactView(viewConfig).getFiles();
    }

    public static FileCollection fileCollection(Configuration c, Dependency dep) {
        return fileCollection(c, filter(dep));
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