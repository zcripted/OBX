# Holograms — Phase 6: Visibility refinements + advanced display

■ **Created:** 2026-05-29 3:00 pm
■ **Last Updated:** 2026-05-29 3:00 pm

Adds the visibility refinements called out in the plan §H and the per-player
toggle surface needed for players to opt out of clutter.

## Categories

### Internal — visibility

* `src/main/java/dev/zcripted/obx/hologram/render/WallOcclusionCheck.java`
  — bounded line-of-sight raycast with a TTL-keyed cache (default 5
  ticks). Steps the ray at 0.5-block resolution, returns false on the
  first occluding block. Falls through to `isSolid()` on older API
  levels that don't expose `isOccluding`.
* `ViewerTracker` — now layers wall occlusion on top of distance + double-
  sided + permission. `hide-behind-walls` defaulted off, opt in per
  hologram.
* Permission-gated visibility was already plumbed through
  `HologramSettings.viewPermission` in Phase 1; Phase 6 just adds the
  command surface (`/sfholo view <id> permission <node>`).

### Commands

* `src/main/java/dev/zcripted/obx/hologram/command/sub/ViewSub.java`
  — `/sfholo view <id> <permission <node>|hide-behind-walls <true|false>|reset>`
* `src/main/java/dev/zcripted/obx/hologram/command/sub/HideSub.java`
  — `/sfholo hide <id>` (per-player hide via `personallyHidden` set).
* `src/main/java/dev/zcripted/obx/hologram/command/sub/ShowSub.java`
  — `/sfholo show <id>` reveals a previously hidden hologram.
* All three registered in `HologramCommand`.

### Language

* `MessageDefaults.java` — `hologram.visibility.hidden`,
  `hologram.visibility.shown`. Both EN + DE.

## Verification

* `mvn -DskipTests compile` — green.
* Manual:
  * `/sfholo view test permission obx.test.view` — players without
    the perm no longer see the hologram on next tick.
  * `/sfholo view test hide-behind-walls true` — walking behind a wall
    triggers a hide; stepping out reveals it within 5 ticks (cache TTL).
  * `/sfholo hide test` — caller no longer sees it. `/sfholo show test`
    reveals it again immediately.

## Notes on icon variants

The plan's plus-points "icon variants (custom model data, player heads,
in-hand item)" are deferred to Phase 7 alongside the GUI editor (they
share the same UI surface — Phase 7 wires both at once).

## Suggested Commit Message

```
OBX Holograms: Phase 6 — hide-behind-walls + per-player toggles
```
