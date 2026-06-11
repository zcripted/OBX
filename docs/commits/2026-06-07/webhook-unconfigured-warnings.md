# 🔔 Unconfigured-Webhook Warnings — Startup Console + Admin Join, Triple Toggle

> While any Discord webhook setting still holds its shipped blank/placeholder value
> (`discord.moderation.webhook-url` / `server-id` / `channel-id`,
> `economy.reporting.discord-webhook`), OBX now warns: **(1)** the console at every
> startup (WARN lines listing the unconfigured paths), and **(2)** admins
> (`obx.admin.warnings`, default op) on join — sent synchronously so the line always
> lands **above the welcome MOTD** (which is dispatched 10 ticks later), with an
> OBX-style hover listing each affected setting plus fix/hide hints (click suggests the
> hide command). The warnings stop on their own the moment real values are in place.
> Disable them three ways: `/obx warn off` (instant, persisted, no reload),
> the Admin GUI **Module Toggles** tile (instant, persisted), or
> `warnings.webhook-unconfigured: false` in `config.yml` (applies on `/obx reload`).

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 5:28 PM EST |
| **Last Updated** | 2026-06-07 5:40 PM EST |
| **Author** | zcripted |
| **Scope** | Core diagnostics, /obx command, Admin GUI, config.yml, plugin.yml, i18n, docs |
| **Files changed** | 10 code/config + 3 docs |
| **Categories** | Feature · Commands · Permissions · GUIs · Config · i18n |
| **Verification** | ✅ `gradlew build` green (tests incl. EN/DE parity, both jars) |

---

## 📋 Summary (patch notes)

- **You'll know when your Discord hookups aren't hooked up.** Until the webhook URL and
  server/channel ids are real values, every boot prints a console warning and joining
  admins get a yellow chat line (above the welcome MOTD) whose hover lists exactly which
  config paths still need values, how to fix them, and how to hide the warnings.
- **"Configured" is validated, not just non-empty:** webhook URLs must look like real
  Discord webhook endpoints (`https://…/api/webhooks/…`), ids must be numeric snowflakes,
  and obvious template text (`YOUR…`, `PASTE…`, `<…>`, `{…}`) still counts as
  unconfigured.
- **Three toggles, clear semantics.** `/obx warn on|off|status` (box-style reply
  with a `[Toggle]` button + console audit line) and the Admin GUI Module Toggles tile
  take effect immediately and write back to `config.yml` — no reload. Editing
  `warnings.webhook-unconfigured` in the file directly applies on `/obx reload` /
  `/obx reload config`.

## 🔧 Changes (newest at top → oldest)

### Core service
- **NEW** [core/src/main/java/dev/zcripted/obx/core/diagnostics/WebhookWarningService.java](../../../core/src/main/java/dev/zcripted/obx/core/diagnostics/WebhookWarningService.java)
  — placeholder detection (URL shape, snowflake ids, template text), startup console
  warning, HIGH-priority join listener (permission-gated, hover + click-to-hide),
  `reload()` for config edits, `setEnabled()` persisting instantly.

### Wiring
- [plugin/src/main/java/dev/zcripted/obx/OBX.java](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java)
  — service constructed/registered in `onEnable` (listener + startup log);
  `reloadPlugin()` re-reads the toggle right after `reloadConfig()`.

### Command
- [plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java](../../../plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java)
  — `warn` subcommand + tab completion (`on|off|status`).
- [core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java)
  — `handleWarn` (box status with live unconfigured count, `[Toggle]` button,
  console audit line), mirroring the deathdrop toggle pattern. Subcommand renamed
  `webhookwarn` → `warn` before release.

### Admin GUI
- [features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java)
  — Module Toggles gains a **Webhook Warnings** tile (slot 19, bell icon with 1.12-safe
  fallbacks) backed by the service's instant + persisted setter.

### Config & permissions
- [plugin/src/main/resources/config.yml](../../../plugin/src/main/resources/config.yml)
  — new `warnings.webhook-unconfigured: true` (commented with toggle/reload semantics).
- [plugin/src/main/resources/plugin.yml](../../../plugin/src/main/resources/plugin.yml)
  — new `obx.admin.warnings` (default op; child of `obx.*`).

### i18n (EN/DE/ES — 10 new keys each)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  — `admin.modules.warn.*` (boxes, button, hover), `admin.warnings.webhook.*`
  (join line + hover title/entry/fix/hide), `admin.gui.module.webhook-warn`.

### Docs
- [docs/information/COMMANDS+PERMISSIONS.md](../../information/COMMANDS+PERMISSIONS.md)
  — `/obx warn` row (Modules section) with `obx.admin.warnings`.
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---webhook-unconfigured-warnings.md](../../changes/2026-06-07---webhook-unconfigured-warnings.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`).
