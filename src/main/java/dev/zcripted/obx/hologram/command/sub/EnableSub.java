package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code /holo enable} — flips the master flag, persists, runs reload(). */
public final class EnableSub implements HoloSubCommand {

    private final HoloContext ctx;

    public EnableSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "enable";
    }

    @Override
    public String permission() {
        return "obx.holo.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (ctx.service().getConfig() == null) {
            ctx.msg(sender, "hologram.error.module_disabled");
            return true;
        }
        ctx.service().getConfig().set("enabled", true);
        ctx.service().save();
        ctx.service().reload();
        Map<String, String> reps = new HashMap<>();
        reps.put("backend", ctx.service().getBackend() == null ? "?" : ctx.service().getBackend().describe());
        ctx.plugin().getLanguageManager().send(sender, "hologram.module.enabled", reps);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
