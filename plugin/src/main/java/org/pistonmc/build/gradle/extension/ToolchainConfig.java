package org.pistonmc.build.gradle.extension;

import org.gradle.api.provider.Property;

public interface ToolchainConfig {
    void mixin();

    interface Forge extends ToolchainConfig {
        Property<String> getVersion();
    }

    interface Fabric extends ToolchainConfig {
        Property<String> getLoaderVersion();

        Property<String> getApiVersion();
    }
}