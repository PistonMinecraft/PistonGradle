package org.pistonmc.build.gradle.run.forge.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.forge.ForgeClient;

import javax.inject.Inject;

public abstract class ForgeClientImpl extends ForgeRunConfigImpl implements ForgeClient {
    @Inject
    public ForgeClientImpl(String name, TaskContainer tasks) {
        super(name, tasks);
        getClient().set(Boolean.TRUE);
        getUsername().convention("Dev").disallowUnsafeRead();
    }
}