# Staff Chat — Toggle Mode, Reply Wiring & Sender-Placeholder Fix

■ **Created:** 2026-06-03 11:54 AM (America/Detroit)

■ **Last Updated:** 2026-06-03 11:54 AM (America/Detroit)

Added a persistent staff-chat **toggle mode** with box-style status feedback, wired
`/rply` to answer a staff-chat message **back into staff chat** (independent of toggle
state), and fixed the staff-chat line rendering the literal `{sender}` placeholder.
`./gradlew build` is **green**; both jars produced; EN/DE/ES parity test passes.

## Commands

- **`/staffchat toggle | on | off`** (aliases `/sc`, `/achat`) — flips persistent staff-chat
  mode for the player. While on, their normal chat is redirected into staff chat until
  turned off. Each toggle replies with a **box-style status** message
  (`messaging.staffchat.toggle.enabled` / `.disabled`). `/staffchat <message>` still sends a
  one-off line. Tab-complete suggests `toggle`/`on`/`off`; console gets `core.player-only`.

## Internal

- **New `StaffChatService`** (`feature.mail.staffchat`, registered + listener in `MailModule`):
  owns the per-player toggle set and the `dispatch(sender, message)` path (the broadcast to
  all online `obx.staffchat` holders + console). On dispatch it calls
  `PrivateMessageService.markStaffChatReplyTarget(...)` for every recipient and clears a
  player's toggle on quit.
- **Reply wiring** (`PrivateMessageService`): new `STAFF_CHAT_ID` reply-target sentinel +
  `markStaffChatReplyTarget`. `deliverToId` routes that sentinel to
  `StaffChatService.dispatch` (gated by `obx.staffchat`), so both `/rply <msg>` and the
  click-to-reply draft answer into staff chat. Because the reply target is set when a
  staff-chat line is **received**, this works whether or not the replier is toggled in — and
  the reply is itself sent as a staff-chat message. The chat listener's `onChat` now also
  redirects a toggled player's normal chat into staff chat (a pending reply **draft** always
  takes precedence).
- **Bug fix:** the one-off send previously passed a `{player}` placeholder while
  `messaging.staffchat.line` uses `{sender}`, so the name rendered as the literal
  `{sender}`. Dispatch now supplies `{sender}` (and `{message}`), so the sender name renders
  correctly.

## Messages (EN/DE/ES)

- New: `messaging.staffchat.reply-label`, `messaging.staffchat.toggle.enabled` (list),
  `messaging.staffchat.toggle.disabled` (list).
- Updated: `messaging.staffchat.usage` now lists the `toggle` sub-command.

## Files

- `features/mail/.../staffchat/StaffChatService.java` — **new** (toggle state + dispatch).
- `features/mail/.../command/StaffChatCommand.java` — toggle handling + send via the service
  (placeholder fix).
- `features/mail/.../pm/PrivateMessageService.java` — staff-chat reply sentinel, mark method,
  `deliverToId` routing, toggle redirect in `onChat`.
- `features/mail/.../MailModule.java` — register + listen `StaffChatService`.
- `core/.../language/MessageDefaultsEN.java` / `DE.java` / `ES.java` — new staff-chat keys.
- `plugin/src/main/resources/plugin.yml` — `/staffchat` usage/description updated.
- `docs/information/COMMANDS+PERMISSIONS.md` — `/staffchat` row updated.

## Cleanup

- Removed the leftover `/tmp/esbatch/` translation-batch scratch files (and the dead-code
  scan scratch files) from the earlier sessions.

## Suggested Commit Message

```
Feature (staffchat): add /sc toggle mode with box-style status, wire /rply to answer staff-chat messages back into staff chat regardless of toggle state, and fix the {sender} placeholder rendering literally in the staff-chat line
```
