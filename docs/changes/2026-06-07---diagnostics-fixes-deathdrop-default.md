# Diagnostics Fixes + Death Grouping On By Default

■ **Created:** 2026-06-07 4:40 pm

■ **Last Updated:** 2026-06-07 4:40 pm

Three `/obx diagnostics` findings closed. Full breakdown in the commit log:
[docs/commits/2026-06-07/diagnostics-fixes-deathdrop-default.md](../commits/2026-06-07/diagnostics-fixes-deathdrop-default.md)

## Categories

### Config (deathdrop default)
- Death-item grouping module now **enabled by default** (was opt-in). Existing live
  configs with an explicit `modules.deathdrop: false` keep their value — flip it or run
  `/obx deathdrop on`.
- `features/deathdrop/src/main/java/dev/zcripted/obx/feature/deathdrop/DeathDropModule.java`
- `plugin/src/main/resources/config.yml`

### Commands (diagnostics — fixes)
- **Fix:** the `/obx diagnostics full` Errors row now gives **each listed issue its own
  hover** with details for that issue only (missing-file list, disabled-module list,
  storage cause + fix hint) instead of one combined hover.
- **Fix:** `moderation.yml` was falsely flagged missing — it's a migrate-once legacy file
  (moderation lives in SQLite). Removed from the key-file check; `/obx config validate`
  now validates the SQLite store (`moderation (SQLite)` row).
- `core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java`

### Internal (i18n)
- `commands.obx.config.validation` + `commands.obx.reload.config.hover` updated
  (`moderation.yml` → `moderation (SQLite)`) in EN/DE/ES.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass; both jars produced.

## Suggested Commit Message
```
Fix (diagnostics): per-issue error hovers, drop stale moderation.yml check; enable deathdrop module by default
```
