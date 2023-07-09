package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.extension.ToolchainConfig;

import javax.inject.Inject;

public abstract class ToolchainConfigImpl implements ToolchainConfig {
    private final Provider<ToolchainConfigImpl> defaultConfig;

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public ToolchainConfigImpl(Provider<ToolchainConfigImpl> defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    @Override
    public void mixin() {
        throw new UnsupportedOperationException("TODO");//TODO
    }

    public abstract static class ForgeImpl extends ToolchainConfigImpl implements ToolchainConfig.Forge {
        @Inject
        public ForgeImpl(Provider<ToolchainConfigImpl> defaultConfig) {
            super(defaultConfig);
        }
    }

    public abstract static class FabricImpl extends ToolchainConfigImpl implements ToolchainConfig.Fabric {
        @Inject
        public FabricImpl(Provider<ToolchainConfigImpl> defaultConfig) {
            super(defaultConfig);
        }
    }
}
