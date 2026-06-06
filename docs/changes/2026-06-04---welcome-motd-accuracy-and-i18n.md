# Welcome MOTD — Tooltip Accuracy + Per-Player Language

■ **Created:** 2026-06-04 11:53 pm (America/Detroit)

■ **Last Updated:** 2026-06-05 1:58 am (America/Detroit)

## Summary
Two improvements to the in-game welcome join MOTD:
- **(a)** The self-documenting hover on the MOTD header line pointed admins to the wrong file
  (`config.yml`) for the message text — which actually lives in the language files. Corrected the
  hover across EN/DE/ES to separate "edit the text" (language file) from "toggle" (config.yml).
- **(b)** The MOTD was always rendered from the EN catalog regardless of a player's `/language`.
  It now resolves per-player language (EN/DE/ES) with an EN fallback.

## Categories

### Messages / GUI (tooltip accuracy)
- Rewrote the `welcome.motd-lines` header hover so it correctly states:
  text lives in `plugins/OBX/languages/<lang>.yml` (`welcome.motd-lines` /
  `welcome.motd-first-join-lines`), and the on/off toggle is `config.yml › join-motd.enabled`.
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`
- Added an ⓘ-prefixed "Quick Reference" footer line to `welcome.motd-lines` (above the credits),
  hovering to a themed tooltip: placeholder names, first-join behavior, per-player language,
  `/obx reload`, and formatting notes. Placeholder names are shown as a "wrap each in `{ }`" note
  rather than literal `{player}` tokens, so the join-time placeholder replacement doesn't substitute
  them in the reference. Added to all three catalogs (EN/DE/ES) for parity.
  - same three `MessageDefaults*` files
- **Bugfix:** the footer hover spilled out of the tooltip from "/language" onward because the EN
  line "player's" used an ASCII apostrophe, which closed the `<hover:show_text:'…'>` argument early.
  Replaced it with a typographic apostrophe (U+2019), which is not a delimiter and renders the same.
  Also nested the tooltip (2-space labels, 4-space values) across EN/DE/ES to match the approved
  layout.
  - same three `MessageDefaults*` files
  - Root-cause note: `LanguageManager.renderMotdNode` wraps hover/click payloads in single quotes
    without escaping; a literal `'` in any MOTD hover breaks it. A follow-up hardening (escape `'`
    in the assembler) would make ASCII apostrophes safe for future authors.

### API
- Added backwards-compatible `default` overloads `getJoinMotdLines(String languageCode)` and
  `getFirstJoinMotdLines(String languageCode)` (default to the EN no-arg result).
  - `api/src/main/java/dev/zcripted/obx/api/playerinfo/JoinLeaveService.java`

### Internal (per-player MOTD i18n)
- `JoinLeaveServiceImpl` now pre-resolves and caches MOTD lines for every language at
  load/reload/toggle (`EnumMap<LanguageRegistry, List<String>>`), keeping the join hot path a map
  read; the no-arg getters still return EN. Added `motdFor(...)` + the per-language getters.
  - `features/playerinfo/.../service/JoinLeaveServiceImpl.java`
- `JoinLeaveListener` resolves the joining player's language via
  `LanguageManager.getLanguage(uuid).code()` and requests the localized MOTD lines.
  - `features/playerinfo/.../listener/JoinLeaveListener.java`

## Testing
- `.\gradlew.bat build` → **BUILD SUCCESSFUL**; both jars produced.
- `:core:test` — `MessageDefaultsTest` (EN/DE/ES parity) and `MotdRoundTripTest` **PASS**.

## Notes
- The MOTD text files are `plugins/OBX/languages/en.yml` / `de.yml` / `es.yml`; the server-list
  ping MOTD is the separate `motd.yml` — unrelated.
- Follow-up idea (not implemented): an additional "quick reference" hover near the MOTD footer and
  permission-gated admin-only tip lines.

### Internal (root-cause hardening)
- `LanguageManager.renderMotdNode` now wraps every hover/click payload via a new
  `quoteArg(...)` that **chooses a delimiter not present in the content** (`'…'` vs `"…"`), instead
  of emitting a fixed `'…'`. A literal apostrophe/quote in any MOTD hover can no longer terminate
  the tag early. This is correct for *both* render paths — real MiniMessage and the Adventure-core
  fallback regex tokenizer (which does **not** honor `\'` escapes), so delimiter selection (not
  escaping) is the right fix. Both-quote-types content neutralizes single quotes to U+2019.
  - `core/src/main/java/dev/zcripted/obx/core/language/LanguageManager.java`
- New unit test asserts the hardened hover tag tokenizes as one complete tag against the *actual*
  Adventure-core `TAG` regex, and that the naive single-quoted form does not (codifies the bug).
  - `core/src/test/java/dev/zcripted/obx/core/language/MotdHoverQuotingTest.java`

### Messages / GUI (layout)
- Banner title changed to **"Welcome to OBX — Obsidian eXtended"** — em dash, with the subtitle
  "Obsidian eXtended" rendered non-bold (still inside the gradient). Localized welcome verb per
  language (EN "Welcome to" / DE "Willkommen bei" / ES "Bienvenido a"); brand kept identical.
- Added a blank line below the banner title.
- Moved the ⓘ MOTD-info line to **below** the "Made by zcripted" credits line, with a blank line
  between them.
  - `MessageDefaultsEN.java`, `MessageDefaultsDE.java`, `MessageDefaultsES.java`
- Wired the same ⓘ quick-reference line into the **first-join** MOTD (`welcome.motd-first-join-lines`)
  too, at the bottom with a blank line before it (EN/DE/ES).
  - `MessageDefaultsEN.java`, `MessageDefaultsDE.java`, `MessageDefaultsES.java`
- Gave the **first-join banner title** the same edit/disable hover as the returning MOTD title (it
  was previously a plain line), so the footer's "Hover the title line to edit or disable" pointer now
  works on both MOTDs and the two are structurally identical. (EN/DE/ES)
  - `MessageDefaultsEN.java`, `MessageDefaultsDE.java`, `MessageDefaultsES.java`

### Docs
- Rewrote `README.md` into a modern marketplace-grade layout (centered header + shield badges,
  "What is OBX / Why OBX", supported-platforms table, feature sections, install, commands link,
  localization, a **Developer API** consumption section, support, license). Corrected the
  localization claim from bilingual (EN/DE) to **trilingual (EN/DE/ES)**.
- README intentionally **does not expose building from source** (no Gradle/module-layout internals);
  the developer section only documents how to *consume* the public `dev.zcripted.obx.api.*` API from
  another plugin (softdepend + runtime service access), per the proprietary nature of the project.
- License section now reflects the actual **proprietary EULA** (`LICENSE`): Copyright © 2026 zcripted,
  all rights reserved, licensed-not-sold, no redistribution/reverse-engineering/derivatives.
  - `README.md`, references `LICENSE`

### Build / API (ProGuard)
- Added scoped `-keep` rules in `proguard.pro` so the public API survives obfuscation: the whole
  `dev.zcripted.obx.api.**` package (real names + original paths) and the nine API-typed getters on
  `dev.zcripted.obx.core.ObxPlugin`. Core-typed getters and all feature implementations stay obfuscated.
  - `proguard.pro`
- Verified against the obfuscated `OBX-1.0.0-beta-b1.jar`: all `api/*` classes present at real paths,
  `ObxPlugin` + all 9 getter names preserved, `EconomyService` member names preserved, and **0**
  `feature/*` classes left unobfuscated.
- **Sensitivity audit of the kept api package** (paid resource): contains only public contracts —
  13 interfaces + 2 POJOs (`Jail`, `EconomyService.BalanceEntry`), the `MAX_BALANCE` constant, a
  trivial `sanitize` clamp, and 2 trivial default methods. No secrets/keys/URLs/webhooks, no internal
  (`core`/`feature`) imports, and the only string literals are the language codes `en`/`de`/`es`.
  Tightened the keep to `{ public protected *; }` so even the POJOs' private fields stay obfuscated —
  nothing beyond the public contract is in the clear.
- **API-surface hardening (implemented + runtime-tested):** added `dev.zcripted.obx.api.OBXApi`, a
  dedicated public entry interface holding only the 9 service getters. `ObxPlugin` now
  `extends … OBXApi` (internal callers unchanged). Dropped the `-keep interface … ObxPlugin` rule and
  narrowed `-keep public class OBX` to `{ <init>; onLoad; onEnable; onDisable }`. Result: the public
  entry is `OBXApi` (in the clean api package); the `ObxPlugin` interface name is now obfuscated and
  the ~60 internal getter names (`getServiceRegistry`, `getKitService`, `getJailService`, …) are no
  longer in the clear. The 9 API getters stay preserved (OBX implements the kept `OBXApi`); Bukkit
  lifecycle overrides are preserved via ProGuard library-override detection (Bukkit is on
  `-libraryjars`).
  - `api/.../OBXApi.java` (new), `core/.../ObxPlugin.java`, `proguard.pro`, `README.md`
- **Verified on the obfuscated jar:** `OBXApi` + 9 getters kept; `ObxPlugin.class` gone; internal
  getter names obfuscated; `feature/*` still 0 unobfuscated.
- **run-paper boot test (obfuscated jar, Paper 1.21.4):** loaded *only* the obf jar (no ambiguity),
  enabled cleanly — platform detected, SQLite schema v1 opened, 100 enchants loaded, hologram backend
  selected, `Done (14.6s)`, **zero exceptions / linkage errors / obfuscation-fallback warnings**.
- Rewrote the README Developer-API section to the verified surface: `compileOnly` against the OBX jar,
  `softdepend`, and a getter→interface table covering only the 9 runtime-obtainable services (dropped
  the unbacked moderation/jail/staff claims).
  - `README.md`

### Internal (update checker → BuiltByBit download link)
- Repointed the update checker's download link from the GitHub releases page to the **BuiltByBit
  resource page** (`https://builtbybit.com/resources/obx-obsidian-extended.111131/`). Renamed
  `UpdateChecker.RELEASES_URL` → `DOWNLOAD_URL`; updated both `ObxModulesView` references; the
  `commands.obx.updates.available-link` message now names BuiltByBit (EN/DE/ES).
  - `core/.../util/update/UpdateChecker.java`, `core/.../command/ObxModulesView.java`, `MessageDefaults{EN,DE,ES}.java`
- **Version source stays GitHub release tags** — BuiltByBit cannot be polled from a distributed jar
  (API needs a private token; resource page is bot-protected/403). Verified: `zcripted/OBX` is public,
  but `/releases/latest` currently returns **404 (no release published yet)**, so the checker returns
  FAILED until a GitHub release tag (matching the BuiltByBit version) is published.

## Suggested Commit Message
```
MOTD: correct the self-documenting hover (text lives in language files) and
render the welcome MOTD in each player's /language
```
