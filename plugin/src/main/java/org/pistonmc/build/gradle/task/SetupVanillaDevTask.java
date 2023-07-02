package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.MinecraftDecompiler;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.mapping.MergeResult;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class SetupVanillaDevTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputJar();
    @Nested
    public abstract Property<MergeResult> getMapping();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void run() throws IOException {
        MergeResult config = getMapping().get();
        MinecraftDecompiler mcd = new MinecraftDecompiler(
                new MinecraftDecompiler.OptionBuilder(getInputJar().get().getAsFile().toPath())
                        .doNotIncludeOthers()
                        .withMapping(new FileReader(config.getInputMappings().get().getAsFile(), StandardCharsets.UTF_8))
                        .output(getOutputJar().get().getAsFile().toPath())
                        .targetNamespace(config.getTargetNamespace().getOrElse("unknown"))
                        .build());
        mcd.deobfuscate();
    }
}