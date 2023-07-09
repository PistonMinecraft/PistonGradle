package org.pistonmc.build.gradle.run;

import org.gradle.api.file.RegularFile;
import org.pistonmc.build.gradle.annotations.AvailableSince;

public interface ServerRunConfig extends RunConfig {
    default void nogui() {
        getGameArguments().add("--nogui");
    }

    /**
     * Initializes 'server.properties' and 'eula.txt', then quits
     */
    default void initSettings() {
        getGameArguments().add("--initSettings");
    }

    default void demo() {
        getGameArguments().add("--demo");
    }

    default void bonusChest() {
        getGameArguments().add("--bonusChest");
    }

    default void forceUpgrade() {
        getGameArguments().add("--forceUpgrade");
    }

    /**
     * This should be used with {@link #forceUpgrade()}. Otherwise, this is ignored
     */
    default void eraseCache() {
        getGameArguments().add("--eraseCache");
    }

    default void singleplayer(String playerName) {
        getGameArguments().addAll("--singleplayer", playerName);
    }

    /**
     * Set the path where the server store all its data except for backups
     * <p>
     * Backups are stored at {universe}/../backups
     * <p>
     * Default value is the working directory
     * @param path Path relative to the working directory
     */
    default void universe(String path) {
        getGameArguments().addAll("--universe", path);
    }

    default void world(String worldName) {
        getGameArguments().addAll("--world", worldName);
    }

    default void port(int port) {
        getGameArguments().addAll("--port", String.valueOf(port));
    }

    default void serverId(String id) {
        getGameArguments().addAll("--serverId", id);
    }

    /**
     * Loads level with vanilla datapack only
     */
    @AvailableSince("20w22a")
    default void safeMode() {
        getGameArguments().add("--safeMode");
    }

    @AvailableSince("21w37a")
    default void jfrProfile() {
        getGameArguments().add("--jfrProfile");
    }

    @AvailableSince("23w06a")
    default void pidFile(RegularFile file) {
        getGameArguments().addAll("--pidFile", file.getAsFile().getAbsolutePath());
    }
}