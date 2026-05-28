package dev.sergeantfuzzy.sfcore.enchant.effect;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import dev.sergeantfuzzy.sfcore.enchant.storage.EnchantStorage;
import dev.sergeantfuzzy.sfcore.enchant.util.Particles;
import dev.sergeantfuzzy.sfcore.enchant.util.Potions;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Drives the passive Arcanum effects that aren't event-triggered: worn-armor
 * potion buffs (Hermes, Nightvision, Aquatic, Curse of the Sleepless), Auto-Repair,
 * Curse of Hunger's faster drain, XP Magnet, Pack Mule's item magnet, Greenthumb's
 * crop growth, Glide Boost, and re-arming Wind Step's double-jump on the ground.
 *
 * <p>Runs once per second. All work is per-online-player and bounded; effect
 * lookups short-circuit when the module is disabled.
 */
public final class EnchantTickTask implements Runnable {

    private static final int PERIOD_TICKS = 20;
    private static final Set<String> NEGATIVE_EFFECTS = new HashSet<String>(Arrays.asList(
            "SLOWNESS", "SLOW", "MINING_FATIGUE", "SLOW_DIGGING", "POISON", "WITHER",
            "WEAKNESS", "HUNGER", "NAUSEA", "CONFUSION", "BLINDNESS", "BAD_OMEN",
            "DARKNESS", "UNLUCK", "LEVITATION"));

    private final Main plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final BoundMovement boundMovement;
    private BukkitTask task;

    public EnchantTickTask(Main plugin, BoundMovement boundMovement) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
        this.boundMovement = boundMovement;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        if (!service.isEnabled()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                tickPlayer(player);
            } catch (Throwable ignored) {
                // never let one player's effect break the loop
            }
        }
    }

    private void tickPlayer(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack boots = player.getInventory().getBoots();
        ItemStack held = EffectUtil.mainHand(player);

        // Movement speed (Hermes).
        int hermes = storage.level(boots, "hermes");
        if (hermes > 0) {
            Potions.apply(player, Potions.SPEED, 60, hermes >= 3 ? 1 : 0);
        }

        // Night vision (helmet).
        if (storage.level(helmet, "nightvision") > 0) {
            Potions.apply(player, Potions.NIGHT_VISION, 260, 0);
        }

        // Aquatic (helmet).
        int aquatic = storage.level(helmet, "aquatic");
        if (aquatic > 0 && inWater(player)) {
            Potions.apply(player, Potions.WATER_BREATHING, 60, 0);
            CustomEnchant e = service.getRegistry().get("aquatic");
            if (e.levelBoolean(aquatic, "night_vision", false)) {
                Potions.apply(player, Potions.NIGHT_VISION, 260, 0);
            }
            if (e.levelBoolean(aquatic, "unlimited_breath", false)) {
                player.setRemainingAir(player.getMaximumAir());
            }
        }

        // Curse of the Sleepless (helmet) — Speed + Night Vision.
        int sleepless = EffectUtil.armorLevel(storage, player, "curse_of_the_sleepless");
        if (sleepless > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_the_sleepless");
            Potions.applyLevel(player, Potions.SPEED, 60, e.levelInt(sleepless, "speed_amp", 1));
            if (e.levelBoolean(sleepless, "night_vision", false)) {
                Potions.apply(player, Potions.NIGHT_VISION, 260, 0);
            }
        }

        // Hex Ward — shorten active harmful potion effects.
        int hexWard = EffectUtil.armorLevel(storage, player, "hex_ward");
        if (hexWard > 0) {
            double reduction = service.getRegistry().get("hex_ward").levelDouble(hexWard, "reduction", 0.0);
            int extra = (int) (reduction * 20);
            if (extra > 0) {
                shortenNegatives(player, extra);
            }
        }

        // Geologist — outline nearby ores with particles while holding the pickaxe.
        int geologist = storage.level(held, "geologist");
        if (geologist > 0) {
            CustomEnchant e = service.getRegistry().get("geologist");
            revealOres(player, e.levelDouble(geologist, "radius", 6.0), e.levelBoolean(geologist, "include_rare", false));
        }

        // Curse of Hunger — faster exhaustion.
        int hunger = EffectUtil.armorLevel(storage, player, "curse_of_hunger");
        if (hunger > 0) {
            double mult = service.getRegistry().get("curse_of_hunger").levelDouble(hunger, "hunger_multiplier", 2.0);
            player.setExhaustion(player.getExhaustion() + (float) (0.2 * (mult - 1.0)));
        }

        // Auto-Repair — probabilistic so it averages 1 durability per interval.
        autoRepair(player);

        // XP Magnet (held tool).
        int magnet = storage.level(held, "xp_magnet");
        if (magnet > 0) {
            double radius = service.getRegistry().get("xp_magnet").levelDouble(magnet, "radius", 4.0);
            pull(player, radius, "EXPERIENCE_ORB");
        }

        // Pack Mule (chest/leggings) — item magnet.
        int packMule = Math.max(storage.level(player.getInventory().getChestplate(), "pack_mule"),
                storage.level(player.getInventory().getLeggings(), "pack_mule"));
        if (packMule > 0) {
            double radius = service.getRegistry().get("pack_mule").levelDouble(packMule, "pickup_radius", 2.0);
            if (service.getRegistry().get("pack_mule").levelBoolean(packMule, "magnet", false)) {
                pull(player, radius, "DROPPED_ITEM", "ITEM");
            }
        }

        // Greenthumb (held hoe/seed) — sampled crop growth.
        int greenthumb = storage.level(held, "greenthumb");
        if (greenthumb > 0) {
            CustomEnchant e = service.getRegistry().get("greenthumb");
            growNearbyCrops(player, e.levelDouble(greenthumb, "radius", 4.0), e.levelDouble(greenthumb, "instant_chance", 0.0));
        }

        // Glide Boost (elytra, gliding).
        if (isGliding(player)) {
            int glide = storage.level(player.getInventory().getChestplate(), "glide_boost");
            if (glide > 0) {
                double speed = service.getRegistry().get("glide_boost").levelDouble(glide, "speed", 0.1);
                Vector dir = player.getLocation().getDirection().normalize().multiply(speed);
                player.setVelocity(player.getVelocity().add(dir));
            }
        }

        // Wind Step — re-arm the double jump while grounded.
        if (storage.level(boots, "wind_step") > 0) {
            String mode = player.getGameMode().name();
            if (player.isOnGround() && !player.isFlying() && (mode.equals("SURVIVAL") || mode.equals("ADVENTURE"))) {
                player.setAllowFlight(true);
            }
        }

        // Curse of the Bound — maintain the real armor-toughness attribute (cleared when not worn).
        int boundBoots = storage.level(boots, "curse_of_the_bound");
        double toughness = boundBoots > 0
                ? service.getRegistry().get("curse_of_the_bound").levelInt(boundBoots, "armor_toughness", 0)
                : 0.0;
        EffectUtil.setBoundToughness(player, toughness);

        // Safety net for the no-sprint throttle (catches fly-state changes / unequips).
        boundMovement.enforce(player);
    }

    private void autoRepair(Player player) {
        for (ItemStack item : allGear(player)) {
            int level = storage.level(item, "auto_repair");
            if (level <= 0 || item.getType().getMaxDurability() <= 0) {
                continue;
            }
            int interval = service.getRegistry().get("auto_repair").levelInt(level, "interval_seconds", 30);
            if (interval <= 0 || Math.random() < 1.0 / interval) {
                repairOne(item);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void repairOne(ItemStack item) {
        short durability = item.getDurability();
        if (durability > 0) {
            item.setDurability((short) (durability - 1));
        }
    }

    private java.util.List<ItemStack> allGear(Player player) {
        java.util.List<ItemStack> list = new java.util.ArrayList<ItemStack>();
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                list.add(armor);
            }
        }
        ItemStack held = EffectUtil.mainHand(player);
        if (held != null) {
            list.add(held);
        }
        return list;
    }

    private void pull(Player player, double radius, String... typeNames) {
        Location loc = player.getLocation();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            String type = entity.getType().name();
            boolean match = false;
            for (String wanted : typeNames) {
                if (type.equals(wanted)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            Vector toPlayer = loc.toVector().subtract(entity.getLocation().toVector());
            if (toPlayer.lengthSquared() > 0.5) {
                entity.setVelocity(toPlayer.normalize().multiply(0.4));
            }
        }
    }

    private void growNearbyCrops(Player player, double radius, double chance) {
        int r = (int) Math.min(8, radius);
        Location base = player.getLocation();
        for (int sample = 0; sample < 12; sample++) {
            int dx = (int) ((Math.random() * (2 * r + 1)) - r);
            int dy = (int) ((Math.random() * 3) - 1);
            int dz = (int) ((Math.random() * (2 * r + 1)) - r);
            try {
                org.bukkit.block.Block block = base.getBlock().getRelative(dx, dy, dz);
                growBlock(block, chance > 0 && Math.random() < chance);
            } catch (Throwable ignored) {
            }
        }
    }

    private void growBlock(org.bukkit.block.Block block, boolean instant) {
        try {
            Object data = block.getClass().getMethod("getBlockData").invoke(block);
            Class<?> ageable = Class.forName("org.bukkit.block.data.Ageable");
            if (!ageable.isInstance(data)) {
                return;
            }
            int age = (Integer) ageable.getMethod("getAge").invoke(data);
            int max = (Integer) ageable.getMethod("getMaximumAge").invoke(data);
            if (age >= max) {
                return;
            }
            int newAge = instant ? max : Math.min(max, age + 1);
            ageable.getMethod("setAge", int.class).invoke(data, newAge);
            Class<?> blockData = Class.forName("org.bukkit.block.data.BlockData");
            block.getClass().getMethod("setBlockData", blockData).invoke(block, data);
        } catch (Throwable ignored) {
            // Pre-1.13 (no BlockData) — Greenthumb growth is unavailable there.
        }
    }

    private void shortenNegatives(Player player, int extraTicks) {
        for (PotionEffect effect : new java.util.ArrayList<PotionEffect>(player.getActivePotionEffects())) {
            if (!NEGATIVE_EFFECTS.contains(effect.getType().getName())) {
                continue;
            }
            int duration = effect.getDuration();
            if (duration <= extraTicks + 5) {
                player.removePotionEffect(effect.getType());
            } else {
                player.removePotionEffect(effect.getType());
                player.addPotionEffect(new PotionEffect(effect.getType(), duration - extraTicks,
                        effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
            }
        }
    }

    private void revealOres(Player player, double radius, boolean includeRare) {
        int r = (int) Math.min(10, radius);
        Block base = player.getLocation().getBlock();
        for (int sample = 0; sample < 30; sample++) {
            int dx = (int) ((Math.random() * (2 * r + 1)) - r);
            int dy = (int) ((Math.random() * (2 * r + 1)) - r);
            int dz = (int) ((Math.random() * (2 * r + 1)) - r);
            Block block = base.getRelative(dx, dy, dz);
            String name = block.getType().name();
            boolean ore = name.endsWith("_ORE");
            boolean rare = name.contains("ANCIENT_DEBRIS") || name.contains("DIAMOND") || name.contains("EMERALD");
            if (ore || (includeRare && rare)) {
                if (!rare || includeRare) {
                    Particles.at(block.getLocation().add(0.5, 0.5, 0.5), Particles.HAPPY, 3, 0.1);
                }
            }
        }
    }

    private boolean inWater(Player player) {
        String eye = player.getEyeLocation().getBlock().getType().name();
        return eye.contains("WATER");
    }

    private boolean isGliding(Player player) {
        try {
            return player.isGliding();
        } catch (Throwable legacy) {
            return false;
        }
    }
}
