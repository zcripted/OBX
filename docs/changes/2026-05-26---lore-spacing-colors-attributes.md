# Enchant Lore Polish: Section Spacing, Category-Colored Names, Clean Attributes

■ **Created:** 2026-05-26 1:13 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 1:13 pm (America/Detroit)

---

## Summary

Three refinements to the sectioned enchantment tooltip:

```
▬▬▬ Enchantments ▬▬▬
Executioner          ●●●●● V MAX     (name in COMBAT red)
Headhunter           ●●○ I           (name in COMBAT red)

▬▬▬ Curses ▬▬▬
Curse of Brittleness ●●○ II          (name dark-red italic)

▬▬▬ Vanilla ▬▬▬
Sharpness            ●●●●● V MAX
Unbreaking           ●●● III MAX

▬▬▬ Attributes ▬▬▬
Attack Damage » 7
Attack Speed » 1.6
```

1. **Blank line between sections** for clean, easy-to-read spacing.
2. **Category-colored names** for Arcane and Curse enchants (Combat=red,
   Defense=blue, Tools=yellow, Farming=green, Utility=aqua, Mystic=purple,
   Cursed=dark red). Dots still encode rarity (curses keep dark-red dots + italic).
3. **Clean Attack Damage / Attack Speed display** on Arcane weapons, replacing the
   default gray attribute lines.

## Categories

### Internal — Lore layout (`EnchantStorage`)
- `writeLore` now assembles each populated section as its own block and joins them
  with a blank line; a blank also precedes any preserved foreign lore. `split()`
  drops the inter-section blanks on re-parse (they always land leading in the
  "other" bucket), so repeated writes don't accumulate spacing.
- `renderCustomLine` colors the enchant **name** with the category color
  (`EnchantCategory.getColor()`); curses keep the italic style. Dot bars are
  unchanged (rarity color, dark-red for curses).

### Internal — Attribute display
- New `WeaponAttributes` computes the main-hand **Attack Damage** and **Attack
  Speed** the client would show: player base (1.0 / 4.0) + the item's `ADD_NUMBER`
  hand-slot modifiers. Source = the item's explicit modifiers if any (they replace
  the defaults, as in vanilla), else the material's default modifiers. All
  reflective (`ItemMeta#getAttributeModifiers` 1.13+, `Material#getDefaultAttributeModifiers`
  1.16+), with attribute identity matched by key name (`…attack_damage` /
  `…attack_speed`) so it survives the 1.21 attribute-registry change. Returns
  `null` (→ vanilla display left untouched) when the item isn't a weapon or the
  values can't be read.
  - `src/main/java/dev/zcripted/obx/enchant/storage/WeaponAttributes.java`
- `EnchantStorage` renders an **Attributes** section (gold header,
  `Attack Damage » 7` / `Attack Speed » 1.6`) on items that carry a custom Arcane
  enchant, and sets `ItemFlag.HIDE_ATTRIBUTES` so the default gray lines don't
  duplicate it. The numbers are read from the item's real modifiers, so if a
  future Arcane enchant adds an attack-damage/speed modifier the display reflects
  it automatically. Attribute lines are recognized as managed decoration in
  `split()` (by the `Attack Damage` / `Attack Speed` label prefix) so they're
  regenerated, not preserved.
  - `src/main/java/dev/zcripted/obx/enchant/storage/EnchantStorage.java`

## Notes / assumptions
- No current Arcanum enchant modifies item attack damage/speed (combat effects are
  event-driven), so today the Attributes section shows the weapon's true base
  values in a clean style — i.e. the "if affected by Arcane, reflect proper
  values" path is satisfied by reading the actual modifiers.
- The Attributes section is scoped to **Arcane gear** (items with a custom
  enchant), per the request ("for Arcane when enchantments are applied").
  Vanilla-only enchanted weapons keep their native attribute display.
- Only weapons/tools that expose an attack-damage attribute get the section; bows,
  armor, etc. are untouched. `ADD_SCALAR` / multiply operations (not used by
  vanilla weapon modifiers) aren't summed.
- Section order: Enchantments → Curses → Vanilla → Attributes → (descriptions),
  mirroring vanilla's attributes-near-the-bottom placement.

## Testing
- Maven build completes with no errors (in-project `./maven`); obfuscated +
  unobfuscated jars rebuilt.
- Standalone re-parse test on a full sectioned tooltip (headers, blanks, all three
  enchant sections, attribute lines, and a 2-line description): custom enchants
  recovered correctly, and every header / blank / vanilla / attribute line is
  dropped while the genuine description is preserved — confirming no accumulation
  on repeated writes (3/3 pass).

## Suggested Commit Message
```
Polish (enchants): section spacing, category-colored names, clean Attack Damage/Speed attribute display
```
