package dev.zcripted.obx.core.motd;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the welcome-MOTD write/read path: a flat dotted key whose value is a
 * mixed List of plain strings + {text,hover,click} maps, written with
 * {@code YamlConfiguration.set + saveToString} and read back with
 * {@code getList} — exactly what LanguageFile does. Verifies the map nodes
 * survive the round-trip (if they don't, only plain lines would render).
 */
class MotdRoundTripTest {

    @Test
    void mapNodesSurviveYamlRoundTrip() throws Exception {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("text", "Welcome");
        List<String> hover = new ArrayList<>();
        hover.add("line a");
        hover.add("line b");
        node.put("hover", hover);

        List<Object> value = new ArrayList<>();
        value.add(" ");
        value.add(node);
        value.add("&8• Online: 1/20");

        YamlConfiguration out = new YamlConfiguration();
        out.set("welcome.motd-lines", value);
        String yaml = out.saveToString();

        YamlConfiguration in = new YamlConfiguration();
        in.loadFromString(yaml);

        assertTrue(in.isList("welcome.motd-lines"), "key should still be a list");
        List<?> read = in.getList("welcome.motd-lines");
        assertEquals(3, read.size(), "all 3 elements should survive");
        long maps = read.stream().filter(o -> o instanceof Map).count();
        assertEquals(1, maps, "the map node must survive as a Map (saved yaml:\n" + yaml + ")");
    }
}
