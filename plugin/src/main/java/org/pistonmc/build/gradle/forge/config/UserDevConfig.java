package org.pistonmc.build.gradle.forge.config;

import cn.maxpixel.mcdecompiler.util.Utils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.pistonmc.build.gradle.PistonGradlePlugin;
import org.pistonmc.build.gradle.forge.ForgeConstants;
import org.pistonmc.build.gradle.forge.config.raw.UserDevConfigRaw;
import org.pistonmc.build.gradle.util.FileUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.pistonmc.build.gradle.util.CollectionUtil.nonNull;
import static org.pistonmc.build.gradle.util.Utils.forName;

public class UserDevConfig extends Config {
    public final MCPConfig mcp;
    public final List<Path> ats;
    public final List<Path> sass;
    public final Path binPatches;
    public final ExternalJarExec binPatcher;
    public final Path patches;
    public final Dependency sources;
    public final Dependency universal;
    public final List<Dependency> libraries;
    public final Map<String, RunConfig> runs;
    public final String patchesOriginalPrefix;
    public final String patchesModifiedPrefix;
    public final List<Dependency> modules;
    public final Charset sourceFileCharset;

    public UserDevConfig(MCPConfig mcp, List<Path> ats, List<Path> sass, Path binPatches, ExternalJarExec binPatcher,
                         Path patches, Dependency sources, Dependency universal, List<Dependency> libraries,
                         Map<String, RunConfig> runs, String patchesOriginalPrefix, String patchesModifiedPrefix, List<Dependency> modules,
                         Charset sourceFileCharset) {
        this.mcp = mcp;
        this.ats = ats;
        this.sass = sass;
        this.binPatches = binPatches;
        this.binPatcher = binPatcher;
        this.patches = patches;
        this.sources = sources;
        this.universal = universal;
        this.libraries = libraries;
        this.runs = runs;
        this.patchesOriginalPrefix = patchesOriginalPrefix;
        this.patchesModifiedPrefix = patchesModifiedPrefix;
        this.modules = modules;
        this.sourceFileCharset = sourceFileCharset;
    }

    public static UserDevConfig load(FileCollection userDevJar, Path extractBaseDir, Configuration forgeSetup, DependencyHandler dependencies) {
        Path path = userDevJar.getSingleFile().toPath();
        Path extract = extractBaseDir.resolve(path.getFileName().toString());
        FileUtil.extractZipTo(path, extract);
        DependencySet deps = forgeSetup.getDependencies();
        try (var isr = new InputStreamReader(Files.newInputStream(extract.resolve(ForgeConstants.CONFIG_PATH)), StandardCharsets.UTF_8)) {
            var raw = PistonGradlePlugin.GSON.fromJson(isr, UserDevConfigRaw.class);
            var mcpDep = dependencies.create(raw.mcp);
            deps.add(mcpDep);
            var mcp = MCPConfig.load(forgeSetup.copy().fileCollection(mcpDep), extractBaseDir, forgeSetup, dependencies);
            List<Path> ats = nonNull(raw.ats).stream().map(extract::resolve).toList();
            List<Path> sass = nonNull(raw.sass).stream().map(extract::resolve).toList();
            var sources = dependencies.create(raw.sources);
            deps.add(sources);
            var universal = dependencies.create(raw.universal);
            deps.add(universal);
            return new UserDevConfig(mcp, ats, sass, extract.resolve(raw.binpatches), ExternalJarExec.from(raw.binpatcher, dependencies, deps),
                    extract.resolve(raw.patches), sources, universal, nonNull(raw.libraries).stream().map(dependencies::create).toList(),
                    raw.runs, raw.patchesOriginalPrefix, raw.patchesModifiedPrefix, nonNull(raw.modules).stream().map(dependencies::create).toList(),
                    forName(raw.sourceFileCharset));
        } catch (IOException e) {
            throw Utils.wrapInRuntime(e);
        }
    }
}