# Multi-platform / multi-version runtime adapter

■ **Created:** 2026-05-07 5:45 pm

■ **Last Updated:** 2026-05-07 5:45 pm

## Summary

OBX now ships as a **single JAR** that runtime-detects the host server and
adapts its API usage accordingly. Supported targets:

- Paper 1.8.8 → 26.1.x
- Spigot 1.8.8 → 26.1.x
- PurPur 1.14 → 26.1.x
- Folia 1.8.8 → 26.1.x
- Vanilla CraftBukkit (best effort)

No multi-module Maven build, no NMS shading, no per-version JARs. The plugin is
still compiled against the Spigot 1.12.2 baseline API; everything that's newer
or fork-specific is reached through reflection-only probes that degrade
gracefully when the class isn't present.

## Categories

### Internal

- New `dev.zcripted.obx.platform.PlatformInfo` — singleton that detects
  the server fork (Folia → PurPur → Paper → Spigot → CraftBukkit, in that
  priority), parses `Bukkit.getBukkitVersion()` into major/minor/patch, and
  flags the presence of the Folia scheduler, the Adventure API, and the new
  Paper plugin loader API.
- New `dev.zcripted.obx.platform.scheduler.SchedulerAdapter` — single
  scheduler entry point used by every OBX component:
    - `runNow`, `runLater`, `runRepeating` route to `BukkitScheduler` on
      Bukkit / Spigot / Paper / PurPur; on Folia they route to
      `GlobalRegionScheduler` via reflection.
    - `runAsync`, `runAsyncLater` route to `BukkitScheduler` async on
      Bukkit-style platforms; on Folia they route to `AsyncScheduler` via
      reflection (delays are converted from ticks to milliseconds).
    - `runAtLocation` and `runAtEntity` use `RegionScheduler` /
      `EntityScheduler` on Folia for thread-safe location-bound and
      entity-bound work; they fall through to `runNow` on non-Folia.
    - All scheduler handles are wrapped in a `CancellableTask` interface so
      neither Bukkit `BukkitTask` nor Folia `ScheduledTask` leaks into call
      sites.
- All previous `BukkitRunnable` / `Bukkit.getScheduler()` call sites have been
  refactored to route through the new adapter.

### Refactored call sites

- `TpsService` — TPS sampling task now Folia-safe.
- `TeleportManager` — teleport warmup no longer uses `BukkitRunnable`; the
  warmup wait runs globally and the actual teleport hops to the player's
  region/entity scheduler so it stays thread-safe under Folia.
- `DaylightCycleFallback` — frozen-time enforcement task runs through the
  adapter.
- `TablistRefreshTask` and `TablistJoinListener` — periodic and join refreshes
  use the adapter so Folia regions don't reject the work.
- `ResourcePackListener` — delayed pack push routes through the adapter.
- `ModerationService` — Discord webhook send uses the adapter's async path so
  Folia's `AsyncScheduler` services it.
- `AdminSubMenu` (restart countdown) — refactored to use a `CancellableTask`
  reference instead of `BukkitRunnable.cancel()` self-reference.
- `WarpMenuInputManager` — chat-input hop back to the main thread now runs on
  the player's entity scheduler under Folia.

### Config / plugin metadata

- `plugin.yml`:
    - `api-version: 1.13` removed so OBX loads on 1.8 → 1.12 servers (which
      reject the field). Modern Paper still loads plugins without
      `api-version`; it just emits an informational warning.
    - `folia-supported: true` added so Folia recognizes the plugin and does not
      refuse to load it.
    - `load: POSTWORLD` made explicit (matches existing behavior).
- `pom.xml` description updated to reflect the multi-platform target.

### API

- `Main#getSchedulerAdapter()` exposes the new scheduler so future modules can
  schedule work without ever importing `BukkitScheduler` or `BukkitRunnable`.
- `Main#getPlatformInfo()` exposes the cached platform detection result so
  diagnostics, the `/obx` info subcommands, and feature gates can branch on
  fork / version without re-detecting.

### How the single-JAR adaptation works

1. **Materials.** Existing `resolveMaterial(...)` helpers (e.g. in
   `HelpGuiMenu`, `MainMenu`, `WarpMenu`) already pass an ordered list of
   candidate names — modern names first, legacy fallbacks after. On a 1.21
   server `GRAY_STAINED_GLASS_PANE` resolves; on 1.12 `STAINED_GLASS_PANE`
   resolves; on 1.8 `THIN_GLASS` resolves. The same JAR works on every
   version.
2. **Adventure / MiniMessage.** `AdventureMessageUtil` already detects
   `net.kyori.adventure.text.minimessage.MiniMessage` via `Class.forName`
   and falls back to BungeeCord chat components when it's absent. The new
   `PlatformInfo#hasAdventureApi()` lets callers pre-flight that branch.
3. **Scheduler.** Folia has different threading semantics — calling
   `Bukkit.getScheduler().runTask` on Folia throws
   `UnsupportedOperationException`. The new `SchedulerAdapter` checks
   `PlatformInfo#hasFoliaScheduler()` once on construction and routes every
   call to the correct scheduler family from then on.
4. **Plugin metadata.** `folia-supported: true` is the only flag Folia checks
   to allow a plugin to load. Removing `api-version` lets pre-1.13 servers
   parse the file without error.

## Modified / Added Files

- `src/main/java/dev/zcripted/obx/platform/PlatformInfo.java` *(new)*
- `src/main/java/dev/zcripted/obx/platform/scheduler/SchedulerAdapter.java` *(new)*
- `src/main/java/dev/zcripted/obx/Main.java`
- `src/main/java/dev/zcripted/obx/util/perf/TpsService.java`
- `src/main/java/dev/zcripted/obx/util/teleport/TeleportManager.java`
- `src/main/java/dev/zcripted/obx/util/control/DaylightCycleFallback.java`
- `src/main/java/dev/zcripted/obx/tablist/scheduler/TablistRefreshTask.java`
- `src/main/java/dev/zcripted/obx/tablist/listener/TablistJoinListener.java`
- `src/main/java/dev/zcripted/obx/platform/bukkit/resourcepack/ResourcePackListener.java`
- `src/main/java/dev/zcripted/obx/moderation/ModerationService.java`
- `src/main/java/dev/zcripted/obx/listener/player/JoinLeaveListener.java`
- `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`
- `src/main/java/dev/zcripted/obx/gui/player/WarpMenuInputManager.java`
- `src/main/resources/plugin.yml`
- `pom.xml`

## Suggested Commit Message

```
Feature (platform): single-JAR runtime adapter for Paper / Spigot / PurPur / Folia 1.8.8 → 26.1.x
```
