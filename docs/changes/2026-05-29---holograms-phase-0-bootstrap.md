# Holograms — Phase 0: Bootstrap & dormant service

■ **Created:** 2026-05-29 12:30 pm
■ **Last Updated:** 2026-05-29 12:30 pm

Bootstrap phase of the custom hologram subsystem outlined in
`planning/brainstorming.txt`. Adds the package skeleton, an inert
`HologramService`, the master flag file, and the wiring into `Main` so the
module starts dormant on every existing install.

No runtime behavior changes for users until `systems/holograms.yml →
enabled: true` is set.

## Categories

### Internal — package skeleton

* New package root `dev.sergeantfuzzy.sfcore.hologram` with submodules
  `backend/`, `model/`, `packet/`, `service/`.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/model/HologramId.java` —
  validated, case-folded name wrapper (lowercase alphanumeric + `_`/`-`,
  1–32 chars). Returned from `HologramId.parse(...)` for user input,
  `HologramId.of(...)` for trusted loads.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/model/Hologram.java` —
  mutable model carrying `HologramId`, `Location` (cloned in/out), an
  ordered `HologramLine` list, a `HologramSettings` block, backend-allocated
  entity ids, current/personal-hidden viewer sets, and a `dirty` flag.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/model/HologramLine.java`
  — abstract base with `TextLine`, `IconLine`, `BlockLine` variants.
  Template text on `TextLine` is the raw user string; resolution lives in
  Phase 3.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/model/HologramSettings.java`
  — billboard / scale / ranges / double-sided / shadow / see-through /
  background color / opacity / line width, plus stubs for interaction
  (Phase 4) and visibility (Phase 6) fields so persistence stays stable.

### Internal — backend abstraction

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/HologramBackend.java`
  — interface: `spawn`, `updateVisibility`, `applyMutations`, `destroy`,
  `supportsPerViewerText`, `describe`.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/DisplayEntityBackend.java`
  — Paper / Spigot ≥ 1.19.4 backend. Phase 0 implements only describe + the
  empty-but-idempotent lifecycle hooks. No `TextDisplay` symbol referenced
  at file scope — every 1.19.4-only API is reached via reflection in
  Phase 1.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/ArmorStandBackend.java`
  — same shape as the display backend, targets 1.12 → 1.19.3.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/BackendSelector.java`
  — picks the right backend at service load: `DisplayEntityBackend` when
  `PlatformInfo#isAtLeast(1, 19, 4)` and the `TextDisplay` class resolves,
  `ArmorStandBackend` otherwise.

### Internal — service + registry + packet stub

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/service/HologramService.java`
  — service entry point. Mirrors `HubService` / `ChatService` lifecycle
  (`load`/`reload`/`save`). Dormant by default — logs a single
  `Module dormant` line via `ConsoleLog` when the master flag is off. When
  enabled, selects a backend, probes the packet layer, and logs the
  selection.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/service/HologramRegistry.java`
  — concurrent `HologramId → Hologram` map plus a reverse index from Bukkit
  entity id to hologram id (used by Phase 4's packet layer to dispatch
  clicks without scanning every hologram).
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/packet/PacketAvailability.java`
  — cached probe stub. Always returns `false` in Phase 0; the actual
  Netty channel injector lands in Phase 4.

### Config

* New resource `src/main/resources/systems/holograms.yml`. Master `enabled`
  flag defaults to `false`. Includes documented tunables for tick interval,
  per-viewer soft cap, default ranges / scale / billboard, interaction
  defaults, and debug logging — mirrors the documentation style of
  `systems/hub.yml`.

### Wiring

* `src/main/java/dev/sergeantfuzzy/sfcore/Main.java` — three additive blocks:
  1. New field `hologramService`.
  2. Constructor + `load()` call placed after the enchant module init,
     before `registerCommands()`.
  3. `save()` in `onDisable` and `reload()` in `reloadPlugin` (the latter
     keyed `holograms.yml` for the per-component timing map).
  4. New `getHologramService()` accessor matching the other module getters.
* `src/main/resources/plugin.yml` — registers `/sfholo` (aliases `sfholograms`,
  `sfh`) under permission `sfcore.holo.use`. The executor is bound in
  Phase 1 / Phase 2 — until then the command resolves to Bukkit's default
  "unknown command" reply, which is harmless.
* `src/main/resources/plugin.yml` — adds the `sfcore.holo.*` permission
  tree (use, admin, create, delete, edit, list, info, tp, interact, gui)
  plus inclusion under the existing `sfcore.*` umbrella so a single grant
  still covers the new module.

### Language

* `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
  — new `hologram` section (English + German comments) plus initial keys:
  `hologram.prefix`, `hologram.module.dormant`, `hologram.module.enabled`,
  `hologram.module.disabled`, `hologram.error.module_disabled`,
  `hologram.error.backend_unavailable`, `hologram.error.invalid_id`,
  `hologram.error.not_found`, `hologram.error.already_exists`,
  `hologram.debug.spawned`, `hologram.debug.removed`. Both English and
  German translations included per `CLAUDE.md`. These will surface in
  `language_en.yml` and `sprache_de.yml` on first server start.

## Verification

* `mvn -DskipTests compile` — green.
* `mvn -DskipTests package` — green up to and including the maven-jar step
  (`target/SF-Core-1.0.0-SNAPSHOT.jar` produced). The exec-maven-plugin
  ProGuard step fails on this local toolchain because Java 25 emits class
  file version 69 (ProGuard 7.5.0 maxes at version 66 / Java 22). This is
  a local environment limitation only; the unobfuscated jar already
  contains all Phase 0 classes and resources:
  * `dev/sergeantfuzzy/sfcore/hologram/{backend,model,service,packet}/*.class`
  * `systems/holograms.yml`
  * Updated `plugin.yml`.
  On a toolchain running Java ≤ 22 (matching the in-project Maven
  distribution referenced by `CLAUDE.md`), the ProGuard pass completes
  normally.
* No new compiler warnings.
* No existing service signatures modified.

## Suggested Commit Message

```
SF-Core Holograms: Phase 0 — bootstrap module skeleton + dormant service
```
