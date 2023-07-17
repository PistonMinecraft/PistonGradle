package org.pistonmc.build.gradle.forge.config.raw;

import org.pistonmc.build.gradle.forge.config.Config;
import org.pistonmc.build.gradle.forge.config.MCPConfig;
import org.pistonmc.build.gradle.forge.config.Side;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class MCPConfigRaw extends Config {
    // V1
    public String version; // Minecraft version
    @Nullable
    public Map<String, Object> data;
    @Nullable
    public Map<Side, List<MCPConfig.Step>> steps;
    @Nullable
    public Map<String, Function> functions;
    @Nullable
    public Map<String, List<String>> libraries;

    // V2
    public boolean official = false;
    public int java_target = 8;
    @Nullable
    public String encoding;


    public static class Function {
        public String version; //Maven artifact for the jar to run
//        @Nullable TODO: We don't support this currently
//        public String repo; //Maven repo to download the jar from
        @Nullable
        public List<String> args;
        @Nullable
        public List<String> jvmargs;
        @Nullable
        public Integer java_version;
    }
}