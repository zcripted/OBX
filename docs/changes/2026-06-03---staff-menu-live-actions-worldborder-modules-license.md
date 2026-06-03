# Staff Menu live refresh + actions, World Border, Module Toggles, LICENSE

■ **Created:** 2026-06-03 12:42 am (America/Detroit)

■ **Last Updated:** 2026-06-03 12:42 am (America/Detroit)

Closes the placeholder/unfinished-feature gaps flagged in the market-readiness
review, plus adds a LICENSE.

## Staff Menu — live refresh (online players + viewer timers)

- `StaffMenu.refresh(plugin, viewer, holder)` re-renders the current page's online
  player heads **and** the viewer's own head **in place** (no re-open, so cursor
  and scroll are untouched), keeping the online list current and the viewer head's
  total-active / current-session time ticking.
- `AdminMenuRefreshTask` now drives both the admin submenus and any open Staff Menu,
  at **~0.5 s (10 ticks)** — sub-second, the practical floor for Bukkit's tick
  scheduler — and only does work for players who actually have such a menu open.
- `StaffMenuHolder.setPlayerAt(...)` lets the refresh keep click-routing correct.
  - `gui/admin/StaffMenu.java`, `gui/admin/AdminMenuRefreshTask.java`, `gui/admin/StaffMenuHolder.java`

## Staff Menu — moderation action flow wired

The per-player Actions menu's Warn / Mute / Kick / Tempban / Ban buttons now work:
clicking one closes the GUI and prompts for a reason in chat (reusing the staff
search chat-capture); the typed reason runs the real `/warn|/mute|/kick|/tempban|/ban`
command, so the **existing box-style responses, permission checks, and logging all
fire**. Empty / `cancel` aborts and reopens the action menu.
  - `gui/admin/StaffMenuInputManager.java` (action prompt + `processActionInput`)
  - `listener/menu/StaffMenuListener.java` (button → prompt)

## Admin Menu — World Border control wired

World Controls → **World Border** now opens a working submenu acting on the
operator's current world: − 1000 / − 100 / + 100 / + 1000 diameter, **Center On Me**,
and **Reset** (vanilla default). Live readout of world + diameter.
  - `gui/admin/AdminSubMenu.java` (`openWorldBorderMenu` / `handleWorldBorderClick`, `SubMenuType.WORLD_BORDER`)
  - `listener/menu/MainMenuListener.java` (routing)

## Admin Menu — Module Toggles wired

Plugin + Systems → **Toggle Modules** now opens a submenu that toggles each module
on/off, persisting to config and applying live: Chat Formatting, Scoreboard,
Tablist, Join/Leave Broadcasts, Welcome MOTD, Hub/Lobby. Existing setters are used
where present (Hub, Join/Leave, Join MOTD); the systems-config modules
(chat/scoreboard/tablist) flip their `enabled` flag on disk and `reload()`.
  - `gui/admin/AdminSubMenu.java` (`openModulesMenu` / `handleModulesClick` / `ModuleEntry` / `writeModuleFlag`, `SubMenuType.MODULES`)
  - `listener/menu/MainMenuListener.java` (routing)

## Messages (EN + DE)

`admin.staff.action.prompt`, `admin.staff.action.cancelled`,
`admin.world.border.{size,centered,reset}`, `admin.module.toggled`,
`admin.module.state.{enabled,disabled}`.
  - `language/MessageDefaults.java`

## LICENSE

Added a proprietary **EULA / All-Rights-Reserved** license (grant, restrictions,
ownership, termination, warranty disclaimer, liability cap, third-party note).
  - `LICENSE`

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 15 tests, 0 failures; both jars built.

## Notes / caveats

- Refresh cadence is tick-bound (Bukkit's minimum is 50 ms); 0.5 s reads as live
  for second-resolution session timers. Each refresh rebuilds heads (incl. a
  moderation-profile lookup) only for players with the menu open.
- Module toggles persist + reload immediately; for the scoreboard/tablist, a board
  already drawn may clear on the next refresh/rejoin rather than instantly.
  Holograms and always-on services (economy/moderation) are intentionally not in
  the toggle list (managed via `/holo` and always active).
- The old `*.placeholder` / `*-placeholder` language keys are now unused but left in
  place (harmless).
- Runtime smoke test still recommended (no live server here): the action-reason
  chat flow, world-border edits, and module toggles were verified by compile +
  the unit suite + static review.

## Suggested Commit Message

```
Feature(admin): live Staff Menu + wired moderation actions, World Border, Module Toggles; add LICENSE

Staff Menu refreshes online players + viewer timers ~0.5s in place; Warn/Mute/Kick/
Tempban/Ban buttons prompt a reason and run the real commands; World Controls →
World Border and Plugin+Systems → Toggle Modules are now functional submenus;
add proprietary EULA LICENSE.
```
