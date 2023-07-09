package org.pistonmc.build.gradle.run;

public interface ClientRunConfig extends RunConfig {
    default void asDemoUser() {
        getFeatures().add("is_demo_user");
    }

    default void withCustomResolution(int width, int height) {
        getFeatures().add("has_custom_resolution");
        getVariables().put("resolution_width", String.valueOf(width));
        getVariables().put("resolution_height", String.valueOf(height));
    }
}