# Combat Enchantments — Phase 2 (Ranged, Batch A)

■ **Created:** 2026-05-26 8:03 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 8:03 pm (America/Detroit)

---

## Summary

Opens Phase 2 with a new `RangedListener` and the first five bow/crossbow
enchants. Brings the Combat expansion to **32 / 44**.

## Categories

### Internal — ranged framework
- `enchant/listener/RangedListener.java` (new) — captures the firing bow's enchant
  context at launch (a clone of the bow stored on the arrow's metadata) and reads
  it back when the arrow deals damage. Registered in `Main` with `CombatState` +
  `CombatParticleService`.
- `CombatState` gained a Hunter's Mark "marked target" store
  (`markTarget`/`markBonus`); the mark bonus is read in `OnHitDamageListener` so a
  marked target takes more damage from any attacker.

### Enchants (combat.yml)
- **Hunter's Mark** (#16) — on hit, marks the target: glow (reflective `setGlowing`
  + scheduled un-glow) + a cross-source damage bonus for the duration.
- **Sniper's Eye** (#19) — damage scales with shot distance; Lv4 adds a long-range
  crit beyond 30 blocks ("Long Shot").
- **Piercing Shot** (#17) — sets the arrow's pierce level (reflective, 1.14+).
  Conflicts ricochet.
- **Ricochet** (#21) — on hit, the arrow bounces to nearby targets with damage
  falloff. Conflicts piercing_shot.
- **Phantom Volley** (#18) — on firing, launches ghostly arrows at nearby visible
  enemies (reduced damage via reflective `setDamage`). Conflicts the existing volley.

## Cross-version notes
- `Arrow#getDamage`/`setDamage` are 1.14+ (`AbstractArrow`); not on the 1.12.2
  compile classpath, so phantom-arrow damage scaling is reflective (full damage on
  older servers). `setPierceLevel` is likewise reflective. `setCritical`/`setGlowing`
  are available on all supported versions but still guarded.

### Batch B (trident + mace smash)
- **Devastator** (#24) — falling mace-smash damage amp (`OnHitDamageListener`) +
  ground-quake shockwave with knockback (`OnHitProcListener`, gated on fall
  distance, re-entrancy-guarded).
- **Trident Storm** (#22) — on a thrown trident's impact (`ProjectileHitEvent`,
  matched by `"TRIDENT"` type name to stay 1.12.2-compile-safe): lightning when
  storming, else an AoE shockwave + Slowness. Trident's backing item is read
  reflectively (`getItemStack`/`getItem`; Paper exposes it).
- **Tidecaller** (#23) — trident impact pushes nearby entities outward + Slowness +
  water particles. (Does NOT place water blocks — avoids griefing.)

## Status: 35 / 44
- Remaining Phase 2: Quickshot(20) (needs packet-level draw control — no clean
  API), the reactive specials Spectral Bind(25)/Predator(37)/Soul Tether(40), and
  the banner-driven Killstreak(7)/Executioner's Cry(29)/Battle Roar(34) — these
  need the reflective `HoloFXService`.
- Remaining Phase 3: Endless Hunger(43), Apex Predator(44) + `/enchants settings`.

## Testing
- Maven build: no errors; both jars built. Compile-verified only — projectile
  behavior (pierce, bounce target-finding, phantom-arrow spawning, mark glow
  cleanup) needs in-game verification.

## Suggested Commit Message
```
Feature (enchants): Combat Phase 2A — RangedListener + 5 bow/crossbow enchants
```
