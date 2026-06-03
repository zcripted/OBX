# Welcome MOTD hover tooltip — bold-inheritance fix

■ **Created:** 2026-05-09 3:35 am

■ **Last Updated:** 2026-05-09 3:35 am

## Summary

Every hover tooltip on the in-game welcome MOTD lines was rendering
100% bold — title, divider, body copy, and action hint — even though
the config wraps `<bold>` only around the title. The redesign in
`2026-05-09---welcome-motd-tooltip-redesign.md` correctly placed bold
on titles only, but the renderer was leaking the visible carrier's
bold flag into the tooltip body through component-style inheritance.

## Root cause

When a tooltip is attached to a component whose own style sets
`bold=true` (e.g. the gradient `<bold>Welcome to the Server</bold>`
header, or the bold-titled `<click>`-bearing spans), the body
components inside the hover content had no explicit bold value:

- **Adventure direct path** (`buildComponentFromSpans`) only called
  `decorate(BOLD)` on spans where `span.bold == true`. Spans that
  weren't bold left their decoration in the `NOT_SET` state.
- **BungeeCord fallback path** (`applyStyle`) only called
  `setBold(true)` on bold spans. Non-bold spans had `bold=null`.

`NOT_SET` / `null` means *inherit from parent* in both Adventure and
BungeeCord. Minecraft's tooltip rendering walks the component tree
top-down and the inherited bold from the visible carrier propagated
into every body line of the hover. The title was already bold, so it
looked correct; the divider, body copy, and `› Click to …` action hint
all picked up bold by inheritance, which is why the entire tooltip
rendered as one solid bold block.

## Fix

Make every span carry an **explicit** decoration value (TRUE or FALSE),
never NOT_SET / null:

- Added reflection lookup for
  `Component.decoration(TextDecoration, boolean)` and rewrote
  `buildComponentFromSpans` to call it for `BOLD`, `ITALIC`,
  `UNDERLINED`, `STRIKETHROUGH`, and `OBFUSCATED` on every span,
  passing the span's actual boolean. Falls back to the old
  `decorate(...)`-only path on Adventure builds that lack the
  boolean overload.
- Rewrote `applyStyle(TextComponent, Span)` to always call
  `setBold(span.bold)` / `setItalic(span.italic)` / etc., rather than
  only flipping the TRUE ones. BungeeCord's `Boolean` decoration
  fields now serialize an explicit `"bold": false` on body spans, so
  the client has no `null` to inherit through.

The same fix applies to the visible top-level message components
because both paths share the same builder. The leading-space spans
on the welcome banner now serialize `bold: false` explicitly — no
visual change (spaces have no bold glyph), but it keeps the rendering
consistent.

## Verification

Maven build (`./maven/bin/mvn.cmd -DskipTests package`) completes
cleanly — only the standard ProGuard `Note:` lines for reflective
access remain, which are informational per `CLAUDE.md`. Output JARs:

- `target/OBX-1.0.0-SNAPSHOT.jar`
- `target/OBX-1.0.0-SNAPSHOT-unobf.jar`

Trace through the welcome banner tooltip
(`config.yml:67`, hover content `<gold><bold>Welcome MOTD</bold></gold>\n<dark_gray>━...</dark_gray>\n<gray>...`):

- Span 1 — `Welcome MOTD`: `bold=true` → `decoration(BOLD, true)` /
  `setBold(true)`. Title renders bold, as designed.
- Span 2 — `\n`: `bold=false` → `decoration(BOLD, false)` /
  `setBold(false)`. Newline carries explicit not-bold; no inheritance.
- Span 3 — `━━━...`: `bold=false`, `color=dark_gray`. Divider renders
  dark gray, plain weight.
- Spans 4+ — body copy and action hint: each carries explicit
  `bold=false`, so the inheritance chain that previously made them
  bold is broken.

Same trace shape applies to the greeting `{player}` hover, the
`/obx help` hover, the Discord hover, and each of the GitHub / Spigot /
BuiltByBit credit-row hovers — they all share the title-bold,
body-plain shape and all benefit from the same fix.

## Categories

### Internal — message dispatch

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
    - Added `ADVENTURE_COMPONENT_DECORATION_BOOL` field and resolved
      `Component.decoration(TextDecoration, boolean)` in the static
      initializer alongside the existing decoration reflection.
    - Rewrote the decoration block in `buildComponentFromSpans` to
      use the boolean overload when available so every span sets
      every decoration to an explicit TRUE/FALSE.
    - Rewrote `applyStyle(TextComponent, Span)` to always call
      `setBold` / `setItalic` / `setUnderlined` / `setStrikethrough` /
      `setObfuscated` with the span's boolean, replacing the
      "only-set-when-true" pattern that left `null` on body spans.

## Files Modified

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`

## Suggested Commit Message

```
Fix (motd): anchor hover-tooltip decorations so body text doesn't inherit bold from visible carrier
```
