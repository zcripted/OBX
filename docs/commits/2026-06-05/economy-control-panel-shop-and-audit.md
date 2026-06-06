# 💰 Economy Phases 1–3 — Control Panel, Audit Log & Player Shop

> The Admin GUI's placeholder Economy tile became a live **Economy Control Panel**; the
> economy gained a persistent **transaction audit log**, **offline-player admin actions**,
> and **confirmation steps**; and a YAML-driven **/shop** (EcoShop-style) shipped with six
> bundled categories plus a **worth.yml item database spanning 1.8.8 → latest**.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 8:28 PM EST |
| **Last Updated** | 2026-06-05 8:28 PM EST |
| **Author** | zcripted |
| **Scope** | Economy feature build-out (admin GUI + audit + shop) |
| **Files changed** | 25 (12 new · 13 modified) |
| **Categories** | Feature · GUIs · Commands · Permissions · Storage · Config · Messages/i18n · Docs |
| **Verification** | ✅ `gradlew build` green · EN/DE/ES parity green · both jars produced |

---

## 📋 Summary (patch notes)

- **🎛 Economy Control Panel** — Admin GUI → Economy now opens a real panel where every
  tile shows live data: top-3 balances, total money supply / accounts / average, the
  number of priced sell materials on your server version, the latest audited
  transactions, and the shop category count. One click runs `/baltop`, opens a
  click-to-fill `/eco` action panel, reloads `worth.yml` or the shop (behind a
  green/red **confirmation menu**), or jumps into `/shop`.
- **🧾 Transaction audit log** — every money movement (admin give/take/set/reset, /pay
  both directions, /sell, shop buys & sells) is recorded in a new `economy_log` table.
  Inspect it with **`/eco log [player] [count]`** or the panel's Recent Transactions tile.
- **🌐 Offline admin actions** — `/eco` now resolves players the server has forgotten via
  the economy database's last-known name, so any account ever created is manageable.
- **🛒 /shop** — categorized buy/sell GUI (alias `/market`): six bundled categories
  (blocks, ores, farming, food, mob drops, redstone) defined in `shop.yml` +
  `shops/*.yml`, fully admin-editable and hot-reloadable. Left-click buys a bundle,
  shift-left a stack, right-click sells a bundle, shift-right everything carried — plus a
  dump-and-close **Sell Items** inventory. Pay-first / remove-first dupe guards both ways.
- **🗃 Version-wide worth database** — `worth.yml` grew from 13 to ~230 priced materials
  organized by version era (1.8.8 baseline through 1.21+), with legacy name aliases for
  1.8.8–1.12.2. Unknown materials are skipped per version — one file serves every
  supported server.

## 🔄 Changes

### 🎛 Admin GUI (staff feature)
- [`AdminSubMenu.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java) — ECONOMY + CONFIRM submenu types, live panel items, chat action panel, reusable confirm menu, refresh-task case.
- [`MainMenuListener.java`](../../../plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java) — click routing for both new types.
- [`features/staff/build.gradle.kts`](../../../features/staff/build.gradle.kts) — `:features:economy` dependency.

### 🧾 Audit + offline + API
- [`EconomyService.java`](../../../api/src/main/java/dev/zcripted/obx/api/economy/EconomyService.java) — non-breaking default methods: `accountCount`, `totalSupply`, `findAccount`, `logTransaction`, `recentTransactions` + `TransactionEntry`.
- [`EconomyServiceImpl.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/EconomyServiceImpl.java) — `economy_log` table + implementations.
- [`EcoCommand.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/EcoCommand.java) — offline resolution, audit writes, `/eco log`.
- [`PayCommand.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java) · [`SellCommand.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/SellCommand.java) — audit writes.

### 🛒 Shop (new `shop` package in the economy feature)
- **NEW** [`ShopService.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java) — YAML loading, version-safe material resolution, default installation.
- **NEW** [`ShopMenu.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopMenu.java) — main/category/sell views, pagination, live balance card.
- **NEW** [`ShopListener.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java) — buy/sell dispatch with dupe guards, sell-inventory payout.
- **NEW** [`ShopCommand.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopCommand.java) — `/shop [category|sell|reload]`.
- [`EconomyModule.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java) — registrations.

### 🗃 Resources & config
- **NEW** [`shop.yml`](../../../plugin/src/main/resources/shop.yml) + [`shops/`](../../../plugin/src/main/resources/shops) `blocks/ores/farming/food/mobdrops/redstone.yml`.
- [`worth.yml`](../../../plugin/src/main/resources/worth.yml) — version-era item database rewrite (~230 entries + legacy aliases).
- [`plugin.yml`](../../../plugin/src/main/resources/plugin.yml) — `shop` command + `obx.shop.*` permission family.

### 🌐 Messages (EN/DE/ES, ~65 new keys each)
- [`MessageDefaultsEN.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
  · [`MessageDefaultsDE.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
  · [`MessageDefaultsES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `admin.gui.eco.*`, `admin.gui.confirm.*`, `economy.eco.log.*`, `shop.*` (+ section comment);
  Economy tile lore rewritten in the completed style.

### 📚 Docs
- [`COMMANDS+PERMISSIONS.md`](../../information/COMMANDS+PERMISSIONS.md) — `/shop` row, `/eco`/`/pay`/`/sell` updates, `obx.shop.*` wildcard.
- [`Change file`](../../changes/2026-06-05---economy-control-panel-shop-and-audit.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; EN/DE/ES key parity green; both jars produced.

## Suggested Commit Message
```
Feature (economy): control panel GUI, audit log + offline /eco, confirm steps, /shop with YAML categories, version-wide worth DB
```
