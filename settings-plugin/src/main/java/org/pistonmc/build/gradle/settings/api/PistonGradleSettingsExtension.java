package org.pistonmc.build.gradle.settings.api;

import org.gradle.api.provider.Property;

public interface PistonGradleSettingsExtension {
    String EXTENSION_NAME = "pistonGradle";

    Property<EnvironmentMode> getEnvironmentMode();

    default void from(PistonGradleSettingsExtension ext) {
        getEnvironmentMode().convention(ext.getEnvironmentMode());
    }
}