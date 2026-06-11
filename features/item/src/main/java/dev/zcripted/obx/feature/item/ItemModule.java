package dev.zcripted.obx.feature.item;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.item.command.AnvilCommand;
import dev.zcripted.obx.feature.item.command.BookCommand;
import dev.zcripted.obx.feature.item.command.ClearInvCommand;
import dev.zcripted.obx.feature.item.command.CraftCommand;
import dev.zcripted.obx.feature.item.command.DisposalCommand;
import dev.zcripted.obx.feature.item.command.EnchantCommand;
import dev.zcripted.obx.feature.item.command.EnderchestCommand;
import dev.zcripted.obx.feature.item.command.GiveCommand;
import dev.zcripted.obx.feature.item.command.HatCommand;
import dev.zcripted.obx.feature.item.command.ItemCommand;
import dev.zcripted.obx.feature.item.command.ItemLoreCommand;
import dev.zcripted.obx.feature.item.command.ItemNameCommand;
import dev.zcripted.obx.feature.item.command.MapCommand;
import dev.zcripted.obx.feature.item.command.MoreCommand;
import dev.zcripted.obx.feature.item.command.RepairCommand;
import dev.zcripted.obx.feature.item.command.ResearchCommand;
import dev.zcripted.obx.feature.item.command.SkullCommand;
import dev.zcripted.obx.feature.item.command.SmithCommand;
import dev.zcripted.obx.feature.item.command.UnbreakableCommand;
import dev.zcripted.obx.feature.item.command.VirtualStationCommand;

/**
 * Item / inventory utility commands. Stateless commands that operate directly on
 * the player's inventory or open vanilla crafting/utility stations.
 */
public final class ItemModule extends AbstractModule {

    @Override
    public String id() {
        return "item";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        command("craft", new CraftCommand(plugin));
        command("research", new ResearchCommand(plugin));
        command("anvil", new AnvilCommand(plugin));
        command("enchant", new EnchantCommand(plugin));
        command("smith", new SmithCommand(plugin));
        command("stonecut", new VirtualStationCommand(plugin, VirtualStationCommand.Station.STONECUTTER));
        command("loom", new VirtualStationCommand(plugin, VirtualStationCommand.Station.LOOM));
        command("grindstone", new VirtualStationCommand(plugin, VirtualStationCommand.Station.GRINDSTONE));
        command("cartography", new VirtualStationCommand(plugin, VirtualStationCommand.Station.CARTOGRAPHY));
        command("map", new MapCommand(plugin));
        command("enderchest", new EnderchestCommand(plugin));
        command("disposal", new DisposalCommand(plugin));
        command("hat", new HatCommand(plugin));
        command("clearinv", new ClearInvCommand(plugin));
        command("repair", new RepairCommand(plugin));
        command("more", new MoreCommand(plugin));
        command("skull", new SkullCommand(plugin));
        command("itemname", new ItemNameCommand(plugin));
        command("itemlore", new ItemLoreCommand(plugin));
        command("unbreakable", new UnbreakableCommand(plugin));
        command("give", new GiveCommand(plugin));
        command("i", new ItemCommand(plugin));
        command("book", new BookCommand(plugin));
    }
}