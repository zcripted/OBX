package dev.zcripted.obx.util.text;

import java.util.HashMap;
import java.util.Map;

public final class Placeholders {

    private Placeholders() {
    }

    public static Map<String, String> with(String key, Object value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value == null ? "" : String.valueOf(value));
        return map;
    }

    public static Map<String, String> with(String key, Object value, String key2, Object value2) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value == null ? "" : String.valueOf(value));
        map.put(key2, value2 == null ? "" : String.valueOf(value2));
        return map;
    }

    public static Map<String, String> with(String key, Object value, String key2, Object value2,
                                           String key3, Object value3) {
        Map<String, String> map = with(key, value, key2, value2);
        map.put(key3, value3 == null ? "" : String.valueOf(value3));
        return map;
    }

    public static Map<String, String> merge(Map<String, String> original, String key, Object value) {
        Map<String, String> map = new HashMap<>();
        if (original != null) {
            map.putAll(original);
        }
        map.put(key, value == null ? "" : String.valueOf(value));
        return map;
    }
}