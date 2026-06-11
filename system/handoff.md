# Session Handoff

## Completed Changes (9 fixes for v1.1.0)

1. **Shop GUI row 2** — slots reorganized in `shop.yml` (ores 12→11, farming 14→12, food 16→14, mobdrops 20→15, redstone 24→16)
2. **Shop GUI row 4** — sell tile from `bottom+2`→`bottom+3`, balance from `bottom+4`→`bottom+5` in `ShopMenu.java` and `ShopListener.java`
3. **`/bank menu` alias** — added in `BankCommand.java`, `plugin.yml`, `COMMANDS+PERMISSIONS.md`
4. **Sell exact-amount guard** — quantity-mode sell refuses if player carries < desired amount (`ShopListener.java`)
5. **Inventory-full buy guard** — checks space before withdrawing money (`ShopListener.java` + `maxAddable()` helper)
6. **Economy digest console** — split `\n` into individual `sendMessage()` calls (`EconomyReportService.java`)
7. **Backpack bundle attributes** — `HIDE_ADDITIONAL_TOOLTIP` via reflection (`BackpackService.java`)
8. **`/unbreakable` type guard** — rejects non-tool/weapon/armor items (`UnbreakableCommand.java` + `isBreakable()`)
9. **New language keys** — `shop.sell-insufficient`, `shop.inventory-full`, `item.unbreakable.not-breakable` added to EN/DE/ES

## Build Status
- `.\gradlew.bat build` — BUILD SUCCESSFUL
- `OBX-1.1.0.jar` produced
- Version already bumped to `1.1.0` in prior session

## Version Scheme
- Minor SemVer bump from stable `1.0.0` → `1.1.0` (new features, backward-compatible)
- No pre-release tag, no build number