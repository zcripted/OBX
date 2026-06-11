# 🖥️ Custom Player-Command Console Log — Styled Replacement for "issued server command"

> The vanilla console line `SergeantFuzzy issued server command: /baltop` is now replaced
> by a styled, fully configurable OBX log:
>
> ```
> [23:13:21 INFO]: [COMMAND] SergeantFuzzy used command /baltop.
> [23:13:21 INFO]: World: world · Date: Sunday, June 7, 2026 · 2026-06-07 · Time: 11:13 PM EST
> ```
>
> The vanilla line is logged by the server internals (not Bukkit) and can't be cancelled
> through the API — it is suppressed with a **log4j2 DENY filter attached to the root
> logger**, built as a *dynamic proxy* so OBX needs no compile-time log4j dependency and
> tolerates every log4j 2.x revision from 1.8.8's beta9 to current Paper (unknown
> interface methods get safe defaults; install failure degrades gracefully to showing
> both lines). The custom line renders full ANSI truecolor through the direct console
> writer with the shared `ConsoleTimestamp` prefix.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 7:40 PM EST |
| **Last Updated** | 2026-06-07 7:40 PM EST |
| **Author** | zcripted |
| **Scope** | Core console, OBX wiring, config.yml |
| **Files changed** | 3 code/config + 2 docs |
| **Categories** | Feature · Console · Config |
| **Verification** | ✅ `gradlew build` green (both jars) · obfuscated jar boots clean on Paper 1.21.4 (`runServerObf`) |

---

## 📋 Summary (patch notes)

- **Player commands log in OBX style.** Every player-issued command writes a purple/gray
  `[COMMAND]` line (plus a context line: world, long + ISO date, time in the configured
  timezone) instead of the flat vanilla message. Cancelled commands aren't logged —
  matching vanilla behavior.
- **Everything is configurable** under `console.command-log` in `config.yml`:
  `enabled`, `suppress-vanilla` (hide the stock line), `timezone` (default
  `America/Detroit`), and the `format` line list with placeholders `{player}`,
  `{command}`, `{world}`, `{date}`, `{date-iso}`, `{time}` — full `&`-code / `&#hex` /
  gradient color support. Config edits apply on `/obx reload`.
- **Safe by construction:** if the log filter can't attach (exotic logging setups), OBX
  logs one warning and shows both lines rather than breaking console logging; on plugin
  disable the filter goes inert (log4j offers no clean detach).

## 🔧 Changes (newest at top → oldest)

### Core console (new)
- **NEW** [core/src/main/java/dev/zcripted/obx/core/console/PlayerCommandConsoleLog.java](../../../core/src/main/java/dev/zcripted/obx/core/console/PlayerCommandConsoleLog.java)
  — MONITOR `PlayerCommandPreprocessEvent` hook renders the configured lines via
  `AdventureMessageUtil.renderAnsi` + `ConsoleTimestamp`; reflective dynamic-proxy
  log4j2 filter DENYs messages containing `" issued server command: "` (interface-based
  method lookup — implementation classes may be package-private); static install guard,
  live-instance indirection so reloads/disable apply without re-attaching.

### Wiring
- [plugin/src/main/java/dev/zcripted/obx/OBX.java](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java)
  — service constructed + listener registered in `onEnable`; `shutdown()` (filter goes
  inert) in `onDisable`.

### Config
- [plugin/src/main/resources/config.yml](../../../plugin/src/main/resources/config.yml)
  — new `console.command-log` section (enabled / suppress-vanilla / timezone / format).

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---custom-command-console-log.md](../../changes/2026-06-07---custom-command-console-log.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — both jars produced.
- `runServerObf`: obfuscated jar boots clean on Paper 1.21.4 with the filter installed.
