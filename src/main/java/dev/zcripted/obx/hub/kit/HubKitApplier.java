package dev.zcripted.obx.hub.kit;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.hub.HubService;
import dev.zcripted.obx.hub.item.HubItems;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Applies (and re-applies) the hub hotbar kit to players. Called from:
 * <ul>
 *   <li>{@code HubJoinListener} on PlayerJoinEvent into a hub world</li>
 *   <li>{@code HubJoinListener} on PlayerRespawnEvent in a hub world</li>
 *   <li>{@code HubJoinListener} on PlayerChangedWorldEvent into a hub world</li>
 *   <li>{@code HubCommand} when an admin toggles hub-mode on (applies to
 *       every currently-online player in a hub world)</li>
 *   <li>{@code HubCommand /hub give} when an admin manually requests a kit</li>
 * </ul>
 *
 * <p>Outside hub worlds (or with hub-mode disabled) this applier never
 * runs — the entire feature is dormant in that state.
 */
public final class HubKitApplier {

    private final Main plugin;
    private final HubService hub;

    public HubKitApplier(Main plugin, HubService hub) {
        this.plugin = plugin;
        this.hub = hub;
    }

    /**
     * Applies the hub kit to {@code player}. No-op if hub-mode is off or the
     * player is not in a hub world.
     */
    public void apply(Player player) {
        if (player == null || !hub.appliesTo(player)) {
            return;
        }
        applyInternal(player);
    }

    /**
     * Applies the kit unconditionally — used by admin {@code /hub give} where
     * the world check is intentional bypass (e.g. give the kit to a player
     * in a non-hub world for testing).
     */
    public void applyForce(Player player) {
        if (player == null) {
            return;
        }
        applyInternal(player);
    }

    /** Applies the kit to every online player currently in a hub world. */
    public void applyToAllInHubWorlds() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (hub.isInHubWorld(player)) {
                applyInternal(player);
            }
        }
    }

    private void applyInternal(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (hub.kitClearInventory()) {
            inventory.clear();
            try {
                inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
            } catch (Throwable ignored) {
            }
        }

        // Resolve every enabled + permitted item to a UNIQUE hotbar slot. If two
        // items are configured to the same slot (or two clamp to the same value)
        // the naive "setItem per item" approach would silently overwrite the
        // earlier item — e.g. the jump rod landing on the server-selector slot,
        // which makes the "selector" cast & teleport instead of opening the GUI.
        // claimSlot() guarantees no overwrite by relocating collisions to the
        // next free slot (first-declared item keeps its configured slot).
        boolean[] occupied = new boolean[9];
        boolean launchpadGranted = false;

        if (hub.isItemEnabled(HubItems.ID_SERVER_SELECTOR) && player.hasPermission("obx.hub.selector")) {
            int slot = claimSlot(occupied, hub.itemSlot(HubItems.ID_SERVER_SELECTOR, 0), HubItems.ID_SERVER_SELECTOR);
            if (slot >= 0) {
                inventory.setItem(slot, HubItems.buildServerSelector(plugin, hub));
            }
        }

        if (hub.isItemEnabled(HubItems.ID_JUMP_ROD) && player.hasPermission("obx.hub.jumprod")) {
            int slot = claimSlot(occupied, hub.itemSlot(HubItems.ID_JUMP_ROD, 1), HubItems.ID_JUMP_ROD);
            if (slot >= 0) {
                inventory.setItem(slot, HubItems.buildJumpRod(plugin, hub));
            }
        }

        if (hub.isItemEnabled(HubItems.ID_VANISH_ALL) && player.hasPermission("obx.hub.vanishall")) {
            int slot = claimSlot(occupied, hub.itemSlot(HubItems.ID_VANISH_ALL, 4), HubItems.ID_VANISH_ALL);
            if (slot >= 0) {
                // Default state on a fresh kit = players visible.
                inventory.setItem(slot, HubItems.buildVanishAll(plugin, hub, true));
            }
        }

        if (hub.isItemEnabled(HubItems.ID_LAUNCHPAD) && player.hasPermission("obx.hub.launchpad")) {
            int slot = claimSlot(occupied, hub.itemSlot(HubItems.ID_LAUNCHPAD, 8), HubItems.ID_LAUNCHPAD);
            if (slot >= 0) {
                inventory.setItem(slot, HubItems.buildLaunchpad(plugin, hub));
            }
            launchpadGranted = true;
        }

        // Hub mode quality-of-life: keep adventure-mode-style "can't break
        // blocks", and grant double-jump flight ONLY to launchpad-eligible
        // players. Flight is set AFTER the gamemode change (which resets ability
        // flags) and only managed for survival/adventure so we never clobber a
        // creative/spectator player's flight.
        try {
            GameMode mode = player.getGameMode();
            if (mode == GameMode.SURVIVAL) {
                player.setGameMode(GameMode.ADVENTURE);
                mode = GameMode.ADVENTURE;
            }
            if (mode == GameMode.ADVENTURE || mode == GameMode.SURVIVAL) {
                player.setAllowFlight(launchpadGranted);
                if (!launchpadGranted) {
                    player.setFlying(false);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Claims a unique hotbar slot. Returns {@code requested} when it is a valid
     * (0–8) and still-free slot, otherwise the next free slot, otherwise -1 when
     * the hotbar is full. Marks the chosen slot occupied. A relocation is logged
     * so a misconfigured duplicate slot is visible to the admin.
     */
    private int claimSlot(boolean[] occupied, int requested, String itemId) {
        if (requested >= 0 && requested < occupied.length && !occupied[requested]) {
            occupied[requested] = true;
            return requested;
        }
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                occupied[i] = true;
                plugin.getLogger().warning("[Hub] Hotbar slot " + requested + " for item '"
                        + itemId + "' was already taken; relocated to slot " + i
                        + ". Check items.*.slot in systems/hub.yml for duplicates.");
                return i;
            }
        }
        return -1;
    }

    /**
     * Strips hub-granted double-jump flight from every online player currently in
     * a hub world. Called when hub-mode is toggled off so players can't keep the
     * launchpad's {@code allowFlight} grant and free-fly after the system is
     * disabled. Creative / spectator flight is left untouched.
     */
    public void revokeFlightInHubWorlds() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!hub.isInHubWorld(player)) {
                continue;
            }
            try {
                GameMode mode = player.getGameMode();
                if (mode == GameMode.ADVENTURE || mode == GameMode.SURVIVAL) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
