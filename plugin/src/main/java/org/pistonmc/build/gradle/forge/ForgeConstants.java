package org.pistonmc.build.gradle.forge;

import org.pistonmc.build.gradle.Constants;

public interface ForgeConstants {
    String CONFIG_PATH = "config.json";

    String TASK_GROUP = Constants.TASK_GROUP + " forge specific";

    String EXTRACT_BASE_DIR = Constants.NAME + "/forge_extract";
}