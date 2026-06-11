package dev.zcripted.obx.feature.hologram.storage;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.anim.AnimationConfig;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramId;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import dev.zcripted.obx.feature.hologram.model.HologramSettings;
import dev.zcripted.obx.core.storage.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Model ↔ {@link ConfigurationSection} translation. Stateless — all methods
 * are static — so callers can serialize one hologram inline without staging
 * an instance.
 *
 * <p>Forward-compatibility rule: any key the deserializer doesn't recognise
 * is silently ignored. New settings can be added in later phases without
 * needing migration logic for older save files.
 */
public final class HologramSerializer {

    private HologramSerializer() {
    }

    public static void write(Hologram hologram, ConfigurationSection section) {
        if (hologram == null || section == null) {
            return;
        }
        ConfigurationSection loc = section.createSection("location");
        LocationSerializer.serialize(loc, hologram.getLocation());

        ConfigurationSection settings = section.createSection("settings");
        HologramSettings s = hologram.getSettings();
        settings.set("billboard", s.getBillboard().name());
        settings.set("text-alignment", s.getTextAlignment().name());
        settings.set("scale", s.getScale());
        settings.set("show-range", s.getShowRange());
        settings.set("update-range", s.getUpdateRange());
        settings.set("double-sided", s.isDoubleSided());
        settings.set("shadow", s.hasShadow());
        settings.set("see-through", s.isSeeThrough());
        settings.set("background-color", s.getBackgroundColor());
        settings.set("text-opacity", s.getTextOpacity());
        settings.set("line-width", s.getLineWidth());
        settings.set("interaction-enabled", s.isInteractionEnabled());
        settings.set("interaction-width", s.getInteractionWidth());
        settings.set("interaction-height", s.getInteractionHeight());
        settings.set("interaction-cooldown-ms", s.getInteractionCooldownMs());
        settings.set("view-permission", s.getViewPermission());
        settings.set("hide-behind-walls", s.isHideBehindWalls());
        settings.set("board-enabled", s.isBoardEnabled());
        settings.set("board-material", s.getBoardMaterial());
        settings.set("board-width", s.getBoardWidth());
        settings.set("board-height", s.getBoardHeight());
        settings.set("board-offset-back", s.getBoardOffsetBack());

        List<HologramLine> lines = hologram.getLines();
        java.util.List<Map<String, Object>> serializedLines = new java.util.ArrayList<>(lines.size());
        for (HologramLine line : lines) {
            Map<String, Object> entry = new LinkedHashMap<>();
            switch (line.getType()) {
                case TEXT: {
                    entry.put("type", "TEXT");
                    entry.put("value", ((HologramLine.TextLine) line).getTemplate());
                    break;
                }
                case ICON: {
                    HologramLine.IconLine icon = (HologramLine.IconLine) line;
                    ItemStack stack = icon.getStack();
                    entry.put("type", "ICON");
                    if (stack != null) {
                        entry.put("material", stack.getType().name());
                        entry.put("amount", stack.getAmount());
                    } else {
                        entry.put("material", "STONE");
                        entry.put("amount", 1);
                    }
                    break;
                }
                case BLOCK: {
                    HologramLine.BlockLine block = (HologramLine.BlockLine) line;
                    entry.put("type", "BLOCK");
                    entry.put("material", block.getMaterial().name());
                    break;
                }
                default:
                    continue;
            }
            serializedLines.add(entry);
        }
        section.set("lines", serializedLines);

        java.util.List<Map<String, Object>> animList = new java.util.ArrayList<>();
        for (AnimationConfig cfg : hologram.getAnimationConfigs()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", cfg.getType());
            entry.put("params", new LinkedHashMap<>(cfg.getParams()));
            entry.put("enabled", cfg.isEnabled());
            animList.add(entry);
        }
        section.set("animations", animList);
    }

    public static Hologram read(String name, ConfigurationSection section, ObxPlugin plugin) {
        if (name == null || section == null || plugin == null) {
            return null;
        }
        HologramId id = HologramId.parse(name);
        if (id == null) {
            plugin.getLogger().warning("[Holograms] Skipping hologram with invalid id: " + name);
            return null;
        }
        ConfigurationSection loc = section.getConfigurationSection("location");
        Location location = LocationSerializer.deserialize(loc, plugin);
        if (location == null) {
            plugin.getLogger().warning("[Holograms] Skipping hologram " + name + " — world not loaded.");
            return null;
        }
        Hologram hologram = new Hologram(id, location);
        ConfigurationSection settings = section.getConfigurationSection("settings");
        if (settings != null) {
            HologramSettings s = hologram.getSettings();
            s.setBillboard(parseEnum(HologramSettings.Billboard.class,
                    settings.getString("billboard"), HologramSettings.Billboard.CENTER));
            s.setTextAlignment(parseEnum(HologramSettings.TextAlignment.class,
                    settings.getString("text-alignment"), HologramSettings.TextAlignment.CENTER));
            s.setScale(settings.getDouble("scale", 1.0));
            s.setShowRange(settings.getDouble("show-range", 48.0));
            s.setUpdateRange(settings.getDouble("update-range", 64.0));
            s.setDoubleSided(settings.getBoolean("double-sided", true));
            s.setShadow(settings.getBoolean("shadow", false));
            s.setSeeThrough(settings.getBoolean("see-through", false));
            s.setBackgroundColor(settings.getInt("background-color", 0x40000000));
            s.setTextOpacity(settings.getInt("text-opacity", 255));
            s.setLineWidth(settings.getInt("line-width", 200));
            s.setInteractionEnabled(settings.getBoolean("interaction-enabled", false));
            s.setInteractionWidth(settings.getDouble("interaction-width", 1.0));
            s.setInteractionHeight(settings.getDouble("interaction-height", 1.0));
            s.setInteractionCooldownMs(settings.getLong("interaction-cooldown-ms", 500L));
            s.setViewPermission(settings.getString("view-permission"));
            s.setHideBehindWalls(settings.getBoolean("hide-behind-walls", false));
            s.setBoardEnabled(settings.getBoolean("board-enabled", false));
            String boardMat = settings.getString("board-material");
            if (boardMat != null && !boardMat.isEmpty()) {
                s.setBoardMaterial(boardMat);
            }
            s.setBoardWidth(settings.getDouble("board-width", 1.5));
            s.setBoardHeight(settings.getDouble("board-height", 0.0));
            s.setBoardOffsetBack(settings.getDouble("board-offset-back", 0.05));
        }
        List<?> lineList = section.getList("lines");
        if (lineList != null) {
            for (Object raw : lineList) {
                if (!(raw instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) raw;
                String typeStr = String.valueOf(entry.get("type")).toUpperCase(Locale.ENGLISH);
                try {
                    HologramLine.Type type = HologramLine.Type.valueOf(typeStr);
                    switch (type) {
                        case TEXT:
                            hologram.addLine(HologramLine.text(String.valueOf(entry.get("value"))));
                            break;
                        case ICON: {
                            String mat = String.valueOf(entry.get("material"));
                            Material material = Material.matchMaterial(mat);
                            if (material == null) {
                                material = Material.STONE;
                            }
                            Object amountObj = entry.get("amount");
                            int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 1;
                            ItemStack stack = new ItemStack(material, Math.max(1, amount));
                            hologram.addLine(HologramLine.icon(stack));
                            break;
                        }
                        case BLOCK: {
                            String mat = String.valueOf(entry.get("material"));
                            Material material = Material.matchMaterial(mat);
                            if (material == null) {
                                material = Material.STONE;
                            }
                            hologram.addLine(HologramLine.block(material));
                            break;
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // unknown line type — ignore so future-phase line types don't break older code
                }
            }
        }
        List<?> animList = section.getList("animations");
        if (animList != null) {
            for (Object raw : animList) {
                if (!(raw instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) raw;
                String type = String.valueOf(entry.get("type"));
                Object paramsObj = entry.get("params");
                Map<String, Object> params = paramsObj instanceof Map
                        ? new LinkedHashMap<>((Map<String, Object>) paramsObj)
                        : new LinkedHashMap<>();
                Object enabledObj = entry.get("enabled");
                boolean enabled = !(enabledObj instanceof Boolean) || ((Boolean) enabledObj);
                hologram.addAnimation(new AnimationConfig(type, params, enabled));
            }
        }
        hologram.clearDirty();
        return hologram;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}