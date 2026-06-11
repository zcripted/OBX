package dev.zcripted.obx.feature.deathdrop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Serializes a list of {@link ItemStack}s to/from a Base64 string using Bukkit's own
 * {@code ConfigurationSerializable} object streams (available since 1.7, so safe on the whole
 * 1.12–1.21 range). Used to persist a death carry-all's contents on its dropped item entity so they
 * survive a restart.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    /** Encodes the (non-null) stacks to Base64, or {@code null} on failure. */
    public static String toBase64(List<ItemStack> items) {
        if (items == null) {
            return null;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(items.size());
            for (ItemStack item : items) {
                out.writeObject(item);
            }
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Decodes a Base64 string produced by {@link #toBase64}, or {@code null} on failure. */
    public static List<ItemStack> fromBase64(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            int size = in.readInt();
            List<ItemStack> items = new ArrayList<>(Math.max(0, size));
            for (int i = 0; i < size; i++) {
                Object read = in.readObject();
                if (read instanceof ItemStack) {
                    items.add((ItemStack) read);
                }
            }
            return items;
        } catch (Throwable ignored) {
            return null;
        }
    }
}