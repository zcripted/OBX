package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.EffectUtil;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tools-category effects driven by {@link BlockBreakEvent}: Vein Miner,
 * Treecapitator, Excavator (area breaks), Smelter (auto-smelt drops), and
 * Fortune Strike (chance to double ore drops). Extra blocks are removed directly
 * (no nested BlockBreakEvent), with all drops routed through the same
 * smelt/fortune/auto-collect pipeline. A re-entrancy guard prevents any loops.
 */
public final class ToolEnchantListener implements Listener {

    private static final int HARD_CAP = 256;
    private static final Map<String, String> SMELT = buildSmeltMap();

    private final EnchantService service;
    private final EnchantStorage storage;
    private final LanguageManager languages;
    private final Set<java.util.UUID> processing = new HashSet<java.util.UUID>();

    public ToolEnchantListener(ObxPlugin plugin) {
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.languages = plugin.getLanguageManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (processing.contains(player.getUniqueId())) {
            return;
        }
        ItemStack tool = EffectUtil.mainHand(player);
        if (player.getGameMode().name().equals("CREATIVE")) {
            // Tool enchants are survival-only. Warn (briefly, per break) when a creative
            // player swings a Treecapitator-enchanted tool so the no-op isn't confusing.
            if (storage.level(tool, "treecapitator") > 0) {
                ComponentMessenger.sendActionBar(player, languages.get(player, "enchant.tool.creative-warning"));
            }
            return;
        }
        int vein = storage.level(tool, "vein_miner");
        int tree = storage.level(tool, "treecapitator");
        int excavator = storage.level(tool, "excavator");
        int smelter = storage.level(tool, "smelter");
        int fortune = storage.level(tool, "fortune_strike");
        if (vein <= 0 && tree <= 0 && excavator <= 0 && smelter <= 0 && fortune <= 0) {
            return;
        }

        Block origin = event.getBlock();
        Set<Block> blocks = new LinkedHashSet<Block>();
        boolean autoCollect = false;
        boolean feltTree = false;

        if (vein > 0) {
            CustomEnchant e = service.getRegistry().get("vein_miner");
            int max = Math.min(HARD_CAP, e.levelInt(vein, "max_blocks", 8));
            autoCollect = e.levelBoolean(vein, "auto_collect", false);
            collectMatching(origin, origin.getType(), max, blocks);
        } else if (tree > 0) {
            CustomEnchant e = service.getRegistry().get("treecapitator");
            if (origin.getType().name().endsWith("_LOG") || origin.getType().name().endsWith("_STEM") || origin.getType().name().endsWith("_WOOD")) {
                int max = Math.min(HARD_CAP, e.levelInt(tree, "max_logs", 24));
                collectLogs(origin, max, blocks);
                feltTree = true;
            }
        } else if (excavator > 0) {
            CustomEnchant e = service.getRegistry().get("excavator");
            collectCube(origin, e.levelBoolean(excavator, "cube", false), blocks);
        }

        // The origin is always handled by this pipeline (so smelter/fortune apply to it too).
        blocks.add(origin);

        boolean changed = blocks.size() > 1 || smelter > 0 || fortune > 0;
        if (!changed) {
            return;
        }

        // Treecapitator merges a felled tree's logs into one named pile instead of
        // scattering loose stacks. The combined stacks are accumulated here and
        // emitted as single holographic-named Item entities after the break loop.
        boolean combineLogDrops = feltTree && !autoCollect;
        List<ItemStack> combinedDrops = combineLogDrops ? new ArrayList<ItemStack>() : null;
        Location combineAt = combineLogDrops ? origin.getLocation().add(0.5, 0.5, 0.5) : null;

        event.setDropItems(false);
        processing.add(player.getUniqueId());
        try {
            for (Block block : blocks) {
                if (block.getType() == Material.AIR) {
                    continue;
                }
                Collection<ItemStack> drops = block.getDrops(tool);
                boolean isOre = block.getType().name().contains("ORE");
                java.util.List<ItemStack> finalDrops = new java.util.ArrayList<ItemStack>();
                for (ItemStack drop : drops) {
                    ItemStack out = smelter > 0 ? smelt(drop) : drop;
                    finalDrops.add(out);
                    if (fortune > 0 && isOre) {
                        double chance = service.getRegistry().get("fortune_strike").levelDouble(fortune, "chance", 0.0);
                        if (Math.random() < chance) {
                            finalDrops.add(out.clone());
                        }
                    }
                }
                block.setType(Material.AIR);
                for (ItemStack out : finalDrops) {
                    if (combineLogDrops) {
                        mergeDrop(combinedDrops, out);
                    } else if (autoCollect) {
                        Map<Integer, ItemStack> overflow = player.getInventory().addItem(out);
                        for (ItemStack leftover : overflow.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), leftover);
                        }
                    } else {
                        block.getWorld().dropItemNaturally(block.getLocation(), out);
                    }
                }
                if (block != origin) {
                    damageTool(player, tool);
                }
            }
            if (combineLogDrops && combinedDrops != null && !combinedDrops.isEmpty()) {
                World world = origin.getWorld();
                if (world != null) {
                    for (ItemStack stack : combinedDrops) {
                        dropCombined(world, combineAt, stack);
                    }
                }
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    /** Adds {@code drop} to {@code combined}, stacking it onto a like item if one is already present. */
    private static void mergeDrop(List<ItemStack> combined, ItemStack drop) {
        if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) {
            return;
        }
        for (ItemStack existing : combined) {
            if (existing.isSimilar(drop)) {
                existing.setAmount(existing.getAmount() + drop.getAmount());
                return;
            }
        }
        combined.add(drop.clone());
    }

    /**
     * Drops {@code stack} as a single Item entity (placed exactly, with no natural
     * scatter) and labels it with a floating "Name xN" holographic name that
     * hovers along with the item. This is what makes a Treecapitator-felled tree
     * land as one named pile rather than many loose stacks.
     */
    private void dropCombined(World world, Location location, ItemStack stack) {
        Item entity;
        try {
            entity = world.dropItem(location, stack);
        } catch (Throwable cannotDrop) {
            world.dropItemNaturally(location, stack);
            return;
        }
        try {
            entity.setCustomName(ChatColor.translateAlternateColorCodes('&',
                    "&e" + prettyName(stack) + " &7x" + stack.getAmount()));
            entity.setCustomNameVisible(true);
        } catch (Throwable ignored) {
            // Naming is cosmetic — never fail the drop over it.
        }
    }

    /** Human-friendly item label: the item's display name, else its title-cased material name (e.g. "Oak Log"). */
    private static String prettyName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta() != null && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }
        String[] words = stack.getType().name().toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.length() == 0 ? stack.getType().name() : name.toString();
    }

    private void collectMatching(Block origin, Material type, int max, Set<Block> out) {
        Deque<Block> queue = new ArrayDeque<Block>();
        queue.add(origin);
        Set<Block> seen = new HashSet<Block>();
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < max) {
            Block current = queue.poll();
            out.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (!seen.contains(next) && next.getType() == type && out.size() + queue.size() < max) {
                            seen.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
    }

    private void collectLogs(Block origin, int max, Set<Block> out) {
        Deque<Block> queue = new ArrayDeque<Block>();
        queue.add(origin);
        Set<Block> seen = new HashSet<Block>();
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < max) {
            Block current = queue.poll();
            out.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block next = current.getRelative(dx, dy, dz);
                        String name = next.getType().name();
                        boolean log = name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD");
                        if (log && !seen.contains(next) && out.size() + queue.size() < max) {
                            seen.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
    }

    private void collectCube(Block origin, boolean cube, Set<Block> out) {
        int yMin = cube ? -1 : 0;
        int yMax = cube ? 1 : 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = yMin; dy <= yMax; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = origin.getRelative(dx, dy, dz);
                    if (isSoft(block.getType())) {
                        out.add(block);
                    }
                }
            }
        }
    }

    private boolean isSoft(Material material) {
        String name = material.name();
        return name.contains("DIRT") || name.contains("SAND") || name.contains("GRAVEL")
                || name.equals("GRASS_BLOCK") || name.contains("CLAY") || name.contains("SOUL_SAND")
                || name.contains("SOUL_SOIL") || name.contains("MUD") || name.contains("SNOW")
                || name.contains("MYCELIUM") || name.contains("PODZOL") || name.contains("COARSE");
    }

    private ItemStack smelt(ItemStack drop) {
        if (drop == null) {
            return drop;
        }
        String to = SMELT.get(drop.getType().name());
        if (to == null) {
            return drop;
        }
        Material material = Material.matchMaterial(to);
        if (material == null) {
            return drop;
        }
        ItemStack out = new ItemStack(material, drop.getAmount());
        return out;
    }

    @SuppressWarnings("deprecation")
    private void damageTool(Player player, ItemStack tool) {
        if (tool == null || tool.getType().getMaxDurability() <= 0) {
            return;
        }
        short next = (short) (tool.getDurability() + 1);
        if (next >= tool.getType().getMaxDurability()) {
            tool.setAmount(0);
        } else {
            tool.setDurability(next);
        }
    }

    private static Map<String, String> buildSmeltMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("RAW_IRON", "IRON_INGOT");
        map.put("RAW_GOLD", "GOLD_INGOT");
        map.put("RAW_COPPER", "COPPER_INGOT");
        map.put("IRON_ORE", "IRON_INGOT");
        map.put("DEEPSLATE_IRON_ORE", "IRON_INGOT");
        map.put("GOLD_ORE", "GOLD_INGOT");
        map.put("DEEPSLATE_GOLD_ORE", "GOLD_INGOT");
        map.put("NETHER_GOLD_ORE", "GOLD_INGOT");
        map.put("COPPER_ORE", "COPPER_INGOT");
        map.put("DEEPSLATE_COPPER_ORE", "COPPER_INGOT");
        map.put("ANCIENT_DEBRIS", "NETHERITE_SCRAP");
        map.put("SAND", "GLASS");
        map.put("RED_SAND", "GLASS");
        map.put("COBBLESTONE", "STONE");
        map.put("CLAY_BALL", "BRICK");
        map.put("NETHERRACK", "NETHER_BRICK");
        map.put("CACTUS", "GREEN_DYE");
        map.put("KELP", "DRIED_KELP");
        return java.util.Collections.unmodifiableMap(map);
    }
}