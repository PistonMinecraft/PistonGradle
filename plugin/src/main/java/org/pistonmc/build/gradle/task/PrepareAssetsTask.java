package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.pistonmc.build.gradle.PistonGradlePlugin;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.StandardOpenOption;

public abstract class PrepareAssetsTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Internal
    public abstract Property<VanillaMinecraftCache> getCache();

    @TaskAction
    public void run() {
        var cache = getCache().get();
        var assetIndex = cache.getAssetIndex(getVersion().get());
        assetIndex.getObjects().forEach((k, v) -> {
            var path = cache.getAssetPath(v.hash());
            if (!FileUtil.verify(path, v.hash(), v.size())) {
                try {
                    var request = HttpRequest.newBuilder(new URI(getUrl(v.hash()))).build();
                    getLogger().info("Downloading asset {} to {}...", k, path);
                    PistonGradlePlugin.CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(path),
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                    getLogger().info("Downloaded asset {}", k);
                } catch (IOException | InterruptedException | URISyntaxException e) {
                   throw Utils.wrapInRuntime(e);
                }
            }
        });
    }

    private static String getUrl(String hash) {
        return "https://resources.download.minecraft.net/" + hash.substring(0, 2) + '/' + hash;
    }
}