package org.pistonmc.build.gradle.forge;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.pistonmc.build.gradle.Constants;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.forge.config.UserDevConfig;
import org.pistonmc.build.gradle.forge.task.SetupMCP;
import org.pistonmc.build.gradle.util.Utils;

import java.nio.file.Path;
import java.util.Map;

public class ForgeSetup {
    private static final Map.Entry<String, String> GROUP_ENTRY = Map.entry("group", "net.minecraftforge");
    private static final Map.Entry<String, String> ARTIFACT_ENTRY = Map.entry("name", "forge");
    private static final Map.Entry<String, String> USERDEV_CLASSIFIER_ENTRY = Map.entry("classifier", "userdev");

    private final Provider<Path> extractBaseDir;
    private final Provider<Directory> setupBaseDir;

    private final TaskProvider<SetupMCP> setupMcp;

    private final NamedDomainObjectProvider<Configuration> forgeSetup;
    private final Provider<Dependency> userDevJar;
    private final Dependency atJar;
    private final Dependency sasJar;

    public ForgeSetup(Project project, MinecraftExtension extension, VanillaMinecraftCache vmc, NamedDomainObjectProvider<Configuration> mcVanillaConfig) {
        this.extractBaseDir = project.getLayout().getBuildDirectory().dir(ForgeConstants.EXTRACT_BASE_DIR).map(dir -> dir.getAsFile().toPath().toAbsolutePath().normalize());
        this.setupBaseDir = project.getLayout().getBuildDirectory().dir(ForgeConstants.SETUP_BASE_DIR);
        var forgeConfig = extension.getToolchains().getForgeConfig();

        var configurations = project.getConfigurations();
        this.forgeSetup = configurations.register(Constants.FORGE_SETUP, config -> {
            config.setDescription("Used when setting up Forge workspace");
            config.setVisible(false);
            config.setCanBeConsumed(false);
            config.setTransitive(false);
        });

        var dependencies = project.getDependencies();
        this.userDevJar = forgeConfig.getArtifactVersion().map(ForgeSetup::userDevDependency).map(dependencies::create);
        this.atJar = dependencies.create(ForgeConstants.AT_DEP);
        this.sasJar = dependencies.create(ForgeConstants.SAS_DEP);
        forgeSetup.configure(c -> {
            c.getDependencies().addLater(userDevJar);
            c.getDependencies().add(atJar);
            c.getDependencies().add(sasJar);
        });

        var tasks = project.getTasks();
        this.setupMcp = tasks.register("setupMCP", SetupMCP.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getCache().set(vmc);
            task.getForgeSetup().set(forgeSetup);
            task.getBaseDirectory().set(setupBaseDir.map(dir -> dir.dir("mcp")));
            task.getMcVanilla().set(mcVanillaConfig);
            task.getAccessTransformerJar().from(forgeSetup.map(c -> c.fileCollection(atJar)));
            task.getSideAnnotationStripperJar().from(forgeSetup.map(c -> c.fileCollection(sasJar)));
        });
    }

    public void postSetup(Project project, MinecraftExtension extension, TaskProvider<Task> setupDevEnv) {
        var dependencies = project.getDependencies();
        var forgeSetup = this.forgeSetup.get();
        var userDevJar = this.userDevJar.get();
        var userDevConfig = UserDevConfig.load(forgeSetup.copy().fileCollection(dep -> Utils.hashEquals(dep, userDevJar)),
                extractBaseDir.get(), forgeSetup, dependencies);
        setupMcp.configure(task -> {
            task.getConfig().set(userDevConfig);
            task.getAccessTransformers().set(extension.getAllAccessTransformers());
        });
        setupDevEnv.configure(t -> t.dependsOn(setupMcp));
    }

    private static Map<String, String> userDevDependency(String artifactVersion) {
        return Map.ofEntries(GROUP_ENTRY, ARTIFACT_ENTRY, Map.entry("version", artifactVersion), USERDEV_CLASSIFIER_ENTRY);
    }
}