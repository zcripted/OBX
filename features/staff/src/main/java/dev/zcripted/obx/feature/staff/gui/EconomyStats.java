package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.api.economy.EconomyService;

import java.util.Collections;
import java.util.List;

/**
 * Short-lived cache for the economy numbers shown by the Admin-Menu Economy tile and
 * the Economy Control panel. Both render on the {@link AdminMenuRefreshTask}'s 0.5s
 * cadence; without this cache every open viewer fired several synchronous SQLite
 * queries per second. With it the queries run at most once per {@link #TTL_MS}
 * regardless of viewer count.
 *
 * <p>Thread-safety: the refresh task dispatches per-entity on Folia, so reads/writes
 * can come from region threads — the snapshot swap is a single volatile write of an
 * immutable object.
 */
final class EconomyStats {

    private static final long TTL_MS = 3000L;

    /** Immutable snapshot of every cached metric. */
    static final class Snapshot {
        final int accounts;
        final double supply;
        final List<EconomyService.BalanceEntry> top;
        final List<EconomyService.TransactionEntry> recent;

        Snapshot(int accounts, double supply,
                 List<EconomyService.BalanceEntry> top,
                 List<EconomyService.TransactionEntry> recent) {
            this.accounts = accounts;
            this.supply = supply;
            this.top = top;
            this.recent = recent;
        }
    }

    private static final Snapshot EMPTY = new Snapshot(-1, -1,
            Collections.<EconomyService.BalanceEntry>emptyList(),
            Collections.<EconomyService.TransactionEntry>emptyList());

    private static volatile Snapshot cached = EMPTY;
    private static volatile long fetchedAt;

    private EconomyStats() {
    }

    /** Cached metrics, refreshed from SQLite at most once per {@link #TTL_MS}. */
    static Snapshot get(EconomyService economy) {
        if (economy == null) {
            return EMPTY;
        }
        long now = System.currentTimeMillis();
        Snapshot snapshot = cached;
        if (now - fetchedAt < TTL_MS && snapshot != EMPTY) {
            return snapshot;
        }
        synchronized (EconomyStats.class) {
            now = System.currentTimeMillis();
            if (now - fetchedAt < TTL_MS && cached != EMPTY) {
                return cached;
            }
            cached = new Snapshot(
                    economy.accountCount(),
                    economy.totalSupply(),
                    economy.topBalances(3),
                    economy.recentTransactions(null, 3));
            fetchedAt = now;
            return cached;
        }
    }
}