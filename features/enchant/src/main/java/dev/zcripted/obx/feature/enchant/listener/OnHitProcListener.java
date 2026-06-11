package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.registry.EnchantRegistry;
import dev.zcripted.obx.feature.enchant.service.CombatHudService;
import dev.zcripted.obx.feature.enchant.service.CombatParticleService;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.SoundPalette;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat enchants whose effect is a status/debuff proc applied to the victim
 * (rather than a modifier on the hit's damage, which lives in
 * {@link OnHitDamageListener}). Runs at {@code HIGHEST} so it fires after the
 * damage math: Concussion, Frostbrand (chill), Hellforged (ignite), Bonecrusher
 * (armor/weakness/shield), Voidstrike (blink), and Bloodletter (bleed DoT).
 *
 * <p>Bleed ticks bypass armor and are scheduled on the {@link SchedulerAdapter};
 * one task per victim, refreshable on re-hit.
 */
public final class OnHitProcListener implements Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatState combatState;
    private final CombatParticleService particles;
    private final CombatHudService hud;
    private final LanguageManager languages;

    private static final String[] CLEAVE_PARTICLE = {"SWEEP_ATTACK", "CRIT"};

    private final ConcurrentHashMap<UUID, Bleed> bleeds = new ConcurrentHashMap<UUID, Bleed>();
    /**
     * Re-entrancy guard so Cleave's secondary hits don't cascade into more cleaves.
     * Thread-local because on Folia {@code EntityDamageByEntityEvent} fires concurrently on
     * multiple region threads — a shared boolean could be cleared by one thread while another
     * is mid-cleave.
     */
    private final ThreadLocal<Boolean> inSecondary = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public OnHitProcListener(ObxPlugin plugin, CombatState combatState, CombatParticleService particles, CombatHudService hud) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.combatState = combatState;
        this.particles = particles;
        this.hud = hud;
        this.languages = plugin.getLanguageManager();
    }

    // ORDERING CONTRACT: runs at HIGHEST, strictly after OnHitDamageListener's HIGH
    // damage-modifier pass, so procs apply to the final damage value. ignoreCancelled=true
    // means a hit cancelled earlier in the chain never procs. Keep HIGHEST > HIGH; see
    // OnHitDamageListener#onDamage. Do NOT reorder these two priorities.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProc(EntityDamageByEntityEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        // Suppress ALL secondary procs when this hit originates from a cleave / tempest /
        // devastator AoE swing. Those effects already apply their own secondary debuffs and
        // must not re-trigger bleed, stun, concussion, etc. on every extra target.
        if (inSecondary.get()) {
            return;
        }
        Player attacker = CombatSupport.resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        LivingEntity victim = (LivingEntity) event.getEntity();
        if (victim.equals(attacker)) {
            return;
        }
        ItemStack weapon = CombatSupport.mainHand(attacker);
        if (weapon == null) {
            return;
        }
        // Read the weapon's enchants ONCE — each storage.level() re-parses all lore, and this
        // handler checks ~12 enchants per hit (combat hot path).
        Map<String, Integer> levels = storage.read(weapon);
        EnchantRegistry registry = service.getRegistry();

        // Concussion — daze with Slowness + Mining Fatigue (+ Nausea at Lv4).
        int concussion = levels.getOrDefault("concussion", 0);
        if (concussion > 0) {
            CustomEnchant e = registry.get("concussion");
            if (Math.random() < e.levelDouble(concussion, "chance", 0.0)) {
                int duration = e.levelInt(concussion, "duration_ticks", 40);
                Potions.applyLevel(victim, Potions.SLOWNESS, duration, e.levelInt(concussion, "slowness", 1));
                Potions.applyLevel(victim, Potions.MINING_FATIGUE, duration, e.levelInt(concussion, "fatigue", 1));
                int nausea = e.levelInt(concussion, "nausea_ticks", 0);
                if (nausea > 0) {
                    Potions.apply(victim, Potions.NAUSEA, nausea, 0);
                }
                particles.spawnRing(victim.getEyeLocation(), 0.4, Particles.CRIT, 8);
            }
        }

        // Frostbrand — chill (Slowness + freeze ticks). Flat frost damage is in OnHitDamageListener.
        int frostbrand = levels.getOrDefault("frostbrand", 0);
        if (frostbrand > 0) {
            CustomEnchant e = registry.get("frostbrand");
            Potions.applyLevel(victim, Potions.SLOWNESS, e.levelInt(frostbrand, "slowness_ticks", 20), e.levelInt(frostbrand, "slowness", 1));
            CombatSupport.setFreezeTicks(victim, e.levelInt(frostbrand, "freeze_ticks", 0));
            particles.spawnRing(victim.getEyeLocation(), 0.5, Particles.CHILL, 10);
            SoundPalette.playAt(victim.getLocation(), SoundPalette.FROST, CombatSupport.volume(service, 0.5f), 1.4f);
        }

        // Hellforged — ignite. Damage bonus vs non-Nether mobs is in OnHitDamageListener.
        int hellforged = levels.getOrDefault("hellforged", 0);
        if (hellforged > 0) {
            int seconds = registry.levelInt("hellforged", hellforged, "fire_seconds", 3);
            victim.setFireTicks(Math.max(victim.getFireTicks(), seconds * 20));
            particles.spawnRing(victim.getLocation().add(0, 1, 0), 0.4, Particles.FLAME, 8);
        }

        // Bonecrusher — armor durability on crit, Weakness, and a chance to disable a shield.
        int bonecrusher = levels.getOrDefault("bonecrusher", 0);
        if (bonecrusher > 0) {
            CustomEnchant e = registry.get("bonecrusher");
            if (CombatSupport.isVanillaCrit(attacker)) {
                int durability = e.levelInt(bonecrusher, "durability_on_crit", 0);
                if (durability > 0) {
                    damageArmorDurability(victim, durability);
                }
                SoundPalette.playAt(victim.getLocation(), SoundPalette.BONE, CombatSupport.volume(service, 0.6f), 0.9f);
            }
            int weakness = e.levelInt(bonecrusher, "weakness_ticks", 0);
            if (weakness > 0) {
                Potions.apply(victim, Potions.WEAKNESS, weakness, 0);
                // Weakness cooldown shown on the target hologram's second line (live countdown).
                hud.trackDebuff(victim, weakness * 50L);
            }
            double shieldDisable = e.levelDouble(bonecrusher, "shield_disable_chance", 0.0);
            if (shieldDisable > 0 && victim instanceof Player && Math.random() < shieldDisable) {
                disableShield((Player) victim, 60);
                notifyShieldDisabled(attacker, (Player) victim);
            }
            // Show this swing's damage to the attacker.
            CombatSupport.actionBar(service, attacker,
                    "&fBonecrusher &8» &c" + CombatSupport.format(event.getFinalDamage()) + " &7damage");
        }

        // Voidstrike — chance to blink the target forward, with disorientation.
        int voidstrike = levels.getOrDefault("voidstrike", 0);
        if (voidstrike > 0) {
            CustomEnchant e = registry.get("voidstrike");
            if (Math.random() < e.levelDouble(voidstrike, "chance", 0.0)) {
                blink(victim, e.levelDouble(voidstrike, "distance", 2.0));
                int nausea = e.levelInt(voidstrike, "nausea_ticks", 0);
                if (nausea > 0) {
                    Potions.apply(victim, Potions.NAUSEA, nausea, 0);
                }
                int blindness = e.levelInt(voidstrike, "blindness_ticks", 0);
                if (blindness > 0) {
                    Potions.apply(victim, Potions.BLINDNESS, blindness, 0);
                }
                SoundPalette.playAt(victim.getLocation(), SoundPalette.PHASE, CombatSupport.volume(service, 0.7f), 1.0f);
            }
        }

        // Bloodletter — apply / refresh an armor-bypassing bleed DoT.
        int bloodletter = levels.getOrDefault("bloodletter", 0);
        if (bloodletter > 0) {
            applyBleed(attacker, victim, registry.get("bloodletter"), bloodletter);
        }

        // Stunlock — chance to briefly stun the target (movement lock + stun window).
        int stunlock = levels.getOrDefault("stunlock", 0);
        if (stunlock > 0) {
            CustomEnchant e = registry.get("stunlock");
            if (Math.random() < e.levelDouble(stunlock, "chance", 0.0)) {
                applyStun(victim, e.levelDouble(stunlock, "stun_seconds", 0.75));
            }
        }

        // Cleave — carve a forward cone, dealing a fraction of the hit to extra targets.
        int cleave = levels.getOrDefault("cleave", 0);
        if (cleave > 0 && !inSecondary.get()) {
            cleave(attacker, victim, event.getFinalDamage(), registry.get("cleave"), cleave, registry);
        }

        // Tempest Strike — a critical hit unleashes a knockback wave (+ small AoE damage).
        int tempest = levels.getOrDefault("tempest_strike", 0);
        if (tempest > 0 && !inSecondary.get() && CombatSupport.isVanillaCrit(attacker)) {
            tempestStrike(attacker, victim, registry.get("tempest_strike"), tempest);
        }

        // Devastator — a falling mace smash quakes the ground around the target.
        int devastator = levels.getOrDefault("devastator", 0);
        if (devastator > 0 && !inSecondary.get() && attacker.getFallDistance() > 1.0f) {
            devastatorShockwave(attacker, victim, registry.get("devastator"), devastator);
        }

        // Manaburn — drain the target and bank a bonus for your next swing.
        int manaburn = levels.getOrDefault("manaburn", 0);
        if (manaburn > 0) {
            CustomEnchant e = registry.get("manaburn");
            if (victim instanceof Player) {
                Player victimPlayer = (Player) victim;
                int hunger = e.levelInt(manaburn, "hunger_drain", 1);
                try {
                    victimPlayer.setFoodLevel(Math.max(0, victimPlayer.getFoodLevel() - hunger));
                } catch (Throwable ignored) {
                    // food level always settable on players; guard anyway
                }
            }
            combatState.addNextSwing(attacker.getUniqueId(), e.levelDouble(manaburn, "next_swing_bonus", 0.05));
            int weakness = e.levelInt(manaburn, "weakness_ticks", 0);
            if (weakness > 0) {
                Potions.apply(victim, Potions.WEAKNESS, weakness, 0);
            }
            particles.spawnTrail(victim.getEyeLocation(), attacker.getEyeLocation(), Particles.MAGIC, 6);
        }
    }

    private void tempestStrike(Player attacker, LivingEntity center, CustomEnchant e, int level) {
        double radius = e.levelDouble(level, "radius", 2.0);
        double strength = e.levelDouble(level, "knockback", 0.5);
        double adjacentDamage = e.levelDouble(level, "adjacent_damage", 0.0);
        int slowTicks = e.levelInt(level, "slowness_ticks", 0);
        Location origin = center.getLocation();
        inSecondary.set(Boolean.TRUE);
        try {
            for (Entity near : center.getNearbyEntities(radius, radius, radius)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker)) {
                    continue;
                }
                LivingEntity target = (LivingEntity) near;
                org.bukkit.util.Vector push = target.getLocation().toVector().subtract(origin.toVector()).setY(0.2);
                if (push.lengthSquared() > 1.0E-6) {
                    push.normalize().multiply(strength);
                    try {
                        target.setVelocity(target.getVelocity().add(push));
                    } catch (Throwable ignored) {
                        // velocity set can be region-restricted on Folia
                    }
                }
                if (adjacentDamage > 0) {
                    try {
                        target.damage(adjacentDamage, attacker);
                    } catch (Throwable ignored) {
                        // skip off-region / invulnerable
                    }
                }
                if (slowTicks > 0) {
                    Potions.applyLevel(target, Potions.SLOWNESS, slowTicks, 1);
                }
            }
        } finally {
            inSecondary.set(Boolean.FALSE);
        }
        particles.spawnRing(origin, radius, CLEAVE_PARTICLE, 16);
        SoundPalette.playAt(origin, SoundPalette.SWEEP, CombatSupport.volume(service, 0.7f), 1.4f);
    }

    private void devastatorShockwave(Player attacker, LivingEntity center, CustomEnchant e, int level) {
        double radius = e.levelDouble(level, "shockwave_radius", 2.0);
        double damage = e.levelDouble(level, "shockwave_damage", 2.0);
        double knockback = e.levelDouble(level, "knockback", 0.5);
        Location origin = center.getLocation();
        inSecondary.set(Boolean.TRUE);
        try {
            for (Entity near : center.getNearbyEntities(radius, radius, radius)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker)) {
                    continue;
                }
                LivingEntity target = (LivingEntity) near;
                try {
                    target.damage(damage, attacker);
                } catch (Throwable ignored) {
                    // off-region / invulnerable
                }
                org.bukkit.util.Vector push = target.getLocation().toVector().subtract(origin.toVector()).setY(0.3);
                if (push.lengthSquared() > 1.0E-6) {
                    push.normalize().multiply(knockback);
                    try {
                        target.setVelocity(target.getVelocity().add(push));
                    } catch (Throwable ignored) {
                        // velocity set can be region-restricted on Folia
                    }
                }
            }
        } finally {
            inSecondary.set(Boolean.FALSE);
        }
        particles.spawnShockwave(origin, radius, new String[]{"BLOCK_CRACK", "CLOUD", "CRIT"}, 8);
        SoundPalette.playAt(origin, new String[]{"ENTITY_GENERIC_EXPLODE", "EXPLODE"}, CombatSupport.volume(service, 0.7f), 0.8f);
    }

    private void applyStun(LivingEntity victim, double seconds) {
        int ticks = Math.max(1, (int) (seconds * 20));
        combatState.setStunned(victim.getUniqueId(), (long) (seconds * 1000));
        // Heavy, auto-expiring slowness locks movement without a scheduled re-enable
        // (so nothing can leave a mob frozen if a re-enable task fails on Folia).
        Potions.apply(victim, Potions.SLOWNESS, ticks, 6);
        Potions.apply(victim, Potions.MINING_FATIGUE, ticks, 9);
        if (victim instanceof Player) {
            Potions.apply(victim, Potions.BLINDNESS, ticks, 0);
        }
        particles.spawnRing(victim.getEyeLocation().add(0, 0.4, 0), 0.4, new String[]{"CRIT", "FIREWORKS_SPARK"}, 8);
        SoundPalette.playAt(victim.getLocation(), SoundPalette.STUN, CombatSupport.volume(service, 0.6f), 0.8f);
    }

    private void cleave(Player attacker, LivingEntity primary, double hitDamage, CustomEnchant e, int level, EnchantRegistry registry) {
        double range = e.levelDouble(level, "range", 3.0);
        double halfCone = Math.toRadians(e.levelDouble(level, "cone_degrees", 60.0)) / 2.0;
        double percent = e.levelDouble(level, "damage_percent", 0.5);
        int bleedLevel = e.levelInt(level, "bleed_level", 0);
        org.bukkit.util.Vector facing = attacker.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 1.0E-6) {
            return;
        }
        facing.normalize();
        inSecondary.set(Boolean.TRUE);
        try {
            for (Entity near : attacker.getNearbyEntities(range, range, range)) {
                if (!(near instanceof LivingEntity) || near.equals(primary) || near.equals(attacker)) {
                    continue;
                }
                org.bukkit.util.Vector to = near.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
                if (to.lengthSquared() < 1.0E-6) {
                    continue;
                }
                if (facing.angle(to.normalize()) <= halfCone) {
                    LivingEntity target = (LivingEntity) near;
                    try {
                        target.damage(hitDamage * percent, attacker);
                    } catch (Throwable ignored) {
                        // off-region on Folia / invulnerable — skip this target
                    }
                    if (bleedLevel > 0) {
                        applyBleed(attacker, target, registry.get("bloodletter"), bleedLevel);
                    }
                }
            }
        } finally {
            inSecondary.set(Boolean.FALSE);
        }
        particles.spawnRing(primary.getLocation(), range * 0.5, CLEAVE_PARTICLE, 12);
    }

    // ── Bloodletter bleed ────────────────────────────────────────────────────

    private static final class Bleed {
        // Volatile: on Folia the refresh path mutates these from the attacker's region thread while
        // the bleed tick reads them on the victim's region thread.
        volatile int remainingTicks;
        volatile int periodTicks;
        volatile double damage;
        volatile UUID source;
        final long expiresAt;      // System.currentTimeMillis() deadline; used by sweepExpired()
        volatile dev.zcripted.obx.core.platform.scheduler.CancellableTask task;

        Bleed(int remainingTicks, int periodTicks, double damage, UUID source) {
            this.remainingTicks = remainingTicks;
            this.periodTicks = periodTicks;
            this.damage = damage;
            this.source = source;
            this.expiresAt = System.currentTimeMillis() + remainingTicks * 50L + 10_000L; // 10s grace
        }
    }

    private void applyBleed(Player attacker, final LivingEntity victim, CustomEnchant e, int level) {
        final UUID id = victim.getUniqueId();
        int duration = e.levelInt(level, "duration_ticks", 80);
        final int period = Math.max(5, e.levelInt(level, "period_ticks", 40));
        double dmg = e.levelDouble(level, "damage", 1.0);
        double perSec = dmg * 20.0 / period;
        long durationMs = duration * 50L;

        // Live blood-loss HUD above the target (health bar + bleed rate) for the bleed window.
        hud.trackHealth(victim, durationMs);
        hud.trackBleed(victim, perSec, durationMs);
        particles.spawnBlood(victim.getLocation().add(0, 1.0, 0));
        // The victim gets a live "bleeding out" countdown in their action bar.
        bleedActionbar(victim, duration);

        Bleed existing = bleeds.get(id);
        if (existing != null) {
            if (Math.random() < e.levelDouble(level, "refresh_chance", 0.0)) {
                existing.remainingTicks = duration;
                existing.damage = dmg;
                existing.periodTicks = period;
            }
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        final Bleed bleed = new Bleed(duration, period, dmg, attacker.getUniqueId());
        bleeds.put(id, bleed);
        final Player source = attacker;
        bleed.task = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                // The repeating task itself fires on the global region (Folia) / main thread.
                // All of the per-tick work below touches the VICTIM entity (health, location,
                // a damage event, particles, the action bar), so on Folia it must run on the
                // victim's own region thread — otherwise every access throws a thread-ownership
                // error that the dealBleed catch swallows, and bleed silently never ticks.
                Runnable tick = new Runnable() {
                    @Override
                    public void run() {
                        Bleed current = bleeds.get(id);
                        if (current == null) {
                            return;
                        }
                        if (victim.isDead() || !victim.isValid() || current.remainingTicks <= 0) {
                            finish(id, current);
                            return;
                        }
                        dealBleed(victim, current.damage, source.isOnline() ? source : null);
                        particles.spawnBlood(victim.getLocation().add(0, 1.0, 0));
                        // Keep the blood-loss HUD alive (and its rate current) across the bleed.
                        hud.trackBleed(victim, current.damage * 20.0 / current.periodTicks, current.periodTicks * 50L * 3L);
                        // Refresh the victim's bleed-out countdown each tick.
                        if (!victim.isDead() && victim.isValid()) {
                            bleedActionbar(victim, current.remainingTicks);
                        }
                        current.remainingTicks -= current.periodTicks;
                        if (current.remainingTicks <= 0) {
                            finish(id, current);
                        }
                    }
                };
                if (scheduler.isFolia()) {
                    scheduler.runAtEntity(victim, tick);
                } else {
                    tick.run();
                }
            }
        }, period, period);
    }

    /**
     * Bloodletter death flourish — when a bleeding entity dies, erupt a blood splatter and
     * send the killer a confirmation title. Lives here because the bleed registry is local.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBleedDeath(EntityDeathEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        Bleed bleed = bleeds.remove(dead.getUniqueId());
        if (bleed == null) {
            return;
        }
        if (bleed.task != null) {
            bleed.task.cancel();
        }
        particles.spawnBloodBurst(dead.getLocation().add(0, 1.0, 0));
        SoundPalette.playAt(dead.getLocation(), SoundPalette.BLEED, CombatSupport.volume(service, 1.08f), 0.7f);
        Player killer = dead.getKiller();
        if (killer != null) {
            String name = dead instanceof Player ? dead.getName() : prettyName(dead.getType().name());
            CombatSupport.title(service, killer, "&4&l☠ SLAIN", "&c" + name + " &7bled out");
        }
        // Victim-facing bleed-out notification + a private gravestone marker.
        if (dead instanceof Player) {
            notifyBleedOut((Player) dead, killer, dead.getLocation());
        }
    }

    /** Shows the victim a live "bleeding out — Ns left" action bar (players only). */
    private void bleedActionbar(LivingEntity victim, int remainingTicks) {
        if (!(victim instanceof Player)) {
            return;
        }
        int seconds = Math.max(1, (int) Math.ceil(remainingTicks / 20.0));
        ComponentMessenger.sendActionBar((Player) victim, languages.get((Player) victim,
                "enchant.bloodletter.actionbar", Collections.singletonMap("seconds", Integer.toString(seconds))));
    }

    /** Death message + title for the bleed-out victim, plus their private gravestone. */
    private void notifyBleedOut(Player victim, Player killer, Location deathLoc) {
        String killerName = killer != null ? killer.getName() : "Bloodletter";
        victim.sendMessage(languages.get(victim, "enchant.bloodletter.death-message",
                Collections.singletonMap("killer", killerName)));
        CombatSupport.title(service, victim,
                languages.get(victim, "enchant.bloodletter.death-title"),
                languages.get(victim, "enchant.bloodletter.death-subtitle",
                        Collections.singletonMap("killer", killerName)));
        if (service.getCombatSettings() != null && service.getCombatSettings().bloodletterGravestone() && deathLoc != null) {
            spawnGravestone(victim, deathLoc.clone(), service.getCombatSettings().bloodletterGravestoneSeconds());
        }
    }

    /**
     * Spawns a temporary gravestone marker at {@code loc} that only {@code viewer} can see
     * (per-player particles), lasting {@code seconds}. The victim also gets a one-time chat
     * line with the spot's coordinates so they can return to it.
     */
    private void spawnGravestone(final Player viewer, final Location loc, int seconds) {
        final SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null || loc.getWorld() == null) {
            return;
        }
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("x", Integer.toString(loc.getBlockX()));
        ph.put("y", Integer.toString(loc.getBlockY()));
        ph.put("z", Integer.toString(loc.getBlockZ()));
        ph.put("world", loc.getWorld().getName());
        viewer.sendMessage(languages.get(viewer, "enchant.bloodletter.gravestone", ph));

        final long period = 12L;
        final int[] remaining = {Math.max(1, (int) (seconds * 20L / period))};
        final dev.zcripted.obx.core.platform.scheduler.CancellableTask[] taskRef = new dev.zcripted.obx.core.platform.scheduler.CancellableTask[1];
        taskRef[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                if (remaining[0]-- <= 0 || !viewer.isOnline()) {
                    if (taskRef[0] != null) {
                        taskRef[0].cancel();
                    }
                    return;
                }
                if (viewer.getWorld() == null || !viewer.getWorld().equals(loc.getWorld())) {
                    return; // only render the marker while the victim is in that world
                }
                // Per-player gravestone: a smoke column topped with a soul flame + crit cap.
                Particles.atPlayer(viewer, loc.clone().add(0, 0.2, 0), Particles.SMOKE, 4, 0.06);
                Particles.atPlayer(viewer, loc.clone().add(0, 0.8, 0), Particles.SOUL_FIRE, 3, 0.08);
                Particles.atPlayer(viewer, loc.clone().add(0, 1.3, 0), Particles.CRIT, 5, 0.12);
            }
        }, period, period);
    }

    private static String prettyName(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean cap = true;
        for (char c : lower.toCharArray()) {
            out.append(cap && Character.isLetter(c) ? Character.toUpperCase(c) : c);
            cap = c == ' ';
        }
        return out.toString();
    }

    /** Sweeps expired bleed entries where the entity despawned and the task no longer fires. */
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        for (java.util.Map.Entry<UUID, Bleed> entry : bleeds.entrySet()) {
            Bleed bleed = entry.getValue();
            if (bleed.expiresAt < now) {
                UUID id = entry.getKey();
                Bleed removed = bleeds.remove(id);
                if (removed != null && removed.task != null) {
                    removed.task.cancel();
                }
            }
        }
    }

    private void finish(UUID id, Bleed bleed) {
        bleeds.remove(id);
        if (bleed != null && bleed.task != null) {
            bleed.task.cancel();
        }
    }

    /** Deals damage that ignores armor, routing a lethal tick through {@code damage()} so death credits/drops work. */
    private void dealBleed(LivingEntity victim, double amount, Player source) {
        try {
            // Gate the tick on the damage pipeline (godmode / region "damage-deny" / invincible
            // flags can veto) WITHOUT applying armor or re-triggering combat enchants: fire a
            // plain EntityDamageEvent (not ByEntity, so OBX's combat listeners don't re-proc),
            // and only apply the armor-bypassing bleed if it isn't cancelled. This stops bleed
            // ticking inside spawn / PvP-disabled / protected regions.
            org.bukkit.event.entity.EntityDamageEvent probe = new org.bukkit.event.entity.EntityDamageEvent(
                    victim, org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM, amount);
            org.bukkit.Bukkit.getPluginManager().callEvent(probe);
            if (probe.isCancelled()) {
                return;
            }
            double health = victim.getHealth();
            if (health - amount > 0.0) {
                victim.setHealth(Math.max(0.0, health - amount));
                try {
                    victim.playEffect(EntityEffect.HURT);
                } catch (Throwable ignored) {
                    // cosmetic flash only
                }
            } else if (source != null) {
                victim.damage(health + 1.0, source);
            } else {
                victim.damage(health + 1.0);
            }
        } catch (Throwable ignored) {
            // off-region on Folia or odd fork — bleed simply skips this tick
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void blink(LivingEntity victim, double distance) {
        try {
            Location from = victim.getLocation();
            Vector direction = from.getDirection().setY(0);
            if (direction.lengthSquared() < 1.0E-6) {
                return;
            }
            direction.normalize().multiply(distance);
            Location to = from.clone().add(direction);
            to.setY(from.getY());
            Particles.at(from.clone().add(0, 1, 0), Particles.PORTAL, 20, 0.4);
            victim.teleport(to);
            Particles.at(to.clone().add(0, 1, 0), Particles.PORTAL, 20, 0.4);
        } catch (Throwable ignored) {
            // teleport can fail (cross-region on Folia, unloaded chunk) — skip
        }
    }

    private void damageArmorDurability(LivingEntity victim, int amount) {
        try {
            EntityEquipment equipment = victim.getEquipment();
            if (equipment == null) {
                return;
            }
            ItemStack[] armor = equipment.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                ItemStack piece = armor[i];
                if (piece == null || piece.getType() == Material.AIR) {
                    continue;
                }
                short max = piece.getType().getMaxDurability();
                if (max <= 0) {
                    continue;
                }
                piece.setDurability((short) Math.min(max, piece.getDurability() + amount));
                armor[i] = piece;
            }
            equipment.setArmorContents(armor);
        } catch (Throwable ignored) {
            // best effort — never break combat over durability
        }
    }

    /** Chat popup to both sides when Bonecrusher shatters a shield. */
    private void notifyShieldDisabled(Player attacker, Player victim) {
        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&f&lBonecrusher &8» &cShattered &f" + victim.getName() + "&c's shield!"));
        victim.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&lBonecrusher &8» &7Your shield was shattered!"));
    }

    private void disableShield(Player player, int ticks) {
        try {
            Material shield = Material.matchMaterial("SHIELD");
            if (shield != null) {
                player.setCooldown(shield, ticks);
            }
        } catch (Throwable ignored) {
            // setCooldown is 1.11+; no-op on older
        }
    }
}