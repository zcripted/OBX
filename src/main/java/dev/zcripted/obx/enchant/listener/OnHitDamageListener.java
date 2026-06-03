package dev.zcripted.obx.enchant.listener;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.enchant.effect.CombatState;
import dev.zcripted.obx.enchant.effect.EndlessHunger;
import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.registry.EnchantRegistry;
import dev.zcripted.obx.enchant.service.CombatHudService;
import dev.zcripted.obx.enchant.service.CombatParticleService;
import dev.zcripted.obx.enchant.service.EnchantService;
import dev.zcripted.obx.enchant.service.HoloFXService;
import dev.zcripted.obx.enchant.storage.EnchantStorage;
import dev.zcripted.obx.enchant.util.Particles;
import dev.zcripted.obx.enchant.util.Potions;
import dev.zcripted.obx.enchant.util.SoundPalette;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * Combat enchants that modify outgoing melee damage or heal the attacker:
 * Berserker's Rage, Momentum, Wrath of the Wild, Soulreaver (stack consumption),
 * Crit Mastery, and Lifesteal. Damage multipliers are combined and applied once
 * via {@code event.setDamage}; heals/feedback read the resulting final damage.
 *
 * <p>Also tracks sprint start/stop (for Momentum) and clears per-player combat
 * state on quit.
 */
public final class OnHitDamageListener implements Listener {

    private final OBX plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatState combatState;
    private final CombatParticleService particles;
    private final HoloFXService holo;
    private final CombatHudService hud;

    /** Combat-active window (ms) the Berserker HUD + Strength + target health bar persist after a hit. */
    private static final long COMBAT_WINDOW_MS = 6000L;

    private static final String[] WIND = {"SWEEP_ATTACK", "CLOUD"};
    private static final String[] PHANTOM = {"DRAGON_BREATH", "CRIT_MAGIC", "CRIT"};
    private static final String[] ROAR = {"LAVA", "FLAME", "CRIT"};

    /**
     * Cached {@code PlayerInteractEvent.getHand()} — added in 1.9. {@code null} on
     * 1.8 (where the event fires once and only the main hand exists), so we treat a
     * missing method as "main hand" and proceed.
     */
    private static final java.lang.reflect.Method GET_HAND = resolveGetHand();

    private static java.lang.reflect.Method resolveGetHand() {
        try {
            return PlayerInteractEvent.class.getMethod("getHand");
        } catch (Throwable ignored) {
            return null;
        }
    }

    public OnHitDamageListener(OBX plugin, CombatState combatState, CombatParticleService particles, HoloFXService holo, CombatHudService hud) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
        this.combatState = combatState;
        this.particles = particles;
        this.holo = holo;
        this.hud = hud;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        // Victim-side reactions for a player taking a hit from a living attacker.
        if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
            Player victimPlayer = (Player) event.getEntity();
            LivingEntity damager = (LivingEntity) event.getDamager();
            ItemStack held = CombatSupport.mainHand(victimPlayer);

            // Mirror Edge — reflect the incoming blow if one is queued.
            if (combatState.hasMirror(victimPlayer.getUniqueId())) {
                double reflect = event.getFinalDamage() * combatState.consumeMirror(victimPlayer.getUniqueId());
                if (reflect > 0 && !damager.equals(victimPlayer)) {
                    try {
                        damager.damage(reflect, victimPlayer);
                    } catch (Throwable ignored) {
                        // off-region on Folia / invulnerable target — skip
                    }
                    Particles.burst(damager.getEyeLocation(), Particles.MAGIC);
                    CombatSupport.actionBar(service, victimPlayer, "&7Mirror Edge &8» &freflected");
                }
            }

            // Brawler's Grit — being hit readies a stronger next attack.
            int gritLevel = storage.level(held, "brawlers_grit");
            if (gritLevel > 0) {
                CustomEnchant e = service.getRegistry().get("brawlers_grit");
                combatState.readyGrit(victimPlayer.getUniqueId(),
                        (long) (e.levelDouble(gritLevel, "window_seconds", 3.0) * 1000), gritLevel);
            }

            // Vengeance — remember who hit you (and optionally mark them with a glow).
            int vengeanceLevel = storage.level(held, "vengeance");
            if (vengeanceLevel > 0) {
                CustomEnchant e = service.getRegistry().get("vengeance");
                combatState.markAttacker(victimPlayer.getUniqueId(), damager.getUniqueId(),
                        (long) (e.levelDouble(vengeanceLevel, "duration_seconds", 10) * 1000));
                if (e.levelBoolean(vengeanceLevel, "glow", false)) {
                    try {
                        damager.setGlowing(true);
                    } catch (Throwable ignored) {
                        // setGlowing is 1.9+; harmless if unavailable
                    }
                }
            }
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

        // During-combat target health hologram for ANY Combat-category enchant on the weapon.
        if (CombatSupport.hasCombatEnchant(service, weapon)) {
            hud.trackHealth(victim, COMBAT_WINDOW_MS);
        }

        EnchantRegistry registry = service.getRegistry();
        double base = event.getDamage();
        double multiplier = 1.0;
        double flatBonus = 0.0;
        double pierce = 0.0;
        boolean crit = false;
        boolean phantomProc = false;
        int apex = storage.level(weapon, "apex_predator");

        // Whirlwind — make a sweep attack deal real (percent of weapon) damage.
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            int whirlwind = storage.level(weapon, "whirlwind");
            if (whirlwind > 0) {
                CustomEnchant e = registry.get("whirlwind");
                double attack = CombatSupport.attackDamage(weapon);
                if (attack > 0) {
                    base = attack * e.levelDouble(whirlwind, "sweep_percent", 0.6);
                }
                if (e.levelBoolean(whirlwind, "ring", false)) {
                    particles.spawnRing(victim.getLocation(), 1.5, WIND, 16);
                }
            }
        }

        // Berserker's Rage — scales with missing HP (in 20% steps).
        int berserk = storage.level(weapon, "berserkers_rage");
        if (berserk > 0) {
            CustomEnchant e = registry.get("berserkers_rage");
            double frac = CombatSupport.healthFraction(attacker);
            int fifths = (int) ((1.0 - frac) / 0.20);
            double berserkBonus = Math.min(e.levelDouble(berserk, "max_bonus", 0.0), fifths * e.levelDouble(berserk, "per_missing_fifth", 0.0));
            multiplier += berserkBonus;
            // Strength is granted whenever you're fighting with Berserker's Rage (refreshed each
            // hit, so it lands from the first hit onward) — Strength II once below the level's
            // strength_below HP threshold (Lv4), Strength I otherwise.
            double strengthBelow = e.levelDouble(berserk, "strength_below", 0.0);
            int strengthLevel = (strengthBelow > 0 && frac <= strengthBelow) ? 2 : 1;
            Potions.applyLevel(attacker, Potions.STRENGTH, (int) (COMBAT_WINDOW_MS / 50L), strengthLevel);
            // Damage-percent action-bar HUD + the target's health bar, both held for the combat window.
            int shownPercent = (int) Math.round(berserkBonus * 100);
            hud.berserkerBar(attacker, "&cBerserker's Rage &7» &f+" + shownPercent + "% damage", COMBAT_WINDOW_MS);
            // (the target health bar is shown by the universal combat-enchant hologram above)
        }

        // Momentum — scales with continuous sprint time.
        int momentum = storage.level(weapon, "momentum");
        if (momentum > 0 && attacker.isSprinting()) {
            CustomEnchant e = registry.get("momentum");
            double seconds = combatState.sprintSeconds(attacker.getUniqueId());
            double max = e.levelDouble(momentum, "max_bonus", 0.0);
            double bonus = Math.min(max, seconds * e.levelDouble(momentum, "per_second", 0.0));
            multiplier += bonus;
            if (bonus >= max && max > 0) {
                particles.spawnTrail(attacker.getLocation().add(0, 1.0, 0), attacker.getLocation().add(0, 1.6, 0), Particles.CLOUD, 4);
            }
        }

        // Wrath of the Wild — scales with nearby hostiles.
        int wrath = storage.level(weapon, "wrath_of_the_wild");
        if (wrath > 0) {
            CustomEnchant e = registry.get("wrath_of_the_wild");
            int mobs = CombatSupport.countNearbyHostiles(attacker, e.levelDouble(wrath, "radius", 6.0));
            multiplier += Math.min(e.levelDouble(wrath, "max_bonus", 0.0), mobs * e.levelDouble(wrath, "per_mob", 0.0));
            if (mobs >= 3) {
                particles.spawnSpiral(attacker, 0.8, 1.8, Particles.HAPPY);
            }
        }

        // Soulreaver — consume current kill-stacks for a damage bonus.
        int soulreaver = storage.level(weapon, "soulreaver");
        if (soulreaver > 0) {
            int stacks = combatState.soulreaverStacks(attacker.getUniqueId());
            if (stacks > 0) {
                CustomEnchant e = registry.get("soulreaver");
                multiplier += stacks * e.levelDouble(soulreaver, "per_stack", 0.0);
                if (stacks >= 3) {
                    particles.spawnAura(attacker, Particles.PORTAL, 10);
                }
                CombatSupport.actionBar(service, attacker, "&5Soulreaver &8» &d" + stacks + " stack" + (stacks == 1 ? "" : "s"));
            }
        }

        // Killstreak — outgoing damage scales with the active kill streak.
        int killstreak = storage.level(weapon, "killstreak");
        if (killstreak > 0) {
            CustomEnchant e = registry.get("killstreak");
            long window = (long) e.levelInt(killstreak, "window_seconds", 8) * 1000L;
            int streak = Math.min(e.levelInt(killstreak, "max_streak", 3), combatState.killstreak(attacker.getUniqueId(), window));
            if (streak >= 1) {
                double add = streak * e.levelDouble(killstreak, "per_step", 0.0);
                // Apex Predator amplifies killstreak buffs.
                if (apex > 0) {
                    add *= registry.get("apex_predator").levelDouble(apex, "killstreak_amp", 1.5);
                }
                multiplier += add;
            }
        }

        // Endless Hunger — permanent per-item stacks add a flat damage bonus.
        int hunger = storage.level(weapon, "endless_hunger");
        if (hunger > 0) {
            multiplier += EndlessHunger.bonus(weapon, registry.get("endless_hunger"), hunger);
        }

        // Apex Predator — the Mythic capstone: raw power and lethal crits. Only one
        // can be active per player; a second copy is inert (warned, throttled).
        if (apex > 0) {
            CustomEnchant e = registry.get("apex_predator");
            multiplier += e.levelDouble(apex, "damage_bonus", 0.15);
            if (CombatSupport.isVanillaCrit(attacker) || Math.random() < e.levelDouble(apex, "crit_chance", 0.10)) {
                crit = true;
                multiplier += e.levelDouble(apex, "crit_bonus", 0.10);
            }
            warnApexConflict(attacker);
        }

        // Crit Mastery — extra crit chance + crit damage on top of a vanilla crit.
        int critMastery = storage.level(weapon, "crit_mastery");
        if (critMastery > 0) {
            CustomEnchant e = registry.get("crit_mastery");
            boolean vanillaCrit = CombatSupport.isVanillaCrit(attacker);
            if (vanillaCrit || Math.random() < e.levelDouble(critMastery, "crit_chance", 0.0)) {
                crit = true;
                multiplier += e.levelDouble(critMastery, "crit_bonus", 0.0);
            }
        }

        // Frostbrand — flat frost damage added on top of the weapon's hit.
        int frostbrand = storage.level(weapon, "frostbrand");
        if (frostbrand > 0) {
            flatBonus += registry.get("frostbrand").levelDouble(frostbrand, "frost_damage", 0.0);
        }

        // Hellforged — bonus damage against non-Nether mobs.
        int hellforged = storage.level(weapon, "hellforged");
        if (hellforged > 0 && !CombatSupport.isNetherFoe(victim)) {
            multiplier += registry.get("hellforged").levelDouble(hellforged, "overworld_bonus", 0.0);
        }

        // Bonecrusher — bonus damage against the undead.
        int bonecrusher = storage.level(weapon, "bonecrusher");
        if (bonecrusher > 0 && CombatSupport.isUndead(victim)) {
            multiplier += registry.get("bonecrusher").levelDouble(bonecrusher, "undead_bonus", 0.0);
        }

        // Stunlock — bonus damage while the target is stunned.
        int stunlock = storage.level(weapon, "stunlock");
        if (stunlock > 0 && combatState.isStunned(victim.getUniqueId())) {
            multiplier += registry.get("stunlock").levelDouble(stunlock, "stunned_bonus", 0.0);
        }

        // Hunter's Mark — a marked target takes more damage from any attacker.
        multiplier += combatState.markBonus(victim.getUniqueId());

        // Soul Tether — a tethered target is more vulnerable to all weapon attacks.
        multiplier += combatState.tetherVulnerability(victim.getUniqueId());

        // Devastator — a falling mace smash hits far harder.
        int devastator = storage.level(weapon, "devastator");
        if (devastator > 0 && attacker.getFallDistance() > 1.0f) {
            multiplier += registry.get("devastator").levelDouble(devastator, "smash_bonus", 0.5);
        }

        // Phantom Edge — chance to phase fully through armor, +extra damage at Lv3.
        int phantom = storage.level(weapon, "phantom_edge");
        if (phantom > 0) {
            CustomEnchant e = registry.get("phantom_edge");
            if (Math.random() < e.levelDouble(phantom, "chance", 0.0)) {
                phantomProc = true;
                pierce = 1.0;
                multiplier += e.levelDouble(phantom, "extra_damage", 0.0);
            }
        }

        // Glasscutter — partial armor penetration (+ chance to shatter a piece at Lv4).
        double shatterChance = 0.0;
        int glasscutter = storage.level(weapon, "glasscutter");
        if (glasscutter > 0) {
            CustomEnchant e = registry.get("glasscutter");
            pierce = Math.max(pierce, e.levelDouble(glasscutter, "pierce", 0.0));
            shatterChance = e.levelDouble(glasscutter, "shatter_chance", 0.0);
        }

        // Manaburn — spend a next-swing bonus banked by a previous hit.
        multiplier += combatState.consumeNextSwing(attacker.getUniqueId());

        // Headsplitter — bonus damage against bare-headed targets.
        int headsplitter = storage.level(weapon, "headsplitter");
        if (headsplitter > 0 && isBareHeaded(victim)) {
            multiplier += registry.get("headsplitter").levelDouble(headsplitter, "barehead_bonus", 0.0);
        }

        // Combo Strike — consecutive hits on one target build to a burst.
        int comboStrike = storage.level(weapon, "combo_strike");
        if (comboStrike > 0) {
            CustomEnchant e = registry.get("combo_strike");
            int hits = e.levelInt(comboStrike, "hits", 3);
            long window = (long) (e.levelDouble(comboStrike, "window_seconds", 3.0) * 1000);
            int count = combatState.registerCombo(attacker.getUniqueId(), victim.getUniqueId(), window);
            if (count >= hits) {
                multiplier += e.levelDouble(comboStrike, "finisher", 0.5);
                combatState.resetCombo(attacker.getUniqueId());
                CombatSupport.actionBar(service, attacker, "&6Combo &8» &e" + hits + "/" + hits + " &6✦");
            } else {
                multiplier += e.levelDouble(comboStrike, "step", 0.0) * (count - 1);
                CombatSupport.actionBar(service, attacker, "&6Combo &8» &e" + count + "/" + hits + " ✦");
            }
        }

        // Vengeance — bonus vs the entity that last hit you (Lv3 guarantees the first crit).
        int vengeance = storage.level(weapon, "vengeance");
        if (vengeance > 0 && combatState.isVengeanceTarget(attacker.getUniqueId(), victim.getUniqueId())) {
            CustomEnchant e = registry.get("vengeance");
            multiplier += e.levelDouble(vengeance, "bonus", 0.0);
            if (e.levelBoolean(vengeance, "first_crit", false) && combatState.consumeVengeanceCrit(attacker.getUniqueId())) {
                crit = true;
            }
        }

        // Brawler's Grit — spend a readied buff for bonus damage (+ Speed/Resistance).
        if (storage.level(weapon, "brawlers_grit") > 0) {
            int gritLevel = combatState.consumeGrit(attacker.getUniqueId());
            if (gritLevel > 0) {
                CustomEnchant e = registry.get("brawlers_grit");
                multiplier += e.levelDouble(gritLevel, "bonus", 0.0);
                if (e.levelBoolean(gritLevel, "speed", false)) {
                    Potions.applyLevel(attacker, Potions.SPEED, 20, 1);
                }
                if (e.levelBoolean(gritLevel, "resistance", false)) {
                    Potions.applyLevel(attacker, Potions.RESISTANCE, 40, 1);
                }
            }
        }

        // Quickdraw — first hit shortly after swapping to this weapon.
        int quickdraw = storage.level(weapon, "quickdraw");
        if (quickdraw > 0) {
            CustomEnchant e = registry.get("quickdraw");
            long window = (long) (e.levelDouble(quickdraw, "window_seconds", 2.0) * 1000);
            if (combatState.tryConsumeQuickdraw(attacker.getUniqueId(), window)) {
                multiplier += e.levelDouble(quickdraw, "bonus", 0.0);
                if (e.levelBoolean(quickdraw, "speed", false)) {
                    Potions.applyLevel(attacker, Potions.SPEED, 20, 1);
                }
                Particles.burst(victim.getEyeLocation(), Particles.SPARK);
                CombatSupport.actionBar(service, attacker, "&eQuickdraw");
            }
        }

        if (multiplier != 1.0 || flatBonus != 0.0) {
            event.setDamage(Math.max(0.0, base * multiplier + flatBonus));
        }
        applyArmorPierce(event, pierce);
        double finalDamage = event.getFinalDamage();

        if (phantomProc) {
            Particles.burst(victim.getEyeLocation(), PHANTOM);
            SoundPalette.play(attacker, SoundPalette.PHASE, CombatSupport.volume(service, 0.7f), 1.2f);
            CombatSupport.actionBar(service, attacker, "&b✦ Phase");
        }
        if (shatterChance > 0 && Math.random() < shatterChance) {
            CombatSupport.shatterRandomArmor(victim, 50);
            SoundPalette.playAt(victim.getLocation(), SoundPalette.FROST, CombatSupport.volume(service, 0.6f), 1.0f);
        }

        // Mirror Edge — chance to ready a reflect that triggers on the next blow you take.
        int mirror = storage.level(weapon, "mirror_edge");
        if (mirror > 0) {
            CustomEnchant e = registry.get("mirror_edge");
            if (!combatState.hasMirror(attacker.getUniqueId()) && Math.random() < e.levelDouble(mirror, "chance", 0.0)) {
                combatState.queueMirror(attacker.getUniqueId(), e.levelDouble(mirror, "reflect", 1.0));
                Particles.burst(attacker.getEyeLocation(), Particles.MAGIC);
                CombatSupport.actionBar(service, attacker, "&7Mirror Edge &8» &fready");
            }
        }

        if (crit) {
            Particles.burst(victim.getEyeLocation(), Particles.CRIT);
            SoundPalette.play(attacker, SoundPalette.CRIT_GOLD, CombatSupport.volume(service, 0.8f), 1.5f);
        }

        // Lifesteal — heal a percentage of the final damage dealt.
        int lifesteal = storage.level(weapon, "lifesteal");
        if (lifesteal > 0) {
            CustomEnchant e = registry.get("lifesteal");
            double healAmount = finalDamage * e.levelDouble(lifesteal, "percent", 0.0);
            if (healAmount > 0) {
                CombatSupport.heal(attacker, healAmount);
                Particles.at(attacker.getEyeLocation(), Particles.HEART, 3, 0.3);
                SoundPalette.play(attacker, SoundPalette.LIFESTEAL, CombatSupport.volume(service, 0.5f), 1.3f);
                CombatSupport.actionBar(service, attacker, "&cLifesteal &8» &c+" + CombatSupport.format(healAmount) + " ❤");
            }
        }
    }

    /**
     * Reduces the armor (and enchant-protection) damage modifiers by {@code pierce}
     * so that fraction of mitigation is ignored. {@code pierce >= 1.0} fully phases
     * through armor. Uses the (deprecated but functional) per-modifier API, guarded
     * for forks where it's unavailable.
     */
    private void applyArmorPierce(EntityDamageByEntityEvent event, double pierce) {
        if (pierce <= 0.0) {
            return;
        }
        double clamped = Math.min(1.0, pierce);
        pierceModifier(event, EntityDamageEvent.DamageModifier.ARMOR, clamped);
        pierceModifier(event, EntityDamageEvent.DamageModifier.MAGIC, clamped);
    }

    private void pierceModifier(EntityDamageByEntityEvent event, EntityDamageEvent.DamageModifier modifier, double pierce) {
        try {
            if (event.isApplicable(modifier)) {
                double current = event.getDamage(modifier);
                if (current != 0.0) {
                    event.setDamage(modifier, current * (1.0 - pierce));
                }
            }
        } catch (Throwable ignored) {
            // modifier API removed/unsupported on this fork — armor pierce simply no-ops
        }
    }

    /** Warns (throttled) when a player carries more than one Apex Predator item. */
    private void warnApexConflict(Player player) {
        if (countApex(player) <= 1 || combatState.onCooldown(player.getUniqueId(), "apex_warn")) {
            return;
        }
        combatState.setCooldown(player.getUniqueId(), "apex_warn", 8000L);
        CombatSupport.actionBar(service, player, "&4&lApex Predator &8» &7Only one can be active at a time");
    }

    private int countApex(Player player) {
        int count = 0;
        try {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && storage.level(item, "apex_predator") > 0) {
                    count++;
                }
            }
        } catch (Throwable ignored) {
            // inventory access issue — skip the lock warning
        }
        return count;
    }

    /** True if the interaction came from the off-hand (1.9+); false on 1.8 or main hand. */
    private static boolean isOffHand(PlayerInteractEvent event) {
        if (GET_HAND == null) {
            return false;
        }
        try {
            Object hand = GET_HAND.invoke(event);
            return hand instanceof Enum && !"HAND".equals(((Enum<?>) hand).name());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isBareHeaded(LivingEntity victim) {
        try {
            EntityEquipment equipment = victim.getEquipment();
            if (equipment == null) {
                return true;
            }
            ItemStack helmet = equipment.getHelmet();
            return helmet == null || helmet.getType() == Material.AIR;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Battle Roar — sneak + right-click with the enchanted weapon to let out a war
     * cry: self-buff (Strength/Resistance) and a debuff (Weakness/Slowness) on nearby
     * foes, on a per-player cooldown. A held-item activation rather than an on-hit
     * trigger, so it lives on {@link PlayerInteractEvent}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onRoar(PlayerInteractEvent event) {
        if (!service.isEnabled() || isOffHand(event)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        ItemStack weapon = CombatSupport.mainHand(player);
        int roar = storage.level(weapon, "battle_roar");
        if (roar <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("battle_roar");
        if (combatState.onCooldown(player.getUniqueId(), "battle_roar")) {
            CombatSupport.actionBar(service, player,
                    "&c⌛ Battle Roar &8» &7" + combatState.cooldownSeconds(player.getUniqueId(), "battle_roar") + "s");
            return;
        }
        int cooldown = e.levelInt(roar, "cooldown_seconds", 60);
        combatState.setCooldown(player.getUniqueId(), "battle_roar", cooldown * 1000L);

        int buffTicks = e.levelInt(roar, "buff_ticks", 120);
        int selfStrength = e.levelInt(roar, "self_strength", 1);
        int selfResistance = e.levelInt(roar, "self_resistance", 0);
        if (selfStrength > 0) {
            Potions.applyLevel(player, Potions.STRENGTH, buffTicks, selfStrength);
        }
        if (selfResistance > 0) {
            Potions.applyLevel(player, Potions.RESISTANCE, buffTicks, selfResistance);
        }

        double radius = e.levelDouble(roar, "radius", 6.0);
        int debuffTicks = e.levelInt(roar, "debuff_ticks", 80);
        int weakness = e.levelInt(roar, "weakness", 1);
        int slowness = e.levelInt(roar, "slowness", 0);
        for (Entity near : player.getNearbyEntities(radius, radius, radius)) {
            if (near instanceof LivingEntity && !near.equals(player)) {
                LivingEntity foe = (LivingEntity) near;
                if (weakness > 0) {
                    Potions.applyLevel(foe, Potions.WEAKNESS, debuffTicks, weakness);
                }
                if (slowness > 0) {
                    Potions.applyLevel(foe, Potions.SLOWNESS, debuffTicks, slowness);
                }
                if (weakness > 0 || slowness > 0) {
                    // Float a live Weakness/Slowness (level + remaining time) readout over the foe.
                    hud.trackDebuff(foe, debuffTicks * 50L);
                }
            }
        }

        particles.spawnShockwave(player.getLocation(), radius, ROAR, 10);
        holo.showKillBanner(player, "&6&l⚔ BATTLE ROAR");
        SoundPalette.play(player, SoundPalette.RAGE, CombatSupport.volume(service, 1.0f), 0.7f);
        CombatSupport.actionBar(service, player, "&6⚔ Battle Roar &8» &eunleashed");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerItemHeldEvent event) {
        combatState.markWeaponSwap(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            combatState.startSprint(event.getPlayer().getUniqueId());
        } else {
            combatState.stopSprint(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatState.clear(event.getPlayer().getUniqueId());
    }
}
