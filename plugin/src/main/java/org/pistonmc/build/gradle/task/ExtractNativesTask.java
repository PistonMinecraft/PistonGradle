package org.pistonmc.build.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;

public abstract class ExtractNativesTask extends DefaultTask {// TODO
    @OutputDirectory
    public abstract DirectoryProperty getExtract();
}