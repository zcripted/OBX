# Death-item grouping feature, /ptime box+buttons, diagnostics hover fixes

■ **Created:** 2026-06-04 3:50 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 3:50 pm (America/Detroit)

A new opt-in **death-item grouping** feature module, box-style + buttons for `/ptime`, and three
`/obx diagnostics` hover-formatting fixes. Build green; both jars; EN/DE/ES parity passes.

---

## Features

- **Death-item grouping (new `:features:deathdrop` module).** When enabled, a dying player's entire
  set of dropped items (hotbar + inventory + armor) is combined into a **single carry-all `Item`
  entity** instead of scattering. The entity is a **chest on 1.13–1.16.5** and a **bundle on 1.17+**,
  always falling back to `CHEST`, and floats with a holographic **`×<count>`** name. Walking over it
  restores the stored items to the player's inventory; **anything that doesn't fit stays inside** the
  entity and the hologram count updates. Opt-in (default **off**).
  - Toggleable from **four surfaces**, all going through `ModuleManager.setEnabled("deathdrop", …)`
    (which persists to `config.yml`): the **Module Toggles GUI** sub-menu (new `DEATHDROP` entry,
    chest icon, slot 17), **`/obx deathdrop <on|off|status>`**, **console**, and the
    **`modules.deathdrop`** flag in `config.yml`.
  - **Box-style messages** (in-game + console). The in-game toggle confirmation adds a `[Toggle]`
    **button row** (click → `/obx deathdrop <opposite>`); console gets the box only (no buttons).
  - Files: `features/deathdrop/build.gradle.kts`, `features/deathdrop/.../DeathDropModule.java`,
    `features/deathdrop/.../listener/DeathDropListener.java`, `settings.gradle.kts`,
    `plugin/build.gradle.kts`, `plugin/.../OBX.java` (`registerModules`),
    `plugin/.../core/command/ObxCommand.java`, `core/.../command/ObxModulesView.java`
    (`handleDeathDrop` + box/button render), `features/staff/.../gui/AdminSubMenu.java`
    (`ModuleEntry.DEATHDROP`), `config.yml` (`modules.deathdrop`), `plugin.yml` (perm +
    `obx.admin` child), new `admin.modules.deathdrop.*` / `admin.gui.module.deathdrop` /
    `deathdrop.hologram` keys in all three `MessageDefaults`.
  - **Documented assumptions:** contents tracking is **in memory** (keyed by entity UUID) for
    cross-version simplicity (no PDC on 1.12–1.13), so a carry-all that outlives a server restart
    degrades to an inert/empty container. On the item despawn timer the tracking is dropped so
    contents expire exactly as vanilla death drops would; on module disable the tracking is cleared.

## Commands

- **`/ptime` now box-style + buttons.** `/ptime <time|reset>` renders a framed box (with the new
  client-side time, or a reset notice) plus an action row of **`[Morning] [Noon] [Night] [Midnight]
  [reset]`** buttons that run `/ptime <mode>` — mirroring the `/time` box, but targeting the player's
  own time. Player-only surface. Files: `features/world/.../service/ServerControlActions.java`
  (`pTimeMessage` / `pTimeButton` / `pTimeResetButton`), `features/world/.../command/PTimeCommand.java`,
  new `admin.ptime.*` / `admin.button.ptime*` keys.

## Fixes — /obx diagnostics

- **Modules & Errors hover raw color codes.** The `X/X enabled` modules hover and the `full` Errors
  hover were built as raw `&`-coded strings, but the hover transport
  (`TextComponent.fromLegacyText`) only understands `§` codes — so the tooltips showed literal `&a`,
  `&l`, … tokens. Fixed by translating every hover line to section codes (`colorizeLines`) in
  `sendHoverValueLine`. File: `core/.../command/ObxDiagnosticsView.java`.
- **"View Full Diagnostics" hover — command is now purple.** The `/obx diagnostics full` command in
  the view-full button's tooltip is now colored purple (`&5`). Files: `commands.obx.diagnostics.full-button-hover`
  in all three `MessageDefaults`.

## Categories Touched
Features (new module), Commands, GUIs, Permissions, Config, Messages (EN/DE/ES), Internal.

## Testing
- `./gradlew :core:test` — EN/DE/ES parity (incl. all new keys) **passes**.
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`. (ProGuard reflective `Note:` lines are
  informational.)

## Suggested Commit Message
```
Feature (deathdrop): group death drops into one carry-all entity + hologram; toggle 4 ways

New opt-in :features:deathdrop module — combines a dead player's drops into one chest/bundle
Item entity with a ×count hologram, restores on pickup (overflow stays inside). Toggle via
Module Toggles GUI, /obx deathdrop, console, or modules.deathdrop in config.yml; box-style
messages (in-game [Toggle] button row, console plain). Also: /ptime box+buttons; fix /obx
diagnostics modules+errors hover raw color codes; make view-full hover command purple.
```
