# MOTD Gradient Rendering + Server-List Hover

■ **Created:** 2026-05-25 11:15 am
■ **Last Updated:** 2026-05-25 11:15 am

## Summary

Resolves three related server-list / formatting issues, all verified to build
in both the obfuscated and unobfuscated JARs:

1. **MOTD gradient rendered as solid color bands.** The MOTD pipeline never
   invoked the gradient engine — it only ran legacy `&`-code translation, so a
   `<gradient:…>` (or `&#RRGGBB` hex) MOTD collapsed to plain/solid color.
2. **Server-list hover (player sample) never appeared on Paper.** The listener
   was registered only for the base `ServerListPingEvent`; on Paper that event
   has no player-sample API, so the hover silently no-opped (MOTD + counter,
   which use the base API, worked fine).
3. **Obfuscation-fragile self-reflection.** Two classes resolved one of their
   own fields by literal name (`getDeclaredField("MISSING_METHOD")` /
   `"MISSING"`), which only survives obfuscation if ProGuard happens to adapt
   the string. Hardened to a stable JDK field.

## Categories

### Internal — gradient rendering (the "two solid colors" fix)
- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
  - New `public static String renderLegacy(String raw)` — renders MiniMessage /
    legacy / `<gradient>` / hex markup to a legacy section-coded string. The 16
    standard colors emit their plain `§`-code (universal, incl. pre-1.16); every
    gradient glyph / custom hex emits `§x§R§R§G§G§B§B` (a true per-glyph RGB run
    → smooth gradient on 1.16+). Gradients are expanded to explicit per-glyph
    colors first, so the result never depends on a downstream MiniMessage pass.
  - New private `hexToStandardCode(String)` helper backing the above.
- `src/main/java/dev/zcripted/obx/util/message/MotdMessageUtil.java`
  - `colorize(...)` now routes lines containing a `<…>` tag or `&#RRGGBB` hex
    through `AdventureMessageUtil.renderLegacy(...)`; pure `&`-code lines (incl.
    the `&x§R…` legacy-hex form) still use `translateAlternateColorCodes` so
    non-gradient MOTDs and the existing default config render byte-for-byte
    identically — zero behavior change for them. This covers both MOTD lines and
    the custom hover lines (both flow through `colorize`).

### Listeners — server-list hover on Paper
- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
  - New `registerPaperPingListener()` dynamically registers the existing handler
    for `com.destroystokyo.paper.event.server.PaperServerListPingEvent` (resolved
    reflectively; absent on base Spigot → no-op). That event declares its own
    `HandlerList`, so a base-`ServerListPingEvent` listener never receives the
    sample-capable instance — this is why the hover never showed on Paper. The
    handler dispatches against the Paper event, whose class exposes
    `getPlayerSample()` / `setHidePlayers()`, so the cached reflection populates
    the hover.
- `src/main/java/dev/zcripted/obx/Main.java`
  - Stores the `MotdPingListener` instance and calls `registerPaperPingListener()`
    right after the normal `registerEvents(...)`.

### Internal — obfuscation hardening
- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
  - `sentinelField()` now resolves `Integer.class.getDeclaredField("MAX_VALUE")`
    (a JDK field, never obfuscated) instead of reflecting on its own renamed
    field. Purely a unique non-null sentinel; never read.
- `src/main/java/dev/zcripted/obx/tablist/format/TablistRenderer.java`
  - Same hardening for its `sentinelField()` (`MISSING` → `Integer.MAX_VALUE`).

## Behavior Notes / Assumptions
- The server-list MOTD is a String API, so gradients are delivered as `§x` hex
  runs (the only way RGB reaches the server-list); these render as a true
  gradient on 1.16+ clients and degrade gracefully on older ones.
- On Paper, both the base and Paper ping events fire; the handler is idempotent
  (same MOTD/counter values set on each, hover set once on the Paper event), so
  double-dispatch is harmless. On base Spigot the hover/sample API does not
  exist and is correctly skipped.

## Verification
- `./maven/bin/mvn -DskipTests package` completes with no errors.
- Both `OBX-1.0.0-SNAPSHOT.jar` (obfuscated) and `-unobf.jar` produced.
- Verified in the obfuscated JAR: `sentinelField` resolves `Integer.MAX_VALUE`
  (JDK string preserved), and the `PaperServerListPingEvent` registration via
  `PluginManager.registerEvent(..., EventExecutor, ...)` is present.

## Suggested Commit Message

```
Fix (motd): Render gradients in MOTD/hover, restore Paper server-list hover, harden obfuscation-fragile reflection
```
