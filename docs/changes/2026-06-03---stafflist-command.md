# `/stafflist` — Online Op/Staff List

■ **Created:** 2026-06-03 1:28 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 1:28 PM (America/Detroit)

Added a `/stafflist` command that reuses the player-list box, filtered to online op/staff
only. `./gradlew build` is **green**; both jars produced; EN/DE/ES parity test passes.

## Commands

- **`/stafflist`** (aliases `/onlinestaff`, `/staffonline`; permission `obx.stafflist`,
  default `true`) — renders the boxed player list filtered to **online op/staff only**.
  The count lives in the header bar (`▍ 𝗢𝗕𝗫 › Online Staff · {count}`), names keep their
  red staff color + AFK/vanish suffixes, sort alphabetically, and are **click-to-message**
  (hover hint + `/msg <name>` suggest) just like `/list`. An empty list reads "No staff are
  currently online." Vanished staff are shown only to holders of `obx.list.vanished`.

## Internal

- **Extracted `PlayerListRender`** (playerinfo command package): the shared `Entry` holder,
  the visible-name comparator, and the clickable / console-fallback `sendNames` renderer.
  `/list` was refactored onto it (no behavior change), and `/stafflist` reuses it — one
  implementation of the interactive-name layout for both commands.

## Files

- `features/playerinfo/.../command/PlayerListRender.java` — **new** shared renderer.
- `features/playerinfo/.../command/StaffListCommand.java` — **new** command.
- `features/playerinfo/.../command/ListCommand.java` — refactored onto `PlayerListRender`.
- `features/playerinfo/.../PlayerInfoModule.java` — register `/stafflist`.
- `core/.../language/MessageDefaultsEN.java` / `DE.java` / `ES.java` — `info.stafflist.header`
  + `info.stafflist.none` (reuses `info.list.names` / `.footer` / `.name-hover` / suffixes).
- `plugin/src/main/resources/plugin.yml` — `stafflist` command + `obx.stafflist` permission
  (and documented the previously-undeclared `obx.list.vanished`).
- `docs/information/COMMANDS+PERMISSIONS.md` — `/stafflist` row.

## Suggested Commit Message

```
Feature (stafflist): add /stafflist (online op/staff only) reusing the click-to-message player-list box via a shared PlayerListRender; refactor /list onto it
```
