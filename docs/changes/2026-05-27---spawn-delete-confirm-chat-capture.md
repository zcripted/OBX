# `/spawn delete` — Click / Chat "confirm" with 10s Timeout

■ **Created:** 2026-05-27 3:40 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 3:40 pm (America/Detroit)

---

## Summary

Removed the typed `/spawn del confirm` subcommand. A `/spawn delete` now confirms by
**clicking the "confirm" word** in the prompt or **typing `confirm` in chat**, within a
**10-second** window; on timeout the player is notified with a clickable re-issue.

## Categories

### Behavior (`command/teleportation/SpawnCommand.java`)
- `SpawnCommand` now implements `Listener`.
- `handleDelete` no longer accepts a `confirm` argument. It arms a pending confirmation
  (10s) and shows the prompt, then schedules a 10s timeout task.
- **Confirm paths** (both call a shared `confirmDelete`, valid only within 10s):
  - **Chat capture** — `onConfirmChat(AsyncPlayerChatEvent, HIGHEST)`: while a delete is
    pending, a chat line of exactly `confirm` is cancelled (not broadcast) and the delete
    runs on the player's region/main thread. Any other message passes through normally.
  - **Click** — the "confirm" word runs a hidden internal `/spawn confirmdelete` token
    (intercepted at the top of `onCommand`, not a documented subcommand).
- **Timeout** — after 10s unconfirmed, the pending is cleared and the player gets
  `teleport.spawn.delete-timeout` with a clickable **retry** (runs `/spawn del`).
- Console (no click/chat) deletes immediately on `/spawn delete`.
- `deleteConfirmations` is now a `ConcurrentHashMap` (async chat + main + timeout access);
  the old `CONFIRM_WORDS` set and the delete tab-completion arg were removed.
- `Main` registers `spawnCommand` as an event listener.

### Messages (`MessageDefaults`)
- Reworded `teleport.spawn.delete-confirm` → "Click {click} or type `confirm` in chat … (10s)"
  ({click} = the clickable word).
- Added `teleport.spawn.delete-timeout` + `teleport.spawn.delete-timeout-hover`.
- `docs/information/COMMANDS+PERMISSIONS.md` `/spawn delete` row updated (no `confirm` arg).

## Notes
- A chat click can only RUN_COMMAND, so the clickable word routes through a hidden
  `/spawn confirmdelete` token rather than the (removed) `/spawn del confirm`. It's a no-op
  without a pending confirmation, so it can't be abused.
- Supersedes `docs/changes/2026-05-27---spawn-prefix-and-clickable-delete-confirm.md`'s
  confirm flow (the light-yellow 𝗦𝗣𝗔𝗪𝗡 prefix from that change is unchanged).

## Testing
- Maven build: exit 0, both jars (obf ~629 KB, unobf ~914 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: `/spawn del` → prompt; click **confirm** or type `confirm`
  (within 10s) deletes; waiting 10s shows the timeout notice with a clickable retry; typing
  `/spawn del confirm` no longer special-cases (just re-prompts).

## Suggested Commit Message
```
Feature (spawn): /spawn delete confirm via click or chat "confirm" + 10s timeout; drop /spawn del confirm subcommand
```
