# Economy Round 11 — Verification Gap Closures

■ **Created:** 2026-06-07 9:25 am

■ **Last Updated:** 2026-06-07 4:10 pm

A verification pass over the Round 9/10 feature claims found seven features that were
partially implemented, unreachable, or silently inert. This change closes all seven and
fixes two latent bugs discovered along the way. Full breakdown (with file links and patch
notes) in the commit log:
[docs/commits/2026-06-07/economy-round-11-gap-closures.md](../commits/2026-06-07/economy-round-11-gap-closures.md)

## Categories

### GUIs (Shop Editor — now functional)
- `/shop admin` editor gained real click handling: left/right-click sets buy/sell price via
  chat prompt, shift-click removes an item, emerald adds the held item or a new category.
- Every edit persists to `shop.yml` / `shops/<file>.yml` immediately and reloads the catalogue.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java` *(new)*
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopService.java`

### Economy (Server account — sink revenue made visible)
- AH sales tax + listing fees, anvil repair fees, and claim upkeep now deposit to a server
  account instead of being burned; `/eco server` shows balance + 7-day source breakdown,
  `/eco server withdraw <amount>` recovers funds (audited). Excluded from /baltop & supply.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ServerAccountService.java` *(new)*
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java`
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/RepairFeeListener.java`

### Commands (Auction entry points)
- `/ah search <text>`, `/ah confirm [id]` (the gate previously pointed at a nonexistent
  subcommand), `/ah bid <id> <amount>`, `/ah auction <startingBid> [buyout] [category]`,
  `/ah sell <price> [category]`.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionCommand.java`

### Reporting (Digest completeness)
- Weekly digest now includes top movers (7-day audit-log balance swings) and biggest sinks
  (server-account revenue per source); Discord webhook posts async; `/eco digest` on demand.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/report/EconomyReportService.java`
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/EcoCommand.java`

### Items (Banknote craft redemption)
- `economy.banknote.craft-redeem` (default on): banknotes alone in a crafting grid show a
  Redeem result; clicking cashes all notes via the guarded token path. Replaces the old
  `craft-recipe`, which produced a valueless blank paper item.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/listener/BanknoteListener.java`
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BanknoteService.java`

### Economy (Bank rank tiers + history fix)
- `economy.bank.tiers` entries accept `permission:` (rank tiers) alongside `min-balance`;
  highest matching rate wins; GUI shows the effective rate.
- **Fix:** bank GUI history queried nonexistent columns (`type`/`timestamp`/`uuid`) — it
  never showed anything; now reads `action`/`ts`/`target_uuid`.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/service/BankService.java`
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/bank/BankMenu.java`

### Economy (Claim upkeep sink)
- Daily `fee-per-claim` for online claim owners (≤7 days backlog), routed to the server
  account. **Documented assumption:** OBX has no claims module — integrates with
  GriefPrevention via reflection when installed; idles otherwise. `obx.upkeep.exempt` skips.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/sink/ClaimUpkeepService.java` *(new)*
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/EconomyModule.java`

### Config
- `plugin/src/main/resources/config.yml` — `craft-redeem`, `claim-upkeep.*`, bank
  `permission` tier docs, AH fee/tax comments.

### Permissions
- `plugin/src/main/resources/plugin.yml` — `obx.upkeep.exempt`, `obx.bank.tier.*` marker;
  `/eco`, `/shop`, `/withdraw`, `/ah` usage lines.

### Internal (i18n)
- 33 new keys × EN/DE/ES (`shop.editor.*`, `economy.ah.*`, `economy.eco.server.*`,
  `economy.note.craft-*`, `economy.sink.upkeep.*`) + updated `/ah` and `/eco` usage.
- **Fix:** EN `economy.note.withdrawn-all` rendered `¬es` (mangled `&anotes`).
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

### Storage (auction migration boot warnings — fix)
- **Fix:** the auction schema migration ran `ALTER TABLE auction_listings ADD COLUMN …`
  unconditionally, logging three `duplicate column name` WARNs on every boot against an
  existing database. `addColumnIfMissing` now consults `pragma_table_info` first (same
  pattern as `PlaytimeService.ensureColumn`) and only alters when the column is missing.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/auction/AuctionService.java`

### Docs
- `docs/information/COMMANDS+PERMISSIONS.md` — `/eco`, `/shop`, `/withdraw`, `/bank`, `/ah`
  rows updated; new permission rows.

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE/ES parity) pass; both jars produced.

## Suggested Commit Message
```
Economy round 11: close verification gaps — live shop editor, server account, /ah entry points, digest movers+sinks, craft redemption, rank tiers, claim upkeep
```
