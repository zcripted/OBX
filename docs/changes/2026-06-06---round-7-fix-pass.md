# Round 7 Fix Pass — Blockers 2–9 + New Findings (Webhook #1 Deferred Per Request)

■ **Created:** 2026-06-06 4:05 pm

■ **Last Updated:** 2026-06-06 4:05 pm

## Summary

Executes the complete Round 6/7 assessment fix list except item #1 (the shipped Discord
webhook — explicitly deferred by request; **it remains a release blocker** until scrubbed
and revoked).

### Blockers fixed

| # | Fix | Files |
|---|---|---|
| 2 | **DecimalFormat thread-safety** — tablist `TPS_FORMAT` and HealthCommand `ONE_DECIMAL` are now `ThreadLocal<DecimalFormat>` (DecimalFormat is not thread-safe; the tablist refresh runs on Folia region threads). | `features/tablist/.../format/TablistRenderer.java`, `core/.../diagnostics/HealthCommand.java` |
| 3 | **Backpack save guard** — `toBase64` returns `null` on serialization failure (was `""`), and `saveContents` SKIPS the write with a console warning instead of overwriting good contents with emptiness. | `features/backpack/.../service/BackpackService.java` |
| 4 | **PM mute bypass closed** — `blockedByMute` checks by **UUID** (name lookups were name-change bypassable), and the inbox **draft-reply path now runs the mute gate** before delivering. | `features/mail/.../pm/PrivateMessageService.java` |
| 5 | **Warp chat-input quit cleanup** — `WarpMenuInputListener` clears the pending prompt on `PlayerQuitEvent` (was a permanent leak that also swallowed the player's first chat line on rejoin). | `features/warp/.../gui/WarpMenuInputListener.java` |
| 6 | **Backpack off-hand swap guard** — F-key (`SWAP_OFFHAND`) into an open backpack slot is now denied; the ClickType is compared **by name** so the class loads on pre-1.16 API baselines, with a 1.8 try/catch around `getItemInOffHand`. | `features/backpack/.../listener/BackpackListener.java` |
| 7 | **CONFIRM menus take the clicking player** — confirm/cancel actions changed from `Runnable` (capturing the open-time `Player`) to `Consumer<Player>` receiving the click-time player; both call sites updated. | `features/staff/.../gui/AdminSubMenu.java` |
| 8 | **`teleport.cancel-on-move` implemented** — new `PlayerMoveEvent` handler in `TeleportManagerImpl` (already a registered listener): block-level movement during a warmup cancels the pending teleport and sends the existing `teleport.warmup.cancelled` message. Cheap hot path (map empty/containsKey early-outs). | `features/teleport/.../service/TeleportManagerImpl.java` |
| 9 | **Hologram reload hardened** — entity teardown wrapped in try/catch-fallback/finally: a throwing `destroyAll()` now falls back to per-hologram destroys and state reset + `load()` ALWAYS proceed (previously a throw aborted the reload mid-way, orphaning entities and bricking the service). | `features/hologram/.../service/HologramService.java` |

### Round 7 new findings fixed

- **4 missing language keys** added in EN/DE/ES lock-step: `item.unbreakable.enabled`,
  `item.unbreakable.disabled`, `teleport.warp.gui.search-empty`, `teleport.warp.gui.console`
  (each previously rendered as raw key text in-game).
- **Creative-clone key dedupe** — `BackpackService.normalizeKeyItem(...)`: on every
  backpack open, the valid key is collapsed to exactly ONE item of amount 1 (creative
  middle-click could clone/stack it; contents were always single-copy, keys now are too).
- **Economy stats cache** — new `EconomyStats` (3s-TTL volatile snapshot, Folia-safe):
  the Admin-Menu Economy tile and the Economy Control panel tiles read cached
  accounts/supply/top-3/recent-transactions instead of firing 6–10 synchronous SQLite
  queries per second per viewer on the 0.5s refresh cadence.
- **Sell-GUI death guard** — dying with the sell inventory open now dumps its contents
  into the normal death drops (was a keep-inventory bypass: items rode through death and
  returned at the respawn point). Also: closing an EMPTY sell inventory no longer sends
  the "nothing sellable" message.

## Still open (deliberately)

- **#1: live Discord webhook + server/channel IDs in `config.yml`** — deferred per
  request. Must be scrubbed (and the webhook revoked in Discord — it is in git history of
  both repos) before sale.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**: all modules compile, EN/DE/ES parity
  green with the 4 new keys; both jars produced.

## Suggested Commit Message
```
Fix (round 7): thread-safe formats, backpack save+key guards, PM mute UUID, warp prompt cleanup, cancel-on-move, hologram reload, missing lang keys, economy stats cache, sell-GUI death guard
```
