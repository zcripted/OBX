# Gradient text rendering — distribute color across visible glyphs only

■ **Created:** 2026-05-08 11:00 am

■ **Last Updated:** 2026-05-09 12:15 am

## Summary

Gradient-styled headers (welcome MOTD, first-join MOTD, tablist header) were
rendering as near-solid colors on join. Two compounding causes:

1. The default templates wrapped padded text inside the gradient tag — e.g.
   `<gradient:#FFAA00:#FFFF55><bold>          Welcome to the Server          </bold></gradient>`
   has 10 leading + 21 visible + 10 trailing characters. MiniMessage assigns
   each character a slot in the color ramp, so the visible letters only spanned
   the middle ~50 % of the gradient (≈ ratio 0.25 → 0.75). With a
   yellow-orange→light-yellow ramp, that compressed range looked nearly solid.
2. The BungeeCord-fallback parser (used on servers without MiniMessage)
   counted whitespace into the ratio for the same reason, and on legacy
   clients without hex support `closestLegacyColor` then rounded all those
   compressed mid-tones to the same one or two legacy `ChatColor` entries —
   producing a literally solid result.

Fix:

- Move the leading/trailing padding **outside** the `<gradient>` tags in the
  default config, so the visible text receives the full color range on both
  the Adventure and BungeeCord rendering paths.
- Update the BungeeCord-fallback `emitGradient` to compute the gradient ratio
  from visible (non-whitespace) glyphs only. Whitespace inside a gradient is
  preserved and inherits the surrounding span style, but no longer consumes a
  slot in the color ramp. This keeps custom configs that still embed
  whitespace inside `<gradient>...</gradient>` rendering correctly on legacy
  clients.

## Categories

### Config

- `src/main/resources/config.yml`
    - `join-motd.lines[1]` — pulled the 10-space leading/trailing pads out of
      the gradient wrapper:
      `          <gradient:#FFAA00:#FFFF55><bold>Welcome to the Server</bold></gradient>          `
    - `join-motd.first-join.lines[1]` — same treatment for
      "A Brand New Adventure!".
- `src/main/resources/systems/tablist.yml`
    - `header[1]` — same treatment for "SF-Core Network".

### Internal

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`
    - `emitGradient(...)` — count and step the gradient ratio over visible
      glyphs only; whitespace chars inside the gradient are emitted with the
      span's inherited style and no per-character color override.
    - **New `expandGradients(String)`** — pre-expands every
      `<gradient:c1:c2>...</gradient>` block into a sequence of explicit
      per-character `<color:#RRGGBB>X</color>` tags (preserving nested tags
      and legacy `&x` codes; whitespace consumes no slot in the ramp).
      Fully-closed `<color:...>...</color>` scopes are emitted instead of
      the unclosed `<#RRGGBB>` shorthand because some MiniMessage builds
      and downstream chat formatters were treating a long run of unclosed
      hex shorthands as one deep-nested scope and merging adjacent glyphs
      back into one color band on the wire. Wired into:
        - `legacyToMiniMessage` (Adventure / MiniMessage send path)
        - `renderBungee` (BaseComponent fallback path)
        - `renderAnsiFromResolved` (console mirror)
      This bypasses MiniMessage's built-in gradient renderer entirely. Some
      MiniMessage builds and proxy/translator layers were collapsing the
      gradient into a small number of solid color bands when wrapped in
      `<bold>` or sent to legacy clients; emitting one explicit `<#RRGGBB>`
      tag per glyph forces a true per-character gradient regardless of the
      downstream renderer.
    - New helper `countVisibleGlyphs(String)` skips tag bodies and legacy
      color/format codes when computing the gradient denominator.
    - **New direct Adventure Component build path (`trySendAdventureDirect` +
      `buildComponentFromSpans` + `buildHoverComponent` + `buildClickEvent`)** —
      tried first inside `trySendAdventure`, falling back to the MiniMessage
      path only if the reflection surface couldn't be wired or the build
      failed. Constructs the outgoing `Component` tree ourselves: each parsed
      `Span` becomes a flat sibling with an explicit
      `TextColor.color(int rgb)`, plus its decorations / hover / click,
      appended onto an empty parent. No MiniMessage parser is between the
      span list and the chat packet, so neither MiniMessage's tag-stack
      semantics nor any chat decorator that re-parses Component output can
      collapse the per-glyph colors into solid bands.
    - Static initializer now resolves and caches handles for
      `Component.text/append/color/decorate/hoverEvent/clickEvent`,
      `TextColor.color(int)`, the five `TextDecoration` enum constants, and
      every `ClickEvent` action factory used by the parser
      (`runCommand`, `suggestCommand`, `openUrl`, `copyToClipboard`,
      `changePage`). All look-ups are guarded — the path advertises itself
      as ready only when every required handle is present, and aborts to
      the legacy MiniMessage path otherwise.
    - **Method lookup uses assignable-parameter matching, not exact match**
      — `findSingleArgMethod` / `findSingleArgStaticMethod` walk
      `clazz.getMethods()` and pick the most specific overload whose single
      parameter is assignable from our argument type. Adventure's
      `Component.hoverEvent(...)` actually takes `HoverEventSource<?>`, not
      `HoverEvent`; an exact `getMethod(... HoverEvent.class)` lookup throws
      `NoSuchMethodException`, which (in the previous revision) was
      bubbling up the surrounding try/catch and silently leaving
      `ADVENTURE_DIRECT_READY = false` — meaning the direct path was wired
      in source but never actually ran. The flexible lookup, plus
      per-handle try/catch, makes the direct path resolve correctly on
      modern Adventure builds.
    - Static init now logs one diagnostic line via
      `Logger.getLogger("SF-Core")`:
      `[gradient] Adventure paths — direct=…, minimessage=…, available=…`
      so it's visible at server start which paths actually wired up.
    - **Runtime path-trace log** — first gradient-bearing message also logs
      which delivery path actually served it
      (`direct/sent`, `direct/threw …`, `legacy-hex/sent`, `bungee/...`)
      via `logFirstGradientPath`. Confirms whether the runtime is on the
      direct-build path or has dropped into a fallback. Logs exactly once
      per server lifetime to keep noise out of busy chats.
    - **`buildComponentFromSpans` hardened** — each span's reflection block
      is now in its own try/catch. One failing span (bad hex, missing
      decoration enum, etc.) skips that glyph instead of dropping the
      whole component to `null` and forcing the fallback ladder. The
      previous all-or-nothing behavior was the most likely cause of the
      "still 2 solid colors" reports on Paper 1.21.x — a single reflective
      throw inside the loop was tanking the entire gradient build,
      cascading down to BungeeCord's `closestLegacyColor` (which collapses
      `#FFAA00 → #FFFF55` to GOLD/YELLOW — exactly two bands).
    - **`lookupAdventureSendMessage` now uses flexible matching** — the
      previous exact-signature `playerClass.getMethod("sendMessage",
      Component.class)` lookup failed on Paper 1.21.x's CraftPlayer (the
      runtime path log surfaced
      `direct/no-sendMessage-method on org.bukkit.craftbukkit.entity.CraftPlayer`),
      causing the entire direct-build path to bail out per-message even
      though the static-init handles all wired up successfully. Switched
      to the same `findSingleArgMethod` helper used elsewhere — finds
      `sendMessage(Component)` or `sendMessage(ComponentLike)` (Component
      is a ComponentLike, so the assignable-parameter matcher accepts
      either). The direct-build path is now the actual primary path on
      Paper 1.21.x rather than relying on the legacy-hex-text fallback.
    - **New legacy-hex-text transport** — between the Adventure paths and
      the BungeeCord BaseComponent path, `send` now tries
      `trySendLegacyHexText` / `renderToLegacyHexText`. Walks the parsed
      spans and emits a single `§x§A§B§C§D§E§F`-coded string, then ships
      via `Player.sendMessage(String)`. Paper's `LegacyComponentSerializer`
      parses those `§x` sequences into per-glyph hex Component children —
      exactly the shape the wire format expects, with zero dependency on
      Component-API reflection. Pre-1.16 clients that don't recognise
      `§x` see un-coloured text instead of a 16-color band collapse, which
      is the better degradation for "support as many versions as
      possible". This path runs before the BungeeCord fallback so that
      `closestLegacyColor`'s band-collapse can no longer be reached when
      MiniMessage is unavailable on the runtime.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`
- `src/main/resources/config.yml`
- `src/main/resources/systems/tablist.yml`

## Suggested Commit Message

```
Fix (gradient): apply color ramp across visible glyphs, not padding
```
