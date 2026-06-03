package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hub.HubService;
import dev.zcripted.obx.hub.item.HubItems;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;

/**
 * Jump-To Rod: teleports the holder to the bobber. The teleport fires on the
 * reel-in (the player's 2nd right-click) as well as when the hook lands, so the
 * player can reel in while the bobber is still mid-air and be moved to it —
 * they do not have to wait for it to hit a block. Only the initial cast
 * ({@code FISHING}) and the passive waiting states ({@code BITE}, {@code LURED})
 * are skipped; every reel/resolved state ({@code IN_GROUND}, {@code CAUGHT_*},
 * {@code FAILED_ATTEMPT}, {@code REEL_IN}) teleports to the hook's current
 * location, range-guarded by {@code items.jump-rod.max-distance}.
 *
 * <h3>Why reflection for getHook()</h3>
 * <p>OBX compiles against the Spigot 1.12 API where
 * {@code PlayerFishEvent.getHook()} returns {@code org.bukkit.entity.Fish}.
 * On modern Paper / Spigot the return type is {@code FishHook} and the old
 * {@code Fish} interface has been removed in 1.21+. Calling {@code getHook()}
 * directly would bake the {@code ()Lorg/bukkit/entity/Fish;} descriptor into
 * the classfile, which throws {@link NoSuchMethodError} at runtime on 1.21+
 * because Paper exposes {@code ()Lorg/bukkit/entity/FishHook;} instead.
 *
 * <p>Looking the method up reflectively and invoking through the {@link Entity}
 * supertype (which both legacy {@code Fish} and modern {@code FishHook}
 * implement) keeps the single-JAR target working from 1.8.8 through 1.21+.
 * This mirrors {@code VanishManager}'s reflective {@code hidePlayer} pattern.
 */
public final class HubFishingListener implements Listener {

    /** Cached {@code PlayerFishEvent.getHook} method — resolved once at class load. */
    private static final Method GET_HOOK = lookupGetHook();

    private static Method lookupGetHook() {
        try {
            Method method = PlayerFishEvent.class.getMethod("getHook");
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private final OBX plugin;
    private final HubService hub;

    public HubFishingListener(OBX plugin, HubService hub) {
        this.plugin = plugin;
        this.hub = hub;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!hub.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!hub.isInHubWorld(player)) {
            return;
        }
        ItemStack held;
        try {
            held = player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            // 1.8 — no main-hand split.
            held = player.getItemInHand();
        }
        if (!HubItems.ID_JUMP_ROD.equals(HubItems.getId(plugin, held))) {
            return;
        }
        if (!player.hasPermission("obx.hub.jumprod")) {
            return;
        }
        // Teleport to wherever the bobber currently is. We fire on the reel-in
        // (the player's 2nd right-click) AND when the hook lands — so the player
        // does NOT have to wait for the hook to hit a block; reeling in while the
        // bobber is still mid-air teleports them to it. Only the initial cast and
        // the passive waiting/biting states are skipped.
        String stateName = event.getState().name();
        if ("FISHING".equals(stateName) || "BITE".equals(stateName) || "LURED".equals(stateName)) {
            return;
        }
        Entity hook = resolveHook(event);
        if (hook == null) {
            return;
        }
        Location destination = hook.getLocation().clone();

        // Range guard — vanilla hook can be cheated past its visual range
        // with high latency. Reject teleports past the configured max (defaults
        // to 60, comfortably beyond the rod's ~33-block reach, so legitimate
        // casts up to the rod's max distance always go through).
        int maxDistance = hub.jumpRodMaxDistance();
        if (destination.getWorld() == null
                || destination.getWorld() != player.getWorld()
                || player.getLocation().distance(destination) > maxDistance) {
            return;
        }

        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());

        // Reset velocity so the player doesn't keep momentum that would
        // catapult them off the landing pad.
        plugin.getSchedulerAdapter().runAtEntity(player, () -> {
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(destination);
            player.setFallDistance(0f);
        });
    }

    /**
     * Reflective {@code getHook()} invocation that survives the
     * 1.12 → 1.21 return-type rename ({@code Fish} → {@code FishHook}). Both
     * concrete types implement {@link Entity}, which gives us
     * {@link Entity#getLocation()} without needing to cast through the
     * version-specific subtype.
     */
    private static Entity resolveHook(PlayerFishEvent event) {
        if (GET_HOOK == null) {
            return null;
        }
        try {
            Object result = GET_HOOK.invoke(event);
            return result instanceof Entity ? (Entity) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
