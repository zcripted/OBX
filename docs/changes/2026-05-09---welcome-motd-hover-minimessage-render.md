# Welcome MOTD hover tooltips: render MiniMessage in BungeeCord fallback

■ **Created:** 2026-05-09 2:17 am

■ **Last Updated:** 2026-05-09 2:17 am

## Summary

Hover tooltips on the in-game welcome MOTD lines (the gradient
"Welcome to the Server" header, the player-name line, the `/sf help`
line, the Discord URL line, and the GitHub / Spigot / BuiltByBit
credits row) were rendering with their MiniMessage markup intact —
players hovering on those lines saw raw text like
`<gold><bold>✦ Welcome MOTD ✦</bold></gold>` instead of a styled
tooltip.

## Root cause

The chat dispatch chain in `AdventureMessageUtil.send(...)` falls
through Adventure (direct + MiniMessage) → BungeeCord
`BaseComponent[]`. The Adventure direct path's `buildHoverComponent`
already routed hover content through `legacyToMiniMessage` +
`parseToSpans`, so it parsed `<gold>` / `<bold>` / `<gradient:...>`
correctly.

The BungeeCord fallback at `applyStyle(...)` did not. It built hover
components with:

```java
TextComponent.fromLegacyText(translateLegacy(String.join("\n", span.hoverLines)));
```

`translateLegacy` only converts legacy `&codes`. `fromLegacyText`
only understands `§`-prefixed legacy section codes. Neither
recognises MiniMessage tags. Anything inside `<hover:show_text:'...'>`
that used `<gold>`, `<bold>`, `<gray>`, `<gradient:...>`, etc. was
shipped to the client verbatim and rendered as raw text in the
tooltip.

The `2026-05-09---welcome-motd-hover-click-dispatch-fix` change
forced messages with hover/click markup to skip the legacy-hex
shortcut and go straight to `renderBungee` so click events would
fire — that fix put more MOTD traffic onto the BungeeCord path,
which exposed this latent rendering bug for every player who hovered
on a welcome-MOTD line.

## Fix

In `AdventureMessageUtil.applyStyle(TextComponent, Span)`, route
hover content through the existing `renderBungee(...)` parser so
MiniMessage tags become real component styling instead of literal
characters:

```java
BaseComponent[] hoverComponents = renderBungee(String.join("\n", span.hoverLines));
```

`renderBungee` already runs the same parse pipeline as the main line
(`expandGradients` → `parseToSpans` → per-span `applyStyle`), so
nested gradients, decorations, and legacy `&codes` inside a hover
tooltip render the same way they would if they were top-level
content. Hover-inside-hover is not a case the welcome MOTD uses, but
the recursion would terminate naturally since the inner spans carry
no further `hoverLines`.

## Verification

Maven build (`./maven/bin/mvn.cmd -DskipTests package`) completes
cleanly — only ProGuard `Note:` lines for reflective access remain,
which are informational per `CLAUDE.md`.

Output JARs:
- `target/SF-Core-1.0.0-SNAPSHOT.jar`
- `target/SF-Core-1.0.0-SNAPSHOT-unobf.jar`

Trace through the welcome-MOTD lines (`config.yml:67-72`):

- `<hover:show_text:'<gold><bold>✦ Welcome MOTD ✦</bold></gold>...'>`
  — joined hover content now routed through `renderBungee`, so
  `<gold>` becomes a real `TextComponent` color and `<bold>` becomes
  a `setBold(true)` call.
- The `/sf help`, Discord, and GitHub/Spigot/BuiltByBit hover
  bodies — same path; their `<yellow>`, `<aqua>`, `<green>`, `<gray>`
  tags now render as styled text.
- Legacy `&codes` inside hovers (e.g. `&eUUID:&f {uuid}` on the
  player-name line) still work because `renderBungee` →
  `parseToSpans` handles `&codes` directly via `applyLegacyCode`.

## Categories

### Internal — message dispatch

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`
    - `applyStyle(TextComponent, Span)` — hover branch now builds
      `BaseComponent[] hoverComponents` via `renderBungee(...)` on
      the joined hover lines instead of `TextComponent.fromLegacyText
      (translateLegacy(...))`. The legacy-fromText path could not
      parse MiniMessage tags, so any tag inside `<hover:show_text:
      '...'>` reached the client unprocessed.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`

## Suggested Commit Message

```
Fix (motd): render MiniMessage tags inside hover tooltips on Bungee path
```
