# OP Staff Chat Tag

■ **Created:** 2026-05-25 5:08 pm
■ **Last Updated:** 2026-05-25 5:47 pm

> Update 2026-05-25 5:47 pm — added a space on each side of the `┃` divider, so
> the tag now renders `ѕᴛᴀꜰꜰ ┃ <name>`. Applied to all four definitions: the chat
> default (`ChatService.getStaffPrefix`) + `chat_management.yml`, and the tablist
> default (`TablistService.getStaffPlayerFormat`) + `tablist.yml`. Verified the
> spaced tag is correct UTF-8 (U+0455 U+1D1B U+1D00 U+A730 U+A730 U+0020 U+2503
> U+0020) in every file; build clean.

## Summary

OP players now get a tag before their name in chat that renders as a red, bold
small-caps "STAFF" followed by a heavy vertical bar: **ѕᴛᴀꜰꜰ┃**`<username>`.
Non-OP players are unchanged. The tag is on by default (no config edit needed)
and fully configurable.

Example OP line: `ѕᴛᴀꜰꜰ┃Steve » hello`  (tag red+bold, name gray, `»` yellow).

## Changes

- `chat/service/ChatService.java`
  - `isStaffPrefixEnabled()` — reads `format.components.prefix.enabled` (default true).
  - `getStaffPrefix()` — reads `format.components.prefix.op`; default
    `<red><bold>ѕᴛᴀꜰꜰ┃</bold></red>` (chars U+0455 U+1D1B U+1D00 U+A730 U+A730 U+2503).
- `chat/listener/ChatManagementListener.java`
  - `buildPlaceholders` sets `prefix` = the tag when `player.isOp()` (and the tag
    is enabled), otherwise an empty string.
- `chat/format/ChatFormatter.java`
  - `compose` prepends the `prefix` value directly before the rendered username.
    Done in code (not via the master template), so it works with existing
    configs and any layout, and is a no-op for non-staff.
- `systems/chat_management.yml`
  - New documented `format.components.prefix` block (`enabled` + `op`).

## Notes
- Gate is `player.isOp()` per request. To instead drive it from a permission
  (e.g. for LuckPerms groups), swap the `player.isOp()` check for a
  `hasPermission(...)` call.
- The tag text is MiniMessage, so owners can change colour/decoration or the
  characters in `chat_management.yml`. Add a trailing space in `op` if a gap
  before the name is wanted.
- Works out-of-the-box: the default comes from code, so an already-deployed
  `chat_management.yml` without the new block still shows the tag for OPs.

## Verification
- Tag characters confirmed correct (U+0455 U+1D1B U+1D00 U+A730 U+A730 U+2503) in
  both `ChatService.java` and `chat_management.yml`.
- Compiled `ChatService.class` contains the exact `ѕᴛᴀꜰꜰ┃` UTF-8 sequence and
  **zero** mojibake — the characters survive `javac`.
- `mvn -DskipTests clean package` builds with exit 0, no "unmappable character"
  warnings; obfuscated + `-unobf` jars produced.

## Suggested Commit Message

```
Feature (chat): Add red/bold "ѕᴛᴀꜰꜰ┃" tag before OP players' names (configurable)
```
