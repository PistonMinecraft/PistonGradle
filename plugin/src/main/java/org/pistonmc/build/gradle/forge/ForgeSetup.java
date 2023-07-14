package org.pistonmc.build.gradle.forge;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.Constants;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.forge.config.UserDevConfig;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Map;

public abstract class ForgeSetup {
    private static final Map.Entry<String, String> GROUP_ENTRY = Map.entry("group", "net.minecraftforge");
    private static final Map.Entry<String, String> ARTIFACT_ENTRY = Map.entry("group", "forge");
    private static final Map.Entry<String, String> USERDEV_CLASSIFIER_ENTRY = Map.entry("classifier", "userdev");

    private final Provider<Path> extractBaseDir;
    private final NamedDomainObjectProvider<Configuration> forgeSetup;
    private final Dependency userDevJar;

    @Inject
    public ForgeSetup(Project project, MinecraftExtension extension) {
        this.extractBaseDir = project.getLayout().getBuildDirectory().dir(ForgeConstants.EXTRACT_BASE_DIR).map(dir -> dir.getAsFile().toPath());
        var forgeConfig = extension.getToolchains().getForgeConfig();

        var configurations = project.getConfigurations();
        this.forgeSetup = configurations.register(Constants.FORGE_SETUP, config -> {
            config.setDescription("Used when setting up Forge workspace");
            config.setVisible(false);
            config.setCanBeConsumed(false);
        });

        var dependencies = project.getDependencies();
        this.userDevJar = dependencies.create(forgeConfig.getArtifactVersion().map(ForgeSetup::userDevDependency));
        forgeSetup.configure(c -> c.getDependencies().add(userDevJar));
    }

    public void postSetup(Project project, MinecraftExtension extension) {
        var dependencies = project.getDependencies();
        var forgeSetup = this.forgeSetup.get();
        var userDevConfig = UserDevConfig.load(forgeSetup.copy().fileCollection(dep -> userDevJar == dep),
                extractBaseDir.get(), forgeSetup, dependencies);
    }

    private static Map<String, String> userDevDependency(String artifactVersion) {
        return Map.ofEntries(GROUP_ENTRY, ARTIFACT_ENTRY, Map.entry("version", artifactVersion), USERDEV_CLASSIFIER_ENTRY);
    }
}