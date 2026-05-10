package dev.sergeantfuzzy.sfcore.listener.menu;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.player.HelpGuiHolder;
import dev.sergeantfuzzy.sfcore.gui.player.HelpGuiMenu;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HelpGuiListener implements Listener {

    private static final Set<String> OVERRIDDEN_COMMANDS = new HashSet<>(Arrays.asList(
            "help", "?", "bukkit:help", "bukkit:?", "minecraft:help", "minecraft:?"
    ));

    private final Main plugin;

    public HelpGuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }
        int spaceIdx = message.indexOf(' ');
        String label = (spaceIdx == -1 ? message.substring(1) : message.substring(1, spaceIdx))
                .toLowerCase(Locale.ENGLISH);
        if (!OVERRIDDEN_COMMANDS.contains(label)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("sfcore.help.gui")) {
            return;
        }
        event.setCancelled(true);
        int page = 1;
        String category = HelpGuiMenu.CATEGORY_ALL;
        if (spaceIdx > 0 && spaceIdx + 1 < message.length()) {
            String tail = message.substring(spaceIdx + 1).trim();
            if (!tail.isEmpty()) {
                String[] parts = tail.split("\\s+");
                for (String part : parts) {
                    if (part.isEmpty()) {
                        continue;
                    }
                    try {
                        page = Integer.parseInt(part);
                    } catch (NumberFormatException ignored) {
                        category = HelpGuiMenu.normalizeCategory(part);
                    }
                }
            }
        }
        HelpGuiMenu.open(plugin, player, page, category);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof HelpGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;
        HelpGuiHolder holder = (HelpGuiHolder) top.getHolder();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= top.getSize()) {
            return;
        }

        if (slot == HelpGuiMenu.PREV_SLOT && holder.getPage() > 1) {
            HelpGuiMenu.open(plugin, player, holder.getPage() - 1, holder.getCategory());
            return;
        }
        if (slot == HelpGuiMenu.NEXT_SLOT && holder.getPage() < holder.getTotalPages()) {
            HelpGuiMenu.open(plugin, player, holder.getPage() + 1, holder.getCategory());
            return;
        }
        if (slot == HelpGuiMenu.CATEGORY_SLOT) {
            ClickType click = event.getClick();
            String nextCategory;
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                nextCategory = HelpGuiMenu.previousCategory(holder.getCategory());
            } else if (click == ClickType.MIDDLE || click == ClickType.DROP) {
                nextCategory = HelpGuiMenu.CATEGORY_ALL;
            } else {
                nextCategory = HelpGuiMenu.nextCategory(holder.getCategory());
            }
            HelpGuiMenu.open(plugin, player, 1, nextCategory);
            return;
        }
        if (slot == HelpGuiMenu.INFO_SLOT) {
            return;
        }
        if (slot == HelpGuiMenu.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot < HelpGuiMenu.PAGE_SIZE) {
            String command = HelpGuiMenu.commandAt(top, slot);
            if (command == null || command.isEmpty()) {
                return;
            }
            player.closeInventory();
            player.chat("/" + command);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof HelpGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }
}
