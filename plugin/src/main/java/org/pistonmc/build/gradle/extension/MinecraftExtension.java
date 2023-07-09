package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.provider.Property;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.run.RunConfig;

public interface MinecraftExtension {
    Property<String> getVersion();

    Property<MappingConfig> getMapping();

    MappingConfig official();

//    MappingConfig parchment();

    ModdingToolchainSpec getToolchains();

    void toolchains(Action<? super ModdingToolchainSpec> action);

    ExtensiblePolymorphicDomainObjectContainer<RunConfig> getRuns();
}