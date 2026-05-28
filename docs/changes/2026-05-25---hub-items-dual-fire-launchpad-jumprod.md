# Hub Items — Dual-Fire Fix, Launchpad-on-Click, Jump-Rod Reel-In

■ **Created:** 2026-05-25 12:09 pm
■ **Last Updated:** 2026-05-25 12:09 pm

## Summary

Fixes the reported hub hotbar item behavior and updates the Admin Panel tooltip.

## Issues & Fixes

### 1 & 3 — Right-click items firing twice (selector double-open, vanish double-toggle)
- **Cause:** On 1.9+ a single right-click fires `PlayerInteractEvent` **twice**
  (once per hand). The dispatcher didn't filter by hand, so the off-hand copy
  ran every handler again — the vanish toggle flipped on→off (looking like it
  did nothing) and the server menu opened twice (the likely source of the
  "teleport-looking" jitter, since the second open re-sends an inventory packet).
- **Fix:** `listener/player/HubItemUseListener.java` now skips the off-hand fire
  via a cached reflective `PlayerInteractEvent.getHand()` (null on 1.8, where the
  event fires once). Every hub item now responds exactly once per click.
- The server-selector compass case is unchanged in intent — it cancels the
  interaction and opens the GUI only; there is no teleport code on that path.

### 2 — Jump-To Rod only worked at close range / on landing
- **Cause:** `HubFishingListener` only teleported on `IN_GROUND`/`CAUGHT_ENTITY`,
  i.e. when the hook actually landed in a block. Cast at typical angles the hook
  arcs down close, so the teleport was always short-range.
- **Fix:** It now teleports on the **reel-in** (the player's 2nd right-click) as
  well as on landing — to wherever the bobber currently is, even mid-air. Only
  the initial cast (`FISHING`) and passive states (`BITE`, `LURED`) are skipped;
  every reel/resolved state (`IN_GROUND`, `CAUGHT_*`, `FAILED_ATTEMPT`, `REEL_IN`)
  teleports to the hook. Range is bounded by `items.jump-rod.max-distance`
  (default 60, beyond the rod's ~33-block reach), so casts up to the rod's max
  distance now work. `src/main/java/dev/sergeantfuzzy/sfcore/listener/player/HubFishingListener.java`.

### 4 — Launchpad did nothing on double-jump or click
- **Cause:** The launch only fired from `PlayerToggleFlightEvent` (double-tap
  space), which needs `allowFlight` granted and the item held — fragile, and a
  no-op in creative / when flight wasn't active.
- **Fix:** Extracted a shared `HubLaunchpadListener.launch(hub, cooldownManager,
  player)` (upward + forward boost from look direction, marks the player launched
  so `HubFallDamageListener` cancels landing fall damage, cooldown-guarded). The
  double-jump path calls it, and `HubItemUseListener` now calls it on a plain
  **right-click** of the launchpad too — so it works regardless of flight state.
  `HubLaunchpadListener.java`, `HubItemUseListener.java`.

### 5 — Admin Panel placeholder tooltip
- `gui/admin/AdminMenu.java` — replaced the vague "Central management hub for
  future SF-Core administrative features." with a short, plain-language
  description:
  - "Your control center for SF-Core."
  - "Manage your server's tools and"
  - "settings from the options here."

## Verification
- `./maven/bin/mvn -DskipTests clean package` builds clean; both obfuscated
  (`SF-Core-1.0.0-SNAPSHOT.jar`) and `-unobf.jar` produced, correctly distinct
  in size.

## Suggested Commit Message

```
Fix (hub): Dedupe right-click (off-hand) fire, launchpad-on-click, jump-rod reel-in teleport; clearer Admin Panel tooltip
```
