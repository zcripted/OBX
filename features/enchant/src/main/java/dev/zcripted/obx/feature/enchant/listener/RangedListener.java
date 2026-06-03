package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.CombatHudService;
import dev.zcripted.obx.feature.enchant.service.CombatParticleService;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.service.ReactiveSpecialsService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.SoundPalette;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bow / crossbow projectile enchants. The firing bow's enchant levels are
 * captured at launch (a clone of the bow stored on the arrow's metadata) and read
 * back when the arrow deals damage, so each shot carries its own enchant context.
 *
 * <p>Implements Piercing Shot (reflective pierce level), Phantom Volley (extra
 * arrows at launch), Sniper's Eye (distance scaling), Hunter's Mark (glow + a
 * cross-source damage mark), and Ricochet (bounce to nearby targets). Trident
 * enchants and the banner-driven ones come in later sub-phases.
 */
public final class RangedListener implements Listener {

    private static final String BOW_META = "obxench.bow";
    private static final String PHANTOM_META = "obxench.phantom";
    private static final String[] PHANTOM_TRAIL = {"CRIT_MAGIC", "CRIT"};
    private static final String[] PHANTOM_SOUND = {"ENTITY_PHANTOM_FLAP", "ENTITY_BAT_TAKEOFF"};
    private static final String[] BOUNCE_TRAIL = {"CRIT", "CRIT_MAGIC"};
    private static final String[] QUICKSHOT_FX = {"FLAME", "CRIT", "FIREWORKS_SPARK"};
    private static final String[] QUICKSHOT_SOUND = {"ENTITY_ARROW_SHOOT", "SHOOT_ARROW"};

    /** Cached {@code PlayerInteractEvent.getHand()} (1.9+); null on 1.8 → treat as main hand. */
    private static final java.lang.reflect.Method GET_HAND = resolveGetHand();

    private static java.lang.reflect.Method resolveGetHand() {
        try {
            return PlayerInteractEvent.class.getMethod("getHand");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantStorage storage;
    private final CombatState combatState;
    private final CombatParticleService particles;
    private final ReactiveSpecialsService reactive;
    private final CombatHudService hud;

    public RangedListener(ObxPlugin plugin, CombatState combatState, CombatParticleService particles, ReactiveSpecialsService reactive, CombatHudService hud) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.storage = service.getStorage();
        this.combatState = combatState;
        this.particles = particles;
        this.reactive = reactive;
        this.hud = hud;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player shooter = (Player) event.getEntity();
        ItemStack bow = event.getBow();
        Entity projectile = event.getProjectile();
        if (bow == null || projectile == null) {
            return;
        }

        int piercing = storage.level(bow, "piercing_shot");
        if (piercing > 0) {
            setPierceLevel(projectile, service.getRegistry().get("piercing_shot").levelInt(piercing, "pierce", 1));
        }

        // Tag the projectile with the bow so on-hit enchants can be resolved later.
        if (piercing > 0 || storage.level(bow, "snipers_eye") > 0
                || storage.level(bow, "hunters_mark") > 0 || storage.level(bow, "ricochet") > 0
                || storage.level(bow, "spectral_bind") > 0 || storage.level(bow, "predator") > 0
                || storage.level(bow, "soul_tether") > 0) {
            projectile.setMetadata(BOW_META, new FixedMetadataValue(plugin, bow.clone()));
        }

        int volley = storage.level(bow, "phantom_volley");
        if (volley > 0) {
            firePhantomArrows(shooter, service.getRegistry().get("phantom_volley"), volley);
        }
    }

    /**
     * Quickshot — a rapid-fire approximation. Right-clicking a bow/crossbow that
     * carries the enchant instantly looses a fully-charged arrow on a short per-level
     * cooldown (true draw-speed is client-side and has no clean server-side API). The
     * arrow is tagged with the firing weapon so on-hit bow enchants still resolve, and
     * piercing carries over. While on cooldown the event is left alone so the vanilla
     * draw works as a fallback.
     */
    @EventHandler(ignoreCancelled = true)
    public void onQuickshot(PlayerInteractEvent event) {
        if (!service.isEnabled() || isOffHand(event)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack weapon = CombatSupport.mainHand(player);
        int level = storage.level(weapon, "quickshot");
        if (level <= 0) {
            return;
        }
        if (combatState.onCooldown(player.getUniqueId(), "quickshot")) {
            return;
        }
        boolean free = player.getGameMode() == GameMode.CREATIVE || hasInfinity(weapon);
        if (!free && !consumeArrow(player)) {
            return;
        }
        event.setCancelled(true);
        fireQuickArrow(player, weapon, service.getRegistry().get("quickshot"), level);
        combatState.setCooldown(player.getUniqueId(), "quickshot",
                (long) service.getRegistry().get("quickshot").levelInt(level, "cooldown_ticks", 20) * 50L);
    }

    private void fireQuickArrow(Player player, ItemStack bow, CustomEnchant e, int level) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        double spread = e.levelDouble(level, "spread", 0.0);
        if (spread > 0) {
            dir.add(new Vector((Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread));
            dir.normalize();
        }
        try {
            Arrow arrow = player.getWorld().spawnArrow(player.getEyeLocation().add(dir.clone().multiply(0.8)), dir, 3.0f, 1.0f);
            arrow.setShooter(player);
            // Preserve on-hit bow enchants (Sniper's Eye, Hunter's Mark, Ricochet, reactive specials).
            arrow.setMetadata(BOW_META, new FixedMetadataValue(plugin, bow.clone()));
            int piercing = storage.level(bow, "piercing_shot");
            if (piercing > 0) {
                setPierceLevel(arrow, service.getRegistry().get("piercing_shot").levelInt(piercing, "pierce", 1));
            }
            try {
                arrow.setCritical(true);
            } catch (Throwable ignored) {
                // setCritical present on all supported versions; guard anyway
            }
            Particles.at(player.getEyeLocation().add(dir.clone().multiply(0.6)), QUICKSHOT_FX, 6, 0.1);
        } catch (Throwable ignored) {
            // spawnArrow can fail in odd worlds — skip the shot
        }
        SoundPalette.playAt(player.getLocation(), QUICKSHOT_SOUND, CombatSupport.volume(service, 0.7f), 1.4f);
        CombatSupport.actionBar(service, player, "&e➤ Quickshot");
    }

    private boolean hasInfinity(ItemStack weapon) {
        try {
            return weapon != null && weapon.containsEnchantment(Enchantment.ARROW_INFINITE);
        } catch (Throwable ignored) {
            // Enchantment constant may be missing/renamed on very new forks
            return false;
        }
    }

    private boolean consumeArrow(Player player) {
        try {
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && isArrow(item.getType())) {
                    if (item.getAmount() <= 1) {
                        inv.setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - 1);
                    }
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // inventory access issue — treat as no ammo
        }
        return false;
    }

    private boolean isArrow(Material material) {
        String name = material.name();
        return name.equals("ARROW") || name.equals("TIPPED_ARROW") || name.equals("SPECTRAL_ARROW");
    }

    private static boolean isOffHand(PlayerInteractEvent event) {
        if (GET_HAND == null) {
            return false;
        }
        try {
            Object hand = GET_HAND.invoke(event);
            return hand instanceof Enum && !"HAND".equals(((Enum<?>) hand).name());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!service.isEnabled() || !(event.getDamager() instanceof Projectile) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        Projectile projectile = (Projectile) event.getDamager();
        ItemStack bow = bowOf(projectile);
        if (bow == null) {
            return;
        }
        LivingEntity victim = (LivingEntity) event.getEntity();
        ProjectileSource source = projectile.getShooter();
        Player shooter = source instanceof Player ? (Player) source : null;

        // During-combat target health hologram for ANY Combat-category enchant on the bow.
        if (shooter != null && CombatSupport.hasCombatEnchant(service, bow)) {
            hud.trackHealth(victim, 6000L);
        }

        // Sniper's Eye — damage scales with shot distance, with a long-range crit.
        int sniper = storage.level(bow, "snipers_eye");
        if (sniper > 0 && shooter != null) {
            CustomEnchant e = service.getRegistry().get("snipers_eye");
            double distance = shooter.getLocation().distance(victim.getLocation());
            double mult = 1.0 + Math.min(e.levelDouble(sniper, "max_bonus", 0.25),
                    (distance / 10.0) * e.levelDouble(sniper, "per_ten", 0.05));
            double critRange = e.levelDouble(sniper, "crit_range", 0.0);
            if (critRange > 0 && distance >= critRange && Math.random() < e.levelDouble(sniper, "crit_chance", 0.0)) {
                mult += 0.5;
                Particles.burst(victim.getEyeLocation(), Particles.CRIT);
                CombatSupport.actionBar(service, shooter, "&6Long Shot");
            }
            event.setDamage(event.getDamage() * mult);
        }

        // Hunter's Mark — mark the target (glow + cross-source damage bonus).
        int mark = storage.level(bow, "hunters_mark");
        if (mark > 0) {
            CustomEnchant e = service.getRegistry().get("hunters_mark");
            double seconds = e.levelDouble(mark, "duration_seconds", 5.0);
            combatState.markTarget(victim.getUniqueId(), e.levelDouble(mark, "bonus", 0.10), (long) (seconds * 1000));
            try {
                victim.setGlowing(true);
                scheduleUnglow(victim, (int) (seconds * 20));
            } catch (Throwable ignored) {
                // setGlowing is 1.9+
            }
            if (shooter != null) {
                CombatSupport.actionBar(service, shooter, "&cMarked");
            }
        }

        // Ricochet — the arrow bounces to nearby targets with falloff.
        int ricochet = storage.level(bow, "ricochet");
        if (ricochet > 0 && shooter != null) {
            ricochet(shooter, victim, event.getFinalDamage(), service.getRegistry().get("ricochet"), ricochet);
        }

        // Reactive specials (Spectral Bind / Predator / Soul Tether) on a bow shot.
        if (shooter != null) {
            reactive.onHit(shooter, victim, bow);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Projectile projectile = event.getEntity();
        // Match by type name so we never reference the 1.13+ Trident class at compile time.
        if (projectile == null || !"TRIDENT".equals(projectile.getType().name())) {
            return;
        }
        ItemStack trident = tridentItem(projectile);
        if (trident == null) {
            return;
        }
        Location loc = projectile.getLocation();
        if (loc.getWorld() == null) {
            return;
        }
        Player shooter = projectile.getShooter() instanceof Player ? (Player) projectile.getShooter() : null;

        int storm = storage.level(trident, "trident_storm");
        if (storm > 0) {
            tridentStorm(loc, shooter, service.getRegistry().get("trident_storm"), storm);
        }
        int tide = storage.level(trident, "tidecaller");
        if (tide > 0) {
            tidecaller(loc, shooter, service.getRegistry().get("tidecaller"), tide);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Reads the thrown trident's backing item (Paper exposes it; null elsewhere). */
    private ItemStack tridentItem(Projectile projectile) {
        for (String method : new String[]{"getItemStack", "getItem"}) {
            try {
                Object item = projectile.getClass().getMethod(method).invoke(projectile);
                if (item instanceof ItemStack) {
                    return (ItemStack) item;
                }
            } catch (Throwable ignored) {
                // try the next accessor
            }
        }
        return null;
    }

    private void tridentStorm(Location loc, Player shooter, CustomEnchant e, int level) {
        World world = loc.getWorld();
        if (world.hasStorm()) {
            int strikes = e.levelInt(level, "lightning", 1);
            for (int i = 0; i < strikes; i++) {
                world.strikeLightning(loc);
            }
            int followup = e.levelInt(level, "followup", 0);
            for (int i = 0; i < followup; i++) {
                world.strikeLightning(loc.clone().add((Math.random() - 0.5) * 6, 0, (Math.random() - 0.5) * 6));
            }
        } else {
            double radius = e.levelDouble(level, "shockwave_radius", 2.0);
            double damage = e.levelDouble(level, "shockwave_damage", 2.0);
            int slow = e.levelInt(level, "slowness_ticks", 0);
            for (LivingEntity target : nearbyLiving(loc, radius)) {
                if (shooter != null && target.equals(shooter)) {
                    continue;
                }
                try {
                    target.damage(damage, shooter);
                } catch (Throwable ignored) {
                    // off-region / invulnerable
                }
                if (slow > 0) {
                    Potions.applyLevel(target, Potions.SLOWNESS, slow, 2);
                }
            }
            particles.spawnShockwave(loc, radius, new String[]{"CLOUD", "CRIT"}, 8);
        }
        SoundPalette.playAt(loc, new String[]{"ENTITY_LIGHTNING_BOLT_THUNDER", "ENTITY_LIGHTNING_THUNDER", "AMBIENCE_THUNDER"},
                CombatSupport.volume(service, 0.8f), 1.0f);
    }

    private void tidecaller(Location loc, Player shooter, CustomEnchant e, int level) {
        double radius = e.levelDouble(level, "radius", 1.0);
        double push = e.levelDouble(level, "push", 0.5);
        int slow = e.levelInt(level, "slowness_ticks", 0);
        for (LivingEntity target : nearbyLiving(loc, radius + 1.0)) {
            if (shooter != null && target.equals(shooter)) {
                continue;
            }
            Vector direction = target.getLocation().toVector().subtract(loc.toVector()).setY(0.3);
            if (direction.lengthSquared() > 1.0E-6) {
                direction.normalize().multiply(push);
                try {
                    target.setVelocity(target.getVelocity().add(direction));
                } catch (Throwable ignored) {
                    // velocity set can be region-restricted on Folia
                }
            }
            if (slow > 0) {
                Potions.applyLevel(target, Potions.SLOWNESS, slow, 1);
            }
        }
        particles.spawnRing(loc, radius, new String[]{"WATER_SPLASH", "DRIP_WATER", "CLOUD"}, 16);
        SoundPalette.playAt(loc, new String[]{"ENTITY_PLAYER_SPLASH", "ENTITY_GENERIC_SPLASH", "SPLASH"},
                CombatSupport.volume(service, 0.6f), 1.0f);
    }

    private List<LivingEntity> nearbyLiving(Location loc, double radius) {
        List<LivingEntity> list = new ArrayList<LivingEntity>();
        try {
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof LivingEntity) {
                    list.add((LivingEntity) entity);
                }
            }
        } catch (Throwable fallback) {
            double r2 = radius * radius;
            for (LivingEntity entity : loc.getWorld().getLivingEntities()) {
                if (entity.getLocation().distanceSquared(loc) <= r2) {
                    list.add(entity);
                }
            }
        }
        return list;
    }

    private ItemStack bowOf(Projectile projectile) {
        if (!projectile.hasMetadata(BOW_META)) {
            return null;
        }
        for (MetadataValue value : projectile.getMetadata(BOW_META)) {
            if (value.value() instanceof ItemStack) {
                return (ItemStack) value.value();
            }
        }
        return null;
    }

    private void setPierceLevel(Entity projectile, int level) {
        try {
            projectile.getClass().getMethod("setPierceLevel", int.class).invoke(projectile, Math.max(0, level));
        } catch (Throwable ignored) {
            // setPierceLevel is 1.14+; arrows just don't pierce on older
        }
    }

    private void setArrowDamage(Arrow arrow, double damage) {
        try {
            arrow.getClass().getMethod("setDamage", double.class).invoke(arrow, damage);
        } catch (Throwable ignored) {
            // setDamage is 1.14+ (AbstractArrow); phantom arrows just deal full damage on older
        }
    }

    private void firePhantomArrows(Player shooter, CustomEnchant e, int level) {
        int count = e.levelInt(level, "arrows", 1);
        double range = e.levelDouble(level, "range", 15.0);
        double damagePercent = e.levelDouble(level, "damage_percent", 0.5);
        List<LivingEntity> targets = new ArrayList<LivingEntity>();
        for (Entity near : shooter.getNearbyEntities(range, range, range)) {
            if (near instanceof LivingEntity && !near.equals(shooter) && (near instanceof Monster || near instanceof Player)) {
                targets.add((LivingEntity) near);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            LivingEntity target = targets.get((int) (Math.random() * targets.size()));
            Vector direction = target.getEyeLocation().toVector().subtract(shooter.getEyeLocation().toVector());
            if (direction.lengthSquared() < 1.0E-6) {
                continue;
            }
            direction.normalize();
            try {
                Arrow arrow = shooter.getWorld().spawnArrow(shooter.getEyeLocation().add(direction.clone().multiply(0.8)), direction, 2.5f, 1.0f);
                arrow.setShooter(shooter);
                arrow.setMetadata(PHANTOM_META, new FixedMetadataValue(plugin, Boolean.TRUE));
                try {
                    arrow.setCritical(false);
                } catch (Throwable ignored) {
                    // setCritical present on all supported versions; guard anyway
                }
                // Vanilla arrow base damage is ~2.0; scale it (reflective — setDamage is 1.14+).
                setArrowDamage(arrow, 2.0 * damagePercent);
                Particles.at(arrow.getLocation(), PHANTOM_TRAIL, 4, 0.1);
            } catch (Throwable ignored) {
                // spawnArrow can fail in odd worlds — skip that phantom
            }
        }
        SoundPalette.play(shooter, PHANTOM_SOUND, CombatSupport.volume(service, 0.5f), 1.2f);
    }

    private void ricochet(Player shooter, LivingEntity firstHit, double damage, CustomEnchant e, int level) {
        int bounces = e.levelInt(level, "bounces", 1);
        double range = e.levelDouble(level, "range", 5.0);
        double percent = e.levelDouble(level, "damage_percent", 0.6);
        Set<UUID> alreadyHit = new HashSet<UUID>();
        alreadyHit.add(firstHit.getUniqueId());
        LivingEntity current = firstHit;
        double bounceDamage = damage * percent;
        for (int b = 0; b < bounces; b++) {
            LivingEntity next = nearestUnhit(current, range, alreadyHit, shooter);
            if (next == null) {
                break;
            }
            particles.spawnTrail(current.getEyeLocation(), next.getEyeLocation(), BOUNCE_TRAIL, 6);
            try {
                next.damage(bounceDamage, shooter);
            } catch (Throwable ignored) {
                // off-region / invulnerable — stop the chain
            }
            alreadyHit.add(next.getUniqueId());
            current = next;
            bounceDamage *= percent;
        }
    }

    private LivingEntity nearestUnhit(LivingEntity from, double range, Set<UUID> exclude, Player shooter) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity near : from.getNearbyEntities(range, range, range)) {
            if (!(near instanceof LivingEntity) || near.equals(shooter) || exclude.contains(near.getUniqueId())) {
                continue;
            }
            double distance = from.getLocation().distanceSquared(near.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (LivingEntity) near;
            }
        }
        return best;
    }

    private void scheduleUnglow(final LivingEntity entity, int ticks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!entity.isDead()) {
                        entity.setGlowing(false);
                    }
                } catch (Throwable ignored) {
                    // entity gone / API unavailable
                }
            }
        }, Math.max(1L, ticks));
    }
}
