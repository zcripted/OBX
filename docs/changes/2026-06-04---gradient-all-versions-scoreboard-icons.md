# Tablist + scoreboard "OBX" gradient on all versions; player/web scoreboard icons

■ **Created:** 2026-06-04 5:12 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 5:12 pm (America/Detroit)

The bold purple "OBX" gradient now renders on the tablist header and the scoreboard title across all
versions (true `§x` hex on 1.16+, a coarse standard-color gradient on older clients), and two
scoreboard field icons were swapped for clearer ones. Build green; both jars; EN/DE/ES parity passes.

---

## Config / Cosmetic

- **Scoreboard player icon → person silhouette** (`Ⓟ` → `☻`) and **web icon → globe** (`⌂` → `⊕`).
  File: `plugin/src/main/resources/systems/scoreboard.yml`.

## Fixes

- **Tablist "OBX" bold gradient now renders on all versions.** The header was reaching the legacy
  `setPlayerListHeader(String)` fallback, which **stripped** the `<gradient>` / `<bold>` tags (leaving
  plain text) while the `&`-coded value lines still showed. That fallback now renders the markup into a
  legacy section-coded string via the new `AdventureMessageUtil.renderLegacyForClient` — a true
  per-glyph **`§x` hex gradient on 1.16+**, and a **nearest-standard-color** approximation on older
  clients (which can't display `§x`) — so the bold purple gradient shows on the String transport too.
  Files: `core/.../util/message/AdventureMessageUtil.java` (`renderLegacyDownsampled`,
  `renderLegacyForClient`, `hexCapable`, tablist legacy fallback).
- **Scoreboard "OBX" now shows the full gradient on all versions (was solid purple).** When the
  Adventure component-title API isn't available (Spigot / older Paper), the title previously dropped
  straight to a compact **solid** bold-purple because a per-glyph `§x` gradient (~14 chars/letter)
  overflowed the assumed hard 32-char cap. The display-name cap is only 32 on 1.8–1.12 — it's far
  higher on 1.13+ — so the rewrite now tries, in order: the **smooth `§x` gradient** (fits the raised
  cap → renders on 1.16+), then a **coarse nearest-standard-color gradient** (short enough for the
  1.8–1.12 cap, and the only colored form pre-1.16), then the compact solid title only as a last
  resort. The Adventure component path is still preferred first (best on Paper). File:
  `features/scoreboard/.../format/ScoreboardRenderer.java` (`applyLegacyTitle`, `isHexCapable`,
  `trySetDisplayName`).

## Categories Touched
Config (scoreboard.yml), Internal (AdventureMessageUtil, ScoreboardRenderer).

## Testing
- `./gradlew :core:test` — EN/DE/ES parity **passes** (no message-key changes this batch).
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Fix (tablist/scoreboard): render the OBX gradient on the legacy String paths for all versions

- AdventureMessageUtil: renderLegacyForClient/renderLegacyDownsampled — §x hex gradient on 1.16+,
  nearest-standard approximation on older clients; tablist legacy fallback renders the gradient
  instead of stripping <gradient>/<bold>
- ScoreboardRenderer: try the full §x gradient (fits the raised 1.13+ display-name cap) then a coarse
  standard-color gradient before the solid fallback, so the title shows a real gradient everywhere
- scoreboard.yml: player icon → ☻ (person), web icon → ⊕ (globe)
```
