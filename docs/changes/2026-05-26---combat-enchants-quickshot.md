# Combat Enchantments — Quickshot (Phase 2 close-out)

■ **Created:** 2026-05-26 10:30 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 10:30 pm (America/Detroit)

---

## Summary

Implements **Quickshot (#20)** as a pure-Bukkit **rapid-fire** approximation (user
decision), closing out Phase 2 at **42 / 44** — all 15 Combat Phase-2 enchants done.

## Why an approximation
A bow's draw/reload time is governed client-side; there is no clean server-side API
to accelerate it without ProtocolLib. The chosen approach instead lets a right-click
instantly loose a fully-charged arrow on a short cooldown, which *feels* like faster
shooting and keeps the plugin a single self-contained jar across 1.8.8 → 1.21.x.

## Categories

### Enchants (combat.yml)
- **Quickshot** (#20) — `BOW`/`CROSSBOW`, uncommon, max 4. Right-click looses a
  fully-charged arrow; cooldown shrinks by level (30/20/14/8 ticks), Lv4 adds a small
  spread. Gated by **enchant presence** rather than material, so it covers crossbows
  without referencing the 1.14+ `Material.CROSSBOW` at compile time.

### Internal
- `enchant/listener/RangedListener.java` — new `onQuickshot(PlayerInteractEvent)`
  handler plus helpers (`fireQuickArrow`, `consumeArrow`, `hasInfinity`, `isArrow`,
  reflective `isOffHand`). The fired arrow is tagged with `BOW_META` so on-hit bow
  enchants (Sniper's Eye, Hunter's Mark, Ricochet, the reactive specials) still
  resolve, and Piercing Shot carries over. Cooldown uses the existing
  `CombatState` generic cooldown store.

## Behavior notes / assumptions
- **Off cooldown:** a right-click is consumed (event cancelled) and fires instantly,
  so the bow effectively becomes click-to-fire. **On cooldown:** the event is left
  untouched, so the vanilla draw still works as a fallback.
- **Ammo:** consumes one arrow from the inventory (`ARROW`/`TIPPED_ARROW`/
  `SPECTRAL_ARROW`); skipped in Creative or with Infinity (guarded reflectively).
- **Draw-time/charge effects** that only exist at vanilla launch (Phantom Volley) do
  not apply to a quick-shot arrow; on-hit effects do (via the metadata tag).

## Status: 42 / 44 — Phase 2 complete
- Remaining (Phase 3): **Endless Hunger (#43)**, **Apex Predator (#44)** +
  `/enchants settings` per-player FX toggle (lore-encoded persistent counters).

## Testing
- Maven build: exit 0, both jars produced (obf ~600 KB, unobf ~871 KB). ProGuard
  `Note:` lines only. Compile-verified only — in-game checks needed for: arrow
  consumption/Infinity/Creative paths, the cooldown gating + vanilla fallback, and
  that quick-shot arrows still trigger on-hit bow enchants.

## Suggested Commit Message
```
Feature (enchants): Combat #20 Quickshot — pure-Bukkit rapid-fire (Phase 2 complete, 42/44)
```
