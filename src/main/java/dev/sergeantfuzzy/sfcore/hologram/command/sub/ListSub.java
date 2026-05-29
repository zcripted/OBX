package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** {@code /holo list} — prints every registered hologram. */
public final class ListSub implements HoloSubCommand {

    private final HoloContext ctx;

    public ListSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return "sfcore.holo.list";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<Hologram> all = ctx.service().getRegistry().all();
        sender.sendMessage("§6§lSF-Core Holograms §8› §7" + all.size() + " loaded");
        sender.sendMessage("§8──────────────────────────────");
        if (all.isEmpty()) {
            sender.sendMessage("§7  (none — use §f/holo create §7to make one)");
            return true;
        }
        List<Hologram> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> a.getId().value().compareTo(b.getId().value()));
        for (Hologram hologram : sorted) {
            Location loc = hologram.getLocation();
            String world = loc.getWorld() == null ? "?" : loc.getWorld().getName();
            sender.sendMessage(String.format("§e› §f%s §8• §7%s @ %.1f, %.1f, %.1f §8• §7%d line(s)",
                    hologram.getId().value(), world, loc.getX(), loc.getY(), loc.getZ(),
                    hologram.getLines().size()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
