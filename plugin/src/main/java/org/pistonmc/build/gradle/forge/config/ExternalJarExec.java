package org.pistonmc.build.gradle.forge.config;

import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.pistonmc.build.gradle.forge.config.raw.MCPConfigRaw;
import org.pistonmc.build.gradle.util.Utils;

import java.util.List;

import static org.pistonmc.build.gradle.util.CollectionUtil.nonNull;

public record ExternalJarExec(Dependency jar, List<String> args, List<String> jvmArgs, JavaLanguageVersion javaVersion) {
    public static ExternalJarExec from(MCPConfigRaw.Function function, DependencyHandler dependencies, DependencySet deps) {
        return from(function, dependencies, deps, JavaLanguageVersion.of(JavaVersion.current().ordinal() + 1));
    }

    public static ExternalJarExec from(MCPConfigRaw.Function function, DependencyHandler dependencies, DependencySet deps, JavaLanguageVersion defaultJava) {
        var dep = dependencies.create(function.version);
        deps.add(dep);
        return new ExternalJarExec(dep, nonNull(function.args), nonNull(function.jvmargs), Utils.of(function.java_version, defaultJava));
    }
}