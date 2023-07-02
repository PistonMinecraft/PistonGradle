package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.extension.RunConfig;
import org.pistonmc.build.gradle.extension.ToolchainConfig;

import javax.inject.Inject;

public abstract class ToolchainConfigImpl implements ToolchainConfig {
    private final Provider<ToolchainConfigImpl> defaultConfig;

    public abstract Property<RunConfigImpl> getClientRun();
    public abstract Property<RunConfigImpl> getServerRun();
    public abstract Property<RunConfigImpl> getDataRun();

    @Inject
    public abstract ObjectFactory getObjectFactory();

    @Inject
    public ToolchainConfigImpl(Provider<ToolchainConfigImpl> defaultConfig) {
        this.defaultConfig = defaultConfig;
        getClientRun().convention(newRunConfig(ToolchainConfigImpl::getClientRun));
        getServerRun().convention(newRunConfig(ToolchainConfigImpl::getServerRun));
        getDataRun().convention(newRunConfig(ToolchainConfigImpl::getDataRun));
    }

    private RunConfigImpl newRunConfig(Transformer<Provider<RunConfigImpl>, ToolchainConfigImpl> transformer) {
        return getObjectFactory().newInstance(RunConfigImpl.class, defaultConfig.map(transformer));
    }

    private void newRunConfig(Transformer<Provider<RunConfigImpl>, ToolchainConfigImpl> transformer, Action<? super RunConfig> runAction, Property<RunConfigImpl> property) {
        var run = newRunConfig(transformer);
        runAction.execute(run);
        property.set(run);
    }

    @Override
    public void client(Action<? super RunConfig> runAction) {
        newRunConfig(ToolchainConfigImpl::getClientRun, runAction, getClientRun());
    }

    @Override
    public void server(Action<? super RunConfig> runAction) {
        newRunConfig(ToolchainConfigImpl::getServerRun, runAction, getServerRun());
    }

    @Override
    public void data(Action<? super RunConfig> runAction) {
        newRunConfig(ToolchainConfigImpl::getDataRun, runAction, getDataRun());
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
