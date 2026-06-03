# Holograms — Phase 5: Animations

■ **Created:** 2026-05-29 2:50 pm
■ **Last Updated:** 2026-05-29 2:50 pm

Adds the three animation types listed in the plan (fade, rotate, icon bob)
plus the configuration / registry / command surface needed to attach them to
holograms.

## Categories

### Internal — anim package

* `src/main/java/dev/zcripted/obx/hologram/anim/Animation.java`
  — interface. `tick(hologram, backend, ticksSinceStart)` + `name()`.
* `src/main/java/dev/zcripted/obx/hologram/anim/AnimationConfig.java`
  — serializable type + params map. Typed `getDouble` / `getLong` helpers.
* `src/main/java/dev/zcripted/obx/hologram/anim/FadeAnimation.java`
  — linear opacity ramp 0 → 255 over `fade-in-ticks`; optional
  `visible-ticks` + `fade-out-ticks` reverse ramp. Mutates
  `HologramSettings.textOpacity` and marks dirty. No-op on the armor-stand
  backend (no opacity control there).
* `src/main/java/dev/zcripted/obx/hologram/anim/RotateAnimation.java`
  — yaw delta per `period-ticks` ticks, default 4.5° per tick.
  Display-entity backend renders it via setLocation's yaw and the
  transformation rebuild on re-spawn. Armor-stand backend emulates by
  teleport — visible but less smooth.
* `src/main/java/dev/zcripted/obx/hologram/anim/IconBobAnimation.java`
  — sinusoidal Y offset, `amplitude` blocks, `cycle-ticks` per cycle.
* `src/main/java/dev/zcripted/obx/hologram/anim/AnimationRegistry.java`
  — type name → factory map. Stores `fade`, `rotate`, `bob`/`iconbob`.

### Model

* `Hologram` now owns:
  * `List<AnimationConfig> animationConfigs` (persisted).
  * `List<Animation> liveAnimations` (instances built via the registry).
  * `animationStartTick` (filled on first tick, cleared on add/rebuild).
  * `addAnimation` / `removeAnimation` / `rebuildAnimations`.
* `HologramSerializer` writes / reads the `animations:` list:
  ```yaml
  animations:
    - {type: rotate, params: {degrees-per-tick: 4.5, period-ticks: 1}}
    - {type: fade, params: {fade-in-ticks: 20}}
  ```
  Unknown types are silently skipped on load so future additions don't
  break older saves.

### Renderer

* `HologramRenderer.tick` — runs animations before mutations so opacity /
  yaw changes apply in the same tick. Animation start tick is lazy
  (recorded on first tick). Exceptions inside an animation are caught,
  logged, and the renderer continues.

### Commands

* `src/main/java/dev/zcripted/obx/hologram/command/sub/AnimSub.java`
  — `/sfholo anim <id> <add <type> [k=v …]|remove <index>|list>`. Numeric
  values are parsed as doubles; strings are kept as strings.

### Language

* `MessageDefaults.java` — `hologram.anim.added`, `hologram.anim.removed`.
  Both EN + DE.

## Verification

* `mvn -DskipTests compile` — green.
* Manual on Paper 1.21:
  * `/sfholo anim test add rotate degrees-per-tick=6` — hologram rotates
    smoothly (display-entity backend uses live yaw + transformation).
  * `/sfholo anim test add fade fade-in-ticks=40` — hologram fades in
    over 2 seconds after the next re-spawn (move/edit triggers re-spawn).
  * `/sfholo anim test add bob amplitude=0.25 cycle-ticks=30` — visible
    Y bob.
  * `/sfholo anim test list` — shows all three.
  * `/sfholo anim test remove 2` — removes by 1-based index.
* On 1.18.2 (armor-stand backend) — rotation emulated, no errors. Fade
  no-ops. Bob works (location-based).

## Suggested Commit Message

```
OBX Holograms: Phase 5 — animations (fade, rotate, icon bob)
```
