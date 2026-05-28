package dev.sergeantfuzzy.sfcore.listener.menu;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.admin.AdminMenuHolder;
import dev.sergeantfuzzy.sfcore.gui.admin.AdminMenu;
import dev.sergeantfuzzy.sfcore.gui.admin.AdminSubMenu;
import dev.sergeantfuzzy.sfcore.gui.player.MainMenu;
import dev.sergeantfuzzy.sfcore.gui.player.MainMenuHolder;
import dev.sergeantfuzzy.sfcore.gui.player.WarpMenu;
import dev.sergeantfuzzy.sfcore.gui.player.WarpMenuHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Prevents players from moving items in the SF-Core main menu and handles click actions.
 */
public final class MainMenuListener implements Listener {

    private final Main plugin;

    public MainMenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isTrackedMenu(topInventory)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }
        if (topInventory.getHolder() instanceof MainMenuHolder) {
            if (event.getRawSlot() == MainMenu.WARP_ITEM_SLOT) {
                WarpMenu.openMain(plugin, (Player) clicker, 0, null, null, false, WarpMenuHolder.BackTarget.MAIN_MENU);
            } else if (event.getRawSlot() == MainMenu.CORE_ITEM_SLOT) {
                plugin.getLanguageManager().send((Player) clicker, "menus.main.thanks");
            }
            return;
        }
        if (topInventory.getHolder() instanceof AdminMenuHolder) {
            AdminMenu.PlaceholderView view = AdminMenu.placeholderForSlot(event.getRawSlot());
            if (view != null) {
                AdminSubMenu.open(plugin, (Player) clicker, view);
                return;
            }
            if (event.getRawSlot() == 37) {
                Player admin = (Player) clicker;
                if (canManageWarps(admin)) {
                    WarpMenu.openManage(plugin, admin, 0, null, null, WarpMenuHolder.BackTarget.ADMIN_MENU);
                } else {
                    plugin.getLanguageManager().send(admin, "core.no-permission");
                }
                return;
            }
            if (event.getRawSlot() == AdminMenu.CLOSE_SLOT) {
                ((Player) clicker).closeInventory();
            }
            return;
        }
        if (topInventory.getHolder() instanceof AdminSubMenu.Holder) {
            AdminSubMenu.Holder holder = (AdminSubMenu.Holder) topInventory.getHolder();
            int slot = event.getRawSlot();
            switch (holder.getType()) {
                case SERVER_CONTROL:
                    if (slot == 10) {
                        AdminSubMenu.openServerStateMenu((Player) clicker);
                    } else if (slot == 12) {
                        AdminSubMenu.openPlayerAccessMenu((Player) clicker);
                    } else if (slot == 14) {
                        AdminSubMenu.openPerformanceMenu((Player) clicker);
                    } else if (slot == 16) {
                        AdminSubMenu.openWorldControlsMenu((Player) clicker);
                    } else if (slot == 19) {
                        AdminSubMenu.openPluginSystemsMenu((Player) clicker);
                    } else if (slot == AdminSubMenu.BACK_SLOT || slot == 31) {
                        AdminMenu.open(plugin, (Player) clicker);
                    } else if (slot == AdminMenu.CLOSE_SLOT) {
                        ((Player) clicker).closeInventory();
                    }
                    break;
            case JAIL_CENTER:
                if (slot == AdminSubMenu.BACK_SLOT) {
                    AdminMenu.open(plugin, (Player) clicker);
                } else if (slot == AdminMenu.CLOSE_SLOT) {
                    ((Player) clicker).closeInventory();
                } else if (slot == 10) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).performCommand("jails");
                } else if (slot == 12) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).sendMessage("§eType: §f/setjail <name>§e to anchor at your current location.");
                } else if (slot == 14) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).sendMessage("§eType: §f/deljail <name>§e to remove a jail anchor.");
                } else if (slot == 16) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).sendMessage("§eType: §f/jailtime <player>§e to check remaining time.");
                }
                break;
            case MOB_TOOLS:
                if (slot == AdminSubMenu.BACK_SLOT) {
                    AdminMenu.open(plugin, (Player) clicker);
                } else if (slot == AdminMenu.CLOSE_SLOT) {
                    ((Player) clicker).closeInventory();
                } else if (slot == 10) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).performCommand("butcher 32");
                } else if (slot == 12) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).sendMessage("§eType: §f/spawnmob <type> [count]§e to spawn mobs.");
                } else if (slot == 14) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).performCommand("smite");
                } else if (slot == 16) {
                    ((Player) clicker).closeInventory();
                    ((Player) clicker).performCommand("tree");
                }
                break;
            case SERVER_STATE:
            case PLAYER_ACCESS:
            case PERFORMANCE_HEALTH:
            case WORLD_CONTROLS:
            case PLUGIN_SYSTEMS:
            case WEATHER:
            case TIME:
            case GAMERULES:
                if (slot == 31) {
                    if (holder.getType() == AdminSubMenu.SubMenuType.WEATHER
                            || holder.getType() == AdminSubMenu.SubMenuType.TIME
                            || holder.getType() == AdminSubMenu.SubMenuType.GAMERULES) {
                        AdminSubMenu.openWorldControlsMenu((Player) clicker);
                    } else {
                        AdminMenu.PlaceholderView serverPlaceholder = AdminMenu.placeholderForSlot(19); // server control slot index
                        if (serverPlaceholder != null) {
                            AdminSubMenu.open(plugin, (Player) clicker, serverPlaceholder);
                        } else {
                            AdminMenu.open(plugin, (Player) clicker);
                        }
                    }
                } else if (slot == AdminMenu.CLOSE_SLOT) {
                    ((Player) clicker).closeInventory();
                } else {
                    AdminSubMenu.handleAction(plugin, (Player) clicker, holder, slot, event.getClick());
                }
                break;
                default:
                    if (slot == AdminSubMenu.BACK_SLOT || slot == 31) {
                        AdminMenu.open(plugin, (Player) clicker);
                    } else if (slot == AdminMenu.CLOSE_SLOT) {
                        ((Player) clicker).closeInventory();
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isTrackedMenu(topInventory)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }

    private boolean canManageWarps(Player player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermission("sfcore.warp.manage")) {
            return true;
        }
        String[] perms = new String[]{"sfcore.warp.set", "sfcore.warp.delete", "sfcore.warp.rename", "sfcore.warp.move", "sfcore.warp.icon", "sfcore.warp.public"};
        for (String perm : perms) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTrackedMenu(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof MainMenuHolder || holder instanceof AdminMenuHolder || holder instanceof AdminSubMenu.Holder;
    }
}

