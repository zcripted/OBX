# Tablist Staff Grouping (OP players separated)

■ **Created:** 2026-05-25 5:35 pm
■ **Last Updated:** 2026-05-25 5:35 pm

## Summary

Staff (OP players) are now grouped together at the top of the TAB player list,
separated from regular players, and shown with the red/bold `ѕᴛᴀꜰꜰ┃` tag before
their name. Implemented with the built-in scoreboard-team sort (no extra
dependencies; works 1.8.8 → 1.21+).

(Per the chosen approach: grouping via team sort. The Minecraft client lays the
list out into auto-columns of ~20 and distributes everyone evenly, so staff
occupy their own column only once the list is large enough to wrap; with a small
list they sit at the top of the single column. A strictly fixed staff column
would require packet-level fake entries / ProtocolLib, which was declined.)

## How it works

- The client orders the player list by scoreboard-team name. SF-Core puts OP
  players on team `sfcore.0staff` and everyone else on `sfcore.1players`; the
  `0`/`1` ensures staff always sort first. Teams are sort-only — no prefix,
  suffix, or nametag option is set, so names above heads are untouched.
- OP players get a distinct tablist entry name (the staff tag); non-OP keep the
  normal name. The visible name is set via `setPlayerListName` (independent of
  the team, which controls sort).

## Changes

- `tablist/format/TablistTeams.java` (new) — manages the two sort teams on the
  main scoreboard. `assign(player, staff)` moves a player only when their staff
  state changes (so the periodic refresh doesn't spam packets); `remove(player)`
  on quit; `reset()` unregisters the teams. All wrapped in try/catch and
  no-ops if the scoreboard API/manager isn't ready.
- `tablist/format/TablistRenderer.java` — `apply` now assigns the team and, for
  OPs, uses the staff name format.
- `tablist/service/TablistService.java` — `isStaffGroupingEnabled()` (default
  true) and `getStaffPlayerFormat()` (default `<red><bold>ѕᴛᴀꜰꜰ┃</bold></red><#AAAAAA>{player}</#AAAAAA>`).
- `tablist/listener/TablistJoinListener.java` — new `onQuit` removes the leaver
  from the teams.
- `tablist/scheduler/TablistRefreshTask.java` — `reset()`s the teams when the
  tablist or grouping is toggled off (so `/sf reload` cleans up).
- `Main.java` — `onDisable` resets the teams.
- `systems/tablist.yml` — new documented `staff-grouping` block (enabled +
  per-OP `player-format`), including the columns caveat.

## Notes
- Gate is `player.isOp()`; OP changes are picked up on the next refresh
  (≤ `refresh-interval-ticks`) or on join. To drive it from a permission/group
  instead, swap the `isOp()` check in `TablistRenderer.apply` / the listener.
- If another plugin manages scoreboard teams for the same players, that plugin's
  team names will compete for the tablist sort — SF-Core assumes it owns this
  (the reporter runs no permissions/tab plugin).

## Verification
- Tag characters verified correct UTF-8 (U+0455 U+1D1B U+1D00 U+A730 U+A730 U+2503)
  in `TablistService.java` and `tablist.yml`; compiled `TablistService.class`
  contains the exact sequence with **0** mojibake.
- `mvn -DskipTests clean package` builds with exit 0, no warnings; obfuscated +
  `-unobf` jars produced.

## Suggested Commit Message

```
Feature (tablist): Group OP/staff at the top of the player list via scoreboard-team sort, with the ѕᴛᴀꜰꜰ┃ tag
```
