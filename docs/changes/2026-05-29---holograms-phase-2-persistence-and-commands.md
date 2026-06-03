# Holograms — Phase 2: Persistence + admin commands

■ **Created:** 2026-05-29 1:30 pm
■ **Last Updated:** 2026-05-29 1:30 pm

Adds the full administrative command tree and disk persistence. Server
admins can now create, edit, list, teleport-to, and delete holograms via
`/sfholo …` with no code, and all state survives restart/reload.

## Categories

### Internal — storage

* `src/main/java/dev/zcripted/obx/hologram/storage/HologramStorage.java`
  — interface. Methods: `loadAll`, `save(Hologram)`, `saveAll(Collection)`,
  `delete(HologramId)`.
* `src/main/java/dev/zcripted/obx/hologram/storage/HologramSerializer.java`
  — static read/write helpers between `Hologram` and `ConfigurationSection`.
  Uses the existing `LocationSerializer`. Forward-compatible — unknown keys
  on read are silently ignored so later phases can add fields without
  migration.
* `src/main/java/dev/zcripted/obx/hologram/storage/YamlHologramStorage.java`
  — single-file (`holograms.yml`) implementation. Atomic write via temp +
  rename. `synchronized` lock keeps concurrent saves safe.
* `HologramService` now constructs the storage in its constructor, calls
  `storage.loadAll()` in `load()` after the renderer / tick loop are wired,
  registers every loaded hologram, and runs `renderer.spawnAll()` so persisted
  holograms render immediately when the module activates.

### Commands — root + 28 subcommands

* New `HoloSubCommand` interface and `HoloContext` helper (shared message /
  resolver / persist+refresh logic).
* New `HologramCommand` registers the full subcommand registry. Routes
  `args[0]` and delegates tab completion. Falls back to a chat help banner.
* New package `hologram/command/sub/` with one class per subcommand
  (~28 files, matching the plan §5):
  * Lifecycle: `CreateSub`, `DeleteSub`, `CopySub`, `MoveSub`,
    `MoveHereSub`, `TpSub`, `ListSub`, `InfoSub`.
  * Lines: `AddLineSub`, `SetLineSub`, `RemoveLineSub`, `InsertBeforeSub`,
    `InsertAfterSub`, `SwapLineSub` (icon / block lines parsed inline).
  * Settings: `ScaleSub`, `BillboardSub`, `BackgroundSub`, `TextAlphaSub`,
    `ShadowSub`, `AlignmentSub`, `SeethroughSub`, `DoubleSidedSub`,
    `ShowRangeSub`, `UpdateRangeSub`.
  * Utility: `BoardSub` (registered now, board renderer arrives in
    Phase 5 — subcommand prints a clear "wired in Phase 5" notice),
    `AimGuiSub` (look-at targeting using a 12-block view-ray projection).
  * Admin: `EnableSub`, `DisableSub`, `ReloadSub`.
* `Phase 1` `debug` subcommand kept on the root command class as a quick
  smoke-test entry point.

### Permissions

* `plugin.yml` already shipped the `obx.holo.*` tree in Phase 0 — all
  29 subcommands map onto one of `obx.holo.{use,create,delete,edit,
  list,info,tp,interact,gui,admin}`.

### Language

* `MessageDefaults.java` — 17 new keys covering every Phase 2 feedback path:
  `hologram.create.success`, `hologram.delete.success`,
  `hologram.copy.success`, `hologram.tp.success`, `hologram.move.success`,
  `hologram.line.added`, `hologram.line.updated`, `hologram.line.removed`,
  `hologram.line.inserted`, `hologram.line.swapped`,
  `hologram.setting.updated`, `hologram.error.invalid_index`,
  `hologram.error.invalid_material`, `hologram.error.invalid_number`,
  `hologram.error.invalid_value`, `hologram.error.world_unknown`,
  `hologram.error.no_aim_target`. Both English and German translations.

### Docs

* `docs/information/COMMANDS+PERMISSIONS.md` — adds the **Holograms —
  admin** section with all 28 subcommands documented (Command · Aliases ·
  Usage · Example · Description · Default · Permission node), plus the
  `obx.holo.*` wildcard entry in the **Wildcards & Bundles** block.

### Bug fix encountered during build

* `InfoSub` originally used Java 14+ switch expressions; rewritten as
  classic switch statements to keep the 1.8 source target green.

## Verification

* `mvn -DskipTests compile` — green.
* Manual checklist (local 1.21 Paper server with `enabled: true`):
  * `/sfholo create welcome` → `holograms.yml` is created and the
    hologram appears at the player's location.
  * `/sfholo addline welcome &eHello %player%` → line is appended,
    visible, persisted.
  * `/stop` + restart → hologram reappears at the same spot.
  * `/sfholo list` shows it. `/sfholo info welcome` prints settings + lines.
  * `/sfholo scale welcome 1.5` → backend re-spawns with new scale.
  * `/sfholo billboard welcome FIXED`, `/sfholo doublesided welcome false`,
    `/sfholo seethrough welcome true` — settings persist + render correctly.
  * `/sfholo movehere welcome` → hologram moves to caller's eye level.
  * `/sfholo copy welcome welcome2` → duplicate with the same content.
  * `/sfholo delete welcome2` → removed from disk + registry, no orphans.
  * Tab-completion works for every subcommand and for hologram ids.
  * Players without `obx.holo.admin` get a clean "no permission" reply
    on `/sfholo enable|disable|reload|debug`.

## Suggested Commit Message

```
OBX Holograms: Phase 2 — persistence + admin commands (29 subcommands)
```
