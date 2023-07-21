package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;
import org.pistonmc.build.gradle.PistonGradlePlugin;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.util.AssetIndex;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.StandardOpenOption;

public abstract class DownloadAssets extends DefaultTask {
    private final FileCollection outputFiles;

    @Nested
    public abstract MapProperty<String, AssetIndex.AssetEntry> getAssets();
    @Internal
    public abstract Property<VanillaMinecraftCache> getCache();

    @Inject
    public abstract WorkerExecutor getWorkers();

    @Inject
    public DownloadAssets(ProjectLayout layout) {
        getAssets().disallowUnsafeRead();
        getCache().disallowUnsafeRead();
        this.outputFiles = layout.files(getAssets().zip(getCache(), (assets, cache) -> assets.values().stream()
                .map(AssetIndex.AssetEntry::hash).map(cache::getAssetPath).collect(ObjectArrayList.toList())));
    }

    @OutputFiles
    public FileCollection getOutputFiles() {
        return outputFiles;
    }

    @TaskAction
    public void run() {
        var queue = getWorkers().noIsolation();
        var cache = getCache().get();
        getAssets().get().forEach((k, v) -> queue.submit(DownloadAsset.class, params -> {
            params.getAssetFile().set(cache.getAssetPath(v.hash()).toFile());
            params.getAssetPath().set(k);
            params.getAssetHash().set(v.hash());
            params.getAssetSize().set(v.size());
        }));
    }

    public interface DownloadAssetWorkParameters extends WorkParameters {
        RegularFileProperty getAssetFile();
        Property<String> getAssetPath();
        Property<String> getAssetHash();
        Property<Integer> getAssetSize();
    }

    public static abstract class DownloadAsset implements WorkAction<DownloadAssetWorkParameters> {
        private static final Logger LOGGER = Logging.getLogger(DownloadAsset.class);

        @Override
        public void execute() {
            var parameters = getParameters();
            var path = parameters.getAssetFile().get().getAsFile().toPath();
            var name = parameters.getAssetPath().get();
            var hash = parameters.getAssetHash().get();
            int size = parameters.getAssetSize().get();
            if (FileUtil.verify(path, hash, size)) {
                LOGGER.info("Verified {}", name);
                return;
            }
            try {
                var request = HttpRequest.newBuilder(new URI(getUrl(hash))).build();
                LOGGER.info("Downloading asset {} to {}...", name, path);
                PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                LOGGER.info("Downloaded asset {}", name);
            } catch (IOException | InterruptedException | URISyntaxException e) {
                throw Utils.wrapInRuntime(e);
            }
        }

        private static String getUrl(String hash) {
            return "https://resources.download.minecraft.net/" + hash.substring(0, 2) + '/' + hash;
        }
    }
}