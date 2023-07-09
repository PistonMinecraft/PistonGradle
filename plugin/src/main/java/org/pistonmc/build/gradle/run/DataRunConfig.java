package org.pistonmc.build.gradle.run;

import java.util.Locale;

public interface DataRunConfig extends RunConfig {
    enum IncludeType {
        /**
         * Include server generators
         */
        SERVER,
        /**
         * Include client generators
         */
        CLIENT,
        /**
         * Include development tools
         */
        DEV,
        /**
         * Include data reports
         */
        REPORTS,
        /**
         * Validate inputs
         */
        VALIDATE,
        /**
         * Include all generators
         */
        ALL
    }

    default void include(IncludeType type) {
        getGameArguments().add("--" + type.name().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Output folder. Defaults to "generated"
     * @param directory Path relative to the working directory
     */
    default void output(String directory) {
        getGameArguments().addAll("--output", directory);
    }

    /**
     * Input folder. Can specify multiple times
     * @param directory Path relative to the working directory
     */
    default void input(String directory) {
        getGameArguments().addAll("--input", directory);
    }
}