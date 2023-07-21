package org.pistonmc.build.gradle;

public interface Constants {
    String PISTON_GRADLE = "PistonGradle";
    String VERSION = "0.1-SNAPSHOT";
    String NAME = "piston_gradle";
    String PACKAGE_NAME = Constants.class.getPackageName();
    String GHP_USERNAME = "GHP_USERNAME";
    String GHP_TOKEN = "GHP_TOKEN";

    String CACHE_DIR = "caches/" + NAME;
    String GENERATED_REPO_DIR = NAME + "/generated_repo";

    /**
     * This configuration marks all dependencies from vanilla mc
     */
    String VANILLA_MC_CONFIGURATION = "vanillaMinecraft";

    String TASK_GROUP = "piston gradle";
    String SETUP_DEV_ENV_TASK = "setupDevEnv";
    String SETUP_VANILLA_DEV_ENV_TASK = "setupVanillaDevEnv";
    String PREPARE_ASSETS_TASK = "prepareAssets";
    String EXTRACT_NATIVES_TASK = "extractNatives";

    String FORCE_UPDATE_VERSION_MANIFEST = PACKAGE_NAME + ".update.manifest.force";
    String MINECRAFT_EXTENSION = "minecraft";
    String FABRIC_SOURCE_SET = "fabric";
}