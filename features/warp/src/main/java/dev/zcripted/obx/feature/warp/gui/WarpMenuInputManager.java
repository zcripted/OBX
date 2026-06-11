package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.staff.gui.AdminMenu;
import dev.zcripted.obx.core.gui.main.MainMenu;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarpMenuInputManager {

    public enum InputType {
        CREATE,
        RENAME,
        SEARCH
    }

    public static final class PendingInput {
        private final InputType type;
        private final String warpKey;
        private final String warpName;
        private final WarpMenuHolder.BackTarget backTarget;
        private final int returnPage;
        private final String categoryFilter;
        private final String searchTerm;
        private final boolean adminMode;
        private final WarpMenuHolder.AdminAction adminAction;

        public PendingInput(InputType type, String warpKey, String warpName, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.AdminAction adminAction) {
            this.type = type;
            this.warpKey = warpKey;
            this.warpName = warpName;
            this.backTarget = backTarget;
            this.returnPage = returnPage;
            this.categoryFilter = categoryFilter;
            this.searchTerm = searchTerm;
            this.adminMode = adminMode;
            this.adminAction = adminAction == null ? WarpMenuHolder.AdminAction.NONE : adminAction;
        }

        public InputType getType() {
            return type;
        }

        public String getWarpKey() {
            return warpKey;
        }

        public String getWarpName() {
            return warpName;
        }

        public WarpMenuHolder.BackTarget getBackTarget() {
            return backTarget;
        }

        public int getReturnPage() {
            return returnPage;
        }

        public String getCategoryFilter() {
            return categoryFilter;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public boolean isAdminMode() {
            return adminMode;
        }

        public WarpMenuHolder.AdminAction getAdminAction() {
            return adminAction;
        }
    }

    private final ObxPlugin plugin;
    private final Map<UUID, PendingInput> inputs = new ConcurrentHashMap<>();

    public WarpMenuInputManager(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void promptCreate(Player player, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        inputs.put(player.getUniqueId(), new PendingInput(InputType.CREATE, null, null, backTarget, returnPage, categoryFilter, searchTerm, true, WarpMenuHolder.AdminAction.NONE));
        plugin.getLanguageManager().send(player, "teleport.warp.gui.prompt.create");
    }

    public void promptRename(Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, WarpMenuHolder.AdminAction adminAction) {
        inputs.put(player.getUniqueId(), new PendingInput(InputType.RENAME, entry.getKey(), entry.getName(), backTarget, returnPage, categoryFilter, searchTerm, true, adminAction));
        plugin.getLanguageManager().send(player, "teleport.warp.gui.prompt.rename", Placeholders.with("warp", entry.getName()));
    }

    public void promptSearch(Player player, boolean adminMode, WarpMenuHolder.BackTarget backTarget, String categoryFilter) {
        inputs.put(player.getUniqueId(), new PendingInput(InputType.SEARCH, null, null, backTarget, 0, categoryFilter, null, adminMode, WarpMenuHolder.AdminAction.NONE));
        plugin.getLanguageManager().send(player, "teleport.warp.gui.prompt.search");
    }

    public PendingInput get(UUID uuid) {
        return inputs.get(uuid);
    }

    public void clear(UUID uuid) {
        inputs.remove(uuid);
    }

    public void handleInput(Player player, PendingInput input, String message) {
        if (player == null || input == null) {
            return;
        }
        plugin.getSchedulerAdapter().runAtEntity(player, () -> processInput(player, input, message.trim()));
    }

    private void processInput(Player player, PendingInput input, String raw) {
        UUID uuid = player.getUniqueId();
        String message = raw.trim();
        if (message.equalsIgnoreCase("cancel")) {
            plugin.getLanguageManager().send(player, "teleport.warp.gui.prompt.cancelled");
            reopen(player, input);
            inputs.remove(uuid);
            return;
        }

        switch (input.getType()) {
            case CREATE:
                handleCreate(player, input, message);
                break;
            case RENAME:
                handleRename(player, input, message);
                break;
            case SEARCH:
                handleSearch(player, input, message);
                break;
            default:
                break;
        }
    }

    private void handleCreate(Player player, PendingInput input, String message) {
        WarpService warpService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class);
        String normalized = warpService.normalizeName(message);
        if (normalized == null) {
            plugin.getLanguageManager().send(player, "teleport.warp.invalid-name");
            return;
        }
        Location location = player.getLocation();
        WarpService.WarpEntry existing = warpService.getWarp(message);
        if (existing != null) {
            WarpMenu.openConfirmOverwrite(plugin, player, message, location, existing, input.getBackTarget(), input.getReturnPage(), input.getCategoryFilter(), input.getSearchTerm());
            inputs.remove(player.getUniqueId());
            return;
        }
        warpService.setWarp(message, location, "general", null, true, null, player.getUniqueId(), player.getName());
        plugin.getLanguageManager().send(player, "teleport.warp.set.created", Placeholders.with("warp", message));
        inputs.remove(player.getUniqueId());
        reopen(player, input);
    }

    private void handleRename(Player player, PendingInput input, String message) {
        WarpService warpService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class);
        WarpService.WarpEntry entry = warpService.getWarp(input.getWarpKey() != null ? input.getWarpKey() : input.getWarpName());
        if (entry == null) {
            plugin.getLanguageManager().send(player, "teleport.warp.not-found", Placeholders.with("warp", input.getWarpName() == null ? "?" : input.getWarpName()));
            inputs.remove(player.getUniqueId());
            reopen(player, input);
            return;
        }
        if (warpService.getWarp(message) != null) {
            plugin.getLanguageManager().send(player, "teleport.warp.rename.conflict", Placeholders.with("warp", message));
            return;
        }
        String normalized = warpService.normalizeName(message);
        if (normalized == null) {
            plugin.getLanguageManager().send(player, "teleport.warp.invalid-name");
            return;
        }
        warpService.renameWarp(entry.getName(), message);
        plugin.getLanguageManager().send(player, "teleport.warp.rename.success", Placeholders.with("old", entry.getName(), "new", message));
        inputs.remove(player.getUniqueId());
        reopen(player, input);
    }

    private void handleSearch(Player player, PendingInput input, String message) {
        String term = message.equalsIgnoreCase("clear") ? null : message;
        inputs.remove(player.getUniqueId());
        WarpMenu.openMain(plugin, player, 0, input.getCategoryFilter(), term, input.isAdminMode(), input.getBackTarget());
    }

    private void reopen(Player player, PendingInput input) {
        switch (input.getBackTarget()) {
            case ADMIN_MENU:
                AdminMenu.open(plugin, player);
                return;
            case WARP_MANAGE:
                WarpMenu.openManage(plugin, player, input.getReturnPage(), input.getCategoryFilter(), input.getSearchTerm(), input.getBackTarget());
                return;
            case MAIN_MENU:
                MainMenu.open(plugin, player);
                return;
            case WARP_MAIN:
                WarpMenu.openMain(plugin, player, input.getReturnPage(), input.getCategoryFilter(), input.getSearchTerm(), input.isAdminMode(), input.getBackTarget());
                return;
            default:
                WarpMenu.openMain(plugin, player, input.getReturnPage(), input.getCategoryFilter(), input.getSearchTerm(), input.isAdminMode(), input.getBackTarget());
        }
    }
}