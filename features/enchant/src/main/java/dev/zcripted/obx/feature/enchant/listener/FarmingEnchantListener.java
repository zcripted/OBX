package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.EffectUtil;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.EnchantRarity;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Farming-category effects: Bountiful Yield (extra crop drops), Harvest Wave
 * (area harvest + replant), Angler's Luck (better fishing loot), and Beastmaster
 * (bonus shear yield). Greenthumb's passive crop growth runs in the tick task.
 */
public final class FarmingEnchantListener implements Listener {

    private final EnchantService service;
    private final EnchantStorage storage;
    private final EnchantItems items;
    private final Set<UUID> processing = new HashSet<UUID>();

    public FarmingEnchantListener(ObxPlugin plugin) {
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.items = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.item.EnchantItems.class);
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
        Block origin = event.getBlock();
        if (!isCrop(origin.getType())) {
            return;
        }
        ItemStack tool = EffectUtil.mainHand(player);

        int harvest = storage.level(tool, "harvest_wave");
        if (harvest > 0) {
            CustomEnchant e = service.getRegistry().get("harvest_wave");
            int size = e.levelInt(harvest, "size", 3);
            boolean replant = e.levelBoolean(harvest, "replant", false);
            boolean autoCollect = e.levelBoolean(harvest, "auto_collect", false);
            harvestArea(player, origin, tool, size, replant, autoCollect);
        }

        int bountiful = storage.level(tool, "bountiful_yield");
        if (bountiful > 0 && isMature(origin)) {
            CustomEnchant e = service.getRegistry().get("bountiful_yield");
            if (Math.random() < e.levelDouble(bountiful, "chance", 0.0)) {
                int extra = e.levelInt(bountiful, "extra", 1);
                for (ItemStack drop : origin.getDrops(tool)) {
                    for (int i = 0; i < extra; i++) {
                        origin.getWorld().dropItemNaturally(origin.getLocation(), drop.clone());
                    }
                }
            }
        }
    }

    private void harvestArea(Player player, Block origin, ItemStack tool, int size, boolean replant, boolean autoCollect) {
        int radius = Math.max(1, size / 2);
        processing.add(player.getUniqueId());
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    Block block = origin.getRelative(dx, 0, dz);
                    if (!isCrop(block.getType()) || !isMature(block)) {
                        continue;
                    }
                    Material cropType = block.getType();
                    Collection<ItemStack> drops = block.getDrops(tool);
                    block.setType(Material.AIR);
                    for (ItemStack drop : drops) {
                        if (autoCollect) {
                            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
                            for (ItemStack leftover : overflow.values()) {
                                block.getWorld().dropItemNaturally(block.getLocation(), leftover);
                            }
                        } else {
                            block.getWorld().dropItemNaturally(block.getLocation(), drop);
                        }
                    }
                    if (replant) {
                        block.setType(cropType); // replanted at age 0
                    }
                }
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onComposter(PlayerInteractEvent event) {
        if (!service.isEnabled() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!isCrop(block.getType()) || isMature(block)) {
            return;
        }
        Player player = event.getPlayer();
        int level = storage.level(EffectUtil.mainHand(player), "composter");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("composter");
        boolean instant = e.levelDouble(level, "double_tier_chance", 0.0) > 0
                && Math.random() < e.levelDouble(level, "double_tier_chance", 0.0);
        if (grow(block, instant)) {
            event.setCancelled(true);
            Particles.at(block.getLocation().add(0.5, 0.5, 0.5), Particles.HAPPY, 8, 0.3);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!service.isEnabled() || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        int level = storage.level(EffectUtil.mainHand(player), "anglers_luck");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("anglers_luck");
        if (e.levelDouble(level, "scroll_chance", 0.0) > 0 && Math.random() < e.levelDouble(level, "scroll_chance", 0.0)) {
            // Replace the catch entity's item with a random scroll.
            if (event.getCaught() instanceof org.bukkit.entity.Item) {
                CustomEnchant random = randomEnchant();
                if (random != null) {
                    ((org.bukkit.entity.Item) event.getCaught()).setItemStack(items.scroll(random, 1, 1));
                    return;
                }
            }
        }
        if (Math.random() < e.levelDouble(level, "treasure_bonus", 0.0)) {
            if (event.getCaught() instanceof org.bukkit.entity.Item) {
                Material treasure = treasure();
                if (treasure != null) {
                    ((org.bukkit.entity.Item) event.getCaught()).setItemStack(new ItemStack(treasure));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) {
            return;
        }
        Player player = event.getPlayer();
        int level = storage.level(EffectUtil.mainHand(player), "beastmaster");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("beastmaster");
        int bonus = e.levelInt(level, "wool_bonus", 1);
        Material wool = Material.matchMaterial("WHITE_WOOL");
        if (wool == null) {
            wool = Material.matchMaterial("WOOL");
        }
        if (wool != null && bonus > 0) {
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(wool, bonus));
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isCrop(Material material) {
        String name = material.name();
        return name.equals("WHEAT") || name.equals("CROPS") || name.equals("CARROTS") || name.equals("CARROT")
                || name.equals("POTATOES") || name.equals("POTATO") || name.equals("BEETROOTS") || name.equals("BEETROOT_BLOCK")
                || name.equals("NETHER_WART") || name.equals("NETHER_WARTS") || name.equals("COCOA");
    }

    @SuppressWarnings("deprecation")
    private boolean isMature(Block block) {
        try {
            Object data = block.getClass().getMethod("getBlockData").invoke(block);
            Class<?> ageable = Class.forName("org.bukkit.block.data.Ageable");
            if (ageable.isInstance(data)) {
                int age = (Integer) ageable.getMethod("getAge").invoke(data);
                int max = (Integer) ageable.getMethod("getMaximumAge").invoke(data);
                return age >= max;
            }
            return true;
        } catch (Throwable legacy) {
            try {
                return block.getData() >= 7;
            } catch (Throwable ignored) {
                return true;
            }
        }
    }

    /** Advances a crop's growth one stage (or to full if {@code instant}). Returns true if it changed. */
    private boolean grow(Block block, boolean instant) {
        try {
            Object data = block.getClass().getMethod("getBlockData").invoke(block);
            Class<?> ageable = Class.forName("org.bukkit.block.data.Ageable");
            if (!ageable.isInstance(data)) {
                return false;
            }
            int age = (Integer) ageable.getMethod("getAge").invoke(data);
            int max = (Integer) ageable.getMethod("getMaximumAge").invoke(data);
            if (age >= max) {
                return false;
            }
            int newAge = instant ? max : Math.min(max, age + 1);
            ageable.getMethod("setAge", int.class).invoke(data, newAge);
            Class<?> blockData = Class.forName("org.bukkit.block.data.BlockData");
            block.getClass().getMethod("setBlockData", blockData).invoke(block, data);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private CustomEnchant randomEnchant() {
        java.util.List<CustomEnchant> all = service.getRegistry().all();
        return all.isEmpty() ? null : all.get((int) (Math.random() * all.size()));
    }

    private Material treasure() {
        String[] options = {"ENCHANTED_BOOK", "NAME_TAG", "NAUTILUS_SHELL", "SADDLE", "BOW"};
        for (int i = 0; i < 6; i++) {
            Material material = Material.matchMaterial(options[(int) (Math.random() * options.length)]);
            if (material != null) {
                return material;
            }
        }
        return Material.matchMaterial("DIAMOND");
    }
}
