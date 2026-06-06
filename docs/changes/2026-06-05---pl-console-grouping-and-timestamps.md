# /pl Console Output: No Timestamps, Names Grouped Under Their Platform

■ **Created:** 2026-06-05 6:13 pm

■ **Last Updated:** 2026-06-05 6:13 pm

## Summary

Running `/pl` from the console produced a half-broken box: the header, the platform section
labels, and the footer printed cleanly (no timestamps), but the **plugin-name rows** carried
a `[HH:mm:ss INFO]:` logger prefix AND printed after **both** section headers instead of
under their own platform:

```
  Bukkit  ·  7
  Paper  ·  1
[22:07:58 INFO]:     OBX, PlaceholderAPI, Vault, …
[22:07:58 INFO]:     CKSMPLands
```

**Root cause:** the box's header/section/footer lines go through `sendPlain(...)` →
`plugin.writeConsoleLine(...)` (the direct stdout writer — timestamp-free), but the name
rows went through `ComponentMessenger.sendJoinedHoverMessages(...)`, whose console fallback
is `sender.sendMessage(...)` — that routes through the **logger pipeline**, which stamps the
prefix and flushes on a different stream, so the rows fell out of order relative to the
direct writes.

**Fix:** `sendPluginLine(...)` now detects console senders and emits the joined, colorized
name row through `sendPlain(...)` like every other line of the box. Result: zero timestamps
on any box line (only the operator's command echo keeps its timestamp, which the server
prints itself), and each plugin list renders directly under its platform header:

```
  Bukkit  ·  7
    OBX, PlaceholderAPI, Vault, ViaBackwards, ViaVersion, WorldEdit, WorldGuard
  Paper  ·  1
    CKSMPLands
```

In-game output is unchanged (players keep the hover tooltips + click-through to
`/obx plugininfo <name>`).

## Categories

### Commands / Console UX
- `core/src/main/java/dev/zcripted/obx/core/command/PluginListCommand.java`
  — `sendPluginLine(...)`: console branch joins the colored names and writes via
  `sendPlain` (direct console writer); the interactive component path remains for players.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Fix (pl): console list rows under their platform header, no logger timestamps
```
