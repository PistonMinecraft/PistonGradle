package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.pistonmc.build.gradle.mapping.MappingConfig;

public interface ToolchainConfig {
    void client(Action<? super RunConfig> runAction);

    void server(Action<? super RunConfig> runAction);

    void data(Action<? super RunConfig> runAction);

    interface Forge extends ToolchainConfig {
        Property<String> getVersion();
    }

    interface Fabric extends ToolchainConfig {
        Property<String> getLoaderVersion();
    }
}