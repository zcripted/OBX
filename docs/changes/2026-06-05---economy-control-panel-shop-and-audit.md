# Economy Phases 1–3: Control Panel, Audit Log, Offline Admin, Confirm Steps, Shop GUI, Version-Wide Worth DB

■ **Created:** 2026-06-05 8:28 pm

■ **Last Updated:** 2026-06-05 8:28 pm

## Summary

The Admin GUI's placeholder Economy tile is now a full **Economy Control Panel**, the
economy gained a persistent **transaction audit log** with **offline-player admin
actions** and **confirmation steps**, and a complete **EcoShop-style player shop**
(`/shop`) shipped with YAML-driven categories plus a **version-aware worth.yml item
database spanning 1.8.8 → latest**.

### Phase 1 — Economy Control Panel (Admin GUI → Economy)
- New `AdminSubMenu` ECONOMY view (27 slots), every tile carrying LIVE data:
  - **Balance Top** (GOLD_BLOCK) — top-3 balances + account count in the lore; click runs `/baltop`.
  - **Economy Overview** (EMERALD) — total money supply, accounts, average, starting balance; click refreshes.
  - **Manage Balances** (PLAYER_HEAD) — click opens a chat action panel with click-to-suggest
    `[Give] [Take] [Set] [Reset] [Log]` buttons prefilling `/eco …` (offline-capable).
  - **Sell Prices** (HOPPER) — live count of priced materials on THIS version; click → confirm → reload `worth.yml`.
  - **Recent Transactions** (WRITABLE_BOOK) — latest 3 audit entries in the lore; click runs `/eco log`.
  - **Shop Manager** (EMERALD_BLOCK) — category count; left-click opens `/shop`, right-click → confirm → reload shop YAML.
  - **Currency Settings** (SUNFLOWER) — symbol / names / starting balance info card.
- Live items re-render via the existing `AdminMenuRefreshTask` (`refresh()` ECONOMY case).
- Admin tile lore updated to the completed-tile style:
  *"Balances, payouts, shop, sell prices, and transaction logs."*
- New `EconomyService` **default methods** (non-breaking API): `accountCount()`,
  `totalSupply()`, `findAccount(name)`, `logTransaction(...)`, `recentTransactions(...)`
  + `TransactionEntry`; all overridden by the SQLite impl.

### Phase 2 — Audit log · offline admin actions · confirmation steps
- New SQLite table **`economy_log`** (ts, actor, target, action, amount, balance_after).
  Writers: `/eco give|take|set|reset` (actor = staff), `/pay` (PAY + RECEIVE, one row per
  side), `/sell` + `/sellall` (SELL), shop trades (SHOP_BUY / SHOP_SELL), sell-GUI payouts.
- **`/eco log [player] [count]`** — newest-first box-style report (defaults 10, cap 50).
- **Offline-player admin actions** — `/eco` (and the GUI's action panel) resolves targets
  through the usercache first, then the economy table's last-known name
  (`findAccount`, case-insensitive), so any player who ever held an account is manageable offline.
- **Reusable confirmation menu** — `AdminSubMenu.openConfirmMenu(...)` (green Confirm /
  info card / red Cancel; actions carried on the holder); used by the panel's
  worth-reload and shop-reload actions.

### Phase 3 — Shop GUI · category YAML · version-wide worth DB
- New `features/economy/shop` package: `ShopService`, `ShopMenu`, `ShopListener`, `ShopCommand`.
- **`/shop`** (alias `/market`): main category menu from **`shop.yml`** (title, rows,
  per-category slot/icon/name/lore/file) + per-category files **`shops/<id>.yml`**
  (title + items: material, amount bundle, buy, sell). Bundled defaults: **blocks, ores,
  farming, food, mobdrops, redstone** — installed once via `saveResource`, never overwritten.
- Category view: 54 slots, 45 items/page with pagination; **left-click buy bundle,
  shift-left buy stack, right-click sell bundle, shift-right sell all carried**; live
  balance card; pay-first / remove-first dupe guards on both directions.
- **Sell Items inventory** (`/shop sell` or the main-menu tile): dump items, close to sell
  everything `worth.yml` prices; unsellable items returned (overflow dropped).
- **Version safety:** every shop/worth material resolves through `Material.matchMaterial`
  at load; unknown names are skipped (and summarized in console) — one catalog serves
  **1.8.8, 1.13, 1.19, 1.21, and beyond**.
- **worth.yml** expanded from 13 entries to a ~230-entry item database organized by version
  era (1.8.8 baseline → 1.13 → 1.14/15 → 1.16 → 1.17/18 → 1.19 → 1.20 → 1.21+), including
  a legacy (pre-1.13) name-alias section (`log`, `sulphur`, `raw_fish`, `ink_sack`, …) so
  1.8.8–1.12.2 servers price the same items.

## Files

### API
- `api/src/main/java/dev/zcripted/obx/api/economy/EconomyService.java` — stats/lookup/log default methods + `TransactionEntry`.

### Economy feature
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/EconomyServiceImpl.java` — `economy_log` table + stats/lookup/log impls.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/WorthService.java` — `pricedCount()` (version-aware).
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/EcoCommand.java` — offline resolution, audit writes, `log` subcommand.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java` · `SellCommand.java` — audit writes.
- **New** `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java` · `ShopMenu.java` · `ShopListener.java` · `ShopCommand.java`.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java` — shop service/listener/command registration.

### Staff feature (Admin GUI)
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java` — ECONOMY + CONFIRM types, panel, confirm menu, refresh case.
- `features/staff/build.gradle.kts` — `:features:economy` dependency.
- `plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java` — ECONOMY/CONFIRM click routing.

### Resources & config
- **New** `plugin/src/main/resources/shop.yml` + `shops/{blocks,ores,farming,food,mobdrops,redstone}.yml`.
- `plugin/src/main/resources/worth.yml` — version-wide item database rewrite.
- `plugin/src/main/resources/plugin.yml` — `shop` command (alias `market`); permissions
  `obx.shop` (true), `obx.shop.sell` (true), `obx.shop.admin` (op), `obx.shop.*` bundle (+ `obx.*` child).

### Messages (EN/DE/ES in lock-step)
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaults{EN,DE,ES}.java`
  — ~65 new keys per language (`admin.gui.eco.*`, `admin.gui.confirm.*`,
  `economy.eco.log.*`, `shop.*` incl. GUI nav/lore templates) + new `shop` section
  comment; `admin.menu.item.economy.lore` rewritten.

### Docs
- `docs/information/COMMANDS+PERMISSIONS.md` — `/shop` row; `/eco`, `/pay`, `/sell`
  descriptions updated (audit/offline/version DB); `obx.shop.*` wildcard row.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**: all modules compile (staff↔economy dep
  acyclic), `MessageDefaultsTest` EN/DE/ES parity green with the new key set, both jars produced.

## Suggested Commit Message
```
Feature (economy): control panel GUI, audit log + offline /eco, confirm steps, /shop with YAML categories, version-wide worth DB
```
