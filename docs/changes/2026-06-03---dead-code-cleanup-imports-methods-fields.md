# Project-Wide Dead Code Cleanup — Unused Imports, Methods & Fields

■ **Created:** 2026-06-03 10:52 AM (America/Detroit)

■ **Last Updated:** 2026-06-03 10:52 AM (America/Detroit)

Scanned all **447** Java source files for dead/unused code and removed verified-dead
imports, private methods, and private fields. Detection used conservative, deterministic
scanners (a symbol is only flagged when it is genuinely unreferenced — comments/javadoc
count as "used", so there are no false removals); every method/field candidate was then
manually verified before removal. `./gradlew clean build` is **green**; both jars produced
(`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`); EN/DE/ES parity and `LanguageRegistry` tests pass.

## Internal

### Unused imports — 97 removed (across 30 files)
- **`OBX.java`** carried the largest cluster (~60 dead imports): feature command/listener
  classes left over from the Gradle multi-module migration — those commands now register
  inside their own feature modules, so the bootstrap no longer references them.
- The remaining unused imports were scattered one-or-two per file (e.g. `SchedulerAdapter`,
  `ChatColor`, `java.util.Set/UUID/Locale/Arrays`, `GameMode`, `Material`, `Placeholders`,
  several `org.bukkit.entity.*` in `AdminSubMenu` after its localization refactor).
- 3 of the 97 were **cascade** removals exposed only after dead methods/fields were deleted
  (`LinkedHashMap`, `java.lang.reflect.Field`, `java.util.concurrent.ConcurrentHashMap`).

### Dead private methods — 11 removed
- **`ComponentMessenger`** — an entire orphaned legacy Spigot-reflection path:
  `createHoverEvent`, `createHoverContent`, `createClickEvent`, `applyEvents`,
  `sendToPlayer` (superseded by the Adventure path + the surviving Spigot fallback).
- **`AdventureMessageUtil.emptyMap()`** and **`DisplayEntityBackend.unusedFieldRef()`** —
  both were `@SuppressWarnings("unused")` stubs that existed only to keep an import
  "referenced"; removed along with the now-genuinely-unused `LinkedHashMap` / `Field`
  imports they were propping up.
- **`ModerationCommand.findOnlinePlayer`**, **`JoinLeaveServiceImpl.orEmpty`**,
  **`ScoreboardRenderer.clearEntries`**, **`AdminSubMenu.fetchTps`** — unreferenced helpers
  (the last left over from the recent Server-Control localization refactor).

### Dead private fields/constants — 7 removed
- **`ComponentMessenger`** — the 4 bungee-event constants `HOVER_EVENT_CLASS`,
  `HOVER_EVENT_ACTION_CLASS`, `CLICK_EVENT_CLASS`, `CLICK_EVENT_ACTION_CLASS` (only the
  removed reflection methods used them; live code uses the `ADVENTURE_*` constants).
- **`Hologram.spawnedAtTick`** (vestigial `final long = 0L`, never read; serializer confirmed
  not to reflect over fields), **`PacketChannelInjector.FAILED_PROBE_GUARD`** (unused guard
  map), **`FlightStateService.DEFAULT_SPEED`** (unused constant).

## Method / Verification

- Imports: an import is flagged only when its **simple name** appears nowhere else in the
  file — safe by construction (a non-wildcard import is needed iff the type is referenced by
  simple name). Removed by original line number to avoid line-shift bugs; re-scanned twice
  to catch cascades.
- Methods/fields: private members are **file-local**, so a within-file reference count of 1
  (declaration only) proves they're dead. Each candidate was read in context and confirmed a
  genuine private declaration; stateful-looking fields were additionally grep-checked
  project-wide for reflective access (none found).
- A `grep -w` quirk in the local toolchain (treats `_` as a word boundary, so it confused
  `HOVER_EVENT_CLASS` with `ADVENTURE_HOVER_EVENT_CLASS`) was caught and the
  underscore-preserving awk scanner was used as the source of truth.

## Files (representative)

`plugin/.../OBX.java`, `core/.../util/text/ComponentMessenger.java`,
`core/.../util/message/AdventureMessageUtil.java`, `features/staff/.../gui/AdminSubMenu.java`,
`features/hologram/.../backend/DisplayEntityBackend.java`,
`features/hologram/.../model/Hologram.java`,
`features/hologram/.../packet/PacketChannelInjector.java`,
`features/moderation/.../command/ModerationCommand.java`,
`features/playerinfo/.../service/JoinLeaveServiceImpl.java`,
`features/scoreboard/.../format/ScoreboardRenderer.java`,
`features/playerstate/.../service/FlightStateService.java`, plus ~20 more files with
one/two unused imports each.

## Suggested Commit Message

```
Cleanup: remove ~97 unused imports, 11 dead private methods (incl. an orphaned legacy Spigot-reflection path in ComponentMessenger), and 7 unused private fields/constants project-wide; build green
```
