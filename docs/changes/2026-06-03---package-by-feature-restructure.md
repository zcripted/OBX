# Package-by-feature restructure + module system

■ **Created:** 2026-06-03 2:52 am (America/Detroit)

■ **Last Updated:** 2026-06-03 2:52 am (America/Detroit)

Converts OBX from package-by-layer to a professional package-by-feature layout
on top of a thin shared core, introduces a real feature-module lifecycle, renames
the main class to `OBX`, and relocates the language files. Done on an isolated
git worktree (branch `worktree-restructure-package-by-feature`) in green,
buildable waves.

## Categories

### Internal / Architecture
- **Main class renamed** `Main` → `OBX` (`dev.zcripted.obx.OBX`); updated all 264
  references, `plugin.yml` `main:`, and the `proguard.pro` keep rule.
- **New target layout** under `src/main/java/dev/zcripted/obx/`:
  - `OBX.java` — thin bootstrap (1047 → ~650 lines): builds core infra, then
    `moduleManager.enableAll()`; `onDisable` → `disableAll()`; reload → `reloadAll()`.
  - `core/` — cross-cutting framework: `module/` (Module, AbstractModule,
    ModuleManager), `service/ServiceRegistry`, `command/`, `gui/`, `storage/`,
    `language/`, `platform/`, `motd/`, `diagnostics/`.
  - `api/` — stable public API: `economy/` (EconomyService + VaultEconomyProvider),
    `hologram/` (HologramFacade + 2 public events).
  - `feature/` — 19 self-contained features: chat, economy, enchant, hologram,
    hub, item, jail, kit, mail, moderation, nickname, playerinfo, playerstate,
    scoreboard, staff, tablist, teleport, warp, world.
  - `util/` — stateless static helpers only.
- **Module framework** (`core/module/`):
  - `Module` — `id()/dependsOn()/enabledByDefault()/enable()/disable()/reload()`.
  - `AbstractModule` — `listener()/command()/service()/onDisable()` helpers with
    automatic teardown (unregister listeners, restore a disabled-command stub,
    drop services, run teardown hooks) so runtime toggling is safe.
  - `ModuleManager` — dependency-ordered (Kahn topo sort) enable/disable/reload,
    runtime `setEnabled(id, bool)` persisted to `config.yml` under `modules.<id>`.
  - `ServiceRegistry` — type-keyed shared-service container; OBX's ~40 getters now
    resolve from it, so feature modules own construction while every existing
    `plugin.getXService()` call site keeps working unchanged.
- **Each feature** now ships an `<Feature>Module` that constructs its services,
  registers them, binds its commands, registers its listeners, and declares its
  reload + teardown. Modules are registered in OBX in the historical construction
  order, which the manager preserves for independent modules — so init/teardown
  order and runtime behavior are unchanged.

### Language / Locale
- Generated language files relocated: `languages/language_en.yml` +
  `languages/sprache_de.yml` → `lang/en.yml` + `lang/de.yml`
  (`core/language/LanguageRegistry`, `core/language/LanguageFile`). EN/DE parity is
  still guaranteed by `MessageDefaults.def(key, en, de)`. `CLAUDE.md` updated.

### Files (representative)
- `src/main/java/dev/zcripted/obx/OBX.java`
- `src/main/java/dev/zcripted/obx/core/module/{Module,AbstractModule,ModuleManager}.java`
- `src/main/java/dev/zcripted/obx/core/service/ServiceRegistry.java`
- `src/main/java/dev/zcripted/obx/feature/<feature>/<Feature>Module.java` (×19)
- `src/main/java/dev/zcripted/obx/core/language/{LanguageRegistry,LanguageFile}.java`
- `proguard.pro`, `src/main/resources/plugin.yml`, `CLAUDE.md`
- 388 source + test files relocated (every package declaration + import rewritten)
- Full file→package mapping: `MIGRATION-MANIFEST.md` (worktree root)

## Verification
- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; **20 tests, 0 failures**;
  both jars built (`OBX-1.0.0-beta-b1.jar` + `-unobf`). Green build maintained at
  every wave (A, B, C, D.1–D.13, Z) — each committed separately.

## Decisions & caveats
- **No live-server runtime QA available here.** The module decomposition is verified
  by compile + the unit suite + static review. Init/teardown order was deliberately
  preserved (registration order = historical order; teardown hooks registered in
  reverse so they execute in the original sequence). A server smoke test of
  enable → reload → disable is recommended before release.
- **Two pre-existing unregistered listeners** (`jail/JailListener`,
  `nickname/NicknameApplyListener`) were never wired by the old bootstrap; left
  unwired to avoid changing behavior. Flagged as possible latent bugs.
- **Removed a dead double-bind of `/list`** (core ListCommand was always overwritten
  by the playerinfo one) and `/nick` (was bound both inline and via the module).
- **"Module Toggles" GUI kept as-is.** It toggles per-system *dormancy*
  (non-destructive), which is finer-grained than `ModuleManager`'s hard
  enable/disable; rewiring it would regress UX. Module-level on/off is available
  config-driven (`modules.<id>`) and via `ModuleManager.setEnabled(...)`. Surfacing
  module toggles in a dedicated GUI page is a clean follow-up.
- **Language relocation** changes the generated path; existing servers' old
  `languages/` files become orphaned and `lang/` is regenerated. Acceptable for a
  beta pre-release.
- Portable Maven (`/maven/`) is gitignored, so the worktree was linked to the main
  checkout's copy to build.

## Suggested Commit Message
```
Restructure to package-by-feature with a feature-module system

Rename Main -> OBX; move all 388 files into core/api/feature/util; add a
Module/AbstractModule/ModuleManager + ServiceRegistry framework and extract
all 19 features into self-contained modules with a thin OBX bootstrap;
relocate language files to lang/en.yml + lang/de.yml. Build green, 20 tests.
```
