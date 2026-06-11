package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Utility-category event effects: Featherfall (reduced fall damage) and
 * Bottomless Quiver (chance to refund an arrow). Movement-speed, vision, glide,
 * and magnet effects are applied passively by the tick task.
 */
public final class UtilityEnchantListener implements Listener {

    private final EnchantService service;
    private final EnchantStorage storage;

    public UtilityEnchantListener(ObxPlugin plugin) {
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (!service.isEnabled() || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        int level = bootsLevel(player, "featherfall");
        if (level <= 0) {
            return;
        }
        double reduction = service.getRegistry().get("featherfall").levelDouble(level, "reduction", 0.0);
        double reduced = event.getDamage() * (1.0 - reduction);
        if (reduced <= 0.0) {
            event.setCancelled(true);
        } else {
            event.setDamage(reduced);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();
        int level = storage.level(bow, "bottomless_quiver");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("bottomless_quiver");
        if (Math.random() < e.levelDouble(level, "chance", 0.0)) {
            Material arrow = Material.matchMaterial("ARROW");
            if (arrow != null && player.getGameMode().name().equals("SURVIVAL")) {
                player.getInventory().addItem(new ItemStack(arrow, 1));
            }
        }
    }

    private int bootsLevel(Player player, String id) {
        ItemStack boots = player.getInventory().getBoots();
        return storage.level(boots, id);
    }
}