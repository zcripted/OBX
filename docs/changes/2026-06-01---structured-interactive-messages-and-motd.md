# Structured Interactive Messages + Branched MOTD Tooltips in Language YAML

■ **Created:** 2026-06-01 3:36 am (America/Detroit)

■ **Last Updated:** 2026-06-01 3:36 am (America/Detroit)

---

## Goal

Make hover tooltips, click actions (run / suggest / open-url / copy), and message
formatting **editable from the language YAML on their own indented sub-sections**,
instead of being hardcoded in Java or buried in long inline `<hover>`/`<click>`
strings. Hover tooltips now branch *beneath* the message they belong to.

---

## Format

An interactive message becomes a structured node:

```yaml
info.info.action.seen:
  text: "&d[seen]"
  hover:
    - "&7Run &f/seen {target}"
  click:
    action: run_command          # run_command | suggest_command | open_url | copy_to_clipboard
    value: "/seen {target}"
```

The welcome MOTD is a **mixed line list** — plain lines stay bare strings; lines
with a whole-line hover/click become branched nodes:

```yaml
welcome.motd-lines:
  - " "
  - text: "          ...Welcome to the Server..."
    hover:
      - "<dark_purple><bold>Welcome MOTD</bold></dark_purple>"
      - "..."
      - "<gold>⚙ Toggle Command</gold>"
      - "<aqua>/obx joinmotd <on|off></aqua>"
  - "&8• &eType &d/obx help&e to view available commands.":   # (shown as node)
    text: "&8• &eType &d/obx help&e to view available commands."
    hover: [ "..." ]
    click: { action: run_command, value: "/obx help" }
  - "<inline-tags multi-link line>"   # kept inline (3 links on one line)
```

**Implementation principle:** each structured piece is stored as an ordinary
flat leaf (`base.text` String, `base.hover` List, `base.click.action` /
`base.click.value` String). That keeps the whole thing fully compatible with the
existing key-sync (which only adds missing leaves and never overwrites operator
edits). YAML renders the nesting as indented sections automatically; the code
reassembles a node into a component / inline line at send time.

---

## Categories

### API — MessageDefaults
- `addInteractive(base, enText, deText, enHover, deHover, clickAction, clickValue)`
  (+ hover-only overload) registers the `.text` / `.hover` / `.click.action` /
  `.click.value` leaves. Click action/value are shared across locales (commands
  /URLs don't translate); text + hover translate normally.
- `addStructuredList(key, en, de)` + `motdLine(...)` builders register the MOTD as
  a mixed `List<Object>` of plain strings and `{text,hover,click}` `LinkedHashMap`
  nodes (deep-copied so the YAML writer emits indented sub-sections).

### API — LanguageManager
- `getInteractivePart(sender, base, replacements)` reads a structured node,
  applies placeholders + the resolved `{prefix}`, and returns a
  `ComponentMessenger.InteractiveMessagePart` (text + hover + click). Click
  payloads get placeholder substitution **without** `&`-colorizing (commands/URLs
  stay literal). Maps `run_command`/`suggest_command`/`open_url`/`copy_to_clipboard`.
- `sendInteractive(sender, base[, replacements])` builds + dispatches one node.
- `resolveMotdLines(registry, key)` + `rawStructuredList` + `renderMotdNode`
  reassemble each structured MOTD node back into the inline
  `<hover:show_text:'…'><click:action:'…'>text</click></hover>` form that
  `AdventureMessageUtil` already renders — so the YAML is clean while on-wire
  output is identical.

### Internal — LanguageFile
- `readRawList(key)` reads a list via `getList` (not the String-coercing
  `getStringList`) so the structured MOTD maps survive the read intact.

### Internal — ComponentMessenger
- Added `InteractiveMessagePart.openUrl(...)` (joins the existing `plain` /
  `interactive` / `copy` factories); the bungee join path already honours the
  explicit `OPEN_URL` action name.

### Converted message keys
- **/info action buttons** → `info.info.action.{seen,playtime,whois,tpa}` structured
  nodes ({target} fills the click command + hover). `InfoCommand` now builds the
  row via `getInteractivePart`.
- **/tpa Accept / Deny buttons** → `teleport.request.{accept,deny}` structured nodes
  ({requester} fills the click command). `TeleportRequestService` uses
  `getInteractivePart`. (Replaces the old `accept-button`/`accept-hover`/
  `deny-button`/`deny-hover` flat keys.)
- **Welcome MOTD** (`welcome.motd-lines`, `welcome.motd-first-join-lines`) →
  structured mixed-line lists. Whole-line-hover lines (Welcome banner, Hello,
  Help, Discord) are branched nodes; the multi-link "Made by … [GitHub] [Spigot]
  [BuiltByBit]" line (3 independent hover/click segments on one line — not
  representable as a single node) intentionally stays an inline-tag string. The
  ⚙ Toggle Command block is now its own hover sub-list.

### Not converted (intentional)
- Mid-sentence single-click prompts like `/spawn delete` confirm
  (`SpawnCommand.sendClickable`) already split message text and hover into separate
  language keys (the only Java-side bit is an internal click token, not
  operator-facing), so they already satisfy "hover on its own section."
- Plain one-line messages (the ~980 non-interactive keys) stay simple scalars —
  they have no hover to branch.

---

## Compatibility
- **Key-sync unchanged**: structured leaves are ordinary keys, so existing files
  gain the new keys on next start/reload without overwriting operator edits.
- **Existing servers**: the old `info.info.actions-*` / `*-button` / `*-hover`
  flat keys are no longer read; operators who customised them should re-apply the
  edits to the new structured keys (old keys are now inert).
- **Rendering identical**: info/tpa rows still go through `ComponentMessenger`;
  MOTD nodes reassemble to the same inline form `AdventureMessageUtil` rendered
  before.

---

## Testing
- In-project Maven build completes with **no errors** (`./maven/bin/mvn -DskipTests package`); both jars produced.
- snakeyaml round-trip verified: a mixed list of plain strings + `{text,hover,click}`
  maps serialises to indented YAML and reads back with order, hover list, and
  nested click map intact.
- Reflectively loaded `MessageDefaults.defaults(EN/DE)` from the built jar:
  interactive keys decompose into `.text`/`.hover`/`.click.action`/`.click.value`
  (DE text translated, click values shared); `welcome.motd-lines` is an 8-element
  mixed list (plain strings + nodes with hover lists + click maps).
- Unit-checked `renderMotdNode`: a structured node reassembles to exactly
  `<hover:show_text:'…\n…'><click:run_command:'/obx help'>text</click></hover>`
  and a plain node passes through unchanged.
- Confirmed no source references the removed flat keys.

---

## Suggested Commit Message
```
Lang: structured interactive messages (text/hover/click as indented YAML sub-sections) + branched MOTD tooltips; convert /info + /tpa buttons and welcome MOTD
```
