# 💰 Economy Round 11 — Verification Gap Closures: Shop Editor Live, Server Account, /ah Entry Points, Real Craft Redemption

> A line-by-line verification of the Round 9/10 feature claims found seven gaps — features
> that existed as backends or GUIs but were unreachable, silently inert, or only half of
> what their docs promised. This round closes all seven: the shop editor GUI now actually
> edits and persists, auction tax/fees flow to a **visible server account** instead of
> being burned, `/ah` gains `search`/`confirm`/`bid`/`auction` entry points, the weekly
> digest gains its promised top movers + biggest sinks sections, banknote craft redemption
> is real (the old recipe produced a valueless blank paper), bank interest gains rank
> tiers, and a claim-upkeep sink lands (GriefPrevention integration). Plus two latent bug
> fixes found along the way. Build green, EN/DE/ES parity holds.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 9:25 AM EST |
| **Last Updated** | 2026-06-07 4:10 PM EST |
| **Author** | zcripted |
| **Scope** | Economy module — shop editor, auction, bank, banknotes, sinks, reporting, i18n, config, docs |
| **Files changed** | 19 code (3 new) + 3 docs |
| **Categories** | Feature completion · Fix · GUIs · Commands · Permissions · Storage · Config · i18n · Docs |
| **Verification** | ✅ `gradlew build` green (tests + shadowJar + ProGuard, both jars) · EN/DE/ES parity green |

---

## 📋 Summary (patch notes)

- **The shop editor is now a real editor.** `/shop admin` previously rendered a GUI whose
  lore promised "click to set price / shift-click to remove" — but no click handler existed
  and nothing could save. Now: left/right-click an item to set its buy/sell price via a
  chat prompt, shift-click removes it, the emerald adds the item in your hand or a new
  category, and **every edit writes straight to `shop.yml` / `shops/*.yml`** and reloads —
  no YAML hand-editing, no unsaved-draft state to lose.
- **Sink money is visible now.** Auction sales tax and listing fees used to vanish (the
  tax was computed and simply never deposited anywhere). They now flow — along with anvil
  repair fees and claim upkeep — into a **server account** admins can inspect with
  `/eco server` (balance + 7-day per-source breakdown) and recover with
  `/eco server withdraw <amount>` for prize pools. Deliberately kept out of `/baltop`
  and total-supply stats; every deposit is audit-logged.
- **The auction house's hidden features are reachable.** Search had a full backend but no
  way to start one (`/ah search <text>` now opens the filtered browse GUI); the
  confirmation gate told players to type `/ah confirm` — a subcommand that didn't exist
  (it does now); bidding gains `/ah bid <id> <amount>`; and auction-style listings can
  finally be created: `/ah auction <startingBid> [buyout] [category]`, with categories
  also accepted on `/ah sell <price> [category]`.
- **The weekly digest now reports what its docs promised.** Top movers (biggest 7-day
  balance swings, from the audit log) and biggest sinks (server-account revenue per
  source) join total supply, top balances, and bank stats. The Discord webhook now posts
  **async** (it previously blocked the main thread), and `/eco digest` triggers a report
  on demand.
- **Banknote craft redemption is real.** The old `craft-recipe` produced a *blank, valueless
  paper item* — a trap. Replaced by `craft-redeem` (default on): put banknotes alone in any
  crafting grid, a gold-nugget "Redeem" result appears, and clicking it cashes every note
  at once through the same dupe-proof guarded path as right-click redemption.
- **Bank interest understands ranks.** `economy.bank.tiers` entries may now name a
  `permission` (e.g. `obx.bank.tier.vip`) instead of a `min-balance`; the highest matching
  rate wins. The bank GUI now shows your *effective* rate (it previously always showed the
  flat config default).
- **Claim upkeep sink.** Daily `fee-per-claim` charged to online claim owners (up to 7 days
  of backlog), collected by the server account. **Assumption (documented):** OBX has no
  claims module, so this integrates with **GriefPrevention** via reflection when installed;
  without it the sink idles with a single console note. Non-destructive by design: players
  who can't pay are warned, never auto-unclaimed. `obx.upkeep.exempt` opts a player out.

### 🐛 Latent bugs found & fixed during verification
- **Bank GUI history never worked**: `BankService.history()` queried columns
  (`type`, `timestamp`, `uuid`) that don't exist in `economy_log`
  (`action`, `ts`, `target_uuid`). The panel silently showed no transactions. Fixed,
  with timestamps formatted for the history tiles.
- **`/withdraw all` message typo**: EN `economy.note.withdrawn-all` contained `¬es`
  (a mangled `&anotes`) — rendered garbage. Fixed.

---

## 🔧 Changes (newest at top → oldest)

### Auction (boot-warning fix, found in live server test)
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java)
  — `addColumnIfMissing` ran `ALTER TABLE … ADD COLUMN` unconditionally; on existing
  databases SQLite rejected the duplicate columns and the store logged three
  `SQL update failed … duplicate column name` WARNs every boot (the local try/catch never
  fired because `SqliteDataStore.executeUpdate` swallows and logs internally). Now checks
  `pragma_table_info` first and only alters when the column is genuinely missing — same
  pattern as `PlaytimeService.ensureColumn`. Boot is warning-free on both fresh and
  migrated databases.

### Docs
- Updated [docs/information/COMMANDS+PERMISSIONS.md](../../information/COMMANDS+PERMISSIONS.md):
  `/eco` (server/digest), `/shop` (editor + category perms + corrected sell-wand text and
  `admin` subcommand name), `/withdraw` (all + denominations + craft redemption), `/bank`
  (gui + tiers), `/ah` (full new subcommand set), new `obx.upkeep.exempt` and
  `obx.bank.tier.<name>` permission rows.

### Config & plugin.yml
- [plugin/src/main/resources/config.yml](../../../plugin/src/main/resources/config.yml):
  `economy.banknote.craft-redeem` replaces `craft-recipe`/`craft-amount`;
  `economy.sinks.claim-upkeep.{enabled, fee-per-claim}` added; bank tier docs gain the
  `permission` form; AH listing-fee/tax comments note server-account collection.
- [plugin/src/main/resources/plugin.yml](../../../plugin/src/main/resources/plugin.yml):
  `obx.upkeep.exempt` + `obx.bank.tier.*` marker permissions; updated usage lines for
  `/eco`, `/shop`, `/withdraw`, `/ah`.

### i18n (EN/DE/ES — 33 new keys each, parity enforced by MessageDefaultsTest)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `shop.editor.*` prompts/results, `economy.ah.*` (bad-id, confirm-none, auction-listed,
  buyout-too-low, no-buyout-label, new usage), `economy.eco.server.*`, `economy.eco.digest-sent`,
  `economy.note.craft-*`, `economy.sink.upkeep.*`; EN `withdrawn-all` typo fix; `/eco` usage
  gains log/server/digest rows.

### Reporting
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/report/EconomyReportService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/report/EconomyReportService.java)
  — top movers (last-vs-first `balance_after` per player over 7 days, server row excluded),
  biggest sinks (server-account `sourceTotals`), server-account balance line, async webhook.

### Sinks (new package members)
- **NEW** [features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ServerAccountService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ServerAccountService.java)
  — single-row `economy_server_account` table; `deposit(actor, action, amount)` with audit
  rows targeting the nil UUID; guarded `withdrawTo`; `sourceTotals(since)`.
- **NEW** [features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ClaimUpkeepService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ClaimUpkeepService.java)
  — `economy_upkeep` anchor table, hourly sweep / daily charge, GriefPrevention claim count
  via reflection, exempt permission, server-account routing, `CLAIM_UPKEEP` audit action.
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/RepairFeeListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/RepairFeeListener.java)
  — anvil fee now deposits to the server account.

### Auction
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java)
  — sales tax (`buy` + `buyoutListing`) and listing fee routed to the server account
  (`AH_TAX`, `AH_LISTING_FEE`); `pendingConfirmId()` for `/ah confirm`.
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionCommand.java)
  — new subcommands `search`, `confirm [id]`, `bid <id> <amount>`,
  `auction <startingBid> [buyout] [category]`; `sell` accepts a category; buyout-vs-bid
  validation; tab completion.

### Bank
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BankService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BankService.java)
  — `effectiveRate(balance, owner)` with `permission` rank tiers; accrual passes the owner;
  **history() column-mismatch fix** (the GUI history was dead).
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/bank/BankMenu.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/bank/BankMenu.java)
  — Account Summary shows the effective (tiered) rate.

### Banknotes
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BanknoteService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BanknoteService.java)
  — `craftRedeemEnabled()` + `tokenValue()`; removed `registerCraftRecipe()` (produced a
  valueless blank note).
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/listener/BanknoteListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/listener/BanknoteListener.java)
  — `PrepareItemCraftEvent` preview (pure-banknote grids only) + `CraftItemEvent`
  redemption through the guarded token path; tagged synthetic result item; wallet-headroom
  stop leaves remaining notes intact.

### Shop editor
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java)
  — `ShopCategory.file()`; persistence writers `writePrice` / `addItem` / `removeItem` /
  `addCategory` (write YAML, then reload — GUI, `/shop`, and disk always agree).
- **NEW** [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java)
  — click dispatch for both editor views, chat-prompt price/category input (HIGHEST
  priority, Folia-safe main-thread apply via `runAtEntity`, quit cleanup, `cancel` keyword),
  drag cancel.

### Module wiring
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java)
  — registers `ServerAccountService`, `ClaimUpkeepService` (+ hourly sweep task),
  `ShopEditorListener`; drops the dead `registerCraftRecipe()` call.

### Admin command
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/EcoCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/EcoCommand.java)
  — `/eco server [withdraw <amount>]`, `/eco digest`; log-action filter list extended
  (AH_TAX, AH_LISTING_FEE, ANVIL_REPAIR, SELL_WAND, CLAIM_UPKEEP, SERVER_WITHDRAW, …);
  tab completion.

---

## ✅ Verification
- `.\gradlew.bat build` — **green**: all modules compile, `core`/`economy` tests pass
  (incl. MessageDefaults EN/DE/ES parity), `OBX-1.0.0-unobf.jar` + ProGuarded
  `OBX-1.0.0.jar` both produced.
