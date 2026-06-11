# 💬 Chat-Prompt Input Leak Fix — Prompt Replies No Longer Broadcast to Public Chat

> Typing into any of OBX's chat prompts (shop editor price/category input, warp menu
> prompts, staff menu player search, `/spawn delete` "confirm") **leaked the typed line
> into public chat** before the prompt swallowed it. Reported against the shop editor's
> Add Category prompt: typing `cancel` cancelled the prompt but also posted "cancel" for
> everyone to see. Root cause: the prompt listeners ran at HIGHEST priority — the same
> priority as the chat feature's `ChatManagementListener`, which **registers earlier**
> (ChatModule is first in `registerModules()`) and **dispatches the formatted line to all
> recipients itself** before cancelling the event. By the time a prompt listener cancelled,
> the message was already on everyone's screen. All four prompt listeners now capture at
> **LOWEST** priority, so they swallow the line before the chat pipeline ever sees it.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 4:25 PM EST |
| **Last Updated** | 2026-06-07 4:25 PM EST |
| **Author** | zcripted |
| **Scope** | Economy (shop editor), Warp, Staff, Teleport — chat-prompt capture listeners |
| **Files changed** | 4 code + 2 docs |
| **Categories** | Fix · GUIs · Chat |
| **Verification** | ✅ `gradlew build` green (tests + shadowJar + ProGuard + encryptStrings, both jars) |

---

## 📋 Summary (patch notes)

- **Fixed: prompt input no longer appears in public chat.** Anything typed while OBX is
  awaiting a chat input — a shop-editor price or category id (including `cancel`), a warp
  menu prompt, a staff-menu player search, or the `/spawn delete` `confirm` keyword — was
  briefly broadcast to all players before being consumed. Prompt input is now fully
  private: it is captured and swallowed before the chat formatter runs.

## 🐛 Root cause

`ChatManagementListener` (chat feature, HIGHEST, `ignoreCancelled = true`) renders the
chat line and **manually sends it to every recipient**, then cancels the event. The four
prompt listeners also ran at HIGHEST — but within one priority Bukkit fires handlers in
**registration order**, and `ChatModule` is registered first in `OBX.registerModules()`.
So the pipeline was: chat formatter broadcasts the line → prompt listener cancels the
(already-delivered) event → prompt processes the input. Moving the prompt capture to
**LOWEST** (earliest) makes the chat formatter skip the cancelled event entirely.

## 🔧 Changes (newest at top → oldest)

### Prompt listeners → LOWEST priority (+ explanatory comments)
- [features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java)
  — `onChat` (buy/sell price, add item, add category, `cancel`); class javadoc updated.
- [features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuInputListener.java](../../../features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuInputListener.java)
  — `onChat` (warp menu prompts).
- [features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/StaffMenuInputListener.java](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/StaffMenuInputListener.java)
  — `onChat` (player-search prompt).
- [features/teleport/src/main/java/dev/zcripted/obx/feature/teleport/command/SpawnCommand.java](../../../features/teleport/src/main/java/dev/zcripted/obx/feature/teleport/command/SpawnCommand.java)
  — `onConfirmChat` (`confirm` capture; non-matching messages still pass through as
  normal chat).

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---chat-prompt-input-leak-fix.md](../../changes/2026-06-07---chat-prompt-input-leak-fix.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`).
- Note: `PrivateMessageService.onChat` (mail feature, staff-chat-mode capture) already ran
  at HIGH — before HIGHEST — and did not leak; left unchanged.
