package dev.zcripted.obx.feature.enchant.effect;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player runtime state for Combat-category enchantments that need memory
 * across events: Soulreaver kill-stacks (with time-based decay) and sprint-start
 * timestamps (for Momentum). In-memory for the session — appropriate for
 * transient combat buffs; cleared on quit.
 */
public final class CombatState {

    private static final class Stacks {
        int count;
        long expiresAt;
    }

    private static final class Combo {
        UUID target;
        int count;
        long lastMs;
    }

    private static final class Vengeance {
        UUID attacker;
        long untilMs;
        boolean critUsed;
    }

    private static final class Mark {
        double bonus;
        long untilMs;
    }

    private static final class Streak {
        int count;
        long lastMs;
    }

    private static final class Tether {
        UUID binder;
        long untilMs;
        double vulnerability;
        double backlash;
    }

    private final ConcurrentHashMap<UUID, Stacks> soulreaver = new ConcurrentHashMap<UUID, Stacks>();
    private final ConcurrentHashMap<UUID, Long> sprintStart = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentHashMap<UUID, Long> stunnedUntil = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentHashMap<UUID, Double> mirrorQueued = new ConcurrentHashMap<UUID, Double>();
    private final ConcurrentHashMap<UUID, Long> weaponSwapAt = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentHashMap<UUID, Long> weaponSwapConsumed = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentHashMap<UUID, Combo> combos = new ConcurrentHashMap<UUID, Combo>();
    private final ConcurrentHashMap<UUID, Double> nextSwingBonus = new ConcurrentHashMap<UUID, Double>();
    private final ConcurrentHashMap<UUID, long[]> grit = new ConcurrentHashMap<UUID, long[]>();
    private final ConcurrentHashMap<UUID, Vengeance> vengeance = new ConcurrentHashMap<UUID, Vengeance>();
    private final ConcurrentHashMap<UUID, Mark> marks = new ConcurrentHashMap<UUID, Mark>();
    private final ConcurrentHashMap<UUID, Streak> killstreaks = new ConcurrentHashMap<UUID, Streak>();
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<String, Long>();
    private final ConcurrentHashMap<UUID, Tether> tethers = new ConcurrentHashMap<UUID, Tether>();
    private final ConcurrentHashMap<UUID, Long> pearlLock = new ConcurrentHashMap<UUID, Long>();

    // ── Soulreaver kill-stacks ──────────────────────────────────────────────

    /** Current valid stack count (0 if expired). */
    public int soulreaverStacks(UUID id) {
        Stacks s = soulreaver.get(id);
        if (s == null || s.expiresAt < System.currentTimeMillis()) {
            return 0;
        }
        return s.count;
    }

    /** Adds one stack (capped) and refreshes the decay timer. */
    public void addSoulreaverStack(UUID id, int max, long decayMillis) {
        long now = System.currentTimeMillis();
        Stacks s = soulreaver.get(id);
        if (s == null || s.expiresAt < now) {
            s = new Stacks();
            soulreaver.put(id, s);
        }
        s.count = Math.min(Math.max(1, max), s.count + 1);
        s.expiresAt = now + decayMillis;
    }

    // ── Sprint tracking (Momentum) ──────────────────────────────────────────

    public void startSprint(UUID id) {
        sprintStart.put(id, System.currentTimeMillis());
    }

    public void stopSprint(UUID id) {
        sprintStart.remove(id);
    }

    /** Seconds spent sprinting continuously, or 0 if not sprinting. */
    public double sprintSeconds(UUID id) {
        Long start = sprintStart.get(id);
        return start == null ? 0.0 : Math.max(0.0, (System.currentTimeMillis() - start) / 1000.0);
    }

    // ── Stunlock ────────────────────────────────────────────────────────────

    public void setStunned(UUID id, long durationMillis) {
        stunnedUntil.put(id, System.currentTimeMillis() + Math.max(0L, durationMillis));
    }

    public boolean isStunned(UUID id) {
        Long until = stunnedUntil.get(id);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            stunnedUntil.remove(id); // drop the expired entry instead of leaving it to leak
            return false;
        }
        return true;
    }

    // ── Mirror Edge (queued reflect) ────────────────────────────────────────

    public void queueMirror(UUID id, double reflectPercent) {
        mirrorQueued.put(id, reflectPercent);
    }

    public boolean hasMirror(UUID id) {
        return mirrorQueued.containsKey(id);
    }

    /** Returns the queued reflect percent and clears it (0 if none was queued). */
    public double consumeMirror(UUID id) {
        Double percent = mirrorQueued.remove(id);
        return percent == null ? 0.0 : percent;
    }

    // ── Quickdraw (weapon-swap window) ──────────────────────────────────────

    public void markWeaponSwap(UUID id) {
        weaponSwapAt.put(id, System.currentTimeMillis());
    }

    /** True if a Quickdraw window is still open and hasn't been consumed for this swap. */
    public boolean tryConsumeQuickdraw(UUID id, long windowMillis) {
        Long swap = weaponSwapAt.get(id);
        if (swap == null || System.currentTimeMillis() - swap > windowMillis) {
            return false;
        }
        Long consumed = weaponSwapConsumed.get(id);
        if (swap.equals(consumed)) {
            return false;
        }
        weaponSwapConsumed.put(id, swap);
        return true;
    }

    // ── Combo Strike ────────────────────────────────────────────────────────

    /** Registers a hit on {@code target}, resetting the combo if it changed or timed out. */
    public int registerCombo(UUID id, UUID target, long windowMillis) {
        long now = System.currentTimeMillis();
        Combo combo = combos.get(id);
        if (combo == null || !target.equals(combo.target) || now - combo.lastMs > windowMillis) {
            combo = new Combo();
            combo.target = target;
            combo.count = 0;
            combos.put(id, combo);
        }
        combo.count++;
        combo.lastMs = now;
        return combo.count;
    }

    public void resetCombo(UUID id) {
        combos.remove(id);
    }

    // ── Manaburn (next-swing bonus) ─────────────────────────────────────────

    public void addNextSwing(UUID id, double bonus) {
        nextSwingBonus.put(id, bonus);
    }

    public double consumeNextSwing(UUID id) {
        Double bonus = nextSwingBonus.remove(id);
        return bonus == null ? 0.0 : bonus;
    }

    // ── Brawler's Grit (reactive next-attack window) ────────────────────────

    public void readyGrit(UUID id, long windowMillis, int level) {
        grit.put(id, new long[]{System.currentTimeMillis() + windowMillis, level});
    }

    /** Returns the grit level if the window is open (consuming it), else 0. */
    public int consumeGrit(UUID id) {
        long[] data = grit.remove(id);
        if (data == null || data[0] < System.currentTimeMillis()) {
            return 0;
        }
        return (int) data[1];
    }

    // ── Vengeance (last attacker) ───────────────────────────────────────────

    public void markAttacker(UUID victim, UUID attacker, long windowMillis) {
        Vengeance v = new Vengeance();
        v.attacker = attacker;
        v.untilMs = System.currentTimeMillis() + windowMillis;
        v.critUsed = false;
        vengeance.put(victim, v);
    }

    public boolean isVengeanceTarget(UUID wielder, UUID target) {
        Vengeance v = vengeance.get(wielder);
        return v != null && v.untilMs > System.currentTimeMillis() && target.equals(v.attacker);
    }

    /** True once per mark — the first Vengeance hit (used for the Lv3 guaranteed crit). */
    public boolean consumeVengeanceCrit(UUID wielder) {
        Vengeance v = vengeance.get(wielder);
        if (v != null && !v.critUsed) {
            v.critUsed = true;
            return true;
        }
        return false;
    }

    // ── Hunter's Mark (cross-source damage mark) ────────────────────────────

    public void markTarget(UUID id, double bonus, long durationMillis) {
        Mark mark = new Mark();
        mark.bonus = bonus;
        mark.untilMs = System.currentTimeMillis() + durationMillis;
        marks.put(id, mark);
    }

    /** Extra damage multiplier-add for a marked target, or 0 if unmarked/expired. */
    public double markBonus(UUID id) {
        Mark mark = marks.get(id);
        if (mark == null) {
            return 0.0;
        }
        if (mark.untilMs <= System.currentTimeMillis()) {
            marks.remove(id); // drop the expired entry instead of leaving it to leak
            return 0.0;
        }
        return mark.bonus;
    }

    // ── Killstreak ──────────────────────────────────────────────────────────

    /** Registers a kill and returns the current streak length (reset if the window lapsed). */
    public int registerKill(UUID id, long windowMillis) {
        long now = System.currentTimeMillis();
        Streak streak = killstreaks.get(id);
        if (streak == null || now - streak.lastMs > windowMillis) {
            streak = new Streak();
            killstreaks.put(id, streak);
        }
        streak.count++;
        streak.lastMs = now;
        return streak.count;
    }

    /** Current streak length if still within {@code windowMillis}, else 0. */
    public int killstreak(UUID id, long windowMillis) {
        Streak streak = killstreaks.get(id);
        return (streak != null && System.currentTimeMillis() - streak.lastMs <= windowMillis) ? streak.count : 0;
    }

    // ── Generic per-player cooldowns (e.g. Battle Roar) ─────────────────────

    public boolean onCooldown(UUID id, String key) {
        Long until = cooldowns.get(id + "|" + key);
        return until != null && until > System.currentTimeMillis();
    }

    public long cooldownSeconds(UUID id, String key) {
        Long until = cooldowns.get(id + "|" + key);
        return until == null ? 0 : Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public void setCooldown(UUID id, String key, long millis) {
        cooldowns.put(id + "|" + key, System.currentTimeMillis() + millis);
    }

    // ── Soul Tether (cross-event link) ──────────────────────────────────────

    /**
     * Tethers {@code target} to {@code binder}: while the tether holds, attackers
     * deal {@code vulnerability} extra damage to the target, and a fraction
     * ({@code backlash}) of the target's outgoing damage is siphoned back to the binder.
     */
    public void tether(UUID target, UUID binder, long durationMs, double vulnerability, double backlash) {
        Tether t = new Tether();
        t.binder = binder;
        t.untilMs = System.currentTimeMillis() + durationMs;
        t.vulnerability = vulnerability;
        t.backlash = backlash;
        tethers.put(target, t);
    }

    private Tether validTether(UUID target) {
        Tether t = tethers.get(target);
        if (t == null) {
            return null;
        }
        if (t.untilMs < System.currentTimeMillis()) {
            tethers.remove(target);
            return null;
        }
        return t;
    }

    /** Extra damage multiplier-add an attacker deals to a tethered target, or 0. */
    public double tetherVulnerability(UUID target) {
        Tether t = validTether(target);
        return t == null ? 0.0 : t.vulnerability;
    }

    /** The binder who tethered {@code target}, or null if untethered/expired. */
    public UUID tetherBinder(UUID target) {
        Tether t = validTether(target);
        return t == null ? null : t.binder;
    }

    /** Fraction of a tethered target's outgoing damage siphoned to its binder, or 0. */
    public double tetherBacklash(UUID target) {
        Tether t = validTether(target);
        return t == null ? 0.0 : t.backlash;
    }

    // ── Spectral Bind (ender-pearl lock) ────────────────────────────────────

    public void lockPearls(UUID id, long durationMs) {
        pearlLock.put(id, System.currentTimeMillis() + durationMs);
    }

    public boolean pearlsLocked(UUID id) {
        Long until = pearlLock.get(id);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            pearlLock.remove(id);
            return false;
        }
        return true;
    }

    /**
     * Purges all state that can be keyed by an arbitrary entity (not just players) when that
     * entity dies. The target-keyed maps ({@link #marks}, {@link #stunnedUntil},
     * {@link #tethers}) are overwhelmingly populated with <em>mob</em> UUIDs, which
     * {@link #clear(UUID)} (wired only to player quit) never reaches — so without this they
     * grow without bound on a long-running server. Call from {@code EntityDeathEvent}.
     */
    /**
     * Periodic janitor: drops time-expired entries from the entity-keyed maps. A mob that
     * <em>despawns</em> (chunk unload / distance / removal) without firing {@code EntityDeathEvent}
     * has a UUID that is never read again, so {@link #removeEntity(UUID)} and the per-read expiry
     * checks never reach it — without this sweep those entries would persist for the server's
     * lifetime. Wire to a low-frequency repeating task (entries are short-lived, so a few minutes
     * is ample). Safe on ConcurrentHashMap (weakly-consistent iteration).
     */
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        marks.values().removeIf(m -> m.untilMs <= now);
        stunnedUntil.values().removeIf(until -> until <= now);
        tethers.values().removeIf(t -> t.untilMs <= now);
        vengeance.values().removeIf(v -> v.untilMs <= now);
        cooldowns.values().removeIf(until -> until <= now);
        pearlLock.values().removeIf(until -> until <= now);
        grit.values().removeIf(d -> d[0] <= now);
    }

    public void removeEntity(UUID id) {
        marks.remove(id);
        stunnedUntil.remove(id);
        tethers.remove(id);
        combos.remove(id);
        killstreaks.remove(id);
        vengeance.remove(id);
        mirrorQueued.remove(id);
        nextSwingBonus.remove(id);
        pearlLock.remove(id);
        // Drop any tether/vengeance that pointed AT the dead entity so references don't dangle.
        for (java.util.Iterator<Tether> it = tethers.values().iterator(); it.hasNext(); ) {
            if (id.equals(it.next().binder)) {
                it.remove();
            }
        }
        for (java.util.Iterator<Vengeance> it = vengeance.values().iterator(); it.hasNext(); ) {
            if (id.equals(it.next().attacker)) {
                it.remove();
            }
        }
    }

    public void clear(UUID id) {
        soulreaver.remove(id);
        sprintStart.remove(id);
        stunnedUntil.remove(id);
        mirrorQueued.remove(id);
        weaponSwapAt.remove(id);
        weaponSwapConsumed.remove(id);
        combos.remove(id);
        nextSwingBonus.remove(id);
        grit.remove(id);
        vengeance.remove(id);
        marks.remove(id);
        killstreaks.remove(id);
        tethers.remove(id);
        pearlLock.remove(id);
        // cooldowns are keyed "uuid|ability" — purge this player's entries so they don't accumulate
        // for the server's lifetime (clear() is the player-quit hook).
        cooldowns.keySet().removeIf(k -> k.startsWith(id + "|"));
        // Drop any tether where this player was the binder so it doesn't dangle.
        for (java.util.Iterator<Tether> it = tethers.values().iterator(); it.hasNext(); ) {
            if (id.equals(it.next().binder)) {
                it.remove();
            }
        }
    }
}