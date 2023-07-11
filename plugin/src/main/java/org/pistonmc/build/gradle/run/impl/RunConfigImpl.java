package org.pistonmc.build.gradle.run.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.Constants;
import org.pistonmc.build.gradle.run.ClientRunConfig;
import org.pistonmc.build.gradle.run.RunConfig;
import org.pistonmc.build.gradle.util.VariableUtil;
import org.pistonmc.build.gradle.util.version.Argument;
import org.pistonmc.build.gradle.util.version.Rule;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public abstract class RunConfigImpl implements RunConfig {
    private final String name;
    private final TaskProvider<JavaExec> runTask;

    @Inject
    protected abstract ProjectLayout getLayout();
    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    public RunConfigImpl(String name, TaskContainer tasks) {
        this.name = name;
        getParents().disallowUnsafeRead();
        getWorkingDirectory().disallowUnsafeRead();
        getMainClass().disallowUnsafeRead();
        getJvmArguments().disallowUnsafeRead();
        getConditionalJvmArguments().disallowUnsafeRead();
        getProperties().disallowUnsafeRead();
        getEnvironments().disallowUnsafeRead();
        getVariables().disallowUnsafeRead();
        getFeatures().disallowUnsafeRead();
        getWorkingDirectory().convention(getLayout().getProjectDirectory().dir("run"));
        this.runTask = tasks.register(RunConfig.super.getRunTaskName(), JavaExec.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            var clientConfig = this instanceof ClientRunConfig c ? c : null;
            var variables = getAllVariables();
            task.getJvmArguments().addAll(getAllJvmArguments().zip(variables, VariableUtil::replaceVariables));
            task.getJvmArguments().addAll(getAllConditionalJvmArguments().zip(variables, (args, v) -> {
                var ret = new ObjectArrayList<String>();
                for (Argument.Conditional arg : args) {
                    if (arg.rules().stream().allMatch(Rule::isAllow)) {
                        VariableUtil.replaceVariables(arg.value(), v, ret);
                    }
                }
                return ret;
            }));
            task.getArgumentProviders().add(getAllGameArguments().zip(variables, VariableUtil::replaceVariables)::get);
            task.getArgumentProviders().add(getAllConditionalGameArguments().zip(variables, (args, v) -> {
                var ret = new ObjectArrayList<String>();
                args: for (Argument.Conditional arg : args) {
                    for (Rule rule : arg.rules()) {
                        if (!rule.isAllow(clientConfig)) continue args;
                    }
                    VariableUtil.replaceVariables(arg.value(), v, ret);
                }
                return ret;
            })::get);
            task.systemProperties(getAllProperties().get());
            task.environment(getAllEnvironments().get());
            task.getMainClass().set(getAllMainClass());
            task.setWorkingDir(getWorkingDirectory().map(dir -> {
                dir.getAsFile().mkdirs();
                return dir;
            }));
        });
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public Provider<List<String>> getAllJvmArguments() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().listProperty(String.class);
            for (RunConfig config : configs) {
                merged.addAll(config.getAllJvmArguments());
            }
            merged.addAll(getJvmArguments());
            return merged;
        }).orElse(getJvmArguments());
    }

    @Override
    public Provider<List<Argument.Conditional>> getAllConditionalJvmArguments() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().listProperty(Argument.Conditional.class);
            for (RunConfig config : configs) {
                merged.addAll(config.getAllConditionalJvmArguments());
            }
            merged.addAll(getConditionalJvmArguments());
            return merged;
        }).orElse(getConditionalJvmArguments());
    }

    @Override
    public Provider<List<String>> getAllGameArguments() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().listProperty(String.class);
            for (RunConfig config : configs) {
                merged.addAll(config.getAllGameArguments());
            }
            merged.addAll(getGameArguments());
            return merged;
        }).orElse(getGameArguments());
    }

    @Override
    public Provider<List<Argument.Conditional>> getAllConditionalGameArguments() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().listProperty(Argument.Conditional.class);
            for (RunConfig config : configs) {
                merged.addAll(config.getAllConditionalGameArguments());
            }
            merged.addAll(getConditionalGameArguments());
            return merged;
        }).orElse(getConditionalGameArguments());
    }

    @Override
    public Provider<Map<String, String>> getAllProperties() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().mapProperty(String.class, String.class);
            for (RunConfig config : configs) {
                merged.putAll(config.getAllProperties());
            }
            merged.putAll(getProperties());
            return merged;
        }).orElse(getProperties());
    }

    @Override
    public Provider<Map<String, String>> getAllEnvironments() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().mapProperty(String.class, String.class);
            for (RunConfig config : configs) {
                merged.putAll(config.getAllEnvironments());
            }
            merged.putAll(getEnvironments());
            return merged;
        }).orElse(getEnvironments());
    }

    @Override
    public Provider<Map<String, String>> getAllVariables() {
        return getParents().flatMap(configs -> {
            var merged = getObjects().mapProperty(String.class, String.class);
            for (RunConfig config : configs) {
                merged.putAll(config.getAllVariables());
            }
            merged.putAll(getVariables());
            return merged;
        }).orElse(getVariables());
    }

    @Override
    public String getRunTaskName() {
        return runTask.getName();
    }

    @Override
    public TaskProvider<JavaExec> getRunTask() {
        return runTask;
    }
}