package org.pistonmc.build.gradle.repo;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.util.ArtifactUtil;

public class GeneratedRepo {
    private final Provider<Directory> baseDir;

    public GeneratedRepo(Provider<Directory> baseDir) {
        this.baseDir = baseDir;
    }

    public void addToProject(Project project) {
        project.getRepositories().maven(repo -> {
            repo.setName("Generated Artifacts");
            repo.setUrl(baseDir);
            repo.getMetadataSources().artifact();
        });
    }

    public Provider<RegularFile> getPath(String group, String artifactId, String version) {
        return baseDir.map(dir -> dir.file(ArtifactUtil.getPath(group, artifactId, version)));
    }
}