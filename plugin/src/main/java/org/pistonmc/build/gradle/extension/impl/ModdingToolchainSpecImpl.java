package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.extension.ToolchainConfig;

import javax.inject.Inject;

public abstract class ModdingToolchainSpecImpl implements ModdingToolchainSpec {
    public abstract Property<ToolchainConfig> getDefaultConfig();
    public abstract Property<ToolchainConfig> getVanillaConfig();
    public abstract Property<ToolchainConfig.Forge> getForgeConfig();
    public abstract Property<ToolchainConfig.Fabric> getFabricConfig();

    @Inject
    public abstract ObjectFactory getObjects();
    @Inject
    public abstract ProviderFactory getProviders();

    @Inject
    public ModdingToolchainSpecImpl() {
        var factory = getObjects();
        var defaultConfig = getDefaultConfig();
        defaultConfig.convention(factory.newInstance(ToolchainConfigImpl.class, getProviders().provider(() -> null)));
    }

    @Override
    public void config(Action<? super ToolchainConfig> configAction) {
        var config = getObjects().newInstance(ToolchainConfigImpl.class, (Object) null);
        configAction.execute(config);
        getDefaultConfig().set(config);
    }

    @Override
    public void vanilla() {
        getVanillaConfig().set(getDefaultConfig());
    }

    @Override
    public void vanilla(Action<? super ToolchainConfig> configAction) {
        var config = getObjects().newInstance(ToolchainConfigImpl.class, getDefaultConfig());
        configAction.execute(config);
        getVanillaConfig().set(config);
    }

    @Override
    public void forge(Action<? super ToolchainConfig.Forge> configAction) {
        var config = getObjects().newInstance(ToolchainConfigImpl.Forge.class, getDefaultConfig());
        configAction.execute(config);
        getForgeConfig().set(config);
    }

    @Override
    public void fabric(Action<? super ToolchainConfig.Fabric> configAction) {
        var config = getObjects().newInstance(ToolchainConfigImpl.Fabric.class, getDefaultConfig());
        configAction.execute(config);
        getFabricConfig().set(config);
    }
}