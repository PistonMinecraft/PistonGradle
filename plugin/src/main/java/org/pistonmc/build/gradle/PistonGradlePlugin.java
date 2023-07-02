package org.pistonmc.build.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.impl.MinecraftExtensionImpl;
import org.pistonmc.build.gradle.extension.impl.ModdingToolchainSpecImpl;
import org.pistonmc.build.gradle.repo.GeneratedRepo;
import org.pistonmc.build.gradle.task.MergeMappingsTask;
import org.pistonmc.build.gradle.task.SetupVanillaDevTask;
import org.pistonmc.build.gradle.util.LowercaseEnumTypeAdapterFactory;
import org.pistonmc.build.gradle.util.ZonedDateTimeTypeAdapter;
import org.pistonmc.build.gradle.util.version.VersionManifest;

import java.net.http.HttpClient;
import java.time.ZonedDateTime;

public class PistonGradlePlugin implements Plugin<Project> {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(VersionManifest.class, VersionManifest.ADAPTER)
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
            .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
            .create();

    private MinecraftExtension extension;
    private TaskProvider<SetupVanillaDevTask> setupVanillaDevTask;

    public void apply(@NotNull Project project) {
        VanillaMinecraftCache vmc = new VanillaMinecraftCache(project);
        GeneratedRepo generatedRepo = new GeneratedRepo(project.getLayout().getBuildDirectory().dir(Constants.GENERATED_REPO_DIR));
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

        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.register(Constants.FORGE_SOURCE_SET, sourceSet -> {
            sourceSet.getJava().srcDirs("src/forge/java", "src/main/java");
            sourceSet.getResources().srcDirs("src/forge/resources", "src/main/resources");
        });
        sourceSets.register(Constants.FABRIC_SOURCE_SET, sourceSet -> {
            sourceSet.getJava().srcDirs("src/fabric/java", "src/main/java");
            sourceSet.getResources().srcDirs("src/fabric/resources", "src/main/resources");
        });

        var mergeMappings = project.getTasks().register("mergeMappings", MergeMappingsTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.getInputMappings().set(((ModdingToolchainSpecImpl) extension.getToolchains()).getMappings());
            task.getOutputMapping().set(project.getLayout().getBuildDirectory().file(Constants.MERGED_MAPPING_FILE));
        });
        this.setupVanillaDevTask = project.getTasks().register("setupVanillaDev", SetupVanillaDevTask.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.dependsOn(mergeMappings);
            task.getInputJar().set(project.getLayout().file(extension.getVersion().map(vmc::getClientJarFile)));
            task.getMapping().set(mergeMappings.flatMap(MergeMappingsTask::getOutputMappingConfig));
            task.getOutputJar().set(extension.getVersion().flatMap(v -> generatedRepo.getPath("net.minecraft", "vanilla", v)));
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
        if (vanillaPresent) {
            project.defaultTasks(setupVanillaDevTask.getName());
            project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME, task -> task.dependsOn(setupVanillaDevTask));
            project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "net.minecraft:vanilla:" + extension.getVersion().get());
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
