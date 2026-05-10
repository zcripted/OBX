# TASK: Add Multi-Language System (EN + DE) with Smart Key Sync

## Goal
Implement a robust language/message system for the plugin.

### Files
Create a `/languages/` directory under the plugin data folder.
- English file: `languages/language_en.yml`
- German file: `languages/sprache_de.yml`

### Creation rules
- On first startup: if `/languages/` or either language file is missing, create it.
- If the directory/files already exist: DO NOT recreate or overwrite them.

---

## Smart key sync (critical)
On:
- startup (onEnable) AND
- `/sfcore reload`

Run a "message key sync" that:
- Loads both YAML files.
- Ensures **every required message key exists** in each file.
- If a key is missing in a file, write it into that file with the default value:
- English file gets the English default string.
- German file gets the German translation string.
- Never delete existing keys.
- Never overwrite existing values.
- Preserve comments if possible (YAML libraries vary; if comments can’t be preserved, still keep the section divider approach by re-writing only missing keys without rewriting the entire file).

NOTE: GUI menu text is excluded from this system (do not store GUI strings in language files).

---

## Language selection commands
Add commands:
- English command: `/language [English|EN|German|DE]`
- German alias: `/sprache [Englisch|EN|Deutsch|DE]`

Behavior:
- Selecting a language sets the player’s preferred language (store by UUID).
- After selection, refresh message provider so future messages sent to that player use the selected file.
- Default language: English (language_en.yml).
- For console messages, always use English unless explicitly configured otherwise.

Persistence:
- Store player language preference in a simple config file, e.g. `player-languages.yml` or `data.yml` with UUID -> lang code (`en` or `de`).

Permissions:
- Player commands should be available to everyone by default (unless project has a central permissions model—match existing conventions).

---

## Message structure + organization
All messages (except GUI menus) must be moved into the language files.

### Organization rules
- Use parent keys by category, nested keys by feature:
  Example:
  economy:
  balance:
  self: "..."
- Use bash-style comments and divider lines to separate sections in each YAML:
  Example:

# ───────────────────────────────────────────────────────
#
#     Economy ─ Category description here with basic understanding of what this section is for...
#
# ───────────────────────────────────────────────────────

economy:
...
- Categories must be organized **alphabetically** by category and category key.
- Within a category, organize subkeys alphabetically (where reasonable!).

### Console variant rule (required)
Any message keys that have console variants must store the console variant **right next to** the original variant in YAML.

Use this structure (console key adjacent to player key):
- If the base key is a single message:
- `message: "..."`
- `console-version: "..."` (directly below it)
- If the base key is player-specific:
- `self: "..."`
- `self_console: "..."` (directly below `self`)
- `other: "..."`
- `other_console: "..."` (directly below `other`)
  OR use a nested structure if preferred, but keep adjacency:
- `self: "..."`
- `console_self: "..."` (adjacent)
  Choose ONE naming convention and apply consistently across the entire language system.

Do NOT scatter console variants elsewhere in the file; they must be immediately adjacent to their non-console counterpart for easy editing.

### Placeholder rules
Use consistent placeholders across languages, e.g.:
- `{player}`
- `{amount}`
- `{prefix}`
- `{value}`
  Do not use inconsistent placeholder names between EN and DE.

Color:
- Keep existing color format used by project (MiniMessage or legacy). Do not mix formats within the same system.
- If project uses MiniMessage, store MiniMessage strings in YAML.

---

## Implementation details (Java / Bukkit)
### New components to add
1) `LanguageManager`
- Responsible for:
- Ensuring `/languages/` exists
- Creating missing files
- Loading YAML
- Key sync on startup/reload
- Resolving player language
- Providing `msg(player, key, placeholders...)`

2) `LanguageFile` abstraction (optional but preferred)
- Wraps a YAML config for a single language
- Has:
- `getString(key)`
- `ensureDefaults(Map<String,String> defaults)` that adds missing keys only

3) `LanguageRegistry`
- Enum or record:
- `EN` -> filename `language_en.yml` -> display `English`
- `DE` -> filename `sprache_de.yml` -> display `Deutsch`
- Accepts command inputs:
- EN: `English`
- DE: `German`
- For /sprache:
- `Englisch` -> EN
- `Deutsch` -> DE

### Message usage
Replace direct hardcoded message strings with:
- `lang.msg(player, "category.subkey.key", placeholders)`
  For console use:
- `lang.msgConsole("category.subkey.key", placeholders)` defaults to EN.
  If a console-specific variant exists, `msgConsole` must prefer it; otherwise fall back to the normal key.

### Reload
Ensure `/sfcore reload` triggers:
- language files reload
- key sync
- message provider refresh

---

## Defaults + Translations (required)
Create a central list of message keys with default EN + DE strings.
- Store these in code as a map of `key -> (en,de)`.
- On sync, add missing keys to the correct file with correct language string.

Translate FULL English -> German for `sprache_de.yml`.
- Use natural German (not literal word-for-word).
- Keep placeholders unchanged.
- Keep tone consistent with EN.

Console variants:
- Provide EN + DE values for console variants too.
- Keep console variants immediately adjacent to their non-console counterparts in the YAML structure.

---

## Deliverables
- New `/languages/` folder creation logic
- Auto-create missing `language_en.yml` and `sprache_de.yml`
- Key sync on enable + reload
- `/language` and `/sprache` commands for player language selection
- Persistent per-player language storage
- All non-GUI plugin messages moved to language system
- Alphabetically organized categories + divider comments in YAML
- Console variants stored adjacent to base messages where applicable

---

## Safety
- Never overwrite existing language values unless directed.
- Never delete keys.
- Don’t break existing command permissions; integrate with current command framework.
- Add minimal logging: report when files are created and how many keys were added during sync.