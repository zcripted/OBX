package dev.sergeantfuzzy.sfcore.hologram.command;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.AimGuiSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.AlignmentSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.AnimSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.BackgroundSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.BillboardSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.BoardSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.CopySub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.CreateSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.DeleteSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.DisableSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.DoubleSidedSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.EnableSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.GuiSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.HideSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ShowSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ViewSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.InfoSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.InteractSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.LineSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ListSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.MoveHereSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.MoveSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.PageSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ReloadSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ScaleSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.SeethroughSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ShadowSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.ShowRangeSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.TextAlphaSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.TpSub;
import dev.sergeantfuzzy.sfcore.hologram.command.sub.UpdateRangeSub;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramId;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramLine;
import dev.sergeantfuzzy.sfcore.hologram.service.HologramService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root dispatcher for {@code /holo}. Registers every {@link HoloSubCommand}
 * at construction time and routes args[0] to the matching subcommand,
 * delegating tab-completion the same way. Falls back to a help banner when
 * no subcommand matches.
 *
 * <p>Keeps the {@code debug} subcommand from Phase 1 (handy for quick smoke
 * tests after a deploy). All other subcommands live as their own classes
 * under {@code command/sub/}.
 */
public final class HologramCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final HologramService service;
    private final HoloContext ctx;
    private final Map<String, HoloSubCommand> subs = new LinkedHashMap<>();

    public HologramCommand(Main plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
        this.ctx = new HoloContext(plugin, service);
        registerSubs();
    }

    private void registerSubs() {
        register(new CreateSub(ctx));
        register(new DeleteSub(ctx));
        register(new ListSub(ctx));
        register(new InfoSub(ctx));
        register(new TpSub(ctx));
        register(new MoveSub(ctx));
        register(new MoveHereSub(ctx));
        register(new CopySub(ctx));
        register(new LineSub(ctx));
        register(new ScaleSub(ctx));
        register(new BillboardSub(ctx));
        register(new BackgroundSub(ctx));
        register(new TextAlphaSub(ctx));
        register(new ShadowSub(ctx));
        register(new AlignmentSub(ctx));
        register(new SeethroughSub(ctx));
        register(new DoubleSidedSub(ctx));
        register(new ShowRangeSub(ctx));
        register(new UpdateRangeSub(ctx));
        register(new BoardSub(ctx));
        register(new AimGuiSub(ctx));
        register(new PageSub(ctx));
        register(new InteractSub(ctx));
        register(new AnimSub(ctx));
        register(new ViewSub(ctx));
        register(new HideSub(ctx));
        register(new ShowSub(ctx));
        // GUI editor menu is owned by Main; we wire it in via late binding
        // because the listener also needs to be registered exactly once.
        dev.sergeantfuzzy.sfcore.hologram.gui.HologramEditorMenu editorMenu = plugin.getHologramEditorMenu();
        if (editorMenu != null) {
            register(new GuiSub(ctx, editorMenu));
        }
        register(new EnableSub(ctx));
        register(new DisableSub(ctx));
        register(new ReloadSub(ctx));
    }

    private void register(HoloSubCommand sub) {
        subs.put(sub.name().toLowerCase(Locale.ENGLISH), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.holo.use")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (service == null) {
            plugin.getLanguageManager().send(sender, "hologram.error.module_disabled");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String name = args[0].toLowerCase(Locale.ENGLISH);
        if ("debug".equals(name)) {
            return handleDebug(sender, args);
        }
        if ("help".equals(name) || "?".equals(name)) {
            sendHelp(sender);
            return true;
        }
        HoloSubCommand sub = subs.get(name);
        if (sub == null) {
            sendHelp(sender);
            return true;
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (!service.isActive() && !"enable".equals(name) && !"reload".equals(name)) {
            plugin.getLanguageManager().send(sender, "hologram.error.module_disabled");
            return true;
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return sub.execute(sender, rest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sfcore.holo.use")) {
            return Collections.emptyList();
        }
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ENGLISH);
            List<String> out = new ArrayList<>();
            out.add("debug");
            out.add("help");
            for (HoloSubCommand sub : subs.values()) {
                if (sub.permission() == null || sender.hasPermission(sub.permission())) {
                    out.add(sub.name());
                }
            }
            List<String> filtered = new ArrayList<>();
            for (String option : out) {
                if (option.startsWith(prefix)) {
                    filtered.add(option);
                }
            }
            return filtered;
        }
        HoloSubCommand sub = subs.get(args[0].toLowerCase(Locale.ENGLISH));
        if (sub == null) {
            return Collections.emptyList();
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return sub.tabComplete(sender, rest);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(dev.sergeantfuzzy.sfcore.hologram.HoloMessages.header("subcommands"));
        sender.sendMessage(dev.sergeantfuzzy.sfcore.hologram.HoloMessages.DIVIDER);
        sender.sendMessage("§e/holo create §f<id> §7[text]   §8• new hologram");
        sender.sendMessage("§e/holo delete §f<id>            §8• remove a hologram");
        sender.sendMessage("§e/holo list                     §8• list all");
        sender.sendMessage("§e/holo info §f[id]              §8• module info or single hologram");
        sender.sendMessage("§e/holo tp §f<id>                §8• teleport to");
        sender.sendMessage("§e/holo move(here) §f<id>        §8• reposition");
        sender.sendMessage("§e/holo copy §f<src> <dest>      §8• duplicate");
        sender.sendMessage("§e/holo line §7<add|set|remove|insertbefore|insertafter|swap> §f<id> …");
        sender.sendMessage("§e/holo scale | billboard | alignment | background | textalpha");
        sender.sendMessage("§e/holo shadow | seethrough | doublesided | showrange | updaterange");
        sender.sendMessage("§e/holo board | aim              §8• board/aim utilities");
        sender.sendMessage("§e/holo enable | disable | reload §8• admin (sfcore.holo.admin)");
    }

    /** Phase 1 debug helper kept for smoke tests. */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("sfcore.holo.admin")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        String rawName = args.length >= 2 ? args[1] : "debug";
        HologramId id = HologramId.parse(rawName);
        if (id == null) {
            plugin.getLanguageManager().send(sender, "hologram.error.invalid_id");
            return true;
        }
        if (service.getRegistry().contains(id)) {
            Hologram existing = service.getRegistry().get(id);
            service.getBackend().destroy(existing);
            service.getRegistry().unregister(id);
            service.getStorage().delete(id);
            Map<String, String> reps = new HashMap<>();
            reps.put("name", id.value());
            plugin.getLanguageManager().send(sender, "hologram.debug.removed", reps);
            return true;
        }
        Location loc = player.getLocation().clone().add(0.0, 2.5, 0.0);
        Hologram hologram = new Hologram(id, loc);
        hologram.addLine(HologramLine.text("&6&l✦ &e&lSF-Core Holograms &6&l✦"));
        hologram.addLine(HologramLine.text("&7Backend: &f" + service.getBackend().describe()));
        hologram.addLine(HologramLine.text("&7Right-click to interact &8(Phase 4)"));
        hologram.addLine(HologramLine.icon(new ItemStack(Material.NETHER_STAR)));
        hologram.addLine(HologramLine.text("&aDebug hologram &7• &f" + id.value()));
        service.getRegistry().register(hologram);
        service.getStorage().save(hologram);
        service.getBackend().spawn(hologram, Collections.singletonList(player));
        service.getRegistry().rebuildEntityIndex(hologram);
        Map<String, String> reps = new HashMap<>();
        reps.put("name", id.value());
        plugin.getLanguageManager().send(sender, "hologram.debug.spawned", reps);
        return true;
    }
}
