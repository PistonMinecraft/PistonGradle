package org.pistonmc.build.gradle.run.forge.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.forge.ForgeServer;

import javax.inject.Inject;

public abstract class ForgeServerImpl extends ForgeRunConfigImpl implements ForgeServer {
    @Inject
    public ForgeServerImpl(String name, TaskContainer tasks) {
        super(name, tasks);
    }
}