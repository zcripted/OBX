# Combat Enchantments — Phase 3 (Persistent Capstones + Settings)

■ **Created:** 2026-05-26 10:55 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 10:55 pm (America/Detroit)

---

## Summary

The final Combat-expansion batch: the two Mythic capstones (Endless Hunger,
Apex Predator) plus the `/enchants settings` per-player FX toggle. **The Combat
expansion is now complete at 44 / 44.**

## Categories

### Enchants (combat.yml)
- **Endless Hunger** (#43) — `SWORD`/`AXE`, Mythic, max 3. Every kill feeds the
  weapon; each milestone (every 10/8/5 kills) grants a **permanent +1% damage stack**
  stored on the item, capped at +15/25/40%. The count persists across logins, the
  weapon's display name shows `[Hunger ×N]`, and a milestone fires a flame aura + a
  HUNGER banner.
- **Apex Predator** (#44) — `WEAPON`, Mythic, max 1. The ultimate capstone: +15%
  damage, +10% crit chance, +10% crit damage, killstreak buffs amplified 1.5×, and a
  5% chance on kill to harvest a random Rare/Epic scroll. Gold burst on every kill.
  **Only one can be active per player** — a second copy is inert and warns (throttled)
  via the action bar.

### Internal
- `enchant/effect/EndlessHunger.java` (new) — persistent kill counter. Stored as a
  single visible progress line in the item's lore; `EnchantStorage` treats it as
  foreign "other" lore and preserves it through its tooltip rewrites (so anvil /
  enchant-table ops don't wipe it). The raw kill total is the source of truth (parsed
  back from the parenthetical); the percent and `[Hunger ×N]` name suffix are derived.
- `enchant/service/CombatPrefs.java` (new) — per-player FX opt-out, persisted as a
  UUID list in `enchants/combat_prefs.yml`. Loaded by `EnchantService.load()` and
  exposed via `getCombatPrefs()`.
- `enchant/listener/OnHitDamageListener.java` — adds the Endless Hunger stack bonus,
  the Apex Predator damage/crit bonus + killstreak amp, and the Apex "only one
  active" throttled warning.
- `enchant/listener/OnKillListener.java` — records the Endless Hunger kill (+ milestone
  FX) and runs the Apex Predator gold burst + random-scroll harvest (via
  `EnchantItems.scroll(...)`).
- `enchant/service/HoloFXService.java` + `enchant/listener/CombatSupport.java` — kill
  banners and combat action-bar feedback now honor the player's `CombatPrefs` opt-out.
- `enchant/command/EnchantsBrowseCommand.java` — `/enchants settings` toggles the
  caller's combat-FX preference (available to any player). New messages
  `enchant.settings.enabled` / `enchant.settings.disabled` (EN + DE in
  `MessageDefaults`). `plugin.yml` usage + `docs/information/about.md` updated.

## Assumptions (documented per CLAUDE.md)
- **"Unique enemies killed"** is implemented as the **total kill count** — each kill is
  a distinct entity and persisting a per-victim UUID set in lore is infeasible.
- **`/enchants settings`** gates the FX the player *generates* (their own kill banners +
  combat action-bar feedback). True per-viewer hiding of world holograms isn't possible
  without packets, so it intentionally silences the caller's own combat noise rather
  than hiding others'.
- **Apex Predator's "constant gold aura on weapon"** is approximated with an on-hit /
  on-kill aura burst rather than a per-tick held-item scan (kept cheap).

## Cross-version notes
- Persistence is lore-based (PDC is 1.14+; the plugin compiles against 1.12.2). The
  Endless Hunger item write-back uses the held-slot setter for cross-version safety.
- Scroll harvest is guarded — if the scroll factory is unavailable it simply no-ops.

## Status: 44 / 44 — Combat expansion COMPLETE
All four phases delivered: Phase 1 (27 melee mechanics), Phase 2 (15 ranged / banner /
reactive / Quickshot), Phase 3 (2 Mythic capstones + `/enchants settings`).

## Testing
- Maven build: exit 0, both jars produced (obf ~606 KB, unobf ~880 KB). ProGuard
  `Note:` lines only. Compile-verified only — in-game checks needed for: Endless
  Hunger persistence across relog + anvil/enchant ops (count not lost, name suffix
  correct), Apex Predator scroll harvest + the one-active lock warning, and that the
  `/enchants settings` opt-out actually silences the caller's banners/feedback.

## Suggested Commit Message
```
Feature (enchants): Combat Phase 3 — Endless Hunger + Apex Predator capstones + /enchants settings (44/44 complete)
```
