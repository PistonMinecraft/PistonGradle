package org.pistonmc.build.gradle.forge.task;

import cn.maxpixel.mcdecompiler.util.FileUtil;
import cn.maxpixel.mcdecompiler.util.JarUtil;
import cn.maxpixel.mcdecompiler.util.LambdaUtil;
import cn.maxpixel.mcdecompiler.util.Utils;
import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.pistonmc.build.gradle.cache.VanillaMinecraftCache;
import org.pistonmc.build.gradle.forge.config.MCPConfig;
import org.pistonmc.build.gradle.forge.config.Side;
import org.pistonmc.build.gradle.forge.config.UserDevConfig;
import org.pistonmc.build.gradle.util.DigestUtil;
import org.pistonmc.build.gradle.util.VariableUtil;

import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static org.pistonmc.build.gradle.util.DependencyUtil.groupAndNameEquals;
import static org.pistonmc.build.gradle.util.FileUtil.extractZipTo;

/**
 * We don't have the functionality to stop at a specific step as there seems no need right now
 */
public abstract class SetupMCP extends DefaultTask {
    private final Provider<Directory> outputDirectory;

    @Internal
    public abstract Property<VanillaMinecraftCache> getCache();
    @Internal
    public abstract Property<Configuration> getForgeSetup();
    @Internal
    public abstract Property<UserDevConfig> getConfig();
    @Internal
    public abstract DirectoryProperty getBaseDirectory();
    @Internal
    public abstract Property<Configuration> getMcVanilla();
    @InputFiles
    public abstract ConfigurableFileCollection getAccessTransformerJar();
    @InputFiles
    public abstract ConfigurableFileCollection getSideAnnotationStripperJar();
    @Input
    public abstract Property<Side> getSide();
    @InputFiles
    public abstract ListProperty<RegularFile> getAccessTransformers();

    public SetupMCP() {
        getCache().disallowUnsafeRead();
        getConfig().disallowUnsafeRead();
        getBaseDirectory().disallowUnsafeRead();
        getAccessTransformerJar().disallowUnsafeRead();
        getSideAnnotationStripperJar().disallowUnsafeRead();
        getSide().convention(Side.JOINED).disallowUnsafeRead();
        getAccessTransformers().convention(List.of()).disallowUnsafeRead();
        this.outputDirectory = getBaseDirectory().zip(getSide().map(Side::toString), Directory::dir).zip(getAccessTransformers(), (dir, ats) -> {
            if (ats.isEmpty()) return dir;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] buf = new byte[65536];
                for (RegularFile at : ats) {
                    try (var fis = new FileInputStream(at.getAsFile())) {
                        int len;
                        while ((len = fis.read(buf)) > 0) {
                            md.update(buf, 0, len);
                        }
                    }
                }
                return dir.dir(DigestUtil.calculateHash(md).toString());
            } catch (NoSuchAlgorithmException | IOException e) {
                throw Utils.wrapInRuntime(e);
            }
        });
    }

    @OutputDirectory
    public Provider<Directory> getOutputDirectory() {
        return outputDirectory;
    }

    @TaskAction
    public void setup() throws IOException {
        var forgeSetup = getForgeSetup().get();
        var userDevConfig = getConfig().get();
        var side = getSide().get();
        var config = userDevConfig.mcp;
        var data = config.data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {// data guarantees that every value is absolute and normalized
            var value = e.getValue();
            if (value instanceof String s) return config.extractPath.resolve(s).toAbsolutePath().normalize().toString();
            else if (value instanceof Map<?,?> m) return config.extractPath.resolve(((Map<String, String>) m)
                    .get(side.toString())).toAbsolutePath().normalize().toString();
            else throw new UnsupportedOperationException();
        }));
        var toolchains = getProject().getExtensions().getByType(JavaToolchainService.class);
        var builtins = new BuiltinFunctions(getCache().get(), config, getMcVanilla().get(), side, getLogger());
        var outputDir = outputDirectory.get();
        getProject().delete(outputDir);
        getProject().mkdir(outputDir);
        var outputs = new Object2ObjectOpenHashMap<String, String>();
        var steps = config.steps.get(side);
        for (var step : steps) {
            var type = step.type();
            getLogger().lifecycle("Executing step {} of type {}", step.name(), step.type());
            var inputs = new Object2ObjectOpenHashMap<>(data);
            for (var entry : step.args().entrySet()) {
                inputs.put(entry.getKey(), VariableUtil.replaceVariable(entry.getValue(), outputs, false));
            }
            var workingDir = outputDir.dir(step.name());
            getProject().mkdir(workingDir);
            if (builtins.has(type)) {
                outputs.put(step.name() + "Output", Objects.requireNonNull(builtins.execute(type, inputs,
                        workingDir.getAsFile().toPath().toAbsolutePath().normalize())).toString());
            } else if (config.functions.containsKey(type)) {
                String ext = inputs.getOrDefault("outputExtension", ".jar");
                inputs.computeIfAbsent("output", k -> workingDir.file(k + ext).getAsFile().getAbsolutePath());
                inputs.computeIfAbsent("log", k -> workingDir.file("log.log").getAsFile().getAbsolutePath());
                if (type.equals("decompile")) {
                    runAccessTransformer(userDevConfig, config, inputs, getProject().mkdir(outputDir.dir("AccessTransformer")).toPath().toAbsolutePath().normalize(), toolchains);
                    runSideAnnotationStripper(userDevConfig, config, inputs, getProject().mkdir(outputDir.dir("SideStripper")).toPath().toAbsolutePath().normalize(), toolchains);
                }
                var func = config.functions.get(type);
                var executable = toolchains.launcherFor(spec -> spec.getLanguageVersion().set(func.javaVersion())).get().getExecutablePath();
                var logFile = workingDir.file("console.log").getAsFile();
                logFile.createNewFile();
                try (var os = new FileOutputStream(logFile)) {
                    getProject().javaexec(spec -> {
                        PrintStream ps = new PrintStream(os);
                        spec.setClasspath(forgeSetup.fileCollection(dep -> groupAndNameEquals(dep, func.jar())))
                                .setArgs(VariableUtil.replaceVariables(func.args(), inputs, false))
                                .setStandardOutput(ps)
                                .setErrorOutput(ps)
                                .workingDir(workingDir)
                                .executable(executable);
                        spec.jvmArgs(VariableUtil.replaceVariables(func.jvmArgs(), inputs, false))
                                .setDefaultCharacterEncoding(config.encoding.name());
                        Function<String, String> quote = s -> '"' + s + '"';
                        ps.println("JVM:         " + executable);
                        ps.println("Jar:         " + spec.getClasspath().getSingleFile().getAbsolutePath());
                        ps.println("JVM Args:    " + spec.getJvmArgs().stream().map(quote).collect(Collectors.joining(", ")));
                        ps.println("Run Args:    " + spec.getArgs().stream().map(quote).collect(Collectors.joining(", ")));
                        ps.println("Working Dir: " + workingDir.getAsFile().getAbsolutePath());
                    }).rethrowFailure().assertNormalExitValue();
                    outputs.put(step.name() + "Output", inputs.get("output"));
                }
            }
        }
    }

    private void runAccessTransformer(UserDevConfig userDevConfig, MCPConfig config, Map<String, String> inputs, Path workingDir, JavaToolchainService toolchains) throws IOException {
        var userAts = getAccessTransformers().get();
        var forgeAts = userDevConfig.ats;
        if (userAts.isEmpty() && forgeAts.isEmpty()) return;
        getLogger().lifecycle(" - Executing AccessTransformer");
        var executable = toolchains.launcherFor(spec -> spec.getLanguageVersion().set(config.javaTarget)).get().getExecutablePath();
        var logFile = workingDir.resolve("console.log");
        String output = workingDir.resolve("output.jar").toString();
        try (var os = Files.newOutputStream(logFile, StandardOpenOption.CREATE)) {
            getProject().javaexec(spec -> {
                spec.classpath(getAccessTransformerJar())
                        .setArgs(List.of("--inJar", inputs.get("input"), "--outJar", output))
                        .setStandardOutput(os)
                        .setErrorOutput(os)
                        .workingDir(workingDir)
                        .executable(executable);
                for (RegularFile userAt : userAts) {
                    spec.args("--atFile", userAt.getAsFile().getAbsolutePath());
                }
                for (Path forgeAt : forgeAts) {
                    spec.args("--atFile", forgeAt.toAbsolutePath().normalize().toString());
                }
            }).assertNormalExitValue().rethrowFailure();
        }
        inputs.put("input", output);
    }

    private void runSideAnnotationStripper(UserDevConfig userDevConfig, MCPConfig config, Map<String, String> inputs, Path workingDir, JavaToolchainService toolchains) throws IOException {
        var forgeSass = userDevConfig.sass;
        if (forgeSass.isEmpty()) return;
        getLogger().lifecycle(" - Executing SideAnnotationStripper");
        var executable = toolchains.launcherFor(spec -> spec.getLanguageVersion().set(config.javaTarget)).get().getExecutablePath();
        var logFile = workingDir.resolve("console.log");
        String output = workingDir.resolve("output.jar").toString();
        try (var os = Files.newOutputStream(logFile, StandardOpenOption.CREATE)) {
            getProject().javaexec(spec -> {
                spec.classpath(getSideAnnotationStripperJar())
                        .setArgs(List.of("--strip", "--input", inputs.get("input"), "--output", output))
                        .setStandardOutput(os)
                        .setErrorOutput(os)
                        .workingDir(workingDir)
                        .executable(executable);
                for (Path forgeSas : forgeSass) {
                    spec.args("--data", forgeSas.toAbsolutePath().normalize().toString());
                }
            }).assertNormalExitValue().rethrowFailure();
        }
        inputs.put("input", output);
    }

    private record BuiltinFunctions(VanillaMinecraftCache cache, MCPConfig config, Configuration mcVanillaConfig, Side side, Logger logger) {
        private static final Attributes.Name BUNDLER_FORMAT = new Attributes.Name("Bundler-Format");
        private static final Map<String, MethodHandle> FUNCTIONS = new Object2ObjectOpenHashMap<>();
        static {
            var lookup = MethodHandles.lookup();
            for (Method m : BuiltinFunctions.class.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Func.class)) {
                    try {
                        FUNCTIONS.put(m.getName(), lookup.unreflectSpecial(m, BuiltinFunctions.class));
                    } catch (IllegalAccessException e) {
                        throw new ExceptionInInitializerError(e);
                    }
                }
            }
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        private @interface Func {}

        @Func
        private Path downloadManifest() {
            cache.getVersionManifest();
            return cache.versionManifestFile;
        }

        @Func
        private Path downloadJson() {
            cache.getVersionJson(config.version);
            return cache.getVersionJsonFile(config.version);
        }

        @Func
        private Path downloadClient() {
            return cache.getClientJar(config.version);
        }

        @Func
        private Path downloadClientMappings() {
            return cache.getClientMappings(config.version);
        }

        @Func
        private Path downloadServer() {
            return cache.getServerJar(config.version);
        }

        @Func
        private Path downloadServerMappings() {
            return cache.getServerMappings(config.version);
        }

        @Func
        private Path strip(Map<String, String> inputs, Path workingDir) throws IOException {
            Set<String> filter;
            try (var lines = Files.lines(Path.of(inputs.get("mappings")))) {
                filter = lines.filter(l -> !l.startsWith("\t"))
                        .map(s -> s.substring(0, s.indexOf(' ')).concat(".class"))
                        .collect(Collectors.toSet());
            }
            boolean whitelist = inputs.getOrDefault("mode", "whitelist").equalsIgnoreCase("whitelist");
            Path output = workingDir.resolve("output.jar");
            try (var in = JarUtil.createZipFs(Path.of(inputs.get("input")));
                 var out = JarUtil.createZipFs(output, true);
                 var files = FileUtil.iterateFiles(in.getPath(""))) {
                files.filter(path -> filter.contains(path.toString()) == whitelist).forEach(LambdaUtil.unwrapConsumer(path -> {
                    try (var is = Files.newInputStream(path);
                         var os = Files.newOutputStream(FileUtil.ensureFileExist(out.getPath(path.toString())), StandardOpenOption.WRITE)) {
                        is.transferTo(os);
                    }
                }));
            }
            return output;
        }

        @Func
        private Path listLibraries(Map<String, String> inputs, Path workingDir) throws IOException {
            var outputPath = inputs.get("output");
            Path output = outputPath == null ? workingDir.resolve("libraries.txt") : Path.of(outputPath);
            var bundlePath = inputs.get("bundle");
            Path bundle = bundlePath == null ? null : Path.of(bundlePath);
            Set<String> libs;
            if (bundle != null) {
                Path extracted = workingDir.resolve(bundle.getFileName().toString());
                extractZipTo(bundle, extracted);
                Path metaInf = extracted.resolve("META-INF");
                Path manifest = metaInf.resolve("MANIFEST.MF");
                if (Files.notExists(manifest)) throw new IllegalArgumentException("Missing META-INF/MANIFEST.MF");

                Manifest man;
                try (InputStream is = Files.newInputStream(manifest)) {
                    man = new Manifest(is);
                }
                String format = man.getMainAttributes().getValue(BUNDLER_FORMAT);
                if (format == null)
                    throw new IllegalArgumentException("Invalid bundler archive; missing format entry from manifest");
                if (!"1.0".equals(format))
                    throw new IllegalArgumentException("Unsupported bundler format " + format + "; only 1.0 is supported");

                try (var lines = Files.lines(metaInf.resolve("libraries.list"))) {
                    Path libraries = metaInf.resolve("libraries");
                    libs = lines.map(s -> s.substring(s.lastIndexOf('\t')))
                            .map(libraries::resolve)
                            .map(Path::toAbsolutePath)
                            .map(Path::normalize)
                            .map(Path::toString)
                            .collect(ObjectOpenHashSet.toSet());
                }
            } else {
                libs = mcVanillaConfig.getFiles().stream().map(File::getAbsolutePath).collect(ObjectOpenHashSet.toSet());
            }
            try (PrintStream ps = new PrintStream(Files.newOutputStream(FileUtil.ensureFileExist(output)), false, config.encoding)) {
                libs.stream().map("-e="::concat).forEach(ps::println);
                ps.flush();
            }
            return output;
        }

        @Func
        private Path inject(Map<String, String> inputs, Path workingDir) throws IOException {
            var inject = Path.of(inputs.get("inject"));
            String template = null;
            Map<String, byte[]> injectFiles;
            try (var files = FileUtil.iterateFiles(inject)) {
                injectFiles = files.collect(Collectors.toMap(p -> inject.relativize(p).toString(),
                        LambdaUtil.unwrap(Files::readAllBytes)));
                if (injectFiles.containsKey("package-info-template.java")) {
                    template = new String(injectFiles.get("package-info-template.java"), config.encoding);
                    injectFiles.remove("package-info-template.java");
                }
            }
            Path output = workingDir.resolve("output.jar");
            try (var in = JarUtil.createZipFs(Path.of(inputs.get("input")));
                 var out = JarUtil.createZipFs(output, true);
                 var files = FileUtil.iterateFiles(in.getPath(""))) {
                files.forEach(LambdaUtil.unwrapConsumer(path -> {
                    try (var is = Files.newInputStream(path);
                         var os = Files.newOutputStream(FileUtil.ensureFileExist(out.getPath(path.toString())), StandardOpenOption.WRITE)) {
                        is.transferTo(os);
                    }
                }));
                if (template != null) {
                    injectPackageInfo(in.getPath(""), template);
                }
                for (var entry : injectFiles.entrySet()) {
                    String path = entry.getKey();
                    if (Side.SERVER == side ? path.contains("/client/") : path.contains("/server/")) continue;
                    try (var os = Files.newOutputStream(FileUtil.ensureFileExist(out.getPath(path)), StandardOpenOption.WRITE)) {
                        os.write(entry.getValue());
                    }
                }
            }
            return output;
        }

        private void injectPackageInfo(Path path, String template) throws IOException {
            try (var ds = Files.newDirectoryStream(path, Files::isDirectory)) {
                for (Path p : ds) {
                    injectPackageInfo(p, template);
                }
                var pkgInfo = path.resolve("package-info.java");
                try (var os = Files.newOutputStream(pkgInfo)) {
                    os.write(template.replace("{PACKAGE}", path.toString().replace('/', '.'))
                            .getBytes(config.encoding));
                }
            }
        }

        @Func
        private Path patch(Map<String, String> inputs, Path workingDir) throws IOException {
            Path output = workingDir.resolve("output.jar");
            Path rejects = workingDir.resolve("rejects.zip");

            CliOperation.Result<PatchOperation.PatchesSummary> result = PatchOperation.builder()
                    .logTo(new LoggingOutputStream(logger, LogLevel.LIFECYCLE))
                    .basePath(Path.of(inputs.get("input")))
                    .patchesPath(Path.of(inputs.get("patches")))
                    .outputPath(output)
                    .verbose(false)
                    .mode(PatchMode.OFFSET)
                    .rejectsPath(rejects)
                    .build()
                    .operate();

            if (result.exit != 0) {
                logger.error("Rejects saved to: {}", rejects);
                throw new RuntimeException("Patch failure.");
            }

            return output;
        }

        public boolean has(String type) {
            return FUNCTIONS.containsKey(type);
        }

        public Path execute(String type, Map<String, String> inputs, Path workingDir) {
            try { // the assumption is that you call #has first and it returned true
                var handle = FUNCTIONS.get(type).bindTo(this);
                if (handle.type().parameterCount() > 0) {
                    handle = MethodHandles.insertArguments(handle, 0, inputs, workingDir);
                }
                return (Path) handle.invokeExact();
            } catch (Throwable e) {
                throw Utils.wrapInRuntime(e);
            }
        }
    }
}
