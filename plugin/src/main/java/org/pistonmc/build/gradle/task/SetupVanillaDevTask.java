package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.MinecraftDecompiler;
import cn.maxpixel.mcdecompiler.reader.ClassifiedMappingReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class SetupVanillaDevTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputJar();
    @Nested
    public abstract Property<MappingConfig> getMappingConfig();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void run() throws IOException {
        var config = getMappingConfig().get();
        MinecraftDecompiler mcd = new MinecraftDecompiler(
                new MinecraftDecompiler.OptionBuilder(getInputJar().get().getAsFile().toPath())
                        .withMapping(new ClassifiedMappingReader<>(config.getType().get(),
                                new FileReader(config.getMappings().get().getAsFile(), StandardCharsets.UTF_8)))
                        .output(getOutputJar().get().getAsFile().toPath())
                        .targetNamespace(config.getTargetNamespace().getOrElse("unknown"))
                        .build());
        mcd.deobfuscate();
    }
}