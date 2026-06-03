package dev.zcripted.obx.feature.economy;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.api.economy.VaultEconomyProvider;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.economy.command.BalTopCommand;
import dev.zcripted.obx.feature.economy.command.BalanceCommand;
import dev.zcripted.obx.feature.economy.command.EcoCommand;
import dev.zcripted.obx.feature.economy.command.PayCommand;
import dev.zcripted.obx.feature.economy.command.SellAllCommand;
import dev.zcripted.obx.feature.economy.command.SellCommand;
import dev.zcripted.obx.feature.economy.command.WorthCommand;
import dev.zcripted.obx.feature.economy.service.WorthService;

/**
 * Economy feature: balances, pay, sell/worth, baltop, eco admin. Registers the
 * {@link EconomyService} (the public API type) + {@link WorthService} and exposes
 * economy to Vault via {@link VaultEconomyProvider}.
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
        VaultEconomyProvider.register(plugin, economy);
        command("balance", new BalanceCommand(plugin));
        command("baltop", new BalTopCommand(plugin));
        command("pay", new PayCommand(plugin));
        command("eco", new EcoCommand(plugin));
        command("worth", new WorthCommand(plugin));
        command("sell", new SellCommand(plugin));
        command("sellall", new SellAllCommand(plugin));
    }
}
