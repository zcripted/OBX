# 🩺 Diagnostics Fixes + Death Grouping On By Default

> Three `/obx diagnostics` findings closed: (1) the **deathdrop** module showed disabled —
> it was wired but deliberately opt-in; it now ships enabled by default. (2) The
> **Errors row** of `/obx diagnostics full` carried one hover across the whole
> comma-joined value; each listed issue ("storage unavailable", "N config file(s)
> missing", "N module(s) disabled") is now its **own hover part** with details specific
> to that issue. (3) `moderation.yml` was falsely reported missing — moderation data
> moved to SQLite long ago and the yml is a migrate-once legacy file (renamed to
> `.migrated`), so the stale checks were removed/repointed at the SQLite store.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 4:40 PM EST |
| **Last Updated** | 2026-06-07 4:40 PM EST |
| **Author** | zcripted |
| **Scope** | Core diagnostics, deathdrop module default, config.yml, i18n |
| **Files changed** | 6 code/config + 2 docs |
| **Categories** | Fix · Commands · Config · i18n |
| **Verification** | ✅ `gradlew build` green (tests incl. EN/DE parity, both jars) |

---

## 📋 Summary (patch notes)

- **Death-item grouping is now on by default.** The module was fully wired but shipped
  opt-in (`enabledByDefault() = false`, `modules.deathdrop: false`). New installs get it
  enabled; it remains toggleable via the Module Toggles GUI, `/obx deathdrop on|off`, or
  config. **Note for existing servers:** a live `config.yml` that already contains
  `modules.deathdrop: false` keeps its explicit value — flip it (or `/obx deathdrop on`,
  which writes back) to enable.
- **Per-issue hovers on the diagnostics Errors row.** Hovering "1 config file(s)
  missing" now shows exactly which files; hovering "N module(s) disabled" lists exactly
  which modules (plus the cause); "storage unavailable" explains the source and a fix
  hint. Previously one combined hover covered the whole row.
- **No more false "moderation.yml missing".** Moderation data lives in SQLite;
  `moderation.yml` is only read once for legacy migration and renamed to `.migrated`.
  The diagnostics key-file check no longer lists it, and `/obx config validate` now
  validates the moderation SQLite store instead of the dead yml (row renamed to
  `moderation (SQLite)` in EN/DE/ES; the `/obx reload config` hover updated to match).

## 🔧 Changes (newest at top → oldest)

### Diagnostics (core)
- [core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java)
  — `sendErrorsLine` + `addErrorPart` replace the single `buildErrorsHover`: each issue is
  its own `InteractiveMessagePart` with a scoped hover (storage cause/fix, missing-file
  list, disabled-module list); `moderation.yml` removed from the key-file existence check;
  `/obx config validate` moderation row now checks the SQLite store.

### Death grouping default
- [features/deathdrop/src/main/java/dev/zcripted/obx/feature/deathdrop/DeathDropModule.java](../../../features/deathdrop/src/main/java/dev/zcripted/obx/feature/deathdrop/DeathDropModule.java)
  — removed the `enabledByDefault() = false` override (interface default is `true`);
  javadoc updated.
- [plugin/src/main/resources/config.yml](../../../plugin/src/main/resources/config.yml)
  — `modules.deathdrop: false` → `true`.

### i18n (EN/DE/ES)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `commands.obx.config.validation` row `moderation.yml` → `moderation (SQLite)`;
  `commands.obx.reload.config.hover` entry `- moderation.yml` → `- moderation (SQLite)`.

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---diagnostics-fixes-deathdrop-default.md](../../changes/2026-06-07---diagnostics-fixes-deathdrop-default.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`).
