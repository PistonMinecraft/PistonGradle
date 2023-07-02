package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.pistonmc.build.gradle.mapping.MappingConfig;

public interface ModdingToolchainSpec {
    void mapping(MappingConfig mapping);

    MappingConfig official();

//    MappingConfig parchment();

    void config(Action<? super ToolchainConfig> configAction);

    void vanilla();

    void vanilla(Action<? super ToolchainConfig> configAction);

    void forge(Action<? super ToolchainConfig.Forge> configAction);

    void fabric(Action<? super ToolchainConfig.Fabric> configAction);
}