# Chat Enchant Tooltip — Lore-Style Level Bar

■ **Created:** 2026-05-27 1:30 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 1:30 pm (America/Detroit)

---

## Summary

The enchant hover tooltip shown for enchant names in chat now renders its level line
with the **item-lore tier dots** (`●`/`○`) plus an RPG-style **`Lvl N`** text label,
instead of a plain `Level: 3/10`.

## Categories

### Internal
- `enchant/storage/EnchantStorage.java` — exposed the lore dot-bar renderer as
  `public static String levelDots(int level, int max, ChatColor fill)` (delegates to the
  existing private `dots(...)`), so the chat tooltip and the gear lore share one source.
- `enchant/util/EnchantHover.java` — the tooltip's level line now uses
  `EnchantStorage.levelDots(...)` (filled dots in the enchant's **rarity color**, empty in
  dark gray) followed by a white `Lvl N` label:
  - applied level: `Level » ●●●○○○○○○○ Lvl 3/10`
  - browse (no specific level): `Max Level » ●●●●●●●●●● Lvl 10`

## Scope
Because everything routes through `EnchantHover.tooltip`, this updates the tooltip
**everywhere** it appears: `/sfench give` & `givebook` confirmations, `/sfench info`,
`/sfench list`, `/enchants`, the Codex books, and the apply/remove feedback.

## Testing
- Maven build: exit 0, both jars (obf ~621 KB, unobf ~903 KB). ProGuard `Note:` lines only.
  Compile-verified — hover an enchant name in chat (e.g. from `/sfench givebook ... apex_predator 1`)
  and confirm the dot bar + `Lvl N` render.

## Suggested Commit Message
```
Polish (enchants): chat tooltip level line uses the lore dot bar (●/○) + 'Lvl N' label
```
