# Spawn Prefix + Clickable Delete-Confirm

■ **Created:** 2026-05-27 2:03 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 2:03 pm (America/Detroit)

---

## Summary

Spawn messages now carry a dedicated light-yellow **𝗦𝗣𝗔𝗪𝗡** wordmark prefix (same
character style as the core OBX prefix), and the `/spawn del` confirmation makes
the word **confirm** clickable (runs `/spawn del confirm`).

## Categories

### Messages / prefix
- `language/MessageDefaults.java` — new `teleport.spawn.prefix` =
  `&e𝗦𝗣𝗔𝗪𝗡 &8➠ &e` (math-bold sans-serif S P A W N, light-yellow, same `➠` separator
  as `core.prefix`). Codepoints verified (U+1D5E6/E3/D4/EA/E1).
- `language/LanguageManager.java` — `resolveMessages` now routes `{prefix}` for any
  `teleport.spawn.*` message to `teleport.spawn.prefix` (mirrors how `enchant.*` uses
  `enchant.prefix`). So **all** spawn messages (`set`, `deleted`, `teleporting`, `info`,
  `missing`, the confirm prompt, …) show the SPAWN wordmark instead of OBX.

### `/spawn del` confirmation
- `teleport.spawn.delete-confirm` reworded to `Click {confirm} or type /spawn del
  confirm to delete the spawn. (expires in 15s)`, with a new
  `teleport.spawn.delete-confirm-hover`.
- `command/teleportation/SpawnCommand.java` — new `sendDeleteConfirm(...)` renders the
  message and makes the **confirm** word a `RUN_COMMAND` component (`/spawn del confirm`)
  via `ComponentMessenger.sendJoinedHoverMessages`; console falls back to plain text.
  The existing 15s confirmation window + `/spawn del confirm` typed path are unchanged,
  so clicking the word or typing the command both work.

## Notes / assumptions
- "Light-yellow" → legacy `&e` (the lightest standard yellow; reliable on 1.8.8→1.21.x,
  no hex required).
- The prompt instructs `type /spawn del confirm` (the accurate command) rather than a
  bare `confirm`, because a lone "confirm" chat line isn't a command. If you want
  literally typing `confirm` in chat to work, that needs a one-shot chat capture — easy
  to add on request.

## Testing
- Maven build: exit 0, both jars (obf ~623 KB, unobf ~906 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: `/spawn del` should show the 𝗦𝗣𝗔𝗪𝗡-prefixed prompt with a
  clickable **confirm**; clicking it (or typing `/spawn del confirm` within 15s) deletes
  the spawn; all other spawn messages should now use the SPAWN prefix.

## Suggested Commit Message
```
Feature (spawn): light-yellow SPAWN message prefix + clickable confirm word on /spawn del
```
