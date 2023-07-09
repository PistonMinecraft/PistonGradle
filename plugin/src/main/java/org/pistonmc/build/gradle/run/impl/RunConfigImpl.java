package org.pistonmc.build.gradle.run.impl;

import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.run.RunConfig;

import javax.inject.Inject;

public abstract class RunConfigImpl implements RunConfig {
    private final String name;

    @Inject
    public RunConfigImpl(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
}