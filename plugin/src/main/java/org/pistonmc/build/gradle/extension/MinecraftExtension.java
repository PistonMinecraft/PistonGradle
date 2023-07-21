package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.run.RunConfig;

import java.util.List;

public interface MinecraftExtension {
    Property<String> getVersion();

    Property<MappingConfig> getMappings();

    ListProperty<RegularFile> getAccessTransformers();

    ListProperty<RegularFile> getAccessWideners();

    MappingConfig official();

//    MappingConfig parchment();

    ModdingToolchainSpec getToolchains();

    void toolchains(Action<? super ModdingToolchainSpec> action);

    ExtensiblePolymorphicDomainObjectContainer<RunConfig> getRuns();

    Provider<List<RegularFile>> getAllAccessTransformers();

    Provider<List<RegularFile>> getAllAccessWideners();
}