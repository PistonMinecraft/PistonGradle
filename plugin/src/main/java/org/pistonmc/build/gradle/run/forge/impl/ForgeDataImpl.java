package org.pistonmc.build.gradle.run.forge.impl;

import org.gradle.api.tasks.TaskContainer;
import org.pistonmc.build.gradle.run.forge.ForgeData;

import javax.inject.Inject;

public abstract class ForgeDataImpl extends ForgeRunConfigImpl implements ForgeData {
    @Inject
    public ForgeDataImpl(String name, TaskContainer tasks) {
        super(name, tasks);
    }
}