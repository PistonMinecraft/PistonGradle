package org.pistonmc.build.gradle.util.version;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.pistonmc.build.gradle.PistonGradlePlugin;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record VersionJson(Map<String, List<Argument>> arguments, AssetIndex assetIndex, String assets, int complianceLevel, Map<String, GameDownload> downloads,
                          String id, JavaVersion javaVersion, List<Library> libraries, Map<String, LoggingConfig> logging, String mainClass,
                          String minecraftArguments, String minimumLauncherVersion, ZonedDateTime releaseTime, ZonedDateTime time, Type type) {
    @JsonAdapter(Argument.Adapter.class)
    public interface Argument {
        record Simple(String value) implements Argument {}
        record Conditional(List<Rule> rules, List<String> value) implements Argument {}

        class Adapter extends TypeAdapter<Argument> {
            @Override
            public void write(JsonWriter out, Argument value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Argument read(JsonReader in) throws IOException {
                JsonToken token = in.peek();
                if (token == JsonToken.STRING) return new Simple(in.nextString());
                else if (token == JsonToken.BEGIN_OBJECT) {
                    in.beginObject();
                    List<Rule> rules = null;
                    List<String> value = null;
                    while (in.peek() != JsonToken.END_OBJECT) switch (in.nextName()) {
                        case "rules" -> rules = PistonGradlePlugin.GSON.fromJson(in, List.class);
                        case "value" -> {
                            if (in.peek() == JsonToken.STRING) value = ObjectLists.singleton(in.nextString());
                            else value = PistonGradlePlugin.GSON.fromJson(in, List.class);
                        }
                    }
                    in.endObject();
                    return new Conditional(Objects.requireNonNull(rules), Objects.requireNonNull(value));
                } else throw new IOException("Invalid argument");
            }
        }
    }

    public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {}

    public record JavaVersion(String component, int majorVersion) {}

    public record GameDownload(String sha1, int size, String url) {}

    public record LoggingConfig(String argument, File file, String type) {
        public record File(String id, String sha1, String size, String url) {}
    }

    public record Library(Downloads downloads, String name, Map<String, String> natives, List<Rule> rules, Extract extract) {
        public record Downloads(Artifact artifact, Map<String, Artifact> classifiers) {
            public record Artifact(String path, String sha1, int size, String url) {}
        }

        public record Extract(List<String> exclude) {}
    }

    public record Rule(Action action, OS os, Features features) {
        public enum Action {
            ALLOW, DISALLOW
        }

        public record OS(String name, String arch, String version) {}

        public record Features(@SerializedName("is_demo_user") boolean isDemoUser, @SerializedName("has_custom_resolution") boolean hasCustomResolution) {}
    }
}