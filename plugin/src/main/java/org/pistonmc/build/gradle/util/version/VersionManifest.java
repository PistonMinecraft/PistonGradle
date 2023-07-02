package org.pistonmc.build.gradle.util.version;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.pistonmc.build.gradle.PistonGradlePlugin;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record VersionManifest(String latestRelease, String latestSnapshot, Map<String, Entry> versions) {
    public static final Adapter ADAPTER = new Adapter();

    public record Entry(String id, Type type, String url, ZonedDateTime time, ZonedDateTime releaseTime, String sha1, int complianceLevel) {}

    public static final class Adapter extends TypeAdapter<VersionManifest> {
        @Override
        public void write(JsonWriter out, VersionManifest value) {
            throw new UnsupportedOperationException("No need to write");
        }

        @Override
        public VersionManifest read(JsonReader in) throws IOException {
            in.beginObject();
            Optional<JsonObject> latest = Optional.empty();
            Optional<List<Entry>> versions = Optional.empty();
            while (in.peek() != JsonToken.END_OBJECT) switch (in.nextName()) {
                case "latest" -> latest = Optional.of(PistonGradlePlugin.GSON.fromJson(in, JsonObject.class));
                case "versions" -> versions = Optional.of(PistonGradlePlugin.GSON.fromJson(in, new TypeToken<>() {}));
                default -> throw new IOException("Invalid version manifest");
            }
            in.endObject();
            JsonObject o = latest.orElseThrow();
            return new VersionManifest(o.get("release").getAsString(), o.get("snapshot").getAsString(),
                    versions.orElseThrow().stream().collect(Collectors.toMap(Entry::id, Function.identity())));
        }
    }
}