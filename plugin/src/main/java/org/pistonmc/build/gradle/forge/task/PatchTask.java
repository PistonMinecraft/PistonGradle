package org.pistonmc.build.gradle.forge.task;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.JarUtil;
import cn.maxpixel.mcdecompiler.util.LambdaUtil;
import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import codechicken.diffpatch.util.archiver.ArchiveFormat;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.pistonmc.build.gradle.forge.ForgeConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PatchTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputJar();
    @InputDirectory
    public abstract DirectoryProperty getPatchDir();
    @OutputFile
    public abstract RegularFileProperty getOutputJar();
    @Input
    public abstract Property<String> getOriginalPrefix();
    @Input
    public abstract Property<String> getModifiedPrefix();
    @InputFiles
    public abstract Property<FileCollection> getSourcesJar();

    public PatchTask() {
        getInputJar().disallowUnsafeRead();
        getPatchDir().disallowUnsafeRead();
        getOutputJar().disallowUnsafeRead();
        getOriginalPrefix().disallowUnsafeRead();
        getModifiedPrefix().disallowUnsafeRead();
        getSourcesJar().disallowUnsafeRead();
    }

    @TaskAction
    public void patch() throws IOException {
        getLogger().debug("Applying patches...");
        Path output = getOutputJar().get().getAsFile().toPath();
        PatchOperation.Builder builder = PatchOperation.builder()
                .logTo(new LoggingOutputStream(getLogger(), LogLevel.LIFECYCLE))
                .basePath(getInputJar().get().getAsFile().toPath(), ArchiveFormat.ZIP)
                .patchesPath(getPatchDir().get().getAsFile().toPath())
                .outputPath(output, ArchiveFormat.ZIP)
                .mode(PatchMode.ACCESS)
                .verbose(ForgeConstants.DEBUG_PATCH_TASK)
                .summary(ForgeConstants.DEBUG_PATCH_TASK);
        if (getOriginalPrefix().isPresent()) {
            builder.aPrefix(getOriginalPrefix().get());
        }
        if (getModifiedPrefix().isPresent()) {
            builder.bPrefix(getModifiedPrefix().get());
        }
        CliOperation.Result<PatchOperation.PatchesSummary> result = builder.build().operate();
        if (result.exit != 0) throw new RuntimeException("Failed to apply patches. See log for details");
        getLogger().debug("Patches applied");
        if (getSourcesJar().isPresent()) {
            getLogger().debug("Injecting extra sources...");
            try (var sources = JarUtil.createZipFs(getSourcesJar().get().getSingleFile().toPath());
                 var patched = JarUtil.createZipFs(output);
                 var files = FileUtil.iterateFiles(sources.getPath(""))) {
                files.filter(p -> !p.toString().startsWith("patches/")).forEach(LambdaUtil.unwrapConsumer(p -> {
                    Path patchedPath = patched.getPath(p.toString());
                    if (Files.notExists(patchedPath)) {
                        try (var is = Files.newInputStream(p);
                             var os = Files.newOutputStream(FileUtil.ensureFileExist(patchedPath))) {
                            is.transferTo(os);
                        }
                    }
                }));
            }
            getLogger().debug("Injected sources");
        }
    }
}