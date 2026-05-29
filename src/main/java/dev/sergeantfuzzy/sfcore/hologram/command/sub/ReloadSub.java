package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.HoloMessages;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo reload} — reloads systems/holograms.yml and re-spawns. */
public final class ReloadSub implements HoloSubCommand {

    private final HoloContext ctx;

    public ReloadSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "sfcore.holo.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long start = System.nanoTime();
        ctx.service().reload();
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        sender.sendMessage(HoloMessages.inline(String.format("§areloaded in §f%.2fms§a • backend: §f%s",
                ms, ctx.service().getBackend() == null ? "dormant" : ctx.service().getBackend().describe())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
