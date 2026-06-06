# Plugin Box Back-Link, Server-Control Lore Restyle, Clear-Inv Hover Fix, Spanish Language Command & Gamerule Name i18n

■ **Created:** 2026-06-03 10:11 AM (America/Detroit)

■ **Last Updated:** 2026-06-03 10:11 AM (America/Detroit)

Five changes. `./gradlew build` is **green**; both jars produced
(`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`); EN/DE/ES message-parity and
`LanguageRegistry` tests pass.

## GUIs / Commands

- **Plugin info box → back to list.** In the `/pl` → plugin info box, the **"Plugin"**
  category word in the header is now a clickable back-link: hovering shows a hint and
  clicking re-runs `/pl` to return to the full list. The title is rendered as
  plain-text + one interactive component (sentinel-split around a new `{category}`
  placeholder); console recipients get the word inline.
  - `commands.pl.info.title` now uses `{category}`; new keys `commands.pl.info.category`
    and `commands.pl.info.back-hover` (EN/DE/ES).
- **Server-Control GUI lore restyle.** All item lore in the **Server Control**, **Server
  State**, **Player Access**, **Performance + Health**, **World Controls**, and
  **Plugin + Systems** sub-menus now inherit the clean, categorized style of the Server
  Control item in the main Admin menu (`admin.scp.*`):
  - gray description line, a `&d&l▸ &dStatus` category accent for status-rich tiles, and
    `&8• &7Label &8» {value}` rows (the `»` separator + bullet motif) instead of the old
    `Label: value` lines. Placeholders are unchanged, so live values still flow through.

## Fixes

- **Clear-inventory hover scope.** `/clearinv`'s summary previously applied the
  breakdown hover to the **entire** message line. It now scopes the hover to **only the
  `(N item(s))`** segment: the message embeds a `{countpart}` placeholder, which is
  rendered into a single hover-bearing component while the rest of the line stays plain
  (`ClearInvCommand.sendWithHover`). New key `inventory.clearinv.count` (EN/DE/ES); the
  `.self`/`.other` messages now use `{countpart}`. Console falls back to an inline count.

## Localization

- **Spanish language command.** Spanish now appears in the language command and has its
  own command, mirroring German's `/sprache`:
  - New `/idioma` command (bound in `OBX.java`, declared in `plugin.yml`, mapped in the
    `/help` GUI). `obx.language` gates it.
  - `LanguageCommand` tab-complete OPTIONS now include Spanish; `LanguageRegistry.ES`
    display name fixed to **"Español"** with added input aliases (`español`, `spanisch`,
    `idioma`, plus `inglés`/`alemán` for EN/DE).
  - `language.invalid` and `language.usage` (EN/DE/ES) now list Spanish; the ES usage
    references `/idioma`. `plugin.yml` usages for `/language` and `/sprache` updated to
    include `Spanish|ES`.
- **Game-rule display names (DE/ES).** The 40 individual game-rule names — previously
  kept in canonical English — are now **translated for German and Spanish**. Added
  `admin.gui.gamerule.name.<rule>` keys (40 × EN/DE/ES); `GameruleEntry.nameKey()` feeds
  the localized name into the editor item, the enabled-rules preview, and the toggle chat
  message. The English `display` field is retained internally for stable A–Z layout
  ordering.

## Files

- `core/src/main/java/dev/zcripted/obx/core/command/PluginListCommand.java`
  — `sendInfoTitle(...)` interactive back-link.
- `features/item/src/main/java/dev/zcripted/obx/feature/item/command/ClearInvCommand.java`
  — hover scoped to the count segment.
- `core/src/main/java/dev/zcripted/obx/core/language/LanguageCommand.java`
  / `LanguageRegistry.java` — Spanish options, aliases, display name.
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — `bind("idioma", ...)`.
- `plugin/src/main/resources/plugin.yml` — `idioma` command + updated usages.
- `core/src/main/java/dev/zcripted/obx/core/gui/help/HelpGuiMenu.java` — `idioma` → Language category.
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`
  — `GameruleEntry.nameKey()` wired into the item, preview, and toggle message.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` / `DE.java` /
  `ES.java` — pl info category/back-hover, clearinv count, restyled `admin.gui.*` lores,
  40 `admin.gui.gamerule.name.*` keys, language usage/invalid updates.
- `docs/information/COMMANDS+PERMISSIONS.md` — `/idioma` row + Spanish in usages.

## Suggested Commit Message

```
Feature (GUI/i18n): plugin-box "Plugin" back-link to /pl, restyle Server Control sub-menu lore to the scp categorized style, scope /clearinv hover to the (N item(s)) segment, add Spanish /idioma command + surface ES in the language command, and translate the 40 gamerule display names for DE/ES
```
