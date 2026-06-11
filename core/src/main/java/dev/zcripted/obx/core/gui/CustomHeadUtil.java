package dev.zcripted.obx.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Helpers for building player skulls (with the player's skin) and custom-textured
 * heads (base64 texture from minecraft-heads.com or similar).
 *
 * <p>Two material names cover every supported version: 1.13+ uses
 * {@code PLAYER_HEAD}; pre-1.13 uses {@code SKULL_ITEM} with data value 3.
 * The lookup cascades through both, so a single jar works on 1.8.x → 1.21.x.
 *
 * <p>Custom-texture injection uses Mojang's {@code GameProfile} class via
 * reflection — no compile-time dependency on internal NMS/authlib types, so
 * the jar still loads on servers that don't expose those classes. If the
 * reflective surface fails (very old Spigot, exotic forks), the helper
 * gracefully falls back to a {@link Material#BARRIER}-typed item so the GUI
 * still has a clickable slot.
 */
public final class CustomHeadUtil {

    private CustomHeadUtil() {
    }

    public static Material headMaterial() {
        Material modern = Material.matchMaterial("PLAYER_HEAD");
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial("SKULL_ITEM");
        return legacy != null ? legacy : Material.BARRIER;
    }

    /**
     * Builds an item that displays {@code player}'s skin as a player head.
     * On 1.13+ this is just a {@code PLAYER_HEAD} with {@code SkullMeta.setOwningPlayer};
     * on 1.8 – 1.12 it falls back to {@code SKULL_ITEM:3} with
     * {@code SkullMeta.setOwner(name)}.
     */
    public static ItemStack playerHead(OfflinePlayer player, String displayName, List<String> lore) {
        Material material = headMaterial();
        boolean legacy = "SKULL_ITEM".equals(material.name());
        ItemStack item = legacy ? new ItemStack(material, 1, (short) 3) : new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta && player != null) {
            SkullMeta skull = (SkullMeta) meta;
            try {
                Method setOwningPlayer = SkullMeta.class.getMethod("setOwningPlayer", OfflinePlayer.class);
                setOwningPlayer.invoke(skull, player);
            } catch (Throwable ignoredModern) {
                if (player.getName() != null) {
                    skull.setOwner(player.getName());
                }
            }
        }
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds an item that displays a custom head texture from a base64-encoded
     * Mojang texture payload (the value field from minecraft-heads.com / mineskin /
     * the Mojang sessionserver). Falls back to a barrier with the same display
     * name and lore on servers where the reflective surface isn't available.
     *
     * @param texture base64 string. The string is the value of the
     *                {@code textures} property in a Mojang {@code GameProfile} —
     *                typically a JSON document like
     *                {@code {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/<hash>"}}}}
     *                that has been base64-encoded.
     */
    public static ItemStack customHead(String texture, String displayName, List<String> lore) {
        Material material = headMaterial();
        boolean legacy = "SKULL_ITEM".equals(material.name());
        ItemStack item = legacy ? new ItemStack(material, 1, (short) 3) : new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        boolean injected = false;
        if (meta instanceof SkullMeta && texture != null && !texture.isEmpty()) {
            injected = injectTexture((SkullMeta) meta, texture);
        }
        if (!injected) {
            // Reflective profile injection failed — fall back to a barrier so
            // the slot is still visible. The display name / lore stay the
            // same so admins still see what the slot is for.
            return fallbackItem(displayName, lore);
        }
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack fallbackItem(String displayName, List<String> lore) {
        Material barrier = Material.matchMaterial("BARRIER");
        if (barrier == null) {
            barrier = Material.matchMaterial("REDSTONE_BLOCK");
        }
        if (barrier == null) {
            barrier = Material.STONE;
        }
        ItemStack item = new ItemStack(barrier);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Injects a base64 texture payload into {@code skull}. Tries the modern
     * {@code SkullMeta.setOwnerProfile(PlayerProfile)} path first (1.18+); on
     * older builds falls back to building an authlib {@code GameProfile} and
     * setting the private {@code profile} field on the skull meta — the same
     * trick every "head" library uses since 1.8.
     *
     * @return true on success
     */
    private static boolean injectTexture(SkullMeta skull, String texture) {
        // Derive the GameProfile UUID deterministically from the texture
        // payload so every render of the same texture pins the same cache
        // entry on the client. Same texture → same UUID; different textures
        // → different UUIDs.
        UUID profileUuid = UUID.nameUUIDFromBytes(texture.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // CraftBukkit / Paper treat a profile with a {@code null} name as
        // "incomplete" and may re-resolve it through Mojang's session
        // service when serializing the skull to the client — that
        // round-trip is what caused the visible head to swap between
        // unrelated textures on click / refresh. Giving the profile a
        // stable, fake-but-valid Minecraft username keeps it "complete" so
        // the server never tries to fill it in.
        String profileName = stableProfileName(profileUuid);

        // Path 1: modern Bukkit PlayerProfile (Paper / Spigot 1.18.1+).
        try {
            Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
            Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");
            Method createProfile = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
            Object profile = createProfile.invoke(null, profileUuid, profileName);
            Method getTextures = playerProfileClass.getMethod("getTextures");
            Object textures = getTextures.invoke(profile);
            Method setSkin = playerTexturesClass.getMethod("setSkin", java.net.URL.class);
            String url = decodeTextureUrl(texture);
            if (url != null) {
                setSkin.invoke(textures, new java.net.URL(url));
                Method setProfile = SkullMeta.class.getMethod("setOwnerProfile", playerProfileClass);
                setProfile.invoke(skull, profile);
                return true;
            }
        } catch (Throwable ignoredModern) {
            // older API — fall through to the GameProfile reflection path.
        }

        // Path 2: authlib GameProfile (1.8 – 1.17 and any modern build that
        // still ships authlib in the same package). Build a profile, attach a
        // {@code textures} property carrying the base64 payload, and shove it
        // into the skull meta's private {@code profile} field.
        try {
            Class<?> gameProfileClass = tryLoadClass(
                    "com.mojang.authlib.GameProfile",
                    "net.minecraft.util.com.mojang.authlib.GameProfile");
            Class<?> propertyClass = tryLoadClass(
                    "com.mojang.authlib.properties.Property",
                    "net.minecraft.util.com.mojang.authlib.properties.Property");
            if (gameProfileClass == null || propertyClass == null) {
                return false;
            }
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(profileUuid, profileName);
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", texture);
            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getProperties.invoke(gameProfile);
            Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(propertyMap, "textures", property);
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skull, gameProfile);
            return true;
        } catch (Throwable ignoredLegacy) {
            return false;
        }
    }

    /**
     * Produces a deterministic, Minecraft-username-shaped name for the
     * synthetic profile (3 – 16 chars, alphanumeric + underscore). The name
     * is derived from the texture-derived UUID so re-renders use the same
     * value, and it is prefixed with {@code head_} so it is extremely
     * unlikely to collide with a real Mojang account.
     */
    private static String stableProfileName(UUID profileUuid) {
        String hex = String.format("%016x", profileUuid.getMostSignificantBits());
        return "head_" + hex.substring(0, 11);
    }

    private static Class<?> tryLoadClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
                // keep trying
            }
        }
        return null;
    }

    /**
     * Decodes the texture URL from a base64 textures payload. The payload is
     * a JSON object like {@code {"textures":{"SKIN":{"url":"..."}}}}; we only
     * need the inner URL for the modern PlayerTextures API. A naive substring
     * scan is enough — full JSON parsing would pull in another dependency.
     */
    private static String decodeTextureUrl(String texture) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(texture);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            int urlIdx = json.indexOf("\"url\"");
            if (urlIdx < 0) {
                return null;
            }
            int colon = json.indexOf(':', urlIdx);
            int firstQuote = json.indexOf('"', colon + 1);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (firstQuote < 0 || secondQuote < 0) {
                return null;
            }
            return json.substring(firstQuote + 1, secondQuote);
        } catch (Throwable ignored) {
            return null;
        }
    }
}