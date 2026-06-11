package dev.zcripted.obx.feature.kit.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.kit.model.Kit;
import dev.zcripted.obx.feature.kit.service.KitService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grants first-join kits. When {@code kits.yml → first-join.enabled} is true, a
 * player who has never claimed before receives every kit flagged
 * {@code first-join: true} on login, exactly once.
 *
 * <p>The one-time guard is the persisted {@code kit_first_join} claim flag, not
 * {@link Player#hasPlayedBefore()}: that method is unreliable on first join
 * (player data is sometimes already written by the time the event fires), so the
 * DB flag is the authoritative "has this player ever received it" record. A
 * consequence is that enabling the feature on an existing server grants the
 * welcome kit to current players the next time they log in — which is the
 * intended one-time-per-player behaviour.
 */
public final class KitFirstJoinListener implements Listener {

    private final ObxPlugin plugin;
    private final KitService service;
    private final LanguageManager languages;

    public KitFirstJoinListener(ObxPlugin plugin, KitService service) {
        this.plugin = plugin;
        this.service = service;
        this.languages = plugin.getLanguageManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (service == null || !service.isFirstJoinEnabled()) {
            return;
        }
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (service.hasReceivedFirstJoinKit(uuid)) {
            return;
        }
        final List<Kit> firstJoinKits = service.getFirstJoinKits();
        if (firstJoinKits.isEmpty()) {
            return;
        }
        // Defer a tick so other join handlers settle the inventory first, then
        // grant and record the claim.
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                // ATOMIC claim BEFORE granting: only the call that wins the claim grants the kit, so a
                // relog / double-join firing two handlers can't duplicate the welcome kit.
                if (!service.tryClaimFirstJoin(uuid)) {
                    return;
                }
                for (Kit kit : firstJoinKits) {
                    Map<String, Object> result = service.giveItems(player, kit);
                    languages.send(player, "kit.first-join-received",
                            Placeholders.with("kit", kit.getDisplayName()));
                    Object dropped = result.get("droppedCount");
                    if (dropped instanceof Integer && (Integer) dropped > 0) {
                        languages.send(player, "kit.overflow", Placeholders.with("count", dropped));
                    }
                }
                // Claim already recorded atomically by tryClaimFirstJoin above — nothing else to mark.
            }
        }, 1L);
    }
}