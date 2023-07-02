package org.pistonmc.build.gradle.task;

import cn.maxpixel.mcdecompiler.mapping.NamespacedMapping;
import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import cn.maxpixel.mcdecompiler.reader.ClassifiedMappingReader;
import cn.maxpixel.mcdecompiler.util.NamingUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import cn.maxpixel.mcdecompiler.writer.ClassifiedMappingWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.mapping.MergeResult;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class MergeMappingsTask extends DefaultTask {
    public static final String FROM_NAMESPACE = "from";
    public static final String TO_NAMESPACE = "to";

    @Nested
    public abstract ListProperty<MappingConfig> getInputMappings();
    @OutputFile
    public abstract RegularFileProperty getOutputMapping();
    @Internal
    public abstract Property<MergeResult> getOutputMappingConfig();

    @TaskAction
    public void merge() {
        var configs = getInputMappings().get();
        ClassifiedMappingWriter<NamespacedMapping> writer = new ClassifiedMappingWriter<>(MappingTypes.TINY_V2);
        configs.stream().map(c -> {
            try (FileReader fr = new FileReader(c.getMappings().get().getAsFile(), StandardCharsets.UTF_8)) {
                var type = c.getType().get();
                ClassifiedMappingReader<?> r = new ClassifiedMappingReader<>(type, fr);
                if (type.isNamespaced()) {
                    var sourceNamespace = c.getSourceNamespace().get();
                    var targetNamespace = c.getTargetNamespace().get();
                    if (sourceNamespace.equals(targetNamespace)) throw new IllegalArgumentException("you stupid");
                    var reader = ((ClassifiedMappingReader<NamespacedMapping>) r);
                    var mappingSourceNamespace = NamingUtil.findSourceNamespace(reader.mappings);
                    if (!mappingSourceNamespace.equals(sourceNamespace)) {
                        ClassifiedMappingReader.swap(reader, mappingSourceNamespace, sourceNamespace);
                    }
                    reader.mappings.forEach(cm -> {
                        cm.mapping.setUnmappedNamespace(sourceNamespace);
                        cm.mapping.setMappedNamespace(targetNamespace);
                    });
                }
            } catch (IOException e) {
                throw Utils.wrapInRuntime(e);
            }
        });
    }
}