# GUI Lore Localization, Interactive Gamemode/Plugin Info, Server Control Preview & Grow-Tree Fix

■ **Created:** 2026-06-03 9:13 AM (America/Detroit)

■ **Last Updated:** 2026-06-03 9:13 AM (America/Detroit)

Localized the top-level Admin menu GUI item lore into EN/DE/ES, added interactive
click/hover affordances to the gamemode confirmation and the `/pl` plugin list, gave the
Server Control icon a live categorized settings preview, and fixed the Mob Tools "Grow
Tree" button so OBX generates the tree natively (no more WorldEdit `/tree` collision).
`./gradlew build` is **green**; both jars produced (`OBX-1.0.0-beta-b1.jar` +
`-unobf.jar`); EN/DE/ES message-parity test passes.

## Bug Fixes

- **Mob Tools → Grow Tree** (`MainMenuListener.MOB_TOOLS` slot 16): no longer dispatches
  `performCommand("tree")` (which WorldEdit's global `/tree` intercepted, producing
  *"Invalid value for [type] (Not a valid tree type: tree)"*). It now calls a new
  OBX-native `growRandomTree(...)` that:
  - Targets the player's crosshair (`getTargetBlock(... , 100)`), falling back to the
    player's feet.
  - Picks a **random** tree from `org.bukkit.TreeType.values()` — which is automatically
    version-correct (Cherry/Mangrove/etc. only appear on versions that ship them), so the
    supported tree set follows the running server's Minecraft version with no manual
    gating.
  - Shuffles + falls through to a type that can actually grow at the location, then sends
    the already-localized `mob.tree.grown` / `mob.tree.failed` messages.
  - The Grow Tree item lore now reads "grows a random tree" instead of "runs /tree".

## Commands / Interactive Messages

- **Gamemode confirmation** (`GamemodeCommand.changeMode`): the `{mode}` value in
  *"Set your gamemode to X"* (`gamemode.changed-self`) and *"Set PLAYER's gamemode to X"*
  (`gamemode.changed-other`) is now an **interactive component** — hovering shows a revert
  tooltip and **clicking reverts** the gamemode to the previous value
  (`/gamemode <previous>`, or `/gamemode <previous> <target>` for the other-player form).
  Console recipients fall back to the plain message. New helper `sendModeChange(...)`
  splits the localized template around a private sentinel so the surrounding text stays
  intact.
- **`/pl` plugin list** (`PluginListCommand`): each plugin name is now **clickable** —
  clicking opens a **box-style info card** with Name, Version, Author, Software, API,
  Status, total **server uptime since last restart**, and the plugin **description**
  (word-wrapped at 42 visible chars, no mid-word breaks). The hover tooltip gained a
  "Click to view detailed info" line. Routed through a new hidden
  `/obx plugininfo <name>` bridge (gated by `obx.pl`, not advertised — mirrors the
  existing `x-action` bridge).

## GUIs

- **Admin menu — Server Control icon** (`AdminMenu` slot 19): replaced the static
  "Future slot for restarts…" placeholder lore with a **live, categorized preview of 13
  current server settings** grouped under Access / Gameplay / World / Server
  (Whitelist, Players, Difficulty, Default Mode, PvP, Hardcore, View Distance, Spawn
  Protect, Allow Flight, Nether, The End, Version, Uptime). Every rendered line is capped
  at **45 visible characters** (color codes excluded) on a word boundary — no mid-word
  breaks.
- **Admin menu — localized item lore (EN/DE/ES)**: the top-level Admin menu icons now pull
  their names + lore from the language files instead of hardcoded English — the 8 grid
  items (Server Control, Economy, Moderation, World Tools, Roles & Permissions, Chat
  Settings, Fun Utilities, Diagnostics) plus the Admin Panel info book, Close button, Warp
  Manager, and Hub / Lobby Controls (including the live enabled/disabled + world-count
  lore and the permission-locked variants). The generic sub-menu (Economy / World Tools /
  Roles / Chat Settings / Diagnostics) inherits the localized title + info card.
  - **Routing robustness:** `PlaceholderData` / `PlaceholderView` now carry a stable
    language-independent **`key`** ("server-control", "moderation", …); `AdminSubMenu.open`
    routes on that key instead of the stripped English display name, so navigation works
    identically for DE/ES players.

## Assumption (documented per CLAUDE.md)

"Localize the GUI item lore into EN/DE/ES" was scoped to the **top-level Admin menu**
(the panel these changes live in) plus the Server Control preview. The deeper Admin
sub-panel interiors (Server Control category items, Jail Center, Mob Tools, World Controls,
Weather/Time/Gamerule editors) still render in English — they are a larger, separate
follow-up and were left intact to avoid a high-risk sweep in this pass.

## Files

- `plugin/src/main/java/dev/zcripted/obx/core/gui/main/MainMenuListener.java`
  — Grow Tree now calls `growRandomTree(...)` (+ `prettyTreeName`); `placeholderForSlot`
  call sites updated for the new `(plugin, player, slot)` signature.
- `features/playerstate/src/main/java/dev/zcripted/obx/feature/playerstate/command/GamemodeCommand.java`
  — interactive revert via `sendModeChange(...)`.
- `core/src/main/java/dev/zcripted/obx/core/command/PluginListCommand.java`
  — clickable names → `sendPluginInfo(...)` box card (+ `wrapText`, `formatUptime`,
  `findBrokenByName`).
- `plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java`
  — hidden `plugininfo` bridge case.
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenu.java`
  — Server Control live preview (`buildServerControlItem`/`serverControlLore` + helpers);
  key-based, localized placeholder + fixed-item rendering; `PlaceholderData`/
  `PlaceholderView` gain `key`.
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`
  — `open(...)` routes on `placeholder.key()`; Grow Tree lore text.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` / `DE.java` /
  `ES.java` — new keys: `gamemode.revert-hover-*`, `commands.pl.name-hover`,
  `commands.pl.info.*`, `admin.scp.*` (Server Control preview),
  `admin.menu.info/close/item.*/warp/hub.*` (localized Admin menu).

## Suggested Commit Message

```
Feature (GUI/i18n): localize Admin menu item lore (EN/DE/ES), live Server Control settings preview, click-to-revert gamemode + clickable /pl plugin info card; fix Mob Tools Grow Tree to generate natively (random version-supported tree)
```
