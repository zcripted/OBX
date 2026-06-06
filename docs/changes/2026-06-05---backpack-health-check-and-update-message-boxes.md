# Backpack Feature, Server Health Check, Box-Style Update Messages, 15-min Checks, Starter-Kit Fix

■ **Created:** 2026-06-05 5:21 pm

■ **Last Updated:** 2026-06-05 5:42 pm

## Summary

Five changes in one pass:

1. **Box-style update messages** — every update check/notification message (`/obx updates`
   results, join/periodic release announcements, notify toggles) now renders in the OBX
   box style (`▍ 𝗢𝗕𝗫 › Updates · …` header, divider, aligned rows).
2. **Backpack feature** (new `:features:backpack` module) — `/backpack` (aliases `/bp`,
   `/pack`): every player gets one persistent 3-row (27-slot) portable storage.
   `convert` turns it into a physical item (version-appropriate material: BUNDLE 1.17+ →
   SHULKER_BOX 1.13+ → PURPLE_SHULKER_BOX 1.11/1.12 → CHEST 1.8.8+); right-clicking the item
   opens the same stored inventory. If the item is lost/destroyed/burned, `respawn`
   re-issues it; an instance **token dupe-guard** voids every older copy (contents live only
   in SQLite, never on the item, so duplicating the item can never duplicate items).
   Void copies are purged on sight; the item cannot be placed as a block or nested inside
   the backpack.
3. **Server health check** — new staff-only `/health` (alias `/healthcheck`, also
   `/obx health`; permission `obx.admin.health`, default op): one clean box-style report
   covering TPS (1m/5m/15m), tick time vs 50 ms budget, process/system CPU + cores, heap
   memory, entities & chunks, players & average/worst ping, sync queue & async workers,
   SQLite store state/size, disk capacity, and per-player averages. Every row carries a
   hover tooltip; key rows + footer `[⟳ Re-run] [TPS Detail] [Diagnostics]` buttons carry
   click actions. Values are color-graded green/yellow/red.
4. **15-minute release checks** — the periodic OBX release re-check now defaults to every
   **15 minutes** (was 60); still configurable via `updates.check-interval-minutes`.
5. **Fix: starter kit only on first join** — kits flagged `first-join: true` can no longer
   be self-claimed via `/kit <name>` after the automatic first-join grant; only the staff
   `/kit give` path can re-issue them.

## Categories

### New Feature — Backpack (`:features:backpack`)
- **New:** `features/backpack/build.gradle.kts`
- **New:** `features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/BackpackModule.java`
- **New:** `features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/service/BackpackService.java`
  — SQLite table `obx_backpack (uuid, token, physical, contents)`; Base64 inventory
  serialization; token rotation (dupe-guard); version-aware item factory with localized,
  divider-formatted lore + machine-read `Owner:`/`ID:` tag lines; save on close/quit/disable.
- **New:** `features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/command/BackpackCommand.java`
  — open / convert / respawn / virtual subcommands with space checks before any mutation.
- **New:** `features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/listener/BackpackListener.java`
  — right-click-to-open with owner+token validation, stale-copy purge, place guard,
  nesting guard (click/shift-click/hotbar-swap/drag), close/quit persistence.
- `settings.gradle.kts`, `plugin/build.gradle.kts` — `backpack` subproject wired in.
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — module registered.

### New Command — Server Health Check
- **New:** `core/src/main/java/dev/zcripted/obx/core/diagnostics/HealthCommand.java`
  — metric gathering with reflection-guarded probes (Paper `getEntityCount`,
  `Player#getPing` → NMS `ping` field, `com.sun.management` CPU loads), Folia-safe
  degradation to `n/a`; box rendering via language keys + `ComponentMessenger`.
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — `/health` bound in `registerCommands`.
- `plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java` — `/obx health`
  subcommand + tab completion.
- `core/src/main/java/dev/zcripted/obx/util/text/Placeholders.java` — added a 3-pair
  `with(...)` overload.

### Updates Module
- `core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java`
  — `DEFAULT_INTERVAL_MINUTES` 60 → **15**.
- `plugin/src/main/resources/config.yml` — `updates.check-interval-minutes: 15`.
- All `commands.obx.updates.*` messages converted to box-style lists (EN/DE/ES).

### Fix — Kits
- `features/kit/src/main/java/dev/zcripted/obx/feature/kit/command/KitCommand.java`
  — non-staff claim path refuses kits with `first-join: true` (new `kit.first-join-only`
  message); `/kit give` (staff) unaffected.

### Config & Permissions
- `plugin/src/main/resources/plugin.yml` — new commands `backpack` (aliases `bp`, `pack`)
  and `health` (alias `healthcheck`); new permissions `obx.backpack.*`/`use`/`convert`/`respawn`
  (default **true**) and `obx.admin.health` (default **op**, added to `obx.admin.*` children;
  `obx.backpack.*` added to `obx.*`).

### Messages (EN/DE/ES updated in lock-step)
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`
- New key groups: `backpack.*` (17 keys), `health.*` (44 keys), `kit.first-join-only`;
  `commands.obx.updates.*` re-shaped to box lists. New `backpack`/`health` section
  comments in all three catalogues.

### In-Game Help (added 5:42 pm)
- `core/src/main/java/dev/zcripted/obx/core/command/ObxHelpView.java` — `/obx health`
  entry added to the `/obx help` catalog (Reload & Diagnostics category, alias
  `healthcheck`; also shown by `/obx commands` and `/obx permissions`).
- `core/src/main/java/dev/zcripted/obx/core/gui/help/HelpGuiMenu.java` — `/help` GUI
  category mapping: `health`/`healthcheck` → Admin, `backpack`/`bp`/`pack` → Utility
  (previously they would have fallen into "Other").
- New `commands.obx.entry.health.usage`/`.description` keys in EN/DE/ES.

### Docs
- `docs/information/COMMANDS+PERMISSIONS.md` — `/backpack`, `/health` rows; `/kit` and
  `/obx updates` descriptions updated; `obx.backpack.*` wildcard row.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**: full pipeline incl. `:features:backpack`,
  unit tests (incl. `MessageDefaultsTest` EN/DE/ES parity), `:plugin:shadowJar`,
  `:plugin:proguard`; both jars produced.

## Suggested Commit Message
```
Feature (backpack, health): portable dupe-guarded backpack + staff health check; box-style update messages, 15-min checks, first-join-only starter kit
```
