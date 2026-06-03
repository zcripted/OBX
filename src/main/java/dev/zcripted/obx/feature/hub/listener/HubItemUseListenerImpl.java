package dev.zcripted.obx.feature.hub.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hub.gui.ServerSelectorMenu;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.feature.hub.item.HubItems;
import dev.zcripted.obx.feature.staff.service.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interaction dispatcher for hub hotbar items. Routes by hub item ID rather
 * than by material or display name so user-customised lores / colors don't
 * break the routing.
 *
 * <p>Runs at {@link EventPriority#LOWEST} and, for any hub item, cancels the
 * interaction and denies the vanilla item use on BOTH left and right clicks.
 * This is a cross-plugin safeguard: it stops other plugins (e.g. WorldEdit's
 * compass navigation wand — {@code /thru} on right-click, {@code /jumpto} on
 * left-click — and its selection wand) from applying their own functions to
 * OBX hub items, regardless of the item's material. The single exception is
 * the jump-rod's right-click, which is left through so the vanilla fishing cast
 * (which powers the teleport) can run.
 */
public final class HubItemUseListenerImpl implements Listener, dev.zcripted.obx.api.hub.HubItemUseListener {

    /**
     * Cached {@code PlayerInteractEvent.getHand()} — added in 1.9. On 1.9+ a
     * single right-click fires this event TWICE (once for each hand); without
     * filtering to the main hand every handler runs twice, which made the
     * vanish toggle flip on→off→on (appearing to do nothing) and opened the
     * selector menu twice. {@code null} on 1.8 where the event fires once.
     */
    private static final Method GET_HAND = resolveGetHand();

    private static Method resolveGetHand() {
        try {
            return PlayerInteractEvent.class.getMethod("getHand");
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Window within which a repeat fire of the SAME hub item is treated as a duplicate. */
    private static final long DUPLICATE_FIRE_WINDOW_MS = 150L;

    private final ObxPlugin plugin;
    private final HubService hub;

    /** Per-player "are other players hidden by my vanish-all toggle?" state. */
    private final Map<UUID, Boolean> vanishAllState = new ConcurrentHashMap<>();

    /** Last action time per {@code uuid|itemId} — debounces duplicate interact fires. */
    private final Map<String, Long> lastActionAt = new ConcurrentHashMap<>();

    public HubItemUseListenerImpl(ObxPlugin plugin, HubService hub) {
        this.plugin = plugin;
        this.hub = hub;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!hub.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!hub.isInHubWorld(player)) {
            return;
        }
        Action action = event.getAction();
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!leftClick && !rightClick) {
            return;
        }
        ItemStack stack;
        try {
            stack = event.getItem();
        } catch (Throwable ignored) {
            stack = player.getInventory().getItemInHand();
        }
        if (stack == null) {
            return;
        }
        String id = HubItems.getId(plugin, stack);
        if (id == null) {
            return;
        }

        // ── Cross-plugin safeguard ──────────────────────────────────────────
        // This is an OBX hub item, so claim the interaction. We run at LOWEST
        // priority (before WorldEdit & friends) and DENY the vanilla item use, on
        // BOTH left and right clicks. That stops other plugins from applying their
        // own functions to the item regardless of its material — most importantly
        // WorldEdit's compass navigation wand (right-click /thru, left-click
        // /jumpto) and its selection wand, all of which honor a cancelled interact.
        // The ONLY exception is the jump-rod's RIGHT-click: it must run the vanilla
        // fishing cast that powers the teleport, so we leave that one through.
        boolean jumpRodCast = rightClick && HubItems.ID_JUMP_ROD.equals(id);
        if (!jumpRodCast) {
            event.setCancelled(true);
            denyVanillaUse(event);
        }

        // Left-clicks never drive a hub action — they're only blocked above.
        if (!rightClick) {
            return;
        }
        // Process the main-hand fire only. On 1.9+ a single right-click fires
        // this event for both hands; handling both flips toggles twice and
        // double-opens the menu.
        if (isOffHandFire(event)) {
            return;
        }
        // Debounce duplicate fires of the SAME item (any residual client/packet
        // duplication) so one click = one action. The jump rod is exempt — it
        // performs no action here and relies on PlayerFishEvent.
        if (!HubItems.ID_JUMP_ROD.equals(id) && isDuplicateFire(player, id)) {
            return;
        }

        switch (id) {
            case HubItems.ID_SERVER_SELECTOR:
                if (!player.hasPermission("obx.hub.selector")) {
                    plugin.getLanguageManager().send(player, "core.no-permission");
                    return;
                }
                ServerSelectorMenu.open(plugin, player);
                return;
            case HubItems.ID_VANISH_ALL:
                if (!player.hasPermission("obx.hub.vanishall")) {
                    plugin.getLanguageManager().send(player, "core.no-permission");
                    return;
                }
                toggleVanishAll(player);
                return;
            case HubItems.ID_LAUNCHPAD:
                // Already cancelled above (suppresses vanilla firework use). Launch
                // directly so it works on a plain right-click as well as the
                // double-tap-space path in HubLaunchpadListener.
                if (hub.isItemEnabled(HubItems.ID_LAUNCHPAD) && player.hasPermission("obx.hub.launchpad")) {
                    HubLaunchpadListener.launch(hub, plugin.getServiceRegistry().get(dev.zcripted.obx.feature.hub.launchpad.LaunchpadCooldownManager.class), player);
                }
                return;
            case HubItems.ID_JUMP_ROD:
                // Not cancelled above — the vanilla cast must run (→ PlayerFishEvent
                // → HubFishingListener teleport). Only block it when unauthorized.
                if (!player.hasPermission("obx.hub.jumprod")) {
                    event.setCancelled(true);
                    denyVanillaUse(event);
                    plugin.getLanguageManager().send(player, "core.no-permission");
                }
                return;
            default:
                // Unknown hub item — already protected above.
        }
    }

    /**
     * Denies the vanilla outcome of an interaction with a hub item so other
     * plugins that honor the interact result (e.g. WorldEdit's tools) don't act
     * on it. {@link PlayerInteractEvent#setCancelled(boolean)} already implies
     * this, but setting the results explicitly is belt-and-braces for forks /
     * plugins that read {@code useItemInHand()} directly.
     */
    private static void denyVanillaUse(PlayerInteractEvent event) {
        try {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        } catch (Throwable ignored) {
            // Result setters vary on very old APIs; setCancelled(true) covers it.
        }
    }

    /**
     * Returns true if the same player triggered the same hub item within
     * {@link #DUPLICATE_FIRE_WINDOW_MS}. Records the current time either way, so
     * the duplicate copy of a dual-fire (which arrives in the same tick) is
     * dropped while a genuine later click still goes through.
     */
    private boolean isDuplicateFire(Player player, String id) {
        long now = System.currentTimeMillis();
        Long previous = lastActionAt.put(player.getUniqueId() + "|" + id, now);
        return previous != null && (now - previous) < DUPLICATE_FIRE_WINDOW_MS;
    }

    /** True only for the off-hand copy of a dual-fire interact (1.9+). */
    private static boolean isOffHandFire(PlayerInteractEvent event) {
        if (GET_HAND == null) {
            return false; // 1.8 — the event fires once, always "main hand".
        }
        try {
            Object hand = GET_HAND.invoke(event);
            return hand != null && "OFF_HAND".equals(String.valueOf(hand));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void toggleVanishAll(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyHidden = vanishAllState.getOrDefault(uuid, Boolean.FALSE);
        boolean nextHidden = !currentlyHidden;
        vanishAllState.put(uuid, nextHidden);

        dev.zcripted.obx.api.staff.VanishApi vanishManager = plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == null || other.getUniqueId().equals(uuid)) {
                continue;
            }
            try {
                if (nextHidden) {
                    hidePlayerCompat(player, other);
                } else {
                    if (vanishManager != null && vanishManager.isVanished(other) && !player.hasPermission("obx.vanish")) {
                        // Don't reveal staff-vanished players to non-staff.
                        continue;
                    }
                    showPlayerCompat(player, other);
                }
            } catch (Throwable ignored) {
            }
        }

        // Swap the hotbar item to reflect the new state.
        PlayerInventory inventory = player.getInventory();
        int slot = hub.itemSlot(HubItems.ID_VANISH_ALL, 4);
        boolean playersVisible = !nextHidden;
        inventory.setItem(slot, HubItems.buildVanishAll(plugin, hub, playersVisible));

        plugin.getLanguageManager().send(player, nextHidden
                ? "hub.vanishall.hidden"
                : "hub.vanishall.visible");
    }

    /** Cleanup hook used by HubService when toggling the system off. */
    public void clearAllVanishState() {
        vanishAllState.clear();
    }

    /**
     * Reveals every player to every other player (respecting staff vanish) and
     * clears the per-player vanish-all memory. Called when hub-mode is toggled
     * off so a player who hid others via the vanish-all toggle isn't left with
     * them permanently hidden after the system is disabled.
     */
    public void resetVisibilityForAll() {
        dev.zcripted.obx.api.staff.VanishApi vanishManager = plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer == null || target == null || viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }
                try {
                    if (vanishManager != null && vanishManager.isVanished(target)
                            && !viewer.hasPermission("obx.vanish")) {
                        // Keep staff-vanished players hidden from non-staff.
                        continue;
                    }
                    showPlayerCompat(viewer, target);
                } catch (Throwable ignored) {
                }
            }
        }
        vanishAllState.clear();
    }

    /**
     * Cross-version hide. The 1.13+ {@code hidePlayer(Plugin, Player)} overload
     * isn't on the 1.12 compile baseline; the deprecated single-arg overload
     * exists from 1.8.8 → modern (just emits a deprecation warning), so we
     * call that directly. {@link VanishManager} uses reflection for the
     * plugin-aware overload because it cares about per-plugin state — here
     * we don't.
     */
    @SuppressWarnings("deprecation")
    private static void hidePlayerCompat(Player viewer, Player target) {
        viewer.hidePlayer(target);
    }

    @SuppressWarnings("deprecation")
    private static void showPlayerCompat(Player viewer, Player target) {
        viewer.showPlayer(target);
    }
}
