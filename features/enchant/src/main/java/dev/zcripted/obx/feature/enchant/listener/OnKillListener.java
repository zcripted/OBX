package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.effect.EndlessHunger;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.CombatParticleService;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.service.HoloFXService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.SoundPalette;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Kill-triggered combat enchants. Currently grants Soulreaver kill-stacks and the
 * Wrath of the Wild cluster heal; killstreak banners and loot effects land with
 * the holographic-FX phase.
 */
public final class OnKillListener implements Listener {

    private static final String[] FEAR_SOUND = {"ENTITY_RAVAGER_ROAR", "ENTITY_ENDER_DRAGON_GROWL", "ENTITY_WOLF_GROWL"};

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatState combatState;
    private final CombatParticleService particles;
    private final HoloFXService holo;

    public OnKillListener(ObxPlugin plugin, CombatState combatState, CombatParticleService particles, HoloFXService holo) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.combatState = combatState;
        this.particles = particles;
        this.holo = holo;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        // Purge any target-keyed combat state (marks / stuns / tethers) for the dead entity.
        // Must run regardless of killer so mob-keyed entries can't accumulate forever.
        combatState.removeEntity(dead.getUniqueId());
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = CombatSupport.mainHand(killer);
        if (weapon == null) {
            return;
        }

        int soulreaver = storage.level(weapon, "soulreaver");
        if (soulreaver > 0) {
            CustomEnchant e = service.getRegistry().get("soulreaver");
            int max = e.levelInt(soulreaver, "max_stacks", 5);
            long decayMillis = (long) e.levelInt(soulreaver, "decay_seconds", 10) * 1000L;
            combatState.addSoulreaverStack(killer.getUniqueId(), max, decayMillis);
            int stacks = combatState.soulreaverStacks(killer.getUniqueId());
            particles.spawnAura(killer, Particles.PORTAL, 8);
            CombatSupport.actionBar(service, killer, "&5Soulreaver &8» &d" + stacks + "/" + max + " stacks");
        }

        int wrath = storage.level(weapon, "wrath_of_the_wild");
        if (wrath > 0) {
            CustomEnchant e = service.getRegistry().get("wrath_of_the_wild");
            double heal = e.levelDouble(wrath, "heal_on_kill", 0.0);
            if (heal > 0 && CombatSupport.countNearbyHostiles(killer, e.levelDouble(wrath, "radius", 6.0)) >= 1) {
                CombatSupport.heal(killer, heal);
            }
        }

        // Killstreak — escalating streak with a banner (RAMPAGE at 5+).
        int killstreak = storage.level(weapon, "killstreak");
        if (killstreak > 0) {
            CustomEnchant e = service.getRegistry().get("killstreak");
            int max = e.levelInt(killstreak, "max_streak", 3);
            long window = (long) e.levelInt(killstreak, "window_seconds", 8) * 1000L;
            int streak = Math.min(max, combatState.registerKill(killer.getUniqueId(), window));
            if (streak >= 2) {
                boolean rampage = streak >= 5;
                String banner = rampage
                        ? "&c&l✦ RAMPAGE &6×" + streak
                        : "&6&l✦ KILLSTREAK &e×" + streak;
                holo.showKillBanner(killer, banner);
                SoundPalette.play(killer, SoundPalette.STREAK, CombatSupport.volume(service, 0.7f),
                        Math.min(2.0f, 1.0f + streak * 0.1f));
            }
        }

        // Executioner's Cry — a kill terrifies (debuffs) nearby foes.
        int cry = storage.level(weapon, "executioners_cry");
        if (cry > 0) {
            CustomEnchant e = service.getRegistry().get("executioners_cry");
            double radius = e.levelDouble(cry, "radius", 4.0);
            int duration = e.levelInt(cry, "duration_ticks", 60);
            int slowness = e.levelInt(cry, "slowness", 1);
            int weakness = e.levelInt(cry, "weakness", 0);
            for (Entity near : killer.getNearbyEntities(radius, radius, radius)) {
                if (near instanceof LivingEntity && !near.equals(killer)) {
                    LivingEntity target = (LivingEntity) near;
                    if (slowness > 0) {
                        Potions.applyLevel(target, Potions.SLOWNESS, duration, slowness);
                    }
                    if (weakness > 0) {
                        Potions.applyLevel(target, Potions.WEAKNESS, duration, weakness);
                    }
                }
            }
            particles.spawnShockwave(killer.getLocation(), radius, new String[]{"SMOKE_NORMAL", "CRIT"}, 8);
            holo.showKillBanner(killer, "&4&l☠ EXECUTIONER");
            SoundPalette.play(killer, FEAR_SOUND, CombatSupport.volume(service, 0.8f), 0.6f);
        }

        // Endless Hunger — every kill feeds the weapon; milestones add a permanent stack.
        int hunger = storage.level(weapon, "endless_hunger");
        if (hunger > 0) {
            boolean milestone = EndlessHunger.record(killer, weapon, service.getRegistry().get("endless_hunger"), hunger);
            if (milestone) {
                particles.spawnAura(killer, new String[]{"FLAME", "LAVA", "CRIT"}, 12);
                SoundPalette.play(killer, new String[]{"ENTITY_PLAYER_LEVELUP", "LEVEL_UP"}, CombatSupport.volume(service, 0.7f), 0.8f);
                int stacks = EndlessHunger.stacks(EndlessHunger.kills(weapon), service.getRegistry().get("endless_hunger"), hunger);
                holo.showKillBanner(killer, "&4&l☠ HUNGER &c+" + stacks + "%");
            }
        }

        // Apex Predator — gold burst on every kill + a chance to harvest a scroll.
        int apex = storage.level(weapon, "apex_predator");
        if (apex > 0) {
            CustomEnchant e = service.getRegistry().get("apex_predator");
            Particles.burst(dead.getEyeLocation(), new String[]{"FIREWORKS_SPARK", "FLAME", "CRIT"});
            particles.spawnAura(killer, new String[]{"FIREWORKS_SPARK", "FLAME"}, 8);
            if (Math.random() < e.levelDouble(apex, "scroll_drop_chance", 0.05)) {
                dropRandomScroll(dead.getLocation());
                holo.showKillBanner(killer, "&6&l✦ HARVEST");
                SoundPalette.play(killer, SoundPalette.CRIT_GOLD, CombatSupport.volume(service, 0.8f), 1.4f);
            }
        }
    }

    /** Drops a random Rare or Epic enchant scroll at {@code loc} (Apex Predator harvest). */
    private void dropRandomScroll(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        try {
            List<CustomEnchant> pool = new ArrayList<CustomEnchant>();
            for (CustomEnchant enchant : service.getRegistry().all()) {
                String rarity = enchant.getRarity().name();
                if ("RARE".equals(rarity) || "EPIC".equals(rarity)) {
                    pool.add(enchant);
                }
            }
            if (pool.isEmpty()) {
                return;
            }
            CustomEnchant chosen = pool.get((int) (Math.random() * pool.size()));
            ItemStack scroll = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.item.EnchantItems.class).scroll(chosen, 1, 1);
            if (scroll != null) {
                loc.getWorld().dropItemNaturally(loc, scroll);
            }
        } catch (Throwable ignored) {
            // scroll factory unavailable / odd world — skip the drop
        }
    }
}
