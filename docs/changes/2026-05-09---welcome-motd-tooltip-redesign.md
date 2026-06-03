# Welcome MOTD hover tooltip redesign

■ **Created:** 2026-05-09 2:34 am

■ **Last Updated:** 2026-05-09 2:34 am

## Summary

Redesigned every hover tooltip on the in-game welcome MOTD lines for a
modern / clean / minimal look. Bold weight is now reserved for the
tooltip title only — body copy is plain weight, which fixes the
overweight-everywhere look the previous tooltips had and gives each
tooltip a clear visual hierarchy (title → divider → body → action
hint).

## Design

Every hover tooltip now follows the same shape:

```
<accent><bold>Title</bold></accent>
━━━━━━━━━━━━━━━━━            (dark gray rule)
Body line one                 (gray)
Body line two                 (gray)
                              (blank gap)
› Click to …                  (dark gray, only on click-bearing lines)
```

Choices and rationale:

- **One bold per tooltip — the title.** The previous design wrapped
  bold around the title AND the action hint (`<bold>▶ Click to
  execute</bold>`), and on legacy hover serialization this produced
  two heavy-weight bands stacked on top of each other. Reserving
  bold for the title is the standard modern-Minecraft tooltip
  pattern and it lets the divider + body breathe.
- **Dark-gray horizontal rule (`━` × 17)** under the title. This is
  the visual anchor that turns the tooltip from a stack of lines
  into a labeled card. 17 glyphs is wide enough to span typical
  body copy without dwarfing it.
- **Body copy in `<gray>`, with `<white>` reserved for code-like
  values** (file paths, config keys, UUID/world). Two-tone body
  reads as "label / value" without needing extra punctuation.
- **Blank visual gap (`\n \n`)** between sections inside multi-
  section tooltips. The space character is required — pure `\n\n`
  with no glyph between collapses on some clients.
- **Action hint (`<dark_gray>› Click to …`)** sits at the bottom of
  every interactive tooltip, in the lightest weight color so it
  doesn't compete with the title. The `›` glyph replaces the heavier
  `▶` that the old design used.
- **Dropped the source-line annotations** from the dev-reference
  tooltip on the welcome banner (`(line 67)`, `(line 78)`, etc.).
  Hard-coded line numbers go stale the moment the config grows by a
  line; the path (`join-motd.lines`) is stable and points the admin
  to the right place without lying about its location.

## Per-line tooltip changes

`config.yml` `join-motd.lines`:

1. **Welcome banner** (`Welcome to the Server`) — admin / dev
   reference panel. Title `Welcome MOTD`, body covers config file,
   lines path, first-join variant, and toggle.
2. **Greeting** (`Hello, {player}!`) — `{player}` now hovers a
   labeled card with the player name as the title and UUID / World
   as the body, replacing the cramped `&eUUID:&f {uuid}\n…` legacy
   line. Uses MiniMessage now so the labels render in the same gray
   the other tooltips use.
3. **Online count** — unchanged (no hover).
4. **`/obx help`** — title `Help Menu`, body `Browse all available /
   OBX commands.`, action hint `› Click to execute`.
5. **Discord** — title `Discord`, body `Join our community / and
   stay connected.`, action hint `› Click to open`.
6. **Credits row** — three side-by-side buttons (`[GitHub]`,
   `[Spigot]`, `[BuiltByBit]`) each with their own card-style
   tooltip in the matching brand accent (green / gold / aqua).

The `enabled`, line count, command bindings, URL targets, gradient
header, and all click events are preserved — only the hover content
shape and visual weight changed.

## Verification

Maven build (`./maven/bin/mvn.cmd -DskipTests package`) completes
clean — only the standard ProGuard `Note:` lines for reflective
access remain. Output JARs:

- `target/OBX-1.0.0-SNAPSHOT.jar`
- `target/OBX-1.0.0-SNAPSHOT-unobf.jar`

Bold-leak check on the BungeeCord transport (the legacy-mode hover
path patched in
`2026-05-09---welcome-motd-hover-minimessage-render.md`):

- The title span emits `§<color>§l<text>` in legacy form.
- The next visible span (the divider) starts with `<dark_gray>`,
  which produces `§8` in the legacy stream. A color code resets
  bold per legacy formatting rules, so the divider and every body
  line below it render without the title's bold weight.
- Modern (1.16+) Adventure clients use the per-component bold flag
  on the JSON wire format, so the same content renders the same way
  without relying on the legacy reset. Both transports agree.

## Categories

### Config

- `src/main/resources/config.yml`
    - `join-motd.lines[1]` — welcome banner hover redesigned.
    - `join-motd.lines[2]` — greeting line; player-name hover
      converted from legacy `&codes` to MiniMessage card style.
    - `join-motd.lines[4]` — `/obx help` hover redesigned.
    - `join-motd.lines[5]` — Discord hover redesigned.
    - `join-motd.lines[6]` — GitHub / Spigot / BuiltByBit hovers
      redesigned with brand-accent titles.

## Files Modified

- `src/main/resources/config.yml`

## Suggested Commit Message

```
Style (motd): redesign welcome hover tooltips — single-bold title,
divider, organized body, minimal action hint
```
