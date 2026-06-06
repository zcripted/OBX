package dev.zcripted.obx.feature.hologram.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import dev.zcripted.obx.feature.hologram.model.HologramSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Chest-GUI overview / editor for a hologram. Implements the plan §I main
 * page only — line preview slots, a settings hint, an animations hint, and
 * a delete confirmation slot. Sub-menus (animations, interaction area) are
 * follow-ups when their command-line equivalents prove insufficient — for
 * now the GUI surfaces every editable hologram alongside the existing
 * {@code /holo ...} commands so operators have both UX paths.
 *
 * <p>Hover tooltips follow {@code CLAUDE.md} guidance: detailed, cleanly
 * formatted, with explicit dividers, action lines, and section spacing.
 */
public final class HologramEditorMenu implements Listener {

    private static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Hologram " + ChatColor.GRAY + "› ";

    private final ObxPlugin plugin;

    public HologramEditorMenu(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Marker holder so click/drag handlers identify this menu by inventory ownership rather
     * than its (spoofable) title string, and so the hologram id rides on the inventory itself
     * (no per-player tracking map to leak on disconnect).
     */
    private static final class EditorHolder implements InventoryHolder {
        private final String idValue;
        private Inventory inventory;

        private EditorHolder(String idValue) {
            this.idValue = idValue;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player, Hologram hologram) {
        EditorHolder holder = new EditorHolder(hologram.getId().value());
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE_PREFIX + hologram.getId().value());
        holder.inventory = inv;
        // Header: identity / location.
        inv.setItem(4, identityItem(hologram));
        // Body: line previews
        List<HologramLine> lines = hologram.getLines();
        for (int i = 0; i < Math.min(lines.size(), 9); i++) {
            inv.setItem(9 + i, lineItem(i + 1, lines.get(i)));
        }
        // Settings + animations + delete tiles
        inv.setItem(18, settingsItem(hologram));
        inv.setItem(20, animationsItem(hologram));
        inv.setItem(22, interactionItem(hologram));
        inv.setItem(26, deleteItem(hologram));
        player.openInventory(inv);
    }

    private ItemStack identityItem(Hologram hologram) {
        ItemStack stack = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + hologram.getId().value());
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE
                    + (hologram.getLocation().getWorld() == null ? "?" : hologram.getLocation().getWorld().getName()));
            lore.add(String.format(ChatColor.GRAY + "Position: " + ChatColor.WHITE + "%.2f, %.2f, %.2f",
                    hologram.getLocation().getX(), hologram.getLocation().getY(), hologram.getLocation().getZ()));
            lore.add(ChatColor.GRAY + "Lines: " + ChatColor.WHITE + hologram.getLines().size());
            lore.add(ChatColor.GRAY + "Animations: " + ChatColor.WHITE + hologram.getAnimationConfigs().size());
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Read-only summary");
            lore.add(divider());
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack lineItem(int oneBasedIndex, HologramLine line) {
        Material material = Material.PAPER;
        String preview;
        switch (line.getType()) {
            case TEXT:
                material = Material.PAPER;
                preview = ((HologramLine.TextLine) line).getTemplate();
                break;
            case ICON:
                ItemStack stack = ((HologramLine.IconLine) line).getStack();
                material = stack == null ? Material.STONE : stack.getType();
                preview = material.name();
                break;
            case BLOCK:
                material = ((HologramLine.BlockLine) line).getMaterial();
                preview = material.name();
                break;
            default:
                preview = "?";
                break;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Line " + oneBasedIndex + " " + ChatColor.GRAY + "(" + line.getType().name() + ")");
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', preview));
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Edit: " + ChatColor.GRAY + "/holo line set <id> " + oneBasedIndex + " <value>");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Remove: " + ChatColor.GRAY + "/holo line remove <id> " + oneBasedIndex);
            lore.add(divider());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack settingsItem(Hologram hologram) {
        ItemStack item = new ItemStack(matchOr("COMPARATOR", "REDSTONE_COMPARATOR", "REPEATER"));
        ItemMeta meta = item.getItemMeta();
        HologramSettings s = hologram.getSettings();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Settings");
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.GRAY + "Billboard: " + ChatColor.WHITE + s.getBillboard().name());
            lore.add(ChatColor.GRAY + "Scale: " + ChatColor.WHITE + String.format("%.2f", s.getScale()));
            lore.add(ChatColor.GRAY + "Show range: " + ChatColor.WHITE + ((int) s.getShowRange()));
            lore.add(ChatColor.GRAY + "Double-sided: " + ChatColor.WHITE + s.isDoubleSided());
            lore.add(ChatColor.GRAY + "See-through: " + ChatColor.WHITE + s.isSeeThrough());
            lore.add(ChatColor.GRAY + "Shadow: " + ChatColor.WHITE + s.hasShadow());
            lore.add(ChatColor.GRAY + "Alignment: " + ChatColor.WHITE + s.getTextAlignment().name());
            lore.add(ChatColor.GRAY + "Text opacity: " + ChatColor.WHITE + s.getTextOpacity());
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Edit with: " + ChatColor.GRAY + "/holo <setting> <id> <value>");
            lore.add(divider());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack animationsItem(Hologram hologram) {
        ItemStack item = new ItemStack(matchOr("CLOCK", "WATCH"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Animations");
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.GRAY + "Count: " + ChatColor.WHITE + hologram.getAnimationConfigs().size());
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Add: " + ChatColor.GRAY + "/holo anim " + hologram.getId().value() + " add <type>");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "List: " + ChatColor.GRAY + "/holo anim " + hologram.getId().value() + " list");
            lore.add(divider());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack interactionItem(Hologram hologram) {
        ItemStack item = new ItemStack(Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        HologramSettings s = hologram.getSettings();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Interaction");
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.GRAY + "Enabled: " + ChatColor.WHITE + s.isInteractionEnabled());
            lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + s.getInteractionCooldownMs() + " ms");
            lore.add(ChatColor.GRAY + "Box: " + ChatColor.WHITE
                    + s.getInteractionWidth() + " × " + s.getInteractionHeight());
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Toggle: " + ChatColor.GRAY + "/holo interact " + hologram.getId().value() + " enable|disable");
            lore.add(divider());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack deleteItem(Hologram hologram) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Delete");
            List<String> lore = new ArrayList<>();
            lore.add(divider());
            lore.add(ChatColor.GRAY + "Permanently remove this hologram");
            lore.add(ChatColor.GRAY + "from disk and the world.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Shift-click to confirm");
            lore.add(divider());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String divider() {
        return ChatColor.DARK_GRAY + "──────────────────────────";
    }

    private static Material matchOr(String... names) {
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.STONE;
    }

    private static EditorHolder holderOf(Inventory top) {
        if (top == null) {
            return null;
        }
        InventoryHolder holder = top.getHolder();
        return holder instanceof EditorHolder ? (EditorHolder) holder : null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        EditorHolder holder = holderOf(event.getView().getTopInventory());
        if (holder == null) {
            return;
        }
        // Cancel every interaction with this read-only menu (covers shift-click, number-key,
        // and collect-to-cursor, which can otherwise pull GUI items into the player's inventory).
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (event.getSlot() == 26 && event.isShiftClick() && holder.idValue != null) {
            player.performCommand("holo delete " + holder.idValue);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        // A drag can deposit items into the menu's empty slots, which are then lost on close.
        if (holderOf(event.getView().getTopInventory()) == null) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
