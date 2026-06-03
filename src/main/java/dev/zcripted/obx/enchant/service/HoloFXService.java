package dev.zcripted.obx.enchant.service;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Spawns short-lived floating text for the combat enchants — damage numbers, kill
 * banners, and general floating labels ("MARKED", "PHASE", …).
 *
 * <p>Implementation uses invisible marker armor stands with a custom name. They
 * work on every supported version (1.8.8 → 1.21.x) and, unlike per-tick Display
 * transforms, need no version-specific reflection beyond the optional
 * {@code setPersistent} call. Removal is scheduled on the hologram's own region
 * via {@link SchedulerAdapter#runAtEntityLater} so it is Folia-safe; a global
 * active-count cap (derived from {@code combat_global.max_holograms_per_player})
 * trims the oldest hologram if combat gets spammy, and {@link #clear()} removes
 * everything on disable. (A 1.19.4+ {@code TextDisplay} renderer is a possible
 * future enhancement; the armor-stand path is the reliable cross-version core.)
 */
public final class HoloFXService {

    /** Damage-number tint by hit type. */
    public enum DamageType {
        NORMAL(ChatColor.WHITE),
        CRIT(ChatColor.YELLOW),
        BLEED(ChatColor.DARK_RED),
        FROST(ChatColor.AQUA),
        VOID(ChatColor.DARK_PURPLE);

        private final ChatColor color;

        DamageType(ChatColor color) {
            this.color = color;
        }

        public ChatColor color() {
            return color;
        }
    }

    private final Main plugin;
    private final ConcurrentLinkedQueue<ArmorStand> active = new ConcurrentLinkedQueue<ArmorStand>();

    public HoloFXService(Main plugin) {
        this.plugin = plugin;
    }

    private CombatSettings settings() {
        return plugin.getEnchantService() == null ? null : plugin.getEnchantService().getCombatSettings();
    }

    /** Floating damage number above a hit, color-coded by type. Gated by {@code damage_numbers}. */
    public void showDamageNumber(Location loc, double damage, DamageType type) {
        CombatSettings s = settings();
        if (s != null && !s.damageNumbers()) {
            return;
        }
        if (loc == null) {
            return;
        }
        DamageType t = type == null ? DamageType.NORMAL : type;
        String text = t.color().toString() + format(damage);
        spawn(loc.clone().add((Math.random() - 0.5) * 0.6, 1.2, (Math.random() - 0.5) * 0.6), text, 24);
    }

    /** Banner above the killing player for ~2s. Gated by {@code kill_banners} + the player's opt-out. */
    public void showKillBanner(Player killer, String text) {
        CombatSettings s = settings();
        if (s != null && !s.killBanners()) {
            return;
        }
        if (killer == null) {
            return;
        }
        if (plugin.getEnchantService() != null
                && !plugin.getEnchantService().getCombatPrefs().fxEnabled(killer.getUniqueId())) {
            return;
        }
        spawn(killer.getLocation().add(0, 2.4, 0), text, 40);
    }

    /** General-purpose floating label (MARKED, PHASE, REFLECTED, …). */
    public void showFloatingText(Location loc, String text, int durationTicks) {
        if (loc == null) {
            return;
        }
        spawn(loc.clone().add(0, 1.2, 0), text, Math.max(10, durationTicks));
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void spawn(Location loc, String text, int durationTicks) {
        if (loc.getWorld() == null) {
            return;
        }
        if (!withinCap()) {
            trimOldest();
        }
        final ArmorStand stand;
        try {
            stand = loc.getWorld().spawn(loc, ArmorStand.class);
        } catch (Throwable cannotSpawn) {
            return;
        }
        try {
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
            stand.setCustomNameVisible(true);
        } catch (Throwable ignored) {
            // name is the whole point — but never throw out of a cosmetic spawn
        }
        try {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setRemoveWhenFarAway(true);
        } catch (Throwable ignored) {
            // older forks may lack a setter; the floating name still shows
        }
        trySetPersistent(stand, false);
        active.add(stand);

        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler != null) {
            scheduler.runAtEntityLater(stand,
                    new Runnable() {
                        @Override
                        public void run() {
                            removeStand(stand);
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            active.remove(stand);
                        }
                    },
                    Math.max(1, durationTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    removeStand(stand);
                }
            }, Math.max(1, durationTicks));
        }
    }

    private boolean withinCap() {
        CombatSettings s = settings();
        int perPlayer = s == null ? 12 : s.maxHologramsPerPlayer();
        int cap = perPlayer * Math.max(1, Bukkit.getOnlinePlayers().size());
        return active.size() < cap;
    }

    private void trimOldest() {
        removeStand(active.poll());
    }

    private void removeStand(ArmorStand stand) {
        if (stand == null) {
            return;
        }
        active.remove(stand);
        try {
            if (stand.isValid()) {
                stand.remove();
            }
        } catch (Throwable ignored) {
            // entity already gone
        }
    }

    /** Removes every tracked hologram (call on plugin disable). */
    public void clear() {
        ArmorStand stand;
        while ((stand = active.poll()) != null) {
            try {
                stand.remove();
            } catch (Throwable ignored) {
                // best effort
            }
        }
    }

    private void trySetPersistent(ArmorStand stand, boolean value) {
        try {
            stand.getClass().getMethod("setPersistent", boolean.class).invoke(stand, value);
        } catch (Throwable ignored) {
            // setPersistent is 1.14+; short TTL handles cleanup on older versions
        }
    }

    private static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }
}
