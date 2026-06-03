# Welcome MOTD Gradient (BungeeCord path) + Server-List Hover Diagnostics

■ **Created:** 2026-05-25 11:52 am
■ **Last Updated:** 2026-05-25 11:52 am

## Summary

Follow-up to `2026-05-25---motd-gradient-and-hover.md`. Two reported issues:

1. **In-game welcome MOTD gradient renders as 2 solid bands** (e.g.
   `<gradient:#FFAA00:#FFFF55>Welcome to the Server</gradient>` shows gold
   "Welcome to" + yellow "the Server"). This is the `config.yml` `join-motd`
   path (`AdventureMessageUtil.send`), NOT the server-list MOTD.
2. **Server-list player-count hover (player sample) still not showing** on Paper.

## Issue 1 — Welcome MOTD gradient → 2 bands

### Root cause
`AdventureMessageUtil` renders chat messages via the Adventure direct-build path,
the MiniMessage path, or a BungeeCord `BaseComponent[]` fallback. The fallback's
`applyHexColor` resolved its hex handles in `resolveHexReflection` using
**`org.bukkit.ChatColor`** (the file's import) — but hex `of(String)` only exists
on **`net.md_5.bungee.api.ChatColor`**. So the lookup always failed, `CHAT_COLOR_OF`
stayed null, and every gradient glyph fell back to `closestLegacyColor()`, which
snaps the color to the nearest of the 16 legacy colors → the smooth ramp collapsed
into ~2 solid bands.

Compounding it: the Adventure **direct** path (true per-glyph Components, needs
only Adventure *core*) was gated behind `ADVENTURE_AVAILABLE`, which requires
MiniMessage. On Paper builds without MiniMessage bundled (1.16–1.17), that gate
skipped the good path and dropped to the broken BungeeCord one.

### Changes — `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
- `resolveHexReflection()` now resolves `of(String)` and `setColor(...)` against
  `net.md_5.bungee.api.ChatColor` (the class that actually carries hex), so the
  BungeeCord fallback applies a true RGB color per glyph → smooth gradient.
- `send(...)` and `broadcast(...)` now also trigger the Adventure path when
  `ADVENTURE_DIRECT_READY` (not only `ADVENTURE_AVAILABLE`), so a Paper build
  without MiniMessage still uses the true per-glyph Component gradient.

### Verification
- Exercised the shared span/gradient logic (`renderLegacy`, which uses the same
  `parseToSpans(expandGradients(...))` as the BungeeCord path) against
  `<gradient:#FFAA00:#FFFF55><bold>Welcome to the Server</bold></gradient>`:
  produces **18 distinct per-glyph colors** (gold `§6` → 16 hex shades → yellow
  `§e`), confirming a smooth gradient rather than 2 bands.

## Issue 2 — Server-list hover

The Paper listener registration (added previously) is correct: on Paper the base
`ServerListPingEvent` lacks the player-sample API, so we also register for
`PaperServerListPingEvent`, whose instance exposes `getPlayerSample()`.

### Changes — `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
- `applyHoverSample(...)` is now more robust (handles an immutable sample list,
  catches `Throwable`) and returns a short outcome tag.
- New one-time-per-event-class diagnostic: the first ping for each distinct event
  class logs `[OBX][MOTD] ping handled: event=<class>, hoverLines=<n>,
  sample=<outcome>`. On Paper this prints two lines (base + Paper events), making
  it obvious which event carries the sample API and whether the hover was set
  (`ok:mutate …` / `ok:setter …`) or why not (`fail:…`). Negligible hot-path cost
  (one set lookup after the first ping per class).

## Testing Notes (important)
- The client **caches** server-list pings. After updating the jar + restarting the
  server, re-open the multiplayer screen (or restart the client) to force a
  re-ping — a stale cache will still show the old (no-hover) result.
- Check the server console for the `[OBX][MOTD] ping handled:` line to confirm
  the Paper event is received and the sample is applied.

## Suggested Commit Message

```
Fix (chat): Render gradients via correct bungee ChatColor + ungate Adventure direct path; add MOTD ping/hover diagnostics
```
