package org.pistonmc.build.gradle.cache;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.Constants;
import org.pistonmc.build.gradle.PistonGradlePlugin;
import org.pistonmc.build.gradle.util.AssetIndex;
import org.pistonmc.build.gradle.util.version.VersionJson;
import org.pistonmc.build.gradle.util.version.VersionManifest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

public class VanillaMinecraftCache {
    private static final HttpRequest MANIFEST_REQUEST = HttpRequest.newBuilder(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")).build();
    private final Logger logger;
    private final Path cacheDir;
    public final Path assetsDir;
    public final Path versionsDir;
    public final Path versionManifestFile;
    private final Object2ObjectOpenHashMap<String, VersionJson> versionJsonCache = new Object2ObjectOpenHashMap<>();
    private final Provider<Boolean> forceUpdateVersionManifest;

    public VanillaMinecraftCache(Project project) {
        this.logger = project.getLogger();
        var providers = project.getProviders();
        this.cacheDir = project.getGradle().getGradleUserHomeDir().toPath().resolve(Constants.CACHE_DIR).resolve("vanilla_minecraft").toAbsolutePath().normalize();
        logger.info("Vanilla Minecraft cache dir: {}", cacheDir);
        this.assetsDir = cacheDir.resolve("assets");
        this.versionsDir = cacheDir.resolve("versions");
        this.versionManifestFile = versionsDir.resolve("version_manifest_v2.json");
        this.forceUpdateVersionManifest = providers.gradleProperty(Constants.FORCE_UPDATE_VERSION_MANIFEST).map(Boolean::parseBoolean).orElse(false);
    }

    private VersionManifest versionManifest;

    public VersionManifest getVersionManifest() {
        if (versionManifest != null) return versionManifest;
        try  {
            if (Files.notExists(versionManifestFile) || forceUpdateVersionManifest.get() ||
                    Files.getLastModifiedTime(versionManifestFile).toMillis() <
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
                logger.lifecycle("Fetching version manifest...");
                PistonGradlePlugin.CLIENT.send(MANIFEST_REQUEST, HttpResponse.BodyHandlers.ofFile(
                        FileUtil.ensureFileExist(versionManifestFile), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                logger.info("Fetched version manifest");
            }
            try (var reader = Files.newBufferedReader(versionManifestFile)) {
                return this.versionManifest = PistonGradlePlugin.GSON.fromJson(reader, VersionManifest.class);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error fetching version manifest", e);
            throw Utils.wrapInRuntime(e);
        }
    }

    public Path getVersionDir(String id) {
        return versionsDir.resolve(id);
    }

    public Path getVersionJsonFile(String id) {
        return getVersionDir(id).resolve(id + ".json");
    }

    public Path getAssetDir(String id) {
        return assetsDir.resolve(id);
    }

    public VersionJson getVersionJson(String id) {
        return versionJsonCache.computeIfAbsent(id, this::loadVersionJson);
    }

    private VersionJson loadVersionJson(String id) {
        Path path = getVersionJsonFile(id);
        var version = getVersionManifest().versions().get(id);
        if (version == null) throw new IllegalArgumentException("u dumb. there's no such mc version");
        try {
            if (!FileUtil.verify(path, version.sha1())) {
                var request = HttpRequest.newBuilder(new URI(version.url())).build();
                logger.lifecycle("Downloading version json for Minecraft {}...", id);
                PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                logger.info("Downloaded version json for Minecraft {}", id);
            }
            try (var reader = Files.newBufferedReader(path)) {
                return PistonGradlePlugin.GSON.fromJson(reader, VersionJson.class);
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw Utils.wrapInRuntime(e);
        }
    }

    private Path getDownload(String id, String key, String fileName, String logName) {
        Path path = getVersionDir(id).resolve(fileName);
        var version = getVersionJson(id).downloads().get(key);
        try {
            if (!FileUtil.verify(path, version.sha1(), version.size())) {
                var request = HttpRequest.newBuilder(new URI(version.url())).build();
                logger.lifecycle("Downloading {} for Minecraft {}...", logName, id);
                PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                logger.info("Downloaded {} for Minecraft {}", logName, id);
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw Utils.wrapInRuntime(e);
        }
        return path;
    }

    public Path getClientJar(String id) {
        return getDownload(id, "client", "client.jar", "client jar");
    }

    public Path getServerJar(String id) {
        return getDownload(id, "server", "server.jar", "server jar");
    }

    public Path getClientMappings(String id) {
        return getDownload(id, "client_mappings", "client.txt", "client mappings");
    }

    public Path getServerMappings(String id) {
        return getDownload(id, "server_mappings", "server.txt", "server mappings");
    }

    public File getClientJarFile(String id) {
        return getClientJar(id).toFile();
    }

    public File getServerJarFile(String id) {
        return getServerJar(id).toFile();
    }

    public File getClientMappingsFile(String id) {
        return getClientMappings(id).toFile();
    }

    public File getServerMappingsFile(String id) {
        return getServerMappings(id).toFile();
    }

    public String getLoggingConfig(VersionJson.LoggingConfig.File file) {
        Path path = getAssetDir("logging").resolve(file.id());
        try {
            if (!FileUtil.verify(path, file.sha1(), file.size())) {
                var request = HttpRequest.newBuilder(new URI(file.url())).build();
                logger.info("Downloading logging config to {}...", path);
                PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                logger.info("Downloaded logging config");
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw Utils.wrapInRuntime(e);
        }
        return path.toString();
    }

    public AssetIndex getAssetIndex(String id) {
        return getAssetIndex(getVersionJson(id).assetIndex());
    }

    public AssetIndex getAssetIndex(VersionJson.AssetIndex assetIndex) {
        Path path = getAssetDir("indexes").resolve(assetIndex.id() + ".json");
        try {
            if (!FileUtil.verify(path, assetIndex.sha1(), assetIndex.size())) {
                var request = HttpRequest.newBuilder(new URI(assetIndex.url())).build();
                logger.info("Downloading asset index to {}...", path);
                PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                logger.info("Downloaded asset index config");
            }
            try (var reader = Files.newBufferedReader(path)) {
                return PistonGradlePlugin.GSON.fromJson(reader, AssetIndex.class);
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw Utils.wrapInRuntime(e);
        }
    }

    public Path getAssetObjects() {
        return getAssetDir("objects");
    }

    public Path getAssetPath(String hash) {
        return getAssetObjects().resolve(hash.substring(0, 2)).resolve(hash);
    }
}