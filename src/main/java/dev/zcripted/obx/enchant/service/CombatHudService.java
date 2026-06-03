package dev.zcripted.obx.enchant.service;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.enchant.listener.CombatSupport;
import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.util.Potions;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live combat HUD: a follower-hologram (one or more stacked invisible marker armor
 * stands) that trails any target — mob <em>or</em> player — showing a health bar on
 * the top line, plus a Bloodletter blood-loss tag and a Battle Roar / Bonecrusher
 * debuff readout (Weakness/Slowness level + remaining time) on a second line. Also
 * drives the attacker's Berserker / Bloodthirst action-bar HUD.
 *
 * <p>A single repeating task repositions every active hologram each tick and rebuilds
 * its lines from the still-active contributions, spawning/removing stand "lines" to
 * match, then removes everything once it all expires. Each contribution carries its
 * own expiry, so a target can be tracked by several enchants at once.
 *
 * <p>Per-entity work is try/caught: on Folia the global-region task may not be allowed
 * to touch an off-region entity, in which case that target is skipped for the tick
 * (the session expires normally). Holograms are short-lived marker stands with no
 * persistence; {@link #clear()} removes them all and stops the task on disable.
 */
public final class CombatHudService {

    private static final char HEART = '❤';
    private static final int HEART_SLOTS = 10;
    /** How often (in ticks) the player action-bar HUD is refreshed (and animated). */
    private static final int ACTIONBAR_PERIOD = 4;
    /** Vertical gap between stacked hologram lines. */
    private static final double LINE_SPACING = 0.28;

    private final Main plugin;
    private final Map<UUID, EntitySession> entities = new ConcurrentHashMap<UUID, EntitySession>();
    private final Map<UUID, PlayerHud> playerHuds = new ConcurrentHashMap<UUID, PlayerHud>();
    private SchedulerAdapter.CancellableTask task;
    private int ticks;

    public CombatHudService(Main plugin) {
        this.plugin = plugin;
    }

    /** Starts the HUD updater (idempotent). Call once in onEnable after the scheduler exists. */
    public void start() {
        if (task != null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        task = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 1L, 1L);
    }

    // ── Public tracking API (called from the combat listeners) ────────────────

    /** Show the target's health bar above it for {@code durationMs}. */
    public void trackHealth(LivingEntity target, long durationMs) {
        EntitySession s = session(target);
        if (s != null) {
            s.healthUntil = Math.max(s.healthUntil, System.currentTimeMillis() + durationMs);
        }
    }

    /** Show a Bloodletter blood-loss tag (and the dropping health bar) for {@code durationMs}. */
    public void trackBleed(LivingEntity target, double damagePerSecond, long durationMs) {
        EntitySession s = session(target);
        if (s != null) {
            long until = System.currentTimeMillis() + durationMs;
            s.bleedUntil = Math.max(s.bleedUntil, until);
            s.healthUntil = Math.max(s.healthUntil, until);
            s.bleedPerSec = damagePerSecond;
        }
    }

    /** Show a Weakness/Slowness cooldown readout (read live each tick) for {@code durationMs}. */
    public void trackDebuff(LivingEntity target, long durationMs) {
        EntitySession s = session(target);
        if (s != null) {
            s.debuffUntil = Math.max(s.debuffUntil, System.currentTimeMillis() + durationMs);
        }
    }

    /** Refresh the attacker's Berserker's Rage damage-percent action-bar segment. */
    public void berserkerBar(Player attacker, String legacyText, long durationMs) {
        if (attacker == null) {
            return;
        }
        PlayerHud hud = playerHud(attacker.getUniqueId());
        hud.berserkerText = legacyText;
        hud.berserkerUntil = System.currentTimeMillis() + durationMs;
        sendNow(attacker, hud);
    }

    /** Refresh the attacker's animated Bloodthirst "restoring health" action-bar segment. */
    public void bloodthirstBar(Player attacker, double healPercent, long durationMs) {
        if (attacker == null) {
            return;
        }
        PlayerHud hud = playerHud(attacker.getUniqueId());
        hud.bloodthirstPercent = healPercent;
        hud.bloodthirstUntil = System.currentTimeMillis() + durationMs;
        sendNow(attacker, hud);
    }

    private PlayerHud playerHud(UUID id) {
        PlayerHud hud = playerHuds.get(id);
        if (hud == null) {
            hud = new PlayerHud();
            playerHuds.put(id, hud);
        }
        return hud;
    }

    private void sendNow(Player player, PlayerHud hud) {
        String line = hud.compose(System.currentTimeMillis(), ticks);
        if (line != null) {
            CombatSupport.actionBar(plugin.getEnchantService(), player, line);
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    private void tick() {
        long now = System.currentTimeMillis();
        ticks++;
        for (Iterator<Map.Entry<UUID, EntitySession>> it = entities.entrySet().iterator(); it.hasNext(); ) {
            EntitySession s = it.next().getValue();
            try {
                LivingEntity target = s.target;
                if (target == null || target.isDead() || !target.isValid() || s.expiry() < now) {
                    s.removeStands();
                    it.remove();
                    continue;
                }
                List<String> lines = s.buildLines(now);
                if (lines.isEmpty()) {
                    s.removeStands();
                    it.remove();
                    continue;
                }
                s.render(lines);
            } catch (Throwable offRegionOrGone) {
                // Folia off-region access or a transient error — skip this tick, let it expire.
            }
        }

        if (ticks % ACTIONBAR_PERIOD == 0) {
            for (Iterator<Map.Entry<UUID, PlayerHud>> it = playerHuds.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, PlayerHud> entry = it.next();
                PlayerHud hud = entry.getValue();
                if (hud.expiry() < now) {
                    it.remove();
                    continue;
                }
                try {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        it.remove();
                        continue;
                    }
                    String line = hud.compose(now, ticks);
                    if (line != null) {
                        CombatSupport.actionBar(plugin.getEnchantService(), player, line);
                    }
                } catch (Throwable ignored) {
                    // skip
                }
            }
        }
    }

    /** Removes every hologram and stops the updater (call on plugin disable). */
    public void clear() {
        for (EntitySession s : entities.values()) {
            try {
                s.removeStands();
            } catch (Throwable ignored) {
                // best effort
            }
        }
        entities.clear();
        playerHuds.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // ── Session management ──────────────────────────────────────────────────────

    private EntitySession session(LivingEntity target) {
        if (target == null || target.isDead() || !target.isValid()) {
            return null;
        }
        EntitySession existing = entities.get(target.getUniqueId());
        if (existing != null) {
            return existing;
        }
        EntitySession s = new EntitySession(target);
        entities.put(target.getUniqueId(), s);
        return s;
    }

    private static double headHeight(LivingEntity target) {
        try {
            return target.getHeight();
        } catch (Throwable legacy) {
            return target instanceof Player ? 1.8 : 1.0;
        }
    }

    private static ArmorStand spawnStand(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        try {
            ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setCustomNameVisible(true);
            stand.setRemoveWhenFarAway(true);
            trySetPersistent(stand);
            return stand;
        } catch (Throwable cannotSpawn) {
            return null;
        }
    }

    private static void trySetPersistent(ArmorStand stand) {
        try {
            stand.getClass().getMethod("setPersistent", boolean.class).invoke(stand, false);
        } catch (Throwable ignored) {
            // setPersistent is 1.14+; the short-lived marker is cleaned up regardless
        }
    }

    private static void removeStand(ArmorStand stand) {
        if (stand == null) {
            return;
        }
        try {
            if (stand.isValid()) {
                stand.remove();
            }
        } catch (Throwable ignored) {
            // already gone
        }
    }

    // ── Rendering helpers ──────────────────────────────────────────────────────

    private static String healthBar(LivingEntity e) {
        double max = maxHealth(e);
        double hp = Math.max(0.0, e.getHealth());
        double ratio = max <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, hp / max));
        int filled = (int) Math.round(ratio * HEART_SLOTS);
        ChatColor color = ratio > 0.5 ? ChatColor.GREEN : (ratio > 0.25 ? ChatColor.GOLD : ChatColor.RED);
        StringBuilder bar = new StringBuilder();
        bar.append(color);
        for (int i = 0; i < filled; i++) {
            bar.append(HEART);
        }
        bar.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < HEART_SLOTS; i++) {
            bar.append(HEART);
        }
        bar.append(" &f").append(fmt(hp)).append("&8/&7").append(fmt(max));
        return bar.toString();
    }

    private static String debuffReadout(LivingEntity e) {
        PotionEffect weakness = effect(e, Potions.WEAKNESS);
        PotionEffect slowness = effect(e, Potions.SLOWNESS);
        if (weakness == null && slowness == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int remaining = 0;
        if (weakness != null) {
            out.append("&cWeakness ").append(CustomEnchant.roman(weakness.getAmplifier() + 1));
            remaining = Math.max(remaining, weakness.getDuration());
        }
        if (slowness != null) {
            if (out.length() > 0) {
                out.append("&8 · ");
            }
            out.append("&bSlowness ").append(CustomEnchant.roman(slowness.getAmplifier() + 1));
            remaining = Math.max(remaining, slowness.getDuration());
        }
        out.append("&8 (&7").append(String.format(Locale.US, "%.1fs", remaining / 20.0)).append("&8)");
        return out.toString();
    }

    private static PotionEffect effect(LivingEntity e, String[] candidates) {
        PotionEffectType type = Potions.type(candidates);
        if (type == null) {
            return null;
        }
        try {
            for (PotionEffect pe : e.getActivePotionEffects()) {
                if (pe.getType().equals(type)) {
                    return pe;
                }
            }
        } catch (Throwable ignored) {
            // off-region read on Folia — treat as absent this tick
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static double maxHealth(LivingEntity e) {
        try {
            return e.getMaxHealth();
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    private static String fmt(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    /** Animated "loading" health-restore segment for Bloodthirst (an 8-cell bar that loops). */
    private static String bloodthirstFrame(double healPercent, int ticks) {
        int cells = 8;
        int lit = (ticks / ACTIONBAR_PERIOD) % (cells + 1);
        StringBuilder bar = new StringBuilder("&c❤ &7Restoring &c+");
        bar.append((int) Math.round(healPercent * 100)).append("% &8[");
        for (int i = 0; i < cells; i++) {
            bar.append(i < lit ? "&c|" : "&8|");
        }
        bar.append("&8]");
        return bar.toString();
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private static final class EntitySession {
        private final LivingEntity target;
        private final List<ArmorStand> stands = new ArrayList<ArmorStand>();
        private long healthUntil;
        private long bleedUntil;
        private long debuffUntil;
        private double bleedPerSec;

        EntitySession(LivingEntity target) {
            this.target = target;
        }

        long expiry() {
            return Math.max(healthUntil, Math.max(bleedUntil, debuffUntil));
        }

        /** The hologram lines, top to bottom: health (+bleed) first, debuff cooldown second. */
        List<String> buildLines(long now) {
            List<String> lines = new ArrayList<String>();
            if (now < healthUntil || now < bleedUntil) {
                String health = healthBar(target);
                if (now < bleedUntil) {
                    health = health + "  &4☠ &c-" + fmt(bleedPerSec) + "/s";
                }
                lines.add(health);
            }
            if (now < debuffUntil) {
                String readout = debuffReadout(target);
                if (readout != null) {
                    lines.add(readout);
                }
            }
            return lines;
        }

        /** Spawns/removes stacked stands to match the line count, repositions, and sets text. */
        void render(List<String> lines) {
            while (stands.size() < lines.size()) {
                ArmorStand spawned = spawnStand(target.getLocation().add(0, headHeight(target) + 0.35, 0));
                if (spawned == null) {
                    break;
                }
                stands.add(spawned);
            }
            while (stands.size() > lines.size()) {
                removeStand(stands.remove(stands.size() - 1));
            }
            int n = stands.size();
            if (n == 0) {
                return;
            }
            double base = headHeight(target) + 0.35;
            double topY = base + (n - 1) * LINE_SPACING;
            Location origin = target.getLocation();
            for (int i = 0; i < n; i++) {
                ArmorStand stand = stands.get(i);
                if (stand == null || !stand.isValid()) {
                    continue;
                }
                try {
                    stand.teleport(origin.clone().add(0, topY - i * LINE_SPACING, 0));
                } catch (Throwable ignored) {
                    // cross-region teleport on Folia / unloaded chunk — retry next tick
                }
                try {
                    stand.setCustomName(ChatColor.translateAlternateColorCodes('&', lines.get(i)));
                } catch (Throwable ignored) {
                    // name set failed — leave previous text
                }
            }
        }

        void removeStands() {
            for (ArmorStand stand : stands) {
                removeStand(stand);
            }
            stands.clear();
        }
    }

    private static final class PlayerHud {
        private String berserkerText;
        private long berserkerUntil;
        private double bloodthirstPercent;
        private long bloodthirstUntil;

        long expiry() {
            return Math.max(berserkerUntil, bloodthirstUntil);
        }

        String compose(long now, int ticks) {
            StringBuilder line = new StringBuilder();
            if (now < berserkerUntil && berserkerText != null) {
                line.append(berserkerText);
            }
            if (now < bloodthirstUntil) {
                if (line.length() > 0) {
                    line.append("  &8|  ");
                }
                line.append(bloodthirstFrame(bloodthirstPercent, ticks));
            }
            return line.length() == 0 ? null : line.toString();
        }
    }
}
