package org.pistonmc.build.gradle.util;

import cn.maxpixel.mcdecompiler.util.JarUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static cn.maxpixel.mcdecompiler.util.FileUtil.*;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class FileUtil {
    private static final Logger LOGGER = Logging.getLogger(FileUtil.class);

    public static void copyDirectory(@NotNull Path source, @NotNull Path target) {
        if (Files.notExists(source)) {
            LOGGER.debug("Source \"{}\" does not exist, skipping this operation...", source);
            return;
        }
        if (!Files.isDirectory(source)) throw new IllegalArgumentException("Source isn't a directory");
        Path p = source.toAbsolutePath().normalize();
        try (Stream<Path> sourceStream = iterateFiles(p)) {
            final Path dest;
            if(Files.exists(target)) {
                if(!Files.isDirectory(target)) throw new IllegalArgumentException("Target exists and it's not a directory");
                Path fileName = source.getFileName();
                if (fileName == null) dest = target;
                else dest = Files.createDirectories(target.resolve(fileName.toString()));
            } else dest = Files.createDirectories(target);
            LOGGER.debug("Coping directory \"{}\" to \"{}\"...", source, target);
            sourceStream.forEach(path -> {
                Path relative = p.relativize(path);
                try (InputStream in = Files.newInputStream(path);
                    OutputStream out = Files.newOutputStream(ensureFileExist(dest.resolve(relative.toString())), TRUNCATE_EXISTING)) {
                    in.transferTo(out);
                } catch (IOException e) {
                    LOGGER.warn("Error coping file \"{}\"", path, e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Error coping directory", e);
        }
    }

    public static void copy(@NotNull Path source, @NotNull Path target) {
        if (Files.notExists(source)) {
            LOGGER.debug("Source \"{}\" does not exist, skipping this operation...", source);
            return;
        }
        if (Files.isDirectory(source)) copyDirectory(source, target);
        else if (Files.isRegularFile(source)) copyFile(source, target);
        else throw new UnsupportedOperationException();
    }

    public static void extractZipTo(Path jar, Path extractDir) {
        var complete = extractDir.resolve("extract.complete");
        if (Files.exists(complete)) return;
        try (var fs = JarUtil.createZipFs(jar)) {
            deleteIfExists(extractDir);
            Files.createDirectories(extractDir);
            copy(fs.getPath(""), extractDir);
            Files.createFile(complete);
        } catch (IOException e) {
            throw Utils.wrapInRuntime(e);
        }
    }
}