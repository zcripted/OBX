# Command Console Log Hardcoded + Light Purple Consistency

|              |                                                                  |
|--------------|------------------------------------------------------------------|
| **Status**   | 🟡 Ready to commit                                               |
| **Created**  | 2026-06-07 3:00 pm EST                                           |
| **Updated**  | 2026-06-07 3:15 pm EST                                           |
| **Author**   | opencode                                                         |
| **Scope**    | Console rendering, config                                        |
| **Files**    | [`PlayerCommandConsoleLog.java`](../../../core/src/main/java/dev/zcripted/obx/core/console/PlayerCommandConsoleLog.java) · [`config.yml`](../../../plugin/src/main/resources/config.yml) · [`OBX.java`](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java) · [`ConsoleLog.java`](../../../core/src/main/java/dev/zcripted/obx/util/message/ConsoleLog.java) (comment only) |
| **Build**    | ✅ `BUILD SUCCESSFUL`                                              |

## Summary

- The `console.command-log` config section was removed — the command log is now
  entirely hardcoded.
- `[COMMAND]` no longer renders as a different purple than `[OBX]` / `[Shop]` /
  `[Economy]` / `[Arcanum]` / `[Holograms]`. The root cause was that
  `AdventureMessageUtil.renderAnsi()` emitted a 24-bit truecolor escape
  (`\u001B[38;2;255;85;255m`) while the OBX brand tags used the ANSI indexed
  colour (`\u001B[95m`). Both now go through `ChatColor.translateAlternateColorCodes`
  → `applyAnsi()` → `\u001B[95m`.

## Improvements

- **Command log hardcoded.** Removed `console.command-log` from `config.yml`.
  All settings (`enabled`, `suppress-vanilla`, `timezone`, `format`) are now
  compile-time constants in `PlayerCommandConsoleLog.java`.
- **Consistent light purple.** Both the OBX brand tags (`[OBX][Shop]`, etc.)
  and the command `[COMMAND]` lines now render through the same ANSI pipeline,
  producing identical escape sequences.

## Patch

- `ConsoleLog.java` comment — removed stale "matching the startup banner"
  parenthetical (no code change).
