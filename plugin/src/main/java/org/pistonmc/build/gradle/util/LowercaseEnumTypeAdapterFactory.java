package org.pistonmc.build.gradle.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? extends T> c = (Class<? extends T>) type.getRawType();
        if (!c.isEnum()) return null;
        Class<Enum<?>> enumClass = (Class<Enum<?>>) c;
        Map<String, T> enumConstants = Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(Enum::name, c::cast));
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) out.nullValue();
                else out.value(value.toString().toLowerCase(Locale.ENGLISH));
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                } else return enumConstants.get(in.nextString().toUpperCase(Locale.ENGLISH));
            }
        };
    }
}