package org.pistonmc.build.gradle.run.impl;

import org.pistonmc.build.gradle.run.DataRunConfig;

import javax.inject.Inject;

public abstract class DataRunConfigImpl extends RunConfigImpl implements DataRunConfig {
    @Inject
    public DataRunConfigImpl(String name) {
        super(name);
    }
}
