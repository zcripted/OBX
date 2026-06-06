# World Controls GUI Improvements

■ **Created:** 2026-06-03 8:42 am (America/Detroit)

■ **Last Updated:** 2026-06-03 8:42 am (America/Detroit)

Enriched the World Controls admin GUI (and its Weather / Game Rule sub-menus) with live
data, clearer styled feedback, and a reorganized gamerule editor. `./gradlew build` is green;
both jars produced; EN/DE/ES message parity test passes (6/6).

## GUIs

- **World Controls menu** (`AdminSubMenu.openWorldControlsMenu`): the **World Border**,
  **Weather Control**, and **Game Rule Editor** items now show live, clean-styled lore:
  - World Border: world, diameter, center, warning (blocks · seconds), damage (buffer · /block).
  - Weather Control: current weather + the available options (Clear / Rain / Thunder).
  - Game Rule Editor: a preview of the first **10 alphabetical enabled** gamerules + the
    total enabled count.
- **Auto Save Toggle**: the menu now re-renders after a toggle so the item's status lore
  reflects the new state immediately.
- **Weather Control sub-menu**: tool/weapon icons (e.g. the Trident "Thunder" icon) no
  longer show vanilla attribute lines — `HIDE_ATTRIBUTES` is now applied to every menu icon
  in `AdminMenuRender.createMenuItem` (and to gamerule items).
- **Game Rule Editor sub-menu** — reorganized:
  - Rules are sorted **A–Z** by display name.
  - **Unsupported/unavailable** gamerules render as **firework stars**, alphabetically, on a
    **separate row** from the supported ones (layout computed against the running version).
  - The menu is now 54 slots with a **dedicated bottom nav row**: Back (slot 45) and Close
    (slot 53) are no longer mixed in with the rule items. `MainMenuListener` routes the new
    nav slots; the dynamic layout is rebuilt per open via `GameruleEntry.rebuildLayout`.

## Messages / Feedback (box-style, EN/DE/ES)

- **Redstone toggle** (`ServerControlActions.redstoneMessage`): box message now states the
  new mode with a red **FROZEN** / green **RESUMED** keyword and a context line on what's
  affected (redstone signals, clocks, pistons, observers & comparators), mirrored to console.
- **Save Worlds** (`ServerControlActions.saveWorldsMessage`): box message detailing **what**
  was flushed (chunks, entities, player data, level.dat) and a per-world row with **hover
  tooltips** showing each world's folder path (click-to-copy). The console mirror prints the
  full per-world detail (name → folder path).
- **Auto Save** (`ServerControlActions.toggleAutoSave` / `setAutoSave`): box message with a
  green **ENABLED** / red **DISABLED** keyword, the **[Toggle]** button on its own indented
  row, and a button-less console mirror. The button runs the existing click-bridge
  (`/obx x-action autosave toggle`), so it works from chat too.

## Files

- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`
  — item lore helpers, Save/Auto-Save handlers, redstone handler, gamerule menu + item +
  `GameruleEntry.rebuildLayout`, nav-slot constants.
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenuRender.java`
  — `hideAttributes` helper applied in `createMenuItem`.
- `features/world/src/main/java/dev/zcripted/obx/feature/world/service/ServerControlActions.java`
  — `redstoneMessage`, `toggleAutoSave`/`setAutoSave`/`autoSaveMessage`, `saveWorldsMessage`.
- `plugin/src/main/java/dev/zcripted/obx/core/command/ObxAdminActions.java`
  — `autosave` click-bridge action.
- `plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java`
  — dedicated GAMERULES case for the new nav slots.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` / `DE.java` / `ES.java`
  — 8 new `admin.redstone.box.*` / `admin.world.autosave.box.*` / `admin.world.save.box.*` keys.
- `core/src/test/java/dev/zcripted/obx/core/language/MessageDefaultsTest.java` — unchanged
  (the existing parity test validates the 8 new keys across EN/DE/ES).

## Suggested Commit Message

```
Feature (world-controls GUI): live border/weather/gamerule lore, styled redstone/save/autosave box messages, alphabetized gamerule editor with firework-star unsupported rules + dedicated nav row
```
