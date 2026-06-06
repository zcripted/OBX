# Permission-Based Staff Definition + Short `/stafflist` Aliases

■ **Created:** 2026-06-03 11:01 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 11:01 PM (America/Detroit)

"Staff" in the player-list family is now **op _or_ the `obx.staff` permission** (not op
only), and `/stafflist` gained short memorable aliases. `./gradlew build` is **green**;
both jars produced; EN/DE/ES parity test passes.

## Permissions

- **`obx.staff`** (default `op`): marks a player as staff for the `/list` Staff/Players
  grouping and for `/stafflist`. Staff status is now `isOp() || hasPermission("obx.staff")`,
  so a member can be staff by op, by permission, or both. Ops remain staff out of the box.

## Commands

- **`/stafflist` aliases** changed from `onlinestaff`, `staffonline` to
  **`sl`, `os`, `mods`, `slist`, `sonline`** (all verified unique across the plugin's full
  command/alias set).

## Internal

- `PlayerListRender` gained `STAFF_PERMISSION` + `isStaff(Player)`; `/list` and `/stafflist`
  both call it, so the op-or-permission rule lives in one place.

## Files

- `features/playerinfo/.../command/PlayerListRender.java` — `STAFF_PERMISSION` + `isStaff`.
- `features/playerinfo/.../command/ListCommand.java` — uses `PlayerListRender.isStaff`.
- `features/playerinfo/.../command/StaffListCommand.java` — uses `PlayerListRender.isStaff`.
- `plugin/src/main/resources/plugin.yml` — `/stafflist` aliases; new `obx.staff` permission.
- `docs/information/COMMANDS+PERMISSIONS.md` — `/list` + `/stafflist` rows.

## Suggested Commit Message

```
Feature (list/stafflist): treat staff as op OR obx.staff permission (shared PlayerListRender.isStaff), and add short /stafflist aliases sl/os/mods/slist/sonline
```
