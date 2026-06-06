# Economy Tile Live-Preview Theme + Economy Control Panel Restructure

■ **Created:** 2026-06-06 7:55 am

■ **Last Updated:** 2026-06-06 7:55 am

## Summary

### Economy tile (Admin Menu, slot 21) — themed live preview
The Economy tile now renders the same clean categorized-preview hover design as the
Server Control / Warp Manager / Fun Utilities tiles, with LIVE values:

```
&7Live snapshot of the economy.

&d&l▸ &dBalances
  &7Accounts      &8» &f128
  &7Total supply  &8» &f$1,204,330.50
  &7Average       &8» &f$9,408.83

&d&l▸ &dMarket
  &7Top balance      &8» &fNotch ($88,120.00)
  &7Shop categories  &8» &f6
  &7Sell prices      &8» &f203

&8──────────────────────
&eClick: &7Open Economy Control
```

- Built by new `AdminMenu.buildEconomyItem(...)` / `economyTileLore(...)` (mirrors
  `buildServerControlItem`), rows reuse the shared `admin.scp.row` format and
  `admin.scp.footer` divider so the theme is pixel-identical; 45-char visible cap with
  word-boundary truncation applied like the SCP tile.
- Refreshes live with the open Admin Menu via `refreshLive(...)` (same cadence as
  Server Control). Cheap queries only (count/sum/top-1); `—` placeholders when a
  service is unavailable.
- New keys `admin.ecp.*` (header, none, cat.balances, cat.market, six labels, click)
  in EN/DE/ES lock-step.

### Economy Control panel — restructured to 36 slots, nav on its own row
- Row 1: Currency card (4) · Row 2: **Balance Top 10 · Overview 11 · Manage 12 ·
  Sell Prices 13 · Transactions 14** (previously 12/14/16/20) · Row 3: **Shop Manager
  centered at 22** (previously 24) · Row 4 (nav-only): **Back 30 · Close 32**.
- New constants `AdminSubMenu.ECONOMY_BACK_SLOT` (30) / `ECONOMY_CLOSE_SLOT` (32);
  `MainMenuListener`'s ECONOMY case routes them and deliberately does NOT treat the
  generic `BACK_SLOT` (22) as back anymore — slot 22 hosts the Shop tile there.
- Click dispatch + the refresh-task ECONOMY case remapped to the new slots.

## Files
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenu.java`
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`
- `plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaults{EN,DE,ES}.java`

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** (EN/DE/ES parity green); both jars produced.

## Suggested Commit Message
```
UI (economy): live themed Admin-Menu tile + Economy Control panel restructure (nav row, tiles 10–14, shop centered)
```
