package org.pistonmc.build.gradle.forge.config;

import java.util.List;
import java.util.Map;

// TODO: We don't support other properties currently
public record RunConfig(String name, String main, List<String> parents, List<String> args, List<String> jvmArgs,
                        boolean client, boolean buildAllProjects, Map<String, String> env, Map<String, String> props) {
}