# 🎛 Economy Tile Theme + Panel Restructure

> The Admin-Menu Economy tile now shows a live, categorized hover preview in the same
> theme as Server Control / Warp Manager / Fun Utilities, and the Economy Control panel
> was restructured: action tiles consolidated to slots 10–14, Shop centered at 22, and
> Back/Close moved to their own bottom row.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-06 7:55 AM EST |
| **Last Updated** | 2026-06-06 3:51 PM EST |
| **Author** | zcripted |
| **Scope** | Admin GUI — Economy tile + Economy Control layout |
| **Files changed** | 6 modified |
| **Categories** | GUIs · Messages/i18n |
| **Verification** | ✅ `gradlew build` green · EN/DE/ES parity green |

---

## 📋 Summary (patch notes)

- **Economy tile, now in the house style** — hovering Economy in the Admin Menu shows the
  same clean preview design as the other finished tiles: a header line, `▸ Balances`
  (accounts / total supply / average) and `▸ Market` (top balance / shop categories /
  sell prices) sections with live values, a divider, and a click hint. It updates while
  the menu is open, exactly like the Server Control tile.
- **Economy Control reorganized** — the five action tiles now sit together on row two
  (Balance Top, Overview, Manage Balances, Sell Prices, Transactions at 10–14), the Shop
  Manager is centered below them (22), and Back/Close moved to their own dedicated
  bottom row (30/32) in a roomier 36-slot menu.

> **3:51 PM update:** the action tiles were manually re-centered to slots **11–15**;
> the click dispatcher and refresh task were realigned to match (a Round-7 audit caught
> the desync — clicks were firing the wrong actions). Final layout: 4 currency ·
> 11 Top · 12 Overview · 13 Manage · 14 Sell Prices · 15 Transactions · 22 Shop ·
> 30 Back · 32 Close.

## 🔄 Changes

- [`AdminMenu.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenu.java)
  — `buildEconomyItem`/`economyTileLore` (live themed preview, shared row/divider keys,
  45-char truncation), wired into `open()` + `refreshLive()`.
- [`AdminSubMenu.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java)
  — panel now 36 slots; tiles remapped 12/14/16/20→11/12/13/14, shop 24→22; new
  `ECONOMY_BACK_SLOT`/`ECONOMY_CLOSE_SLOT` (30/32); click + refresh cases updated.
- [`MainMenuListener.java`](../../../plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java)
  — ECONOMY nav routing uses the dedicated slots (generic BACK_SLOT 22 now hosts the Shop tile).
- [`MessageDefaultsEN/DE/ES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
  — new `admin.ecp.*` keys (12 per language).

### 📄 Change file
- [`docs/changes/2026-06-06---economy-tile-theme-and-panel-restructure.md`](../../changes/2026-06-06---economy-tile-theme-and-panel-restructure.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
UI (economy): live themed Admin-Menu tile + Economy Control panel restructure (nav row, tiles 10–14, shop centered)
```
