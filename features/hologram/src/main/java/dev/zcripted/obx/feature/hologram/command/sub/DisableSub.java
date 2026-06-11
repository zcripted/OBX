package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo disable} — flips the master flag, persists, runs reload(). */
public final class DisableSub implements HoloSubCommand {

    private final HoloContext ctx;

    public DisableSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "disable";
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
        ctx.service().getConfig().set("enabled", false);
        ctx.service().save();
        ctx.service().reload();
        ctx.msg(sender, "hologram.module.disabled");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}