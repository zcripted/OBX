# InvSee — live-mirror GUI with custom title and hotbar separator

■ **Created:** 2026-05-09 6:00 pm

## Summary

`/invsee` previously called `viewer.openInventory(target.getInventory())`,
which opened the target's actual `PlayerInventory`. That approach has
two ergonomic problems:

1. The inventory title was locked to the vanilla `"Inventory"` string —
   no way to indicate **whose** inventory the operator was looking at.
2. The 4×9 layout dropped the operator straight into the target's main
   storage with the hotbar at the bottom of *their own* viewport — no
   visual divider between the two areas.

This change replaces the raw open with a **live-mirror chest GUI**:
custom title `"{player}'s Inventory"`, explicit separator row between
the main inventory area and the hotbar, dedicated armor + offhand
slots on the bottom row, and a 4 Hz refresh task that copies the
target's slot state into the mirror as soon as the target adds /
removes / changes items so the operator sees changes within
~250 ms.

## Categories

### GUIs

- **New GUI**: 54-slot live-mirror chest opened by `/invsee <player>`.
  Layout:
  ```
  Row 0 (slots  0..8 ) ← target slots  9..17  (main inventory, top)
  Row 1 (slots  9..17) ← target slots 18..26  (main inventory, mid)
  Row 2 (slots 18..26) ← target slots 27..35  (main inventory, bottom)
  Row 3 (slots 27..35) — SEPARATOR (filler glass; unmapped)
  Row 4 (slots 36..44) ← target slots  0..8   (hotbar)
  Row 5 (slots 45..53):
          45 ← target 39 (helmet)
          46 ← target 38 (chestplate)
          47 ← target 37 (leggings)
          48 ← target 36 (boots)
          49 ← target 40 (offhand, 1.9+ only — silently absent on 1.8)
          50..52  filler glass
          53      red-X close head (reuses StaffMenu.buildCloseHead)
  ```
- **Custom title**: `"{player}'s Inventory"` (EN) / `"Inventar von {player}"`
  (DE), trimmed defensively to 32 chars to satisfy the legacy pre-1.13
  inventory-title cap.
- **Live updates** at ~4 Hz (5-tick refresh period). The per-slot diff
  in `InvSeeMenu.refreshFromTarget` only writes to slots whose
  contents actually changed, so the chest doesn't re-paint every
  frame and the operator's cursor doesn't snap.
- **View-only**: every click and drag on the mirror is cancelled — the
  operator sees changes the target makes but cannot pull or shove
  items through the GUI. Editing through a live-refreshed mirror
  would race the refresher; intentionally out of scope for this
  drop.
- **Auto-close on target offline**: if the target logs out while
  someone is viewing them, the refresher closes the viewer's GUI on
  its next tick instead of leaving a frozen snapshot.

### Internal

- New classes:
  - `gui/admin/InvSeeMenu.java` — static `open(plugin, viewer, target)`
    + `refreshFromTarget(...)` that does the per-slot diff. Carries
    the slot-map builder and a `safeGet` helper that wraps
    `PlayerInventory#getItem` in try/Throwable so the offhand slot
    on 1.8 (which doesn't exist) silently returns null instead of
    throwing.
  - `gui/admin/InvSeeMenuHolder.java` — `InventoryHolder` with target
    UUID, target name, slot map, and close-button slot; constants
    `UNMAPPED = -1` so unmapped slots have a single named sentinel.
  - `gui/admin/InvSeeMenuManager.java` — owns the
    `Map<UUID, InvSeeMenuHolder>` of open mirrors and the
    `SchedulerAdapter.runRepeating` task that drives the refresh. On
    plugin disable, force-closes any still-open mirrors so they
    don't outlive the plugin instance.
  - `listener/menu/InvSeeMenuListener.java` — cancels every click /
    drag on `InvSeeMenuHolder` inventories, routes the close-button
    click to `closeInventory()`, and on `InventoryCloseEvent` drops
    the viewer from `InvSeeMenuManager` so the refresher stops
    touching the discarded chest.
- `Main.java`:
  - New field `invSeeMenuManager` + `getInvSeeMenuManager()` accessor.
  - Instantiates the manager alongside the existing menu managers.
  - Registers `InvSeeMenuListener` next to `StaffMenuListener`.
  - `onEnable` calls `invSeeMenuManager.start()` (after scheduler is
    available); `onDisable` calls `invSeeMenuManager.stop()` (before
    other shutdown so any open mirrors close cleanly).
- `command/admin/InvSeeCommand.java` — replaced
  `viewer.openInventory(target.getInventory())` with
  `InvSeeMenu.open(plugin, viewer, target)`. All permission checks,
  privileged-target gating, self-view rules, console-log emission,
  and tab-completion are unchanged.

### Language

New EN + DE key in `MessageDefaults`:

- `player.invsee.menu.title` — chest title with `{player}` placeholder.
  - EN: `&6&l{player}&6's Inventory`
  - DE: `&6&lInventar von {player}`

`InvSeeMenu.open` trims the rendered title to 32 chars defensively for
legacy 1.12 / 1.8 servers — modern (1.13+) servers handle longer
titles fine, but the trim is cheap insurance.

## Files modified

- `src/main/java/dev/zcripted/obx/Main.java`
- `src/main/java/dev/zcripted/obx/command/admin/InvSeeCommand.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Files added

- `src/main/java/dev/zcripted/obx/gui/admin/InvSeeMenu.java`
- `src/main/java/dev/zcripted/obx/gui/admin/InvSeeMenuHolder.java`
- `src/main/java/dev/zcripted/obx/gui/admin/InvSeeMenuManager.java`
- `src/main/java/dev/zcripted/obx/listener/menu/InvSeeMenuListener.java`
- `docs/changes/2026-05-09---invsee-live-mirror-gui.md`

## Verification

- `& ".\maven\bin\mvn.cmd" -DskipTests package` produced a fresh
  obfuscated `target/OBX-1.0.0-SNAPSHOT.jar` with no `[ERROR]`
  or `BUILD FAILURE` lines. Only ProGuard `Note:` lines for
  reflective accesses (informational per CLAUDE.md).
- ProGuard's existing `* implements org.bukkit.inventory.InventoryHolder`
  + `* implements org.bukkit.event.Listener` keep-rules cover
  `InvSeeMenuHolder` and `InvSeeMenuListener` automatically; no
  new `-keep` directives were needed.
- The slot map is a `static final` 54-element int array built once;
  refresh dispatch is O(54) per open viewer per tick, capped by the
  per-slot diff so the work-per-tick is negligible.

## Notes / non-changes

- `InvSeeCommand` permission tiers (`obx.invsee.basic`,
  `obx.invsee.full`) and `isPrivilegedTarget` rules are
  unchanged — the GUI replacement is strictly a UX change.
- No `docs/information/about.md` update — the `/invsee` command,
  permissions, and aliases are unchanged.
- The 4 Hz refresh cadence is a deliberate choice: faster (1-tick)
  refreshes would feel jittery during the target's drag operations
  because each intermediate cursor state would render briefly in the
  mirror. 5 ticks lands on the natural client interpolation window
  while still feeling instant.

## Suggested Commit Message

```
Feature (invsee): replace raw inventory open with a 54-slot live-mirror chest GUI carrying a custom "{player}'s Inventory" title, an explicit separator row between the hotbar and main inventory, and a 4 Hz refresh task that mirrors target inventory edits in real time
```
