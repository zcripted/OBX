# Welcome MOTD hover/click dispatch fix

■ **Created:** 2026-05-09 2:05 am

■ **Last Updated:** 2026-05-09 2:05 am

## Summary

Hover tooltips and click events on the welcome MOTD lines (the gradient
"Welcome to the Server" header, the `/sf help` line, the Discord URL
line, and the GitHub / Spigot / BuiltByBit credits row) were rendering
plain — gradient color was correct but no tooltip on hover and the
clickable URLs/commands didn't fire on click.

Root cause was in the dispatch fallback chain inside
`AdventureMessageUtil.send(...)`. The chain was:

1. `trySendAdventureDirect` — Adventure Component build (preserves
   hover/click).
2. `trySendAdventure` MiniMessage path — MiniMessage deserialize
   (preserves hover/click).
3. `trySendLegacyHexText` — render to `§x§A§B§C§D§E§F` legacy section
   string. **This path STRIPS hover and click events** because the
   legacy serializer has no syntax to carry them across the wire.
4. `renderBungee` — BungeeCord `BaseComponent[]` (preserves hover/click).

Whenever the Adventure paths failed at runtime — which is exactly what
the prior `[gradient] runtime path: direct/no-sendMessage-method` log
told us was happening on this user's Paper 1.21.11 build — the message
fell through to step 3 and emerged on the wire as a colored string with
no interactivity.

## Fix

Two changes in `AdventureMessageUtil`:

1. **Skip `trySendLegacyHexText` when the input has hover or click
   markup.** A new helper `hasInteractiveMarkup(...)` does a fast
   prefix scan for `<hover` and `<click` tags on the resolved string;
   when either is present, the dispatcher jumps straight from the
   Adventure paths to `renderBungee`, which builds a `BaseComponent[]`
   with explicit hex color (via `ChatColor.of("#hex")` reflection on
   1.16+) AND preserves hover/click via `TextComponent.setHoverEvent`
   / `setClickEvent`.
2. **Surface the runtime-path log on interactive messages too**,
   not just gradient ones, with explicit hover / click span counts
   and a flag indicating whether hover/click reflection is wired:

   ```
   [gradient] runtime path: direct/sent (spans=23, hover-spans=21, click-spans=0,
   hover-reflection=true, click-reflection=true, first-3-colors=...)
   ```

   This makes it possible to verify from the server log alone
   (a) that the direct-Adventure path actually fired,
   (b) that the parser correctly attached hover/click to the spans,
   and (c) that the reflection handles for `Component#hoverEvent` /
   `Component#clickEvent` are non-null on this Paper build.

   Useful both for confirming this fix works and for diagnosing future
   "still not interactive" reports without needing extra round trips.

## Verification

A standalone parser harness (see commit message) confirmed the
parser produces correct spans pre-fix:

```
=== sfhelp ===
hasInteractiveMarkup: true
  [/sf help] color=55FFFF hover=YES click=RUN_COMMAND//sf help
Spans=4 hover=1 click=1

=== credits ===
hasInteractiveMarkup: true
  [[GitHub]] color=... hover=YES click=OPEN_URL/https://github.com/SergeantFuzzy/SF-Core
Spans=7 hover=3 click=3
```

Spans had hover/click attached correctly. The bug was strictly in
which transport carried the spans to the wire.

## Categories

### Internal — message dispatch

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`
    - `send(...)` — fallback chain reordered. New
      `containsInteractive` flag gates the
      `trySendLegacyHexText` short-circuit; when the input has hover
      or click markup, the dispatcher skips legacy-hex and goes to
      `renderBungee` directly.
    - **New `hasInteractiveMarkup(String)` helper** — checks for
      `<hover` or `<click` (case-insensitive). Used both by `send(...)`
      to gate the legacy-hex skip and by `trySendAdventureDirect` to
      decide whether to emit the diagnostic log.
    - `trySendAdventureDirect(...)` — diagnostic log expanded:
        - Counts hover-spans and click-spans before component build.
        - Includes those counts in success / failure messages.
        - Logs whether the hover and click reflection handles are
          wired.
        - Triggers on interactive-only messages (no gradient required)
          so the URL / `/sf help` lines also produce a runtime-path
          log we can read.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`

## Suggested Commit Message

```
Fix (motd): preserve hover/click when Adventure path fails — route
interactive messages through BungeeCord, not legacy-hex-text
```
