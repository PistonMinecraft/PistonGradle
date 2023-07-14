package org.pistonmc.build.gradle.util;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CollectionUtil {
    public static <T> List<T> nonNull(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }

    public static <K, V> Map<K, V> nonNull(@Nullable Map<K, V> map) {
        return map == null ? Map.of() : map;
    }
}