# Scoreboard field icons, tablist color fix, scoreboard title fix, /pay tab-complete fix

■ **Created:** 2026-06-04 4:16 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 4:16 pm (America/Detroit)

Clearer scoreboard field icons plus three rendering/UX fixes: tablist colors now show on every
transport, the scoreboard header renders the full "OBX" (not just "O"), and `/pay` tab-completes
online players. Build green; both jars; EN/DE/ES parity passes.

---

## Config / Cosmetic

- **Better scoreboard field icons.** Replaced the player/online/bank/ip/web icons in
  `scoreboard.yml` with clearer, better-mapped glyphs (health's heart is unchanged):
  `Ⓟ Player` · `❤ Health` · `● Online` (green status dot) · `⛁ Bank` (coin stack) ·
  `⇆ IP` (connection) · `⌂ Web` (home page). File: `plugin/src/main/resources/systems/scoreboard.yml`.

## Fixes

- **Tablist color formatting now renders.** The header/footer value lines used `<gray>` / `<#hex>` /
  `<dark_gray>` MiniMessage tags. On the legacy `setPlayerListHeader(String)` fallback transport,
  `stripUnsupportedTags` deletes every `<…>` tag → the text rendered with **no color at all**. The
  value/label lines now use legacy **`&`-codes** (which survive all three tablist transports —
  Adventure, Spigot `BaseComponent[]`, and the legacy String fallback); only the brand lines keep a
  `<gradient>` tag (degrading to plain text only on the oldest legacy fallback). File:
  `plugin/src/main/resources/systems/tablist.yml`.
- **Scoreboard header now shows full "OBX", not "O".** Two-part fix:
  - The objective title's component path (`AdventureMessageUtil.toComponent` /
    `adventureComponentClass`) was gated on **MiniMessage** being present
    (`ADVENTURE_COMPONENT_CLASS`). It now uses the **direct-build** Adventure path
    (new `ADVENTURE_DIRECT_COMPONENT_CLASS`), so the true bold gradient renders on Paper builds that
    ship Adventure core but not MiniMessage. Files: `core/.../util/message/AdventureMessageUtil.java`.
  - The **legacy fallback** (servers with no Adventure component API at all) rendered a per-glyph hex
    gradient (~14 chars/letter) that overflowed the objective's hard **32-char** cap, so a blind
    truncate left only `O`. It now collapses to a compact **bold dark-purple** rendering of the plain
    title when the full legacy form won't fit — so the whole word (`OBX`) is always visible. File:
    `features/scoreboard/.../format/ScoreboardRenderer.java` (`legacyTitleFallback`).
- **`/pay` tab-completes online players again.** The completer **excluded the sender**, so it returned
  an empty list when few players were online — which suppresses suggestions entirely (read as "broken").
  It now suggests every online player whose name matches the prefix (self-payment is still blocked at
  execution), sorted, plus light amount hints (`10/100/1000`) on the second argument — matching the
  working `/balance` completer. File: `features/economy/.../command/PayCommand.java`.

## Categories Touched
Config (scoreboard.yml, tablist.yml), Internal (AdventureMessageUtil, ScoreboardRenderer), Commands (PayCommand).

## Testing
- `./gradlew :core:test` — EN/DE/ES parity **passes** (no message-key changes this batch).
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Fix (scoreboard/tablist/pay): tablist colors, full OBX title, /pay tab-complete; clearer SB icons

- scoreboard.yml: clearer player/online/bank/ip/web icons (Ⓟ ● ⛁ ⇆ ⌂)
- tablist.yml: use &-codes on value lines so colors render on the legacy String transport too
- AdventureMessageUtil: toComponent uses the direct-build Adventure path (no MiniMessage requirement)
- ScoreboardRenderer: compact bold-purple legacy fallback so the title shows full OBX, not just O
- PayCommand: don't exclude self from tab suggestions (was suppressing all suggestions); add amount hints
```
