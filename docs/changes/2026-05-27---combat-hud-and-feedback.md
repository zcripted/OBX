# Combat HUDs + Feedback (Berserker / Bloodletter / Battle Roar / Bloodthirst)

■ **Created:** 2026-05-27 1:15 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 1:15 pm (America/Detroit)

---

## Summary

Adds live combat HUDs and richer feedback to four enchants, plus two genuine bug
fixes (Berserker Strength never applying; Bloodletter using the grey "heart" hit
particle instead of blood). Per the user's design choices: above-target HUDs use
**follower-hologram armor stands for all targets** (mobs and players), and the
Berserker damage-percent HUD uses an **action-bar line**.

## Categories

### Internal — shared HUD infrastructure
- `enchant/service/CombatHudService.java` (new) — one repeating updater task drives:
  - **Follower holograms**: an invisible marker armor stand that trails any target
    (mob or player), rebuilt each tick from active contributions (health bar / blood-loss
    tag / Battle Roar Weakness-Slowness readout), each with its own expiry; removed when
    all expire. `trackHealth` / `trackBleed` / `trackDebuff`.
  - **Player action-bar HUD**: composes Berserker (`+X% damage`) and the animated
    Bloodthirst "Restoring +N% [|||··]" loading segment, refreshed every 4 ticks.
  - Per-entity work is try/caught (Folia off-region access just skips a tick); `clear()`
    removes all stands and cancels the task on disable.
- `enchant/util/Particles.java` — new `block(...)` spawns block-fragment particles
  (resolves `BLOCK_CRACK`/`BLOCK`/`BLOCK_DUST` + `BlockData`/`MaterialData` reflectively),
  used for blood (`REDSTONE_BLOCK` shards).
- `enchant/service/CombatParticleService.java` — `spawnBlood` (bleed spray) and
  `spawnBloodBurst` (death splatter).
- `enchant/listener/CombatSupport.java` — `title(...)` helper (cross-version `sendTitle`,
  honors the per-player FX opt-out).
- `Main.java` — constructs/starts `CombatHudService`, injects it into the combat
  listeners, and clears it on disable.

### Enchants
- **Berserker's Rage** — **Bug fix:** Strength now applies whenever you're fighting with
  the enchant (refreshed each hit, so it lands from the first hit), Strength I normally and
  Strength II once below the level's `strength_below` HP threshold (Lv4). Previously
  Strength only triggered at Lv4 below 25% HP, so a normal axe never showed it. Adds the
  **action-bar damage-% HUD** and a **health bar above the target** during the 6 s combat
  window.
- **Bloodletter** — **Bug fix:** bleed particles were `DAMAGE_INDICATOR` (the grey heart
  hit-particle); now real **red blood** shards that fall, on the initial hit and every
  bleed tick. Adds a **live blood-loss HUD** (health bar + `-X/s`) above the target that
  drops accurately as the bleed ticks, and on death a **blood splatter** + a `☠ SLAIN`
  title/subtitle to the killer.
- **Battle Roar** — affected foes now show a floating **Weakness/Slowness readout** above
  them (level + remaining time, read live from their effects).
- **Bloodthirst** — while actively healing in combat, an **animated "Restoring +N%"
  action-bar loading bar** (percent scales with level); the Lv4 on-kill heart bonus now
  fires a `❤ BLOODTHIRST` kill-bonus title/subtitle.

## Assumptions / notes (per CLAUDE.md)
- "HUD above the action bar" (Berserker) is realized as an **action-bar line** (user choice);
  Berserker + Bloodthirst share one composed line so they don't fight for the bar.
- Above-target HUDs are **follower armor stands** visible to everyone (user choice). On
  Folia, the global updater may not touch an off-region entity on a given tick (it's skipped
  and retried); this is the accepted trade-off of the follower approach.
- Combat-FX text follows the existing inline (non-localized) combat-FX convention; titles
  honor `/enchants settings` via `CombatSupport.title`.

## Testing
- Maven build: exit 0, both jars (obf ~621 KB, unobf ~903 KB). ProGuard `Note:` lines only.
  Compile-verified only. In-game checks: Berserker Strength now present from the first hit
  (+ HUD/health bar); Bloodletter shows red blood + a dropping blood-loss bar and a death
  splatter/title; Battle Roar shows the debuff readout over foes; Bloodthirst shows the
  animated heal bar + Lv4 kill-bonus title. Verify holograms despawn after the window and
  on disable.

## Suggested Commit Message
```
Feature (enchants): Combat HUDs (follower holograms + action bar) for Berserker/Bloodletter/Battle Roar/Bloodthirst; fix Berserker Strength + Bloodletter blood particles
```
