# Documentation — Full Enchantment Reference

■ **Created:** 2026-05-27 (America/Detroit)

■ **Last Updated:** 2026-05-27 (America/Detroit)

---

## Summary

Added a complete, navigable reference listing **every** Arcane and Cursed enchantment
(100 total), built directly from the live YAML rosters so it stays accurate.

## Categories

### Documentation
- `docs/information/enchantments.md` (new) — every enchantment grouped by **category
  (A–Z)** and listed **alphabetically (A–Z)** within each. Per entry: description,
  effects (with per-level scaling), applicable items (item-tag expansion), available
  levels, and a concrete **in-game test clause**. Includes a how-to-apply section, a
  rarity + item-tag legend, and a table of contents.
- Counts: Combat 53, Cursed 8, Defense 8, Farming 6, Mystic 8, Tools 8, Utility 9 — **100**.

## Verification
- Cross-checked the doc's enchant ids against
  `src/main/resources/enchants/{combat,cursed,defense,farming,mystic,tools,utility}.yml`:
  zero ids missing in either direction.
- Confirmed alphabetical ordering within each category.

## Notes
- Source of truth is the enchant YAML files; effect values mirror their per-level
  params. `loot.yml`/`config.yml`/`scrolls.yml` are integration/config, not enchant
  rosters, so they're excluded.

## Suggested Commit Message
```
Docs: Add full Arcane/Cursed enchantment reference (docs/information/enchantments.md)
```
