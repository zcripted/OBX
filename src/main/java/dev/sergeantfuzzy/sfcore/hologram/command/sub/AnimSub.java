package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.HoloMessages;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationConfig;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationRegistry;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramId;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * {@code /holo anim <id> <add <type> [k=v …]|remove <targets>|toggle <target>|reset|list>}
 *
 * <p>Known types: {@code fade}, {@code rotate}, {@code bob}.
 *
 * <p><strong>remove</strong> accepts a comma-separated list of indices and/or
 * type names ({@code 1,3} or {@code bob,fade} or mixed {@code 1,fade}); a
 * type-name token removes <em>every</em> currently-configured animation of
 * that type. Indices are 1-based.
 *
 * <p><strong>list</strong> renders each entry with a clickable {@code [ON]} /
 * {@code [OFF]} status pill (BungeeCord chat components) that runs
 * {@code /holo anim <id> toggle <index>}, so operators can disable/re-enable
 * an animation without retyping its parameters. Console gets the plain-text
 * fallback.
 *
 * <p><strong>toggle</strong> flips a config's enabled flag in place and
 * rebuilds the live animation list — the disabled config is retained so
 * it can be flipped back on later without re-entering parameters.
 *
 * <p><strong>reset</strong> drops every animation on the hologram in one
 * call.
 */
public final class AnimSub implements HoloSubCommand {

    private final HoloContext ctx;

    public AnimSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "anim";
    }

    @Override
    public String permission() {
        return "sfcore.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6/holo anim §f<id> §7<add <type> [k=v …]|remove <targets>|toggle <target>|reset|list>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        String op = args[1].toLowerCase(Locale.ENGLISH);
        switch (op) {
            case "list":
                renderList(sender, hologram);
                return true;
            case "add": {
                if (args.length < 3) {
                    sender.sendMessage("§6/holo anim §f<id> add §7<type> [k=v …]");
                    return true;
                }
                String type = args[2].toLowerCase(Locale.ENGLISH);
                if (!AnimationRegistry.isKnown(type)) {
                    sender.sendMessage("§cUnknown animation type. Known: fade, rotate, bob.");
                    return true;
                }
                Map<String, Object> params = new LinkedHashMap<>();
                for (int i = 3; i < args.length; i++) {
                    String[] kv = args[i].split("=", 2);
                    if (kv.length != 2) {
                        continue;
                    }
                    try {
                        params.put(kv[0], Double.parseDouble(kv[1]));
                    } catch (NumberFormatException ignored) {
                        params.put(kv[0], kv[1]);
                    }
                }
                hologram.addAnimation(new AnimationConfig(type, params));
                ctx.persistAndRefresh(hologram);
                ctx.msg(sender, "hologram.anim.added", "name", hologram.getId().value());
                return true;
            }
            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage("§6/holo anim §f<id> remove §7<index|type>[,<index|type>…]");
                    return true;
                }
                int removed = removeTargets(hologram, args[2]);
                if (removed == 0) {
                    ctx.msg(sender, "hologram.error.invalid_index");
                    return true;
                }
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§aRemoved §f" + removed
                        + " §aanimation" + (removed == 1 ? "" : "s") + " from §f"
                        + hologram.getId().value()));
                return true;
            }
            case "toggle": {
                if (args.length < 3) {
                    sender.sendMessage("§6/holo anim §f<id> toggle §7<index|type>");
                    return true;
                }
                int index = ctx.parseLineIndex(args[2]);
                if (index < 0) {
                    index = findFirstAnimationOfType(hologram, args[2]);
                }
                List<AnimationConfig> configs = hologram.getAnimationConfigs();
                if (index < 0 || index >= configs.size()) {
                    ctx.msg(sender, "hologram.error.invalid_index");
                    return true;
                }
                AnimationConfig cfg = configs.get(index);
                cfg.setEnabled(!cfg.isEnabled());
                hologram.rebuildAnimations();
                hologram.markDirty();
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§7" + cfg.getType() + " §8· "
                        + (cfg.isEnabled() ? "§aenabled" : "§cdisabled")));
                return true;
            }
            case "reset":
            case "clear": {
                int count = hologram.getAnimationConfigs().size();
                if (count == 0) {
                    sender.sendMessage(HoloMessages.inline("§7No animations to reset on §f"
                            + hologram.getId().value()));
                    return true;
                }
                // Remove from the end so each removeAnimation() call sees a
                // stable index for the next-up element.
                for (int i = count - 1; i >= 0; i--) {
                    hologram.removeAnimation(i);
                }
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§aReset §f" + count
                        + " §aanimation" + (count == 1 ? "" : "s") + " on §f"
                        + hologram.getId().value()));
                return true;
            }
            default:
                ctx.msg(sender, "hologram.error.invalid_value");
                return true;
        }
    }

    /**
     * Parses a comma-separated target list (mixed indices and type names),
     * deduplicates resolved 0-based indices, and removes them from highest to
     * lowest so list positions stay valid through the loop. A type-name
     * token resolves to <em>every</em> matching config (so
     * {@code remove bob} drops two bobs if both exist).
     */
    private int removeTargets(Hologram hologram, String rawTargets) {
        String[] tokens = rawTargets.split(",");
        // Descending so we pop tail elements first → earlier indices stay valid.
        TreeSet<Integer> indices = new TreeSet<>(Collections.reverseOrder());
        for (String token : tokens) {
            String trim = token.trim();
            if (trim.isEmpty()) {
                continue;
            }
            int idx = ctx.parseLineIndex(trim);
            if (idx >= 0) {
                indices.add(idx);
                continue;
            }
            indices.addAll(findAllAnimationsOfType(hologram, trim));
        }
        int removed = 0;
        for (int i : indices) {
            if (hologram.removeAnimation(i)) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Sends a {@code /holo anim list} report. Each entry is rendered as
     * {@code <index>) [ON]/[OFF] <type> {params…}} where the status pill is a
     * clickable BungeeCord component that runs the toggle command. Console
     * senders fall back to plain legacy text since they can't receive
     * components.
     */
    private void renderList(CommandSender sender, Hologram hologram) {
        sender.sendMessage(HoloMessages.header(hologram.getId().value() + " §8· §7animations"));
        sender.sendMessage(HoloMessages.DIVIDER);
        List<AnimationConfig> configs = hologram.getAnimationConfigs();
        if (configs.isEmpty()) {
            sender.sendMessage("§7  (none)");
            sender.sendMessage("§8  §oTip: §7/holo anim §f" + hologram.getId().value()
                    + " §7add §f<type>");
            return;
        }
        boolean canClick = sender instanceof Player;
        for (int i = 0; i < configs.size(); i++) {
            AnimationConfig cfg = configs.get(i);
            int index1Based = i + 1;
            String prefix = "§8  " + index1Based + ") ";
            String statusPill = cfg.isEnabled() ? "§a[ON] " : "§c[OFF]";
            String suffix = " §7" + cfg.getType() + " §8" + cfg.getParams();
            if (!canClick) {
                sender.sendMessage(prefix + statusPill + suffix);
                continue;
            }
            String toggleCmd = "/holo anim " + hologram.getId().value() + " toggle " + index1Based;
            String hoverLabel = cfg.isEnabled() ? "§eClick to disable" : "§eClick to enable";
            BaseComponent[] line = buildClickableRow(prefix, statusPill, suffix, toggleCmd, hoverLabel);
            ((Player) sender).spigot().sendMessage(line);
        }
        sender.sendMessage("§8  §oClick §a[ON]§r§8§o / §c[OFF]§r§8§o to toggle.");
    }

    /**
     * Stitches the three legacy-text segments into a {@link BaseComponent}
     * array, attaching a {@link ClickEvent#Action#RUN_COMMAND} and a hover
     * tooltip only to the status pill so the rest of the row is plain.
     */
    private static BaseComponent[] buildClickableRow(String prefix, String pill, String suffix,
                                                     String runCommand, String hoverLegacy) {
        ComponentBuilder builder = new ComponentBuilder("");
        for (BaseComponent piece : TextComponent.fromLegacyText(prefix)) {
            builder.append(piece, ComponentBuilder.FormatRetention.NONE);
        }
        BaseComponent[] hoverParts = TextComponent.fromLegacyText(hoverLegacy);
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, runCommand);
        // Old-form HoverEvent ctor — works on every BungeeCord shipped with
        // every supported Spigot/Paper version; the newer Text wrapper isn't.
        @SuppressWarnings("deprecation")
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverParts);
        for (BaseComponent piece : TextComponent.fromLegacyText(pill)) {
            piece.setClickEvent(click);
            piece.setHoverEvent(hover);
            builder.append(piece, ComponentBuilder.FormatRetention.NONE);
        }
        for (BaseComponent piece : TextComponent.fromLegacyText(suffix)) {
            builder.append(piece, ComponentBuilder.FormatRetention.NONE);
        }
        return builder.create();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        if (args.length == 2) {
            return filterPrefix(Arrays.asList("add", "remove", "toggle", "reset", "list"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            return Arrays.asList("fade", "rotate", "bob");
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("toggle"))) {
            return suggestActiveAnimations(args[0], args[2], args[1].equalsIgnoreCase("remove"));
        }
        return Collections.emptyList();
    }

    /**
     * Returns the 0-based position of the first {@link AnimationConfig} whose
     * type case-insensitively matches {@code typeName}, or {@code -1} when no
     * such animation is configured.
     */
    private int findFirstAnimationOfType(Hologram hologram, String typeName) {
        if (hologram == null || typeName == null) {
            return -1;
        }
        String normalised = typeName.toLowerCase(Locale.ENGLISH);
        List<AnimationConfig> configs = hologram.getAnimationConfigs();
        for (int i = 0; i < configs.size(); i++) {
            if (normalised.equals(configs.get(i).getType().toLowerCase(Locale.ENGLISH))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns every 0-based position of an {@link AnimationConfig} whose
     * type case-insensitively matches {@code typeName}.
     */
    private List<Integer> findAllAnimationsOfType(Hologram hologram, String typeName) {
        if (hologram == null || typeName == null) {
            return Collections.emptyList();
        }
        String normalised = typeName.toLowerCase(Locale.ENGLISH);
        List<AnimationConfig> configs = hologram.getAnimationConfigs();
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            if (normalised.equals(configs.get(i).getType().toLowerCase(Locale.ENGLISH))) {
                matches.add(i);
            }
        }
        return matches;
    }

    /**
     * Builds the tab-completion list for {@code remove} and {@code toggle}.
     * Resolves the hologram silently, emits each currently-configured
     * animation's 1-based index plus its type name (deduplicated).
     *
     * <p>When {@code supportsCommaList} is true (the {@code remove} case),
     * partial input ending in or containing a comma is supported: the part
     * before the last comma is preserved as a prefix on every suggestion so
     * tab cycles between completions for the trailing token only.
     */
    private List<String> suggestActiveAnimations(String rawId, String partial, boolean supportsCommaList) {
        HologramId id = HologramId.parse(rawId);
        if (id == null) {
            return Collections.emptyList();
        }
        Hologram hologram = ctx.service().getRegistry().get(id);
        if (hologram == null) {
            return Collections.emptyList();
        }
        List<AnimationConfig> configs = hologram.getAnimationConfigs();
        if (configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> bareSuggestions = new ArrayList<>(configs.size() * 2);
        LinkedHashSet<String> seenTypes = new LinkedHashSet<>();
        for (int i = 0; i < configs.size(); i++) {
            bareSuggestions.add(String.valueOf(i + 1));
            String type = configs.get(i).getType();
            if (type != null && !type.isEmpty()) {
                seenTypes.add(type.toLowerCase(Locale.ENGLISH));
            }
        }
        bareSuggestions.addAll(seenTypes);

        String commaPrefix = "";
        String tail = partial == null ? "" : partial;
        if (supportsCommaList) {
            int lastComma = tail.lastIndexOf(',');
            if (lastComma >= 0) {
                commaPrefix = tail.substring(0, lastComma + 1);
                tail = tail.substring(lastComma + 1);
            }
        }
        String tailLower = tail.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>(bareSuggestions.size());
        for (String candidate : bareSuggestions) {
            if (candidate.toLowerCase(Locale.ENGLISH).startsWith(tailLower)) {
                filtered.add(commaPrefix + candidate);
            }
        }
        return filtered;
    }

    private static List<String> filterPrefix(List<String> source, String raw) {
        if (raw == null || raw.isEmpty()) {
            return source;
        }
        String lower = raw.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>(source.size());
        for (String candidate : source) {
            if (candidate.startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}
