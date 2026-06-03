package dev.zcripted.obx.enchant.listener;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.enchant.effect.BoundMovement;
import dev.zcripted.obx.enchant.effect.EffectUtil;
import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.service.EnchantService;
import dev.zcripted.obx.enchant.storage.EnchantStorage;
import dev.zcripted.obx.enchant.util.Potions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Cursed-category passive drawbacks: Curse of the Glutton (food buffs + debuff),
 * Curse of the Sleepless (no sleep), Curse of the Bound (no sprint), and Curse of
 * Greed (reduced healing). Faster hunger drain (Curse of Hunger) is applied by
 * the tick task; the combat upsides are in the combat listener.
 */
public final class CursedEnchantListener implements Listener {

    private final Main plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final BoundMovement boundMovement;

    public CursedEnchantListener(Main plugin, BoundMovement boundMovement) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
        this.boundMovement = boundMovement;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        ItemStack food = event.getItem();
        int level = storage.level(food, "curse_of_the_glutton");
        if (level <= 0) {
            return;
        }
        Player player = event.getPlayer();
        CustomEnchant e = service.getRegistry().get("curse_of_the_glutton");
        Potions.applyLevel(player, Potions.STRENGTH, e.levelInt(level, "strength_seconds", 60) * 20, e.levelInt(level, "strength_amp", 2));
        Potions.applyLevel(player, Potions.HUNGER, e.levelInt(level, "hunger_seconds", 30) * 20, e.levelInt(level, "hunger_amp", 2));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (EffectUtil.armorLevel(storage, player, "curse_of_the_sleepless") > 0) {
            event.setCancelled(true);
            plugin.getLanguageManager().send(player, "enchant.effect.sleepless");
        }
    }

    /**
     * "Cannot sprint" enforcement. Cancelling the toggle / {@code setSprinting(false)}
     * is ineffective on modern clients (and the hunger gate is bypassed in creative),
     * so {@link BoundMovement} removes the sprint speed benefit via the abilities
     * walk/fly speed instead — restored the moment the player stops sprinting.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        boundMovement.update(event.getPlayer(), event.isSprinting());
    }

    /** Clean up the Bound speed throttle and toughness attribute on logout. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boundMovement.restore(event.getPlayer());
        EffectUtil.setBoundToughness(event.getPlayer(), 0.0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        int level = storage.level(EffectUtil.mainHand(player), "curse_of_greed");
        if (level <= 0) {
            return;
        }
        double reduction = service.getRegistry().get("curse_of_greed").levelDouble(level, "healing_reduction", 0.0);
        event.setAmount(Math.max(0.0, event.getAmount() * (1.0 - reduction)));
    }
}
