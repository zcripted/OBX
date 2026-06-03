# Welcome Messages Moved to Language System + MOTD Toggle-Command Hover

■ **Created:** 2026-06-01 1:55 am (America/Detroit)

■ **Last Updated:** 2026-06-01 1:55 am (America/Detroit)

---

## Summary

Two changes:

1. **Moved all player-facing message text out of `config.yml`** (join/leave
   broadcasts and the in-game welcome MOTD) **into the language system** under a
   new `welcome.*` section, so every player-facing string now lives in one place
   (`languages/language_en.yml` + `sprache_de.yml`). The module **toggles** stay
   in `config.yml`. Dead decorative keys (`prefix`, `banner.header/footer`) were
   removed.
2. **Added a "Toggle Command" block** to the welcome-MOTD first line's hover
   tooltip, documenting the in-game command that flips the MOTD on/off, with a
   command/admin ⚙ icon and professional formatting.

---

## Categories

### Internal — Language System
- **`MessageDefaults.java`**: new `welcome` section + 5 keys (EN + DE):
  - `welcome.join-message`, `welcome.leave-message`, `welcome.first-join-message`
  - `welcome.motd-lines` (8 lines), `welcome.motd-first-join-lines` (7 lines)
  - These carry full Adventure / MiniMessage markup (`<gradient>`, `<hover>`,
    `<click>`) and placeholders `{player} {displayname} {world} {online} {max} {uuid}`.
  - Generated/synced into the language files by the existing key-sync on startup
    and `/obx reload` (never overwrites operator edits).
- **`LanguageManager.java`**: new `rawTemplate(registry, key)` and
  `rawList(registry, key)` accessors that return the **uncolorized** template
  (no `&`-code/hex translation, no `{prefix}` injection) so MiniMessage tags reach
  `AdventureMessageUtil` intact. The legacy `send`/`get` colorizer would mangle
  `<gradient>`/`<hover>`/`<click>`, so these messages must bypass it.

### Internal — Join/Leave Service
- **`JoinLeaveService.java`**: `cacheSnapshot()` now pulls the message text from
  the language system (`welcome.*`, EN catalog) via the raw accessors instead of
  reading `join-leave.*` / `join-motd.*` strings from `config.yml`. Toggle booleans
  (`join-leave.enabled`, `suppress-vanilla`, `first-join.enabled`,
  `join-motd.enabled`, `join-motd.first-join.enabled`) still come from `config.yml`.
  Removed the now-unused `readLines()` helper. Init order is safe — `LanguageManager`
  is constructed before `JoinLeaveService` in `Main.onEnable`.
- **`ObxCommand.java`**: the partial `/obx reload config` path now also re-snapshots
  `JoinLeaveService` after `languages.reload()` (the full `/obx reload` already did,
  via `Main.reloadPlugin`), so edited welcome text refreshes without a server restart.

### Config — config.yml
- Removed the `join-leave.{join-message,leave-message,first-join.message}` strings
  and `join-motd.{lines,first-join.lines}` — leaving only the enable toggles, with
  a header pointing operators to the language keys.
- Removed dead keys **`prefix`** and **`banner.header`/`banner.footer`** (no Java
  reads them — the real prefix is `core.prefix` in the language files; the boot
  banner is built programmatically in `Main.buildBannerLines`). Kept `debug: false`.

### Feature — Welcome MOTD Toggle Hover
- The welcome-MOTD first line (`Welcome to the Server`) hover tooltip gains a new
  block under the existing customize/disable notes:
  ```
  ⚙ Toggle Command
  Flip the MOTD in-game with
  /obx joinmotd <on|off>
  ```
  - `⚙` (U+2699) command/admin icon, gold heading (`<gold>`), gray instruction,
    aqua command (`<aqua>`) — consistent with the tooltip's existing palette.
  - Localised: German variant reads `⚙ Umschalt-Befehl` / `/obx joinmotd <on|off>`.
  - The `Lines Path` / `First-Join Variant` references in the same tooltip were
    updated to the new key names (`welcome.motd-lines` /
    `welcome.motd-first-join-lines`).

---

## Behavior / Compatibility
- **Rendering unchanged**: `JoinLeaveListener` still calls
  `AdventureMessageUtil.broadcast` / `sendLines` with the same per-player
  placeholders, so gradients/hover/click render exactly as before.
- **Fresh installs**: language files generate with the `welcome.*` defaults; the
  trimmed `config.yml` ships without the message strings.
- **Existing servers**: the key-sync adds the missing `welcome.*` keys to the
  already-generated language files on next start/reload (existing values are never
  overwritten). Operators who had customised the old `config.yml` join/leave/MOTD
  text should move those edits into the `welcome.*` language keys; the old config
  keys are now ignored.

---

## Testing
- In-project Maven build completes with **no errors** (`./maven/bin/mvn -DskipTests package`); both jars produced.
- `config.yml` validated through the real SnakeYAML parser (parses clean after the cuts).
- Reflectively loaded `MessageDefaults.defaults(EN/DE)` from the built jar:
  all 5 `welcome.*` keys present (3 strings + lists of 8 and 7), and the MOTD first
  line contains the `/obx joinmotd <on|off>` Toggle Command block in **both** EN and DE.

---

## Suggested Commit Message
```
Lang: move join/leave + welcome-MOTD text from config.yml into welcome.* language keys; add /obx joinmotd toggle block to MOTD hover; drop dead prefix/banner keys
```
