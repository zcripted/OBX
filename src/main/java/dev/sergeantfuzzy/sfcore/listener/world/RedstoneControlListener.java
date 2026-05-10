package dev.sergeantfuzzy.sfcore.listener.world;

import dev.sergeantfuzzy.sfcore.util.control.ServerControlState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RedstoneControlListener implements Listener {

    private static final Set<String> REDSTONE_NAMES = new HashSet<>(Arrays.asList(
            "REDSTONE", "REDSTONE_WIRE", "REDSTONE_TORCH", "REDSTONE_TORCH_ON", "REDSTONE_TORCH_OFF", "REDSTONE_WALL_TORCH",
            "DIODE", "DIODE_BLOCK_OFF", "DIODE_BLOCK_ON", "REPEATER", "COMPARATOR",
            "PISTON", "PISTON_BASE", "STICKY_PISTON", "PISTON_STICKY_BASE", "PISTON_MOVING_PIECE",
            "OBSERVER", "DISPENSER", "DROPPER", "HOPPER",
            "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL",
            "REDSTONE_LAMP", "REDSTONE_LAMP_ON", "TARGET", "NOTE_BLOCK", "DAYLIGHT_DETECTOR"
    ));

    @EventHandler(ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!ServerControlState.isRedstoneFrozen()) {
            return;
        }
        event.setNewCurrent(event.getOldCurrent());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (!ServerControlState.isRedstoneFrozen()) {
            return;
        }
        Block block = event.getBlock();
        if (block != null && isRedstoneBlock(block.getType())) {
            event.setCancelled(true);
        }
    }

    private boolean isRedstoneBlock(Material material) {
        if (material == null) {
            return false;
        }
        return REDSTONE_NAMES.contains(material.name());
    }
}
