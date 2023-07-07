package org.pistonmc.build.gradle.extension.impl;

import cn.maxpixel.mcdecompiler.mapping.type.MappingTypes;
import org.gradle.api.Action;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ModdingToolchainSpec;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import javax.inject.Inject;

public abstract class MinecraftExtensionImpl implements MinecraftExtension {
    private final VanillaMinecraftCache vmc;
    private final ModdingToolchainSpec toolchains;
    private boolean demoUser;
    private boolean customResolution;
    private int width;
    private int height;

    @Inject
    public abstract ObjectFactory getObjects();
    @Inject
    public abstract ProjectLayout getLayout();

    @Inject
    public MinecraftExtensionImpl(VanillaMinecraftCache vmc) {
        this.vmc = vmc;
        this.toolchains = getObjects().newInstance(ModdingToolchainSpecImpl.class);
    }

    @Override
    public void asDemoUser() {
        this.demoUser = true;
    }

    public boolean isDemoUser() {
        return demoUser;
    }

    @Override
    public void withCustomResolution(int width, int height) {
        this.customResolution = true;
        this.width = width;
        this.height = height;
    }

    public boolean isCustomResolution() {
        return customResolution;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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