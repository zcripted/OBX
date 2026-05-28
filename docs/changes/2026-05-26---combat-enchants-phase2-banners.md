# Combat Enchantments — Phase 2 (Banner Batch + HoloFXService)

■ **Created:** 2026-05-26 8:55 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 8:55 pm (America/Detroit)

---

## Summary

Adds the holographic-FX foundation (`HoloFXService`) and the three banner-driven
combat enchants that depended on it. Brings the Combat expansion to **38 / 44**.

## Categories

### Internal — holographic FX
- `enchant/service/HoloFXService.java` (new) — short-lived floating text via
  invisible marker armor stands (works 1.8.8 → 1.21.x, no Display reflection).
  Exposes `showDamageNumber` (tinted by `DamageType`), `showKillBanner`, and
  `showFloatingText`. Gated by `combat_global.kill_banners` / `damage_numbers`;
  per-player active-count cap derived from `combat_global.max_holograms_per_player`;
  `clear()` removes all on disable. Removal is scheduled on the hologram's own
  region via the new `SchedulerAdapter#runAtEntityLater` so it is Folia-safe.
- `platform/scheduler/SchedulerAdapter.java` — added `runAtEntityLater(Entity,
  task, retired, delayTicks)` (Folia entity scheduler delayed, else Bukkit
  `runTaskLater`) + `FoliaSchedulers.runAtEntityDelayed`.
- `enchant/effect/CombatState.java` — added a `Streak` store with
  `registerKill`/`killstreak` (windowed) and a generic per-player cooldown store
  (`onCooldown`/`cooldownSeconds`/`setCooldown`); both cleared in `clear()`.
- `Main.java` — constructs `holoFX`, injects it into `OnHitDamageListener` and
  `OnKillListener`, and calls `holoFX.clear()` in `onDisable`.

### Enchants (combat.yml)
- **Killstreak** (#7) — `WEAPON`, rare, max 5. Chaining kills within an 8s window
  builds a streak that scales outgoing damage (`per_step` × streak, in
  `OnHitDamageListener`); each kill shows a banner via `OnKillListener`
  (KILLSTREAK ×N, **RAMPAGE** at ×5+). Conflicts `combo_strike`/`soulreaver`
  (combo-systems group).
- **Executioner's Cry** (#29) — `AXE`/`MACE`, legendary, max 3. Each kill terrifies
  nearby foes (Slowness, + Weakness at Lv2+) within a scaling radius, with a smoke
  shockwave, fear sound, and an EXECUTIONER banner (`OnKillListener`).
- **Battle Roar** (#34) — `SWORD`/`AXE`/`MACE`, epic, max 3. **Sneak + right-click**
  the weapon to roar: self Strength (+ Resistance at Lv3) and a Weakness/Slowness
  debuff on nearby enemies, on a per-player cooldown (60/45/30s). New
  `PlayerInteractEvent` handler in `OnHitDamageListener`.

## Cross-version notes
- `HoloFXService.setPersistent` is 1.14+ (reflective; short TTL handles older
  versions). `PlayerInteractEvent#getHand()` is 1.9+ — resolved via a cached
  reflective `GET_HAND` (treated as main-hand on 1.8 so Battle Roar still fires
  once), matching `HubItemUseListener`'s approach.

## Status: 38 / 44
- Remaining Phase 2: Quickshot (#20, needs packet-level draw control — no clean
  API), and the reactive specials Spectral Bind (#25), Predator (#37),
  Soul Tether (#40).
- Remaining Phase 3: Endless Hunger (#43), Apex Predator (#44) +
  `/enchants settings` per-player FX toggle.

## Testing
- Maven build: exit 0, both jars produced (obf ~587 KB, unobf ~853 KB). ProGuard
  `Note:` lines only. Compile-verified only — banner spawning, fear debuffs, the
  Battle Roar sneak+right-click trigger, cooldown messaging, and hologram cleanup
  need in-game verification.

## Suggested Commit Message
```
Feature (enchants): Combat Phase 2 banners — HoloFXService + Killstreak/Executioner's Cry/Battle Roar
```
