package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.CombatHudService;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.Sounds;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the Combat-category Arcanum effects on hit and on kill. Effects are
 * data-driven: the listener reads the level of each relevant enchantment from
 * the attacker's weapon (or armor, for the cursed melee buffs) via
 * {@link EnchantStorage} and applies the configured per-level parameters.
 *
 * <p>Only fires when the module is enabled; every effect is individually guarded
 * so a missing API on an older server degrades that single effect rather than
 * breaking combat.
 */
public final class CombatEnchantListener implements Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatHudService hud;

    /** Per-attacker Vampiric Edge stacking state. */
    private final Map<UUID, VampiricState> vampiric = new HashMap<UUID, VampiricState>();

    public CombatEnchantListener(ObxPlugin plugin, EnchantService service, CombatHudService hud) {
        this.plugin = plugin;
        this.service = service;
        this.storage = service.getStorage();
        this.hud = hud;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity victim = (LivingEntity) event.getEntity();

        // Victim-side reactive effects (Riposte) — victim is a player.
        if (victim instanceof Player && event.getDamager() instanceof LivingEntity) {
            handleRiposte((Player) victim, (LivingEntity) event.getDamager(), event);
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (victim == attacker) {
            return;
        }
        ItemStack weapon = mainHand(attacker);
        double baseDamage = event.getDamage();
        double finalDamage = event.getFinalDamage();

        // ── Damage modifiers (applied to the event) ──────────────────────────
        double multiplier = 1.0;
        double flatBonus = 0.0;

        int executioner = storage.level(weapon, "executioner");
        if (executioner > 0) {
            CustomEnchant e = service.getRegistry().get("executioner");
            double pct = healthFraction(victim);
            double threshold = e.levelDouble(executioner, "threshold", 0.0);
            double bonus = e.levelDouble(executioner, "bonus", 0.0);
            double threshold2 = e.levelDouble(executioner, "threshold2", 0.0);
            double bonus2 = e.levelDouble(executioner, "bonus2", 0.0);
            if (threshold2 > 0 && pct <= threshold2) {
                multiplier += bonus2;
                debug(attacker, "Executioner: +" + (int) (bonus2 * 100) + "% (sub-" + (int) (threshold2 * 100) + "%)");
            } else if (pct <= threshold) {
                multiplier += bonus;
                debug(attacker, "Executioner: +" + (int) (bonus * 100) + "%");
            }
        }

        int brittleness = storage.level(weapon, "curse_of_brittleness");
        if (brittleness > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_brittleness");
            flatBonus += e.levelInt(brittleness, "effective_bonus", 0);
        }

        int hunger = armorLevel(attacker, "curse_of_hunger");
        if (hunger > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_hunger");
            multiplier += e.levelDouble(hunger, "damage_bonus", 0.0);
        }

        int echoes = armorLevel(attacker, "curse_of_echoes");
        if (echoes > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_echoes");
            // All hits crit: a flat damage bump approximating a crit when applicable.
            if (e.levelBoolean(echoes, "all_crit", false)) {
                multiplier += 0.5;
            } else if (Math.random() < e.levelDouble(echoes, "crit_rate", 0.0)) {
                multiplier += 0.5;
            }
            double reflect = e.levelDouble(echoes, "reflect_percent", 0.0);
            if (reflect > 0) {
                damageSelf(attacker, finalDamage * reflect);
            }
        }

        if (multiplier != 1.0 || flatBonus != 0.0) {
            double newDamage = baseDamage * multiplier + flatBonus;
            event.setDamage(Math.max(0.0, newDamage));
            finalDamage = event.getFinalDamage();
        }

        // ── On-hit proc effects ──────────────────────────────────────────────
        int frostbite = storage.level(weapon, "frostbite");
        if (frostbite > 0) {
            CustomEnchant e = service.getRegistry().get("frostbite");
            if (Math.random() < e.levelDouble(frostbite, "chance", 0.0)) {
                int duration = e.levelInt(frostbite, "duration_ticks", 40);
                Potions.applyLevel(victim, Potions.SLOWNESS, duration, e.levelInt(frostbite, "slowness", 1));
                int fatigue = e.levelInt(frostbite, "fatigue", 0);
                if (fatigue > 0) {
                    Potions.applyLevel(victim, Potions.MINING_FATIGUE, duration, fatigue);
                }
                Particles.at(victim.getEyeLocation(), Particles.CHILL, 10, 0.3);
                debug(attacker, "Frostbite proc");
            }
        }

        int soulfire = storage.level(weapon, "soulfire");
        if (soulfire > 0) {
            CustomEnchant e = service.getRegistry().get("soulfire");
            int seconds = e.levelInt(soulfire, "duration_seconds", 3);
            victim.setFireTicks(Math.max(victim.getFireTicks(), seconds * 20));
            Particles.at(victim.getLocation().add(0, 1, 0), Particles.SOUL_FIRE, 10, 0.3);
            debug(attacker, "Soulfire ignite " + seconds + "s");
        }

        int bloodthirst = storage.level(weapon, "bloodthirst");
        if (bloodthirst > 0) {
            CustomEnchant e = service.getRegistry().get("bloodthirst");
            double percent = e.levelDouble(bloodthirst, "heal_percent", 0.0);
            double healed = finalDamage * percent;
            if (healed > 0) {
                heal(attacker, healed);
                // Animated "restoring health" action-bar HUD while actively healing in combat.
                hud.bloodthirstBar(attacker, percent, 3000L);
                // Same during-combat health hologram above the target as Berserker's Rage (~6s window).
                hud.trackHealth(victim, 6000L);
            }
        }

        int vampLevel = storage.level(weapon, "vampiric_edge");
        if (vampLevel > 0) {
            applyVampiric(attacker, victim, finalDamage, vampLevel);
        }

        int chain = storage.level(weapon, "chain_lightning");
        if (chain > 0) {
            CustomEnchant e = service.getRegistry().get("chain_lightning");
            chainLightning(attacker, victim, finalDamage, e, chain);
        }

        int pulverize = storage.level(weapon, "pulverize");
        if (pulverize > 0) {
            CustomEnchant e = service.getRegistry().get("pulverize");
            aoe(attacker, victim, finalDamage, e.levelDouble(pulverize, "radius", 2.0),
                    e.levelDouble(pulverize, "damage_percent", 0.2), e.levelBoolean(pulverize, "knockback", false));
            Particles.at(victim.getLocation(), Particles.SMOKE, 14, 0.5);
        }

        // Petrify — stun a non-player mob by freezing its AI for a short window.
        int petrify = storage.level(weapon, "petrify");
        if (petrify > 0 && !(victim instanceof Player)) {
            CustomEnchant e = service.getRegistry().get("petrify");
            if (Math.random() < e.levelDouble(petrify, "chance", 0.0)) {
                double seconds = e.levelDouble(petrify, "stun_seconds", 1.5);
                stun(victim, seconds);
                double amp = e.levelDouble(petrify, "damage_amp", 0.0);
                if (amp > 0) {
                    event.setDamage(event.getDamage() * (1.0 + amp));
                }
                Particles.at(victim.getLocation().add(0, 1, 0), Particles.SMOKE, 20, 0.4);
                debug(attacker, "Petrify stun " + seconds + "s");
            }
        }
    }

    private void stun(final LivingEntity victim, double seconds) {
        try {
            victim.setAI(false);
        } catch (Throwable ignored) {
            // setAI is 1.9+; fall back to heavy slowness below regardless.
        }
        Potions.applyLevel(victim, Potions.SLOWNESS, (int) (seconds * 20), 6);
        Potions.applyLevel(victim, Potions.WEAKNESS, (int) (seconds * 20), 4);
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (victim != null && !victim.isDead()) {
                    try {
                        victim.setAI(true);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, Math.max(1L, (long) (seconds * 20)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = mainHand(killer);

        int soulHarvest = storage.level(weapon, "soul_harvest");
        if (soulHarvest > 0) {
            CustomEnchant e = service.getRegistry().get("soul_harvest");
            double bonus = e.levelDouble(soulHarvest, "xp_percent", 0.0);
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * (1.0 + bonus)));
        }

        int greed = storage.level(weapon, "curse_of_greed");
        if (greed > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_greed");
            double dropBonus = e.levelDouble(greed, "drop_bonus", 0.0);
            duplicateDrops(event, dropBonus);
        }

        int ravenous = storage.level(weapon, "curse_of_ravenous");
        if (ravenous > 0) {
            CustomEnchant e = service.getRegistry().get("curse_of_ravenous");
            double radius = e.levelDouble(ravenous, "radius", 2.0);
            double hearts = e.levelDouble(ravenous, "damage_hearts", 2.0);
            ravenousBurst(killer, dead.getLocation(), radius, hearts * 2.0);
            if (e.levelBoolean(ravenous, "xp_double", false)) {
                event.setDroppedExp(event.getDroppedExp() * 2);
            }
        }

        int bloodthirst = storage.level(weapon, "bloodthirst");
        if (bloodthirst > 0) {
            CustomEnchant e = service.getRegistry().get("bloodthirst");
            int bonusHearts = e.levelInt(bloodthirst, "kill_bonus_hearts", 0);
            if (bonusHearts > 0) {
                heal(killer, bonusHearts * 2.0);
                CombatSupport.title(service, killer, "&c&l❤ BLOODTHIRST",
                        "&7Kill bonus &8» &c+" + bonusHearts + " ❤");
            }
        }

        int headhunter = storage.level(weapon, "headhunter");
        if (headhunter > 0) {
            CustomEnchant e = service.getRegistry().get("headhunter");
            boolean player = dead instanceof Player;
            double chance = e.levelDouble(headhunter, player ? "player_chance" : "mob_chance", 0.0);
            if (Math.random() < chance) {
                ItemStack head = headFor(dead);
                if (head != null) {
                    dead.getWorld().dropItemNaturally(dead.getLocation(), head);
                }
            }
        }

        int pact = storage.level(weapon, "summoners_pact");
        if (pact > 0) {
            CustomEnchant e = service.getRegistry().get("summoners_pact");
            if (Math.random() < e.levelDouble(pact, "chance", 0.0)) {
                summonGolem(killer, e.levelInt(pact, "duration_seconds", 15));
            }
        }
    }

    // ── Effect helpers ────────────────────────────────────────────────────────

    private void handleRiposte(Player victim, LivingEntity attacker, EntityDamageByEntityEvent event) {
        ItemStack weapon = mainHand(victim);
        int level = storage.level(weapon, "riposte");
        if (level <= 0) {
            return;
        }
        CustomEnchant e = service.getRegistry().get("riposte");
        if (Math.random() >= e.levelDouble(level, "chance", 0.0)) {
            return;
        }
        double reflect = event.getFinalDamage() * e.levelDouble(level, "reflect_percent", 0.0);
        if (reflect > 0) {
            try {
                attacker.damage(reflect, victim);
            } catch (Throwable ignored) {
            }
        }
        int weaknessSeconds = e.levelInt(level, "weakness_seconds", 0);
        if (weaknessSeconds > 0) {
            Potions.applyLevel(attacker, Potions.WEAKNESS, weaknessSeconds * 20, 1);
        }
        debug(victim, "Riposte counter");
    }

    private void applyVampiric(Player attacker, LivingEntity victim, double damage, int level) {
        CustomEnchant e = service.getRegistry().get("vampiric_edge");
        UUID id = attacker.getUniqueId();
        VampiricState state = vampiric.get(id);
        long now = System.currentTimeMillis();
        UUID targetId = victim.getUniqueId();
        if (state == null || !targetId.equals(state.target) || now - state.lastHit > 4000L) {
            state = new VampiricState(targetId);
        } else {
            state.stacks = Math.min(state.stacks + 1, e.levelInt(level, "max_stacks", 5) - 1);
        }
        state.lastHit = now;
        vampiric.put(id, state);
        double percent = e.levelDouble(level, "base_percent", 0.0) + e.levelDouble(level, "per_stack", 0.0) * state.stacks;
        heal(attacker, damage * percent);
    }

    private void chainLightning(Player attacker, LivingEntity origin, double damage, CustomEnchant e, int level) {
        int jumps = e.levelInt(level, "jumps", 1);
        double range = e.levelDouble(level, "range", 3.0);
        double pct = e.levelDouble(level, "damage_percent", 0.25);
        boolean particles = e.levelBoolean(level, "particles", false);
        int done = 0;
        for (Entity nearby : origin.getNearbyEntities(range, range, range)) {
            if (done >= jumps) {
                break;
            }
            if (nearby instanceof LivingEntity && nearby != attacker) {
                LivingEntity target = (LivingEntity) nearby;
                try {
                    target.damage(damage * pct, attacker);
                } catch (Throwable ignored) {
                }
                if (particles) {
                    Sounds.play(attacker, Sounds.ZAP, 0.4f, 1.6f);
                    Particles.at(target.getEyeLocation(), Particles.SPARK, 8, 0.2);
                }
                done++;
            }
        }
        if (done > 0) {
            debug(attacker, "Chain Lightning hit " + done);
        }
    }

    private void aoe(Player attacker, LivingEntity center, double damage, double radius, double pct, boolean knockback) {
        for (Entity nearby : center.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity && nearby != attacker && nearby != center) {
                LivingEntity target = (LivingEntity) nearby;
                try {
                    target.damage(damage * pct, attacker);
                    if (knockback) {
                        target.setVelocity(target.getLocation().toVector().subtract(center.getLocation().toVector()).normalize().multiply(0.4).setY(0.2));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void ravenousBurst(Player killer, Location center, double radius, double damage) {
        if (center.getWorld() == null) {
            return;
        }
        for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (nearby instanceof LivingEntity) {
                try {
                    ((LivingEntity) nearby).damage(damage);
                } catch (Throwable ignored) {
                }
            }
        }
        try {
            center.getWorld().createExplosion(center, 0.0f, false);
        } catch (Throwable ignored) {
        }
    }

    private void duplicateDrops(EntityDeathEvent event, double chancePerDrop) {
        java.util.List<ItemStack> extra = new java.util.ArrayList<ItemStack>();
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && Math.random() < chancePerDrop) {
                extra.add(drop.clone());
            }
        }
        event.getDrops().addAll(extra);
    }

    private void summonGolem(Player owner, int seconds) {
        try {
            final Entity golem = owner.getWorld().spawnEntity(owner.getLocation(), EntityType.IRON_GOLEM);
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (golem != null && !golem.isDead()) {
                        golem.remove();
                    }
                }
            }, Math.max(1, seconds) * 20L);
            debug(owner, "Summoner's Pact golem");
        } catch (Throwable ignored) {
        }
    }

    private ItemStack headFor(LivingEntity dead) {
        try {
            if (dead instanceof Player) {
                Material material = matchOr("PLAYER_HEAD", "SKULL_ITEM");
                ItemStack head = legacySkull(material, 3);
                org.bukkit.inventory.meta.ItemMeta meta = head.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                    setSkullOwner((org.bukkit.inventory.meta.SkullMeta) meta, (Player) dead);
                    head.setItemMeta(meta);
                }
                return head;
            }
            EntityType type = dead.getType();
            String name = type.name();
            if (name.equals("WITHER_SKELETON")) {
                return single(matchOr("WITHER_SKELETON_SKULL", "SKULL_ITEM"), 1);
            }
            if (name.equals("ZOMBIE")) {
                return legacySkull(matchOr("ZOMBIE_HEAD", "SKULL_ITEM"), 2);
            }
            if (name.equals("SKELETON")) {
                return legacySkull(matchOr("SKELETON_SKULL", "SKULL_ITEM"), 0);
            }
            if (name.equals("CREEPER")) {
                return legacySkull(matchOr("CREEPER_HEAD", "SKULL_ITEM"), 4);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private ItemStack legacySkull(Material material, int legacyData) {
        if (material != null && material.name().equals("SKULL_ITEM")) {
            return new ItemStack(material, 1, (short) legacyData);
        }
        return single(material, 1);
    }

    @SuppressWarnings("deprecation")
    private void setSkullOwner(org.bukkit.inventory.meta.SkullMeta meta, Player player) {
        try {
            meta.setOwningPlayer(player);
        } catch (Throwable legacy) {
            try {
                meta.setOwner(player.getName());
            } catch (Throwable ignored) {
            }
        }
    }

    private static ItemStack single(Material material, int amount) {
        return new ItemStack(material == null ? Material.matchMaterial("BONE") : material, amount);
    }

    private static Material matchOr(String name, String fallback) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : Material.matchMaterial(fallback);
    }

    // ── Generic helpers ─────────────────────────────────────────────────────

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private double healthFraction(LivingEntity entity) {
        double max = entity.getMaxHealth();
        return max <= 0 ? 1.0 : entity.getHealth() / max;
    }

    @SuppressWarnings("deprecation")
    private void heal(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        double max = player.getMaxHealth();
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    private void damageSelf(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        try {
            double newHealth = player.getHealth() - amount;
            player.setHealth(Math.max(0.0, newHealth));
        } catch (Throwable ignored) {
        }
    }

    private int armorLevel(Player player, String enchantId) {
        int best = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            int level = storage.level(piece, enchantId);
            if (level > best) {
                best = level;
            }
        }
        return best;
    }

    @SuppressWarnings("deprecation")
    private ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            return player.getInventory().getItemInHand();
        }
    }

    private void debug(Player player, String message) {
        if (service.isDebugListener(player)) {
            player.sendMessage(ChatColor.DARK_GRAY + "[Arcanum] " + ChatColor.GRAY + message);
        }
    }

    private static final class VampiricState {
        private final UUID target;
        private int stacks;
        private long lastHit;

        private VampiricState(UUID target) {
            this.target = target;
            this.stacks = 0;
            this.lastHit = System.currentTimeMillis();
        }
    }
}
