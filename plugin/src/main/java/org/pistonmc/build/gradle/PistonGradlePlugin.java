package org.pistonmc.build.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.impl.MinecraftExtensionImpl;
import org.pistonmc.build.gradle.extension.impl.ModdingToolchainSpecImpl;
import org.pistonmc.build.gradle.repo.GeneratedRepo;
import org.pistonmc.build.gradle.task.SetupVanillaDevTask;
import org.pistonmc.build.gradle.util.LowercaseEnumTypeAdapterFactory;
import org.pistonmc.build.gradle.util.OSName;
import org.pistonmc.build.gradle.util.ZonedDateTimeTypeAdapter;
import org.pistonmc.build.gradle.util.version.VersionJson;
import org.pistonmc.build.gradle.util.version.VersionManifest;

import java.net.http.HttpClient;
import java.time.ZonedDateTime;
import java.util.List;

public class PistonGradlePlugin implements Plugin<Project> {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(VersionManifest.class, VersionManifest.ADAPTER)
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
            .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
            .create();

    private VanillaMinecraftCache vmc;
    private GeneratedRepo generatedRepo;
    private MinecraftExtension extension;

    private TaskProvider<SetupVanillaDevTask> setupVanillaDevTask;
    private TaskProvider<Task> setupDevEnv;

    private NamedDomainObjectProvider<Configuration> mcVanillaConfiguration;

    private SourceSet mainSourceSet;
    private NamedDomainObjectProvider<SourceSet> forgeSourceSet;
    private NamedDomainObjectProvider<SourceSet> fabricSourceSet;

    public void apply(@NotNull Project project) {
        project.getBuildscript().getRepositories().maven(repo -> {
            repo.setName("Minecraft Decompiler");
            repo.setUrl("https://maven.pkg.github.com/MaxPixelStudios/MinecraftDecompiler");
            repo.credentials(passwordCredentials -> {
                passwordCredentials.setUsername(System.getenv(Constants.GHP_USERNAME));
                passwordCredentials.setPassword(System.getenv(Constants.GHP_TOKEN));
            });
            repo.content(descriptor -> descriptor.includeModule("cn.maxpixel", "minecraft-decompiler"));
        });
        this.vmc = new VanillaMinecraftCache(project);
        this.generatedRepo = new GeneratedRepo(project.getLayout().getBuildDirectory().dir(Constants.GENERATED_REPO_DIR));
        var manifest = project.provider(vmc::getVersionManifest);

        project.getPluginManager().apply(JavaPlugin.class);

        this.extension = project.getExtensions().create(MinecraftExtension.class, Constants.MINECRAFT_EXTENSION, MinecraftExtensionImpl.class, vmc);
        extension.getVersion().convention(manifest.map(VersionManifest::latestRelease));

        project.getRepositories().mavenCentral(repo -> repo.content(d -> {
            d.excludeGroupAndSubgroups("net.minecraft");
            d.excludeGroupAndSubgroups("net.minecraftforge");
            d.excludeGroupAndSubgroups("net.fabricmc");
            d.excludeGroupAndSubgroups("com.mojang");
        }));
        project.getRepositories().maven(repo -> {
            repo.setName("Minecraft Libraries");
            repo.setUrl("https://libraries.minecraft.net/");
            repo.getMetadataSources().artifact();
        });
        generatedRepo.addToProject(project);

        var configurations = project.getConfigurations();
        this.mcVanillaConfiguration = configurations.register(Constants.MINECRAFT_VANILLA_CONFIGURATION, config -> {
            config.setDescription("Marks all dependencies from vanilla MC");
            config.setVisible(false);
            config.setTransitive(false);
            config.setCanBeConsumed(false);
            config.setCanBeResolved(false);
        });

        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        this.mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var mainOutput = mainSourceSet.getOutput();
        this.forgeSourceSet = sourceSets.register(Constants.FORGE_SOURCE_SET, sourceSet -> {
            sourceSet.getJava().srcDirs("src/forge/java");
            sourceSet.getResources().srcDirs("src/forge/resources");
            sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(mainOutput));
            sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(mainOutput));
        });
        this.fabricSourceSet = sourceSets.register(Constants.FABRIC_SOURCE_SET, sourceSet -> {
            sourceSet.getJava().srcDirs("src/fabric/java");
            sourceSet.getResources().srcDirs("src/fabric/resources");
            sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(mainOutput));
            sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(mainOutput));
        });

        var tasks = project.getTasks();
        this.setupVanillaDevTask = tasks.register(Constants.SETUP_VANILLA_DEV_ENV_TASK, SetupVanillaDevTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.getInputJar().set(project.getLayout().file(extension.getVersion().map(vmc::getClientJarFile)));
            task.getMappingConfig().set(extension.getMapping());
            task.getOutputJar().set(extension.getVersion().flatMap(v -> generatedRepo.getPath("net.minecraft", "vanilla", v)));
        });
        this.setupDevEnv = tasks.register(Constants.SETUP_DEV_ENV_TASK, task -> {
            task.setGroup(Constants.TASK_GROUP);
        });

        project.afterEvaluate(this::postApply);
    }

    public void postApply(@NotNull Project project) {
        var toolchains = (ModdingToolchainSpecImpl) extension.getToolchains();
        var vanilla = toolchains.getVanillaConfig();
        var forge = toolchains.getForgeConfig();
        var fabric = toolchains.getFabricConfig();
        var vanillaPresent = vanilla.isPresent();
        var forgePresent = forge.isPresent();
        var fabricPresent = fabric.isPresent();
        var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        if (vanillaPresent || forgePresent || fabricPresent) {
            var startParameter = project.getGradle().getStartParameter();
            if (startParameter.getTaskRequests().isEmpty()) {
                startParameter.setTaskNames(List.of(setupDevEnv.getName()));
            }
            var dependencies = project.getDependencies();
            var configName = mcVanillaConfiguration.getName();
            var osName = OSName.getCurrent();
            var version = extension.getVersion().get();
            vmc.getVersionJson(version).libraries().stream().filter(library -> library.rules() == null ||
                    library.rules().stream().allMatch(VersionJson.Rule::isAllow)).forEach(library -> {
                        dependencies.add(configName, library.name());
                        var natives = library.natives();
                        if (natives != null) {
                            var classifier = natives.get(osName);
                            if (classifier != null) {
                                dependencies.add(configName, library.name() + ':' + classifier);
                            }
                        }
                    });
        }
        if (vanillaPresent) {
            setupDevEnv.configure(t -> t.dependsOn(setupVanillaDevTask));
            project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME, task -> task.dependsOn(setupVanillaDevTask));
            project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "net.minecraft:vanilla:" + extension.getVersion().get());
            project.getConfigurations().named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, c -> c.extendsFrom(mcVanillaConfiguration.get()));
        }
        if (forgePresent) {
            project.getRepositories().maven(repo -> {
                repo.setName("Forge Maven");
                repo.setUrl("https://maven.minecraftforge.net/");
                repo.metadataSources(sources -> {
                    sources.gradleMetadata();
                    sources.mavenPom();
                    sources.artifact();
                });
            });
        }
        if (fabricPresent) {
            project.getRepositories().maven(repo -> {
                repo.setName("Fabric Maven");
                repo.setUrl("https://maven.fabricmc.net/");
            });
        }
    }
}
