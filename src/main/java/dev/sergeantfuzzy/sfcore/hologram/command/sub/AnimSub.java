package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationConfig;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationRegistry;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
                sender.sendMessage("§6§lAnimations §8› §f" + hologram.getId().value());
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
                    sender.sendMessage("§6/holo anim §f<id> remove §7<index>");
                    return true;
                }
                int index = ctx.parseLineIndex(args[2]);
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
        return java.util.Collections.emptyList();
    }
}
