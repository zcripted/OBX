# Shop Quantity Menu — Per-Item Buy/Sell Submenu (±1/5/10/30/50)

■ **Created:** 2026-06-07 4:57 pm

■ **Last Updated:** 2026-06-07 4:57 pm

Clicking a shop item now opens a per-item quantity submenu (toggle buy/sell, step the
amount by ±1/5/10/30/50, confirm) with a live OBX-style receipt. Full breakdown in the
commit log:
[docs/commits/2026-06-07/shop-quantity-menu.md](../commits/2026-06-07/shop-quantity-menu.md)

## Categories

### GUIs
- New QUANTITY view (45 slots): +steps row (lime) above the centered item, −steps row
  (red) below; bottom nav Back · Mode toggle · Confirm · Balance · Close. Item tile shows
  mode, amount, effective unit price, colored total, balance, and finite stock — live.
- Left-click an item = buy mode, right-click = sell mode; shift-left/shift-right keep the
  quick buy-stack / sell-all shortcuts. Lore hints updated.
- Confirm reuses the guarded `buyUnits`/`sellUnits` paths (stock, caps, dupe guards,
  audit log, hover-receipt chat message); the menu re-renders in place after each trade.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopMenu.java`
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java`

### Internal (i18n)
- 17 new `shop.gui.qty.*` keys + reworded `shop.gui.item.hint-buy`/`hint-sell` × EN/DE/ES.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass; both jars produced.

## Suggested Commit Message
```
Feature (shop): per-item quantity menu — buy/sell toggle, ±1/5/10/30/50 steps, live receipt, confirm via guarded unit paths
```
