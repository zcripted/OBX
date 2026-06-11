# Shop Buy/Sell Result Hover Tooltips

■ **Created:** 2026-06-07 4:35 pm

■ **Last Updated:** 2026-06-07 4:35 pm

The shop's chat result lines ("Sold X stack(s) for …", "Bought …", "Sold …") now carry
OBX-style hover tooltips (title bar, divider, `»` detail rows) with the full transaction
breakdown. Full breakdown in the commit log:
[docs/commits/2026-06-07/shop-result-hover-tooltips.md](../commits/2026-06-07/shop-result-hover-tooltips.md)

## Categories

### GUIs / Chat
- **Buy result** (`shop.bought`): hover shows item, amount, effective unit price, total
  paid, balance before → after, and remaining stock + restock countdown for
  finite-stock items.
- **Single-item sell result** (`shop.sold`): hover shows item, amount, effective unit
  price, active sell boost (when > ×1.00), total gained, balance before → after.
- **Bulk sell result** (`shop.sell-gui.result`): hover shows stacks/types summary, a
  per-item breakdown (up to 8 types: `● Item ×N (+value)` with an "…and N more" line),
  sell boost (when active), total gained, balance before → after.
- **Documented assumption:** "bank gain" interpreted as the wallet balance change —
  shown as the `You Gained +X` row plus the `Balance before → after` row (shop proceeds
  go to the wallet, not the /bank account).
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java`

### Internal (i18n)
- 16 new keys × EN/DE/ES (`shop.hover.*`): titles (bought/sold/bulk), detail rows
  (item/amount/unit-price/boost/gain/cost/balance/stock), bulk breakdown
  (count/items/entry/more). Reuses `core.divider-line`.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass; both jars produced.

## Suggested Commit Message
```
Feature (shop): detail hover tooltips on buy/sell/bulk-sell result messages (EN/DE/ES)
```
