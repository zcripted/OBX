# Teleport, Position & Private Messaging (+ /help categories)

■ **Created:** 2026-05-27 4:41 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 8:54 pm (America/Detroit)

---

## Summary

Adds three command families — admin/player teleportation, a `/pos` coordinate command, and
a full private-message system with an offline inbox GUI — plus a new **Messaging** `/help`
category, with all new commands categorized in `/help` and `COMMANDS+PERMISSIONS.md`.

## Categories

### Teleport
- `command/teleportation/TeleportCommand.java` — `/tp` (+ `/teleport`) and `/tphere`,
  admin-only (`obx.teleport.admin`). `/tp <player>` (go to) / `/tp <a> <b>` (move one to
  another) / `/tphere <player>` (bring). Console can move players but has no position itself,
  and there's no console to teleport to.
- `command/teleportation/TpaCommand.java` + `util/teleport/TeleportRequestService.java` —
  `/tpa <player>` requests a teleport; the target gets a styled prompt with clickable
  **Accept** / **Deny** (which run `/tpaccept` / `/tpdeny`), 60s expiry. Default-allowed
  (`obx.teleport.request`).

### Position
- `command/teleportation/PositionCommand.java` — `/pos` (+ `/position`,
  `obx.position`): a styled coordinate report with a **click-to-copy** line
  (COPY_TO_CLIPBOARD, added to `ComponentMessenger`; falls back to suggest on <1.15) and a
  **live action bar** that updates as you move and stops after ~5s.

### Private messaging
- `message/MessageService.java` (Listener) — live DM delivery with a clickable **Reply**,
  last-sender reply targets, a 60s **reply draft** (chat-captured; type to send, `cancel`
  or the Cancel button aborts), an **offline inbox** (queued in `MessageStore` →
  `messages.yml`), and a join notification with a clickable **Open**.
- `message/InboxMessage.java` / `MessageStore.java` — message model + capped (28) per-player
  YAML persistence.
- `message/InboxMenu.java` / `InboxMenuHolder.java` + `listener/menu/InboxMenuListener.java`
  — 54-slot inbox GUI: glass-pane border, message-preview items (sender, local
  date + `h:mm a` time, short preview) in a 4×7 grid; click to read the full message in chat.
- Commands `command/message/{MsgCommand,ReplyCommand,InboxCommand}.java` — `/msg` (+
  `/tell`, `/pm`, `/whisper`), `/rply` (+ `/reply`, `/r`), `/inbox` (+ `/inbound`),
  `obx.message`. Players **cannot** message the console; the console may send "as Console"
  (those messages carry no reply button). Safeguards for no recent sender / offline targets.

### /help + docs
- `gui/player/HelpGuiMenu.java` — new **Messaging** filter category; all new commands mapped
  to their categories (Teleport for tp/tphere/tpa/pos, Messaging for msg/rply/inbox). The GUI
  already lists per-viewer-runnable commands A–Z, so the new ones appear for whoever can use them.
- `MessageDefaults` — `commands.help.gui.category.messaging` + all new command/feature messages
  (EN+DE).
- `docs/information/COMMANDS+PERMISSIONS.md` — restructured into **one table per category**
  (Admin, Core, Language, Messaging, Moderation, Teleport, Utility, Other) matching the
  `/help` GUI, with a clickable **Index** of anchor links at the top that scroll to each
  category section. The old single "Help GUI Categories" legend + combined Player/Admin
  tables were replaced; every existing row was re-bucketed into its category (bold
  sub-group rows kept for Homes/Spawn/Warps/Hub under Teleport, Gamemode under Utility, and
  Arcanum + Wildcards under Other), and the redundant inline `*(Teleport)*` / `*(Messaging)*`
  category tags were dropped since the section header now conveys the category.

### Wiring / permissions
- `Main` constructs the services, registers `MessageService` + `InboxMenuListener`, and binds
  the commands. `plugin.yml` declares the commands + new permissions: `obx.teleport.admin`
  (op), `obx.teleport.request` / `obx.position` / `obx.message` (default true).

## Notes / assumptions
- Click-to-reply uses a stateful 60s draft (chat capture), per the request; the clickable
  word runs `/rply` (start draft) / `/rply cancel`.
- Offline-target resolution uses `Bukkit.getOfflinePlayer(name)` gated by `hasPlayedBefore()`
  (standard approach; a typo'd unknown name may incur a one-time profile lookup).
- The reply chat-capture and the `/spawn delete` chat-capture both run at HIGHEST and only
  act on their own pending state, so they coexist.

## Testing
- Maven build: exit 0, both jars (obf ~662 KB, unobf ~959 KB). ProGuard `Note:` lines only.
  Compile-verified only. In-game checks: `/tp`/`/tphere` (admin) + `/tpa` accept/deny;
  `/pos` copy + live action bar; `/msg` online/offline, `/rply` direct + draft + cancel,
  offline inbox + join notice + GUI read; `/help` Messaging filter + new commands A–Z; and
  that players can't message Console / Console can't be teleported.

## Suggested Commit Message
```
Feature: teleport (/tp /tphere /tpa), /pos, and private messaging (/msg /rply /inbox + offline inbox GUI); add Messaging /help category + docs
```
