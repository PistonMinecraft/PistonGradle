package org.pistonmc.build.gradle.run;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.*;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.pistonmc.build.gradle.util.version.Argument;

import java.util.List;
import java.util.Map;

public interface RunConfig extends Named {
    ListProperty<RunConfig> getParents();

    DirectoryProperty getWorkingDirectory();

    Property<String> getMainClass();

    ListProperty<String> getJvmArguments();

    ListProperty<Argument.Conditional> getConditionalJvmArguments();

    ListProperty<String> getGameArguments();

    ListProperty<Argument.Conditional> getConditionalGameArguments();

    MapProperty<String, String> getProperties();

    MapProperty<String, String> getEnvironments();

    MapProperty<String, String> getVariables();

    SetProperty<String> getFeatures();

    /**
     * Inspired by ForgeGradle
     * In the most cases you don't need to care about this
     */
    Property<Boolean> getClient();

    default Provider<String> getAllMainClass() {
        return getMainClass().orElse(getParents().flatMap(configs -> configs.stream().map(RunConfig::getAllMainClass)
                .reduce(Provider::orElse).orElse(null)));
    }

    Provider<List<String>> getAllJvmArguments();

    Provider<List<Argument.Conditional>> getAllConditionalJvmArguments();

    Provider<List<String>> getAllGameArguments();

    Provider<List<Argument.Conditional>> getAllConditionalGameArguments();

    Provider<Map<String, String>> getAllProperties();

    Provider<Map<String, String>> getAllEnvironments();

    Provider<Map<String, String>> getAllVariables();

    default String getRunTaskName() {
        return "run" + Character.toUpperCase(getName().charAt(0)) + getName().substring(1);
    }

    TaskProvider<JavaExec> getRunTask();
}