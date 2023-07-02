package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;

import javax.inject.Inject;

public abstract class MinecraftExtensionImpl implements MinecraftExtension {
    private final ModdingToolchainSpec toolchains;

    @Inject
    public abstract ObjectFactory getObjectFactory();

    @Inject
    public MinecraftExtensionImpl(VanillaMinecraftCache vmc) {
        this.toolchains = getObjectFactory().newInstance(ModdingToolchainSpecImpl.class, this, vmc);
    }

    @Override
    public ModdingToolchainSpec getToolchains() {
        return toolchains;
    }

    @Override
    public void toolchains(Action<? super ModdingToolchainSpec> action) {
        action.execute(toolchains);
    }
}