# Combat Enchantments — Phase 1 (Foundation + Batch 1)

■ **Created:** 2026-05-26 6:33 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 6:33 pm (America/Detroit)

---

## Summary

First increment of the 44-enchant Combat expansion. Lands the shared
infrastructure the whole expansion needs, plus the first 6 working enchants
(damage-modifier / kill-stacking family). Phased per agreement; holographic FX,
ranged rewrites, and persistent capstones come in later phases.

## Decisions applied (from requester)
- Persistent counters (#43/#44) will be **lore-encoded** (no PDC — this module is
  lore-only for 1.8→1.21 single-jar). Built in Phase 3.
- Holographic FX is **phased after** mechanics.
- **Phased delivery**, compile-verified per phase.

## Codebase deviations from the prompt (intentional)
- **`combat_global` lives in `enchants/config.yml`, not `combat.yml`.** `combat.yml`
  is a roster file — its top-level keys are parsed as enchant definitions, so a
  settings block there would log "unknown category". config.yml is the module's
  settings file (alongside `apply`/`pvp`/`conflict_groups`).
- **No `EnchantCooldownManager` added** — the existing `EnchantState` already is a
  per-`(playerUUID, key)` cooldown store; it will be reused for cooldown enchants.
- **`SCYTHE`** isn't an item tag → aliased to `SWORD` (Soulreaver). **`MACE`** is a
  valid tag (runtime name-matched; harmlessly never matches pre-1.21).

## Categories

### Internal — shared combat infrastructure (new)
- `enchant/util/SoundPalette.java` — named, version-safe combat sound palette.
- `enchant/service/CombatParticleService.java` — trail/ring/spiral/shockwave/aura
  helpers (Folia-safe scheduler for timed FX; honors `particle_intensity`).
- `enchant/effect/CombatState.java` — per-player Soulreaver kill-stacks (with
  decay) and sprint tracking (Momentum); cleared on quit.
- `enchant/service/CombatSettings.java` — immutable view of `combat_global`
  (damage_numbers, kill_banners, action_bar_feedback, particle_intensity,
  sound_volume_multiplier, max_holograms_per_player). Loaded by `EnchantService`
  (`getCombatSettings()`).
- `enchant/listener/CombatSupport.java` — shared helpers (attacker resolution,
  main-hand, health math, nearby-hostile count, crit approximation, config-gated
  action-bar/sound output).

### Internal — split listeners (new)
- `enchant/listener/OnHitDamageListener.java` — Berserker's Rage, Momentum, Wrath
  of the Wild, Soulreaver (stack consumption), Crit Mastery, Lifesteal; plus the
  damage portions of Frostbrand (flat frost), Hellforged (non-Nether bonus), and
  Bonecrusher (undead bonus). Also tracks sprint and clears state on quit.
- `enchant/listener/OnKillListener.java` — Soulreaver stack gain on kill, Wrath
  of the Wild cluster heal.
- `enchant/listener/OnHitProcListener.java` (batch 2) — status/debuff/elemental
  procs that run at HIGHEST (after damage): Concussion, Frostbrand (chill + freeze),
  Hellforged (ignite), Bonecrusher (armor-durability on crit + Weakness + shield
  disable), Voidstrike (blink + nausea/blindness), and Bloodletter (armor-bypass
  bleed DoT scheduled per victim, refreshable). `Potions` gained `NAUSEA`/`BLINDNESS`;
  `CombatSupport` gained `setFreezeTicks`, `isUndead`, `isNetherFoe`.
- All registered in `Main` (with `CombatState` + `CombatParticleService`).

### Config / definitions
- `enchants/config.yml` — new `combat_global` block + 7 new `conflict_groups`
  (fire_frost, cc_displacement, cc_hard, lifesteal_family, combo_systems,
  ranged_pierce, cleave_aoe).
- `enchants/combat.yml` — batch 1: **Lifesteal**(30), **Crit Mastery**(35),
  **Berserker's Rage**(4), **Wrath of the Wild**(10), **Momentum**(41),
  **Soulreaver**(1). batch 2: **Bloodletter**(3), **Concussion**(6),
  **Frostbrand**(11), **Hellforged**(12), **Bonecrusher**(15), **Voidstrike**(13).
  batch 3: **Phantom Edge**(5), **Glasscutter**(42), **Mirror Edge**(14),
  **Stunlock**(32), **Whirlwind**(33), **Cleave**(39).
  batch 4: **Tempest Strike**(2), **Quickdraw**(9), **Headsplitter**(8),
  **Combo Strike**(26), **Manaburn**(31), **Brawler's Grit**(36), **Vengeance**(38).
  batch 5: **Plunderer**(28), **Reaper's Toll**(27).

### Batch 5 notes
- `OnDeathListener` extended: Plunderer multiplies common drops + chance at an
  extra drop and a random Arcanum scroll (via `EnchantItems` + a random registry
  enchant). Reaper's Toll drops a marker-encoded "Lost Soul" item on kill; a new
  `EntityPickupItemEvent` handler detects it, cancels the pickup, and grants
  Regeneration (+ Absorption at Lv4). Soul level is encoded in a hidden lore
  marker (`Arcanum Soul L<level>`) and read back on pickup.

### Batch 4 notes
- New `OnDeathListener` (Headsplitter head drops — player + common mob heads,
  reflective owner-skin set).
- `CombatState` extended: weapon-swap window (Quickdraw), combo counter (Combo
  Strike), banked next-swing bonus (Manaburn), reactive grit window (Brawler's
  Grit), and last-attacker memory (Vengeance, with a one-shot guaranteed crit).
- Victim-side reactions (in the player-being-hit branch): Brawler's Grit readies,
  Vengeance marks + optionally glows the attacker.
- Tempest Strike crit knockback reuses the `inSecondary` re-entrancy guard so its
  AoE damage can't cascade.

### Batch 3 notes
- Armor penetration (Phantom Edge full, Glasscutter partial) via the deprecated-but-
  functional per-`DamageModifier` API (ARMOR + MAGIC), guarded. Glasscutter Lv4 can
  shatter one armor piece (`CombatSupport.shatterRandomArmor`).
- Whirlwind rewrites sweep-attack damage to a percent of the weapon's real attack
  damage (via `WeaponAttributes`) — caught on `DamageCause.ENTITY_SWEEP_ATTACK`.
- Mirror Edge: a landed hit queues a reflect (`CombatState`); the next incoming blow
  reflects a percent back to the attacker (victim-side, before the attacker branch).
- Stunlock: movement-lock via heavy auto-expiring Slowness/Fatigue (no scheduled
  AI re-enable, so a Folia scheduling failure can't freeze a mob permanently) +
  a stun window read by Stunlock's bonus damage.
- Cleave: forward-cone secondary damage with a re-entrancy guard (`inCleave`) so its
  own hits can't cascade into more cleaves.

## Status
- **27 / 44** combat enchants implemented. **Phase 1 melee mechanics are COMPLETE**
  — foundation + 4 listeners (`OnHitDamageListener`, `OnHitProcListener`,
  `OnKillListener`, `OnDeathListener`).
- Remaining ranged/holo (Phase 2): Hunter's Mark(16), Piercing Shot(17), Phantom
  Volley(18), Sniper's Eye(19), Quickshot(20), Ricochet(21), Trident Storm(22),
  Tidecaller(23), Devastator(24), Spectral Bind(25), Predator(37), Soul Tether(40),
  Killstreak(7), Executioner's Cry(29), Battle Roar(34) + `HoloFXService`/`RangedListener`.
- Remaining persistent (Phase 3): Endless Hunger(43), Apex Predator(44) + `/enchants settings`.

## Testing
- Maven build: no errors; obfuscated + unobfuscated jars built.
- Per-enchant in-game verification still required (cannot live-test here) — see the
  master testing checklist; this increment should be smoke-tested before Phase 2.

## Suggested Commit Message
```
Feature (enchants): Combat expansion Phase 1 — shared FX/state foundation + 6 enchants
```
