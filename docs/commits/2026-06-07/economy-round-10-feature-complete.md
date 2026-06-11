# 💰 Economy Round 10 — Feature-Complete: Edge Cases, Exploits, Bank GUI, Shop Stock, Sinks

> All requested economy features are now implemented and hardened. The remaining edge-case
> discoveries from the adversarial review are closed (auction-house storage race, bank
> atomicity, claim item-loss on full inventory), shop stock limits + dynamic pricing are
> wired end-to-end with YAML config and restock timers, PAPI placeholders are cached to
> prevent lag, Vault unregisters cleanly on disable, and three craft-arbitrage money
> printers (golden carrot, baked potato, cookie) are price-fixed. New additions: bank GUI,
> sell wand, shop editor menu, weekly-top sink, anvil-repair fee sink, economy report
> service. Build green, EN/DE/ES parity holds.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 8:38 AM EST |
| **Last Updated** | 2026-06-07 8:38 AM EST |
| **Author** | zcripted |
| **Scope** | Economy module — auction, bank, shop, sell, pay, PAPI, config, i18n, docs |
| **Files changed** | 25 (18 modified · 7 new) |
| **Categories** | Edge cases · Exploits · Feature completion · Fix · Threading · GUIs · Storage · Config · i18n · Docs |
| **Verification** | ✅ `gradlew build` green · EN/DE/ES parity green |

---

## 📋 Summary (patch notes)

- **Auction house hardened.** The storage-race bug that could silently destroy a listed item
  is fixed — the listing fee is refunded and the item returned on failure. Claiming no
  longer loses items to corrupted records or withholds money on full inventory. Staff can
  shift-click to cancel any listing via `obx.ah.admin`.
- **Bank deposits are now atomic.** A failed deposit (wallet-at-cap, storage error) restores
  the wallet instead of losing money between pocket and vault.
- **Shop stock limits are fully wired.** Items can have finite stock with configurable
  `restock-minutes` timers. The GUI shows remaining stock and next restock time. Dynamic
  pricing adjusts buy/sell multipliers based on trade volume, with sell-side capped at
  base to block self-pump arbitrage. Both features driven from `shop.yml` + `shops/*.yml`.
- **PAPI `%obx_balance%` now cached** (2s per-UUID) — no more DB hits every frame.
- **Vault economy unregisters cleanly** on plugin disable (no orphaned provider).
- **Three craft-arbitrage money printers closed** — golden carrot, baked potato, and
  cookie buy prices adjusted so crafting them for profit yields zero or negative return.
- **Pay confirm threshold + tax.** Payments at/above `economy.pay.confirm-threshold`
  require `/pay confirm` within 30s. Optional transfer tax (`tax-percent`) is burned as
  a money sink.
- **Daily sell cap.** `economy.sell.daily-cap` limits total daily sell revenue per player,
  reset at midnight UTC. Refuses (never clamps) when the wallet is at the cap.
- **Rank sell multipliers.** `obx.sell.multiplier.<n>` grants a payout bonus (clamped
  0.1–10×). The highest granted multiplier applies.
- **Economy log with action filter.** `/eco log [player|*] [page] [action]` shows audit
  entries filtered to one action type (GIVE/TAKE/SET/PAY/SELL/…). Retention = `log-retention-days`.
- **Bank GUI** (`/bank` now opens a live menu with balance, deposit/withdraw buttons,
  interest rate display and transaction history).
- **Sell wand.** Chest-click with `obx.shop.sellwand` to sell all matching items directly
  from the chest.
- **Shop editor menu.** Admin GUI (`/shop edit`) for live shop config editing (Phase 10).
- **Economy sinks.** Weekly baltop snapshot (`WeeklyTopService`) and anvil repair fee
  (`RepairFeeListener`) as money sinks.
- **Economy report service.** Tracks AH tax, pay tax, anvil fees, and bank stats for
  server-economy monitoring.
- **i18n parity.** All new messages have EN/DE/ES translations.

## 🔧 Changes (newest → oldest)

### New files

| File | What |
|------|------|
| `features/economy/…/bank/BankMenu.java` | Interactive `/bank` GUI (balance, deposit, withdraw, interest info, history) |
| `features/economy/…/bank/BankMenuListener.java` | Click handling for bank GUI |
| `features/economy/…/shop/SellWandListener.java` | Chest-click sell wand (`obx.shop.sellwand`) |
| `features/economy/…/shop/ShopEditorMenu.java` | Admin in-game shop editor GUI |
| `features/economy/…/sink/WeeklyTopService.java` | Weekly baltop snapshot economy sink |
| `features/economy/…/sink/RepairFeeListener.java` | Anvil repair fee economy sink |
| `features/economy/…/report/EconomyReportService.java` | Tracks AH tax, pay tax, anvil fees, bank stats |

### Modified files

| File | What changed |
|------|-------------|
| `features/economy/…/EconomyModule.java` | Wire new services (bank GUI, sell wand, sinks, report) + payday + daily cap + boost |
| `features/economy/…/service/EconomyServiceImpl.java` | Log retention prune logic, vault unregister on disable |
| `features/economy/…/service/BankService.java` | Atomic deposit/withdraw, wallet-at-cap refusal, interest accrual |
| `features/economy/…/service/BanknoteService.java` | Token-based banknote redemption |
| `features/economy/…/service/SellBoost.java` | `obx.sell.multiplier.<n>` rank multiplier lookup + clamp |
| `features/economy/…/service/SellLimitTracker.java` | Daily sell cap per-player (midnight UTC reset) |
| `features/economy/…/auction/AuctionService.java` | Storage-race fix, confirm threshold, admin cancel, claim item-loss fix |
| `features/economy/…/auction/AuctionListener.java` | Shift-click admin cancel in browse GUI |
| `features/economy/…/auction/AuctionMenu.java` | Claim flow hardening, admin cancel UI |
| `features/economy/…/command/BalTopCommand.java` | Weekly baltop toggle support |
| `features/economy/…/command/BankCommand.java` | Wire bank GUI, interest display |
| `features/economy/…/command/WithdrawCommand.java` | Banknote token flow |
| `features/economy/…/shop/ShopService.java` | Finite stock + restock timer + dynamic pricing wiring |
| `features/economy/…/shop/ShopMenu.java` | Stock display in GUI, restock countdown |
| `features/economy/…/shop/ShopListener.java` | Stock enforcement on buy, daily cap messaging |
| `features/economy/…/shop/ShopCommand.java` | Admin reload, shop editor entry |
| `core/…/language/MessageDefaultsEN.java` | All new EN language keys (sell, pay, bank, ah, wand, …) |
| `core/…/language/MessageDefaultsDE.java` | All new DE language keys |
| `core/…/language/MessageDefaultsES.java` | All new ES language keys |
| `plugin/src/main/resources/config.yml` | New economy config keys (daily-cap, confirm-threshold, tax-percent, bank, payday, log-retention-days, sinks) |
| `plugin/src/main/resources/plugin.yml` | New permissions: `obx.ah.admin`, `obx.ah.confirm.bypass`, `obx.shop.sellwand`, `obx.shop.category.*`, `obx.baltop.weekly`, `obx.payday` |

## 📝 Full file paths

```
M  core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java
M  core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java
M  core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionListener.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionMenu.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/BalTopCommand.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/BankCommand.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/WithdrawCommand.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BankService.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BanknoteService.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopCommand.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopMenu.java
M  features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java
M  plugin/src/main/resources/config.yml
M  plugin/src/main/resources/plugin.yml
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/bank/BankMenu.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/bank/BankMenuListener.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/report/EconomyReportService.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/SellWandListener.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorMenu.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/WeeklyTopService.java
?? features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/RepairFeeListener.java
```
