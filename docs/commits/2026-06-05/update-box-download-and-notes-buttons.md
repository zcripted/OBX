# 🔘 Update Box — Download & Release Notes Buttons

> The "New Release Available" box now ends with `[Download]` (BuiltByBit) and
> `[Release Notes]` (GitHub latest release) buttons in-game — and the same two
> destinations as plain text rows on the console, where click/hover doesn't exist.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 7:39 PM EST |
| **Last Updated** | 2026-06-05 7:50 PM EST |
| **Author** | zcripted |
| **Scope** | Update-available message UX (command + join + periodic announce) |
| **Files changed** | 6 modified |
| **Categories** | Feature · Commands · Messages/i18n · Console/UX |
| **Verification** | ✅ `gradlew build` green (EN/DE/ES parity) · both jars produced |

---

## 📋 Summary (patch notes)

When OBX tells you a new release is out, acting on it is now one click:

- **In-game** the box closes with two buttons: green **[Download]** opens the BuiltByBit
  resource page; purple **[Release Notes]** opens the GitHub latest-release page. Both
  carry divider-formatted hover tooltips showing exactly where they lead.
- **Console** gets the same information as readable text fields instead (buttons can't
  work in a terminal):
  `Download › builtbybit.com/…` and `Release Notes › github.com/zcripted/OBX/releases/latest`.
- One renderer now serves all three delivery paths — `/obx updates`, the join-time notice,
  and the 15-minute periodic announce — so they can never drift apart.
- **(7:50 PM)** `/obx updates` and `/obx updates check` now close **every** outcome with
  the actions — *Up To Date* and *Check Failed* included — via the new shared
  `sendLinkActions(...)`; the `current`/`failed` boxes dropped their trailing blank so the
  actions row closes them cleanly.

## 🔄 Changes

### 🧩 Internal
- [`UpdateChecker.java`](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java)
  — new `RELEASE_NOTES_URL` constant (GitHub latest-release page).
- [`UpdateNotificationService.java`](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java)
  — `sendAvailableMessages(...)` public + recipient-aware: player → OPEN_URL button row,
  console → text block; new `{notes}` placeholder.
- [`ObxModulesView.java`](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java)
  — `/obx updates` UPDATE_AVAILABLE case delegates to the shared renderer.

### 🌐 Messages (EN/DE/ES in lock-step)
- [`MessageDefaultsEN.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
  · [`MessageDefaultsDE.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
  · [`MessageDefaultsES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)
  - `commands.obx.updates.available-link` → two aligned console field rows + closing blank
  - NEW `commands.obx.updates.button.download` / `.hover` and `.button.notes` / `.hover`

### 📄 Change file
- [`docs/changes/2026-06-05---update-box-download-and-notes-buttons.md`](../../changes/2026-06-05---update-box-download-and-notes-buttons.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced; EN/DE/ES key parity green.

## Suggested Commit Message
```
Feature (updates): Download + Release Notes buttons on the update box (text links on console)
```
