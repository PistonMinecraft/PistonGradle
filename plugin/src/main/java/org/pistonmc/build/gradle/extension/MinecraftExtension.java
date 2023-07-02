package org.pistonmc.build.gradle.extension;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;

public interface MinecraftExtension {
    Property<String> getVersion();

    ModdingToolchainSpec getToolchains();

    void toolchains(Action<? super ModdingToolchainSpec> action);
}