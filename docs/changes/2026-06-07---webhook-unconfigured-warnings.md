# Unconfigured-Webhook Warnings — Startup Console + Admin Join, Triple Toggle

■ **Created:** 2026-06-07 5:28 pm

■ **Last Updated:** 2026-06-07 5:40 pm

While Discord webhook settings still hold their shipped blank/placeholder values, OBX
warns the console on each startup and admins on join (above the welcome MOTD, hover
lists the affected paths). Stops automatically once configured. Full breakdown in the
commit log:
[docs/commits/2026-06-07/webhook-unconfigured-warnings.md](../commits/2026-06-07/webhook-unconfigured-warnings.md)

## Categories

### Commands
- New `/obx warn <on|off|status>` (permission `obx.admin.warnings`, default op) —
  instant effect, persisted to `config.yml`, no reload needed; box-style reply with a
  `[Toggle]` button + console audit line.
- `plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java`
- `core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java`

### GUIs
- Admin GUI → Module Toggles gains a **Webhook Warnings** tile (instant + persisted).
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`

### Internal (new core service)
- `core/src/main/java/dev/zcripted/obx/core/diagnostics/WebhookWarningService.java` *(new)*
  — checks `discord.moderation.webhook-url/server-id/channel-id` and
  `economy.reporting.discord-webhook` for blank/placeholder values; console warning at
  startup; HIGH-priority join warning (sent before the 10-tick-delayed welcome MOTD)
  with OBX hover (per-setting list, fix hint, click-to-hide).
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — wiring + reload hook.

### Config / Permissions
- `plugin/src/main/resources/config.yml` — `warnings.webhook-unconfigured: true`
  (file edits apply on `/obx reload`).
- `plugin/src/main/resources/plugin.yml` — `obx.admin.warnings` permission.

### Internal (i18n)
- 10 new keys × EN/DE/ES (`admin.modules.warn.*`, `admin.warnings.webhook.*`,
  `admin.gui.module.webhook-warn`).
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

### Docs
- `docs/information/COMMANDS+PERMISSIONS.md` — `/obx warn` row.

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass; both jars produced.

## Suggested Commit Message
```
Feature (warnings): unconfigured Discord-webhook warnings — startup console + admin join (above MOTD), /obx warn + GUI tile + config toggle
```
