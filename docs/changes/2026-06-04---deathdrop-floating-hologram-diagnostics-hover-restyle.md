# Active floating death-drop hologram + diagnostics hover restyle

■ **Created:** 2026-06-04 4:26 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 4:26 pm (America/Detroit)

The death-item hologram now floats actively above the carry-all entity (tracking it each tick), and
the `/obx diagnostics` modules + errors hover tooltips were redesigned to match the clean `/obx reload`
hover style (no more all-bold). Build green; both jars; EN/DE/ES parity passes.

---

## Fixes

- **Death-drop hologram now floats actively on top of the item.** Previously the count label was the
  carry-all **item's own custom name** (rendered at the item, can't sit above it). It's now a separate
  invisible **marker armor-stand hologram** spawned just above the item and **teleported to follow it
  every tick** (`followTick`), so the `×<count>` name floats actively on top of the moving/settling
  item instead of being a fixed label. Folia-safe: each reposition is dispatched on the stand's own
  region thread via the entity scheduler; the per-tick loop early-returns when no piles exist.
  Pickup updates the count, full pickup/despawn/disable removes the stand, and holograms orphaned by a
  hard crash are swept on enable (tagged `obx_deathdrop_holo`, non-Folia). If the stand can't spawn it
  falls back to the item's own name label. Files:
  `features/deathdrop/.../listener/DeathDropListener.java` (rewritten),
  `features/deathdrop/.../DeathDropModule.java` (`start()` / `shutdown()` lifecycle).
- **`/obx diagnostics` modules hover was all bold — redesigned to the `/obx reload` style.** The hover
  header used `&5&lModules`; the `&l` **leaked bold across every line** through the legacy hover
  serializer's style inheritance. Both the **modules** hover and the **`full` Errors** hover are now
  built in the reload-hover style — a hex `▍ 𝗢𝗕𝗫  ›  <title>` header bar (no `&l`), a
  `core.divider-line` rule, and clean `  ● name` / `    • detail` rows. To render the hex header,
  `colorizeLines` now runs the full `AdventureMessageUtil.renderLegacy` colorizer (handles `&#RRGGBB`)
  instead of a plain `&`→`§` translate. File: `core/.../command/ObxDiagnosticsView.java`.

## Categories Touched
Internal (DeathDropListener, DeathDropModule, ObxDiagnosticsView).

## Testing
- `./gradlew :core:test` — EN/DE/ES parity **passes** (no message-key changes this batch).
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Fix (deathdrop/diagnostics): floating death hologram tracks the item; reload-style diagnostics hovers

- deathdrop: replace the item's fixed custom-name label with a marker armor-stand hologram that
  floats above the item and follows it every tick (Folia-safe via the entity scheduler); update on
  pickup, remove on pickup/despawn/disable, sweep crash orphans on enable
- diagnostics: redesign the modules + errors hover tooltips to the /obx reload style (hex header,
  divider-line, bullet rows) and drop the &l that was leaking bold across the whole tooltip;
  colorize hover lines via renderLegacy so the hex header renders
```
