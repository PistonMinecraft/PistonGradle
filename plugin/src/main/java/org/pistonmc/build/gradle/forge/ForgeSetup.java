package org.pistonmc.build.gradle.forge;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.JarUtil;
import cn.maxpixel.mcdecompiler.util.LambdaUtil;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.forge.config.RunConfig;
import org.pistonmc.build.gradle.forge.config.Side;
import org.pistonmc.build.gradle.forge.config.UserDevConfig;
import org.pistonmc.build.gradle.forge.task.*;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.repo.GeneratedRepo;
import org.pistonmc.build.gradle.run.forge.*;
import org.pistonmc.build.gradle.run.forge.impl.ForgeClientImpl;
import org.pistonmc.build.gradle.run.forge.impl.ForgeDataImpl;
import org.pistonmc.build.gradle.run.forge.impl.ForgeGameTestServerImpl;
import org.pistonmc.build.gradle.run.forge.impl.ForgeServerImpl;
import org.pistonmc.build.gradle.task.ExtractNatives;
import org.pistonmc.build.gradle.util.DependencyUtil;
import org.pistonmc.build.gradle.util.Utils;
import org.pistonmc.build.gradle.util.version.VersionJson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ForgeSetup {
    private static final String FORGE_GROUP = "net.minecraftforge";
    private static final String FORGE_ARTIFACT = "forge";
    private static final Map.Entry<String, String> GROUP_ENTRY = Map.entry("group", FORGE_GROUP);
    private static final Map.Entry<String, String> ARTIFACT_ENTRY = Map.entry("name", "forge");
    private static final Map.Entry<String, String> USERDEV_CLASSIFIER_ENTRY = Map.entry("classifier", "userdev");

    private final Provider<Path> extractBaseDir;
    private final Provider<Directory> setupBaseDir;
    private final Provider<Directory> cpDir;

    private NamedDomainObjectProvider<SourceSet> sourceSet;

    private final TaskProvider<SetupMCP> setupMcp;
    private final TaskProvider<PatchTask> genPatched;
    private final TaskProvider<SourcesTask> genSources;
    private final TaskProvider<JavaCompile> genRecompile;
    private final TaskProvider<Jar> packRecompile;
    private final TaskProvider<GenExtras> genExtras;
    private final TaskProvider<Zip> packRuntimeMappings;
    private final TaskProvider<ReobfTask> reobfForgeJar;
    private TaskProvider<Jar> forgeJar;

    private final NamedDomainObjectProvider<Configuration> forgeSetup;
    private final NamedDomainObjectProvider<Configuration> forgeMc;
    private final Provider<Dependency> userDevJar;
    private final Dependency atJar;
    private final Dependency sasJar;
    private final VanillaMinecraftCache vmc;

    public ForgeSetup(Project project, MinecraftExtension extension, VanillaMinecraftCache vmc, GeneratedRepo generatedRepo) {
        this.vmc = vmc;
        this.extractBaseDir = project.getLayout().getBuildDirectory().dir(ForgeConstants.EXTRACT_BASE_DIR).map(dir -> dir.getAsFile().toPath().toAbsolutePath().normalize());
        this.setupBaseDir = project.getLayout().getBuildDirectory().dir(ForgeConstants.SETUP_BASE_DIR);
        this.cpDir = setupBaseDir.map(dir -> dir.dir("classpath_files"));
        var forgeConfig = extension.getToolchains().getForgeConfig();
        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        var configurations = project.getConfigurations();
        this.forgeSetup = configurations.register(ForgeConstants.SETUP_CONFIGURATION, config -> {
            config.setDescription("Used when setting up Forge workspace");
            config.setVisible(false);
            config.setCanBeConsumed(false);
            config.setTransitive(false);
        });
        this.forgeMc = configurations.register(ForgeConstants.MC_CONFIGURATION, config -> {
            config.setDescription("Marks all dependencies from Forged MC");
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
        this.sourceSet = sourceSets.register(ForgeConstants.SOURCE_SET, sourceSet -> {
            sourceSet.getJava().srcDirs("src/forge/java");
            sourceSet.getResources().srcDirs("src/forge/resources");
            var mainOutput = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput();
            sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(mainOutput));
            sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(mainOutput));
            this.forgeJar = tasks.register(sourceSet.getJarTaskName(), Jar.class, task -> {
                task.from(sourceSet.getOutput());
                task.getArchiveAppendix().set("forge");
            });
        });

        this.setupMcp = tasks.register("setupMCP", SetupMCP.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getCache().set(vmc);
            task.getForgeSetup().set(forgeSetup);
            task.getBaseDirectory().set(setupBaseDir.map(dir -> dir.dir("mcp")));
            task.getAccessTransformerJar().from(forgeSetup.map(c -> c.fileCollection(atJar)));
            task.getSideAnnotationStripperJar().from(forgeSetup.map(c -> c.fileCollection(sasJar)));
        });
        this.genPatched = tasks.register("genForgePatched", PatchTask.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.dependsOn(setupMcp);
            task.getInputJar().set(setupMcp.flatMap(SetupMCP::getOutputDirectory).map(dir -> dir.file("patch/output.jar")));// TODO: Remove this hardcoded shit
            task.getOutputJar().set(forgeConfig.getArtifactVersion().flatMap(v -> generatedRepo.getPath(FORGE_GROUP, FORGE_ARTIFACT, v, "patched")));
        });
        var runtimeMappings = setupBaseDir.map(dir -> dir.dir("runtime_mappings"));
        this.genSources = tasks.register("genForgeSources", SourcesTask.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getPatchedJar().set(genPatched.flatMap(PatchTask::getOutputJar));
            task.getMappings().from(extension.getMappings());
            task.getObf2Official().fileProvider(extension.getVersion().map(vmc::getClientMappingsFile));
            task.getOutputSourcesJar().set(forgeConfig.getGeneratedArtifactVersion().flatMap(v -> generatedRepo.getPath(FORGE_GROUP, FORGE_ARTIFACT, v, "sources")));
            task.getOutputMethodsCsv().set(runtimeMappings.map(dir -> dir.file("methods.csv")));
            task.getOutputFieldsCsv().set(runtimeMappings.map(dir -> dir.file("fields.csv")));
            task.getOutputMappings().set(setupBaseDir.map(dir -> dir.file("srg2mcp.tsrg")));
        });
        this.packRuntimeMappings = tasks.register("packForgeRuntimeMappings", Zip.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.from(runtimeMappings).include("**").dependsOn(genSources);
            ((RegularFileProperty) task.getArchiveFile()).set(forgeConfig.getGeneratedArtifactVersion().flatMap(v -> generatedRepo.getPath(FORGE_GROUP, "runtime-mappings", v)));
        });
        this.genExtras = tasks.register("genForgeClientExtras", GenExtras.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getClientJar().fileProvider(extension.getVersion().map(vmc::getClientJarFile));
            task.getOutputJar().set(setupBaseDir.map(dir -> dir.file("client-extra.jar")));
        });
        this.genRecompile = tasks.register("genForgeRecomp", JavaCompile.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.setSource(project.zipTree(genSources.flatMap(SourcesTask::getOutputSourcesJar)));
            task.getDestinationDirectory().set(setupBaseDir.map(dir -> dir.dir("recomp")));
        });
        this.packRecompile = tasks.register("packForgeRecomp", Jar.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getManifest().from(genRecompile.flatMap(AbstractCompile::getDestinationDirectory).map(dir -> dir.file("META-INF/MANIFEST.MF")));
            task.from(genRecompile.flatMap(AbstractCompile::getDestinationDirectory)).include("**");
            ((RegularFileProperty) task.getArchiveFile()).set(forgeConfig.getGeneratedArtifactVersion().flatMap(v -> generatedRepo.getPath(FORGE_GROUP, FORGE_ARTIFACT, v)));// FIXME: Possibly broken in future versions
        });
        this.reobfForgeJar = tasks.register("reobfForgeJar", ReobfTask.class, task -> {
            task.setGroup(ForgeConstants.TASK_GROUP);
            task.getInputJar().set(sourceSet.flatMap(s -> tasks.named(s.getJarTaskName(), Jar.class)).flatMap(AbstractArchiveTask::getArchiveFile));
            task.getMcJar().set(packRecompile.flatMap(AbstractArchiveTask::getArchiveFile));
            task.getMappings().set(genSources.flatMap(SourcesTask::getOutputMappings));
        });
    }

    public void postSetup(Project project, MinecraftExtension extension, TaskProvider<Task> setupDevEnv, TaskProvider<ExtractNatives> extractNativesTask,
                          NamedDomainObjectProvider<Configuration> mcVanillaConfig) {
        project.getRepositories().maven(repo -> {
            repo.setName("Forge Maven");
            repo.setUrl("https://maven.minecraftforge.net/");
            repo.metadataSources(sources -> {
                sources.gradleMetadata();
                sources.mavenPom();
                sources.artifact();
            });
        });
        var toolchains = project.getExtensions().getByType(JavaToolchainService.class);
        var dependencies = project.getDependencies();
        var forgeConfig = extension.getToolchains().getForgeConfig();
        var forgeSetup = this.forgeSetup.get();
        var forgeSourceSet = this.sourceSet.get();
        var userDevJar = this.userDevJar.get();
        var userDevConfig = UserDevConfig.load(forgeSetup.copy().fileCollection(dep -> Utils.hashEquals(dep, userDevJar)),
                extractBaseDir.get(), forgeSetup, dependencies);
        forgeMc.configure(c -> {
            c.extendsFrom(mcVanillaConfig.get());
            var deps = c.getDependencies();
            deps.addAll(userDevConfig.mcp.libraries.get(Side.JOINED));
            deps.addAll(userDevConfig.libraries);
            deps.add(dependencies.create(project.files(genExtras.flatMap(GenExtras::getOutputJar),
                    packRuntimeMappings.flatMap(AbstractArchiveTask::getArchiveFile))
                    .builtBy(genExtras, packRuntimeMappings)));
        });
        project.getConfigurations().named(forgeSourceSet.getImplementationConfigurationName(), c -> {
            c.extendsFrom(forgeMc.get());
            c.getDependencies().addLater(forgeConfig.getGeneratedArtifactVersion().map(v -> "net.minecraftforge:forge:" + v).map(dependencies::create));
        });

        setupMcp.configure(task -> {
            task.getConfig().set(userDevConfig);
            task.getMcVanilla().set(mcVanillaConfig);
            task.getAccessTransformers().set(extension.getAllAccessTransformers());
        });
        genPatched.configure(task -> {
            task.getPatchDir().set(userDevConfig.patches.toFile());
            task.getOriginalPrefix().set(userDevConfig.patchesOriginalPrefix);
            task.getModifiedPrefix().set(userDevConfig.patchesModifiedPrefix);
            task.getSourcesJar().set(forgeSetup.fileCollection(dep -> DependencyUtil.groupAndNameEquals(dep, userDevConfig.sources)).filter(f -> f.getPath().contains("-sources")));// FIXME: WTF
        });
        genSources.configure(task -> {
            task.getEncoding().set(userDevConfig.sourceFileCharset);
            task.getObf2Srg().set(userDevConfig.mcp.extractPath.resolve((String) userDevConfig.mcp.data.get("mappings")).toFile());
            task.getOfficial().set(userDevConfig.mcp.official);
        });
        genRecompile.configure(task -> {
            task.setClasspath(forgeMc.get());
            task.getJavaCompiler().set(toolchains.compilerFor(spec -> spec.getLanguageVersion().set(userDevConfig.mcp.javaTarget)));
            task.doLast("copyResources", t -> {
                var universal = forgeSetup.fileCollection(dep -> DependencyUtil.groupAndNameEquals(dep, userDevConfig.universal)).filter(f -> f.getPath().contains("-universal")).getSingleFile();// FIXME: WTF
                try (var fs = JarUtil.createZipFs(universal.toPath());
                     var files = FileUtil.iterateFiles(fs.getPath(""))) {
                    files.filter(p -> {
                        String path = p.toString();
                        return !(path.endsWith(".class") || (p.startsWith("META-INF") && (path.endsWith(".DSA") || path.endsWith(".SF"))));
                    }).forEach(LambdaUtil.unwrapConsumer(p -> {
                        String path = p.toString();
                        File f = task.getDestinationDirectory().file(path).get().getAsFile();
                        if (f.exists()) return;
                        // TODO: universal filters
                        f.getParentFile().mkdirs();
                        f.createNewFile();
                        try (var is = Files.newInputStream(p);
                             var os = new FileOutputStream(f)) {
                            is.transferTo(os);
                        }
                    }));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        genExtras.configure(task -> {
            task.getEncoding().set(userDevConfig.mcp.encoding);
            task.getMcpMappings().set(userDevConfig.mcp.extractPath.resolve((String) userDevConfig.mcp.data.get("mappings")).toFile());
        });
        setupDevEnv.configure(t -> t.dependsOn(packRecompile));
        var tasks = project.getTasks();
        tasks.named(forgeSourceSet.getCompileJavaTaskName(), t -> t.dependsOn(packRecompile));
        tasks.named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(reobfForgeJar));

        var forgeModules = project.getConfigurations().create("forgeModules");
        forgeModules.setDescription("Configuration that stores all modules from Forge userdev config");
        forgeModules.setCanBeConsumed(false);
        forgeModules.setVisible(false);
        forgeModules.getDependencies().addAll(userDevConfig.modules);

        var objects = project.getObjects();
        var rtc = project.getConfigurations().named(forgeSourceSet.getRuntimeClasspathConfigurationName());
        project.delete(cpDir);
        project.mkdir(cpDir);
        extension.getRuns().withType(ForgeRunConfig.class).configureEach(config -> config.getRunTask().configure(task -> task.classpath(forgeSourceSet.getRuntimeClasspath())));
        for (Map.Entry<String, RunConfig> run : userDevConfig.runs.entrySet()) {
            var value = run.getValue();
            switch (run.getKey()) {
                case "client" -> {
                    var defaultClient = objects.newInstance(ForgeClientImpl.class, "defaultForgeClient");
                    configureRunConfig(extension, userDevConfig, value, defaultClient, vmc.getVersionJson(extension.getVersion().get()), forgeModules, rtc, packRecompile.flatMap(AbstractArchiveTask::getArchiveFile));
                    defaultClient.getVariables().put("natives", extractNativesTask.flatMap(ExtractNatives::getExtract).map(Utils::mapToAbsolutePath));
                    extension.getRuns().withType(ForgeClient.class).configureEach(config -> config.getParents().add(defaultClient));
                }
                case "data" -> {
                    var defaultData = objects.newInstance(ForgeDataImpl.class, "defaultForgeData");
                    configureRunConfig(extension, userDevConfig, value, defaultData, vmc.getVersionJson(extension.getVersion().get()), forgeModules, rtc, packRecompile.flatMap(AbstractArchiveTask::getArchiveFile));
                    extension.getRuns().withType(ForgeData.class).configureEach(config -> config.getParents().add(defaultData));
                }
                case "server" -> {
                    var defaultServer = objects.newInstance(ForgeServerImpl.class, "defaultForgeServer");
                    configureRunConfig(extension, userDevConfig, value, defaultServer, vmc.getVersionJson(extension.getVersion().get()), forgeModules, rtc, packRecompile.flatMap(AbstractArchiveTask::getArchiveFile));
                    extension.getRuns().withType(ForgeServer.class).configureEach(config -> config.getParents().add(defaultServer));
                }
                case "gameTestServer" -> {
                    var defaultGameTestServer = objects.newInstance(ForgeGameTestServerImpl.class, "defaultGameTestServer");
                    configureRunConfig(extension, userDevConfig, value, defaultGameTestServer, vmc.getVersionJson(extension.getVersion().get()), forgeModules, rtc, packRecompile.flatMap(AbstractArchiveTask::getArchiveFile));
                    extension.getRuns().withType(ForgeGameTestServer.class).configureEach(config -> config.getParents().add(defaultGameTestServer));
                }
            }
        }
    }

    private static Map<String, String> userDevDependency(String artifactVersion) {
        return Map.ofEntries(GROUP_ENTRY, ARTIFACT_ENTRY, Map.entry("version", artifactVersion), USERDEV_CLASSIFIER_ENTRY);
    }

    private void configureRunConfig(MinecraftExtension extension, UserDevConfig userDevConfig, RunConfig forgeConfig, ForgeRunConfig config,
                                    VersionJson versionJson, Configuration forgeModules, NamedDomainObjectProvider<Configuration> runtimeClasspath,
                                    Provider<RegularFile> minecraftJar) {
        config.getMainClass().set(forgeConfig.main());
        config.getGameArguments().set(forgeConfig.args());
        config.getJvmArguments().set(forgeConfig.jvmArgs());
        config.getClient().set(forgeConfig.client());
        config.getEnvironments().set(forgeConfig.env());
        config.getProperties().set(forgeConfig.props());

        var vars = config.getVariables();
        vars.put("assets_root", vmc.assetsDir.toString());
        vars.put("asset_index", versionJson.assets());
        vars.put("modules", Utils.joinAbsoluteFiles(forgeModules));
        vars.put("mc_version", userDevConfig.mcp.version);
        vars.put("mcp_version", userDevConfig.mcpDep.getVersion());
        vars.put("mcp_mappings", extension.getMappings().flatMap(MappingConfig::getMappingName).getOrElse("unknown"));
        vars.put("source_roots", sourceSet.map(SourceSet::getOutput).map(Utils::joinAbsoluteFiles));
        vars.put("runtime_classpath", runtimeClasspath.map(Utils::joinAbsoluteFiles));
        vars.put("runtime_classpath_file", runtimeClasspath.map(this::writeConfiguration));
        vars.put("minecraft_classpath", forgeMc.map(Utils::joinAbsoluteFiles).zip(minecraftJar, ForgeSetup::joinFile));
        vars.put("minecraft_classpath_file", forgeMc.zip(minecraftJar, this::writeConfiguration));
    }

    private static String joinFile(String l, RegularFile f) {
        return l + File.pathSeparatorChar + f.getAsFile().getAbsolutePath();
    }

    private String writeConfiguration(Configuration configuration) {
        var file = cpDir.get().file(configuration.getName() + ".txt").getAsFile();
        if (!file.exists()) {
            try {
                file.createNewFile();
                try (var os = new FileOutputStream(file)) {
                    os.write(String.join(System.lineSeparator(), configuration.getFiles().stream().map(File::getAbsolutePath)
                            .toArray(String[]::new)).getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file.getAbsolutePath();
    }

    private String writeConfiguration(Configuration configuration, RegularFile f) {
        var file = cpDir.get().file(configuration.getName() + ".txt").getAsFile();
        if (!file.exists()) {
            try {
                file.createNewFile();
                try (var os = new FileOutputStream(file)) {
                    os.write(String.join(System.lineSeparator(), configuration.getFiles().stream().map(File::getAbsolutePath)
                            .toArray(String[]::new)).getBytes(StandardCharsets.UTF_8));
                    os.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    os.write(f.getAsFile().getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file.getAbsolutePath();
    }
}