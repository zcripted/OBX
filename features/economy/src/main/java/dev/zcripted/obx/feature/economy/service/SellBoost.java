package dev.zcripted.obx.feature.economy.service;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Locale;

/**
 * Rank-based sell-payout multipliers via permission nodes:
 * {@code obx.sell.multiplier.<factor>} (e.g. {@code obx.sell.multiplier.1.5}).
 * The HIGHEST granted factor wins; values are clamped to [0.1, 10]. No node
 * (or only unparsable ones) means the neutral 1.0.
 *
 * <p>Applied by every sell path AFTER the base worth/shop price is computed and
 * BEFORE the daily cap check — the cap limits what actually lands in the wallet.
 */
public final class SellBoost {

    private static final String PREFIX = "obx.sell.multiplier.";
    private static final double MIN = 0.1;
    private static final double MAX = 10.0;

    private SellBoost() {
    }

    /** The effective sell multiplier for {@code player} (1.0 when none granted). */
    public static double multiplier(Player player) {
        if (player == null) {
            return 1.0;
        }
        double best = 0.0;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String node = info.getPermission().toLowerCase(Locale.ENGLISH);
            if (!node.startsWith(PREFIX)) {
                continue;
            }
            try {
                double factor = Double.parseDouble(node.substring(PREFIX.length()));
                if (Double.isFinite(factor)) {
                    best = Math.max(best, Math.min(MAX, Math.max(MIN, factor)));
                }
            } catch (NumberFormatException ignored) {
                // malformed node — skip
            }
        }
        return best <= 0.0 ? 1.0 : best;
    }
}
