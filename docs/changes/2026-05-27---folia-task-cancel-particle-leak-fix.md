# Fix — Leaking combat particle tasks (Folia cancel reflection)

■ **Created:** 2026-05-27 (America/Detroit)

■ **Last Updated:** 2026-05-27 (America/Detroit)

---

## Symptom

After using an Apex Predator (or any kill/hit that spawns a combat aura — Soulreaver,
Endless Hunger milestone, Executioner's Cry shockwave, etc.) the gold/flame aura
particles **followed the player forever**, persisting after the weapon was removed from
the inventory.

## Root cause

On **Folia**, `GlobalRegionScheduler.runAtFixedRate(...)` returns a `ScheduledTask` whose
concrete implementation class is **not public**. `SchedulerAdapter.FoliaCancellableTask`
resolved `cancel()`/`isCancelled()` via `delegate.getClass().getMethod(...)` and invoked
them — which throws `IllegalAccessException` (the *declaring* class isn't accessible),
caught and swallowed by `catch (Throwable ignored)`. So **the cancel silently failed**:
the self-cancelling combat aura repeating task never stopped and kept emitting particles
every 4 ticks for as long as the player stayed online. (Same class of bug noted in
`obfuscation-reflection-gotcha` — reflecting against an inaccessible class.)

## Categories

### Internal — scheduler
- `platform/scheduler/SchedulerAdapter.java` — `FoliaCancellableTask` now resolves
  `cancel`/`isCancelled` from the **public** `io.papermc.paper.threadedregions.scheduler.ScheduledTask`
  interface (with `setAccessible(true)`), so the cancel actually takes effect on Folia.
  This fixes leaks for **every** self-cancelling repeating task (auras, shockwaves, …),
  not just Apex.

### Internal — defense-in-depth (particles)
- `enchant/service/CombatParticleService.java`:
  - `spawnAura` / `spawnShockwave` now check expiry **before** spawning and `return`
    (stop) once the effect's lifetime is up or the player left — so even a future flaky
    cancel can't keep emitting particles.
  - Both bodies are wrapped in `try/catch` that stops the task on any thrown exception
    (a thrown repeating-task body would otherwise keep repeating).
  - Live timed-effect tasks are tracked in a set; new `clear()` cancels them all.
- `Main.java` — `onDisable` now calls `combatParticles.clear()` alongside `holoFX.clear()`.

## Player remediation
Already-leaked tasks from before the fix are cleared by a server **restart** or
`/obx reload` (plugin disable cancels all of its scheduler tasks), or by **relogging**
(the aura body is guarded by `player.isOnline()`, so it stops emitting once the player
leaves). After this build, the auras self-terminate on schedule.

## Not bugs (verified)
- **Scroll drop:** Apex's harvest is **5% per kill**. Over 20 kills the chance of seeing
  at least one is ~64% — so zero drops in 20 kills is ordinary variance (~36%), not a
  failure. The drop path (`OnKillListener.dropRandomScroll`) is correct: it picks a random
  Rare/Epic enchant and drops a scroll at the victim's location.
- **"Only one Apex active":** the action-bar warning is informational. Combat enchants only
  read the **held (main-hand)** weapon, so a second Apex item in the inventory is already
  inert — the warning just surfaces that; it doesn't add separate suppression.

## Testing
- Maven build: exit 0, both jars produced (obf ~612 KB, unobf ~890 KB). ProGuard `Note:`
  lines only. Compile-verified. In-game: on Folia, confirm the aura now stops ~0.6 s after
  the last kill and does not survive item removal.

## Suggested Commit Message
```
Fix (enchants): Folia ScheduledTask cancel via public interface — stop leaking combat aura particle tasks
```
