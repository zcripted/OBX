# `/sf reload` — Redesigned Message, Console Summary & Accountability

■ **Created:** 2026-05-27 3:30 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 3:30 pm (America/Detroit)

---

## Summary

`/sf reload` now reports **per-component load times**, with a cleaner styled hover
(matching `/sf about` / `/pl`), a clean ANSI console summary, and cross-channel
accountability (who reloaded).

## Categories

### Internal
- `Main.reloadPlugin()` now returns an ordered `Map<String,Long>` of **component →
  elapsed nanoseconds** (times each reload step inline). Existing callers (the admin
  GUI) harmlessly ignore the return.

### In-game hover (`command/core/SFCoreCommand.java`)
- The full-reload hover is now built **dynamically**:
  - SYSTEM-red header `&#FF4526▍ 𝗦𝗬𝗦𝗧𝗘𝗠 › Reload Complete` + a divider (same style as
    `/sf about`/`/pl`).
  - Each component listed **alphabetically** with a clean `•` bullet and its individual
    load time in parentheses using **small-caps** units, e.g. `• config.yml (12.4ᴍꜱ)`.
  - `Total › <time>` line, then a `ℹ Tip` line (info icon + "Tip" in light-yellow).
  - The old "Reload scope:" text and `- ` list style are gone.
- The base chat line is now a concise `&aReload complete &8(&f<time>&8)` (with the
  SYSTEM prefix), the hover carries the detail.

### Console summary + accountability
- A clean console block via `ConsoleLog` (which renders §-codes as ANSI matching the
  in-game palette): a green `[SF-Core][Reload] Reloaded by <who> in <time> (N components)`
  line + a `Reloaded: a (Xms), b (Yms), …` component list (plain `ms` for terminal fonts).
- **Player runs `/sf reload`** → player sees the styled in-game message; the console logs
  it **with the player's name**.
- **Console runs the reload** → console logs it; every online player with
  `sfcore.admin.reload` is notified in-game (`commands.sf.reload.notify`) **crediting
  "Console"**.

### Messages (`MessageDefaults`)
- Replaced static `commands.sf.reload.full.hover` with fragment keys:
  `full.header`, `full.entry`, `full.total-line`, `full.tip-line`; reworded `full.base`;
  added `commands.sf.reload.notify`. (All under `commands.sf.reload.*` → SYSTEM prefix.)

## Notes / assumptions
- "Small custom capital" load times → small-caps unit `ᴍꜱ` (U+1D0D U+A731) in-game;
  digits have no small-caps form, and the console uses plain `ms` (terminal fonts lack the
  small-caps glyphs).
- `&#FF4526` in the hover header renders on 1.16+ (legacy `§x`); degrades on older.
- The header is SYSTEM-red to stay cohesive with the reload command's SYSTEM prefix while
  matching the `/sf about` *structure* (▍ wordmark › divider · rows).

## Testing
- Maven build: exit 0, both jars (obf ~627 KB, unobf ~911 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: `/sf reload` → styled base + hover with alphabetical
  per-component `ᴍꜱ` times, `ℹ Tip`, console green summary naming the player. From console
  → online reload-perm players get the "reloaded by Console" notice.

## Suggested Commit Message
```
Feature (sf): redesigned /sf reload — per-component load times, styled hover, ANSI console summary, who-reloaded accountability
```
