# Holograms — Phase 5 board + HOLOGRAM prefix

■ **Created:** 2026-05-29 6:25 pm  
■ **Last Updated:** 2026-05-29 6:25 pm

## Summary

Closed the only real Phase X stub in the holograms module — `/holo board` was
returning *"Board backing is wired in Phase 5 (transforms / boards). The
model already persists board state — use this command after Phase 5 lands
to configure it."* despite the model not actually carrying any board state.
This change wires the feature end-to-end (settings → YAML → BlockDisplay
rendering → real subcommand tree → InfoSub readout) and additionally
replaces every hardcoded `§...SF-Core Holograms §8› §...` chat prefix in
the package with a shared `HoloMessages` style matching the SF-Core /
Arcanum wordmark convention but in an aqua palette with a benzene-ring
icon, so the holograms surface is visually distinct from the rest of
SF-Core.

## Categories

### Internal (Phase 5 board)

- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/model/HologramSettings.java`
  — added five fields: `boardEnabled` (boolean, default false),
  `boardMaterial` (String — name-string instead of Material so older saves
  survive cross-version renames; defaults to `"WHITE_CONCRETE"` and
  resolves at spawn time), `boardWidth` (default 1.5 blocks),
  `boardHeight` (default 0 → auto-fit to line stack), `boardOffsetBack`
  (default 0.05 blocks behind text plane). Setters clamp inputs to sane
  bounds (0.1–32 blocks for width/height, 0–2 for offset).
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/storage/HologramSerializer.java`
  — round-trips the five new fields under `settings:` (`board-enabled`,
  `board-material`, `board-width`, `board-height`, `board-offset-back`).
  Existing saves without these keys default-load to the off / sensible
  values per the established forward-compat rule.
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/DisplayEntityBackend.java`
  — new `spawnBoard` helper called from `spawn()` when
  `settings.isBoardEnabled()`. Spawns a single `BlockDisplay` at the
  hologram origin, matches the billboard + view-range of the text lines,
  and applies a `Transformation` that scales the unit block to
  `(width × heightOrAuto × 0.05)` and translates it
  `(-w/2, midY - h/2, -offset - depth)` in entity-local space so it sits
  centered horizontally, vertically aligned with the line stack, and
  pushed behind the text plane. Auto-height = `lineCount × lineSpacing +
  0.2`. Falls back to `Material.STONE` if the configured material is
  missing on the runtime (the legacy compile target lacks
  `WHITE_CONCRETE` as a direct symbol).

### Commands

- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/BoardSub.java`
  — replaced the stub with the real implementation:
  - `/holo board <id>` — print current board state for the hologram
  - `/holo board <id> enable | on`
  - `/holo board <id> disable | off`
  - `/holo board <id> material <MATERIAL>` (any block material)
  - `/holo board <id> size <width> [height | auto]`
  - `/holo board <id> offset <distance>` (blocks behind text)
  
  Each mutating action calls `ctx.persistAndRefresh(hologram)` so the
  change is committed to YAML *and* the backend respawns the entities
  for all current viewers. Tab-complete suggests subcommand keywords
  and block-material names.
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/InfoSub.java`
  — added a board readout line to the per-hologram report
  (enabled flag · material · width × height · back-offset).

### Internal (HOLOGRAM prefix utility)

- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/HoloMessages.java` *(new)* —
  shared style constants + helpers:
  - `PREFIX_INLINE = "§3⌬ §b𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠 §8➠ §7"` — Arcanum-style
    chat-feedback prefix
  - `PREFIX_BOX = "§3▍ §b𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠  §8›  §f"` — SF-Core-style report
    header
  - `DIVIDER = "§8──────────────────────────────"`
  - `inline(body)` / `header(body)` helpers
  
  Palette: aqua (`§b` primary, `§3` accent). Icon: `⌬` (U+232C
  BENZENE RING WITH CIRCLE) — wireframe-hex glyph that reads as
  "holographic projection". Bold word uses the math sans-serif bold
  variant (`𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠`) so it survives unicode-safe transports
  without depending on chat-color bold. Visually distinct from
  SF-Core's orange `▍` and Arcanum's purple `✦`.

### Internal (prefix adoption)

Replaced hardcoded `§6§lSF-Core Holograms §8› §7…` / `§e§lSF-Core
Holograms §8› §7…` / `§6§lHologram §8› §f…` headers with
`HoloMessages.PREFIX_BOX` / `PREFIX_INLINE` / `DIVIDER` in:

- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/HologramCommand.java`
  — `sendHelp()` help-screen header
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/ListSub.java`
  — list header + divider
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/ReloadSub.java`
  — reload-success line
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/InfoSub.java`
  — both the module overview header and the per-hologram detail header
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/AnimSub.java`
  — animations-list header
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/AimGuiSub.java`
  — closest-hologram feedback
- `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/BoardSub.java`
  — state-display, usage, and every command-output line

The `HologramEditorMenu` GUI title bar and the debug-hologram in-world
text in `HologramCommand.handleDebug` were left untouched (out of scope
for "chat prefix").

## Notes on what was NOT changed

A grep across the hologram package finds many `// Phase 4`, `// Phase 6`,
`// Phase 7` annotations. Each was inspected: they are historical
documentation describing which phase introduced an already-shipped
feature (interactions, viewer permissions, hide-behind-walls,
per-player hidden set, GUI editor). The `// Phase 6+ refinement` note
in `InteractionDispatcher.dispatch` refers to per-line ray-projected
click precision (currently approximates to whole-hologram precision) —
this is a polish item, not a stub. No other subcommand returned a
"not implemented" / "Phase X lands" message.

## Suggested Commit Message

```
Holograms: complete Phase 5 board + adopt HOLOGRAM prefix
```

(Already committed as `b392276`.)
