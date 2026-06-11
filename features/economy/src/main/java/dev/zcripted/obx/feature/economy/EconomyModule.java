package dev.zcripted.obx.feature.economy;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.economy.auction.AuctionCommand;
import dev.zcripted.obx.feature.economy.auction.AuctionListener;
import dev.zcripted.obx.feature.economy.auction.AuctionService;
import dev.zcripted.obx.feature.economy.bank.BankMenuListener;
import dev.zcripted.obx.feature.economy.command.BalTopCommand;
import dev.zcripted.obx.feature.economy.command.BalanceCommand;
import dev.zcripted.obx.feature.economy.command.BankCommand;
import dev.zcripted.obx.feature.economy.command.EcoCommand;
import dev.zcripted.obx.feature.economy.command.PayCommand;
import dev.zcripted.obx.feature.economy.command.SellAllCommand;
import dev.zcripted.obx.feature.economy.command.SellCommand;
import dev.zcripted.obx.feature.economy.command.WithdrawCommand;
import dev.zcripted.obx.feature.economy.command.WorthCommand;
import dev.zcripted.obx.feature.economy.report.EconomyReportService;
import dev.zcripted.obx.feature.economy.service.BankService;
import dev.zcripted.obx.feature.economy.service.BanknoteService;
import dev.zcripted.obx.feature.economy.service.SellLimitTracker;
import dev.zcripted.obx.feature.economy.service.WorthService;
import dev.zcripted.obx.feature.economy.shop.ShopPricing;
import dev.zcripted.obx.feature.economy.sink.RepairFeeListener;
import dev.zcripted.obx.feature.economy.sink.WeeklyTopService;
import dev.zcripted.obx.feature.economy.shop.SellWandListener;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;

/**
 * Economy feature: balances, pay (confirm threshold + optional transfer tax),
 * sell/worth (daily caps + rank multipliers), baltop, eco admin (paginated audit
 * log), the category shop (finite stock + dynamic pricing), banknotes
 * ({@code /withdraw}), the interest bank ({@code /bank}), the auction house
 * ({@code /ah}), scheduled payday, and the {@code %obx_*%} PlaceholderAPI
 * expansion. Registers {@link EconomyService} and exposes it to Vault.
 */
public final class EconomyModule extends AbstractModule {

    @Override
    public String id() {
        return "economy";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        EconomyService economy = service(EconomyService.class,
                new dev.zcripted.obx.feature.economy.service.EconomyServiceImpl(plugin));
        economy.load();
        WorthService worth = service(WorthService.class, new WorthService(plugin));
        worth.load();
        dev.zcripted.obx.feature.economy.shop.ShopService shop = service(
                dev.zcripted.obx.feature.economy.shop.ShopService.class,
                new dev.zcripted.obx.feature.economy.shop.ShopService(plugin));
        shop.load();
        ShopPricing pricing = service(ShopPricing.class, new ShopPricing(plugin));
        pricing.load();
        SellLimitTracker sellLimits = service(SellLimitTracker.class, new SellLimitTracker(plugin));
        sellLimits.load();
        BanknoteService banknotes = service(BanknoteService.class, new BanknoteService(plugin));
        banknotes.load();
        BankService bank = service(BankService.class, new BankService(plugin));
        bank.load();
        AuctionService auction = service(AuctionService.class, new AuctionService(plugin));
        auction.load();

        // Sinks + reporting
        dev.zcripted.obx.feature.economy.sink.ServerAccountService serverAccount = service(
                dev.zcripted.obx.feature.economy.sink.ServerAccountService.class,
                new dev.zcripted.obx.feature.economy.sink.ServerAccountService(plugin));
        serverAccount.load();
        dev.zcripted.obx.feature.economy.sink.ClaimUpkeepService claimUpkeep = service(
                dev.zcripted.obx.feature.economy.sink.ClaimUpkeepService.class,
                new dev.zcripted.obx.feature.economy.sink.ClaimUpkeepService(plugin));
        claimUpkeep.load();
        WeeklyTopService weeklyTop = service(WeeklyTopService.class, new WeeklyTopService(plugin));
        weeklyTop.load();
        EconomyReportService report = service(EconomyReportService.class, new EconomyReportService(plugin));

        if (VaultEconomyProvider.register(plugin, economy)) {
            onDisable(() -> VaultEconomyProvider.unregister(plugin));
        }
        registerPlaceholderExpansion(plugin);

        listener(new dev.zcripted.obx.feature.economy.listener.EconomyJoinListener(economy));
        listener(new dev.zcripted.obx.feature.economy.shop.ShopListener(plugin));
        listener(new dev.zcripted.obx.feature.economy.shop.ShopEditorListener(plugin));
        listener(new dev.zcripted.obx.feature.economy.listener.BanknoteListener(plugin, banknotes));
        listener(new AuctionListener(plugin));
        listener(new BankMenuListener(plugin));
        listener(new RepairFeeListener(plugin));
        listener(new SellWandListener(plugin));

        command("balance", new BalanceCommand(plugin));
        command("baltop", new BalTopCommand(plugin));
        command("pay", new PayCommand(plugin));
        command("eco", new EcoCommand(plugin));
        command("worth", new WorthCommand(plugin));
        command("sell", new SellCommand(plugin));
        command("sellall", new SellAllCommand(plugin));
        command("shop", new dev.zcripted.obx.feature.economy.shop.ShopCommand(plugin));
        command("withdraw", new WithdrawCommand(plugin));
        command("bank", new BankCommand(plugin));
        command("ah", new AuctionCommand(plugin));

        // Expired listings → sellers' returns ledgers, every 5 minutes.
        final dev.zcripted.obx.core.platform.scheduler.CancellableTask sweep =
                plugin.getSchedulerAdapter().runRepeating(auction::sweepExpired, 6000L, 6000L);
        onDisable(() -> {
            if (sweep != null) {
                sweep.cancel();
            }
        });

        // Weekly top snapshot + digest, every 7 days (initial delay: 1 hour).
        if (plugin.getConfig().getBoolean("economy.sinks.weekly-top.enabled", true)) {
            final dev.zcripted.obx.core.platform.scheduler.CancellableTask weekly =
                    plugin.getSchedulerAdapter().runRepeating(() -> {
                        weeklyTop.snapshot();
                        report.generateDigest();
                    }, 72000L, 7L * 24L * 3600L * 20L); // 1 hour initial, then every 7 days
            onDisable(() -> {
                if (weekly != null) {
                    weekly.cancel();
                }
            });
        }

        // Claim upkeep sweep: hourly check, charges each online owner once per day.
        if (claimUpkeep.isEnabled()) {
            final dev.zcripted.obx.core.platform.scheduler.CancellableTask upkeep =
                    plugin.getSchedulerAdapter().runRepeating(claimUpkeep::sweep, 1200L, 72000L);
            onDisable(() -> {
                if (upkeep != null) {
                    upkeep.cancel();
                }
            });
        }

        startPayday(plugin, economy);
    }

    /**
     * Scheduled salary ({@code economy.payday.*}): every {@code interval-minutes},
     * every online player holding {@code obx.payday} receives {@code amount}.
     * Amount/permission are re-read each cycle so config reloads apply live;
     * interval changes take effect on module reload.
     */
    private void startPayday(ObxPlugin plugin, EconomyService economy) {
        if (!plugin.getConfig().getBoolean("economy.payday.enabled", false)) {
            return;
        }
        long intervalTicks = Math.max(1, plugin.getConfig().getInt("economy.payday.interval-minutes", 60)) * 1200L;
        final dev.zcripted.obx.core.platform.scheduler.CancellableTask payday =
                plugin.getSchedulerAdapter().runRepeating(() -> {
                    if (!plugin.getConfig().getBoolean("economy.payday.enabled", false)) {
                        return; // disabled by reload — sleep until module reload removes the task
                    }
                    double amount = EconomyService.sanitize(
                            plugin.getConfig().getDouble("economy.payday.amount", 50.0));
                    if (amount <= 0.0) {
                        return;
                    }
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        if (!online.hasPermission("obx.payday")) {
                            continue;
                        }
                        if (!economy.depositStrict(online.getUniqueId(), online.getName(), amount)) {
                            continue; // wallet at cap — skip rather than destroy value
                        }
                        economy.logTransaction("PAYDAY", online.getUniqueId(), online.getName(),
                                "PAYDAY", amount, economy.getBalance(online.getUniqueId()));
                        plugin.getLanguageManager().send(online, "economy.payday.received",
                                Placeholders.with("amount", economy.format(amount)));
                    }
                }, intervalTicks, intervalTicks);
        onDisable(() -> {
            if (payday != null) {
                payday.cancel();
            }
        });
    }

    /**
     * Registers the {@code %obx_*%} PlaceholderAPI expansion when PAPI is installed.
     * The expansion class is only touched behind a Class.forName probe — PAPI is a
     * compileOnly dependency and may be absent at runtime.
     */
    private void registerPlaceholderExpansion(ObxPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            final dev.zcripted.obx.feature.economy.papi.ObxEconomyExpansion expansion =
                    new dev.zcripted.obx.feature.economy.papi.ObxEconomyExpansion(plugin);
            if (expansion.register()) {
                dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Economy",
                        "PlaceholderAPI expansion registered (§d%obx_*%§7)");
                onDisable(() -> {
                    try {
                        expansion.unregister();
                    } catch (Throwable ignored) {
                        // PAPI already shut down — nothing to do
                    }
                });
            }
        } catch (Throwable papiUnavailable) {
            // PAPI plugin present but API incompatible — economy works fine without it.
        }
    }
}