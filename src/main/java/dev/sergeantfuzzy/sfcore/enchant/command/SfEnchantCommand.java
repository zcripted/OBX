package dev.sergeantfuzzy.sfcore.enchant.command;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.item.EnchantGuideBook;
import dev.sergeantfuzzy.sfcore.enchant.item.EnchantItems;
import dev.sergeantfuzzy.sfcore.enchant.item.ScrollKind;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.model.EnchantCategory;
import dev.sergeantfuzzy.sfcore.enchant.model.ItemTag;
import dev.sergeantfuzzy.sfcore.enchant.service.ApplyResult;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantFeedback;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import dev.sergeantfuzzy.sfcore.enchant.util.EnchantHover;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /sfench} (aliases {@code /sfenchant}, {@code /sfe}) — the Arcanum admin
 * command. Handles giving scrolls/books, direct apply/remove, listing and info,
 * reloads, loot toggles, and proc-debug. The {@code admin} GUI is opened by
 * {@link dev.sergeantfuzzy.sfcore.enchant.gui.EnchantAdminMenu} once that phase
 * is wired in.
 */
public final class SfEnchantCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final EnchantFeedback feedback;
    private final LanguageManager languages;

    public SfEnchantCommand(Main plugin) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.feedback = plugin.getEnchantFeedback();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!service.isEnabled()) {
            languages.send(sender, "enchant.module-disabled");
            return true;
        }
        if (args.length == 0) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (sub.equals("admin")) {
            if (!requirePermission(sender, "sfcore.enchants.admin")) {
                return true;
            }
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            plugin.getEnchantAdminMenu().open((Player) sender, false);
            return true;
        }
        if (sub.equals("apply")) {
            return handleApply(sender, args);
        }
        if (sub.equals("remove")) {
            return handleRemove(sender, args);
        }
        if (sub.equals("give")) {
            // /sfench give <player> book <category> → categorized codex guide book.
            if (args.length >= 3 && args[2].equalsIgnoreCase("book")) {
                return handleGiveGuideBook(sender, args);
            }
            return handleGive(sender, args, false);
        }
        if (sub.equals("givebook")) {
            return handleGive(sender, args, true);
        }
        if (sub.equals("bookinfo")) {
            return handleBookInfo(sender, args);
        }
        if (sub.equals("bookapply")) {
            return handleBookApply(sender, args);
        }
        if (sub.equals("protect")) {
            return handleUtility(sender, args, ScrollKind.PROTECTION);
        }
        if (sub.equals("success")) {
            return handleUtility(sender, args, ScrollKind.SUCCESS);
        }
        if (sub.equals("list")) {
            return handleList(sender, args);
        }
        if (sub.equals("info")) {
            return handleInfo(sender, args);
        }
        if (sub.equals("reload")) {
            if (!requirePermission(sender, "sfcore.enchants.reload")) {
                return true;
            }
            service.reload();
            languages.send(sender, "enchant.reload.done", one("count", Integer.toString(service.getRegistry().size())));
            return true;
        }
        if (sub.equals("loot")) {
            return handleLoot(sender, args);
        }
        if (sub.equals("debug")) {
            return handleDebug(sender, args);
        }
        languages.send(sender, "enchant.usage");
        return true;
    }

    // ── apply / remove ────────────────────────────────────────────────────────

    private boolean handleApply(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.apply")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (args.length < 3) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player player = (Player) sender;
        CustomEnchant enchant = resolveEnchant(sender, args[1]);
        if (enchant == null) {
            return true;
        }
        int level = parseLevel(sender, enchant, args[2]);
        if (level <= 0) {
            return true;
        }
        ItemStack inHand = mainHand(player);
        ApplyResult result = service.apply(inHand, enchant, level);
        feedback.send(player, result, inHand);
        if (result.isSuccess()) {
            setMainHand(player, inHand);
        }
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.apply")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player player = (Player) sender;
        CustomEnchant enchant = resolveEnchant(sender, args[1]);
        if (enchant == null) {
            return true;
        }
        ItemStack inHand = mainHand(player);
        Map<String, String> ph = one("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
        int level = service.getStorage().level(inHand, enchant.getId());
        if (level <= 0) {
            languages.send(player, "enchant.remove.not-present", ph);
            return true;
        }
        // Curse of the Bound cannot be removed.
        if (enchant.levelBoolean(level, "no_remove", false)) {
            languages.send(player, "enchant.remove.locked", ph);
            return true;
        }
        service.remove(inHand, enchant);
        setMainHand(player, inHand);
        EnchantHover.send(player, languages.get(player, "enchant.removed", ph), enchant, level, null);
        return true;
    }

    // ── give / givebook / utility ───────────────────────────────────────────

    private boolean handleGive(CommandSender sender, String[] args, boolean book) {
        if (!requirePermission(sender, "sfcore.enchants.give")) {
            return true;
        }
        if (args.length < 4) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            languages.send(sender, "enchant.target-offline", one("player", args[1]));
            return true;
        }
        CustomEnchant enchant = resolveEnchant(sender, args[2]);
        if (enchant == null) {
            return true;
        }
        int level = parseLevel(sender, enchant, args[3]);
        if (level <= 0) {
            return true;
        }
        int amount = args.length >= 5 ? Math.max(1, Math.min(64, parseIntOr(args[4], 1))) : 1;
        ItemStack item = book ? items.book(enchant, level, amount) : items.scroll(enchant, level, amount);
        giveItem(target, item);

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
        ph.put("roman", CustomEnchant.roman(level));
        ph.put("player", target.getName());
        ph.put("kind", languages.get(sender, book ? "enchant.kind.book" : "enchant.kind.scroll"));
        // Render the line, then attach the enchant tooltip to the enchant-name portion —
        // same hover treatment as the "Applied <enchant>" feedback and /sfench info/list.
        EnchantHover.send(target, languages.get(target, "enchant.give.received", ph), enchant, level, null);
        if (sender != target) {
            EnchantHover.send(sender, languages.get(sender, "enchant.give.sent", ph), enchant, level, null);
        }
        return true;
    }

    /**
     * {@code /sfench give <player> book <category>} — gives a stylized, interactive
     * "Codex" guide book for a category. The <em>recipient</em> must hold
     * {@code sfcore.enchants.book} (or be op); otherwise the executor is told (chat +
     * title) and no book is given.
     */
    private boolean handleGiveGuideBook(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.give")) {
            return true;
        }
        if (args.length < 4) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            languages.send(sender, "enchant.target-offline", one("player", args[1]));
            return true;
        }
        EnchantCategory category = EnchantCategory.fromId(args[3]);
        if (category == null) {
            languages.send(sender, "enchant.unknown-category", one("name", args[3]));
            return true;
        }
        // Recipient must have permission or op status to receive the book.
        if (!target.hasPermission("sfcore.enchants.book") && !target.isOp()) {
            Map<String, String> ph = one("player", target.getName());
            languages.send(sender, "enchant.book.recipient-no-permission", ph);
            sendTitle(sender,
                    languages.get(sender, "enchant.book.recipient-no-permission-title", ph),
                    languages.get(sender, "enchant.book.recipient-no-permission-subtitle", ph));
            return true;
        }
        giveItem(target, EnchantGuideBook.create(service, category));

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("category", category.getColor() + category.getDisplayName());
        ph.put("count", Integer.toString(service.getRegistry().byCategory(category).size()));
        ph.put("player", target.getName());
        languages.send(target, "enchant.book.received", ph);
        if (sender != target) {
            languages.send(sender, "enchant.book.sent", ph);
        }
        return true;
    }

    /** {@code /sfench bookinfo <id>} — book "learn more in chat" click target. */
    private boolean handleBookInfo(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.book")) {
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        CustomEnchant enchant = resolveEnchant(sender, args[1]);
        if (enchant == null) {
            return true;
        }
        renderInfo(sender, enchant);
        return true;
    }

    /**
     * {@code /sfench bookapply <id> <level>} — book level-click target. Applies to the
     * item in hand (main hand if applicable, else off-hand), and only ever raises the
     * enchant to a <em>higher</em> level than what's already present.
     */
    private boolean handleBookApply(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.book")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (args.length < 3) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player player = (Player) sender;
        CustomEnchant enchant = resolveEnchant(sender, args[1]);
        if (enchant == null) {
            return true;
        }
        int level = parseLevel(sender, enchant, args[2]);
        if (level <= 0) {
            return true;
        }
        // Resolve the target item: prefer the main hand; if it isn't applicable (e.g.
        // the open codex book is there), fall back to the off-hand.
        boolean offHand = false;
        ItemStack target = mainHand(player);
        if (!isApplicableTarget(target, enchant)) {
            ItemStack off = offHand(player);
            if (isApplicableTarget(off, enchant)) {
                target = off;
                offHand = true;
            } else {
                languages.send(player, "enchant.book.no-item", one("tags", ItemTag.describe(enchant.getTags())));
                return true;
            }
        }
        // Codex rule: only raise to a strictly higher level than the existing one.
        int existing = service.getStorage().level(target, enchant.getId());
        if (existing >= level) {
            Map<String, String> ph = new LinkedHashMap<String, String>();
            ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
            ph.put("roman", CustomEnchant.roman(existing));
            languages.send(player, "enchant.book.lower-level", ph);
            return true;
        }
        ApplyResult result = service.apply(target, enchant, level);
        feedback.send(player, result, target);
        if (result.isSuccess()) {
            if (offHand) {
                setOffHand(player, target);
            } else {
                setMainHand(player, target);
            }
        }
        return true;
    }

    private boolean handleUtility(CommandSender sender, String[] args, ScrollKind kind) {
        if (!requirePermission(sender, "sfcore.enchants.give")) {
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            languages.send(sender, "enchant.target-offline", one("player", args[1]));
            return true;
        }
        int amount = args.length >= 3 ? Math.max(1, Math.min(64, parseIntOr(args[2], 1))) : 1;
        giveItem(target, items.utility(kind, amount));

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("amount", Integer.toString(amount));
        ph.put("kind", kind.getLabel());
        ph.put("player", target.getName());
        languages.send(target, "enchant.give.utility-received", ph);
        if (sender != target) {
            languages.send(sender, "enchant.give.utility-sent", ph);
        }
        return true;
    }

    // ── list / info ─────────────────────────────────────────────────────────

    private boolean handleList(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.list")) {
            return true;
        }
        EnchantCategory filter = args.length >= 2 ? EnchantCategory.fromId(args[1]) : null;
        if (args.length >= 2 && filter == null) {
            languages.send(sender, "enchant.unknown-category", one("name", args[1]));
            return true;
        }
        int count = filter == null ? service.getRegistry().size() : service.getRegistry().byCategory(filter).size();
        for (String line : languages.list(sender, "enchant.list.header", one("count", Integer.toString(count)))) {
            sender.sendMessage(line);
        }
        for (EnchantCategory category : EnchantCategory.values()) {
            if (filter != null && category != filter) {
                continue;
            }
            List<CustomEnchant> list = service.getRegistry().byCategory(category);
            if (list.isEmpty()) {
                continue;
            }
            Map<String, String> head = new LinkedHashMap<String, String>();
            head.put("color", category.getColor().toString());
            head.put("category", category.getDisplayName());
            head.put("count", Integer.toString(list.size()));
            sender.sendMessage(languages.get(sender, "enchant.list.category", head));
            for (CustomEnchant enchant : list) {
                Map<String, String> row = new LinkedHashMap<String, String>();
                row.put("enchant", enchant.getDisplayName());
                row.put("maxlevel", "I–" + CustomEnchant.roman(enchant.getMaxLevel()));
                row.put("tags", ItemTag.describe(enchant.getTags()));
                EnchantHover.send(sender, languages.get(sender, "enchant.list.entry", row), enchant, 0, null);
            }
        }
        for (String line : languages.list(sender, "enchant.list.footer", Collections.<String, String>emptyMap())) {
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.list")) {
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "enchant.usage");
            return true;
        }
        CustomEnchant enchant = resolveEnchant(sender, args[1]);
        if (enchant == null) {
            return true;
        }
        renderInfo(sender, enchant);
        return true;
    }

    /** Prints the full styled detail block for an enchant to chat (shared by info + bookinfo). */
    private void renderInfo(CommandSender sender, CustomEnchant enchant) {
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
        ph.put("color", enchant.getCategory().getColor().toString());
        ph.put("category", enchant.getCategory().getDisplayName());
        ph.put("rarity", enchant.getRarity().getColor() + enchant.getRarity().getDisplayName());
        ph.put("maxlevel", Integer.toString(enchant.getMaxLevel()));
        ph.put("tags", ItemTag.describe(enchant.getTags()));

        sender.sendMessage(" ");
        EnchantHover.send(sender, languages.get(sender, "enchant.info.header", ph), enchant, 0, null);
        sender.sendMessage(languages.get(sender, "core.divider-line"));
        sender.sendMessage(languages.get(sender, "enchant.info.meta", ph));
        sender.sendMessage(languages.get(sender, "enchant.info.tags", ph));
        sender.sendMessage(" ");
        for (String line : enchant.getDescription()) {
            sender.sendMessage(languages.get(sender, "enchant.info.desc-line",
                    one("line", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)))));
        }
        sender.sendMessage(" ");
        for (int level = 1; level <= enchant.getMaxLevel(); level++) {
            Map<String, String> lvl = new LinkedHashMap<String, String>();
            lvl.put("color", enchant.getCategory().getColor().toString());
            lvl.put("roman", CustomEnchant.roman(level));
            lvl.put("params", describeLevel(enchant, level));
            sender.sendMessage(languages.get(sender, "enchant.info.level-line", lvl));
        }
        sender.sendMessage(languages.get(sender, "core.divider-line"));
    }

    private String describeLevel(CustomEnchant enchant, int level) {
        ConfigurationSection section = enchant.levelSection(level);
        if (section == null) {
            return "-";
        }
        List<String> parts = new ArrayList<String>();
        for (String key : section.getKeys(false)) {
            parts.add(key.replace('_', ' ') + " " + ChatColor.WHITE + section.get(key) + ChatColor.GRAY);
        }
        return parts.isEmpty() ? "-" : String.join(ChatColor.DARK_GRAY + ", " + ChatColor.GRAY, parts);
    }

    // ── loot / debug ──────────────────────────────────────────────────────────

    private boolean handleLoot(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.loot")) {
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            plugin.reloadEnchantLoot();
            languages.send(sender, "enchant.loot.reloaded");
            return true;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("toggle")) {
            String chest = args[2].toLowerCase(Locale.ENGLISH);
            if (!service.isLootChestKnown(chest)) {
                languages.send(sender, "enchant.loot.unknown", one("chest", chest));
                return true;
            }
            boolean newValue = !service.isLootChestEnabled(chest);
            try {
                service.persistLootToggle(chest, newValue);
                plugin.reloadEnchantLoot();
            } catch (IOException exception) {
                sender.sendMessage(ChatColor.RED + "Failed to save loot.yml: " + exception.getMessage());
                return true;
            }
            Map<String, String> ph = new LinkedHashMap<String, String>();
            ph.put("chest", chest);
            ph.put("state", languages.get(sender, newValue ? "enchant.state.enabled" : "enchant.state.disabled"));
            languages.send(sender, "enchant.loot.toggled", ph);
            return true;
        }
        languages.send(sender, "enchant.usage");
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "sfcore.enchants.debug")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        boolean on = args.length >= 2 ? args[1].equalsIgnoreCase("on") : !service.isDebugListener(player);
        service.setDebug(player, on);
        languages.send(player, on ? "enchant.debug.on" : "enchant.debug.off");
        return true;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private boolean requirePermission(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return true;
        }
        languages.send(sender, "core.no-permission");
        return false;
    }

    private CustomEnchant resolveEnchant(CommandSender sender, String name) {
        CustomEnchant enchant = service.getRegistry().get(name);
        if (enchant == null) {
            enchant = service.getRegistry().byDisplayName(name.replace('_', ' '));
        }
        if (enchant == null) {
            languages.send(sender, "enchant.unknown-enchant", one("name", name));
        }
        return enchant;
    }

    private int parseLevel(CommandSender sender, CustomEnchant enchant, String raw) {
        int level = CustomEnchant.parseRoman(raw);
        if (level <= 0 || !enchant.hasLevel(level)) {
            Map<String, String> ph = new LinkedHashMap<String, String>();
            ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
            ph.put("maxlevel", Integer.toString(enchant.getMaxLevel()));
            languages.send(sender, "enchant.unknown-level", ph);
            return 0;
        }
        return level;
    }

    private static int parseIntOr(String raw, int def) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.updateInventory();
    }

    /** True if the item can receive {@code enchant} (and isn't the codex book itself). */
    private boolean isApplicableTarget(ItemStack item, CustomEnchant enchant) {
        if (item == null) {
            return false;
        }
        String type = item.getType().name();
        if (type.equals("AIR") || type.equals("WRITTEN_BOOK") || type.equals("WRITABLE_BOOK")) {
            return false;
        }
        return ItemTag.matchesAny(item, enchant.getTags());
    }

    /** Sends a title to a player executor (no-op for console). Strings are pre-translated. */
    private void sendTitle(CommandSender sender, String title, String subtitle) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;
        try {
            player.sendTitle(title, subtitle, 10, 60, 10);
        } catch (Throwable legacy) {
            try {
                player.sendTitle(title, subtitle);
            } catch (Throwable ignored) {
                // titles unsupported on this fork — the chat error still delivered
            }
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            return player.getInventory().getItemInHand();
        }
    }

    private ItemStack offHand(Player player) {
        try {
            return player.getInventory().getItemInOffHand();
        } catch (Throwable legacy) {
            // off-hand is 1.9+; no off-hand target on older servers
            return null;
        }
    }

    private void setOffHand(Player player, ItemStack item) {
        try {
            player.getInventory().setItemInOffHand(item);
            player.updateInventory();
        } catch (Throwable ignored) {
            // off-hand setter unavailable — main-hand path handles older servers
        }
    }

    @SuppressWarnings("deprecation")
    private void setMainHand(Player player, ItemStack item) {
        try {
            player.getInventory().setItemInMainHand(item);
        } catch (NoSuchMethodError legacy) {
            player.getInventory().setItemInHand(item);
        }
        player.updateInventory();
    }

    private static Map<String, String> one(String key, String value) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(key, value);
        return map;
    }

    // ── tab completion ──────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("admin", "give", "givebook", "apply", "remove", "list", "info",
                    "reload", "loot", "protect", "success", "debug"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (args.length == 2) {
            if (sub.equals("apply") || sub.equals("remove") || sub.equals("info")) {
                return filter(enchantIds(), args[1]);
            }
            if (sub.equals("list")) {
                return filter(categoryIds(), args[1]);
            }
            if (sub.equals("give") || sub.equals("givebook") || sub.equals("protect") || sub.equals("success")) {
                return filter(playerNames(), args[1]);
            }
            if (sub.equals("loot")) {
                return filter(Arrays.asList("toggle", "reload"), args[1]);
            }
            if (sub.equals("debug")) {
                return filter(Arrays.asList("on", "off"), args[1]);
            }
        }
        if (args.length == 3) {
            if (sub.equals("apply")) {
                return filter(levelsFor(args[1]), args[2]);
            }
            if (sub.equals("give")) {
                List<String> options = new ArrayList<String>(enchantIds());
                options.add("book");
                return filter(options, args[2]);
            }
            if (sub.equals("givebook")) {
                return filter(enchantIds(), args[2]);
            }
            if (sub.equals("loot") && args[1].equalsIgnoreCase("toggle")) {
                return filter(service.lootChestTypes(), args[2]);
            }
        }
        if (args.length == 4) {
            if (sub.equals("give") && args[2].equalsIgnoreCase("book")) {
                return filter(categoryIds(), args[3]);
            }
            if (sub.equals("give") || sub.equals("givebook")) {
                return filter(levelsFor(args[2]), args[3]);
            }
        }
        return Collections.emptyList();
    }

    /** Every valid level (1..maxLevel) for the named enchant, for tab completion. */
    private List<String> levelsFor(String enchantName) {
        CustomEnchant enchant = service.getRegistry().get(enchantName);
        if (enchant == null) {
            enchant = service.getRegistry().byDisplayName(enchantName.replace('_', ' '));
        }
        List<String> levels = new ArrayList<String>();
        if (enchant != null) {
            for (int level = 1; level <= enchant.getMaxLevel(); level++) {
                levels.add(Integer.toString(level));
            }
        }
        return levels;
    }

    private List<String> enchantIds() {
        List<String> ids = new ArrayList<String>();
        for (CustomEnchant enchant : service.getRegistry().all()) {
            ids.add(enchant.getId());
        }
        return ids;
    }

    private List<String> categoryIds() {
        List<String> ids = new ArrayList<String>();
        for (EnchantCategory category : EnchantCategory.values()) {
            ids.add(category.getId());
        }
        return ids;
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
