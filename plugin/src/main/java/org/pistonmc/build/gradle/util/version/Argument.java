package org.pistonmc.build.gradle.util.version;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.pistonmc.build.gradle.PistonGradlePlugin;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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
                    case "rules" -> rules = PistonGradlePlugin.GSON.fromJson(in, new TypeToken<>() {});
                    case "value" -> {
                        if (in.peek() == JsonToken.STRING) value = ObjectLists.singleton(in.nextString());
                        else value = PistonGradlePlugin.GSON.fromJson(in, new TypeToken<>() {});
                    }
                }
                in.endObject();
                return new Conditional(Objects.requireNonNull(rules), Objects.requireNonNull(value));
            } else throw new IOException("Invalid argument");
        }
    }
}
