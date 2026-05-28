# Universal Combat Health Hologram + Multi-line HUD + Bonecrusher Feedback

■ **Created:** 2026-05-27 1:55 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 1:55 pm (America/Detroit)

---

## Summary

The during-combat target **health hologram now appears for ALL combat-category
enchants** (not just Berserker/Bloodthirst), the follower hologram became
**multi-line** (health on line 1, a Weakness/Slowness cooldown on line 2), and
**Bonecrusher** gained per-swing damage, a shield-shatter chat popup, and the
Weakness cooldown readout.

## Categories

### Internal — HUD
- `enchant/service/CombatHudService.java` — `EntitySession` now renders **stacked
  armor-stand lines**: line 1 = health bar (+ Bloodletter blood-loss tag), line 2 =
  the live Weakness/Slowness readout (level + remaining seconds, e.g. `Weakness I (1.0s)`).
  Stands are spawned/removed to match the active line count and repositioned stacked
  each tick. The Battle Roar debuff readout now lands on this second line too.
- `enchant/listener/CombatSupport.java` — new `hasCombatEnchant(service, item)`.

### Universal health hologram (all combat enchants)
- `enchant/listener/OnHitDamageListener.java` — once the attacker's weapon carries any
  Combat-category enchant, the target's health hologram is shown for the ~6 s combat
  window (covers every melee combat enchant; Berserker's now-redundant explicit call
  removed).
- `enchant/listener/RangedListener.java` — same for bow/crossbow hits (CombatHudService
  injected; `Main` registration updated).

### Bonecrusher (`enchant/listener/OnHitProcListener.java`)
- **Per-swing damage** shown on the attacker's action bar: `Bonecrusher » 12.5 damage`.
- **Shield shatter chat popup** to both sides when the Lv4 shield-disable procs.
- **Weakness cooldown** added to the target hologram's second line via `trackDebuff`
  (live countdown of the remaining Weakness).

## Notes
- Multi-line holograms use up to two stacked marker armor stands per target; both are
  cleaned up on expiry and on disable. Same Folia caveat as before (off-region targets
  are skipped for a tick).
- Bonecrusher's swing-damage and Berserker's percent both use the action bar; on a weapon
  carrying both, the ~0.5 s Berserker refresh and the per-swing Bonecrusher line may
  alternate. Acceptable; can be merged later if desired.

## Testing
- Maven build: exit 0, both jars (obf ~622 KB, unobf ~905 KB). ProGuard `Note:` lines only.
  Compile-verified. In-game: any combat-enchanted weapon should float the target health bar;
  Bonecrusher should show swing damage, a shield-shatter chat message, and a `Weakness I (Xs)`
  second line that counts down.

## Suggested Commit Message
```
Feature (enchants): universal combat health hologram + multi-line target HUD; Bonecrusher swing damage / shield popup / weakness cooldown
```
