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
                  id 'org.pistonmc.build'
                }
                
                import org.pistonmc.build.gradle.run.ClientRunConfig
                import org.pistonmc.build.gradle.run.ServerRunConfig
                import org.pistonmc.build.gradle.run.DataRunConfig
                
                minecraft {
                  version = '1.20.1'
                  mapping = official()
                  toolchains {
                    //vanilla()
                    forge {
                      version = '47.1.28'
                    }
                  }
                  runs {
                    random
                    vanillaClient(ClientRunConfig)
                    vanillaServer(ServerRunConfig)
                    vanillaData(DataRunConfig)
                  }
                }
                """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments(/*"dependencies", */Constants.SETUP_DEV_ENV_TASK, "--stacktrace", "--info");
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
