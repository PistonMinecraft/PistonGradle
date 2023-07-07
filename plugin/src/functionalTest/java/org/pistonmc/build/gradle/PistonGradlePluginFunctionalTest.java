package org.pistonmc.build.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * A simple functional test for the 'org.pistonmc.build.gradle.greeting' plugin.
 */
class PistonGradlePluginFunctionalTest {
    @TempDir
    File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    @Test void canRunTask() throws IOException {
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(), """
                plugins {
                  id('org.pistonmc.build')
                }
                
                minecraft {
                  version = '1.20.1'
                  mapping = official()
                  toolchains {
                    vanilla()
                  }
                }
                """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments(Constants.SETUP_DEV_ENV_TASK, "dependencies", "--stacktrace", "--info");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        System.out.println(result.getOutput());
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
