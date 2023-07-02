package org.pistonmc.build.gradle.mapping;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

public interface MergeResult {
    @InputFile
    RegularFileProperty getInputMappings();

    @Input
    Provider<String> getTargetNamespace();
}