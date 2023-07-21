package org.pistonmc.build.gradle.run.forge.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.forge.ForgeGameTestServer;

import javax.inject.Inject;

public abstract class ForgeGameTestServerImpl extends ForgeRunConfigImpl implements ForgeGameTestServer {
    @Inject
    public ForgeGameTestServerImpl(String name, TaskContainer tasks) {
        super(name, tasks);
    }
}
