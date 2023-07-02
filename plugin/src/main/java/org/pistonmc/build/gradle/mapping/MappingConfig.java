package org.pistonmc.build.gradle.mapping;

import cn.maxpixel.mcdecompiler.mapping.type.MappingType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public interface MappingConfig {
    @Internal
    Property<MappingType.Classified<?>> getType();

    @InputFile
    RegularFileProperty getMappings();

    @Input
    @Optional
    Property<String> getSourceNamespace();

    @Input
    @Optional
    Property<String> getTargetNamespace();
}