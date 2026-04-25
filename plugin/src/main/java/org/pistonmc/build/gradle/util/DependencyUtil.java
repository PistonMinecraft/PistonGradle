package org.pistonmc.build.gradle.util;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.FileCollection;

import java.util.Objects;

public class DependencyUtil {
    // Don't check versions. Reason:
    // 1. There's no need because there is only one version in the end
    // 2. ModuleComponentIdentifier contains resolved versions, while Dependency may contain
    //    dynamic versions(e.g. a.getVersion() is "8.0.7" while b.getVersion() is "8.0.+").
    //    Therefore, the version check could fail, leading to an empty filtered collection
    public static boolean equals(ComponentIdentifier a, Dependency b) {
        return (a instanceof ModuleComponentIdentifier m) &&
                Objects.equals(m.getGroup(), b.getGroup()) &&
                m.getModule().equals(b.getName());
    }

    public static Action<ArtifactView.ViewConfiguration> filter(Dependency dep) {
        return view -> view.componentFilter(id -> equals(id, dep));
    }

    public static FileCollection fileCollection(Configuration c, Dependency dep) {
        return c.getIncoming().artifactView(filter(dep)).getFiles();
    }
}