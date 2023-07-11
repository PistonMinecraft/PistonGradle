package org.pistonmc.build.gradle.run.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.ClientRunConfig;

import javax.inject.Inject;

public abstract class ClientRunConfigImpl extends RunConfigImpl implements ClientRunConfig {
    @Inject
    public ClientRunConfigImpl(String name, TaskContainer tasks) {
        super(name, tasks);
        getUsername().disallowUnsafeRead();
        getUsername().convention("Dev");
    }
}