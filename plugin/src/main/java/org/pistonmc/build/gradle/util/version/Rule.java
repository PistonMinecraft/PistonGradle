package org.pistonmc.build.gradle.util.version;

import org.jetbrains.annotations.Nullable;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.util.OSName;

import java.util.Map;
import java.util.regex.Pattern;

public record Rule(Action action, OS os, Map<String, Boolean> features) {
    public enum Action {
        ALLOW, DISALLOW
    }

    public record OS(String name, String arch, String version) {
    }

    public boolean isAllow() {
        return isAllow(null);
    }

    public boolean isAllow(@Nullable ClientRunConfig config) {
        boolean ret = true;
        if (os != null) {// FIXME: Is it REALLY determined like this?
            ret &= os.name == null || OSName.getCurrent().toString().equals(os.name);
            ret &= os.arch == null || Pattern.compile(os.arch).matcher(System.getProperty("os.arch")).find();
            ret &= os.version == null || Pattern.compile(os.version).matcher(System.getProperty("os.version")).find();
        }
        if (features != null) {
            if (config == null) {
                ret = false;
            } else {
                var featureSet = config.getFeatures().get();
                for (Map.Entry<String, Boolean> entry : features.entrySet()) {
                    ret &= featureSet.contains(entry.getKey()) == entry.getValue();
                }
            }
        }
        return (action == Action.ALLOW) == ret;
    }
}
