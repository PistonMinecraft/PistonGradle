package org.pistonmc.build.gradle.extension.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Provider;
import org.pistonmc.build.gradle.extension.RunConfig;

import javax.inject.Inject;

public abstract class RunConfigImpl implements RunConfig {
    private final ObjectArrayList<String> vmArgs = new ObjectArrayList<>();
    private final ObjectArrayList<String> gameArgs = new ObjectArrayList<>();
    private final Provider<RunConfigImpl> defaultConfig;

    @Inject
    public RunConfigImpl(ProjectLayout layout, Provider<RunConfigImpl> defaultConfig) {
        this.defaultConfig = defaultConfig;
        getGameDir().convention(defaultConfig.flatMap(RunConfig::getGameDir).orElse(layout.getProjectDirectory().dir("run")));
    }

    @Override
    public void vmArg(String... arguments) {
        vmArgs.addElements(vmArgs.size(), arguments);
    }

    @Override
    public void gameArg(String... arguments) {
        gameArgs.addElements(gameArgs.size(), arguments);
    }

    public ObjectArrayList<String> getVmArgs() {
        var defaultConfig = this.defaultConfig.getOrNull();
        if (defaultConfig != null) {
            ObjectArrayList<String> args = new ObjectArrayList<>(defaultConfig.vmArgs);
            args.addAll(vmArgs);
            return args;
        }
        return vmArgs;
    }

    public ObjectArrayList<String> getGameArgs() {
        var defaultConfig = this.defaultConfig.getOrNull();
        if (defaultConfig != null) {
            ObjectArrayList<String> args = new ObjectArrayList<>(defaultConfig.gameArgs);
            args.addAll(gameArgs);
            return args;
        }
        return gameArgs;
    }
}