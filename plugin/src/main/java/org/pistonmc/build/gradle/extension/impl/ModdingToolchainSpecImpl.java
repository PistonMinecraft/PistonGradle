package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.extension.ToolchainConfig;

import javax.inject.Inject;

public abstract class ModdingToolchainSpecImpl implements ModdingToolchainSpec {
    private final ToolchainConfig defaultConfig;
    private final ToolchainConfig vanillaConfig;
    private final ToolchainConfig.Forge forgeConfig;
    private final ToolchainConfig.Fabric fabricConfig;

    @Inject
    public ModdingToolchainSpecImpl(ObjectFactory objects) {
        this.defaultConfig = objects.newInstance(ToolchainConfigImpl.class, (Object) null);
        this.vanillaConfig = objects.newInstance(ToolchainConfigImpl.class, defaultConfig);
        this.forgeConfig = objects.newInstance(ToolchainConfigImpl.Forge.class, defaultConfig);
        this.fabricConfig = objects.newInstance(ToolchainConfigImpl.Fabric.class, defaultConfig);
    }

    @Override
    public void config(Action<? super ToolchainConfig> configAction) {
        configAction.execute(defaultConfig);
    }

    @Override
    public void vanilla() {
        vanillaConfig.getEnabled().set(true);
    }

    @Override
    public void vanilla(Action<? super ToolchainConfig> configAction) {
        vanilla();
        configAction.execute(vanillaConfig);
    }

    @Override
    public void forge(Action<? super ToolchainConfig.Forge> configAction) {
        forgeConfig.getEnabled().set(true);
        configAction.execute(forgeConfig);
    }

    @Override
    public void fabric(Action<? super ToolchainConfig.Fabric> configAction) {
        fabricConfig.getEnabled().set(true);
        configAction.execute(fabricConfig);
    }

    @Override
    public ToolchainConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ToolchainConfig getVanillaConfig() {
        return vanillaConfig;
    }

    @Override
    public ToolchainConfig.Forge getForgeConfig() {
        return forgeConfig;
    }

    @Override
    public ToolchainConfig.Fabric getFabricConfig() {
        return fabricConfig;
    }
}