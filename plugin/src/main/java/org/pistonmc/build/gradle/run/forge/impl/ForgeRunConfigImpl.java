package org.pistonmc.build.gradle.run.forge.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.forge.ForgeRunConfig;
import org.pistonmc.build.gradle.run.impl.RunConfigImpl;

import javax.inject.Inject;

public abstract class ForgeRunConfigImpl extends RunConfigImpl implements ForgeRunConfig {
    @Inject
    public ForgeRunConfigImpl(String name, TaskContainer tasks) {
        super(name, tasks);
        getVariableDollarBegin().set(Boolean.FALSE);
    }
}