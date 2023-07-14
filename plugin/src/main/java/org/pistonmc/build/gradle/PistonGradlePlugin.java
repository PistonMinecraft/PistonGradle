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
import org.pistonmc.build.gradle.repo.GeneratedRepo;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.run.DataRunConfig;
import org.pistonmc.build.gradle.run.ServerRunConfig;
import org.pistonmc.build.gradle.run.impl.ClientRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.DataRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.ServerRunConfigImpl;
import org.pistonmc.build.gradle.task.DownloadAssetsTask;
import org.pistonmc.build.gradle.task.ExtractNativesTask;
import org.pistonmc.build.gradle.task.SetupVanillaDevTask;
import org.pistonmc.build.gradle.util.LowercaseEnumTypeAdapterFactory;
import org.pistonmc.build.gradle.util.OSName;
import org.pistonmc.build.gradle.util.ZonedDateTimeTypeAdapter;
import org.pistonmc.build.gradle.util.version.Argument;
import org.pistonmc.build.gradle.util.version.Rule;
import org.pistonmc.build.gradle.util.version.VersionJson;
import org.pistonmc.build.gradle.util.version.VersionManifest;

import java.net.http.HttpClient;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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
    private TaskProvider<DownloadAssetsTask> prepareAssetsTask;
    private TaskProvider<ExtractNativesTask> extractNativesTask;

    private NamedDomainObjectProvider<Configuration> mcVanillaConfiguration;

    private SourceSet mainSourceSet;
    private NamedDomainObjectProvider<SourceSet> forgeSourceSet;
    private NamedDomainObjectProvider<SourceSet> fabricSourceSet;

    public void apply(@NotNull Project project) {
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
            task.getOutputJar().set(extension.getVersion().flatMap(v -> generatedRepo.getPath("net.minecraft", "vanilla", v, "client")));
        });
        this.prepareAssetsTask = tasks.register(Constants.PREPARE_ASSETS_TASK, DownloadAssetsTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.getAssets().set(extension.getVersion().map(v -> vmc.getAssetIndex(v).objects()));
            task.getCache().set(vmc);
        });
        this.extractNativesTask = tasks.register(Constants.EXTRACT_NATIVES_TASK, ExtractNativesTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.getExtract().set(project.getLayout().getBuildDirectory().dir("natives"));
        });
        this.setupDevEnv = tasks.register(Constants.SETUP_DEV_ENV_TASK, task -> {
            task.setGroup(Constants.TASK_GROUP);
        });

        extension.getRuns().configureEach(config -> {
            project.getLogger().debug("The name of the running task for config {} is {}", config.getName(), config.getRunTaskName());
            config.getRunTask().configure(task -> {
                task.classpath(mcVanillaConfiguration);
                var clientConfig = config instanceof ClientRunConfig c ? c : null;
                if (clientConfig != null) {
                    task.dependsOn(prepareAssetsTask, extractNativesTask);
                }
            });
        });

        project.afterEvaluate(this::postApply);
    }

    public void postApply(@NotNull Project project) {
        var version = extension.getVersion().get();
        var toolchains = extension.getToolchains();
        var vanilla = toolchains.getVanillaConfig();
        var forge = toolchains.getForgeConfig();
        var fabric = toolchains.getFabricConfig();
        var vanillaPresent = vanilla.getEnabled().get();
        var forgePresent = forge.getEnabled().get();
        var fabricPresent = fabric.getEnabled().get();
        var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        var vanillaDependencyNotation = Map.of(
                "group", "net.minecraft",
                "name", "vanilla",
                "version", version,
                "classifier", "client"
        );
        var tasks = project.getTasks();
        var dependencies = project.getDependencies();
        var configurations = project.getConfigurations();
        if (vanillaPresent || forgePresent || fabricPresent) {
            var startParameter = project.getGradle().getStartParameter();
            if (startParameter.getTaskNames().isEmpty()) {
                startParameter.setTaskNames(List.of(setupDevEnv.getName()));
            }
            var configName = mcVanillaConfiguration.getName();
            var osName = OSName.getCurrent();
            vmc.getVersionJson(version).libraries().stream()
                    .filter(library -> library.rules() == null || library.rules().stream().allMatch(Rule::isAllow))
                    .forEach(library -> {
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
        if (vanillaPresent) {// FIXME: trash code
            setupDevEnv.configure(t -> t.dependsOn(setupVanillaDevTask));
            tasks.named(JavaPlugin.CLASSES_TASK_NAME, task -> task.dependsOn(setupVanillaDevTask));
            dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, vanillaDependencyNotation);
            configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, c -> c.extendsFrom(mcVanillaConfiguration.get()));

            var defaultClient = project.getObjects().newInstance(ClientRunConfigImpl.class, "defaultVanillaClient");
            configureDefaultVanillaClientArgs(vmc.getVersionJson(version), defaultClient);
            // We use JavaExec.classpath to add classpaths, but we need to set this variable because arguments in version json needs it
            defaultClient.getVariables().put("classpath", "none");
            defaultClient.getVariables().put("natives_directory", extractNativesTask.flatMap(ExtractNativesTask::getExtract)
                    .map(dir -> dir.getAsFile().getAbsolutePath()));
            extension.getRuns().withType(ClientRunConfig.class).configureEach(config -> {
                config.getParents().add(defaultClient);
                config.getRunTask().configure(task -> task.classpath(setupVanillaDevTask));
            });

            var defaultData = project.getObjects().newInstance(DataRunConfigImpl.class, "defaultVanillaData");
            defaultData.getMainClass().set("net.minecraft.data.Main");
            extension.getRuns().withType(DataRunConfig.class).configureEach(config -> {
                config.getParents().add(defaultData);
                config.getRunTask().configure(task -> task.classpath(setupVanillaDevTask));
            });

            var defaultServer = project.getObjects().newInstance(ServerRunConfigImpl.class, "defaultVanillaServer");
            defaultServer.getMainClass().set("net.minecraft.server.Main");
            extension.getRuns().withType(ServerRunConfig.class).configureEach(config -> {
                config.getParents().add(defaultServer);
                config.getRunTask().configure(task -> task.classpath(setupVanillaDevTask));
            });
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
            var sourceSet = forgeSourceSet.get();
            configurations.named(sourceSet.getImplementationConfigurationName(), c -> c.extendsFrom(mcVanillaConfiguration.get()));
        }
        if (fabricPresent) {
            project.getRepositories().maven(repo -> {
                repo.setName("Fabric Maven");
                repo.setUrl("https://maven.fabricmc.net/");
            });
            var sourceSet = fabricSourceSet.get();
            dependencies.add(sourceSet.getImplementationConfigurationName(), vanillaDependencyNotation);
            configurations.named(sourceSet.getImplementationConfigurationName(), c -> c.extendsFrom(mcVanillaConfiguration.get()));
        }
    }

    private void configureDefaultVanillaClientArgs(VersionJson versionJson, ClientRunConfig config) {
        var arguments = versionJson.arguments();
        if (arguments != null) {
            var game = arguments.get("game");
            game.stream().filter(Argument.Simple.class::isInstance)
                    .map(Argument.Simple.class::cast)
                    .map(Argument.Simple::value)
                    .forEach(config.getGameArguments()::add);
            game.stream().filter(Argument.Conditional.class::isInstance)
                    .map(Argument.Conditional.class::cast)
                    .forEach(config.getConditionalGameArguments()::add);
            var jvm = arguments.get("jvm");
            jvm.stream().filter(Argument.Simple.class::isInstance)
                    .map(Argument.Simple.class::cast)
                    .map(Argument.Simple::value)
                    .forEach(config.getJvmArguments()::add);
            jvm.stream().filter(Argument.Conditional.class::isInstance)
                    .map(Argument.Conditional.class::cast)
                    .forEach(config.getConditionalJvmArguments()::add);
        }
        var minecraftArguments = versionJson.minecraftArguments();
        if (minecraftArguments != null) {
            throw new UnsupportedOperationException("TODO");// TODO: support older versions
        }
        if (arguments == null && minecraftArguments == null) throw new NullPointerException();
        var logging = versionJson.logging();
        if (logging != null && logging.containsKey("client")) {
            var clientLogging = logging.get("client");
            config.getJvmArguments().add(clientLogging.argument().replace("${path}",
                    vmc.getLoggingConfig(clientLogging.file())));
        }
        config.getMainClass().set(versionJson.mainClass());
        config.getVariables().put("auth_player_name", config.getUsername());
        config.getVariables().put("version_name", versionJson.id());
        config.getVariables().put("game_directory", ".");
        config.getVariables().put("assets_root", vmc.assetsDir.toString());
        config.getVariables().put("assets_index_name", versionJson.assets());
        config.getVariables().put("auth_uuid", "Unknown");
        config.getVariables().put("auth_access_token", "Unknown");
        config.getVariables().put("clientid", "Unknown");
        config.getVariables().put("auth_xuid", "Unknown");
        config.getVariables().put("user_type", "msa");
        config.getVariables().put("version_type", versionJson.type().toString());
        config.getVariables().put("launcher_name", Constants.PISTON_GRADLE);
        config.getVariables().put("launcher_version", Constants.VERSION);
    }
}
