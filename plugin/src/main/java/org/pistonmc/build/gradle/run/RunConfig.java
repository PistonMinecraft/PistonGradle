package org.pistonmc.build.gradle.run;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.*;
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

    default Provider<String> getAllMainClass() {
        var parents = getParents().get();
        if (parents.isEmpty() || getMainClass().isPresent()) return getMainClass();
        return parents.stream().map(RunConfig::getAllMainClass).reduce((l, r) -> l.zip(r, (a, b) -> a == null ? b : a)).get();
    }

    default Provider<List<String>> getAllJvmArguments() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getJvmArguments();
        return parents.stream().map(RunConfig::getAllJvmArguments).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new ObjectArrayList<>(a);
            ret.addAll(b);
            return ret;
        })).get().zip(getJvmArguments(), (l, r) -> {
            var ret = new ObjectArrayList<>(l);
            ret.addAll(r);
            return ret;
        });
    }

    default Provider<List<Argument.Conditional>> getAllConditionalJvmArguments() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getConditionalJvmArguments();
        return parents.stream().map(RunConfig::getAllConditionalJvmArguments).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new ObjectArrayList<>(a);
            ret.addAll(b);
            return ret;
        })).get().zip(getConditionalJvmArguments(), (l, r) -> {
            var ret = new ObjectArrayList<>(l);
            ret.addAll(r);
            return ret;
        });
    }

    default Provider<List<String>> getAllGameArguments() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getGameArguments();
        return parents.stream().map(RunConfig::getAllGameArguments).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new ObjectArrayList<>(a);
            ret.addAll(b);
            return ret;
        })).get().zip(getGameArguments(), (l, r) -> {
            var ret = new ObjectArrayList<>(l);
            ret.addAll(r);
            return ret;
        });
    }

    default Provider<List<Argument.Conditional>> getAllConditionalGameArguments() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getConditionalGameArguments();
        return parents.stream().map(RunConfig::getAllConditionalGameArguments).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new ObjectArrayList<>(a);
            ret.addAll(b);
            return ret;
        })).get().zip(getConditionalGameArguments(), (l, r) -> {
            var ret = new ObjectArrayList<>(l);
            ret.addAll(r);
            return ret;
        });
    }

    default Provider<Map<String, String>> getAllProperties() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getProperties();
        return parents.stream().map(RunConfig::getAllProperties).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new Object2ObjectOpenHashMap<>(a);
            ret.putAll(b);
            return ret;
        })).get().zip(getProperties(), (l, r) -> {
            var ret = new Object2ObjectOpenHashMap<>(l);
            ret.putAll(r);
            return ret;
        });
    }

    default Provider<Map<String, String>> getAllEnvironments() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getEnvironments();
        return parents.stream().map(RunConfig::getAllEnvironments).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new Object2ObjectOpenHashMap<>(a);
            ret.putAll(b);
            return ret;
        })).get().zip(getEnvironments(), (l, r) -> {
            var ret = new Object2ObjectOpenHashMap<>(l);
            ret.putAll(r);
            return ret;
        });
    }

    default Provider<Map<String, String>> getAllVariables() {
        var parents = getParents().get();
        if (parents.isEmpty()) return getVariables();
        return parents.stream().map(RunConfig::getAllVariables).reduce((l, r) -> l.zip(r, (a, b) -> {
            var ret = new Object2ObjectOpenHashMap<>(a);
            ret.putAll(b);
            return ret;
        })).get().zip(getVariables(), (l, r) -> {
            var ret = new Object2ObjectOpenHashMap<>(l);
            ret.putAll(r);
            return ret;
        });
    }
}