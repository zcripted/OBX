# 🎒 Backpack + 🩺 Server Health Check + 📦 Box-Style Update Messages

> Two new features and three refinements: a per-player **portable backpack** with a
> token-based dupe-guard, a staff-only **`/health` server health check** with hover
> tooltips and click actions, **box-style** update messages everywhere, release checks
> every **15 minutes**, and the starter kit locked to **first join only**.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 5:21 PM EST |
| **Last Updated** | 2026-06-05 5:42 PM EST |
| **Author** | zcripted |
| **Scope** | New backpack feature module · health check command · updates UX · kit fix |
| **Files changed** | 20 (6 new · 14 modified) |
| **Categories** | Feature · Commands · Permissions · Messages/i18n · Storage · Config · Fix · Docs |
| **Verification** | ✅ `gradlew build` green · unit tests green (incl. EN/DE/ES parity) · both jars produced |

---

## 📋 Summary (patch notes)

- **🎒 Backpack** — every player now has a personal 3-row backpack: `/backpack` (or `/bp`)
  opens it anywhere. Want it as an item? `/backpack convert` hands you a real backpack item
  (a bundle on modern servers, a shulker box or chest on older ones) that opens on
  right-click. Lose it, burn it, blow it up — your items are safe: `/backpack respawn`
  gives the item back, and **every old copy instantly becomes void**, so neither the
  backpack nor anything inside it can ever be duplicated. The item also can't be placed
  down or stuffed inside itself.
- **🩺 `/health`** — staff get a one-command physical: TPS, tick time, CPU, memory,
  entities, chunks, players, ping, scheduler queues, database, and disk — all in one tidy
  box, color-graded green/yellow/red, with explanations on hover and one-click follow-ups
  (`/tps`, diagnostics, re-run).
- **📦 Prettier update messages** — every update check and notification now uses the same
  box layout as the rest of OBX instead of plain one-liners.
- **⏱ Faster release checks** — the server now re-checks for new OBX releases every
  **15 minutes** by default (was hourly). Still configurable.
- **🛠 Starter kit fixed** — the starter kit is granted **only on a player's very first
  join**. It can no longer be re-claimed with `/kit starter`; staff can still hand it out
  with `/kit give`.

---

## 🔄 Changes (newest → oldest)

### 📖 In-game help (5:42 PM)
- [`ObxHelpView.java`](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxHelpView.java) —
  `/obx health` entry in the `/obx help` catalog (Reload & Diagnostics, alias `healthcheck`;
  also listed by `/obx commands` + `/obx permissions`).
- [`HelpGuiMenu.java`](../../../core/src/main/java/dev/zcripted/obx/core/gui/help/HelpGuiMenu.java) —
  `/help` GUI categories: `health`/`healthcheck` → Admin, `backpack`/`bp`/`pack` → Utility.
- EN/DE/ES: new `commands.obx.entry.health.usage`/`.description` keys.

### 📚 Docs
- [`docs/information/COMMANDS+PERMISSIONS.md`](../../information/COMMANDS+PERMISSIONS.md) —
  `/backpack` + `/health` rows, `/kit` first-join note, `/obx updates` box/15-min note,
  `obx.backpack.*` wildcard row.
- [`docs/changes/2026-06-05---backpack-health-check-and-update-message-boxes.md`](../../changes/2026-06-05---backpack-health-check-and-update-message-boxes.md)

### 🌐 Messages — EN/DE/ES in lock-step
- [`MessageDefaultsEN.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
  · [`MessageDefaultsDE.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
  · [`MessageDefaultsES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  - `commands.obx.updates.*` → box-style lists (check results, notify toggles, announce pair)
  - New `backpack.*` (17 keys: boxes, guards, detailed item lore with dividers) and
    `health.*` (44 keys: header, sections, rows, hover tooltips, buttons) groups
  - New `kit.first-join-only` denial message
  - New `backpack` / `health` YAML section comments

### 🎒 Backpack — new feature module `:features:backpack`
- **NEW** [`BackpackModule.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/BackpackModule.java) — module wiring (service, command, listener, save-on-disable).
- **NEW** [`BackpackService.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/service/BackpackService.java) — SQLite `obx_backpack` table (uuid/token/physical/contents), Base64 inventory serialization, token rotation dupe-guard, version-aware item factory (BUNDLE → SHULKER_BOX → PURPLE_SHULKER_BOX → CHEST for 1.8.8→latest).
- **NEW** [`BackpackCommand.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/command/BackpackCommand.java) — open/convert/respawn/virtual.
- **NEW** [`BackpackListener.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/listener/BackpackListener.java) — right-click open + validation, void-copy purge, place/nesting guards, close/quit saves.
- [`settings.gradle.kts`](../../../settings.gradle.kts) · [`plugin/build.gradle.kts`](../../../plugin/build.gradle.kts) — subproject wired in.

### 🩺 Health check
- **NEW** [`HealthCommand.java`](../../../core/src/main/java/dev/zcripted/obx/core/diagnostics/HealthCommand.java) — gathering + box rendering; reflection-guarded probes degrade to `n/a` (Folia-safe).
- [`ObxCommand.java`](../../../plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java) — `/obx health` route + tab completion.
- [`Placeholders.java`](../../../core/src/main/java/dev/zcripted/obx/util/text/Placeholders.java) — 3-pair `with(...)` overload.

### ⚙️ Bootstrap, config & permissions
- [`OBX.java`](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java) — registers `BackpackModule`, binds `/health`.
- [`config.yml`](../../../plugin/src/main/resources/config.yml) — `updates.check-interval-minutes: 15`.
- [`plugin.yml`](../../../plugin/src/main/resources/plugin.yml) — `backpack` (aliases `bp`, `pack`) + `health` (alias `healthcheck`) commands; `obx.backpack.*` (default true) + `obx.admin.health` (default op) permissions.

### ⏱ Updates module
- [`UpdateNotificationService.java`](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java) — periodic check default 60 → **15** minutes.

### 🛠 Kit fix
- [`KitCommand.java`](../../../features/kit/src/main/java/dev/zcripted/obx/feature/kit/command/KitCommand.java) — first-join kits refuse the self-claim path; staff `/kit give` unaffected.

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** (compile + tests → `:plugin:shadowJar` →
  `:plugin:proguard`); `OBX-<ver>-unobf.jar` and `OBX-<ver>.jar` produced.
- `MessageDefaultsTest` EN/DE/ES key + shape parity green with all new key groups.

## Suggested Commit Message
```
Feature (backpack, health): portable dupe-guarded backpack + staff health check; box-style update messages, 15-min checks, first-join-only starter kit
```
