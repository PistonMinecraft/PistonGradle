package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.api.MinecraftDecompiler;
import cn.maxpixel.mcdecompiler.common.app.util.FileUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class SetupVanillaDev extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputJar();
    @Nested
    @Optional
    public abstract Property<MappingConfig> getMappingConfig();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    public SetupVanillaDev() {
        getInputJar().disallowUnsafeRead();
        getMappingConfig().disallowUnsafeRead();
        getOutputJar().disallowUnsafeRead();
    }

    @TaskAction
    public void run() throws IOException {
        if (!getMappingConfig().isPresent() || !getMappingConfig().get().getMappings().isPresent()) {
            getLogger().info("No mappings present, simply copying the input");
            FileUtil.copyFile(getInputJar().get().getAsFile().toPath(), getOutputJar().get().getAsFile().toPath());
            return;
        }
        var config = getMappingConfig().get();
        MinecraftDecompiler mcd = new MinecraftDecompiler(
                new MinecraftDecompiler.OptionBuilder(getInputJar().get().getAsFile().toPath())
                        .withMapping(config.getType().get().read(new FileReader(config.getMappings().get().getAsFile(), StandardCharsets.UTF_8)))
                        .output(getOutputJar().get().getAsFile().toPath())
                        .namespaceTarget(config.getMappedNamespace().getOrElse("unknown"))
                        .build());
        mcd.deobfuscate();
    }
}