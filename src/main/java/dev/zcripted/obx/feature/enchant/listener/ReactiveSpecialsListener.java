package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.service.ReactiveSpecialsService;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Routes events for the reactive-special enchants to {@link ReactiveSpecialsService}:
 * applies the on-hit effects for melee swings (the ranged listener covers bows),
 * siphons Soul Tether backlash when a tethered entity deals damage, and enforces
 * the Spectral Bind Lv3 ender-pearl lock.
 */
public final class ReactiveSpecialsListener implements Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final CombatState combatState;
    private final ReactiveSpecialsService reactive;

    public ReactiveSpecialsListener(ObxPlugin plugin, CombatState combatState, ReactiveSpecialsService reactive) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.combatState = combatState;
        this.reactive = reactive;
    }

    /** Melee attach — the ranged listener handles the projectile (bow) case. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!service.isEnabled() || event.getDamager() instanceof Projectile || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        reactive.onHit(attacker, victim, CombatSupport.mainHand(attacker));
    }

    /**
     * Soul Tether backlash — when a tethered entity deals damage, a fraction of it
     * is siphoned back to the binder. Read at MONITOR so we see the final damage;
     * the siphon is sourceless ({@code damage(amount)}) so it never re-triggers this
     * handler, and is scheduled on the binder's region to stay Folia-safe.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTetheredDealsDamage(EntityDamageByEntityEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Player dealer = CombatSupport.resolveAttacker(event.getDamager());
        if (dealer == null) {
            return;
        }
        UUID binderId = combatState.tetherBinder(dealer.getUniqueId());
        if (binderId == null || binderId.equals(dealer.getUniqueId())) {
            return;
        }
        double backlash = combatState.tetherBacklash(dealer.getUniqueId());
        if (backlash <= 0.0) {
            return;
        }
        final double siphon = event.getFinalDamage() * backlash;
        if (siphon <= 0.0) {
            return;
        }
        final Player binder = Bukkit.getPlayer(binderId);
        if (binder == null || !binder.isOnline() || binder.equals(event.getEntity())) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        Runnable apply = new Runnable() {
            @Override
            public void run() {
                try {
                    binder.damage(siphon);
                } catch (Throwable ignored) {
                    // binder gone / invulnerable
                }
            }
        };
        if (scheduler != null) {
            scheduler.runAtEntity(binder, apply);
        } else {
            apply.run();
        }
        CombatSupport.actionBar(service, binder, "&5Soul Tether &8» &c-" + CombatSupport.format(siphon) + " ❤");
    }

    /**
     * Spectral Bind Lv3 — a bound player cannot escape with ender pearls. Cancelling
     * the launch (rather than refunding) is dupe-safe: the chained throw is simply
     * lost, which fits the flavor of the chain ripping the pearl away.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof EnderPearl)) {
            return;
        }
        ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player)) {
            return;
        }
        Player thrower = (Player) source;
        if (combatState.pearlsLocked(thrower.getUniqueId())) {
            event.setCancelled(true);
            CombatSupport.actionBar(service, thrower, "&bSpectral Bind &8» &7pearl blocked");
        }
    }
}
