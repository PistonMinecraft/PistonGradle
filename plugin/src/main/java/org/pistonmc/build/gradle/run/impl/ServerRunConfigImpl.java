package org.pistonmc.build.gradle.run.impl;

import org.pistonmc.build.gradle.run.ServerRunConfig;

import javax.inject.Inject;

public abstract class ServerRunConfigImpl extends RunConfigImpl implements ServerRunConfig {
    @Inject
    public ServerRunConfigImpl(String name) {
        super(name);
    }
}