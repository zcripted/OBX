# Custom Player-Command Console Log

■ **Created:** 2026-06-07 7:40 pm

■ **Last Updated:** 2026-06-07 7:40 pm

Replaces the vanilla `<name> issued server command: /cmd` console line with a styled,
configurable OBX log line (+ world/date/time context line). Full breakdown in the
commit log:
[docs/commits/2026-06-07/custom-command-console-log.md](../commits/2026-06-07/custom-command-console-log.md)

## Categories

### Console (Feature)
- New `console.command-log` config: `enabled`, `suppress-vanilla`, `timezone`
  (default America/Detroit), `format` lines with `{player}` `{command}` `{world}`
  `{date}` `{date-iso}` `{time}` placeholders; ANSI truecolor rendering; vanilla line
  suppressed via a reflective (proxy-based, no compile dep) log4j2 root-logger filter.
- Cancelled commands aren't logged (matches vanilla); filter failure degrades to
  showing both lines; config edits apply on `/obx reload`.
- `core/src/main/java/dev/zcripted/obx/core/console/PlayerCommandConsoleLog.java` *(new)*
- `plugin/src/main/java/dev/zcripted/obx/OBX.java`
- `plugin/src/main/resources/config.yml`

## Verification
- `.\gradlew.bat build` green — both jars produced; obfuscated jar boots clean on
  Paper 1.21.4 (`runServerObf`).

## Suggested Commit Message
```
Feature (console): styled player-command log replaces vanilla "issued server command" line (log4j proxy filter + configurable format)
```
