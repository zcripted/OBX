# Plugin-Wide Styled Info Messages + Server-Selector Close Button

■ **Created:** 2026-05-25 6:46 pm
■ **Last Updated:** 2026-05-25 6:46 pm

## Summary
Brought the OBX "plugin info" command outputs onto the single shared report
style already used by `/pl`, `/obx info`, and `/obx about` — the
`▍ 𝗢𝗕𝗫  ›  <Title>` header bar, a slim 30× `─` (U+2500) divider, and indented
`  &7<Label>  &8›  &f<value>` / `  &6<usage>  &8›  &7<desc>` rows. Also added a
close button to the hub server-selector GUI.

**Scope decision (documented assumption):** "plugin-wide / all similar messages"
was applied to the *informational / reference* command family (help, version,
commands, permissions — joining about/info/pl/diagnostics which already use it).
Operational confirmations (reload, debug, updates, config status) intentionally
keep their one-line `{prefix}…` status style — they are action feedback, a
different message family, and already carry the OBX brand prefix.

## Categories

### Messages / Styling
- **`/obx version`** — was a single `{prefix}&6OBX …` line; now a boxed report
  (`▍ 𝗢𝗕𝗫 › Version`, divider, aligned `Version`/`Build` rows).
- **`/obx help` pages** — header is now the title bar (`… › Help · Page x/y`, no
  prefix); command rows are indented `  &6/cmd  &8›  &7description`; the page is
  wrapped in the 30× `─` box divider; prev/next nav is indented and spaced inside
  the box.
- **`/obx help <command>` detail** — boxed: `… › <usage>` title, divider, aligned
  `Description`/`Category`/`Permission` rows.
- **`/obx commands`** — boxed `… › Commands` header + indented
  `  &6<usage>  &8›  &7<description>` rows (was gold/`-`/yellow manual format).
- **`/obx permissions`** — boxed `… › Permissions` header + indented
  `  &6<usage>  &8›  &f<permission>` rows (was gold/`|`/yellow manual format).
- New shared key **`core.divider-line`** = `&8` + 30× `─` (U+2500), the divider
  used by all the styled reports.
- Files:
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
    (restyled `commands.obx.version`, `commands.obx.help.header`,
    `…help.header-category`, `…help.detail.*`, `…help.line-suffix`; new
    `core.divider-line`, `commands.obx.commands.title|entry`,
    `commands.obx.permissions.title|entry`; EN + DE).
  - `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
    (new `boxDivider()`; restructured `sendHelp`, `sendHelpLine`,
    `sendHelpNavigation`, `sendHelpDetail`, `handleVersion`, `handlePermissions`,
    `handleCommandsList`; removed now-unused `divider()`).

### GUIs
- **Hub server-selector** — added a close button (default `BARRIER`, bottom-center
  slot, auto-placed so it never clobbers a server icon). Clicking it closes the
  menu. Name/lore are language-driven (`hub.selector.close.name` /
  `hub.selector.close.lore`); appearance toggles (enabled/slot/material) live in
  `systems/hub.yml` → `selector.close-button`.
- Files:
  - `src/main/java/dev/zcripted/obx/gui/player/ServerSelectorMenu.java`
    (renders/places the close button via new `buildCloseButton`).
  - `src/main/java/dev/zcripted/obx/gui/player/ServerSelectorHolder.java`
    (tracks `closeSlot` via `setCloseSlot` / `isCloseSlot`).
  - `src/main/java/dev/zcripted/obx/listener/menu/ServerSelectorListener.java`
    (closes the inventory when the close slot is clicked).

### Config
- `src/main/resources/systems/hub.yml` — new `selector.close-button`
  (`enabled`, `slot: -1`, `material: "BARRIER"`, `material-fallback`).

### Internal
- `src/main/java/dev/zcripted/obx/hub/HubService.java` — new
  `selectorCloseEnabled()`, `selectorCloseMaterial()`, `selectorCloseSlot()`.

## Notes
- All glyphs verified UTF-8 in source and the compiled `MessageDefaults.class`:
  `▍` (U+258D), math-bold `𝗢𝗕𝗫` (surrogate `ED A0 B5 …`), `›` (U+203A),
  `·` (U+00B7), the 30× `─` (U+2500) divider; German `ß`/umlauts intact;
  **0** mojibake.
- `core.divider` (gold `------`) is left untouched for any non-info commands;
  the styled family uses `core.divider-line`.

## Verification
- `mvn -DskipTests clean package` exits 0, no errors / unmappable warnings;
  obfuscated + `-unobf` jars rebuilt.
- Compiled `MessageDefaults.class`: glyphs present, 0 mojibake. Selector classes
  (`ServerSelectorMenu`/`Holder`/`Listener`) present in the jar.

## Suggested Commit Message

```
Style: Unify /obx help, version, commands, permissions on the /pl report style; add hub selector close button
```
