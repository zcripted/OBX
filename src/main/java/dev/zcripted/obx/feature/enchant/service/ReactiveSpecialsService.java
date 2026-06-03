package dev.zcripted.obx.feature.enchant.service;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.listener.CombatSupport;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.SoundPalette;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * The three "reactive special" combat enchants, driven from both the melee and
 * the ranged hit sites:
 *
 * <ul>
 *   <li><b>Spectral Bind</b> — a hit chains the target (Slowness for the bind
 *       window, a periodic pull-back at higher levels, and an ender-pearl lock at
 *       Lv3), with a cyan chain line drawn between the two.</li>
 *   <li><b>Predator</b> — a hit paints a red tracking line toward the last target
 *       for a few seconds; higher levels add the target's HP (and speed) to the
 *       attacker's action bar. Particle trails render through walls client-side,
 *       which is what gives the "see through walls" tracking feel.</li>
 *   <li><b>Soul Tether</b> — a hit tethers the target to the attacker: attackers
 *       deal bonus damage to the tethered, while a fraction of the tethered's
 *       outgoing damage is siphoned back to the binder. The vulnerability and the
 *       backlash are applied by the combat listeners that read {@link CombatState}.</li>
 * </ul>
 *
 * <p>The recurring visuals run on the Folia-aware {@link SchedulerAdapter} and
 * self-cancel once their window elapses or an endpoint goes away, matching
 * {@link CombatParticleService}'s timed-effect pattern.
 */
public final class ReactiveSpecialsService {

    private static final String[] CHAIN = {"SNOWFLAKE", "DRIP_WATER", "WATER_SPLASH", "CRIT_MAGIC"};
    private static final String[] PREDATOR_LINE = {"REDSTONE", "DUST", "FLAME"};
    private static final String[] CHAIN_SOUND = {"BLOCK_CHAIN_PLACE", "BLOCK_ANVIL_LAND", "ANVIL_LAND"};
    private static final String[] TETHER_SOUND = {"BLOCK_RESPAWN_ANCHOR_CHARGE", "ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT"};

    /** Visual tick period; pull/HP cadences below are multiples of this. */
    private static final long PERIOD = 5L;

    private final OBX plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatState combatState;
    private final CombatParticleService particles;

    public ReactiveSpecialsService(OBX plugin, CombatState combatState, CombatParticleService particles) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
        this.combatState = combatState;
        this.particles = particles;
    }

    /**
     * Applies the reactive specials present on {@code weapon} for a hit by
     * {@code attacker} on {@code victim}. Called from the melee listener (held
     * weapon) and the ranged listener (firing bow), so it covers SWORD and BOW.
     */
    public void onHit(Player attacker, LivingEntity victim, ItemStack weapon) {
        if (attacker == null || victim == null || weapon == null || victim.equals(attacker)) {
            return;
        }

        int bind = storage.level(weapon, "spectral_bind");
        if (bind > 0) {
            spectralBind(attacker, victim, service.getRegistry().get("spectral_bind"), bind);
        }

        int predator = storage.level(weapon, "predator");
        if (predator > 0) {
            predator(attacker, victim, service.getRegistry().get("predator"), predator);
        }

        int tether = storage.level(weapon, "soul_tether");
        if (tether > 0) {
            soulTether(attacker, victim, service.getRegistry().get("soul_tether"), tether);
        }
    }

    // ── Spectral Bind ───────────────────────────────────────────────────────

    private void spectralBind(final Player binder, final LivingEntity target, CustomEnchant e, int level) {
        final int durationTicks = e.levelInt(level, "duration_ticks", 60);
        int slowness = e.levelInt(level, "slowness", 1);
        final boolean pull = e.levelBoolean(level, "pull", false);
        final long pullInterval = Math.max(PERIOD, e.levelInt(level, "pull_interval", 20));
        final double pullStrength = e.levelDouble(level, "pull_strength", 0.4);
        final double pullMinDistance = e.levelDouble(level, "pull_min_distance", 2.5);

        if (slowness > 0) {
            Potions.applyLevel(target, Potions.SLOWNESS, durationTicks, slowness);
        }
        if (e.levelBoolean(level, "block_pearls", false)) {
            combatState.lockPearls(target.getUniqueId(), durationTicks * 50L);
        }
        SoundPalette.playAt(target.getLocation(), CHAIN_SOUND, CombatSupport.volume(service, 0.7f), 1.2f);
        CombatSupport.actionBar(service, binder, "&bSpectral Bind &8» &fbound");

        final SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                if (elapsed[0] >= durationTicks || target.isDead() || !binder.isOnline()
                        || !binder.getWorld().equals(target.getWorld())) {
                    if (handle[0] != null) {
                        handle[0].cancel();
                    }
                    return;
                }
                particles.spawnTrail(binder.getLocation().add(0, 1.0, 0), target.getLocation().add(0, 1.0, 0), CHAIN, 10);
                if (pull && elapsed[0] % pullInterval == 0) {
                    pullToward(target, binder.getLocation(), pullStrength, pullMinDistance);
                }
                elapsed[0] += PERIOD;
            }
        }, 1L, PERIOD);
    }

    private void pullToward(LivingEntity target, Location anchor, double strength, double minDistance) {
        try {
            Location loc = target.getLocation();
            if (loc.distance(anchor) < minDistance) {
                return;
            }
            Vector direction = anchor.toVector().subtract(loc.toVector());
            if (direction.lengthSquared() < 1.0E-6) {
                return;
            }
            direction.normalize().multiply(strength).setY(Math.min(0.3, strength * 0.5));
            target.setVelocity(target.getVelocity().multiply(0.6).add(direction));
        } catch (Throwable ignored) {
            // velocity set can be region-restricted on Folia / target gone
        }
    }

    // ── Predator ──────────────────────────────────────────────────────────────

    private void predator(final Player hunter, final LivingEntity prey, CustomEnchant e, int level) {
        final int durationTicks = e.levelInt(level, "duration_ticks", 100);
        final boolean showHp = e.levelBoolean(level, "show_hp", false);
        final boolean showSpeed = e.levelBoolean(level, "show_speed", false);

        final SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                if (elapsed[0] >= durationTicks || prey.isDead() || !hunter.isOnline()
                        || !hunter.getWorld().equals(prey.getWorld())) {
                    if (handle[0] != null) {
                        handle[0].cancel();
                    }
                    return;
                }
                particles.spawnTrail(hunter.getEyeLocation(), prey.getLocation().add(0, 1.0, 0), PREDATOR_LINE, 12);
                if (showHp) {
                    StringBuilder bar = new StringBuilder("&cPredator &8» &f");
                    bar.append(hearts(prey));
                    if (showSpeed) {
                        bar.append(" &7| &b").append(CombatSupport.format(speed(prey))).append(" b/t");
                    }
                    CombatSupport.actionBar(service, hunter, bar.toString());
                }
                elapsed[0] += PERIOD;
            }
        }, 1L, PERIOD);
    }

    private static String hearts(LivingEntity entity) {
        try {
            return "&c❤ " + CombatSupport.format(entity.getHealth());
        } catch (Throwable ignored) {
            return "&c❤ ?";
        }
    }

    private static double speed(LivingEntity entity) {
        try {
            Vector v = entity.getVelocity();
            return Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    // ── Soul Tether ─────────────────────────────────────────────────────────

    private void soulTether(final Player binder, final LivingEntity target, CustomEnchant e, int level) {
        final int durationTicks = e.levelInt(level, "duration_ticks", 160);
        double backlash = e.levelDouble(level, "backlash_percent", 0.10);
        double vulnerability = e.levelDouble(level, "vulnerability", 0.25);
        combatState.tether(target.getUniqueId(), binder.getUniqueId(), durationTicks * 50L, vulnerability, backlash);

        if (e.levelBoolean(level, "glow", false)) {
            try {
                target.setGlowing(true);
                scheduler().runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!target.isDead()) {
                                target.setGlowing(false);
                            }
                        } catch (Throwable ignored) {
                            // entity gone / API unavailable
                        }
                    }
                }, durationTicks);
            } catch (Throwable ignored) {
                // setGlowing is 1.9+
            }
        }
        SoundPalette.playAt(target.getLocation(), TETHER_SOUND, CombatSupport.volume(service, 0.6f), 0.8f);
        CombatSupport.actionBar(service, binder, "&5Soul Tether &8» &dlinked");

        final SchedulerAdapter scheduler = scheduler();
        if (scheduler == null) {
            return;
        }
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                if (elapsed[0] >= durationTicks || target.isDead() || !binder.isOnline()
                        || !binder.getWorld().equals(target.getWorld())) {
                    if (handle[0] != null) {
                        handle[0].cancel();
                    }
                    return;
                }
                particles.spawnTrail(binder.getLocation().add(0, 1.0, 0), target.getLocation().add(0, 1.0, 0), Particles.PORTAL, 10);
                elapsed[0] += PERIOD;
            }
        }, 1L, PERIOD);
    }

    private SchedulerAdapter scheduler() {
        return plugin.getSchedulerAdapter();
    }
}
