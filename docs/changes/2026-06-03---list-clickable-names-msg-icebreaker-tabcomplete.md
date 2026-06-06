# Clickable Player-List Names → /msg, with Ice-Breaker Tab Completion

■ **Created:** 2026-06-03 1:19 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 1:19 PM (America/Detroit)

Made the player names in `/list` clickable to start a private message, and added
family-friendly ice-breaker suggestions to `/msg`'s message-body tab completion.
`./gradlew build` is **green**; both jars produced; EN/DE/ES parity test passes.

## Commands / GUIs

- **Clickable `/list` names** (`ListCommand`): each player name in the Staff/Players
  sections is now an interactive component — **hovering** shows
  *"Click to message <name>"* and **clicking suggests `/msg <name> `** (SUGGEST_COMMAND,
  with a trailing space so the cursor is ready for the message). Console (no click) falls
  back to the plain joined names line. Names keep their color, AFK/vanish suffixes, and
  sort order; the click target uses the raw account name. New key `info.list.name-hover`
  (EN/DE/ES).
- **`/msg` ice-breaker tab completion** (`MsgCommand`): once a recipient is chosen, the
  first word of the message body tab-completes to up to **10** localized, family-friendly
  ice-breakers / Minecraft-humor openers (filtered by what you've typed). They live in the
  new `message.icebreakers` list key (EN/DE/ES) as plain text, so selecting one fills the
  chat line ready to send.

## Files

- `features/playerinfo/.../command/ListCommand.java` — `Entry` holder (raw name + colored
  display) and a `sendNames` helper rendering interactive, click-to-message names (console
  fallback to `info.list.names`).
- `features/mail/.../command/MsgCommand.java` — `onTabComplete` now offers the localized
  ice-breakers for the message body (`args.length == 2`).
- `core/.../language/MessageDefaultsEN.java` / `DE.java` / `ES.java` — `info.list.name-hover`
  and the `message.icebreakers` list (10 entries each).

## Suggested Commit Message

```
Feature (list/msg): make /list player names click-to-message (suggest /msg <name> with hover), and add up to 10 localized family-friendly ice-breaker tab-completions for the /msg message body
```
