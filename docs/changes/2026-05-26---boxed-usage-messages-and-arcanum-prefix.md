# Boxed Usage Messages + Custom Arcanum Chat Prefix

■ **Created:** 2026-05-26 6:10 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 6:10 pm (America/Detroit)

---

## Summary

Two related message-styling changes:

1. **Usage messages** for moderation and other commands now use the same boxed
   report layout as `/list` and `/sf info` (title bar + rule + indented row),
   with a clear category in the title. Moderation usages read
   `▍ SF-CORE › Moderation · Ban`.
2. **Arcanum enchantment chat messages** now carry their own
   `✦ ARCANUM ➠` wordmark instead of the shared `SF-CORE ➠` prefix, and all
   Arcanum report headers were unified to the same light-purple identity.

## Categories

### Internal — Usage message styling
- New `MessageDefaults.usageBox(category, command, usage, description)` helper
  renders a single usage in the shared boxed style (blank · title bar · rule ·
  blank · `<usage> › <description>` · blank). The math-bold `SF-CORE`, `▍`, `›`,
  and the rule are reused from shared constants (`BOX_TITLE`, `BOX_RULE`), declared
  before the static initializer so the helper is available to it.
- Converted all player-facing `*.usage*` keys to `usageBox(...)` (EN + DE):
  - **Moderation** (with a `Moderation` category header): ban, unban, kick, mute,
    unmute, tempban, warn, status.
  - Teleportation (top, delete-home), Warps (category/delete/icon/info/move/
    public/rename/set/tp), Gamemode (self, target), Utility (research),
    Players (vital, god, invsee), Language, and the Admin module toggles
    (joinleave, joinmotd).
  - `-console` variants are intentionally left as concise one-liners.
- `/sf config`, `/sf config validate`, and `/sf debug` outputs are now single
  boxed reports (`commands.sf.config.list`, `commands.sf.config.validation` with
  per-file state placeholders, `commands.sf.debug.report`). `SFCoreCommand`
  handlers updated to send one message each; the old per-line keys were removed.
  - `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/command/core/SFCoreCommand.java`

### Internal — Arcanum chat prefix
- New `enchant.prefix` message default — `&5✦ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠 &8➠ &7` (light-purple
  math-bold wordmark; codepoints verified). `LanguageManager.resolveMessages`
  now resolves `{prefix}` to `enchant.prefix` for any `enchant.*` key and to
  `core.prefix` otherwise (single central change via the new `prefixFor` helper).
  All 70+ Arcanum chat-feedback messages pick this up automatically; generic
  shared messages (`core.no-permission`, etc.) keep the SF-Core prefix.
  - `src/main/java/dev/sergeantfuzzy/sfcore/language/LanguageManager.java`
- Unified the Arcanum boxed report headers (`enchant.usage`, `enchant.list.header`,
  `enchant.info.header`) from gold to the same light-purple identity
  (`&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠`) so the module reads cohesively.

## Notes / assumptions
- All `add(key, en, de)` defaults carry both languages, so the EN + DE language
  files (generated from `MessageDefaults`) are both updated; there are no
  separate YAML resources to edit.
- Existing servers keep their already-generated `languages/*.yml` (syncDefaults
  never overwrites existing keys), so the new styling applies to fresh installs
  or to keys not already present; regenerate the language files to adopt it on an
  existing install.
- Action-bar enchant messages stay prefix-less by design (brief HUD text).

## Testing
- Maven build completes with no errors (in-project `./maven`); obfuscated +
  unobfuscated jars rebuilt.
- Arcanum prefix glyph codepoints verified on disk (math-bold A R C A N U M =
  U+1D5D4 … U+1D5E0; ✦ U+2726; ➠ U+27A0).

## Suggested Commit Message
```
Style (messages): boxed usage messages (Moderation category) + custom ✦ ARCANUM enchant prefix
```
