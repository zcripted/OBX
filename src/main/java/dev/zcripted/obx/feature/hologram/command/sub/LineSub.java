package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Umbrella dispatcher for line-editing subcommands. Routes
 * {@code /holo line <action> <id> …} to the right per-action class:
 * <ul>
 *   <li>{@code add}        → {@link AddLineSub}</li>
 *   <li>{@code set}        → {@link SetLineSub}</li>
 *   <li>{@code remove}     → {@link RemoveLineSub} (alias {@code delete})</li>
 *   <li>{@code insertbefore} → {@link InsertBeforeSub} (alias {@code before})</li>
 *   <li>{@code insertafter}  → {@link InsertAfterSub}  (alias {@code after})</li>
 *   <li>{@code swap}       → {@link SwapLineSub}</li>
 * </ul>
 *
 * <p>Each routed subcommand is constructed once and held on this instance.
 * Permission is the most permissive of its actions
 * ({@code obx.holo.edit}); each routed class still re-checks its own
 * permission inside {@code execute}.
 */
public final class LineSub implements HoloSubCommand {

    private final HoloContext ctx;
    private final Map<String, HoloSubCommand> actions = new LinkedHashMap<>();

    public LineSub(HoloContext ctx) {
        this.ctx = ctx;
        AddLineSub add = new AddLineSub(ctx);
        SetLineSub set = new SetLineSub(ctx);
        RemoveLineSub remove = new RemoveLineSub(ctx);
        InsertBeforeSub before = new InsertBeforeSub(ctx);
        InsertAfterSub after = new InsertAfterSub(ctx);
        SwapLineSub swap = new SwapLineSub(ctx);
        actions.put("add", add);
        actions.put("set", set);
        actions.put("remove", remove);
        actions.put("delete", remove);
        actions.put("insertbefore", before);
        actions.put("before", before);
        actions.put("insertafter", after);
        actions.put("after", after);
        actions.put("swap", swap);
    }

    @Override
    public String name() {
        return "line";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§5/holo line §7<add|set|remove|insertbefore|insertafter|swap> §f<id> …");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        HoloSubCommand routed = actions.get(action);
        if (routed == null) {
            ctx.msg(sender, "hologram.error.invalid_value");
            return true;
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return routed.execute(sender, rest);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "set", "remove", "insertbefore", "insertafter", "swap");
        }
        if (args.length >= 2) {
            HoloSubCommand routed = actions.get(args[0].toLowerCase(Locale.ENGLISH));
            if (routed != null) {
                String[] rest = new String[args.length - 1];
                System.arraycopy(args, 1, rest, 0, rest.length);
                return routed.tabComplete(sender, rest);
            }
        }
        return java.util.Collections.emptyList();
    }
}
