package dev.zcripted.obx.feature.economy.listener;

import dev.zcripted.obx.api.economy.EconomyService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Seeds an account row (at the starting balance) the first time a player joins, so every
 * real player has a stored balance — keeping {@code /balance} and {@code /baltop} consistent
 * (no fabricated starting balance for accounts that aren't tracked).
 */
public final class EconomyJoinListener implements Listener {

    private final EconomyService economy;

    public EconomyJoinListener(EconomyService economy) {
        this.economy = economy;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (economy != null) {
            economy.ensureAccount(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        }
    }
}