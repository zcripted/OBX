# Combat Enchantments — Phase 2 (Reactive Specials)

■ **Created:** 2026-05-26 9:25 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 9:25 pm (America/Detroit)

---

## Summary

Adds the three stateful "reactive special" combat enchants — Spectral Bind,
Predator, and Soul Tether — behind a shared `ReactiveSpecialsService` driven from
both the melee and bow hit sites. Brings the Combat expansion to **41 / 44**
(only Quickshot #20 remains in Phase 2).

## Categories

### Internal — reactive-specials framework
- `enchant/service/ReactiveSpecialsService.java` (new) — owns the mechanics and the
  recurring FX (chain line, predator tracking line, tether line) on the Folia-aware
  `SchedulerAdapter`; each visual self-cancels when its window elapses or an endpoint
  leaves. Single `onHit(attacker, victim, weapon)` entry point so SWORD and BOW share
  one code path.
- `enchant/listener/ReactiveSpecialsListener.java` (new) — routes events: melee
  attach (`EntityDamageByEntityEvent`, non-projectile), Soul Tether backlash
  (`MONITOR`, siphons sourceless damage to the binder — no re-trigger, scheduled on
  the binder's region), and the Spectral Bind Lv3 ender-pearl lock
  (`ProjectileLaunchEvent`, cancel-only so it's dupe-safe).
- `enchant/listener/RangedListener.java` — now tags the firing bow for the three new
  enchants and calls `reactive.onHit(shooter, victim, bow)` on a bow hit.
- `enchant/listener/OnHitDamageListener.java` — adds the Soul Tether vulnerability to
  the outgoing-damage multiplier (one line, beside Hunter's Mark).
- `enchant/effect/CombatState.java` — adds a `Tether` store
  (`tether`/`tetherVulnerability`/`tetherBinder`/`tetherBacklash`) and a pearl-lock
  store (`lockPearls`/`pearlsLocked`); `clear()` drops tethers where the player was
  target **or** binder.
- `Main.java` — constructs `reactiveSpecials`, injects it into `RangedListener`, and
  registers `ReactiveSpecialsListener`.

### Enchants (combat.yml)
- **Spectral Bind** (#25) — `SWORD`/`BOW`, epic, max 3. A hit applies Slowness for
  the bind window (Slowness II/III/IV ≈ the spec's 30/50/70% feel), a periodic
  pull-back toward the binder at Lv2+, and an ender-pearl lock at Lv3, with a cyan
  chain line and a `BLOCK_CHAIN_PLACE` sound.
- **Predator** (#37) — `BOW`/`SWORD`, epic, max 3. Paints a red tracking line to the
  last target for 5/8/12 s; Lv2 adds the target's HP to the attacker's action bar,
  Lv3 adds its speed. (Particle trails render through walls client-side, giving the
  "see through walls" feel without packets.)
- **Soul Tether** (#40) — `SWORD`/`BOW`, legendary, max 3. Tethers the target: all
  weapon attacks deal +25/40/60% to it, while 10/15/20% of its outgoing damage is
  siphoned back to the binder; Lv3 glows the tethered target. Faint purple tether
  line between the two.

## Assumptions (documented per CLAUDE.md)
- **Spectral Bind slow** is implemented as a flat Slowness effect for the whole bind
  rather than a direction-gated "only when moving away" slow — there's no clean
  cross-version per-tick directional-slow API; the pull-back covers the "stop them
  fleeing" intent.
- **Soul Tether vulnerability** reads through `OnHitDamageListener`, so it amplifies
  **weapon** attacks by players (same limitation as Hunter's Mark). Mob/fist hits on
  the tethered are unaffected.
- **Predator "see through walls"** is satisfied by particle visibility (particles are
  not occluded client-side); no glowing/packet tracker is used.

## Cross-version notes
- `setGlowing` is 1.9+ (guarded). `ProjectileLaunchEvent`/`EnderPearl` exist on all
  supported versions. Velocity sets for the pull are wrapped (region-restricted on
  Folia). The recurring visuals follow `CombatParticleService`'s `runRepeating`
  precedent.

## Status: 41 / 44
- Remaining Phase 2: **Quickshot (#20)** — no clean server-side API to speed up bow
  draw/reload; needs a design decision (ProtocolLib, or a creative approximation
  such as a post-shot reload-haste / auto-nock). I'll surface concrete options.
- Remaining Phase 3: Endless Hunger (#43), Apex Predator (#44) +
  `/enchants settings` per-player FX toggle (lore-encoded persistent counters).

## Testing
- Maven build: exit 0, both jars produced (obf ~598 KB, unobf ~869 KB). ProGuard
  `Note:` lines only. Compile-verified only — the bind slow/pull, pearl lock,
  predator tracking line + HP/speed readout, and the tether vulnerability/backlash
  loop all need in-game verification (especially the backlash not looping and the
  pearl-cancel not duping across versions).

## Suggested Commit Message
```
Feature (enchants): Combat Phase 2 reactive specials — Spectral Bind / Predator / Soul Tether
```
