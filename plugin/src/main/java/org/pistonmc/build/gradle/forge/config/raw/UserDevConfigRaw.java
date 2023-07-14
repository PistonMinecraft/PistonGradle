package org.pistonmc.build.gradle.forge.config.raw;

import org.pistonmc.build.gradle.forge.config.Config;
import org.pistonmc.build.gradle.forge.config.RunConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDevConfigRaw extends Config {
    // V1
//    @Nullable
    public String mcp;    // Do not specify this unless there is no parent.
//    @Nullable TODO: We don't support this currently
//    public String parent; // To fully resolve, we must walk the parents until we hit null, and that one must specify a MCP value.
    @Nullable
    public List<String> ats;
    @Nullable
    public List<String> sass;
//    @Nullable TODO: We don't support this currently
//    public List<String> srgs;
//    @Nullable TODO: We don't support this currently
//    public List<String> srg_lines;
    public String binpatches; //To be applied to joined.jar, remapped, and added to the classpath
    public MCPConfigRaw.Function binpatcher;
    public String patches;
    @Nullable
    public String sources;
    @Nullable
    public String universal; //Remapped and added to the classpath, Contains new classes and resources
    @Nullable
    public List<String> libraries; //Additional libraries.
//    @Nullable TODO: We don't support this currently
//    public String inject;
    @Nullable
    public Map<String, RunConfig> runs;
//    @Nullable TODO: We don't support this currently
//    public String sourceCompatibility;
//    @Nullable TODO: We don't support this currently
//    public String targetCompatibility;

    // V2
//    public UserDevConfig.DataFunction processor; TODO: We don't support this currently
    public String patchesOriginalPrefix;
    public String patchesModifiedPrefix;
//    @Nullable TODO: We don't support this currently
//    public Boolean notchObf; //This is a Boolean so we can set to null and it won't be printed in the json.
//    @Nullable TODO: We don't support this currently
//    public List<String> universalFilters;
    @Nullable
    public List<String> modules; // Modules passed to --module-path
    @Nullable
    public String sourceFileCharset;

    public static class DataFunction extends MCPConfigRaw.Function {
        protected Map<String, String> data;

        public Map<String, String> getData() {
            return this.data == null ? Collections.emptyMap() : data;
        }

        @Nullable
        public String setData(String name, String path) {
            if (this.data == null)
                this.data = new HashMap<>();
            return this.data.put(name, path);
        }
    }
}
