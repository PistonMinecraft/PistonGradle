package org.pistonmc.build.gradle.settings.api;

public enum EnvironmentMode {
    /**
     * Allows only one modding environment per project
     */
    SINGLE,
    /**
     * Allows multiple modding environments to be set up in one project by taking the advantage of source sets
     */
    MULTIPLE
}
