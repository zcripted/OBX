# 🔔 Update Notifications — Startup Check, Periodic Re-Check, Default-ON Persisted Opt-Out

> OBX now notices its own updates: one console check at startup, a re-check every hour while
> the server runs (announced **once per release**), and in-game notifications that are **ON by
> default** for staff holding `obx.updates.notify` — with the opt-out saved to the database so
> it survives restarts.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 4:50 PM EST |
| **Last Updated** | 2026-06-05 4:50 PM EST |
| **Author** | zcripted |
| **Scope** | Update checker lifecycle + notification persistence |
| **Files changed** | 10 (1 new · 9 modified) |
| **Categories** | Internal · Config · Permissions · Messages/i18n · Storage · Docs |
| **Verification** | ✅ `gradlew build` green · unit tests green (incl. EN/DE/ES parity) · both jars produced |

---

## 📋 Summary (patch notes)

Before this change, OBX only checked for updates when someone ran `/obx updates`, or when a
player who had manually opted in happened to re-join — and that opt-in list was forgotten on
every restart. Now:

- **You hear about it at startup** — the console prints a clear `[OBX][Updates]` line on boot:
  a new release is available (with the BuiltByBit download link), you're up to date, or the
  check couldn't reach GitHub.
- **You hear about it mid-session** — the server quietly re-checks every 60 minutes (configurable)
  and announces a newly published release **once** — to the console and to online staff. No more
  missing a release because nobody re-joined.
- **Staff are notified by default** — anyone with `obx.updates.notify` (ops by default) gets the
  join-time and mid-session notices automatically. Don't want them? `/obx updates notify` turns
  them off **permanently** — the choice is stored in OBX's database and survives restarts
  (run it again to turn them back on).
- **Nothing touches the main thread** — all GitHub queries run async (Folia-safe), exactly like
  the existing `/obx updates` command.

---

## 🔄 Changes (newest → oldest)

### 📚 Docs
- [`docs/information/COMMANDS+PERMISSIONS.md`](../../information/COMMANDS+PERMISSIONS.md) —
  `/obx updates notify` row now documents default-ON behaviour + persisted opt-out.

### 🌐 Messages (EN/DE/ES edited in lock-step — no new keys, parity holds by construction)
- [`core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
- [`core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
- [`core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  - `commands.obx.updates.notify.enabled` → green; notes "default; saved across restarts"
  - `commands.obx.updates.notify.disabled` → red; notes "saved across restarts"
  - `commands.obx.entry.updates-notify.description` → documents default-ON + persisted opt-out

### ⚙️ Config & Permissions
- [`plugin/src/main/resources/config.yml`](../../../plugin/src/main/resources/config.yml) — new
  `updates:` section — `check-on-startup: true`, `check-interval-minutes: 60` (`0` disables),
  `notify-players: true` (master switch for the in-game messages).
- [`plugin/src/main/resources/plugin.yml`](../../../plugin/src/main/resources/plugin.yml) —
  `obx.updates.notify` description rewritten for the default-ON model (default level stays `op`).

### 🧩 Internal — new service + wiring
- **NEW** [`core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java`](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java) —
  owns the `UpdateChecker`; runs the startup check, schedules/reschedules the periodic re-check,
  dedupes announces (once per version per runtime), evaluates eligibility
  (config master switch + `obx.updates.notify` + not opted out), and persists opt-outs in
  SQLite table `obx_update_notify_optout` (UUID-keyed; `CONSOLE` for the console). Degrades to
  in-memory toggles when the store is unavailable.
- [`plugin/src/main/java/dev/zcripted/obx/OBX.java`](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java) —
  registers the service in the `ServiceRegistry` before commands bind; `start()` after
  listeners, `stop()` in `onDisable`, `reload()` in `reloadPlugin()` (timed as `update-checker`).
- [`plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java`](../../../plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java) —
  resolves the shared service from the registry (fallback: fresh unstarted instance).
- [`core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java`](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java) —
  removed the in-memory opt-IN set; `/obx updates notify` now calls the service's persisted
  toggle and the join listener delegates to `notifyOnJoin` (default-ON semantics).

### 📄 Change file
- [`docs/changes/2026-06-05---update-notifications-default-on.md`](../../changes/2026-06-05---update-notifications-default-on.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** (compile + tests → `:plugin:shadowJar` →
  `:plugin:proguard`); `OBX-<ver>-unobf.jar` and `OBX-<ver>.jar` both produced.
- `MessageDefaultsTest` (EN/DE/ES key parity) green — only existing keys were edited.

## Suggested Commit Message
```
Feature (updates): startup + periodic release checks, default-on persisted notifications
```
