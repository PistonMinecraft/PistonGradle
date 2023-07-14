package org.pistonmc.build.gradle.forge.config;

import cn.maxpixel.mcdecompiler.util.Utils;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.pistonmc.build.gradle.PistonGradlePlugin;
import org.pistonmc.build.gradle.forge.ForgeConstants;
import org.pistonmc.build.gradle.forge.config.raw.MCPConfigRaw;
import org.pistonmc.build.gradle.util.FileUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.pistonmc.build.gradle.util.CollectionUtil.nonNull;
import static org.pistonmc.build.gradle.util.Utils.forName;

public class MCPConfig {
    public final String version;
    public final Data data;
    public final Map<Side, List<Step>> steps;
    public final Map<String, ExternalJarExec> functions;
    public final Map<Side, List<Dependency>> libraries;
    public final boolean official;
    public final JavaVersion javaTarget;
    public final Charset encoding;

    public MCPConfig(String version, Data data, Map<Side, List<Step>> steps, Map<String, ExternalJarExec> functions,
                     Map<Side, List<Dependency>> libraries, boolean official, JavaVersion javaTarget,
                     Charset encoding) {
        this.version = version;
        this.data = data;
        this.steps = steps;
        this.functions = functions;
        this.libraries = libraries;
        this.official = official;
        this.javaTarget = javaTarget;
        this.encoding = encoding;
    }

    public static MCPConfig load(FileCollection mcpZip, Path extractBaseDir, Configuration forgeSetup, DependencyHandler dependencies) {
        Path path = mcpZip.getSingleFile().toPath();
        Path extract = extractBaseDir.resolve(path.getFileName().toString());
        FileUtil.extractZipTo(path, extract);
        DependencySet deps = forgeSetup.getDependencies();
        try (var isr = new InputStreamReader(Files.newInputStream(extract.resolve(ForgeConstants.CONFIG_PATH)), StandardCharsets.UTF_8)) {
            var raw = PistonGradlePlugin.GSON.fromJson(isr, MCPConfigRaw.class);
            Data data = raw.data == null ? null : Data.from(extract, raw.data);
            var functions = nonNull(raw.functions).entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), ExternalJarExec.from(e.getValue(), dependencies, deps)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new MCPConfig(raw.version, data, raw.steps, functions, nonNull(raw.libraries).entrySet().stream()
                    .map(e -> Map.entry(Side.of(e.getKey()), nonNull(e.getValue()).stream().map(dependencies::create).toList()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), raw.official, JavaVersion.values()[raw.java_target - 1], forName(raw.encoding));
        } catch (IOException e) {
            throw Utils.wrapInRuntime(e);
        }
    }

    public enum Side {
        CLIENT, JOINED, SERVER;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Side of(String s) {
            return valueOf(s.toUpperCase(Locale.ENGLISH));
        }
    }

    public record Data(Path mappings, Path inject, Map<Side, Path> patches) {
        public static Data from(Path base, Map<String, Object> raw) {
            return new Data(base.resolve((String) raw.get("mappings")), base.resolve((String) raw.get("inject")),
                    ((Map<String, String>) raw.get("patches")).entrySet().stream().map(e -> Map.entry(Side.of(e.getKey()), base.resolve(e.getValue())))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    @JsonAdapter(value = Step.Adapter.class, nullSafe = false)
    public record Step(String type, String name, Map<String, String> args) {
        public static class Adapter extends TypeAdapter<Step> {
            @Override
            public void write(JsonWriter out, Step value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Step read(JsonReader in) throws IOException {
                String type = null;
                String name = null;
                Map<String, String> args = new Object2ObjectOpenHashMap<>();
                in.beginObject();
                String key;
                while (in.peek() != JsonToken.END_OBJECT) switch (key = in.nextName()) {
                    case "type" -> {
                        type = in.nextString();
                        if (name == null) name = type;
                    }
                    case "name" -> name = in.nextString();
                    default -> args.put(key, in.nextString());
                }
                in.endObject();
                return new Step(Objects.requireNonNull(type, "Could not parse step: Missing 'type'"),
                        Objects.requireNonNull(name), Map.copyOf(args));
            }
        }
    }
}