# Chat-Prompt Input Leak Fix — Prompt Replies Broadcast to Public Chat

■ **Created:** 2026-06-07 4:25 pm

■ **Last Updated:** 2026-06-07 4:25 pm

Typing into an OBX chat prompt (e.g. `cancel` in the shop editor's Add Category prompt)
posted the line to public chat before the prompt consumed it. Full breakdown in the
commit log:
[docs/commits/2026-06-07/chat-prompt-input-leak-fix.md](../commits/2026-06-07/chat-prompt-input-leak-fix.md)

## Categories

### GUIs / Chat (Fix)
- **Fix:** prompt-capture listeners ran at HIGHEST — the same priority as the chat
  feature's `ChatManagementListener`, which registers earlier and manually dispatches the
  formatted line to all recipients before cancelling. Prompt input was therefore broadcast
  before being swallowed. All four prompt listeners now capture at LOWEST priority, so the
  line never reaches the chat pipeline.
- `features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopEditorListener.java`
- `features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuInputListener.java`
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/StaffMenuInputListener.java`
- `features/teleport/src/main/java/dev/zcripted/obx/feature/teleport/command/SpawnCommand.java`

## Verification
- `.\gradlew.bat build` green — tests pass; both jars produced.

## Suggested Commit Message
```
Fix (prompts): chat-prompt input leaked to public chat — capture at LOWEST before the chat pipeline renders (shop editor, warp/staff menus, /spawn delete confirm)
```
