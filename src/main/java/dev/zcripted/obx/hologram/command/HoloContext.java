package dev.zcripted.obx.hologram.command;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramId;
import dev.zcripted.obx.hologram.service.HologramService;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared dependencies + helper methods for every {@link HoloSubCommand}.
 * Holds references to {@link OBX} / {@link HologramService} and centralises
 * the language-key feedback patterns ("hologram not found", "invalid id",
 * etc.) so each subcommand stays focused on its own logic.
 */
public final class HoloContext {

    private final OBX plugin;
    private final HologramService service;

    public HoloContext(OBX plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public OBX plugin() {
        return plugin;
    }

    public HologramService service() {
        return service;
    }

    /** Send a language key with no replacements. */
    public void msg(CommandSender sender, String key) {
        plugin.getLanguageManager().send(sender, key);
    }

    /** Send a language key with one replacement. */
    public void msg(CommandSender sender, String key, String placeholder, String value) {
        Map<String, String> reps = new HashMap<>();
        reps.put(placeholder, value);
        plugin.getLanguageManager().send(sender, key, reps);
    }

    /** Resolve a user-supplied id string to a registered hologram, sending feedback on failure. */
    public Hologram resolveHologram(CommandSender sender, String raw) {
        HologramId id = HologramId.parse(raw);
        if (id == null) {
            msg(sender, "hologram.error.invalid_id");
            return null;
        }
        Hologram hologram = service.getRegistry().get(id);
        if (hologram == null) {
            msg(sender, "hologram.error.not_found", "name", id.value());
            return null;
        }
        return hologram;
    }

    /** Parse a 1-based line index from user input, or {@code -1} if invalid. */
    public int parseLineIndex(String raw) {
        try {
            int oneBased = Integer.parseInt(raw);
            return oneBased - 1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /** Persist a single hologram and trigger a backend re-spawn for its viewers. */
    public void persistAndRefresh(Hologram hologram) {
        hologram.markDirty();
        service.getStorage().save(hologram);
        if (service.getRenderer() != null) {
            service.getBackend().applyMutations(hologram);
            service.getRegistry().rebuildEntityIndex(hologram);
        }
    }
}
