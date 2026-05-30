package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.HoloMessages;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationConfig;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationRegistry;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramId;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /holo anim <id> <add <type> [k=v …]|remove <index>|list>}.
 * Known types: {@code fade}, {@code rotate}, {@code bob}.
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
            sender.sendMessage("§6/holo anim §f<id> §7<add <type> [k=v …]|remove <index>|list>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        String op = args[1].toLowerCase();
        switch (op) {
            case "list":
                sender.sendMessage(HoloMessages.header(hologram.getId().value() + " §8· §7animations"));
                if (hologram.getAnimationConfigs().isEmpty()) {
                    sender.sendMessage("§7  (none)");
                } else {
                    int i = 0;
                    for (AnimationConfig cfg : hologram.getAnimationConfigs()) {
                        sender.sendMessage("§8  " + (++i) + ") §7" + cfg.getType() + " §8" + cfg.getParams());
                    }
                }
                return true;
            case "add": {
                if (args.length < 3) {
                    sender.sendMessage("§6/holo anim §f<id> add §7<type> [k=v …]");
                    return true;
                }
                String type = args[2].toLowerCase();
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
                    sender.sendMessage("§6/holo anim §f<id> remove §7<index|type>");
                    return true;
                }
                int index = ctx.parseLineIndex(args[2]);
                // Fall back to type-name lookup when the arg isn't a number — lets
                // operators type the tab-completed name (e.g. "bob") instead of
                // having to look up the index first.
                if (index < 0) {
                    index = findFirstAnimationOfType(hologram, args[2]);
                }
                if (!hologram.removeAnimation(index)) {
                    ctx.msg(sender, "hologram.error.invalid_index");
                    return true;
                }
                ctx.persistAndRefresh(hologram);
                ctx.msg(sender, "hologram.anim.removed", "name", hologram.getId().value());
                return true;
            }
            default:
                ctx.msg(sender, "hologram.error.invalid_value");
                return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("add", "remove", "list");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            return Arrays.asList("fade", "rotate", "bob");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            // Suggest both 1-based indices and unique type names of every
            // animation currently attached to the hologram — typed name or
            // numeric index both work in execute().
            return suggestActiveAnimations(args[0], args[2]);
        }
        return java.util.Collections.emptyList();
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
     * Builds the tab-completion list for {@code /holo anim <id> remove …}.
     * Resolves the hologram silently (no error messages on a partial id),
     * then emits each currently-configured animation's 1-based index plus its
     * type name (deduplicated). The user can type either form.
     */
    private List<String> suggestActiveAnimations(String rawId, String partial) {
        HologramId id = HologramId.parse(rawId);
        if (id == null) {
            return java.util.Collections.emptyList();
        }
        Hologram hologram = ctx.service().getRegistry().get(id);
        if (hologram == null) {
            return java.util.Collections.emptyList();
        }
        List<AnimationConfig> configs = hologram.getAnimationConfigs();
        if (configs.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<String> suggestions = new ArrayList<>(configs.size() * 2);
        java.util.LinkedHashSet<String> seenTypes = new java.util.LinkedHashSet<>();
        for (int i = 0; i < configs.size(); i++) {
            suggestions.add(String.valueOf(i + 1));
            String type = configs.get(i).getType();
            if (type != null && !type.isEmpty()) {
                seenTypes.add(type.toLowerCase(Locale.ENGLISH));
            }
        }
        suggestions.addAll(seenTypes);
        if (partial == null || partial.isEmpty()) {
            return suggestions;
        }
        String prefix = partial.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>(suggestions.size());
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }
}
