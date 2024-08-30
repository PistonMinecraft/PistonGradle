package org.pistonmc.build.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


class PistonGradlePluginTest {
    @Test
    void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("org.pistonmc.build");

        // Verify the result
        assertNotNull(project.getTasks().findByName(Constants.SETUP_VANILLA_DEV_ENV_TASK));
    }
}
