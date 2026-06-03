package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.EffectUtil;
import dev.zcripted.obx.feature.enchant.effect.EnchantState;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.Sounds;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defense-category effects: Aegis (flat reduction), Thornmail (reflect),
 * Last Stand &amp; Phoenix Feather (lethal saves), Second Wind (low-HP burst),
 * Phase Cloak (invis on hit), and Soulbound (keep an item through death).
 */
public final class DefenseEnchantListener implements Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final EnchantState state;

    /** Items kept by Soulbound, returned on respawn. */
    private final Map<UUID, List<ItemStack>> soulbound = new ConcurrentHashMap<UUID, List<ItemStack>>();

    public DefenseEnchantListener(ObxPlugin plugin, EnchantState state) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.state = state;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();

        int aegis = EffectUtil.armorLevel(storage, player, "aegis");
        if (aegis > 0) {
            double reduction = service.getRegistry().get("aegis").levelDouble(aegis, "reduction", 0.0);
            event.setDamage(Math.max(0.0, event.getDamage() * (1.0 - reduction)));
        }

        // Curse of the Bound's armor toughness is applied as the real
        // GENERIC_ARMOR_TOUGHNESS attribute (maintained by EnchantTickTask), so
        // there is nothing to do here for it.

        double projected = player.getHealth() - event.getFinalDamage();
        if (projected <= 0.0) {
            if (tryLastStand(player) || tryPhoenix(player)) {
                event.setDamage(0.0);
                return;
            }
        }

        int phase = EffectUtil.armorLevel(storage, player, "phase_cloak");
        if (phase > 0) {
            CustomEnchant e = service.getRegistry().get("phase_cloak");
            if (Math.random() < e.levelDouble(phase, "chance", 0.0)) {
                Potions.apply(player, Potions.INVISIBILITY, e.levelInt(phase, "invis_seconds", 2) * 20, 0);
                if (e.levelBoolean(phase, "speed", false)) {
                    Potions.apply(player, Potions.SPEED, 60, 0);
                }
            }
        }

        // Second Wind — checked after the hit lands.
        final int secondWind = EffectUtil.armorLevel(storage, player, "second_wind");
        if (secondWind > 0 && !state.onCooldown(player, "second_wind")) {
            final CustomEnchant e = service.getRegistry().get("second_wind");
            final double threshold = e.levelDouble(secondWind, "hp_threshold", 0.3);
            final UUID id = player.getUniqueId();
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Player p = plugin.getServer().getPlayer(id);
                    if (p == null || !p.isOnline() || !p.isValid()) {
                        return;
                    }
                    if (EffectUtil.healthFraction(p) <= threshold) {
                        Potions.applyLevel(p, Potions.SPEED, 60, e.levelInt(secondWind, "speed_amp", 1));
                        Potions.apply(p, Potions.REGENERATION, 60, 1);
                        EffectUtil.heal(p, e.levelInt(secondWind, "heal_hearts", 1) * 2.0);
                        state.setCooldown(p, "second_wind", e.levelInt(secondWind, "cooldown_seconds", 60));
                        Sounds.confirm(p);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        final Player victim = (Player) event.getEntity();
        int thorns = EffectUtil.armorLevel(storage, victim, "thornmail");
        if (thorns > 0 && event.getDamager() instanceof LivingEntity) {
            CustomEnchant e = service.getRegistry().get("thornmail");
            double pct = e.levelDouble(thorns, "reflect_percent", 0.0);
            double reflect = event.getFinalDamage() * pct;
            if (reflect > 0) {
                try {
                    ((LivingEntity) event.getDamager()).damage(reflect, victim);
                } catch (Throwable ignored) {
                }
            }
        }

        // Shieldbreaker Resist — chance to ignore an axe shield-disable.
        int shieldResist = shieldEnchantLevel(victim, "shieldbreaker_resist");
        if (shieldResist > 0 && isBlocking(victim) && usedAxe(event.getDamager())) {
            double ignore = service.getRegistry().get("shieldbreaker_resist").levelDouble(shieldResist, "ignore_chance", 0.0);
            if (Math.random() < ignore) {
                final Material shield = Material.matchMaterial("SHIELD");
                if (shield != null) {
                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                victim.setCooldown(shield, 0);
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                }
            }
        }

        // Curse of the Bound — reduced knockback. Applied one tick later (after
        // vanilla knockback resolves) and only to the horizontal velocity so jumps
        // and knock-up aren't flattened.
        int bound = storage.level(victim.getInventory().getBoots(), "curse_of_the_bound");
        if (bound > 0) {
            final double factor = Math.max(0.0, 1.0 - service.getRegistry().get("curse_of_the_bound").levelDouble(bound, "knockback_reduction", 0.0));
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (victim.isOnline()) {
                        org.bukkit.util.Vector v = victim.getVelocity();
                        victim.setVelocity(new org.bukkit.util.Vector(v.getX() * factor, v.getY(), v.getZ() * factor));
                    }
                }
            });
        }
    }

    private int shieldEnchantLevel(Player player, String id) {
        int main = storage.level(EffectUtil.mainHand(player), id);
        int off = 0;
        try {
            off = storage.level(player.getInventory().getItemInOffHand(), id);
        } catch (Throwable ignored) {
        }
        return Math.max(main, off);
    }

    private boolean isBlocking(Player player) {
        try {
            return player.isBlocking();
        } catch (Throwable legacy) {
            return false;
        }
    }

    private boolean usedAxe(org.bukkit.entity.Entity damager) {
        if (!(damager instanceof Player)) {
            return false;
        }
        return EffectUtil.mainHand((Player) damager).getType().name().endsWith("_AXE");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Player player = event.getEntity();
        List<ItemStack> kept = new ArrayList<ItemStack>();
        java.util.Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (storage.level(drop, "soulbound") > 0) {
                CustomEnchant sb = service.getRegistry().get("soulbound");
                // Single-use: consume the enchant, keep the item.
                storage.remove(drop, "soulbound");
                kept.add(drop.clone());
                it.remove();
            }
        }
        if (!kept.isEmpty()) {
            soulbound.put(player.getUniqueId(), kept);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        state.clearLifeFlags(player);
        List<ItemStack> kept = soulbound.remove(player.getUniqueId());
        if (kept != null) {
            for (ItemStack item : kept) {
                player.getInventory().addItem(item);
            }
        }
    }

    private boolean tryLastStand(Player player) {
        int level = EffectUtil.armorLevel(storage, player, "last_stand");
        if (level <= 0 || state.onCooldown(player, "last_stand_life")) {
            return false;
        }
        CustomEnchant e = service.getRegistry().get("last_stand");
        int duration = e.levelInt(level, "duration_seconds", 5) * 20;
        player.setHealth(Math.min(EffectUtil.maxHealth(player), 4.0));
        Potions.applyLevel(player, Potions.RESISTANCE, duration, e.levelInt(level, "resistance_amp", 2));
        Potions.applyLevel(player, Potions.REGENERATION, duration, e.levelInt(level, "regen_amp", 1));
        if (e.levelBoolean(level, "absorption", false)) {
            Potions.apply(player, Potions.ABSORPTION, duration, 0);
        }
        // Block re-trigger until respawn.
        state.setCooldown(player, "last_stand_life", 60L * 60L * 24L);
        Particles.burst(player.getLocation().add(0, 1, 0), Particles.TOTEM);
        Sounds.success(player);
        plugin.getLanguageManager().send(player, "enchant.effect.last-stand");
        return true;
    }

    private boolean tryPhoenix(Player player) {
        int level = EffectUtil.armorLevel(storage, player, "phoenix_feather");
        if (level <= 0 || state.onCooldown(player, "phoenix_feather")) {
            return false;
        }
        CustomEnchant e = service.getRegistry().get("phoenix_feather");
        double pct = e.levelDouble(level, "revive_percent", 0.5);
        player.setHealth(Math.max(1.0, EffectUtil.maxHealth(player) * pct));
        player.setFireTicks(0);
        Potions.apply(player, Potions.REGENERATION, 100, 1);
        state.setCooldown(player, "phoenix_feather", e.levelInt(level, "cooldown_hours", 24) * 3600L);
        Particles.burst(player.getLocation().add(0, 1, 0), Particles.TOTEM);
        Sounds.success(player);
        plugin.getLanguageManager().send(player, "enchant.effect.phoenix");
        return true;
    }
}
