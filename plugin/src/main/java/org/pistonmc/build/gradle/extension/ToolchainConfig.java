package org.pistonmc.build.gradle.extension;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public interface ToolchainConfig {
    Property<Boolean> getEnabled();

    void mixin();

    interface Forge extends ToolchainConfig {
        Property<String> getVersion();

        Provider<String> getArtifactVersion();
    }

    interface Fabric extends ToolchainConfig {
        Property<String> getLoaderVersion();

        Property<String> getApiVersion();
    }
}