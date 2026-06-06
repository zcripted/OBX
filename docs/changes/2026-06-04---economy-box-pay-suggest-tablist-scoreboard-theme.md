# Economy box-style + pay suggestions, tablist/scoreboard purple theme, scoreboard fixes

■ **Created:** 2026-06-04 3:26 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 3:26 pm (America/Detroit)

Banner release-date fallback, box-style `/bal` `/bal <target>` `/baltop`, click-to-suggest
"did you mean?" guarding on `/pay`, a brand-violet live tablist (uptime/tps/ping), bold true-purple
scoreboard + tablist headers, scoreboard IP color + per-field icons, and a pixel-accurate
auto-fitting scoreboard divider. Build green; both jars; EN/DE/ES parity passes.

---

## Commands

- **`/bal` and `/bal <target>` are now box-style.** `economy.balance.self` / `economy.balance.other`
  changed from one-liners to framed boxes (header bar, divider, themed `Label › Value` rows).
  Files: `BalanceCommand.java` (unchanged logic), `economy.balance.*` in all three `MessageDefaults`.
- **`/baltop` is now box-style.** `economy.baltop.header` opens the box (title + divider),
  `economy.baltop.entry` is a themed `#. player › amount` row, `economy.baltop.footer` closes it
  with the page indicator. Files: `BalTopCommand.java` (unchanged logic), `economy.baltop.*`.
- **`/pay` similar-name guarding (new).** When the typed target can't be resolved to a real account,
  `/pay` now shows a **box-style, click-to-suggest list** of similar/known names instead of only the
  flat "unknown player" line. A candidate matches when its name shares the typed **prefix** or any run
  of **≥3 consecutive characters** with what was typed; candidates are drawn from online players plus
  every known economy account (`EconomyService.topBalances`), de-duped, ranked (prefix → shorter →
  A–Z), and capped at 8. Clicking a name **suggests `/pay <name> `** so the payer just types the
  amount and sends. Falls back to the plain `economy.unknown-player` message when nothing looks close.
  Files: `PayCommand.java`, new `economy.pay.suggest.*` keys (`header`, `entry.text`, `entry.hover`,
  `entry.click.action`, `entry.click.value`, `footer`).
- **`/pay` tab-completion** already suggested online player names (and excludes self); confirmed it
  also lists every online name **before** any characters are typed (empty-prefix match). No change
  needed beyond verification.

## Console / Startup

- **Banner release date.** The `onEnable`/`onDisable` banner's `Version: … (Released …)` line now
  falls back to **today's date** (formatted `MMM d, yyyy`) when the build carries no stamped
  `releaseDate`, instead of printing `Unknown`. File: `plugin/.../OBX.java` (`buildBannerLines`).

## Tablist

- **Brand color theme + live values.** Header/footer recolored to the OBX violet theme (bold
  `#9D4EDD→#C77DFF` gradient title, gray labels, bright-violet `#C77DFF` values; the old near-black
  `#2A0A45` start is gone). Added a live **`{uptime}`** placeholder (server uptime since JVM start,
  `Dd HH:mm:ss`, drops the day segment under 24h) sourced from the runtime MX bean (ms resolution).
  **Refresh lowered 40 → 2 ticks (~100 ms)** so uptime seconds, TPS and ping tick in real time.
  Files: `tablist.yml`, `TablistRenderer.java` (`formatUptime`/`two`, `{uptime}` placeholder).

## Scoreboard

- **Bold true-purple gradient title.** The objective title is now set via the Adventure
  **`displayName(Component)`** API (no 32-char legacy cap), so `scoreboard.yml`'s
  `<gradient:#9D4EDD:#C77DFF><bold>OBX</bold></gradient>` renders as a real bold per-letter gradient;
  servers without the component API fall back to a truncated legacy string. Added
  `AdventureMessageUtil.toComponent(raw)` / `adventureComponentClass()`. Files:
  `AdventureMessageUtil.java`, `ScoreboardRenderer.java` (`ensureObjective`/`applyComponentTitle`),
  `scoreboard.yml`.
- **IP color matches Web.** The `IP` value changed from yellow (`&e`) to the same purple (`&d`) as the
  `Web` value. File: `scoreboard.yml`.
- **Per-field icons.** Each field carries a small accent-colored icon to its left:
  `☻ Player · ❤ Health · ⚐ Online · ✦ Bank · ⚡ IP · ⌂ Web`. The health-percent indent was retuned
  (`11 → 14` spaces) to stay aligned under the first heart now that the row carries a `❤ ` prefix.
  File: `scoreboard.yml`.

## Fixes

- **Scoreboard divider auto-fit (pixel-accurate).** The `{divider}` auto-fit measured **character
  count** but built **N spaces** — far too short for letter-heavy lines because Minecraft's font is
  proportional (letters ~6px, spaces ~4px). It now measures each content line's **pixel width**
  (`visiblePixels` + a per-glyph `charPixels` table; color/format `§X` pairs count as 0) and sizes the
  divider to `pixelWidth / 4` spaces, so it grows to fit the longest line (e.g. a long
  `SergeantFuzzy` vs a short `{username}`) without overshooting. File: `ScoreboardRenderer.java`
  (`substituteDividers`, `buildDivider`, new `visiblePixels`/`charPixels`/`SPACE_PIXELS`).

## Categories Touched
Commands, Messages (EN/DE/ES), Config (scoreboard.yml, tablist.yml), Internal (renderers, util).

## Testing
- `./gradlew :core:test` — EN/DE/ES parity (incl. all new `economy.*` keys) **passes**.
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Feature (economy/tablist/scoreboard): box-style bal/baltop, /pay similar-name suggestions,
purple live tablist + bold gradient headers, scoreboard IP color + icons + pixel divider fit

- Banner: release date falls back to today when unstamped
- /bal, /bal <target>, /baltop -> box-style (EN/DE/ES)
- /pay: click-to-suggest "did you mean?" box for unresolved/similar names (>=3-char run)
- Tablist: violet theme, live {uptime}, refresh 40->2 ticks
- Scoreboard: bold purple gradient title via Adventure component API, IP=Web purple,
  per-field icons, pixel-accurate auto-fitting divider
```
