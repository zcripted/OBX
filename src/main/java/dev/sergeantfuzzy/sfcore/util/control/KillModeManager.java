package dev.sergeantfuzzy.sfcore.util.control;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillModeManager implements Listener {

    private static final double STEP = 0.2;

    private final Main plugin;
    private final LanguageManager languages;
    private final Set<UUID> enabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public KillModeManager(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (enabled.contains(uuid)) {
            enabled.remove(uuid);
            return false;
        }
        enabled.add(uuid);
        return true;
    }

    public boolean isEnabled(UUID uuid) {
        return uuid != null && enabled.contains(uuid);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!isEnabled(player.getUniqueId())) {
            return;
        }
        if (!player.hasPermission("sfcore.kill")) {
            enabled.remove(player.getUniqueId());
            languages.send(player, "core.no-permission");
            return;
        }

        int maxRange = Math.max(1, plugin.getConfig().getInt("targeting.kill.max-range", 40));
        LivingEntity target = findTarget(player, maxRange);
        if (target == null) {
            languages.send(player, "admin.kill.no-target", Placeholders.with("range", maxRange));
            return;
        }

        target.setHealth(0.0);
        languages.send(player, "admin.kill.success", Placeholders.with("target", formatTargetName(target)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        enabled.remove(event.getPlayer().getUniqueId());
    }

    private LivingEntity findTarget(Player player, int maxRange) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) {
            return null;
        }
        Vector direction = eye.getDirection().normalize();
        Vector step = direction.clone().multiply(STEP);
        Location point = eye.clone();
        double traveled = 0.0;
        while (traveled <= maxRange) {
            if (traveled > 0 && isBlocking(point.getBlock().getType())) {
                break;
            }
            LivingEntity hit = findLivingNear(point, player);
            if (hit != null) {
                return hit;
            }
            point.add(step);
            traveled += STEP;
        }
        return null;
    }

    private LivingEntity findLivingNear(Location point, Player player) {
        for (Entity entity : point.getWorld().getNearbyEntities(point, 0.6, 0.6, 0.6)) {
            if (entity == player) {
                continue;
            }
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (living.isDead() || !living.isValid()) {
                continue;
            }
            return living;
        }
        return null;
    }

    private boolean isBlocking(Material material) {
        return material != null && material.isSolid();
    }

    private String formatTargetName(LivingEntity target) {
        if (target instanceof Player) {
            return ((Player) target).getName();
        }
        return target.getName();
    }
}
