package org.pistonmc.build.gradle.mapping;

import cn.maxpixel.mcdecompiler.mapping.type.MappingType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public interface MappingConfig {
    /**
     * Used by Forge
     */
    @Input
    @Optional
    Property<String> getMappingName();

    @Internal
    Property<MappingType.Classified<?>> getType();

    @InputFile
    RegularFileProperty getMappings();

    /**
     * The namespace where obfuscated names stays in.
     * In the most cases you don't need to set this
     */
    @Input
    @Optional
    Property<String> getObfNamespace();

    /**
     * The target namespace to remap to
     * This is required when using namespaced mappings
     */
    @Input
    @Optional
    Property<String> getMappedNamespace();

    default void from(Provider<MappingConfig> other) {
        getType().set(other.flatMap(MappingConfig::getType));
        getMappings().set(other.flatMap(MappingConfig::getMappings));
        getObfNamespace().set(other.flatMap(MappingConfig::getObfNamespace));
        getMappedNamespace().set(other.flatMap(MappingConfig::getMappedNamespace));
    }
}