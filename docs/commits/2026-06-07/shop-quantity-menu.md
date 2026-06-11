# 🧮 Shop Quantity Menu — Per-Item Buy/Sell Submenu with ±1/5/10/30/50 Steps

> Clicking an item in a shop category now opens a dedicated **quantity menu** for that
> item instead of instantly trading one bundle. The 5-row view centers the item with a
> live receipt (mode, amount, effective unit price, colored total, balance, stock),
> puts a green **+1/+5/+10/+30/+50** row above it and a red **−1/−5/−10/−30/−50** row
> below it (button stack sizes mirror their step), and a bottom nav row:
> **Back · Mode toggle (Buy ⇄ Sell) · Confirm · Balance · Close**. Confirm executes
> through the existing guarded transaction paths — stock clamps, daily sell cap,
> dupe-proof remove-first selling, audit logging, and the hover-receipt chat messages
> all apply unchanged. Shift-click shortcuts survive as quick actions
> (shift-left = buy a full stack, shift-right = sell everything carried). EN/DE/ES.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 4:57 PM EST |
| **Last Updated** | 2026-06-07 4:57 PM EST |
| **Author** | zcripted |
| **Scope** | Economy — shop GUI (new QUANTITY view), i18n |
| **Files changed** | 5 code + 2 docs |
| **Categories** | Feature · GUIs · i18n |
| **Verification** | ✅ `gradlew build` green (tests incl. EN/DE parity, both jars) |

---

## 📋 Summary (patch notes)

- **Pick exactly how many you trade.** Click any shop item (left = buy mode,
  right = sell mode) to open its quantity menu: bump the amount up or down by 1, 5, 10,
  30, or 50, flip between buying and selling with one click, and watch the centered item
  tile recalculate the receipt live — effective unit price (dynamic pricing and sell
  boosts included), colored total (`-cost` red / `+gain` green), your balance, and the
  remaining stock for finite-stock items.
- **Confirm is safe.** The Confirm button routes through the exact same guarded code as
  before: out-of-stock denial before money moves, pay-first buying, remove-first selling,
  daily sell caps, audit log entries, and the chat receipt with its hover breakdown.
  After a trade the menu re-renders in place (new balance/stock/totals) so you can chain
  purchases without reopening.
- **Quick actions kept.** Shift-left still instantly buys a full stack; shift-right
  still instantly sells everything you carry. Item lore hints updated to teach both flows.

## 🔧 Changes (newest at top → oldest)

### Shop GUI (new QUANTITY view)
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopMenu.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopMenu.java)
  — `ViewType.QUANTITY` + slot constants (`QTY_*`, steps `{1,5,10,30,50}`, amount cap
  2304); `Holder` gains `itemIndex` + mutable `quantity`/`buying`; `openQuantity` /
  `renderQuantity` (in-place redraw); `effectiveUnitPrice` centralizes the
  per-unit math (bundle÷size × dynamic multiplier; sell × boost) shared with the
  transaction paths; `refreshBalance` redraws the whole QUANTITY view after a trade.
  1.12-safe button materials with fallbacks (lime/red stained glass → emerald/redstone).
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java)
  — `handleQuantityClick` (± steps, mode toggle with not-buyable/not-sellable guards,
  confirm, back, close); category plain-clicks open the quantity menu, shift-clicks keep
  the quick paths; `buy`/`sell` refactored into exact-unit `buyUnits`/`sellUnits`
  (identical math/guards, now reusable by both flows).

### i18n (EN/DE/ES — 17 new keys each + 2 updated hints)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `shop.gui.qty.*` (title, item name/lore receipt, mode labels, ± button name/lore,
  toggle, confirm lore-buy/lore-sell, back lore); `shop.gui.item.hint-buy`/`hint-sell`
  reworded for the new click flow.

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---shop-quantity-menu.md](../../changes/2026-06-07---shop-quantity-menu.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`).
