package dev.sergeantfuzzy.sfcore.enchant.listener;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.effect.EnchantState;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import dev.sergeantfuzzy.sfcore.enchant.storage.EnchantStorage;
import dev.sergeantfuzzy.sfcore.enchant.util.Particles;
import dev.sergeantfuzzy.sfcore.enchant.util.Potions;
import dev.sergeantfuzzy.sfcore.enchant.util.Sounds;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Mystic movement effects bound to boots: Wind Step (double jump via the
 * flight-toggle trick), Voidwalker (sneak-in-air blink), Time Slip (sneak slow
 * bubble), and Beacon's Memory binding (sneak + right-click). Re-granting the
 * double-jump on landing is handled by the tick task.
 */
public final class MovementEnchantListener implements Listener {

    private final Main plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final EnchantState state;

    public MovementEnchantListener(Main plugin, EnchantState state) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
        this.state = state;
    }

    @EventHandler
    public void onFlightToggle(PlayerToggleFlightEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isFlying() || player.getGameMode().name().equals("CREATIVE") || player.getGameMode().name().equals("SPECTATOR")) {
            return;
        }
        int level = bootsLevel(player, "wind_step");
        if (level <= 0) {
            return;
        }
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        if (state.onCooldown(player, "wind_step")) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("wind_step");
        Vector velocity = player.getLocation().getDirection().multiply(0.6).setY(0.9);
        player.setVelocity(velocity);
        state.setCooldown(player, "wind_step", e.levelInt(level, "cooldown_seconds", 30));
        Particles.at(player.getLocation(), Particles.CLOUD, 16, 0.3);
        Sounds.play(player, Sounds.MAGIC, 0.5f, 1.5f);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!service.isEnabled() || !event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        int voidwalker = bootsLevel(player, "voidwalker");
        if (voidwalker > 0 && !player.isOnGround() && !state.onCooldown(player, "voidwalker")) {
            blink(player, voidwalker);
            return;
        }
        int timeSlip = bootsLevel(player, "time_slip");
        if (timeSlip > 0 && !state.onCooldown(player, "time_slip")) {
            CustomEnchant e = service.getRegistry().get("time_slip");
            double radius = e.levelDouble(timeSlip, "radius", 5.0);
            int amp = e.levelInt(timeSlip, "slowness_amp", 4);
            int duration = e.levelInt(timeSlip, "duration_seconds", 2) * 20;
            for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
                if (nearby instanceof LivingEntity && nearby != player) {
                    Potions.applyLevel((LivingEntity) nearby, Potions.SLOWNESS, duration, amp);
                }
            }
            state.setCooldown(player, "time_slip", e.levelInt(timeSlip, "cooldown_seconds", 90));
            Particles.at(player.getLocation().add(0, 1, 0), Particles.MAGIC, 30, radius / 2.0);
            Sounds.play(player, Sounds.MAGIC, 0.5f, 0.6f);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!service.isEnabled() || !event.getPlayer().isSneaking()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        int level = bootsLevel(player, "beacons_memory");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("beacons_memory");
        state.bindRecall(player, player.getLocation(), e.levelInt(level, "slots", 1));
        plugin.getLanguageManager().send(player, "enchant.recall.bound");
        Sounds.confirm(player);
    }

    private void blink(Player player, int level) {
        CustomEnchant e = service.getRegistry().get("voidwalker");
        double distance = e.levelDouble(level, "distance", 5.0);
        Location from = player.getLocation();
        Vector dir = from.getDirection().normalize();
        Location target = null;
        for (double d = distance; d >= 1.0; d -= 1.0) {
            Location candidate = from.clone().add(dir.clone().multiply(d));
            if (isSafe(candidate)) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            return;
        }
        target.setYaw(from.getYaw());
        target.setPitch(from.getPitch());
        Particles.burst(from.add(0, 1, 0), Particles.PORTAL);
        player.teleport(target);
        Particles.burst(target.clone().add(0, 1, 0), Particles.PORTAL);
        if (e.levelBoolean(level, "resistance", false)) {
            Potions.apply(player, Potions.RESISTANCE, 60, 0);
        }
        state.setCooldown(player, "voidwalker", e.levelInt(level, "cooldown_seconds", 30));
        Sounds.play(player, Sounds.BLINK, 0.6f, 1.2f);
    }

    private boolean isSafe(Location loc) {
        if (loc.getWorld() == null) {
            return false;
        }
        return !loc.getBlock().getType().isSolid()
                && !loc.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private int bootsLevel(Player player, String id) {
        ItemStack boots = player.getInventory().getBoots();
        return storage.level(boots, id);
    }
}
