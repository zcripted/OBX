# AFK auto-detect fix, /ban-style admin messages + buttons, full gamerule editor

■ **Created:** 2026-06-02 8:16 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 8:16 pm (America/Detroit)

## AFK

- **Root-cause fix:** `AfkService` was constructed but never registered as a
  listener and never started, so neither idle auto-AFK nor activity tracking ran.
  Now registered + started in `Main` (`afkService.start()` with the task fleet,
  `registerEvents(afkService, this)` with the listeners).
- AFK now clears on **any movement OR looking around** (yaw/pitch), not only when
  the player crosses a block boundary.
- Auto-AFK still honors `afk.idle-seconds` (and `afk.kick-seconds`) from `config.yml`.
  - `src/main/java/dev/zcripted/obx/util/control/AfkService.java`
  - `src/main/java/dev/zcripted/obx/Main.java`

## Admin server-control actions — centralized, /ban-styled, listed, console-logged

New `ServerControlActions` helper is the single source for whitelist, join-lock,
clear-entities, kick-non-ops, and spectator-only — used by BOTH the Admin GUI and
the new `/obx` button sub-commands so clicks and chat buttons run identical logic.
Every action sends the actor a `{prefix}` (`/ban`-style) message and mirrors a
line to the console.

- **Kick non-ops:** lists each kicked player (or "none online to kick"); console
  gets a listed roster via `ConsoleLog.list`.
- **Spectator only:** lists each switched player **and the mode they came from**
  (or "none"); console listed.
- **Whitelist / join-lock** enable·disable·toggle: `/ban`-style message **with a
  clickable `[Toggle]` button**.
- **Cleared entities:** `/ban`-style message **with clickable `[All] [Mobs] [Items]`
  buttons** to clear by type.
- **TPS, weather, redstone:** already `{prefix}`-styled in chat; now also logged to
  console.
  - `src/main/java/dev/zcripted/obx/util/control/ServerControlActions.java` (new)
  - `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java` (handlers delegate; removed dead `kickNonOps`/`clearEntities`/`ClearMode`)

## Chat-button click-bridge (hidden, no public command surface)

Minecraft chat buttons can only trigger a command (no "call a method" click
action), so the styled toggle/clear buttons need a command target. Rather than
add public sub-commands, the buttons invoke a single **hidden internal bridge**:
`/obx x-action <action> [mode]`. It is NOT advertised in tab-complete, `/obx
help`, or the docs, requires no new command registration (reuses the already-
registered `/obx`) and no new permission node (gated by `obx.admin.menu`). It
calls the same {@code ServerControlActions} source the GUI uses — zero duplicated
logic. The Admin **GUI** buttons remain wired straight to source (no command hop).
  - `src/main/java/dev/zcripted/obx/command/core/ObxAdminActions.java` (new — `BRIDGE_TOKEN` + `bridge()`)
  - `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java` (single hidden dispatch case)

## Game Rule Editor — wire all boolean gamerules

- Expanded `GameruleEntry` from 13 to the full set of **39 boolean gamerules**
  across MC 1.8 → latest (announce/command-block-output/raids/limited-crafting/
  warden/vines/freeze/global-sound/projectiles-break-blocks/explosion-drop-decay
  variants/water+lava source conversion/…).
- Slots auto-assign across the 45-slot editor, skipping the reserved close (8) and
  back (31) slots. Rules the running server doesn't recognise render as **N/A** and
  are inert (existing graceful path), so one list works on every version. Each icon
  has a universally-available fallback so nothing renders as AIR.
  - `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`

## Messages

Added EN+DE keys: `admin.kick-nonops.{header,entry,none}`,
`admin.spectator.{header,entry,none}`, `admin.button.toggle`,
`admin.{whitelist,joinlock}.button.hover`,
`admin.button.clear-{all,mobs,items}` (+ `.hover`).
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 14 tests, 0 failures.
- Both jars built (obfuscated 947,775 / unobfuscated 1,465,674).

## Notes / caveats

- The clickable buttons require a Player (console gets a plain message + the action
  still runs). They invoke `/obx …` so they work regardless of obfuscation (the
  command name comes from plugin.yml, not the renamed class).
- Runtime smoke test recommended for the GUI/button flows (no server available in
  this environment); logic verified by compile + unit suite + static review.
- Legacy single-line keys `admin.kick-nonops.done` / `admin.spectator.applied`
  are now unused but left in place (harmless).

## Suggested Commit Message

```
Feature(admin): fix AFK auto-detect, /ban-style admin messages + buttons, full gamerules

Register+start AfkService and clear AFK on move/look; centralize whitelist/joinlock/
clear-entities/kick/spectator in ServerControlActions with prefixed messages,
clickable toggle/clear buttons, listed responses, and console output; add /obx
button-target subcommands; expand the Game Rule Editor to all 39 boolean gamerules.
```
