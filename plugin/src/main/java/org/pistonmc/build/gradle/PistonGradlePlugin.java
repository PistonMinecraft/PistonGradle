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
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.impl.MinecraftExtensionImpl;
import org.pistonmc.build.gradle.extension.impl.ModdingToolchainSpecImpl;
import org.pistonmc.build.gradle.repo.GeneratedRepo;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.run.DataRunConfig;
import org.pistonmc.build.gradle.run.RunConfig;
import org.pistonmc.build.gradle.run.ServerRunConfig;
import org.pistonmc.build.gradle.run.impl.ClientRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.DataRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.ServerRunConfigImpl;
import org.pistonmc.build.gradle.task.ExtractNativesTask;
import org.pistonmc.build.gradle.task.PrepareAssetsTask;
import org.pistonmc.build.gradle.task.SetupVanillaDevTask;
import org.pistonmc.build.gradle.util.LowercaseEnumTypeAdapterFactory;
import org.pistonmc.build.gradle.util.OSName;
import org.pistonmc.build.gradle.util.VariableUtil;
import org.pistonmc.build.gradle.util.ZonedDateTimeTypeAdapter;
import org.pistonmc.build.gradle.util.version.Argument;
import org.pistonmc.build.gradle.util.version.Rule;
import org.pistonmc.build.gradle.util.version.VersionJson;
import org.pistonmc.build.gradle.util.version.VersionManifest;

import java.io.File;
import java.net.http.HttpClient;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private TaskProvider<PrepareAssetsTask> prepareAssetsTask;

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
//            config.setCanBeResolved(false);
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
        this.prepareAssetsTask = tasks.register(Constants.PREPARE_ASSETS_TASK, PrepareAssetsTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.doNotTrackState("WIP");// TODO
            task.getVersion().set(extension.getVersion());
            task.getCache().set(vmc);
        });
        var extractNativesTask = tasks.register(Constants.EXTRACT_NATIVES_TASK, ExtractNativesTask.class);// TODO
        this.setupDevEnv = tasks.register(Constants.SETUP_DEV_ENV_TASK, task -> {
            task.setGroup(Constants.TASK_GROUP);
        });

        project.afterEvaluate(this::postApply);
    }

    public void postApply(@NotNull Project project) {
        var version = extension.getVersion().get();
        var toolchains = (ModdingToolchainSpecImpl) extension.getToolchains();
        var vanilla = toolchains.getVanillaConfig();
        var forge = toolchains.getForgeConfig();
        var fabric = toolchains.getFabricConfig();
        var vanillaPresent = vanilla.isPresent();
        var forgePresent = forge.isPresent();
        var fabricPresent = fabric.isPresent();
        var javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        var vanillaDependencyNotation = Map.of(
                "group", "net.minecraft",
                "name", "vanilla",
                "version", version,
                "classifier", "client"
        );
        if (vanillaPresent || forgePresent || fabricPresent) {
            var startParameter = project.getGradle().getStartParameter();
            if (startParameter.getTaskNames().isEmpty()) {
                startParameter.setTaskNames(List.of(setupDevEnv.getName()));
            }
            var dependencies = project.getDependencies();
            var configName = mcVanillaConfiguration.getName();
            var osName = OSName.getCurrent();
            vmc.getVersionJson(version).libraries().stream().filter(library -> library.rules() == null ||
                    library.rules().stream().allMatch(Rule::isAllow)).forEach(library -> {
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
            project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, vanillaDependencyNotation);
            project.getConfigurations().named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, c -> c.extendsFrom(mcVanillaConfiguration.get()));
            var defaultClient = project.getObjects().newInstance(ClientRunConfigImpl.class, "defaultVanillaClient");
            configureDefaultVanillaClientArgs(vmc.getVersionJson(version), defaultClient);
            var path = extension.getVersion().flatMap(v -> generatedRepo.getPath("net.minecraft", "vanilla", v, "client")).get();
            defaultClient.getVariables().put("classpath", mcVanillaConfiguration.get().resolve().stream().map(File::getAbsolutePath).collect(Collectors.joining(";")) + ';' + path);
            defaultClient.getVariables().put("natives_directory", project.getLayout().getBuildDirectory().dir("natives").get().getAsFile().getAbsolutePath());// TODO
            extension.getRuns().withType(ClientRunConfig.class, config -> config.getParents().add(defaultClient));
            var defaultData = project.getObjects().newInstance(DataRunConfigImpl.class, "defaultVanillaData");
            defaultData.getMainClass().set("net.minecraft.data.Main");
            extension.getRuns().withType(DataRunConfig.class, config -> config.getParents().add(defaultData));
            var defaultServer = project.getObjects().newInstance(ServerRunConfigImpl.class, "defaultVanillaServer");
            defaultServer.getMainClass().set("net.minecraft.server.Main");
            extension.getRuns().withType(ServerRunConfig.class, config -> config.getParents().add(defaultServer));
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
            project.getConfigurations().named(sourceSet.getImplementationConfigurationName(), c -> c.extendsFrom(mcVanillaConfiguration.get()));
        }
        if (fabricPresent) {
            project.getRepositories().maven(repo -> {
                repo.setName("Fabric Maven");
                repo.setUrl("https://maven.fabricmc.net/");
            });
            var sourceSet = forgeSourceSet.get();
            project.getDependencies().add(sourceSet.getImplementationConfigurationName(), vanillaDependencyNotation);
            project.getConfigurations().named(sourceSet.getImplementationConfigurationName(), c -> c.extendsFrom(mcVanillaConfiguration.get()));
        }
        if (vanillaPresent || forgePresent || fabricPresent) {
            extension.getRuns().configureEach(config -> {
                var name = config.getName();
                config.getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("run"));
                var taskName = "run" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                project.getLogger().info("The name of the running task for config {} is {}", name, taskName);
                project.getTasks().create(taskName, JavaExec.class, task -> {
                    task.setGroup(Constants.TASK_GROUP);
                    task.dependsOn(prepareAssetsTask);
                    var variables = config.getAllVariables().get();
                    task.jvmArgs(VariableUtil.replaceVariables(config.getAllJvmArguments().get(), variables));
                    config.getAllConditionalJvmArguments().get().forEach(conditional -> {
                        if (conditional.rules().stream().allMatch(Rule::isAllow)) {
                            task.jvmArgs(VariableUtil.replaceVariables(conditional.value(), variables));
                        }
                    });
                    task.args(VariableUtil.replaceVariables(config.getAllGameArguments().get(), variables));
                    config.getAllConditionalGameArguments().get().forEach(conditional -> {
                        if (conditional.rules().stream().allMatch(rule -> rule.isAllow(config instanceof ClientRunConfig c ? c : null))) {
                            task.args(VariableUtil.replaceVariables(conditional.value(), variables));
                        }
                    });
                    task.systemProperties(config.getAllProperties().get());
                    task.environment(config.getAllEnvironments().get());
                    task.getMainClass().set(config.getAllMainClass());
                    var dir = config.getWorkingDirectory().get().getAsFile();
                    dir.mkdirs();
                    task.setWorkingDir(dir);
                });
            });
        }
    }

    private void configureDefaultVanillaClientArgs(VersionJson versionJson, RunConfig config) {
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
        config.getVariables().put("auth_player_name", "Dev");
        config.getVariables().put("version_name", versionJson.id());
        config.getVariables().put("game_directory", ".");
        config.getVariables().put("assets_root", vmc.assetsDir.toString());
        config.getVariables().put("assets_index_name", versionJson.assets());
        config.getVariables().put("auth_uuid", "Unknown");
        config.getVariables().put("auth_access_token", "Unknown");
        config.getVariables().put("clientid", "Unknown");
        config.getVariables().put("auth_xuid", "Unknown");
        config.getVariables().put("user_type", "msa");
        config.getVariables().put("version_type", "release");
        config.getVariables().put("launcher_name", "PistonGradle");
        config.getVariables().put("launcher_version", "0.1-SNAPSHOT");
    }
}
