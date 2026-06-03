# SYSTEM Message Prefix (+ hex colors) & Enchant-Book Use Hint

■ **Created:** 2026-05-27 3:07 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 3:07 pm (America/Detroit)

---

## Summary

Two changes: (1) `/obx reload`, `/obx reload <file>`, and `/obx debug` messages now carry a
custom **⚙ 𝗦𝗬𝗦𝗧𝗘𝗠** wordmark prefix in hex `#FF4526` (and `colorize()` gained
`&#RRGGBB` hex support); (2) right-clicking a custom enchant scroll/book in hand shows a
brief action-bar hint that it must be applied to an item.

## Categories

### Messages / prefix
- `language/MessageDefaults.java` — new `system.prefix` =
  `&#FF4526⚙ 𝗦𝗬𝗦𝗧𝗘𝗠 &8➠ &7` (cog U+2699 + math-bold SYSTEM, hex `#FF4526`, same
  character style + `➠` as `core.prefix`). Codepoints verified.
- `language/LanguageManager.java`:
  - `colorize()` now translates **`&#RRGGBB`** into the `§x§R§R§G§G§B§B` legacy hex
    sequence (renders on 1.16+; safely ignored where unsupported). Plugin-wide, and
    backward-compatible (no existing message used `&#`).
  - `resolveMessages` routes `{prefix}` for `commands.obx.reload.*` and
    `commands.obx.debug.*` keys to `system.prefix` (covers `/obx reload`, `/obx reload config`,
    `/obx reload <file>`, and `/obx debug`). `/obx config`, `/obx diagnostics`, etc. keep the
    OBX prefix.

### Enchant scroll/book right-click hint
- `enchant/listener/EnchantBookUseListener.java` (new) — on a right-click with an Arcanum
  **book** or **enchant scroll** in hand (`ScrollKind.BOOK` / `ENCHANT_SCROLL`), sends an
  action-bar hint (`enchant.book.use-hint`) that it must be applied to an item; the bar
  fades on its own after ~3 s (no clear task). Codex guide books (no `kindOf` marker) and
  utility scrolls (protection/success/extraction/transmutation) are excluded. Registered in
  `Main`. New EN+DE message `enchant.book.use-hint`.

## Notes / assumptions
- Hex `#FF4526` only renders on **1.16+** clients (legacy `§x`). The server is 1.16+
  (Netherite); on older servers it degrades.
- The hint covers both the enchant **book** and **scroll** forms (both are applied to an
  item, never right-clicked). If only the book form was intended, the scroll behavior is
  still accurate.
- Action bars auto-fade ~3 s, which matches the "disappears after 3 seconds" request, so
  no scheduled clear is used.

## Testing
- Maven build: exit 0, both jars (obf ~625 KB, unobf ~908 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: `/obx reload`, `/obx reload config`, `/obx reload <file>`, and
  `/obx debug` should show the ⚙ SYSTEM prefix; right-clicking an enchant scroll/book should
  flash the "must be applied to an item" action bar that fades after ~3 s.

## Suggested Commit Message
```
Feature (sf/enchants): ⚙ SYSTEM prefix (#FF4526 + &#hex support) for /obx reload & debug; action-bar hint on right-clicking enchant scrolls/books
```
