package dev.zcripted.obx.feature.warp.command;

import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mutating / admin warp subcommands extracted from {@link WarpCommand}:
 * {@code set}, {@code delete}, {@code rename}, {@code move}, {@code icon},
 * and {@code public}. Bodies are moved verbatim.
 */
final class WarpAdminCommands {

    private static final Set<String> CONFIRM_WORDS = new HashSet<>(Arrays.asList("confirm", "yes", "y"));
    private static final long CONFIRM_WINDOW_MS = 15_000L;

    private final WarpService warpService;
    private final LanguageManager languages;
    private final Map<UUID, PendingAction> deleteConfirmations = new HashMap<>();
    private final Map<UUID, PendingAction> overwriteConfirmations = new HashMap<>();

    WarpAdminCommands(WarpService warpService, LanguageManager languages) {
        this.warpService = warpService;
        this.languages = languages;
    }

    boolean handleSet(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("obx.warp.set") && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.set");
            return true;
        }
        Player player = (Player) sender;
        String nameInput = args.get(0);
        String normalized = warpService.normalizeName(nameInput);
        if (normalized == null) {
            languages.send(sender, "teleport.warp.invalid-name");
            return true;
        }
        WarpService.WarpEntry existing = warpService.getWarp(nameInput);
        boolean confirm = args.size() > 1 && CONFIRM_WORDS.contains(args.get(1).toLowerCase(Locale.ENGLISH));
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (existing != null && !confirm) {
            overwriteConfirmations.put(uuid, new PendingAction(normalized, now));
            languages.send(sender, "teleport.warp.set.confirm", Placeholders.with("warp", existing.getName()));
            return true;
        }
        if (existing != null) {
            PendingAction action = overwriteConfirmations.get(uuid);
            if (action == null || !action.warpKey.equalsIgnoreCase(normalized) || now - action.requestedAt > CONFIRM_WINDOW_MS) {
                overwriteConfirmations.put(uuid, new PendingAction(normalized, now));
                languages.send(sender, "teleport.warp.set.confirm", Placeholders.with("warp", existing.getName()));
                return true;
            }
        }
        warpService.setWarp(nameInput, player.getLocation(), "general", null, true, null, player.getUniqueId(), player.getName());
        languages.send(sender, existing == null ? "teleport.warp.set.created" : "teleport.warp.set.updated", Placeholders.with("warp", nameInput));
        return true;
    }

    boolean handleDelete(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.delete") && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.delete");
            return true;
        }
        String warpName = args.get(0);
        WarpService.WarpEntry entry = warpService.getWarp(warpName);
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", warpName));
            return true;
        }
        UUID key = sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID();
        long now = System.currentTimeMillis();
        boolean confirm = args.size() > 1 && CONFIRM_WORDS.contains(args.get(1).toLowerCase(Locale.ENGLISH));
        if (!confirm) {
            deleteConfirmations.put(key, new PendingAction(entry.getKey(), now));
            languages.send(sender, "teleport.warp.delete.confirm", Placeholders.with("warp", entry.getName()));
            return true;
        }
        PendingAction action = deleteConfirmations.get(key);
        if (action == null || !action.warpKey.equalsIgnoreCase(entry.getKey()) || now - action.requestedAt > CONFIRM_WINDOW_MS) {
            deleteConfirmations.put(key, new PendingAction(entry.getKey(), now));
            languages.send(sender, "teleport.warp.delete.confirm-needed", Placeholders.with("warp", entry.getName()));
            return true;
        }
        warpService.deleteWarp(entry.getKey());
        deleteConfirmations.remove(key);
        languages.send(sender, "teleport.warp.deleted", Placeholders.with("warp", entry.getName()));
        return true;
    }

    boolean handleRename(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.rename") && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.size() < 2) {
            languages.send(sender, "teleport.warp.usage.rename");
            return true;
        }
        String oldName = args.get(0);
        String newName = args.get(1);
        WarpService.WarpEntry existing = warpService.getWarp(oldName);
        if (existing == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", oldName));
            return true;
        }
        if (warpService.getWarp(newName) != null) {
            languages.send(sender, "teleport.warp.rename.conflict", Placeholders.with("warp", newName));
            return true;
        }
        String normalized = warpService.normalizeName(newName);
        if (normalized == null) {
            languages.send(sender, "teleport.warp.invalid-name");
            return true;
        }
        warpService.renameWarp(oldName, newName);
        Map<String, String> placeholders = Placeholders.with("old", existing.getName(), "new", newName);
        languages.send(sender, "teleport.warp.rename.success", placeholders);
        return true;
    }

    boolean handleMove(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("obx.warp.move") && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.move");
            return true;
        }
        String warpName = args.get(0);
        WarpService.WarpEntry entry = warpService.getWarp(warpName);
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", warpName));
            return true;
        }
        Player player = (Player) sender;
        warpService.moveWarp(warpName, player.getLocation(), player.getUniqueId(), player.getName());
        languages.send(sender, "teleport.warp.move.success", Placeholders.with("warp", entry.getName()));
        return true;
    }

    boolean handleIcon(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.manage") && !sender.hasPermission("obx.warp.icon")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.icon");
            return true;
        }
        WarpService.WarpEntry entry = warpService.getWarp(args.get(0));
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", args.get(0)));
            return true;
        }
        if (args.size() == 1) {
            warpService.setIcon(entry.getKey(), null);
            languages.send(sender, "teleport.warp.icon.cleared", Placeholders.with("warp", entry.getName()));
            return true;
        }
        String icon = args.get(1);
        if (org.bukkit.Material.matchMaterial(icon) == null) {
            languages.send(sender, "teleport.warp.icon.invalid", Placeholders.with("icon", icon));
            return true;
        }
        warpService.setIcon(entry.getKey(), icon);
        languages.send(sender, "teleport.warp.icon.updated", Placeholders.with("warp", entry.getName(), "icon", icon.toUpperCase(Locale.ENGLISH)));
        return true;
    }

    boolean handlePublic(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.manage") && !sender.hasPermission("obx.warp.public")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.public");
            return true;
        }
        WarpService.WarpEntry entry = warpService.getWarp(args.get(0));
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", args.get(0)));
            return true;
        }
        boolean newState = !entry.isPublic();
        if (args.size() > 1) {
            String raw = args.get(1).toLowerCase(Locale.ENGLISH);
            if (raw.equals("true") || raw.equals("yes") || raw.equals("on")) {
                newState = true;
            } else if (raw.equals("false") || raw.equals("no") || raw.equals("off")) {
                newState = false;
            } else {
                languages.send(sender, "teleport.warp.usage.public");
                return true;
            }
        }
        warpService.setPublic(entry.getKey(), newState);
        languages.send(sender, "teleport.warp.public.updated", Placeholders.with("warp", entry.getName(), "state", newState ? languages.get(sender, "teleport.warp.visibility.public") : languages.get(sender, "teleport.warp.visibility.hidden")));
        return true;
    }

    private static final class PendingAction {
        private final String warpKey;
        private final long requestedAt;

        private PendingAction(String warpKey, long requestedAt) {
            this.warpKey = warpKey;
            this.requestedAt = requestedAt;
        }
    }
}