package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;

public interface ModdingToolchainSpec {
    void config(Action<? super ToolchainConfig> configAction);

    void vanilla();

    void vanilla(Action<? super ToolchainConfig> configAction);

    void forge(Action<? super ToolchainConfig.Forge> configAction);

    void fabric(Action<? super ToolchainConfig.Fabric> configAction);
}