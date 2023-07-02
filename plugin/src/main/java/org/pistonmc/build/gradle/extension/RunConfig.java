package org.pistonmc.build.gradle.extension;

import org.gradle.api.file.DirectoryProperty;

public interface RunConfig {
    DirectoryProperty getGameDir();

    void vmArg(String... arguments);

    void gameArg(String... arguments);
}