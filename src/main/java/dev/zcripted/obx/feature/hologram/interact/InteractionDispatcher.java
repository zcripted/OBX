package dev.zcripted.obx.feature.hologram.interact;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramId;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import dev.zcripted.obx.feature.hologram.packet.InteractDecoder;
import dev.zcripted.obx.feature.hologram.service.HologramService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OBX-thread dispatcher for hologram interactions. Called both by the
 * packet handler (when the Netty layer is available) and by the raycast
 * targeter (fallback). Enforces per-hologram per-player cooldowns and runs
 * the CText action — either as console or as the player based on the
 * {@code asConsole} convention (commands prefixed with {@code !} run as
 * console; everything else runs as the player).
 */
public final class InteractionDispatcher {

    private static final ConcurrentHashMap<UUID, Map<HologramId, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    private InteractionDispatcher() {
    }

    public static void dispatch(ObxPlugin plugin, HologramService service, Player viewer, HologramId id, InteractDecoder.Decoded decoded) {
        if (plugin == null || service == null || viewer == null || id == null) {
            return;
        }
        Hologram hologram = service.getRegistry().get(id);
        if (hologram == null || !hologram.getSettings().isInteractionEnabled()) {
            return;
        }
        // Phase 7 public API event — third-party plugins can cancel this to
        // suppress the configured command.
        dev.zcripted.obx.feature.hologram.api.HologramInteractEvent event =
                new dev.zcripted.obx.feature.hologram.api.HologramInteractEvent(hologram, viewer, decoded.action);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        long cooldownMs = hologram.getSettings().getInteractionCooldownMs();
        Map<HologramId, Long> perPlayer = COOLDOWNS.computeIfAbsent(viewer.getUniqueId(), uuid -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        Long last = perPlayer.get(id);
        if (last != null && now - last < cooldownMs) {
            Map<String, String> reps = new HashMap<>();
            reps.put("ms", String.valueOf(cooldownMs - (now - last)));
            plugin.getLanguageManager().send(viewer, "hologram.interact.cooldown", reps);
            return;
        }
        perPlayer.put(id, now);

        // Walk every text line, parse CText, execute the first clickable
        // command that matches the action. (Future: target specific lines
        // by ray-projected line height — Phase 6+ refinement.)
        for (HologramLine line : hologram.getLines()) {
            if (line.getType() != HologramLine.Type.TEXT) {
                continue;
            }
            String template = ((HologramLine.TextLine) line).getTemplate();
            List<CTextParser.Segment> segments = CTextParser.parse(template);
            for (CTextParser.Segment segment : segments) {
                if (!segment.isClickable()) {
                    continue;
                }
                runCommand(plugin, viewer, segment.command, decoded);
                return;
            }
        }
    }

    private static void runCommand(ObxPlugin plugin, Player viewer, String command, InteractDecoder.Decoded decoded) {
        if (command == null || command.isEmpty()) {
            return;
        }
        String prepared = command.replace("{player}", viewer.getName())
                .replace("{uuid}", viewer.getUniqueId().toString());
        boolean asConsole = prepared.startsWith("!");
        if (asConsole) {
            prepared = prepared.substring(1).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
        } else {
            Bukkit.dispatchCommand(viewer, prepared);
        }
    }
}
