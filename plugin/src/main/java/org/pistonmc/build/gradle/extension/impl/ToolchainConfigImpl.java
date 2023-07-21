package org.pistonmc.build.gradle.extension.impl;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.extension.MinecraftExtension;
import org.pistonmc.build.gradle.extension.ToolchainConfig;
import org.pistonmc.build.gradle.mapping.MappingConfig;

import javax.inject.Inject;
import java.util.Optional;

public abstract class ToolchainConfigImpl implements ToolchainConfig {
    protected final MinecraftExtension extension;
    private final ToolchainConfigImpl defaultConfig;

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public ToolchainConfigImpl(MinecraftExtension extension, Optional<ToolchainConfigImpl> defaultConfig) {
        this(extension, defaultConfig.orElse(null));
    }

    public ToolchainConfigImpl(MinecraftExtension extension, ToolchainConfigImpl defaultConfig) {
        this.extension = extension;
        this.defaultConfig = defaultConfig;
        if (defaultConfig == null) {
            getEnabled().convention(false);
        } else {
            getEnabled().convention(defaultConfig.getEnabled());
        }
        getEnabled().finalizeValueOnRead();
    }

    @Override
    public void mixin() {
        throw new UnsupportedOperationException("TODO");//TODO
    }

    public abstract static class ForgeImpl extends ToolchainConfigImpl implements ToolchainConfig.Forge {
        private final Provider<String> mappingName;

        @Inject
        public ForgeImpl(MinecraftExtension extension, ToolchainConfigImpl defaultConfig) {
            super(extension, defaultConfig);
            this.mappingName = extension.getMappings().flatMap(MappingConfig::getMappingName);
            getVersion().finalizeValueOnRead();
        }

        @Override
        public Provider<String> getArtifactVersion() {
            return extension.getVersion().zip(getVersion(), (mc, forge) -> mc + '-' + forge);
        }

        @Override
        public Provider<String> getGeneratedArtifactVersion() {
            return getArtifactVersion().zip(mappingName, (a, m) -> a + "_mapped_" + m);
        }
    }

    public abstract static class FabricImpl extends ToolchainConfigImpl implements ToolchainConfig.Fabric {
        @Inject
        public FabricImpl(MinecraftExtension extension, ToolchainConfigImpl defaultConfig) {
            super(extension, defaultConfig);
            getLoaderVersion().finalizeValueOnRead();
            getApiVersion().finalizeValueOnRead();
        }
    }
}
