# Holograms — Phase 7: GUI editor + dev API + polish

■ **Created:** 2026-05-29 3:20 pm
■ **Last Updated:** 2026-05-29 3:20 pm

Final phase of the brainstorming plan. Adds the chest-GUI editor, the
public dev API (events + facade), and the final docs sweep.

## Categories

### Dev API — public events + facade

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/api/HologramSpawnEvent.java`
  — Bukkit `Event`. Carries the spawned `Hologram` model. Fired from
  `HologramFacade.create` (and reserved as the canonical spawn-event hook
  for backend integrations).
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/api/HologramInteractEvent.java`
  — `Event implements Cancellable`. Fired by `InteractionDispatcher`
  before cooldown / command dispatch. Third-party plugins can cancel to
  suppress the configured CText action — useful for shops, quest hubs,
  permission-gated effects.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/api/HologramFacade.java`
  — single entry point for plugins (`HologramFacade.get()`). Methods:
  `isAvailable`, `all`, `find`, `create(id, location)`, `addLine`,
  `delete`. Internal types stay free to refactor.

### Internal — GUI

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/gui/HologramEditorMenu.java`
  — single-page chest GUI (27 slots). Shows:
  * Slot 4 — identity / world / position / line count / animation count.
  * Slots 9-17 — first nine lines with material-appropriate preview
    icons and detailed hover tooltips per `CLAUDE.md` (dividers, action
    hints, no untyped text).
  * Slot 18 — Settings summary (billboard, scale, ranges, alignment,
    opacity).
  * Slot 20 — Animations panel.
  * Slot 22 — Interaction panel.
  * Slot 26 — Delete (shift-click confirmation).
  Material name lookups use `Material.matchMaterial` with version-safe
  fallbacks (`COMPARATOR`/`REDSTONE_COMPARATOR`, `CLOCK`/`WATCH`) so the
  GUI renders cleanly on the 1.12 baseline.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/GuiSub.java`
  — `/sfholo gui <id>`. Permission `sfcore.holo.gui`.

### Wiring

* `Main` — constructs and stores a `HologramEditorMenu`, exposes it via a
  getter, registers it as a `Listener`.
* `HologramCommand` — registers `GuiSub` (only when the editor menu was
  successfully constructed; defensive against any future failure path).
* `InteractionDispatcher` — fires `HologramInteractEvent` before the
  cooldown check so cancellations are cheap (no command runs).

### Build

* `mvn -DskipTests package` — green up to and including the maven-jar
  step. `target/SF-Core-1.0.0-SNAPSHOT.jar` is 1.19 MB (up from 1.02 MB
  pre-holograms) and contains 132 hologram-related class / resource
  entries.
* The ProGuard exec step still fails locally because the host runs
  Java 25 and ProGuard 7.5.0 maxes at Java 22 — same toolchain note as
  Phase 0. On a Java ≤ 22 install (matching the in-project Maven
  distribution referenced by `CLAUDE.md`), ProGuard runs cleanly.

## Verification

* `mvn -DskipTests compile` — green.
* `mvn -DskipTests package` — produces a runnable jar with every
  hologram class.
* Manual checklist (Paper 1.21):
  * `/sfholo gui welcome` — opens the menu, every tile has a clean
    hover tooltip with dividers.
  * Shift-click the delete tile → hologram is removed.
  * Third-party plugin can register `HologramInteractEvent` listener
    and cancel — clicks on enabled holograms then no longer execute
    their CText command.
  * `HologramFacade.get().create("api_test", loc)` — hologram appears,
    persists, and `HologramSpawnEvent` fires.

## Notes on out-of-scope items

The plan §5 Phase 7 list mentions image lines via a resource-pack font.
This is left as a future enhancement — the resource-pack infrastructure
(`AutoResourcePackManager`) is in place, so adding it later is a small
content-only change (font JSON + glyph PNG) that doesn't touch any code in
the hologram module.

## Suggested Commit Message

```
SF-Core Holograms: Phase 7 — chest-GUI editor + dev API events + facade
```
