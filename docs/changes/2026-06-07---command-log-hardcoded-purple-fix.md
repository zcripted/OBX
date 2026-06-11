■ **Created:** 2026-06-07 3:00 pm EST
■ **Last Updated:** 2026-06-07 3:15 pm EST

## Categories
Internal, Config, Console

## Summary
Command console log is now hardcoded (removed from config.yml), and the
`[COMMAND]` log now uses the same ANSI rendering pipeline as the OBX brand
tags so `[COMMAND]`, `[OBX]`, `[Shop]`, `[Economy]`, `[Arcanum]`, and
`[Holograms]` all render in the same light purple.

## Changes

### Internal
- **`core/.../console/PlayerCommandConsoleLog.java`**: Replaced all config reads
  (`console.command-log.enabled`, `.suppress-vanilla`, `.timezone`, `.format`)
  with hardcoded constants. Replaced `AdventureMessageUtil.renderAnsi()` (which
  produced 24-bit truecolor `\u001B[38;2;255;85;255m`) with
  `ChatColor.translateAlternateColorCodes()` so the output flows through the
  same `applyAnsi()` → `\u001B[95m` pipeline as the OBX brand tags.

### Config
- **`plugin/src/main/resources/config.yml`**: Removed the entire
  `console.command-log` section (lines 244–257). The command log is no longer
  configurable.

### Minor
- **`plugin/.../OBX.java`**: Updated comment to reflect hardcoded nature.
- **`core/.../message/ConsoleLog.java`**: Comment cleanup only (no code change).

## Verification
- Gradle build: **BUILD SUCCESSFUL**
- Both jars produced without errors
