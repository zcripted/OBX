# 🧾 /pl Console Fix — Grouped Names, No Timestamps

> Console `/pl` no longer leaks `[HH:mm:ss INFO]:` prefixes into the box, and each
> platform's plugin names now print directly under their own section header.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 6:13 PM EST |
| **Last Updated** | 2026-06-05 6:13 PM EST |
| **Author** | zcripted |
| **Scope** | /pl console rendering |
| **Files changed** | 1 modified |
| **Categories** | Fix · Console/UX |
| **Verification** | ✅ `gradlew build` green · both jars produced |

---

## 📋 Summary (patch notes)

Running `/pl` from the console used to print the two platform headers together, then dump
all the plugin names below them with logger timestamps. The name rows were taking the chat
fallback path (`sender.sendMessage` → logger pipeline → timestamp + different stream) while
the rest of the box used OBX's direct console writer. The name rows now use the same direct
writer, so:

- **No timestamps anywhere in the box** — only the command echo line keeps its server-printed
  timestamp.
- **Names sit under their platform** — `Bukkit · 7` is followed by its seven plugins, then
  `Paper · 1` by its one.
- In-game `/pl` is unchanged (hover tooltips + click-to-info per plugin name).

## 🔄 Changes

- [`core/src/main/java/dev/zcripted/obx/core/command/PluginListCommand.java`](../../../core/src/main/java/dev/zcripted/obx/core/command/PluginListCommand.java)
  — `sendPluginLine(...)` console branch: join the colorized names and emit via
  `sendPlain` (direct stdout) instead of the component fallback.

### 📄 Change file
- [`docs/changes/2026-06-05---pl-console-grouping-and-timestamps.md`](../../changes/2026-06-05---pl-console-grouping-and-timestamps.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Fix (pl): console list rows under their platform header, no logger timestamps
```
