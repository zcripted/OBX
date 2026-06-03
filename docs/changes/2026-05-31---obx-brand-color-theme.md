# OBX Brand Color Theme — Plugin-Wide Recolor

■ **Created:** 2026-05-31 4:21 pm (America/Detroit)

■ **Last Updated:** 2026-05-31 9:52 pm (America/Detroit)

---

## Summary

Shifts the overall in-game **and** console message theme from the legacy
gold/aqua accent palette to the **OBX (Obsidian eXtended)** brand identity —
the obsidian → violet → magenta ramp:

```
#0A0A0F  →  #2A0A45  →  #6A1B9A
obsidian     deep violet   magenta fleck
```

The decorative accent colors that carried the old warm identity (gold, aqua)
are remapped to the OBX violet family. Functional **status** colors
(green = success, red = error, amber/yellow = warning/neutral) and structural
colors (gray, dark-gray, white) are left intact for readability, and the
enchant module's **semantic rarity colors** are deliberately preserved.

This change is applied plugin-wide across Java sources, resource configs, and
the console/boot output.

---

## Design Decisions / Assumptions

Per the rebrand brief (palette in `docs/information/Renaming.txt`) and the two
scoping choices confirmed for this task:

1. **Scope = "everything except status".** All decorative/neutral accents are
   recolored to the violet ramp; the red/green/amber status cue palette is kept.
   Amber (`&e` / yellow) is treated as the kept "amber status/neutral" color, so
   message body text stays readable rather than turning monochrome purple.
2. **Readability = "decorative-only dark end, brighten text".** The near-black
   `#0A0A0F` is used **only** as the dark end of decorative gradients/banners,
   never as standalone body text. The readable on-brand accent for actual text
   is `#6A1B9A` (and the legacy light-purple `&d`), which renders clearly on
   both a dark in-game chat background and a dark console.

**Color mapping applied:**

| Role (old) | Old code | New code | Notes |
|---|---|---|---|
| Primary accent (gold) | `&6` / `§6` / `ChatColor.GOLD` | `&5` / `§5` / `ChatColor.DARK_PURPLE` | deep brand violet for wordmarks, bars, dividers, key values |
| Secondary accent (aqua) | `&b` / `§b` / `ChatColor.AQUA` | `&d` / `§d` / `ChatColor.LIGHT_PURPLE` | bright readable lavender |
| Cool accent (dark aqua) | `&3` / `ChatColor.DARK_AQUA` | `&5` / `ChatColor.DARK_PURPLE` | folded into deep violet |
| Named tag (gold) | `<gold>` | `<dark_purple>` | MiniMessage hover / MOTD headers |
| Named tag (aqua) | `<aqua>` | `<light_purple>` | MiniMessage hover / MOTD headers |
| Named tag (dark aqua) | `<dark_aqua>` | `<dark_purple>` | MiniMessage hover / MOTD headers |
| System wordmark hex | `#FF4526` | `#6A1B9A` | brand violet |
| Inbox wordmark hex | `#FFD93D` | `#6A1B9A` | brand violet |
| Warp GUI title gradient | `#FFD54A` → `#FFB300` | `#A855F7` → `#6A1B9A` | bright → brand violet |
| Chat separator hex | `#FFFF55` | `#6A1B9A` | brand violet |
| MOTD/config gold gradient | `#FFAA00` | `#6A1B9A` | brand violet |
| Tablist gold / cyan accent | `#FFAA00` / `#55FFFF` | `#6A1B9A` | brand violet |

**Kept unchanged (functional / structural / semantic):**

- Status: `&a`/`&2`/green, `&c`/`&4`/red, `&e`/yellow (amber), and the
  green/red gradient examples in `config.yml`.
- Structure: `&7` gray, `&8` dark-gray, `&f` white.
- **Enchant module rarity & category colors** (`enchant/model/EnchantRarity.java`:
  COMMON `WHITE`, UNCOMMON `GREEN`, RARE `BLUE`, EPIC `DARK_PURPLE`,
  LEGENDARY `GOLD`, MYTHIC `RED`; `enchant/model/EnchantCategory.java`:
  COMBAT `RED`, DEFENSE `BLUE`, TOOLS `YELLOW`, FARMING `GREEN`,
  UTILITY `AQUA`, MYSTIC `DARK_PURPLE`, CURSED `DARK_RED`). These are
  categorical (status-like) `ChatColor` values; the entire `enchant/**` Java
  tree and `resources/enchants/**` were excluded from the remap so rarity and
  category tiers do not collapse into one color. The Arcanum module
  already carries its own on-brand violet wordmark (`&5✦ &d ARCANUM`).

**Already on-brand before this change (verified, untouched):**
`ConsoleLog` prefix (`&5&l[OBX]`, subsystem `&d`), the boot banner gradient
(`#2A0A45 → #6A1B9A`), `motd.yml`, and `systems/scoreboard.yml`.

---

## Platform Readability

- **In-game (1.16+):** hex/gradients render true brand violet via the Adventure
  direct-build / legacy-hex paths in
  `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`.
- **In-game (pre-1.16):** custom hex downsamples to the nearest legacy color;
  the legacy violet codes (`&5`/`&d`) used for the bulk remap render natively.
- **Console:** OBX's themed console output (`ConsoleLog`, boot banner) maps the
  legacy violet codes / hex to ANSI magenta/truecolor; all other messages routed
  to the console are color-stripped to plain readable text by
  `AdventureMessageUtil` (`stripFormatting`), so console output is always legible.

---

## Incident: `#` → `F` corruption in two YAML files (found & fixed)

During verification, `src/main/resources/config.yml` and
`systems/chat_management.yml` were found with a `#` → `F` corruption introduced
mid-session by a faulty bulk replacement: YAML comment markers (`# ...` → `F ...`)
and hex prefixes (`<#2A0A45>` → `<F2A0A45>`, `</#FFFF55>` → `</FFFFF55>`,
comment examples `(&e / #FFFF55)` → `(&e / FFFFF55)`). This broke YAML structure
and every affected color tag.

`ChatService.java` had the same defect on its template-default string literals.

**Resolution:** diffed both files against `HEAD` (which was clean) to separate the
legitimate, pre-existing SF-Core → OBX branding changes from the corruption, then
reversed the `#` → `F` substitution in every affected context (opening tags,
closing tags, `<gradient:>`/`<shadow:>` values, comment markers, and `RRGGBB`
placeholder examples) while preserving all branding and color-theme edits.
`ChatService.java` was rewritten cleanly. Post-fix sweep: **0** corruption
signatures remain anywhere in the tree; both files retain valid YAML and correct
`#RRGGBB` hex.

**Follow-up (server-load failure):** a first restore pass missed `config.yml`
lines 14–15 and 135–136, which were originally *blank* `#` comment lines. Because
the file uses CRLF endings, the corrupted bare `F` was followed by `\r` (not a
space or end-of-line), so the context-anchored regex skipped it — leaving four
lines that read just `F`. A bare `F` line is invalid YAML, so the live server
threw `expected '<document start>', but found '<block mapping start>'` at line 16
and `config.yml` failed to load. Fixed by restoring any whole line that is only
optional-whitespace + `F` back to `#`. Verified by parsing **every** bundled
`.yml` (22 files) through the real SnakeYAML parser Bukkit uses — all parse
cleanly (`INVALID_YAML_COUNT=0`).

---

## Console Output Fixes

Two console issues from the live boot log were also resolved:

1. **Doubled `[OBX] [OBX]` prefix.** Eleven storage/economy/service classes
   logged through `plugin.getLogger()` with a *literal* `"[OBX] "` baked into the
   message — but Bukkit's `PluginLogger` already prepends `[OBX]`, producing
   `[OBX] [OBX] …`. Removed the redundant literal from all 33 call sites across:
   `economy/EconomyService.java`, `economy/VaultEconomyProvider.java`,
   `kit/KitService.java`, `messaging/MessageService.java`,
   `moderation/ModerationService.java`, `nickname/NicknameService.java`,
   `storage/DataService.java`, `storage/SqliteDataStore.java`,
   `util/control/FlightStateService.java`, `util/control/PerPlayerTimeService.java`,
   `util/perf/PlaytimeService.java`. The framework now renders `[OBX]` exactly once.
2. **Console prefix recolored to light purple.** `ConsoleLog`'s `BRAND` (was gold
   `&6`) and `TAGGED` subsystem tag (was aqua `&b`) are now both light purple
   (`&d`), matching the OBX brand. `src/main/java/dev/zcripted/obx/util/message/ConsoleLog.java`.

---

## Scoreboard Title Showed Only "O"

The sidebar title was `&#2A0A45&lO&#4A126F&lB&#6A1B9A&lX` — a 3-color per-letter
gradient. `ScoreboardRenderer.colorize()` expands every `&#RRGGBB` into the
14-char legacy hex sequence `§x§R§R§G§G§B§B`, so the title rendered to **51
characters**. `ensureObjective()` then applies `truncate(title, 32)` — the hard
scoreboard **objective display-name limit** (32 chars including color codes,
enforced on 1.8–1.12 and respected here for cross-version safety). The cut landed
inside the first color sequence, leaving only the colored **"O"** visible. The
dark `#2A0A45` start was also nearly invisible against the dark sidebar.

**Fix:** a 2-stop purple gradient that renders to **31 chars** (fits the 32-char
cap on every supported version 1.12 → 1.21) using viewable brand violets:

```
title: "&#6A1B9AO&#A855F7BX"
```

- `O` → brand violet `#6A1B9A`; `B` + `X` → bright violet `#A855F7`.
- Verified by simulating `colorize()` + `truncate(32)`: rendered length 31,
  full visible text `OBX`, both color stops intact.
- A full 3-distinct-color per-letter gradient cannot fit (3 × 14 = 42 > 32) while
  remaining safe on pre-1.13 servers, so a 2-stop ramp is the widest universal
  option. The YAML comment documents the 32-char budget for anyone customizing.
- `src/main/resources/systems/scoreboard.yml`

---

## Leftover SF-CORE Wordmark in Message Prefixes

A deep search (plain `SF-CORE`/`SF_CORE`/`SFCORE` **and** the styled math-bold
unicode wordmark, both as literal glyphs and `\uXXXX` escapes) found two message
prefixes in `MessageDefaults.java` still carrying the old **𝗦𝗙-𝗖𝗢𝗥𝗘** wordmark:

- `core.prefix` (line 101) — the default prefix on most OBX messages.
- `admin.plugin.reloaded-console` (line 1067).

Both stored the wordmark as `\uXXXX` escapes. Replaced **𝗦𝗙-𝗖𝗢𝗥𝗘** with **𝗢𝗕𝗫**
in the *identical* math-bold sans-serif style (`𝗢𝗕𝗫`),
preserving the surrounding `&5` violet color, `&8➠` separator, and trailing color.

Not changed (intentional): the `SF_CORE_*` identifiers in
`gui/player/HelpGuiMenu.java` are internal variable/constant **names** only —
their values already resolve to `"OBX"` and the plugin command package — so they
carry no user-facing text.

- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

---

## Scoreboard Website, Health-% Alignment & `/list` Redesign

**Website.** Sidebar `{website}` now shows `obx.zcripted.dev`.
- `src/main/resources/systems/scoreboard.yml` (`server-website`)
- `src/main/java/dev/zcripted/obx/scoreboard/service/ScoreboardService.java` (default fallback)

**Health-percent alignment.** The `&c{health_percent}%` line had no indent, so the
percentage left-aligned at column 0 instead of under the heart bar. The bar is
always 10 icons (filled + empty), so its first heart sits at a constant position
regardless of health level, so a constant indent aligns it for every value (7%,
93%, 100%) and any fill level. Minecraft's font is proportional, so the indent is
tuned in **pixels**, not characters: the `"Health » "` label is ~46px wide and a
space is ~4px, so an 11-space (~44px) indent puts the percentage's **first digit**
under the **first heart**. A YAML comment documents how to nudge it.
- `src/main/resources/systems/scoreboard.yml`

**`/list` redesign.** Rebuilt the player-list output as a boxed report matching
`/pl` and `/obx about`: a strikethrough top bar with the `&5▍ &5𝗢𝗕𝗫 &8› &fPlayers`
brand header, an `Online » count/max` summary line, the colored name list, and a
strikethrough bottom bar. **OP/staff names render red (`&c`), regular players
yellow (`&e`)** — matching the existing tablist/scoreboard name-team convention
(both key on `isOp()`). Names are sorted by visible name (color prefix stripped)
and joined with `&7, `; AFK/vanish suffixes are preserved.
- `src/main/java/dev/zcripted/obx/command/info/ListCommand.java` (color-by-op, boxed send sequence, sort fix)
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java` (`info.list.header/summary/entries/no-players/footer`)

**Completed the purple rebrand.** The boxed-report headers (`/pl`, `/obx about`,
`/obx info`, `/whois`, usage boxes, etc.) still carried the old gold `&6▍ &6𝗢𝗕𝗫`
wordmark — 16 stragglers the original plugin-wide rebrand missed. All converted to
brand violet `&5`, so the new `/list` header matches the rest of the report family.
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java` (16 × `&6` → `&5`)

---

## Categories

### Config / Resources
- `src/main/resources/config.yml` — MOTD gold gradient → brand violet (`#FFAA00` → `#6A1B9A`); legacy gold/aqua codes remapped.
- `src/main/resources/systems/tablist.yml` — gold/cyan accents → brand violet.
- `src/main/resources/systems/chat_management.yml` — chat separator `#FFFF55` → `#6A1B9A`.
- Other top-level resource YMLs scanned and remapped where decorative gold/aqua codes were present.

### Internal — Messages
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java` — all gold/aqua legacy accents → violet; system (`#FF4526`) and inbox (`#FFD93D`) wordmark hex → `#6A1B9A`.

### GUIs / Commands / Listeners (non-enchant)
- 92 files total updated by the deterministic remap across
  `command/**`, `gui/**` (incl. `gui/shared/WarpMenuStyling.java` gradient),
  `chat/**` (incl. `chat/service/ChatService.java` separator default),
  `listener/**`, `hub/**`, `hologram/**`, `moderation/**`, `util/**`, and `Main.java`.

### Excluded (intentional)
- `src/main/java/dev/zcripted/obx/enchant/**` and `src/main/resources/enchants/**` — semantic rarity/category color systems preserved.

---

## Language Files (`language_en.yml` / `sprache_de.yml`)

These files are **generated at runtime** from `MessageDefaults` and are not
committed to the repo, so there is no in-repo file to edit — the new defaults
flow from the updated `MessageDefaults.java`.

**Deployment note:** on an existing server, the previously-generated
`language_en.yml` / `sprache_de.yml` are operator-owned and keep their current
(gold) color values; the self-heal adds missing *keys* but does not overwrite
existing *values*. To pick up the new theme on an existing install, regenerate
those files (delete + `/obx reload`, or reset the affected entries). Fresh
installs get the violet theme automatically.

---

## Testing
- In-project Maven build completes with **no errors** (`./maven/bin/mvn -DskipTests package`).
- Both obfuscated and unobfuscated jars build successfully.
- Verified post-remap: 0 gold/aqua legacy codes, `ChatColor.GOLD/AQUA/DARK_AQUA`
  enums, or named `<gold>`/`<aqua>`/`<dark_aqua>` tags remain in non-enchant Java
  or resources; enchant rarity/category colors intact; off-brand decorative hex
  removed from Java and the targeted resource files.
- Build re-run clean after the named-tag pass; both jars regenerated.

---

## Suggested Commit Message
```
Theme (brand): Recolor in-game + console messages to OBX violet palette (keep status & enchant rarity colors)
```
