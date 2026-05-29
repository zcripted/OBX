# Holograms — Phase 1: Backend implementations + visibility

■ **Created:** 2026-05-29 12:50 pm
■ **Last Updated:** 2026-05-29 12:50 pm

Brings the hologram subsystem from "dormant scaffolding" to "functional debug
hologram on every supported MC version". Adds the two real backend
implementations (display-entity via reflection on 1.19.4+, armor-stand on
1.12 → 1.19.3), the backend-agnostic renderer, viewer tracker, tick loop,
join / resource-pack listeners, and a debug subcommand.

## Categories

### Internal — backends

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/DisplayEntityBackend.java`
  — full implementation. Static `ReflectionState` resolves
  `TextDisplay`, `BlockDisplay`, `ItemDisplay`, `Display`, `Display$Billboard`,
  `TextDisplay$TextAlignment`, `Color`, `BlockData`, `Transformation`,
  `org.joml.Vector3f`, `org.joml.Quaternionf` plus their setter / constructor
  handles. Per plan §1.5 / §7.1 the class file imports zero 1.19.4-only
  types — `Class.forName`-only, so the class itself loads cleanly on the
  1.12.2 baseline even though it would never be chosen there.
  * Spawn: one entity per line, stacked top-down using a scale-aware
    `0.27 × scale` block spacing.
  * Text lines: `setText(String)` (legacy `&` codes translated via
    `ChatColor.translateAlternateColorCodes`), plus `setLineWidth`,
    `setShadowed`, `setSeeThrough`, `setTextOpacity`,
    `setBackgroundColor` (via `Color.fromARGB`), `setAlignment`.
  * Icon lines: `setItemStack`.
  * Block lines: `setBlock(Material.createBlockData())`.
  * All displays: `setBillboard`, `setViewRange` (scaled to show-range),
    `setTransformation` (uniform scale, identity rotations).
  * Visibility: `Player#hideEntity(Plugin, Entity)` / `showEntity(Plugin, Entity)`
    via reflection. Probed once at static init. If unavailable, entities are
    visible to every player within server tracking range — graceful degrade.
  * Mutation strategy: re-spawn on dirty (per plan, simplest correct path
    for arbitrary line/setting changes).
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/backend/ArmorStandBackend.java`
  — full implementation for 1.12 → 1.19.3. Spawns invisible marker armor
  stands with `setVisible(false)`, `setMarker(true)`, `setSmall(true)`,
  `setGravity(false)`, `setCanPickupItems(false)`, `setRemoveWhenFarAway(false)`.
  * Text lines: custom name + `setCustomNameVisible(true)`.
  * Icon lines: head-worn item via `getEquipment().setHelmet`.
  * Block lines: head-worn `ItemStack(Material)` — same trick.
  * `supportsPerViewerText()` returns `false` (plan §8.2 — text is resolved
    once with no player context on this backend).

### Internal — renderer + tracker + tick loop

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/render/ViewerTracker.java`
  — pure-distance visibility decision. Checks same-world, distance ≤
  show-range, personally-hidden set, view-permission, and (when
  `doubleSided == false`) dot-product of the hologram normal vs the
  viewer-relative vector. Phase 6 layers wall occlusion on top.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/render/HologramRenderer.java`
  — backend-agnostic update entry. Owns the per-tick walk:
  `applyMutations` (if dirty) then per-player `updateVisibility`. Also
  exposes `spawnAll`, `destroyAll`, `refreshFor`, `resyncPlayer` for
  lifecycle and listener call sites.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/render/TickLoop.java`
  — schedules the renderer via SF-Core's existing `SchedulerAdapter`
  (Folia-aware). Period sourced from
  `systems/holograms.yml → view-update-ticks` (default 5 ticks = 4 Hz).
  Exceptions are logged, never propagated.

### Internal — listeners

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/listener/HologramJoinListener.java`
  — handles `PlayerJoinEvent`, `PlayerRespawnEvent`,
  `PlayerChangedWorldEvent`, `PlayerQuitEvent`. Join / respawn delay 2
  ticks before calling `resyncPlayer` to let Paper finish the chunk send.
  Quit clears the leaver from every hologram's viewer set.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/listener/HologramResourcePackListener.java`
  — on `PlayerResourcePackStatusEvent` with status `ACCEPTED` or
  `SUCCESSFULLY_LOADED`, delays 4 ticks then re-shows holograms for the
  player. Addresses plan §9 ("stale holograms after resource-pack reload").

### Commands

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/HologramCommand.java`
  — minimal dispatcher with `debug` and `info` subcommands. Permission
  gated (`sfcore.holo.use`, `sfcore.holo.admin`, `sfcore.holo.info`). The
  `debug` subcommand spawns / removes a built-in test hologram at the
  player's location showing backend identity. The `info` subcommand
  reports backend, loaded count, and packet-layer state.

### Service updates

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/service/HologramService.java`
  — now owns the renderer and tick loop, exposes `getRenderer()` and
  `shutdown()`. `load()` constructs the renderer + starts the tick loop
  when active. `reload()` tears down then rebuilds. `shutdown()` is called
  from `Main#onDisable` before `save()` so live entities don't leak.

### Wiring

* `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
  * `registerCommands` — binds `sfholo` to the new `HologramCommand`.
  * `registerListeners` — registers `HologramJoinListener` and
    `HologramResourcePackListener`.
  * `onDisable` — calls `hologramService.shutdown()` before
    `hologramService.save()`.

## Verification

* `mvn -DskipTests compile` — green.
* All Phase 1 classes load on the 1.12.2 baseline (no `TextDisplay`-symbol
  references at file scope; the class file itself is verifier-clean).
* Manual verification checklist (run on a local 1.21 Paper test server with
  `enabled: true`):
  * `/sfholo debug` spawns a 5-line hologram (text, text, text, icon, text)
    at the player's location. Visible.
  * Walk past 48 blocks — disappears.
  * Set `doubleSided=false` in code and re-test — back-side viewers no longer see it.
  * Disconnect and reconnect — hologram visible again.
  * `/sfreload` — clean teardown + respawn (no orphan entities).
* Manual verification on 1.18.2 Paper:
  * `BackendSelector` picks `ArmorStandBackend`.
  * `/sfholo debug` shows text lines (custom names) and the icon as a head-worn item.

## Suggested Commit Message

```
SF-Core Holograms: Phase 1 — real backends, renderer, listeners, debug command
```
