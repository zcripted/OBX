# 🛒 Shop Result Hover Tooltips — Full Transaction Breakdown on Buy/Sell Messages

> The shop's chat confirmations ("Sold 3 stack(s) for +120 …", "Bought 64x Cobblestone …")
> were single flat lines. They now carry the OBX clean hover style (purple title bar,
> divider, `»` detail rows): hovering a result shows the item, amount, effective unit
> price, any active sell boost, the money gained/paid, the balance before → after, and —
> for finite-stock purchases — the remaining stock with the restock countdown. The bulk
> sell-inventory result additionally breaks down every sold item type (`● Item ×N
> (+value)`, up to 8 with an "…and N more" overflow line). EN/DE/ES.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 4:35 PM EST |
| **Last Updated** | 2026-06-07 4:35 PM EST |
| **Author** | zcripted |
| **Scope** | Economy — shop buy/sell result messages, i18n |
| **Files changed** | 4 code + 2 docs |
| **Categories** | Feature · GUIs · Chat · i18n |
| **Verification** | ✅ `gradlew build` green (tests incl. EN/DE parity, both jars) |

---

## 📋 Summary (patch notes)

- **Hover any shop result message for the receipt.** Buying or selling in the shop GUI
  (and dumping into the sell inventory) now produces a result line whose hover tooltip
  is a full transaction receipt: what, how many, at what effective unit price (dynamic
  pricing and sell boosts included), what you gained or paid, and your balance before and
  after. Bulk sales list each item type sold with its quantity and value.
- **Documented assumption:** the requested "bank gain" is interpreted as the wallet
  balance change (shop money goes to the wallet, not `/bank`) — shown as the
  `You Gained +X` row plus `Balance before → after`.

## 🔧 Changes (newest at top → oldest)

### Shop listener (hover assembly)
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java)
  — `buy()` / `sell()` / `onClose()` build hover lines (title + `core.divider-line` +
  detail rows) and send via `ComponentMessenger.sendHoverMessage` instead of a plain
  `languages.send`; bulk close-out now tallies per-material amounts/values
  (`LinkedHashMap`, `BULK_HOVER_LIMIT = 8`); balance-before captured ahead of each
  deposit/withdraw; `lang(...)` shorthands added.

### i18n (EN/DE/ES — 16 new keys each)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `shop.hover.{bought,sold,bulk}.title`, `shop.hover.{item,amount,unit-price,boost,gain,cost,balance,stock}`,
  `shop.hover.bulk.{count,items,entry,more}`.

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---shop-result-hover-tooltips.md](../../changes/2026-06-07---shop-result-hover-tooltips.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`).
