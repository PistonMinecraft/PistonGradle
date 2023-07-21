package org.pistonmc.build.gradle.forge.task;

import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import cn.maxpixel.mcdecompiler.reader.ClassifiedMappingReader;
import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.JarUtil;
import cn.maxpixel.mcdecompiler.util.LambdaUtil;
import cn.maxpixel.mcdecompiler.util.NamingUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public abstract class GenExtras extends DefaultTask {
    @Internal
    public abstract Property<Charset> getEncoding();
    @InputFile
    public abstract RegularFileProperty getClientJar();
    @InputFile
    public abstract RegularFileProperty getMcpMappings();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    public GenExtras() {
        getEncoding().convention(StandardCharsets.UTF_8).disallowUnsafeRead();
        getClientJar().disallowUnsafeRead();
        getMcpMappings().disallowUnsafeRead();
        getOutputJar().disallowUnsafeRead();
    }

    @TaskAction
    public void extract() throws IOException {
        var reader = new ClassifiedMappingReader<>(MappingTypes.TSRG_V2, new FileReader(getMcpMappings().get().getAsFile(), getEncoding().get()));
        try (var clientFs = JarUtil.createZipFs(getClientJar().get().getAsFile().toPath());
             var output = JarUtil.createZipFs(getOutputJar().get().getAsFile().toPath(), true);
             var client = FileUtil.iterateFiles(clientFs.getPath(""))) {
            var names = reader.mappings.stream().map(cm -> cm.mapping.getUnmappedName()).collect(Collectors.toSet());
            client.filter(p -> !names.contains(NamingUtil.file2Native(p.toString()))).forEach(LambdaUtil.unwrapConsumer(p -> {
                String path = p.toString();
                Path outputPath = output.getPath(path);
                try (var is = Files.newInputStream(p);
                     var os = Files.newOutputStream(FileUtil.ensureFileExist(outputPath))) {
                    is.transferTo(os);
                }
            }));
        }
    }
}