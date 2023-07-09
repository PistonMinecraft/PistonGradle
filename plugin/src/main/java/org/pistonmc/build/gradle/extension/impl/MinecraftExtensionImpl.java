package org.pistonmc.build.gradle.extension.impl;

import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.run.DataRunConfig;
import org.pistonmc.build.gradle.run.ServerRunConfig;
import org.pistonmc.build.gradle.run.impl.ClientRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.DataRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.ServerRunConfigImpl;

import javax.inject.Inject;

public abstract class MinecraftExtensionImpl implements MinecraftExtension {
    private final VanillaMinecraftCache vmc;
    private final ModdingToolchainSpec toolchains;

    @Inject
    public abstract ObjectFactory getObjects();
    @Inject
    public abstract ProjectLayout getLayout();

    @Inject
    public MinecraftExtensionImpl(VanillaMinecraftCache vmc) {
        this.vmc = vmc;
        this.toolchains = getObjects().newInstance(ModdingToolchainSpecImpl.class);
        getRuns().registerFactory(ClientRunConfig.class, name -> getObjects().newInstance(ClientRunConfigImpl.class, name));
        getRuns().registerFactory(DataRunConfig.class, name -> getObjects().newInstance(DataRunConfigImpl.class, name));
        getRuns().registerFactory(ServerRunConfig.class, name -> getObjects().newInstance(ServerRunConfigImpl.class, name));
    }

    @Override
    public MappingConfig official() {
        var m = getObjects().newInstance(MappingConfig.class);
        m.getType().set(MappingTypes.PROGUARD);
        m.getMappings().set(getLayout().file(getVersion().map(vmc::getClientMappingsFile)));
        return m;
    }

//    @Override
//    public MappingConfig parchment() {
//        return null;
//    }

    @Override
    public ModdingToolchainSpec getToolchains() {
        return toolchains;
    }

    @Override
    public void toolchains(Action<? super ModdingToolchainSpec> action) {
        action.execute(toolchains);
    }
}