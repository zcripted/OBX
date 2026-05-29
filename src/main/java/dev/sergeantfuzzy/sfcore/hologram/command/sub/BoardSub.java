package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.HoloMessages;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramSettings;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /holo board <id> [enable|disable|material <mat>|size <w> [h]|offset <back>]} —
 * Configures the block-display slab backing rendered behind the text lines.
 *
 * <p>No-arg usage prints the current board state for the hologram so operators
 * can confirm a change without alt-tabbing to YAML. Each mutating action
 * commits the new settings via {@link HoloContext#persistAndRefresh} which
 * persists to YAML and respawns the backend entities, so the updated board
 * appears immediately on every viewer.
 */
public final class BoardSub implements HoloSubCommand {

    private final HoloContext ctx;

    public BoardSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "board";
    }

    @Override
    public String permission() {
        return "sfcore.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        HologramSettings settings = hologram.getSettings();
        if (args.length == 1) {
            sendState(sender, hologram, settings);
            return true;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        switch (action) {
            case "enable":
            case "on":
                settings.setBoardEnabled(true);
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§aEnabled board for §f" + hologram.getId().value()));
                return true;
            case "disable":
            case "off":
                settings.setBoardEnabled(false);
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§cDisabled board for §f" + hologram.getId().value()));
                return true;
            case "material":
            case "mat":
            case "block":
                if (args.length < 3) {
                    sender.sendMessage(HoloMessages.inline("§6/holo board §f<id> material §7<MATERIAL>"));
                    return true;
                }
                Material material = Material.matchMaterial(args[2]);
                if (material == null || !material.isBlock()) {
                    sender.sendMessage(HoloMessages.inline("§cUnknown or non-block material: §f" + args[2]));
                    return true;
                }
                settings.setBoardMaterial(material.name());
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline("§aBoard material set to §f" + material.name()));
                return true;
            case "size":
            case "scale":
                if (args.length < 3) {
                    sender.sendMessage(HoloMessages.inline("§6/holo board §f<id> size §7<width> §8[height|auto]"));
                    return true;
                }
                double width;
                try {
                    width = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(HoloMessages.inline("§cWidth must be a number (blocks)."));
                    return true;
                }
                double height = 0.0;
                if (args.length >= 4) {
                    if ("auto".equalsIgnoreCase(args[3])) {
                        height = 0.0;
                    } else {
                        try {
                            height = Double.parseDouble(args[3]);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(HoloMessages.inline("§cHeight must be a number, or 'auto'."));
                            return true;
                        }
                    }
                }
                settings.setBoardWidth(width);
                settings.setBoardHeight(height);
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline(String.format(
                        "§aBoard size set to §f%.2f §7× §f%s §7blocks",
                        settings.getBoardWidth(),
                        settings.getBoardHeight() <= 0.0 ? "auto" : String.format("%.2f", settings.getBoardHeight()))));
                return true;
            case "offset":
            case "back":
                if (args.length < 3) {
                    sender.sendMessage(HoloMessages.inline("§6/holo board §f<id> offset §7<distance>"));
                    return true;
                }
                double back;
                try {
                    back = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(HoloMessages.inline("§cOffset must be a number (blocks behind text)."));
                    return true;
                }
                settings.setBoardOffsetBack(back);
                ctx.persistAndRefresh(hologram);
                sender.sendMessage(HoloMessages.inline(String.format(
                        "§aBoard offset set to §f%.2f §7blocks behind text", settings.getBoardOffsetBack())));
                return true;
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(HoloMessages.header("Board commands"));
        sender.sendMessage(HoloMessages.DIVIDER);
        sender.sendMessage("§e/holo board §f<id>                          §8• show state");
        sender.sendMessage("§e/holo board §f<id> §7enable §8|§7 disable      §8• toggle backing");
        sender.sendMessage("§e/holo board §f<id> §7material §f<MATERIAL>    §8• set block type");
        sender.sendMessage("§e/holo board §f<id> §7size §f<w> §8[h|auto]    §8• resize");
        sender.sendMessage("§e/holo board §f<id> §7offset §f<distance>      §8• depth behind text");
    }

    private void sendState(CommandSender sender, Hologram hologram, HologramSettings settings) {
        sender.sendMessage(HoloMessages.header(hologram.getId().value() + " §8· §7board"));
        sender.sendMessage(HoloMessages.DIVIDER);
        sender.sendMessage(String.format("§7Enabled : §f%s", settings.isBoardEnabled() ? "§ayes" : "§cno"));
        sender.sendMessage(String.format("§7Material: §f%s", settings.getBoardMaterial()));
        sender.sendMessage(String.format("§7Size    : §f%.2f §7× §f%s §7blocks",
                settings.getBoardWidth(),
                settings.getBoardHeight() <= 0.0 ? "auto" : String.format("%.2f", settings.getBoardHeight())));
        sender.sendMessage(String.format("§7Offset  : §f%.2f §7blocks behind text", settings.getBoardOffsetBack()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        if (args.length == 2) {
            return filterPrefix(Arrays.asList("enable", "disable", "material", "size", "offset"), args[1]);
        }
        if (args.length == 3 && "material".equalsIgnoreCase(args[1])) {
            String prefix = args[2].toUpperCase(Locale.ENGLISH);
            List<String> matches = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isBlock()) continue;
                if (material.name().startsWith(prefix)) {
                    matches.add(material.name());
                }
                if (matches.size() >= 24) break;
            }
            return matches;
        }
        if (args.length == 4 && "size".equalsIgnoreCase(args[1])) {
            return Collections.singletonList("auto");
        }
        return Collections.emptyList();
    }

    private static List<String> filterPrefix(List<String> source, String raw) {
        if (raw == null || raw.isEmpty()) return source;
        String lower = raw.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String candidate : source) {
            if (candidate.startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}
