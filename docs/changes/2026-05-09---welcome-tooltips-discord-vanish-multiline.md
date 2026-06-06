# Welcome MOTD tooltip parser fix + discord underline + vanish multiline

■ **Created:** 2026-05-09 1:28 am

■ **Last Updated:** 2026-05-09 1:28 am

## Summary

Four follow-up adjustments after the welcome MOTD / staff-tool drop:

1. **Welcome MOTD hover tooltips and click events were rendering as raw text
   in chat** — the MiniMessage parser used a naive `indexOf('>')` to locate the
   close of an opening tag, but the new welcome / `/obx help` / discord lines
   wrap their click targets in hover tags whose args contain `>` characters
   inside single-quoted MiniMessage markup (e.g.
   `<hover:show_text:'<gold><bold>X</bold></gold>'>`). The parser stopped at
   the `>` of `<gold>`, leaked the rest into chat as literal text, and the
   click/hover bindings never attached.
2. **Discord URL no longer underlined** — removed the leading `&n` from the
   visible URL text in the welcome MOTD's discord line.
3. **`/invsee` usage line drops the `[STAFF]` badge** — usage hints are
   styled the same across OBX commands; re-flagging the staff origin in
   the usage line was redundant noise.
4. **`/vanish` toggle output now spans two lines** — the descriptive trailer
   ("You are now invisible to other players.", etc.) is its own chat line
   under the toggle line, so the action verdict reads at a glance and the
   description doesn't get crowded into the toggle row.

## Categories

### Internal — parser

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
    - **`findTagEnd(text, from)` already added in the prior change** —
      quote-aware close-bracket finder. Skips `>` chars that lie inside
      single- or double-quoted argument strings. This change confirms
      every callsite is wired through it (verified by grepping
      `indexOf\('>'` post-fix; result: zero remaining occurrences).

### Config

- `src/main/resources/config.yml`
    - `join-motd.lines[4]` (Discord line) — removed `&n` from
      `&b&nhttps://discord.gg/UxktSyT9Ag` so the URL is aqua but no
      longer underlined.

### Language strings

- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
    - `player.invsee.usage` — leading `&8[&dSTAFF&8] ` removed; it now
      starts with `{prefix}&eUsage: …` matching every other usage line in
      the plugin.
    - `player.vanish.enabled` / `…disabled` / `…enabled-other` /
      `…disabled-other` / `…enabled-target` / `…disabled-target` — each
      converted from a single-line String to a two-element
      `Arrays.asList(toggleLine, descriptionLine)`. The
      `LanguageManager.resolveMessages` path already handles list-typed
      defaults by iterating each entry through `applyPlaceholders` and
      sending each as its own message, so the language-file generator
      writes them as YAML lists and runtime lookups produce two distinct
      chat lines per toggle.

## Files Modified

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- `src/main/resources/config.yml`

## Suggested Commit Message

```
Fix (welcome+staff): hover parse, discord underline, vanish multiline
```
