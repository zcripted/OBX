package dev.zcripted.obx.feature.enchant;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.enchant.command.EnchantsBrowseCommand;
import dev.zcripted.obx.feature.enchant.command.ObxEnchantCommand;
import dev.zcripted.obx.feature.enchant.command.RecallCommand;
import dev.zcripted.obx.feature.enchant.command.SatchelCommand;
import dev.zcripted.obx.feature.enchant.effect.BoundMovement;
import dev.zcripted.obx.feature.enchant.effect.CombatState;
import dev.zcripted.obx.feature.enchant.effect.EffectUtil;
import dev.zcripted.obx.feature.enchant.effect.EnchantState;
import dev.zcripted.obx.feature.enchant.effect.EnchantTickTask;
import dev.zcripted.obx.feature.enchant.effect.SatchelCloseListener;
import dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu;
import dev.zcripted.obx.feature.enchant.gui.EnchantMenuListener;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.listener.CombatEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.CursedEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.DefenseEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.EnchantBookUseListener;
import dev.zcripted.obx.feature.enchant.listener.EnchantLoreListener;
import dev.zcripted.obx.feature.enchant.listener.FarmingEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.MovementEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.OnDeathListener;
import dev.zcripted.obx.feature.enchant.listener.OnHitDamageListener;
import dev.zcripted.obx.feature.enchant.listener.OnHitProcListener;
import dev.zcripted.obx.feature.enchant.listener.OnKillListener;
import dev.zcripted.obx.feature.enchant.listener.RangedListener;
import dev.zcripted.obx.feature.enchant.listener.ReactiveSpecialsListener;
import dev.zcripted.obx.feature.enchant.listener.ToolEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.UtilityEnchantListener;
import dev.zcripted.obx.feature.enchant.loot.EnchantLoot;
import dev.zcripted.obx.feature.enchant.scroll.AnvilEnchantListener;
import dev.zcripted.obx.feature.enchant.scroll.ScrollApplyService;
import dev.zcripted.obx.feature.enchant.scroll.ScrollDragListener;
import dev.zcripted.obx.feature.enchant.service.CombatHudService;
import dev.zcripted.obx.feature.enchant.service.CombatParticleService;
import dev.zcripted.obx.feature.enchant.service.EnchantFeedback;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.service.HoloFXService;
import dev.zcripted.obx.feature.enchant.service.ReactiveSpecialsService;
import org.bukkit.entity.Player;

/**
 * Arcanum custom-enchantment feature: enchant registry/effects, scrolls, combat
 * HUD + particles, loot, satchels, and the GUI. The large construction graph and
 * teardown (tick-task stop, satchel save, FX clears, bound-toughness reset) live
 * here; OBX keeps the historical enable order by registering this module last.
 */
public final class EnchantModule extends AbstractModule {

    @Override
    public String id() {
        return "enchant";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        EnchantService enchantService = service(EnchantService.class, new EnchantService(plugin));
        enchantService.load();
        service(EnchantItems.class, new EnchantItems(enchantService));
        service(EnchantFeedback.class, new EnchantFeedback(plugin));
        service(EnchantAdminMenu.class, new EnchantAdminMenu(plugin));
        ScrollApplyService scrollApply = new ScrollApplyService(plugin);
        EnchantLoot enchantLoot = service(EnchantLoot.class, new EnchantLoot(plugin));
        enchantLoot.register();
        EnchantState enchantState = new EnchantState(plugin);
        CombatState combatState = new CombatState();
        CombatParticleService combatParticles = new CombatParticleService(plugin);
        HoloFXService holoFX = new HoloFXService(plugin);
        ReactiveSpecialsService reactiveSpecials = new ReactiveSpecialsService(plugin, combatState, combatParticles);
        CombatHudService combatHud = new CombatHudService(plugin);
        combatHud.start();
        BoundMovement boundMovement = new BoundMovement(enchantService.getStorage());
        EnchantTickTask tickTask = new EnchantTickTask(plugin, boundMovement);
        tickTask.start();

        // Listeners (combat effects + GUI routing).
        listener(new CombatEnchantListener(plugin, enchantService, combatHud));
        listener(new EnchantMenuListener(plugin));
        listener(new AnvilEnchantListener(plugin, scrollApply));
        listener(new ScrollDragListener(plugin, scrollApply));
        listener(new DefenseEnchantListener(plugin, enchantState));
        listener(new ToolEnchantListener(plugin));
        listener(new FarmingEnchantListener(plugin));
        listener(new UtilityEnchantListener(plugin));
        listener(new MovementEnchantListener(plugin, enchantState));
        listener(new SatchelCloseListener(enchantState));
        listener(new CursedEnchantListener(plugin, boundMovement));
        listener(new EnchantLoreListener(plugin));
        listener(new EnchantBookUseListener(plugin));
        listener(new OnHitDamageListener(plugin, combatState, combatParticles, holoFX, combatHud));
        listener(new OnKillListener(plugin, combatState, combatParticles, holoFX));
        OnHitProcListener onHitProc = new OnHitProcListener(plugin, combatState, combatParticles, combatHud);
        listener(onHitProc);
        listener(new OnDeathListener(plugin));
        listener(new RangedListener(plugin, combatState, combatParticles, reactiveSpecials, combatHud));
        listener(new ReactiveSpecialsListener(plugin, combatState, reactiveSpecials));

        // Low-frequency janitor (every 2 min): purge time-expired CombatState entries so a mob that
        // DESPAWNS without dying (chunk unload / distance / removal) — and whose UUID is never read
        // again — can't leave a permanent mob-keyed entry. EntityDeathEvent already purges on death.
        dev.zcripted.obx.core.platform.scheduler.CancellableTask combatStateSweep =
                plugin.getSchedulerAdapter().runRepeating(combatState::sweepExpired, 2400L, 2400L);
        onDisable(combatStateSweep::cancel);
        // Same janitor sweeps stale bleed entries for entities that despawned before the bleed
        // expired (Folia entity scheduler skips removed entities, so the normal cleanup never fires).
        dev.zcripted.obx.core.platform.scheduler.CancellableTask bleedSweep =
                plugin.getSchedulerAdapter().runRepeating(onHitProc::sweepExpired, 2400L, 2400L);
        onDisable(bleedSweep::cancel);

        // Commands.
        command("obxench", new ObxEnchantCommand(plugin));
        command("enchants", new EnchantsBrowseCommand(plugin));
        command("recall", new RecallCommand(plugin, enchantState));
        command("satchel", new SatchelCommand(plugin, enchantState));

        // Teardown — registered in reverse so it runs in the historical order:
        // tick-stop, satchel save, FX clears, then bound-toughness reset.
        onDisable(() -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                EffectUtil.setBoundToughness(online, 0.0);
                boundMovement.restore(online);
            }
        });
        onDisable(combatHud::clear);
        onDisable(combatParticles::clear);
        onDisable(holoFX::clear);
        onDisable(enchantState::saveAll);
        onDisable(tickTask::stop);
    }

    @Override
    public void reload(ObxPlugin plugin) {
        EnchantService enchantService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        if (enchantService != null) {
            enchantService.reload();
        }
        EnchantLoot loot = plugin.getServiceRegistry().get(EnchantLoot.class);
        if (loot != null) {
            loot.reload();
        }
    }
}