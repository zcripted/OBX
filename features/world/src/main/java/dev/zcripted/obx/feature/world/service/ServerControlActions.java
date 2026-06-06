package dev.zcripted.obx.feature.world.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.message.ConsoleLog;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Single home for the server-control actions that are reachable from BOTH the
 * Admin GUI and the {@code /obx} button sub-commands (so the clickable chat
 * buttons and the menu clicks run identical logic).
 *
 * <p>Every action applies the change, sends the actor a {@code /ban}-style
 * prefixed message — with clickable buttons where the spec calls for them
 * (whitelist / join-lock toggles, per-type entity clears) — and mirrors a plain
 * line to the console.</p>
 */
public final class ServerControlActions {

    public enum ClearMode {ALL, MOBS_ONLY, ITEMS_ONLY}

    private ServerControlActions() {
    }

    // ── Whitelist ────────────────────────────────────────────────────────────

    public static void toggleWhitelist(ObxPlugin plugin, CommandSender actor) {
        setWhitelist(plugin, actor, !Bukkit.hasWhitelist());
    }

    public static void setWhitelist(ObxPlugin plugin, CommandSender actor, boolean enabled) {
        Bukkit.setWhitelist(enabled);
        LanguageManager lang = plugin.getLanguageManager();
        String state = lang.get(actor, enabled ? "admin.whitelist.state.enabled" : "admin.whitelist.state.disabled");
        String body = lang.get(actor, "admin.whitelist.toggled", Placeholders.with("state", state));
        frameTop(actor, "Whitelist");
        sendWithToggle(plugin, actor, body, "/obx x-action whitelist toggle", "admin.whitelist.button.hover");
        frameBottom(actor);
        console(plugin, "Whitelist " + (enabled ? "enabled" : "disabled") + " by " + name(actor));
    }

    // ── Join lock ────────────────────────────────────────────────────────────

    public static void toggleJoinLock(ObxPlugin plugin, CommandSender actor) {
        setJoinLock(plugin, actor, !ServerControlState.isJoinLocked());
    }

    public static void setJoinLock(ObxPlugin plugin, CommandSender actor, boolean locked) {
        ServerControlState.setJoinLocked(locked);
        LanguageManager lang = plugin.getLanguageManager();
        if (locked) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.isOp()) {
                    target.kickPlayer(lang.get(target, "admin.joinlock.kick-reason"));
                }
            }
        }
        String state = lang.get(actor, locked ? "admin.joinlock.state.enabled" : "admin.joinlock.state.disabled");
        String body = lang.get(actor, "admin.joinlock.enabled", Placeholders.with("state", state));
        frameTop(actor, "Join Lock");
        sendWithToggle(plugin, actor, body, "/obx x-action joinlock toggle", "admin.joinlock.button.hover");
        frameBottom(actor);
        console(plugin, "Join lock " + (locked ? "enabled" : "disabled") + " by " + name(actor));
    }

    // ── Clear entities ───────────────────────────────────────────────────────

    public static int clearEntities(ObxPlugin plugin, CommandSender actor, ClearMode mode) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (mode == ClearMode.MOBS_ONLY && !(entity instanceof LivingEntity)) {
                    continue;
                }
                if (mode == ClearMode.ITEMS_ONLY && !(entity instanceof Item)) {
                    continue;
                }
                if (entity.getType() == EntityType.ARMOR_STAND) {
                    continue;
                }
                entity.remove();
                removed++;
            }
        }
        LanguageManager lang = plugin.getLanguageManager();
        String modeKey;
        switch (mode) {
            case MOBS_ONLY: modeKey = "admin.performance.mode.mobs"; break;
            case ITEMS_ONLY: modeKey = "admin.performance.mode.items"; break;
            case ALL: default: modeKey = "admin.performance.mode.all"; break;
        }
        String body = lang.get(actor, "admin.performance.cleared",
                Placeholders.with("removed", String.valueOf(removed), "mode", lang.get(actor, modeKey)));
        frameTop(actor, "Clear Entities");
        actor.sendMessage(legacy(body));
        sendButtonRow(actor, Arrays.asList(
                button(lang, actor, "admin.button.clear-all", "admin.button.clear-all.hover", "/obx x-action clearentities all"),
                button(lang, actor, "admin.button.clear-mobs", "admin.button.clear-mobs.hover", "/obx x-action clearentities mobs"),
                button(lang, actor, "admin.button.clear-items", "admin.button.clear-items.hover", "/obx x-action clearentities items")));
        frameBottom(actor);
        console(plugin, "Cleared " + removed + " entities (" + mode + ") by " + name(actor));
        return removed;
    }

    // ── Kick non-ops ─────────────────────────────────────────────────────────

    public static void kickNonOps(ObxPlugin plugin, CommandSender actor) {
        LanguageManager lang = plugin.getLanguageManager();
        List<String> kicked = new ArrayList<>();
        for (Player target : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (!target.isOp()) {
                kicked.add(target.getName());
                target.kickPlayer(lang.get(target, "admin.kick-nonops.reason"));
            }
        }
        frameTop(actor, "Kick Non-Ops");
        if (kicked.isEmpty()) {
            lang.send(actor, "admin.kick-nonops.none");
        } else {
            lang.send(actor, "admin.kick-nonops.header", Placeholders.with("count", String.valueOf(kicked.size())));
            for (String n : kicked) {
                lang.send(actor, "admin.kick-nonops.entry", Placeholders.with("player", n));
            }
        }
        frameBottom(actor);
        ConsoleLog.list(plugin, "Kicked " + kicked.size() + " non-op player(s) by " + name(actor) + ":", kicked);
    }

    // ── Spectator-only ───────────────────────────────────────────────────────

    public static void forceSpectator(ObxPlugin plugin, CommandSender actor) {
        LanguageManager lang = plugin.getLanguageManager();
        List<String[]> switched = new ArrayList<>(); // [name, fromMode]
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.isOp() || target.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            String fromMode = friendlyMode(target.getGameMode());
            target.setGameMode(GameMode.SPECTATOR);
            switched.add(new String[]{target.getName(), fromMode});
        }
        frameTop(actor, "Spectator Only");
        if (switched.isEmpty()) {
            lang.send(actor, "admin.spectator.none");
        } else {
            lang.send(actor, "admin.spectator.header", Placeholders.with("count", String.valueOf(switched.size())));
            for (String[] entry : switched) {
                lang.send(actor, "admin.spectator.entry",
                        Placeholders.with("player", entry[0], "mode", entry[1]));
            }
        }
        frameBottom(actor);
        List<String> consoleLines = new ArrayList<>();
        for (String[] entry : switched) {
            consoleLines.add(entry[0] + " (from " + entry[1] + ")");
        }
        ConsoleLog.list(plugin, "Forced " + switched.size() + " non-op player(s) into Spectator by " + name(actor) + ":", consoleLines);
    }

    /**
     * Frames a simple (non-interactive) one-line action message in the OBX box —
     * the same styled layout as the {@code /ban} usage messages. Used by the
     * weather / redstone / TPS handlers.
     */
    public static void boxMessage(ObxPlugin plugin, CommandSender actor, String boxName, String bodyKey, java.util.Map<String, String> placeholders) {
        LanguageManager lang = plugin.getLanguageManager();
        String body = placeholders == null ? lang.get(actor, bodyKey) : lang.get(actor, bodyKey, placeholders);
        frameTop(actor, boxName);
        actor.sendMessage(legacy(body));
        frameBottom(actor);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    // The OBX boxed-report frame, matching the /ban usage message layout:
    //   (blank) / ▍ 𝗢𝗕𝗫  ›  Server  ·  <Name> / 30× ─ / (blank) / <body> / (blank)
    private static final String BOX_RULE = "&8" + repeat('─', 30);

    private static String boxTitle(String name) {
        // ▍ 𝗢𝗕𝗫  ›  Server  ·  <name>  (math-bold OBX as surrogate-pair escapes).
        return "&5▍ &5𝗢𝗕𝗫  &8›  &fServer  &8·  &f" + name;
    }

    private static void frameTop(CommandSender actor, String name) {
        actor.sendMessage("");
        actor.sendMessage(legacy(boxTitle(name)));
        actor.sendMessage(legacy(BOX_RULE));
        actor.sendMessage("");
    }

    private static void frameBottom(CommandSender actor) {
        actor.sendMessage("");
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            b.append(c);
        }
        return b.toString();
    }

    private static void sendWithToggle(ObxPlugin plugin, CommandSender actor, String body, String command, String hoverKey) {
        LanguageManager lang = plugin.getLanguageManager();
        actor.sendMessage(legacy(body));
        sendButtonRow(actor, Arrays.asList(
                button(lang, actor, "admin.button.toggle", hoverKey, command)));
    }

    /**
     * Renders a dedicated, indented action row of clickable buttons on its own
     * line beneath the box body — e.g. {@code   [Toggle]} or
     * {@code   [All]  [Mobs]  [Items]}. Console senders (which can't click) just
     * see the body line above, so the row is skipped for them.
     */
    private static void sendButtonRow(CommandSender actor, List<InteractiveMessagePart> buttons) {
        if (!(actor instanceof Player) || buttons.isEmpty()) {
            return;
        }
        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain(legacy("  &8» ")));
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                parts.add(InteractiveMessagePart.plain("  "));
            }
            parts.add(buttons.get(i));
        }
        ComponentMessenger.sendJoinedHoverMessages(actor, parts);
    }

    /**
     * Boxed weather message: a body line with the weather type in a secondary
     * colour (capitalised, e.g. {@code Rain}) plus an action row carrying a button
     * for every weather event. Used by both the Weather Control GUI and
     * {@code /weather}. The buttons run {@code /weather <mode>}.
     */
    public static void weatherMessage(ObxPlugin plugin, CommandSender actor, String mode) {
        LanguageManager lang = plugin.getLanguageManager();
        String typeName = lang.get(actor, "admin.weather.type." + mode);
        frameTop(actor, "Weather");
        actor.sendMessage(legacy(lang.get(actor, "admin.weather.set", Placeholders.with("weather", typeName))));
        sendButtonRow(actor, Arrays.asList(
                weatherButton(lang, actor, "clear"),
                weatherButton(lang, actor, "rain"),
                weatherButton(lang, actor, "thunder")));
        frameBottom(actor);
        console(plugin, "Weather set to " + mode + " by " + name(actor));
    }

    /**
     * Boxed redstone toggle message: the new mode (red FROZEN / green RESUMED) plus
     * a context line on what redstone freezing affects, then a plain console mirror.
     */
    public static void redstoneMessage(ObxPlugin plugin, CommandSender actor, boolean frozen) {
        LanguageManager lang = plugin.getLanguageManager();
        frameTop(actor, "Redstone");
        actor.sendMessage(legacy(lang.get(actor, frozen ? "admin.redstone.box.frozen" : "admin.redstone.box.resumed")));
        actor.sendMessage(legacy(lang.get(actor, "admin.redstone.box.context")));
        frameBottom(actor);
        console(plugin, "Redstone updates " + (frozen ? "frozen" : "resumed") + " by " + name(actor));
    }

    /** Flip auto-save on every loaded world; returns the new state. */
    public static boolean toggleAutoSave(ObxPlugin plugin, CommandSender actor) {
        boolean newState = Bukkit.getWorlds().isEmpty() || !Bukkit.getWorlds().get(0).isAutoSave();
        setAutoSave(plugin, actor, newState);
        return newState;
    }

    /** Set auto-save explicitly on every loaded world (used by the chat bridge). */
    public static void setAutoSave(ObxPlugin plugin, CommandSender actor, boolean enabled) {
        for (World world : Bukkit.getWorlds()) {
            world.setAutoSave(enabled);
        }
        autoSaveMessage(plugin, actor, enabled);
    }

    /**
     * Boxed auto-save message: the new state (green ENABLED / red DISABLED) with the
     * [Toggle] button on its own indented row, mirrored button-less to the console.
     */
    private static void autoSaveMessage(ObxPlugin plugin, CommandSender actor, boolean enabled) {
        LanguageManager lang = plugin.getLanguageManager();
        frameTop(actor, "Auto-Save");
        actor.sendMessage(legacy(lang.get(actor, enabled ? "admin.world.autosave.box.enabled" : "admin.world.autosave.box.disabled")));
        sendButtonRow(actor, Arrays.asList(
                button(lang, actor, "admin.button.toggle", "admin.world.autosave.box.toggle-hover", "/obx x-action autosave toggle")));
        frameBottom(actor);
        console(plugin, "Auto-save " + (enabled ? "enabled" : "disabled") + " for all worlds by " + name(actor));
    }

    /**
     * Boxed Save-Worlds report: what was flushed and where. The per-world row carries
     * a hover tooltip with each world's folder path + saved data (click copies the
     * path). The console mirror prints the full per-world detail.
     */
    public static void saveWorldsMessage(ObxPlugin plugin, CommandSender actor, List<World> worlds) {
        LanguageManager lang = plugin.getLanguageManager();
        frameTop(actor, "Save Worlds");
        actor.sendMessage(legacy(lang.get(actor, "admin.world.save.box.header",
                Placeholders.with("count", String.valueOf(worlds.size())))));
        actor.sendMessage(legacy(lang.get(actor, "admin.world.save.box.detail")));
        if (actor instanceof Player && !worlds.isEmpty()) {
            List<InteractiveMessagePart> parts = new ArrayList<>();
            parts.add(InteractiveMessagePart.plain(legacy("  &8» &7Worlds: ")));
            for (int i = 0; i < worlds.size(); i++) {
                World world = worlds.get(i);
                String path = worldPath(world);
                if (i > 0) {
                    parts.add(InteractiveMessagePart.plain(legacy("&8, ")));
                }
                List<String> hover = Arrays.asList(
                        legacy("&d" + world.getName()),
                        legacy("&8» &7Folder: &f" + path),
                        legacy("&8» &7Saved: &fchunks, entities, player data, level.dat"),
                        legacy("&8(click to copy the folder path)"));
                parts.add(InteractiveMessagePart.copy(legacy("&f" + world.getName()), hover, path));
            }
            ComponentMessenger.sendJoinedHoverMessages(actor, parts);
        }
        frameBottom(actor);
        console(plugin, "Saved " + worlds.size() + " world(s) by " + name(actor)
                + " — chunks, entities, player data, level.dat:");
        for (World world : worlds) {
            console(plugin, "  - " + world.getName() + " -> " + worldPath(world));
        }
    }

    private static String worldPath(World world) {
        try {
            return world.getWorldFolder().getPath();
        } catch (Throwable ignored) {
            return world.getName();
        }
    }

    private static InteractiveMessagePart weatherButton(LanguageManager lang, CommandSender actor, String mode) {
        String typeName = lang.get(actor, "admin.weather.type." + mode);
        String label = legacy(lang.get(actor, "admin.button.weather", Placeholders.with("weather", typeName)));
        List<String> hover = Arrays.asList(legacy(lang.get(actor, "admin.button.weather.hover", Placeholders.with("weather", typeName))));
        return InteractiveMessagePart.interactive(label, hover, "/weather " + mode, true);
    }

    /**
     * Boxed time message with a body line (world + named time + tick) plus an action row of
     * morning/noon/night/midnight buttons. Used by {@code /time}, {@code /day}, {@code /night},
     * and {@code /sun}; the buttons run {@code /time set <ticks>}.
     */
    public static void timeMessage(ObxPlugin plugin, CommandSender actor, String worldName, long ticks) {
        LanguageManager lang = plugin.getLanguageManager();
        frameTop(actor, "Time");
        java.util.Map<String, String> ph = new java.util.LinkedHashMap<>();
        ph.put("world", worldName);
        ph.put("time", String.valueOf(ticks));
        ph.put("label", timeLabel(lang, actor, ticks));
        actor.sendMessage(legacy(lang.get(actor, "admin.time.set", ph)));
        sendButtonRow(actor, Arrays.asList(
                timeButton(lang, actor, "morning", 1000L),
                timeButton(lang, actor, "noon", 6000L),
                timeButton(lang, actor, "night", 13000L),
                timeButton(lang, actor, "midnight", 18000L)));
        frameBottom(actor);
        console(plugin, "Time set to " + ticks + " in " + worldName + " by " + name(actor));
    }

    private static String timeLabel(LanguageManager lang, CommandSender actor, long ticks) {
        if (ticks == 1000L) return lang.get(actor, "admin.time.type.morning");
        if (ticks == 6000L) return lang.get(actor, "admin.time.type.noon");
        if (ticks == 13000L) return lang.get(actor, "admin.time.type.night");
        if (ticks == 18000L) return lang.get(actor, "admin.time.type.midnight");
        return String.valueOf(ticks);
    }

    private static InteractiveMessagePart timeButton(LanguageManager lang, CommandSender actor, String mode, long ticks) {
        String typeName = lang.get(actor, "admin.time.type." + mode);
        String label = legacy(lang.get(actor, "admin.button.time", Placeholders.with("time", typeName)));
        List<String> hover = Arrays.asList(legacy(lang.get(actor, "admin.button.time.hover", Placeholders.with("time", typeName))));
        return InteractiveMessagePart.interactive(label, hover, "/time set " + ticks, true);
    }

    /**
     * Boxed personal-time message for {@code /ptime}: a body line (the new client-side time, or a
     * reset notice) plus an action row of morning/noon/night/midnight buttons and a Reset button.
     * Unlike {@link #timeMessage} the buttons run {@code /ptime <mode>} so they target the player's
     * own time, not the world. Player-only surface — console never reaches it.
     */
    public static void pTimeMessage(ObxPlugin plugin, CommandSender actor, long ticks, boolean reset) {
        LanguageManager lang = plugin.getLanguageManager();
        frameTop(actor, "Personal Time");
        if (reset) {
            actor.sendMessage(legacy(lang.get(actor, "admin.ptime.reset")));
        } else {
            java.util.Map<String, String> ph = new java.util.LinkedHashMap<>();
            ph.put("time", String.valueOf(ticks));
            ph.put("label", timeLabel(lang, actor, ticks));
            actor.sendMessage(legacy(lang.get(actor, "admin.ptime.set", ph)));
        }
        sendButtonRow(actor, Arrays.asList(
                pTimeButton(lang, actor, "morning", 1000L),
                pTimeButton(lang, actor, "noon", 6000L),
                pTimeButton(lang, actor, "night", 13000L),
                pTimeButton(lang, actor, "midnight", 18000L),
                pTimeResetButton(lang, actor)));
        frameBottom(actor);
    }

    private static InteractiveMessagePart pTimeButton(LanguageManager lang, CommandSender actor, String mode, long ticks) {
        String typeName = lang.get(actor, "admin.time.type." + mode);
        String label = legacy(lang.get(actor, "admin.button.time", Placeholders.with("time", typeName)));
        List<String> hover = Arrays.asList(legacy(lang.get(actor, "admin.button.ptime.hover", Placeholders.with("time", typeName))));
        return InteractiveMessagePart.interactive(label, hover, "/ptime " + mode, true);
    }

    private static InteractiveMessagePart pTimeResetButton(LanguageManager lang, CommandSender actor) {
        String label = legacy(lang.get(actor, "admin.button.ptime-reset"));
        List<String> hover = Arrays.asList(legacy(lang.get(actor, "admin.button.ptime-reset.hover")));
        return InteractiveMessagePart.interactive(label, hover, "/ptime reset", true);
    }

    // Labels/hover must be section-coded before reaching the BungeeCord
    // TextComponent.fromLegacyText path, so colorize them here.
    private static InteractiveMessagePart button(LanguageManager lang, CommandSender actor,
                                                 String labelKey, String hoverKey, String command) {
        String label = legacy(lang.get(actor, labelKey));
        List<String> hover = Arrays.asList(legacy(lang.get(actor, hoverKey)));
        return InteractiveMessagePart.interactive(label, hover, command, true);
    }

    private static String friendlyMode(GameMode mode) {
        switch (mode) {
            case CREATIVE: return "Creative";
            case ADVENTURE: return "Adventure";
            case SPECTATOR: return "Spectator";
            case SURVIVAL: default: return "Survival";
        }
    }

    private static String legacy(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    private static void console(ObxPlugin plugin, String message) {
        ConsoleLog.info(plugin, message);
    }

    private static String name(CommandSender actor) {
        return actor == null ? "console" : actor.getName();
    }
}
