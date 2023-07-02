package org.pistonmc.build.gradle;

public interface Constants {
    String NAME = "piston_gradle";
    String PACKAGE_NAME = Constants.class.getPackageName();
    String CACHE_DIR = "caches/" + NAME;
    String GENERATED_REPO_DIR = NAME + "/generated_repo";
    String MERGED_MAPPING_FILE = NAME + "/merged_mapping.tiny";
    String TASK_GROUP = "piston gradle";
    String FORCE_UPDATE_VERSION_MANIFEST = PACKAGE_NAME + ".update.manifest.force";
    String MINECRAFT_EXTENSION = "minecraft";
    String FORGE_SOURCE_SET = "forge";
    String FABRIC_SOURCE_SET = "fabric";
}