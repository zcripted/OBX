# Hub Item Cross-Plugin Interaction Safeguard

■ **Created:** 2026-05-25 3:28 pm
■ **Last Updated:** 2026-05-25 3:28 pm

## Summary

Adds a material-independent safeguard so other plugins can't apply their own
functions to OBX hub kit items. Motivated by WorldEdit's navigation wand
(a compass: right-click `/thru`, left-click `/jumpto`) teleporting players who
clicked the compass server-selector.

## Change

`src/main/java/dev/zcripted/obx/listener/player/HubItemUseListener.java`
- Listener moved from `EventPriority.NORMAL` to **`EventPriority.LOWEST`** so
  OBX claims the interaction before WorldEdit (and other plugins) handle it.
- For ANY hub item, the interaction is now **cancelled + `useItemInHand`/
  `useInteractedBlock` set to `DENY`** on **both left AND right clicks** (the
  WorldEdit nav wand uses left-click `/jumpto` and right-click `/thru`). Plugins
  that honor a cancelled interact — WorldEdit does; e.g. WorldGuard region
  protection already blocks WE wands the same way — will not act on the item,
  regardless of its material.
- **Only exception:** the jump-rod's right-click is left through, because it must
  run the vanilla fishing cast that powers its teleport. (A fishing rod isn't a
  WorldEdit tool, so this is not a conflict surface.) Left-clicking the jump rod
  is still claimed/blocked.
- New `denyVanillaUse(event)` helper sets both interaction results to `DENY`.

## Scope / Notes
- Left-clicks on hub items now do nothing (they're claimed). In a hub world the
  items have no legitimate left-click use, and attacking entities is a separate
  event, so combat is unaffected.
- This protects against plugins that respect the interact result. A plugin that
  ignores cancellation entirely and acts purely on material can still only be
  fully avoided by not sharing that material — which is why the default
  server-selector material is `NETHER_STAR` (see the previous change). The two
  layers together cover both well-behaved and material-only plugins.

## Verification
- `./maven/bin/mvn -DskipTests clean package` builds clean; obfuscated and
  `-unobf` jars produced.

## Suggested Commit Message

```
Feature (hub): Claim hub-item interactions at LOWEST + deny vanilla use (both clicks) so other plugins (WorldEdit wands) can't act on kit items
```
