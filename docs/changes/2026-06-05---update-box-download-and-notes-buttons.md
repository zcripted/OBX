# Update Box: Download + Release Notes Buttons (In-Game) / Text Links (Console)

■ **Created:** 2026-06-05 7:39 pm

■ **Last Updated:** 2026-06-05 7:50 pm

## Summary

The "New Release Available" update box now closes with recipient-appropriate links:

- **In-game (players):** a clickable button row at the bottom of the box —
  - `[Download]` (green) — opens the **BuiltByBit resource page** to download the latest
    release; hover tooltip shows the destination + latest version.
  - `[Release Notes]` (purple) — opens the **GitHub latest-release page**
    (`github.com/zcripted/OBX/releases/latest`) to read what changed; hover tooltip
    included.
- **Console:** the same two destinations as **plain text field rows** (no buttons/hover —
  the server console has no click/hover transport):
  ```
    Download       ›  https://builtbybit.com/resources/obx-obsidian-extended.111131/
    Release Notes  ›  https://github.com/zcripted/OBX/releases/latest
  ```

Applies everywhere the "update available" box renders: `/obx updates`, the join-time
notification, and the once-per-version periodic announce — all three now share one
renderer.

### Rev 2 (7:50 pm) — actions on EVERY manual-check outcome
`/obx updates` and `/obx updates check` (same handler) now close **every** result box
with the Download / Release-Notes actions, not just "update available":
- **Up To Date** — box ends with the buttons (in-game) / text rows (console), so a manual
  check still offers the resource page and the current release's notes.
- **Check Failed** — the links are offered so the user can verify availability manually
  when GitHub is unreachable.
- New shared `UpdateNotificationService.sendLinkActions(...)` carries the recipient-aware
  rendering (extracted from `sendAvailableMessages`, which now composes it); the
  `current`/`failed` box keys dropped their trailing blank line in EN/DE/ES so the actions
  row closes the box without a gap.

## Categories

### Internal
- `core/src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java`
  — new `RELEASE_NOTES_URL` constant (`https://github.com/zcripted/OBX/releases/latest`).
- `core/src/main/java/dev/zcripted/obx/util/update/UpdateNotificationService.java`
  — `sendAvailableMessages(...)` is now public and recipient-aware: players get the
  `[Download]` / `[Release Notes]` button row (OPEN_URL click + hover via
  `ComponentMessenger.InteractiveMessagePart.openUrl`), console gets the
  `available-link` text block. New `{notes}` placeholder alongside `{url}`.
- `core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java`
  — the `/obx updates` UPDATE_AVAILABLE case delegates to the shared renderer instead of
  sending the keys itself (command, join, and periodic outputs can no longer drift).

### Messages (EN/DE/ES in lock-step)
- `commands.obx.updates.available-link` — reshaped into the console text block: two
  aligned field rows (`Download ›`, `Release Notes ›`) + closing blank.
- New keys ×3 languages:
  - `commands.obx.updates.button.download` + `.hover` (BuiltByBit; divider-formatted tooltip)
  - `commands.obx.updates.button.notes` + `.hover` (GitHub release notes)
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** (incl. `MessageDefaultsTest` EN/DE/ES
  parity with the 4 new keys per language); both jars produced.

## Suggested Commit Message
```
Feature (updates): Download + Release Notes buttons on the update box (text links on console)
```
