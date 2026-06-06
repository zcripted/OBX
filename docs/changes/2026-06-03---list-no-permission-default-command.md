# `/list` Made a True Default Command (No Permission)

■ **Created:** 2026-06-03 11:10 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 11:10 PM (America/Detroit)

`/list` no longer requires any permission — it's a default command available to everyone.
`./gradlew build` is **green**; both jars produced; EN/DE/ES parity test passes.

## Before / after

- **Before:** `/list` declared `permission: obx.list`, and `obx.list` defaulted to `true`.
  So every player could already use it by default, but a permission node existed (and an
  admin could revoke it).
- **After:** the `permission:` line and the in-code `hasPermission("obx.list")` gate were
  removed, and the now-unused `obx.list` node was deleted. `/list` (and aliases
  `/players`, `/online`, `/who`, `/playerlist`) are available to everyone with no
  permission. `obx.list.vanished` still gates whether vanished players are shown.

## Cleanup

- Deleted the **dead orphaned `core.command.ListCommand`** — a leftover duplicate of the
  live `playerinfo.command.ListCommand` (registered in `PlayerInfoModule`). It was
  instantiated/imported nowhere and still referenced `obx.list`, so it was confusing dead
  code.

## Files

- `features/playerinfo/.../command/ListCommand.java` — removed the permission check.
- `core/.../command/ListCommand.java` — **deleted** (dead duplicate).
- `plugin/src/main/resources/plugin.yml` — removed `permission: obx.list` from the `list`
  command and the `obx.list` permission node.
- `docs/information/COMMANDS+PERMISSIONS.md` — `/list` row (no permission).

## Suggested Commit Message

```
Change (list): make /list a permission-free default command (drop obx.list gate + node) and delete the dead duplicate core ListCommand
```
