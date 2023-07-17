package org.pistonmc.build.gradle.extension.impl;

import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.mapping.MappingConfig;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.run.DataRunConfig;
import org.pistonmc.build.gradle.run.RunConfig;
import org.pistonmc.build.gradle.run.ServerRunConfig;
import org.pistonmc.build.gradle.run.impl.ClientRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.DataRunConfigImpl;
import org.pistonmc.build.gradle.run.impl.RunConfigImpl;
import org.pistonmc.build.gradle.run.impl.ServerRunConfigImpl;

import javax.inject.Inject;
import java.util.List;

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
        getVersion().finalizeValueOnRead();
        getMapping().finalizeValueOnRead();
        var runs = getRuns();
        runs.registerBinding(RunConfig.class, RunConfigImpl.class);
        runs.registerBinding(ClientRunConfig.class, ClientRunConfigImpl.class);
        runs.registerBinding(DataRunConfig.class, DataRunConfigImpl.class);
        runs.registerBinding(ServerRunConfig.class, ServerRunConfigImpl.class);
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

    @Override
    public Provider<List<RegularFile>> getAllAccessTransformers() {
        return null;
    }

    @Override
    public Provider<List<RegularFile>> getAllAccessWideners() {
        return null;
    }
}