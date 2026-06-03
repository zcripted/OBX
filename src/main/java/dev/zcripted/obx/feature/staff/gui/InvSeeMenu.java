package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.warp.gui.WarpMenuStyling;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;

/**
 * Live-mirror chest GUI for {@code /invsee}. Replaces the legacy
 * {@code viewer.openInventory(target.getInventory())} call (which opened the
 * target's actual {@link PlayerInventory}, locking the title to the vanilla
 * "Inventory" string and exposing the bottom hotbar with no visual divider).
 *
 * <p>Layout (54 slots, 6 rows):
 * <pre>
 *   Row 0 (slots  0..8 ) ← target slots  9..17  (main inventory, top row)
 *   Row 1 (slots  9..17) ← target slots 18..26  (main inventory, middle)
 *   Row 2 (slots 18..26) ← target slots 27..35  (main inventory, bottom)
 *   Row 3 (slots 27..35) — SEPARATOR (filler glass; no mapping)
 *   Row 4 (slots 36..44) ← target slots  0..8   (hotbar)
 *   Row 5 (slots 45..53):
 *           45 ← target 39 (helmet)
 *           46 ← target 38 (chestplate)
 *           47 ← target 37 (leggings)
 *           48 ← target 36 (boots)
 *           49 ← target 40 (offhand, 1.9+)
 *           50..52  filler
 *           53      red-X close head
 * </pre>
 *
 * <p>"Live status" is achieved by registering each open menu with
 * {@link InvSeeMenuManager}; the manager's repeating refresh task (4 Hz)
 * copies any slot whose contents diverge from the target's current state
 * back into the viewer's chest. The viewer cannot edit through the GUI —
 * {@link dev.zcripted.obx.feature.staff.gui.InvSeeMenuListener} cancels
 * every click so this is a strictly view-only mirror. (Editing through the
 * mirror would require deciding when to push the viewer's drag back into the
 * target vs. when the refresher should overwrite it; out of scope for this
 * drop.)
 */
public final class InvSeeMenu {

    public static final int INVENTORY_SIZE = 54;
    public static final int CLOSE_SLOT = 53;
    /** Inclusive-start, exclusive-end slot range for the mid-GUI separator row. */
    public static final int SEPARATOR_START = 27;
    public static final int SEPARATOR_END_EXCLUSIVE = 36;

    private InvSeeMenu() {
    }

    public static void open(OBX plugin, Player viewer, Player target) {
        LanguageManager languages = plugin.getLanguageManager();
        int[] slotMap = buildSlotMap();
        InvSeeMenuHolder holder = new InvSeeMenuHolder(target.getUniqueId(), target.getName(), slotMap, CLOSE_SLOT);

        String title = languages.get(viewer, "player.invsee.menu.title",
                Placeholders.with("player", target.getName()));
        if (title == null || title.isEmpty()) {
            title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + target.getName() + ChatColor.DARK_PURPLE + "'s Inventory";
        }
        title = ChatColor.translateAlternateColorCodes('&', title);
        // Bukkit pre-1.13 caps inventory titles at 32 chars; trim defensively
        // so a long username doesn't blow up createInventory on legacy
        // servers. Modern (1.13+) servers handle the longer titles fine.
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inv);

        // Separator row — explicit filler glass so it reads as a divider
        // rather than as "more empty inventory". The same filler is also
        // dropped into the bottom-row gaps for visual consistency.
        ItemStack filler = WarpMenuStyling.createFiller();
        for (int i = SEPARATOR_START; i < SEPARATOR_END_EXCLUSIVE; i++) {
            inv.setItem(i, filler.clone());
        }
        for (int i = 50; i < CLOSE_SLOT; i++) {
            inv.setItem(i, filler.clone());
        }

        // Initial snapshot from target inventory.
        refreshFromTarget(inv, target, slotMap);

        // Reuse the staff-menu close head so the close button matches the
        // visual language operators are already used to.
        inv.setItem(CLOSE_SLOT, StaffMenu.buildCloseHead(viewer, languages));

        if (plugin.getInvSeeMenuManager() != null) {
            plugin.getInvSeeMenuManager().register(viewer, holder);
        }
        viewer.openInventory(inv);
    }

    /**
     * Copies every mapped slot from {@code target}'s player inventory into the
     * mirror {@code inv}, but only writes when the contents actually differ —
     * avoiding the visual flicker / cursor-snap that calling
     * {@link Inventory#setItem(int, ItemStack)} on every frame would produce.
     */
    public static void refreshFromTarget(Inventory inv, Player target, int[] slotMap) {
        if (inv == null || target == null || slotMap == null) {
            return;
        }
        PlayerInventory targetInv = target.getInventory();
        if (targetInv == null) {
            return;
        }
        for (int guiSlot = 0; guiSlot < slotMap.length; guiSlot++) {
            int targetSlot = slotMap[guiSlot];
            if (targetSlot < 0) {
                continue;
            }
            ItemStack currentTarget = safeGet(targetInv, targetSlot);
            ItemStack currentGui = inv.getItem(guiSlot);
            if (!isSameItem(currentGui, currentTarget)) {
                inv.setItem(guiSlot, currentTarget == null ? null : currentTarget.clone());
            }
        }
    }

    private static ItemStack safeGet(PlayerInventory targetInv, int slot) {
        // PlayerInventory#getSize varies per platform / version
        // (1.8 = 40, 1.9+ = 41, some forks return only the 36 storage
        // slots from getSize()). A try/Throwable wrap is the cheapest
        // way to stay version-portable without re-checking on every call.
        try {
            if (slot < 0 || slot >= targetInv.getSize()) {
                return null;
            }
            return targetInv.getItem(slot);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isSameItem(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        // ItemStack#isSimilar compares type + meta but not amount;
        // amount is checked separately so quantity changes still trigger
        // a re-paint.
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    private static int[] buildSlotMap() {
        int[] map = new int[INVENTORY_SIZE];
        Arrays.fill(map, InvSeeMenuHolder.UNMAPPED);
        // Main inventory: GUI 0..26 ↔ target 9..35
        for (int i = 0; i < 27; i++) {
            map[i] = 9 + i;
        }
        // Separator row 27..35 stays UNMAPPED.
        // Hotbar: GUI 36..44 ↔ target 0..8
        for (int i = 0; i < 9; i++) {
            map[36 + i] = i;
        }
        // Armor + offhand on the bottom row.
        map[45] = 39; // helmet
        map[46] = 38; // chestplate
        map[47] = 37; // leggings
        map[48] = 36; // boots
        map[49] = 40; // offhand (silently absent on 1.8 — safeGet handles)
        // 50..52 stay UNMAPPED (filler), 53 stays UNMAPPED (close button).
        return map;
    }
}
