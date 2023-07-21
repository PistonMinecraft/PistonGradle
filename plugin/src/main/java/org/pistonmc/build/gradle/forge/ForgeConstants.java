package org.pistonmc.build.gradle.forge;

import org.pistonmc.build.gradle.Constants;

public interface ForgeConstants {
    boolean DEBUG_PATCH_TASK = false;

    String AT_DEP = "net.minecraftforge:accesstransformers:8.0.+:fatjar";
    String SAS_DEP = "net.minecraftforge:mergetool:1.1.7:fatjar";

    String CONFIG_PATH = "config.json";

    String TASK_GROUP = Constants.TASK_GROUP + " forge specific";

    String EXTRACT_BASE_DIR = Constants.NAME + "/forge_extract";
    String SETUP_BASE_DIR = Constants.NAME + "/forge_setup";

    String SETUP_CONFIGURATION = "forgeSetup";
    String MC_CONFIGURATION = "forgeMinecraft";

    String SOURCE_SET = "forge";
}