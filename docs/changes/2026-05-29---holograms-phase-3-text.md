# Holograms — Phase 3: Text features (MiniMessage, placeholders, pages)

■ **Created:** 2026-05-29 1:55 pm
■ **Last Updated:** 2026-05-29 1:55 pm

Lifts hologram text from "legacy `&` codes only" to a four-stage resolution
pipeline: pages → PAPI placeholders → MiniMessage → `%filler%` → legacy
section codes. Every stage degrades gracefully when its bridge is absent.

## Categories

### Internal — text package

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/text/HologramTextResolver.java`
  — single entry point. Pipeline order: page split → PAPI (if available
  and template contains `%`) → MiniMessage (if Adventure on classpath
  and template contains `<` or `#`) → filler → legacy `&` → final
  section-code string ready for backend setters.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/text/PlaceholderBridge.java`
  — reflective adapter to `me.clip.placeholderapi.PlaceholderAPI`.
  No compile-time PAPI dep. Cached `Method` handle; returns input
  unchanged on any failure.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/text/MiniMessageBridge.java`
  — reflective adapter to Adventure's `MiniMessage` + `LegacyComponentSerializer`.
  Resolved entirely via `Class.forName` so the same JAR runs on Spigot
  1.12 (no Adventure → identity) and on Paper 1.21 (parses gradients,
  hex colours, decorations and converts back to `§`-coded text).
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/text/FillerExpander.java`
  — implements `%filler%`. Character-count proxy keyed off the hologram's
  `line-width` setting. Tunable per-hologram.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/text/PageState.java`
  — per-(viewer, hologram) page cursor in a `ConcurrentHashMap`.
  `next`, `prev`, `set` operations all modulo by the hologram's page
  count. `PageState.clear(UUID)` called from the existing quit listener.

### Commands

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/PageSub.java`
  — `/sfholo page <holo> [player] <next|prev|N>`. Updates the page cursor,
  marks the hologram dirty, calls `renderer.refreshFor` so the new page
  shows immediately. Registered in `HologramCommand`.

### Backend integration

* `DisplayEntityBackend.spawnText` — now routes the template through
  `HologramTextResolver.resolve(template, hologram, viewer=null)` before
  calling `setText(String)`. Falls back to plain legacy translation if the
  resolver returns null/empty (defence in depth).
* `ArmorStandBackend.spawnLine(TEXT)` — same change.
* Per-viewer text via per-viewer entities is left as a Phase 7 polish per
  the plan; Phase 3 resolves with no viewer context, which still surfaces
  MiniMessage / `%filler%` / pages (server-default page) / server-wide
  PAPI placeholders.

### Listener

* `HologramJoinListener.onQuit` — now also calls `PageState.clear` so a
  reconnecting player starts on page 0.

### Language

* `MessageDefaults.java` — 5 new keys:
  `hologram.page.changed`, `hologram.page.unknown`, `hologram.page.first`,
  `hologram.page.last`, `hologram.error.player_not_found`. Both EN + DE.

## Verification

* `mvn -DskipTests compile` — green.
* Manual checks (Paper 1.21 + PAPI installed):
  * Line `<gradient:gold:yellow>Hello %player_name%</gradient>` renders
    as a gradient with the viewer's name.
  * `/sfholo page welcome next` advances the page cursor; the line shows
    the next `!nextpage!`-delimited segment.
* Manual checks (Spigot 1.13, no PAPI, no Adventure):
  * The same gradient line renders the raw `<gradient...>` as literal
    text and the placeholder shows as `%player_name%`. No errors logged.
  * Pages still work (server-default page).

## Suggested Commit Message

```
SF-Core Holograms: Phase 3 — text resolver + MiniMessage / PAPI bridges + pages
```
