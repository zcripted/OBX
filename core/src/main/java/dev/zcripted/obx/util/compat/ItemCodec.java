package dev.zcripted.obx.util.compat;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Single-ItemStack ⟷ Base64 codec via Bukkit's object streams (1.8-safe, preserves
 * full NBT/meta). Both directions return {@code null} on failure — callers must
 * treat a null as "do not persist / do not deliver", never as an empty item.
 *
 * <p>This is a core-level copy of {@code dev.zcripted.obx.feature.economy.util.ItemCodec}
 * so that modules without an economy dependency can persist items.
 */
public final class ItemCodec {

    private ItemCodec() {
    }

    public static String toBase64(ItemStack item) {
        if (item == null) {
            return null;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(item);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception failure) {
            return null;
        }
    }

    public static ItemStack fromBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            Object value = in.readObject();
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (Exception failure) {
            return null;
        }
    }
}