package org.pistonmc.build.gradle.run.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.ServerRunConfig;

import javax.inject.Inject;

public abstract class ServerRunConfigImpl extends RunConfigImpl implements ServerRunConfig {
    @Inject
    public ServerRunConfigImpl(String name, TaskContainer tasks) {
        super(name, tasks);
    }
}