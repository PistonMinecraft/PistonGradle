package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.pistonmc.build.gradle.mapping.MappingConfig;

public interface MinecraftExtension {
    Property<String> getVersion();

    void asDemoUser();

    void withCustomResolution(int width, int height);

    Property<MappingConfig> getMapping();

    MappingConfig official();

//    MappingConfig parchment();

    ModdingToolchainSpec getToolchains();

    void toolchains(Action<? super ModdingToolchainSpec> action);
}