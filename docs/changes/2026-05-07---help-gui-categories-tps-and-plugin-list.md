# Help GUI categories, /tps, and /pl overrides

■ **Created:** 2026-05-07 4:30 pm

■ **Last Updated:** 2026-05-07 4:30 pm

## Summary

- Added a category filter button to the bottom row of the `/help` GUI so admins
  and players can sort visible commands by category (All, Admin, Core, Language,
  Moderation, Teleport, Utility, Other). Categories are derived from the package
  of each command's executor; commands belonging to other plugins land under
  Other.
- Implemented a custom `/tps` command that reports rolling 1m / 5m / 15m TPS and
  current MSPT alongside player count and JVM uptime. The console variant uses
  OBX's existing `&` → ANSI translation pipeline so the in-game palette is
  preserved in terminals.
- Implemented a custom `/pl` (alias `/plugins`) command that lists every loaded
  plugin grouped by detected platform (Bukkit / Paper / Purpur / Folia). Empty
  sections are hidden. Status colors: enabled = light orange, broken = light
  red, disabled = light gray. Broken plugins are detected by scanning the
  plugins folder for JARs whose declared name in `plugin.yml` /
  `paper-plugin.yml` is not present in the loaded plugin set.
- Added `PlayerCommandPreprocessEvent` overrides for `/tps`, `/pl`, `/plugins`,
  and the `bukkit:` / `paper:` / `purpur:` / `spigot:` namespaced variants, so
  the OBX implementations are used regardless of plugin load order.

## Categories

### Commands

- `/help` (existing) — bottom-row category filter button now visible at slot 48.
  Left-click cycles to the next category, right-click to the previous, middle-
  click resets to All. Page resets to 1 on category change.
- `/help [page] [category]` — args may now include a category name (e.g.,
  `/help 2 Moderation`).
- `/tps` *(new)* — server performance report. Aliases: `lag`, `mspt`,
  `performance`. Permission: `obx.tps` (default `op`). Console-safe.
- `/pl` *(new)* — grouped plugin list. Aliases: `plugins`. Permission:
  `obx.pl` (default `op`). Console-safe.

### GUIs

- `HelpGuiMenu` — added category filter button (slot 48), category-aware page
  titles, per-entry category lore line, and click handlers for cycling
  categories.

### Permissions

- `obx.tps` *(new)* — default `op`. Required to view `/tps`.
- `obx.pl` *(new)* — default `op`. Required to view `/pl`.
- `obx.*` now grants `obx.tps` and `obx.pl` as children.

### Config

- No external config keys added.

### Internal

- New `TpsService` runs a tick-aligned task to maintain rolling sample windows
  for accurate TPS / MSPT metrics on any Bukkit fork (no NMS reflection).
- `CommandOverrideListener` aliases extended to cover `/tps` and `/pl` /
  `/plugins`, including `paper:` / `purpur:` / `spigot:` namespaces, with the
  known-namespaces set widened accordingly.

### API

- `Main#getTpsService()` exposes the new performance probe so other modules
  (e.g., diagnostics output) can read TPS / MSPT without instantiating their
  own measurer.

## Modified / Added Files

- `src/main/java/dev/zcripted/obx/Main.java`
- `src/main/java/dev/zcripted/obx/gui/player/HelpGuiHolder.java`
- `src/main/java/dev/zcripted/obx/gui/player/HelpGuiMenu.java`
- `src/main/java/dev/zcripted/obx/listener/menu/HelpGuiListener.java`
- `src/main/java/dev/zcripted/obx/listener/player/CommandOverrideListener.java`
- `src/main/java/dev/zcripted/obx/command/core/HelpGuiCommand.java`
- `src/main/java/dev/zcripted/obx/command/admin/TpsCommand.java` *(new)*
- `src/main/java/dev/zcripted/obx/command/admin/PluginListCommand.java` *(new)*
- `src/main/java/dev/zcripted/obx/util/perf/TpsService.java` *(new)*
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- `src/main/resources/plugin.yml`

## Suggested Commit Message

```
Feature (help/tps/pl): Help GUI category filter, /tps performance report, and /pl plugin list overrides
```
