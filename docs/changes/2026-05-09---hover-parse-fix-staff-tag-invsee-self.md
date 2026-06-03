# Hover-tag parse fix + STAFF tag + full-tier invsee self

■ **Created:** 2026-05-09 1:08 am

■ **Last Updated:** 2026-05-09 1:08 am

## Summary

Four follow-ups to the previous welcome-MOTD / vanish / invsee drop:

1. **Hover tag parser bug fix** — the welcome MOTD's hover content was
   leaking into chat as raw text because the parser used a naive
   `indexOf('>')` to find tag closings and the new hover args
   (`<hover:show_text:'<gold>...'>`) contained `>` characters inside
   single-quoted args that prematurely terminated the tag.
2. **`[STAFF]` indicator** added to every `/vanish` and `/invsee`
   in-game message so it's unambiguous a staff tool fired and what state
   it produced.
3. **Full-tier `/invsee` can now view its own inventory.** Basic-tier
   still cannot.
4. **Tab-complete for `/vanish [player]` and `/invsee <player>`** stays
   wired via `findSingleArgMethod`-style flexible registration in
   `Main.bind(...)` — both commands implement `TabCompleter` and hand
   back online-player names that match the typed prefix; basic-tier
   `/invsee` filters privileged targets out of suggestions.

## Categories

### Internal — parser

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
    - **New `findTagEnd(text, from)` helper** — quote-aware close-bracket
      finder. Skips over `>` chars that lie inside single- or
      double-quoted argument strings. MiniMessage hover/click args
      commonly contain markup like
      `'show_text:<gold>X</gold>'`; the previous `indexOf('>')` would
      stop at the `>` of `<gold>`, leaving the rest of the hover content
      to leak into chat as visible text.
    - **All four `indexOf('>')` callsites updated** to use
      `findTagEnd`:
        - `parseToSpans` main loop (the tag walker).
        - `expandGradients` open-tag closing finder.
        - `expandGradients` inner-iteration tag-skipper.
        - `countVisibleGlyphs` tag-skipper.
    - Net effect: the welcome MOTD hover with its
      `<gold>/<bold>/<gray>/<yellow>/<white>`-styled args now parses as
      a single hover wrapper instead of fragmenting into raw text +
      mismatched style frames.

### Language strings

- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
    - All `player.vanish.*` and `player.invsee.*` user-facing messages
      now lead with `&8[&dSTAFF&8]` after `{prefix}`, then
      action label → state arrow → state badge → description, e.g.
      `{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lEnabled&7. You are now invisible to other players.`
    - New `player.invsee.opened-self` for the full-tier self-view path.
      Distinct copy from `player.invsee.opened` so the message clearly
      differentiates "viewing your own" from "viewing someone else's".
    - English + German pairs maintained for every key.

### Commands

- `src/main/java/dev/zcripted/obx/command/admin/InvSeeCommand.java`
    - Self-view branch:
      `if (self && !hasFull) → cannot-view-self`. Full-tier holders fall
      through and successfully open their own inventory; the
      `opened-self` message is sent rather than the `opened` template
      so the sender sees clear "viewing your own inventory" feedback
      rather than the awkward "Viewing PlayerName's inventory" with
      their own name.
    - Privileged-target check now scoped to `!self` so the self-view
      path doesn't accidentally re-trigger the no-permission gate.
    - `cannot-view-self` language line rewritten to explain that the
      restriction is tied to the basic permission tier rather than the
      command itself, so basic-tier staff get a useful error instead of
      a flat "you can't /invsee yourself".

### Tab completion (verified, no behavior change)

- `VanishCommand.onTabComplete` continues to gate suggestions on
  `obx.vanish.others` and only fires for the first arg slot. With
  default permissions (`op` for both `obx.vanish` and
  `obx.vanish.others`), an op typing `/vanish <prefix>` or
  `/v <prefix>` sees online-player names matching the prefix.
- `InvSeeCommand.onTabComplete` continues to gate on the basic OR full
  permission, fires only on the first arg slot, and silently filters
  out privileged players (ops + permission holders) for basic-tier
  users so the suggestion list reflects what the executor would
  actually accept.

## Files Modified

- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- `src/main/java/dev/zcripted/obx/command/admin/InvSeeCommand.java`

## Suggested Commit Message

```
Fix (parser+staff): quote-aware tag end, [STAFF] tag, invsee self-view
```
