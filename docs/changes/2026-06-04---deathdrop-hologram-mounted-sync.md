# Death-drop hologram floats in perfect sync with the item (mounted, all versions)

■ **Created:** 2026-06-04 5:26 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 5:26 pm (America/Detroit)

The death-item hologram now moves simultaneously with the carry-all item by mounting it as a passenger
instead of chasing it with a per-tick teleport. Build green; both jars; EN/DE/ES parity passes.

---

## Fix

- **Hologram now floats simultaneously with the item.** The previous implementation teleported a
  follower armor stand to the item's position once per tick. A teleported entity isn't
  client-interpolated, so it **jumped a tick behind** the item (which the client *does* interpolate) —
  the name visibly lagged/stuttered, especially during the death-throw arc. The hologram stand is now
  **mounted on the item as a passenger** (`item.setPassenger(stand)`), so the client renders the item
  and its name as a single unit and the name moves in **perfect sync with zero lag**. This works on
  every supported version — `setPassenger(Entity)` is present across 1.12 → 1.21 — and is Folia-safe
  (no scheduler/region task at all). If the stand can't be mounted on a given build, it falls back to
  naming the item itself (still perfectly synced, just lower).
  - The per-tick follow task, the `trackedItems` map, and all the Folia per-entity reposition dispatch
    were removed (no longer needed), simplifying the listener. Pickup still updates the count; full
    pickup / despawn / disable removes the stand; crash orphans are still swept on enable
    (tagged `obx_deathdrop_holo`). The stand is an invisible **marker** armor stand (no hitbox, so it
    never interferes with the item or players).
  - Files: `features/deathdrop/.../listener/DeathDropListener.java` (rewritten to mount instead of
    chase). `DeathDropModule` `start()`/`shutdown()` lifecycle is unchanged.

## Categories Touched
Internal (DeathDropListener).

## Testing
- `./gradlew :core:test` — EN/DE/ES parity **passes** (no message-key changes this batch).
- `./gradlew build` — **BUILD SUCCESSFUL**; both jars produced:
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Fix (deathdrop): mount the hologram on the item so the name floats in perfect sync

Replace the per-tick teleport-follow (which lagged a tick behind the client-interpolated item) with
mounting the marker armor stand as the item's passenger via setPassenger — the client renders them as
one, so the ×count name moves simultaneously with the item with zero lag on all versions (1.12–1.21)
and on Folia (no task). Falls back to the item's own name if mounting fails.
```
