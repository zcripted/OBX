# Sectioned, Tiered Enchantment Lore (Enchantments / Curses / Vanilla)

■ **Created:** 2026-05-26 12:28 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 12:28 pm (America/Detroit)

---

## Summary

Reworks the enchantment tooltip on items into a themed, sectioned layout with a
tiered dot bar per enchantment:

```
▬▬▬ Enchantments ▬▬▬      (custom, non-cursed — dots colored by rarity)
Frostbite            ●●●●○ IV
Executioner          ●●●●● V MAX

▬▬▬ Curses ▬▬▬            (custom, cursed — dark red)
Curse of Brittleness ●●○ II

▬▬▬ Vanilla ▬▬▬           (Bukkit enchantments — white dots)
Sharpness            ●●●●● V MAX
Unbreaking           ●●● III MAX
Mending              ● I MAX
```

- Filled dots = current level, total dots = the enchantment's max level; a `MAX`
  marker is shown when an enchant is fully leveled.
- Custom Arcanum enchants are split into **Enchantments** (non-cursed) and
  **Curses** (cursed) and always sit **above** the **Vanilla** section.
- Within each section, entries are ordered by **level (highest first)**, then
  rarity (custom), then name.

## Decisions (from the requester)

- **Scope:** all enchanted items — the enchanting table and anvil are hooked so
  even pure-vanilla gear gets the styled sections, not just Arcanum gear.
- **Dot color (custom):** by **rarity** (Common→white, Uncommon→green,
  Rare→blue, Epic→purple, Legendary→gold, Mythic→red). Curses use dark red,
  vanilla uses white.
- **Item names:** unchanged — only the lore is restyled (the example title
  "Netherite Sword of the Frostforge" was illustrative; no auto-renaming).

## Categories

### Internal — Lore engine (storage)
- `EnchantStorage` rewritten:
  - `writeLore` builds the three sections + a tiered dot bar, sorts by level
    desc, preserves genuine non-SF lore below a blank, and adds
    `ItemFlag.HIDE_ENCHANTS` when it renders the Vanilla section (so the default
    blue enchant lines don't duplicate it).
  - New robust `parseLine`: strips a trailing `MAX`, the roman level, and a
    dot-bar token, then matches the remaining name — so it recovers custom
    enchants from both the **new dotted** format and the **legacy** `<name>
    <ROMAN>` format (existing items migrate on the next write). Section headers
    and vanilla lines never resolve to a custom enchant.
  - `split` now drops SF-generated decoration (headers / dotted lines) so
    repeated writes don't accumulate; foreign lore is preserved.
  - New `refresh(ItemStack[, extraVanilla])` rebuilds the full tooltip from the
    item's current custom + vanilla enchantments.
  - `renderLine` (the compact one-line scroll/book payload encoding) is
    unchanged, so scrolls/books look the same and still parse.
  - Glyphs verified as UTF-8 on disk; round-trip (render→parse) validated with a
    standalone test (11/11 cases incl. legacy + two-word names + curses).
  - `src/main/java/dev/zcripted/obx/enchant/storage/EnchantStorage.java`
- New cross-version vanilla friendly-name resolver (e.g. `DAMAGE_ALL`/`sharpness`
  → "Sharpness", `binding_curse` → "Curse of Binding"): `getKey()` reflection on
  1.13+, legacy `getName()` map fallback for 1.8–1.12.
  - `src/main/java/dev/zcripted/obx/enchant/storage/VanillaEnchantNames.java`

### Internal — Vanilla restyle hooks
- New `EnchantLoreListener` restyles vanilla enchant tooltips:
  - `EnchantItemEvent` (enchanting table) — merges the pending
    `getEnchantsToAdd()` so the styled lore matches what the player receives.
    Skips books (their enchants are stored separately and aren't hidden by
    `HIDE_ENCHANTS`).
  - `PrepareAnvilEvent` — clones and restyles the result preview. Skips the
    Arcanum scroll/book anvil path and plain renames/repairs.
  - `src/main/java/dev/zcripted/obx/enchant/listener/EnchantLoreListener.java`
  - Registered in `src/main/java/dev/zcripted/obx/Main.java`

### Config
- `enchants/config.yml` gains `lore.style_vanilla_enchants` (default `true`).
  When false, only custom Arcanum enchants are styled and vanilla tooltips are
  left untouched.
  - `src/main/resources/enchants/config.yml`
  - Read in `EnchantService` (`isStyleVanilla()`, pushed to `EnchantStorage`).

## Assumptions / Notes
- Total dots = the enchantment's real max level (so a maxed Unbreaking shows
  `●●● III MAX`). The example mockup's fixed-5 bars were treated as illustrative;
  per-enchant max is the meaningful representation.
- `MAX` is shown for any fully-leveled enchant, vanilla included (the mockup only
  showed it on a custom enchant). Trivially adjustable if undesired.
- Vanilla curses (Curse of Binding/Vanishing) live in the **Vanilla** section
  (they are vanilla enchantments); the **Curses** section is custom Arcanum only.
- Column alignment uses space padding and is approximate — Minecraft's tooltip
  font is proportional, so dot bars won't line up to the pixel.
- Grindstone enchant removal is not hooked (no 1.12.2-API event); such an item
  restyles on its next table/anvil/custom-enchant interaction.

## Testing
- Maven build completes with no errors (in-project `./maven`); obfuscated +
  unobfuscated jars build.
- Standalone render→parse round-trip test: 11/11 pass.

## Suggested Commit Message
```
Feature (enchants): Sectioned Enchantments/Curses/Vanilla lore with tiered rarity dot bars
```
