# Fix: Custom Enchant Adding a Visible "Unbreaking" (Glow Marker Leak)

■ **Created:** 2026-05-26 12:59 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 12:59 pm (America/Detroit)

---

## Symptom

Applying a custom Arcanum enchant (e.g. Headhunter) to a sword also showed a
vanilla **Unbreaking** enchantment in the tooltip.

## Root cause

The enchant "glow" is added by `enchant/util/Glow`. On 1.20.5+ it should use the
clean `ItemMeta#setEnchantmentGlintOverride(Boolean)` (no real enchant). That call
is reflective (the plugin compiles against the 1.12.2 API), and it lacked
`setAccessible(true)` — so on some runtimes the reflective `invoke` against the
internal `CraftMetaItem` class fails with an access/module error. `trySetGlint`
then returned `false` and fell back to the legacy glow hack:
`addEnchant(DURABILITY /* Unbreaking */, 1) + HIDE_ENCHANTS`.

That hidden Unbreaking was invisible before, but the new styled **Vanilla** lore
section reads the item's real enchantments and surfaced it as a genuine entry.

## Fix

### `enchant/util/Glow.java`
- `trySetGlint` now calls `method.setAccessible(true)` before invoking, so the
  glint-override path works reliably on 1.20.5+. The Unbreaking fallback is then
  only used on servers that genuinely lack the API (pre-1.20.5), where a real
  hidden enchant is the only way to glow.

### `enchant/storage/EnchantStorage.java`
- The cosmetic glow marker is no longer shown as a real enchant: when an item
  carries a custom Arcanum enchant, a lone level-1 Unbreaking (the glow marker) is
  dropped from the Vanilla section. The glow marker only ever lands on
  custom-enchanted items, so this is precise.
- `HIDE_ENCHANTS` is now applied whenever OBX manages the vanilla display —
  i.e. when a Vanilla section is rendered **or** the item has real enchants (which
  covers the skipped glow marker and the enchanting-table path, where the chosen
  enchants aren't on the item yet). This keeps the default blue lines (including
  the hidden marker) off the tooltip.

## Behavior after the fix
- **New items:** glint override is used, so no Unbreaking is added at all — clean
  Enchantments section, item glows, no Vanilla line.
- **Pre-1.20.5 servers:** the glow still needs a hidden Unbreaking, but it is no
  longer displayed as a vanilla enchant.
- **Already-affected items:** the leftover hidden Unbreaking remains on the item
  functionally (a harmless +durability) but is hidden from the tooltip. Re-rolling
  the item on a fresh sword shows the correct, Unbreaking-free result. It is not
  auto-stripped to avoid ever deleting a player's legitimately-applied Unbreaking I.

## Notes / assumptions
- The skip targets **level-1** Unbreaking on items that also have a custom enchant
  — the exact signature of the glow marker. A player who intentionally puts
  Unbreaking I on a custom-enchanted item won't see that single level in the
  Vanilla section (Unbreaking II+ is unaffected). Acceptable, rare edge.

## Testing
- Maven build completes with no errors (in-project `./maven`); obfuscated +
  unobfuscated jars build.

## Suggested Commit Message
```
Fix (enchants): glint-override setAccessible + hide cosmetic glow Unbreaking from Vanilla lore
```
