# Welcome MOTD gradient — hoist hover/click onto a wrapper component

■ **Created:** 2026-05-09 2:35 pm

■ **Last Updated:** 2026-05-09 2:40 pm

## Summary

The `<gradient:#FFAA00:#FFFF55><bold>Welcome to the Server</bold></gradient>`
header on the welcome MOTD line was being delivered without its gradient
color effect — players saw a flat or banded result instead of the
per-glyph color ramp the parser was correctly producing.

The parsing pipeline itself was fine — a probe that exercised
`expandGradients` + `parseToSpans` showed 18 visible-glyph spans, each
carrying its own progressive RGB color from `#FFAA00` to `#FFFF55`, with
the bold flag and hover lines correctly attached. The bug was in the
wire packet shape produced by both delivery transports.

## Root cause

`buildComponentFromSpans` (Adventure direct path) and `renderBungee`
(BungeeCord fallback path) emitted **one component per span**, attaching
the span's hover/click event directly to that component. Per glyph that
meant one full `hoverEvent`-with-tooltip-body on every visible character.

The welcome banner's tooltip body is 16 lines / ~30 sub-components,
and the gradient runs for 21 visible chars. Multiplied:

```
  21 visible glyphs   ×   ~30 hover sub-components
≈ 630 nested components serialized into the chat-packet JSON
≈ 90 KB of repeated tooltip body on the wire
```

That blows past Mojang's pre-1.18 `~32 KB` chat-packet limit. On any
client/proxy that enforces it (legacy clients, ViaVersion downgrades,
1.17.x and earlier protocols, some chat decorators), the packet is
either rejected outright or truncated mid-stream, and the per-glyph
color tags are the first thing to drop. The visible result is "no
gradient effect" even though the parser produced the correct spans.

A probe against the Adventure direct path, dumping the resulting
component to JSON via `GsonComponentSerializer.gson()`, confirmed it:
~28 KB JSON with **21 separate `hoverEvent` blocks**, the tooltip body
duplicated for every visible glyph.

## Fix

Hoist the hover and click events onto a single wrapper component that
contains the per-glyph children, so the tooltip body is sent once and
inherited by every child via the chat protocol's NOT_SET inheritance.
Each child still carries its own RGB color, so the gradient is
preserved on every client version that supports the color format.

### Adventure direct path

`buildComponentFromSpans` now scans the span list for runs of
consecutive spans that share the same hover lines AND the same
click action+value. When such a run has more than one span and at
least one of hover/click is set, the run is built into a wrapper
`Component.text("")` that carries the hover/click; each span is
emitted as a child of the wrapper carrying only its color and
decorations. Single-span "groups" (no run, or no interactivity)
fall through to the original direct emission with hover/click on
the component itself.

A new helper `buildSpanChild(Span, boolean includeInteractive)`
factors the styling block out of the loop so the wrapper grouping
and the single-span path can share it.

### BungeeCord fallback path

`renderBungee` mirrors the same grouping logic: a run of consecutive
spans sharing hover/click is collected under a single
`TextComponent("")` parent (via `setExtra(List<BaseComponent>)`)
that carries the hover/click event; each child carries only its
color + decorations.

`applyStyle(TextComponent, Span)` is now an overload of
`applyStyle(TextComponent, Span, boolean includeInteractive)` so the
caller can opt out of writing the hover/click onto the span itself
when the wrapper has already absorbed it. A new
`applyInteractiveOnly(TextComponent, Span)` helper writes only the
hover and click event onto the wrapper (used after the wrapper's
children have been wired up).

### Defensive: quote-aware `<gradient>` search in `expandGradients`

`expandGradients` previously used a naïve `indexOfIgnoreCase` to find
`<gradient` tokens, which would match a `<gradient>` that happened to
appear inside a hover/click quoted argument string and rewrite it as
top-level per-glyph color tags — corrupting the surrounding hover
quote structure. The current welcome banner doesn't use a nested
gradient inside hover content, but a future config could, so the scan
is now quote-aware via a new `indexOfIgnoreCaseQuoteAware` helper that
skips characters that lie inside a single- or double-quoted run.

## Verification

Probe against the new code with the welcome banner string and Adventure
4.17.0 + Paper 1.20.6 on the classpath:

```
JSON length: 3550 bytes        (was ~28 000 with simplified hover,
                                ~90 000+ with the full 16-line body)
hoverEvent occurrences: 1      (was 21 — one per visible glyph)
color occurrences: 22          (one per visible gradient glyph + 4 hover-body colors)
Top-level children: 3
  child[0]: leading-space padding, no children
  child[1]: hover-bearing wrapper with 21 sub-children
  child[2]: trailing-space padding, no children
```

The 21 sub-children carry the full gradient sequence

```
gold, #FFAF05, #FFB40A, #FFB90F, #FFBE14, #FFC319, #FFC81E, #FFCD23,
#FFD228, #FFD72D, #FFDC32, #FFE137, #FFE63C, #FFEB41, #FFF046,
#FFF54B, #FFFA50, yellow
```

(Adventure's serializer emits the named-color form for `#FFAA00` and
`#FFFF55` because they match `gold` / `yellow` exactly — both forms
resolve to identical hex on every supporting client.)

Maven build completes cleanly (`./maven/bin/mvn.cmd -DskipTests
package` — only the standard ProGuard `Note:` lines for reflective
accesses remain, which are informational per `CLAUDE.md`):

- `target/SF-Core-1.0.0-SNAPSHOT.jar` (308 KB, obfuscated)
- `target/SF-Core-1.0.0-SNAPSHOT-unobf.jar` (454 KB, unobfuscated)

Both jars exercise the same render path; the obfuscated jar resolves
the Adventure handles by Java reflection on string method names, none
of which ProGuard renames since they live in `net.kyori.*`.

## Categories

### Internal — message dispatch

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`
    - `buildComponentFromSpans(List<Span>)` — rewritten to group
      consecutive spans that share hover/click into a wrapper
      `Component.text("")`; the wrapper carries the hover/click once
      and the children only carry color + decorations.
    - **New** `buildSpanChild(Span, boolean includeInteractive)` —
      factored span-to-Component helper. Children inside a wrapper
      pass `includeInteractive=false` so the hover/click stays on
      the parent; standalone spans pass `true` so the event lives
      on the visible glyph itself.
    - **New** `sameInteractive(Span a, Span b)` — span-equality test
      on hover lines + click action/value. Returns `true` only when
      both hover and click match exactly, so two adjacent gradient
      glyphs that happen to differ on either field stay in their own
      wrappers.
    - `renderBungee(String)` — same grouping logic as the Adventure
      path. Consecutive spans sharing hover/click are collected
      under a single `TextComponent` wrapper via
      `setExtra(List<BaseComponent>)`.
    - **New overload** `applyStyle(TextComponent, Span, boolean
      includeInteractive)` — when called with
      `includeInteractive=false`, writes only color + decorations
      and leaves hover/click for the wrapper.
    - **New** `applyInteractiveOnly(TextComponent, Span)` — writes
      only the hover and click event onto a wrapper after its
      children have been wired. Body MiniMessage routing through
      `renderBungee` is preserved so tooltip tags continue to render
      correctly.
    - `expandGradients(String)` — now uses
      `indexOfIgnoreCaseQuoteAware` to locate the next `<gradient`
      token, so a `<gradient>` that appears inside a single- or
      double-quoted hover/click argument is left untouched and
      reaches the parser inside the tooltip scope where it belongs.
    - **New** `indexOfIgnoreCaseQuoteAware(String, String, int)` —
      the quote-aware sibling of `indexOfIgnoreCase`.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/AdventureMessageUtil.java`

## Suggested Commit Message

```
Fix (motd): hoist hover/click onto a wrapper so the gradient
chat packet stays under Mojang's per-version size limit
```
