# Staff Chat Usage Reformat, Player-List Sections & Toggle-Leak Fix

■ **Created:** 2026-06-03 12:33 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 12:33 PM (America/Detroit)

Reformatted the staff-chat command (bare toggle), split the player list into Staff and
Players sections, and fixed toggled staff-chat messages leaking to non-staff. `./gradlew
build` is **green**; both jars produced; EN/DE/ES parity test passes.

## Commands

- **`/sc` reformat** (`StaffChatCommand`): the **bare command** now toggles staff-chat mode
  (was `/sc toggle`); **`/sc <message>`** sends a single staff-chat line whether or not
  toggle mode is on. The `toggle|on|off` sub-words were removed (so "toggle" is no longer a
  reserved word — `/sc toggle` now just sends "toggle"). Usage messages (EN/DE/ES),
  `plugin.yml`, and the toggle-confirmation hint were updated; the now-unused
  `StaffChatService.setToggled` was removed.

## GUIs / Messages

- **`/list` Staff + Players sections** (`ListCommand`): the boxed report now renders two
  categorized sections — **`▸ Staff (n)`** and **`▸ Players (n)`** — each with its sorted,
  colored names (op names red, players yellow, with AFK/vanish suffixes) or **`(none)`** when
  empty, between the existing top/summary/bottom bars. Keeps the standard OBX box theme.
  New keys: `info.list.section.staff`, `info.list.section.players`, `info.list.names`,
  `info.list.section-empty` (EN/DE/ES).

## Fixes

- **Toggled staff-chat leak**: with staff-chat mode on, a normal chat message routed to
  staff chat was **also broadcast to all players as normal chat**. Cause: both
  `ChatManagementListener` (the chat formatter) and `PrivateMessageService.onChat` ran at
  `EventPriority.HIGHEST`, where same-priority order is registration-dependent — when the
  formatter ran first it snapshotted recipients and broadcast the message before PM could
  cancel the event. Fix: PM's chat capture now runs at **`EventPriority.HIGH`** (strictly
  before the formatter's `HIGHEST` + `ignoreCancelled=true`), so the draft/staff-chat-toggle
  redirect cancels the event *before* it is broadcast. (This also fixes the same latent leak
  for the click-to-reply draft.) `/sc <message>` was already safe because it is a command,
  not a chat event.

## Files

- `features/mail/.../command/StaffChatCommand.java` — bare-toggle / message reformat.
- `features/mail/.../staffchat/StaffChatService.java` — removed unused `setToggled`.
- `features/mail/.../pm/PrivateMessageService.java` — `onChat` priority `HIGHEST` → `HIGH`.
- `features/playerinfo/.../command/ListCommand.java` — Staff/Players section rendering.
- `core/.../language/MessageDefaultsEN.java` / `DE.java` / `ES.java` — list section keys +
  staff-chat usage/hint updates.
- `plugin/src/main/resources/plugin.yml` — `/staffchat` usage.
- `docs/information/COMMANDS+PERMISSIONS.md` — `/staffchat` row.

## Suggested Commit Message

```
Feature/Fix (staffchat + list): make bare /sc toggle staff-chat mode (/sc <message> sends), split /list into Staff and Players sections, and fix toggled staff-chat messages leaking to non-staff by capturing chat at HIGH (before the HIGHEST formatter broadcast)
```
