package org.pistonmc.build.gradle.extension.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.extension.ToolchainConfig;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import javax.inject.Inject;
import java.util.Objects;

public abstract class ModdingToolchainSpecImpl implements ModdingToolchainSpec {
    private final MinecraftExtension ext;
    private final VanillaMinecraftCache vmc;

    public abstract Property<ToolchainConfig> getDefaultConfig();
    public abstract Property<ToolchainConfig> getVanillaConfig();
    public abstract Property<ToolchainConfig.Forge> getForgeConfig();
    public abstract Property<ToolchainConfig.Fabric> getFabricConfig();
    public abstract ListProperty<MappingConfig> getMappings();

    @Inject
    public abstract ObjectFactory getObjects();
    @Inject
    public abstract ProviderFactory getProviders();
    @Inject
    public abstract ProjectLayout getLayout();

    @Inject
    public ModdingToolchainSpecImpl(MinecraftExtension ext, VanillaMinecraftCache vmc) {
        this.ext = ext;
        this.vmc = vmc;
        var factory = getObjects();
        var defaultConfig = getDefaultConfig();
        defaultConfig.convention(factory.newInstance(ToolchainConfigImpl.class, getProviders().provider(() -> null)));
    }

    @Override
    public void mapping(MappingConfig mapping) {
        getMappings().add(Objects.requireNonNull(mapping));
    }

    @Override
    public MappingConfig official() {
        var m = getObjects().newInstance(MappingConfig.class);
        m.getMappings().set(getLayout().file(ext.getVersion().map(vmc::getClientMappingsFile)));
        return m;
    }

//    @Override
//    public MappingConfig parchment() {
//        return null;
//    }

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