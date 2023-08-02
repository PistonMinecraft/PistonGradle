package org.pistonmc.build.gradle.forge.task;

import cn.maxpixel.mcdecompiler.MinecraftDecompiler;
import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import cn.maxpixel.mcdecompiler.reader.ClassifiedMappingReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public abstract class ReobfTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputJar();
    @InputFile
    public abstract RegularFileProperty getMcJar();
    @InputFile
    public abstract RegularFileProperty getMappings();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();// TODO: automatically setup this

    public ReobfTask() {
        getInputJar().disallowUnsafeRead();
        getMcJar().disallowUnsafeRead();
        getMappings().disallowUnsafeRead();
        getOutputJar().disallowUnsafeRead();
    }

    @TaskAction
    public void run() throws FileNotFoundException {
        MinecraftDecompiler mcd = new MinecraftDecompiler(new MinecraftDecompiler.OptionBuilder(getInputJar().get().getAsFile().toPath(), true)
                .withMapping(new ClassifiedMappingReader<>(MappingTypes.TSRG_V1, new FileInputStream(getMappings().get().getAsFile())))
                .addExtraJar(getMcJar().get().getAsFile().toPath())
                .addExtraClass("*")
                .output(getOutputJar().get().getAsFile().toPath())
                .build());
        mcd.deobfuscate();
    }
}