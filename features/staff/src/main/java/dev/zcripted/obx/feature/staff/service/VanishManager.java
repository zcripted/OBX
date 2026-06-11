package dev.zcripted.obx.feature.staff.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State and event-handler bundle for the {@code /vanish} staff tool. Tracks which
 * players are currently vanished — and at which {@link Tier} — and applies
 * tier-aware "true vanish" semantics: invisible to non-staff and to specific
 * other vanished players per the visibility matrix below, ignored by hostile
 * mobs, immune to passive damage triggers (combust, food drain), and silently
 * absent from item-pickup events.
 *
 * <h3>Tier model</h3>
 * <ul>
 *   <li><b>Lower tier</b> — granted by {@code obx.vanish}. Visible to other
 *       lower-tier vanished and to higher-tier vanished viewers (so admins can
 *       supervise their staff). Carries an above-head {@code [L]} indicator on
 *       the player's nameplate.</li>
 *   <li><b>Higher tier</b> — granted by {@code obx.vanish.admin}. Invisible
 *       to <em>every</em> other player on the server, including other
 *       higher-tier vanished users. No nameplate indicator (since no one can
 *       see them anyway).</li>
 * </ul>
 *
 * <h3>Visibility matrix</h3>
 * <pre>
 *   Viewer →           Non-vanish  Lower-vanish  Higher-vanish
 *   Target ↓
 *   Non-vanish:           SEE         SEE           SEE
 *   Lower-vanish:        HIDDEN       SEE           SEE (with [L] prefix)
 *   Higher-vanish:       HIDDEN      HIDDEN        HIDDEN
 * </pre>
 *
 * <p>"True vanish" is implemented via Bukkit's
 * {@link Player#hidePlayer(Plugin, Player)} (1.13+) where available, falling
 * back to the legacy {@link Player#hidePlayer(Player)} on 1.8 – 1.12 servers.
 * Both reflective lookups are resolved once and cached so the toggle path stays
 * out of reflection on every invocation.
 *
 * <p>Vanished staff stay vanished until they log out (state lives in-memory
 * only — restart and quit clear it, matching the typical EssentialsX
 * expectation that vanish is a session-local toggle).
 *
 * <p>While a player is vanished, an action-bar indicator is refreshed once per
 * second so the operator always sees their current state and tier without
 * having to reopen any menu — the "clean / modern / organized" running
 * indicator the design calls for.
 */
public class VanishManager implements Listener, dev.zcripted.obx.api.staff.VanishApi {

    /** Permission strings — kept centralised so command + manager agree. */
    public static final String PERM_VANISH = "obx.vanish";
    public static final String PERM_VANISH_HIGHER = "obx.vanish.admin";

    /** Bukkit scoreboard-team name used to attach the lower-tier nameplate prefix. */
    private static final String TEAM_LOWER = "sf_vanish_low";

    /** Above-head nameplate prefix for lower-tier vanished players. Kept as a
     *  constant rather than a translatable string because Bukkit team prefixes
     *  are server-global (not per-viewer) and capped at 16 chars on legacy
     *  versions — a translator override would either bypass that limit or
     *  collide with non-English clients. The "[L]" marker is symbolic enough
     *  to read in any locale. */
    private static final String TEAM_LOWER_PREFIX = "§7[§8L§7] ";

    /** Action-bar refresh cadence. The vanilla action bar fades after ~3s, so
     *  20 ticks (1 s) is a comfortable margin that keeps the indicator always on
     *  screen without spamming packets. */
    private static final long ACTION_BAR_PERIOD_TICKS = 20L;

    public enum Tier {
        LOWER,
        HIGHER
    }

    private final ObxPlugin plugin;
    private final Map<UUID, Tier> vanished = new ConcurrentHashMap<UUID, Tier>();

    private final Method hidePlayerWithPlugin;
    private final Method showPlayerWithPlugin;
    private final Method hidePlayerLegacy;
    private final Method showPlayerLegacy;

    private dev.zcripted.obx.core.platform.scheduler.CancellableTask actionBarTask;

    public VanishManager(ObxPlugin plugin) {
        this.plugin = plugin;
        this.hidePlayerWithPlugin = lookup(Player.class, "hidePlayer", Plugin.class, Player.class);
        this.showPlayerWithPlugin = lookup(Player.class, "showPlayer", Plugin.class, Player.class);
        this.hidePlayerLegacy = lookup(Player.class, "hidePlayer", Player.class);
        this.showPlayerLegacy = lookup(Player.class, "showPlayer", Player.class);
    }

    /**
     * Starts the action-bar refresh loop. Idempotent — calling twice is a no-op.
     * Should be called exactly once during plugin enable, after the scheduler
     * adapter is available.
     */
    public void start() {
        if (actionBarTask != null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        actionBarTask = scheduler.runRepeating(this::refreshActionBars,
                ACTION_BAR_PERIOD_TICKS, ACTION_BAR_PERIOD_TICKS);
    }

    public void stop() {
        if (actionBarTask != null) {
            try {
                actionBarTask.cancel();
            } catch (Throwable ignored) {
            }
            actionBarTask = null;
        }
        // On disable, also reveal everyone so leftover hide-state doesn't
        // persist into a reload of the plugin (Bukkit caches per-plugin
        // hide state keyed by Plugin instance).
        for (Map.Entry<UUID, Tier> entry : new HashMap<UUID, Tier>(vanished).entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) {
                continue;
            }
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(p.getUniqueId())) {
                    showFrom(viewer, p);
                }
            }
            leaveLowerTeam(p);
        }
        vanished.clear();
    }

    /**
     * Toggles vanish for {@code player}. Returns {@code true} when vanish is now
     * <em>active</em> after the toggle, {@code false} when it was disabled. The
     * tier is resolved at the moment of activation from the player's
     * permissions — re-toggling re-resolves, so a permission change between
     * toggles is picked up on the next enable.
     */
    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (vanished.remove(uuid) != null) {
            // Vanish was active — drop nameplate prefix, then re-evaluate
            // visibility against everyone (player is now non-vanished, so
            // everyone can see them and they can see all non-vanished
            // players normally).
            leaveLowerTeam(player);
            rebuildVisibility(player);
            // Clear the running action bar by overwriting with empty text once;
            // the next vanilla action bar update from any other source will
            // overwrite this within 3 s, and we explicitly send blank to avoid
            // the stale "Vanished" line lingering until fade.
            ComponentMessenger.sendActionBar(player, "");
            return false;
        }
        Tier tier = resolveTier(player);
        vanished.put(uuid, tier);
        if (tier == Tier.LOWER) {
            joinLowerTeam(player);
        }
        rebuildVisibility(player);
        // Drop any active mob aggro / fire so the toggle feels clean.
        player.setFireTicks(0);
        // Send the action-bar indicator immediately so the operator gets
        // instant feedback without waiting up to a tick for the periodic
        // refresh.
        sendActionBarFor(player, tier);
        return true;
    }

    public boolean isVanished(UUID uuid) {
        return uuid != null && vanished.containsKey(uuid);
    }

    public boolean isVanished(Player player) {
        return player != null && vanished.containsKey(player.getUniqueId());
    }

    public Tier getTier(UUID uuid) {
        return uuid == null ? null : vanished.get(uuid);
    }

    public Tier getTier(Player player) {
        return player == null ? null : vanished.get(player.getUniqueId());
    }

    private Tier resolveTier(Player player) {
        return player.hasPermission(PERM_VANISH_HIGHER) ? Tier.HIGHER : Tier.LOWER;
    }

    /**
     * Re-applies the bidirectional visibility for {@code subject} against every
     * other online player based on the current tier map. Called when subject's
     * vanish state changes (toggle on or off).
     */
    private void rebuildVisibility(Player subject) {
        UUID subjectUuid = subject.getUniqueId();
        Tier subjectTier = vanished.get(subjectUuid);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == null || other.getUniqueId().equals(subjectUuid)) {
                continue;
            }
            Tier otherTier = vanished.get(other.getUniqueId());
            applyPair(other, subject, otherTier, subjectTier);
            applyPair(subject, other, subjectTier, otherTier);
        }
    }

    /** Applies "viewer can/can't see target" per the visibility matrix. */
    private void applyPair(Player viewer, Player target, Tier viewerTier, Tier targetTier) {
        if (canSee(viewerTier, targetTier)) {
            showFrom(viewer, target);
        } else {
            hideFrom(viewer, target);
        }
    }

    /** Pure visibility-matrix predicate — see class javadoc for the table. */
    private static boolean canSee(Tier viewerTier, Tier targetTier) {
        if (targetTier == null) {
            return true; // non-vanished targets are visible to everyone
        }
        if (viewerTier == null) {
            return false; // non-vanished viewers can never see vanished targets
        }
        if (targetTier == Tier.HIGHER) {
            return false; // higher-tier vanished is invisible to every other vanished viewer
        }
        // targetTier == LOWER, viewer is vanished (LOWER or HIGHER) → visible
        return true;
    }

    private void hideFrom(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }
        if (hidePlayerWithPlugin != null) {
            try {
                hidePlayerWithPlugin.invoke(viewer, plugin, target);
                return;
            } catch (Throwable ignored) {
                // fall through to legacy
            }
        }
        if (hidePlayerLegacy != null) {
            try {
                hidePlayerLegacy.invoke(viewer, target);
            } catch (Throwable ignored) {
                // platform doesn't expose either overload — vanish becomes a no-op,
                // which is the safest possible failure mode.
            }
        }
    }

    private void showFrom(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }
        if (showPlayerWithPlugin != null) {
            try {
                showPlayerWithPlugin.invoke(viewer, plugin, target);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (showPlayerLegacy != null) {
            try {
                showPlayerLegacy.invoke(viewer, target);
            } catch (Throwable ignored) {
            }
        }
    }

    private static Method lookup(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * Adds {@code player} to the shared {@link #TEAM_LOWER} scoreboard team so
     * the configured prefix renders above their head. The team is created on
     * first use with the prefix sourced from the language file (default
     * {@code §7[§8L§7] }) — using the main scoreboard so every viewer that can
     * see the lower-tier player picks up the prefix automatically. The team
     * prefix is intentionally short to fit the legacy 16-char limit on
     * pre-1.13 servers.
     */
    private void joinLowerTeam(Player player) {
        Team team = ensureLowerTeam();
        if (team == null) {
            return;
        }
        try {
            team.addEntry(player.getName());
        } catch (Throwable ignored) {
            // 1.7-style scoreboards exposed addPlayer(OfflinePlayer); modern
            // ones use addEntry(String). The compile-target only sees one
            // method, so a runtime Throwable here means the platform is
            // missing it — silently degrade (no above-head indicator).
        }
    }

    private void leaveLowerTeam(Player player) {
        Team team = lookupLowerTeam();
        if (team == null) {
            return;
        }
        try {
            team.removeEntry(player.getName());
        } catch (Throwable ignored) {
        }
    }

    private Team ensureLowerTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return null;
        }
        Scoreboard board = manager.getMainScoreboard();
        Team existing = board.getTeam(TEAM_LOWER);
        if (existing != null) {
            return existing;
        }
        try {
            Team created = board.registerNewTeam(TEAM_LOWER);
            created.setPrefix(TEAM_LOWER_PREFIX);
            return created;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Team lookupLowerTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return null;
        }
        return manager.getMainScoreboard().getTeam(TEAM_LOWER);
    }

    private void refreshActionBars() {
        if (vanished.isEmpty()) {
            return;
        }
        // The action bar must be sent on each player's own region thread under Folia; this loop fires
        // on the global thread, so dispatch the per-player send to that player's region.
        final boolean folia = plugin.getSchedulerAdapter() != null && plugin.getSchedulerAdapter().isFolia();
        for (Map.Entry<UUID, Tier> entry : vanished.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) {
                continue;
            }
            final Player player = p;
            final Tier tier = entry.getValue();
            Runnable work = () -> sendActionBarFor(player, tier);
            if (folia) {
                plugin.getSchedulerAdapter().runAtEntity(player, work);
            } else {
                work.run();
            }
        }
    }

    private void sendActionBarFor(Player player, Tier tier) {
        String key = tier == Tier.HIGHER
                ? "player.vanish.action-bar.higher"
                : "player.vanish.action-bar.lower";
        String message = plugin.getLanguageManager().get(player, key);
        if (message == null || message.isEmpty()) {
            message = tier == Tier.HIGHER
                    ? "§c● §7VANISHED §8| §fHIGHER TIER §8| §7Invisible to all"
                    : "§a● §7VANISHED §8| §fLOWER TIER §8| §7Visible to staff";
        }
        message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        ComponentMessenger.sendActionBar(player, message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        // New arrival is non-vanished by definition; apply the matrix so they
        // can't see any currently-vanished staff.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(joining.getUniqueId())) {
                continue;
            }
            Tier onlineTier = vanished.get(online.getUniqueId());
            applyPair(joining, online, null, onlineTier);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Drop the vanish flag if the staff member logs out — they'll have to
        // re-toggle on next login. Avoids surprising re-vanish across sessions
        // and keeps the team membership clean.
        Player p = event.getPlayer();
        Tier had = vanished.remove(p.getUniqueId());
        if (had == Tier.LOWER) {
            leaveLowerTeam(p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getTarget();
        if (isVanished(target)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getEntity();
        if (isVanished(target)) {
            event.setCancelled(true);
            target.setFireTicks(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getEntity();
        if (!isVanished(target)) {
            return;
        }
        // Vanished staff are untouchable by other entities, including
        // projectiles fired at them while they were already invisible (mob
        // aggro is already suppressed in onMobTarget but ricochets and AOE
        // can still hit). LivingEntity is the broadest type with aggro;
        // Mob (a 1.13+ subtype) is intentionally avoided so this code stays
        // compile-compatible with the 1.8.8 baseline.
        event.setCancelled(true);
        if (event.getDamager() instanceof LivingEntity && !(event.getDamager() instanceof Player)) {
            try {
                ((org.bukkit.entity.Creature) event.getDamager()).setTarget(null);
            } catch (Throwable ignored) {
                // Damager isn't a Creature (e.g. ranged Skeleton arrow returns its shooter
                // which is a Creature on 1.8 but not always); skip aggro reset on failure.
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (isVanished(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && isVanished((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }
}