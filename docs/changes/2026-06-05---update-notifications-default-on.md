# Update Notifications — Startup Check, Periodic Re-Check, Persisted Default-ON Opt-Out

■ **Created:** 2026-06-05 4:50 pm

■ **Last Updated:** 2026-06-05 4:50 pm

## Summary

The update checker previously only ran on demand (`/obx updates`) or when a player who had
manually opted in re-joined — and the opt-in list was in-memory, so it was wiped every restart.
This change makes update awareness automatic and durable:

1. **Startup check (console):** one async check during `onEnable`; the console gets a themed
   `[OBX][Updates]` line for every outcome (new release / up to date / check failed).
2. **Periodic re-check:** while the server runs, OBX re-checks every
   `updates.check-interval-minutes` (default 60) and announces a newly found release **once per
   version** — to the console and to every eligible online player. So a release published
   mid-session is now noticed without anyone re-joining.
3. **Default-ON, persisted opt-out:** players holding `obx.updates.notify` (default: op) now
   receive notifications **by default** — on join and via the periodic announce.
   `/obx updates notify` flips the preference, which is stored in SQLite
   (`obx_update_notify_optout`) and survives restarts. If the database is unavailable the
   toggle still works in-memory for the session.

All network I/O stays off the main thread (existing `UpdateChecker.checkAsync` plumbing);
results are delivered on the main/global thread, Folia-safe.

## Categories

### Internal (new service)
- **New:** `core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java` —
  owns the `UpdateChecker`, the startup check, the periodic re-check (rescheduled on
  `/obx reload`), the once-per-version announce dedupe, and the persisted opt-out set
  (keyed by UUID; `CONSOLE` for the console sender).
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — constructs + registers the service in the
  `ServiceRegistry` before commands bind; `start()` after listeners, `stop()` in `onDisable`,
  `reload()` (with timing entry `update-checker`) in `reloadPlugin()`.
- `plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java` — resolves the shared
  service from the registry instead of constructing a bare `UpdateChecker`.
- `core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java` — the in-memory
  opt-IN set is gone; `/obx updates notify` delegates to the service's persisted toggle, and
  the join listener delegates to `UpdateNotificationService.notifyOnJoin`.

### Config
- `plugin/src/main/resources/config.yml` — new `updates:` section:
  - `check-on-startup` (default `true`)
  - `check-interval-minutes` (default `60`, `0` disables the periodic re-check)
  - `notify-players` (default `true`, master switch for the in-game messages)

### Permissions
- `plugin/src/main/resources/plugin.yml` — `obx.updates.notify` description rewritten to state
  the default-ON behaviour and persisted opt-out (default level unchanged: `op`).

### Messages (EN/DE/ES updated in lock-step)
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`
- Edited keys (no new keys, so EN/DE/ES parity holds by construction):
  - `commands.obx.updates.notify.enabled` — now green, notes default + persistence
  - `commands.obx.updates.notify.disabled` — now red, notes persistence
  - `commands.obx.entry.updates-notify.description` — documents default-ON + persisted opt-out

### Storage
- New SQLite table `obx_update_notify_optout (id TEXT PRIMARY KEY)`, created via
  `CREATE TABLE IF NOT EXISTS` on service start (consistent with the per-service table
  pattern; no schema-version bump needed for an additive table).

### Docs
- `docs/information/COMMANDS+PERMISSIONS.md` — `/obx updates notify` row documents the
  default-ON + persistence behaviour.

## Behaviour matrix (after this change)

| Scenario | Notifies? |
|---|---|
| Server startup | ✅ Console line (any outcome), config-gated |
| Release published while server runs | ✅ Once per version on the next periodic check (console + eligible players) |
| Eligible player joins while outdated | ✅ On join (async check) |
| `/obx updates` run manually | ✅ Unchanged |
| Opt-out surviving a restart | ✅ Persisted in SQLite |

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; full pipeline (compile + tests incl.
  `MessageDefaultsTest` parity + `:plugin:shadowJar` + `:plugin:proguard`), both jars produced.

## Suggested Commit Message
```
Feature (updates): startup + periodic release checks, default-on persisted notifications
```
