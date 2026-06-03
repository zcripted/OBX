# Fix — Language Self-Heal Re-Adding a Key Every Reload

■ **Created:** 2026-05-27 4:02 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 4:02 pm (America/Detroit)

---

## Symptom

Every reload printed:
```
[OBX] Added 1 missing keys to language_en.yml
[OBX] Added 1 missing keys to sprache_de.yml
```
The language self-heal is meant to add new message keys to the on-disk YAML **once**
(when the plugin introduces new messages), then stay quiet — not fire every reload.

## Root cause

`commands.obx.config.validation` was registered as a message (the `/obx config validate`
report list) **and** was also the parent of `commands.obx.config.validation.data-missing`.
A YAML node can't be both a value and a section, so writing both collapsed
`commands.obx.config.validation` into a section (dropping the list value).
`LanguageFile.readValue` returns `null` for a configuration section, so `syncDefaults`
saw the default as "missing" and re-added it on **every** reload (one per language file).
Side effect: the validation report value never actually persisted to disk (the command
still worked via the in-jar default fallback).

## Fix

- `language/MessageDefaults.java` — renamed the child `commands.obx.config.validation.data-missing`
  → `commands.obx.config.data-missing` (a sibling, not a child), so `commands.obx.config.validation`
  is a pure leaf and round-trips correctly.
- `command/core/ObxCommand.java` — updated the validate handler to read the new key.
- Verified there are **no remaining keys that are both a leaf value and a parent** in
  `MessageDefaults` (scanned all 656 keys).

## Notes
- Pre-existing (came in with `/obx config validate`), not from recent changes.
- The `[OBX][Arcanum] Loaded 100 custom enchantments across 7 categories.` line is
  normal informational output on enchant (re)load.
- After this build + one reload, the missing-key sync settles and the message stops
  recurring. (The orphaned `commands.obx.config.validation` section already on disk is
  harmless and simply ignored.)

## Testing
- Maven build: exit 0, both jars (obf ~629 KB, unobf ~914 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: reload twice — the second reload should no longer log
  "Added 1 missing keys"; `/obx config validate` still renders correctly.

## Suggested Commit Message
```
Fix (lang): resolve leaf/section key collision (commands.obx.config.validation) that re-added a key every reload
```
