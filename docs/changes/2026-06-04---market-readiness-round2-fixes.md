# Market-Readiness Round 2 — Fix Batch

■ **Created:** 2026-06-04 1:47 am (America/Detroit)

■ **Last Updated:** 2026-06-04 1:47 am (America/Detroit)

Follow-up to `2026-06-04---market-readiness-fixes.md`. A second full assessment (5 parallel
auditors across all 20 areas, each verifying the first batch and hunting fresh) surfaced 5
serious issues the first fix-list didn't cover — including one regression introduced by the
round-1 anti-forge work. This batch fixes everything from BLOCKER down to actionable LOW.

Build green (`./gradlew build`); both jars produced (`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`);
EN/DE/ES parity test passes (2 new keys: `jail.containment`, `kit.inventory-full`).

> **Still deliberately out of scope:** the live Discord webhook token in `config.yml` (owner's
> external rotation; not authorized). Untouched.

---

## BLOCKER / HIGH

### Enchant — strict anti-forge mode bricked all scrolls & books (regression from round 1)
Scrolls/books write their enchant lore via `renderLine` and never pass through `storage.apply`,
so they carried **no HMAC signature**. Under `trust_unsigned_lore: false`, `storage.read` returned
empty for them → `payloadEnchant`/`payloadLevel` null → every scroll/book inert; and the apply path
could re-sign hand-forged lore, bypassing strict mode entirely.
- `EnchantStorage.signatureLineFor(id, level)` — new public helper emitting the invisible signature line.
- `EnchantItems.scroll()` / `book()` — now stamp that signature, so payloads verify in strict mode.
- `EnchantStorage.split()` — in strict mode, drops an existing enchant set that isn't validly
  signed instead of carrying it forward, so a forged set can't be blessed by a later legitimate write.

Files: `features/enchant/.../storage/EnchantStorage.java`, `features/enchant/.../item/EnchantItems.java`.

### Hologram — editor GUI item-loss + dead delete button
The editor menu cancelled clicks but had **no `InventoryDragEvent` handler** (dragged items lost on
close) and guarded on a spoofable title string; its delete button called a non-existent `sfholo`
command (rebrand miss).
- Converted to a dedicated `InventoryHolder` (identifies the menu by ownership, carries the hologram
  id — removes the never-cleaned `openFor` map / its small leak).
- Added an `InventoryDragEvent` handler; click handler now cancels collect-to-cursor / shift / number-key.
- Fixed `sfholo delete` → `holo delete`.

Files: `features/hologram/.../gui/HologramEditorMenu.java`.

### Jail — no movement containment (jailed players could walk out)
There was no `PlayerMoveEvent` handler at all; jail only restrained teleports/commands.
- `JailListener.onMove` — cache-backed (no DB on the move path); pulls a strayed player back to the
  anchor via `event.setTo(...)` when they cross the containment radius or change world.
- `JailService.getJailAnchor(uuid)`, `getContainmentRadius()` (configurable via `containment-radius`
  in `jails.yml`, default 10, min 2).
- New message `jail.containment` (EN/DE/ES).

Files: `features/jail/.../listener/JailListener.java`, `features/jail/.../service/JailService.java`,
`core/.../language/MessageDefaults{EN,DE,ES}.java`.

### Economy — Vault provider returned dishonest responses
`createPlayerAccount` returned `true` without creating anything; `depositPlayer` returned `SUCCESS`
even on a no-op or negative amount — Vault-backed shops/jobs could mis-account.
- `createPlayerAccount` now calls `economy.ensureAccount` (false on unknown player).
- `depositPlayer` rejects non-positive/NaN/Inf with `FAILURE`, measures the real balance delta, and
  reports `FAILURE` when nothing moved.

Files: `features/economy/.../VaultEconomyProvider.java`.

### Enchant — bloodletter bleed silently dead on Folia
`applyBleed` scheduled via `runRepeating` (global region), then touched the victim entity
cross-region → swallowed `IllegalStateException` → bleed never ticked on Folia.
- The repeating task now dispatches each tick via `runAtEntity(victim, …)` on Folia (inline on Bukkit),
  matching the scoreboard/tablist pattern.

Files: `features/enchant/.../listener/OnHitProcListener.java`.

## MEDIUM

- **Chat color/MiniMessage injection ungated.** Normal chat was the one user-text path reaching
  MiniMessage without honoring `obx.message.color`; `allow-formatting-in-messages: true` also handed
  every player full MiniMessage incl. `<click:run_command>`.
  - `MessageSanitizer.sanitizeChat(sender, raw, allowFormatting)` — strips `&`/`§` for players without
    `obx.message.color`; neutralizes all tags unless formatting is allowed AND the sender has color
    permission, in which case only the interactive tags (`<click>`/`<hover>`/`<insert>`) are stripped.
  - `MessageSanitizer.neutralizeTags(...)` — used to neutralize the `{displayname}` placeholder (a
    third-party plugin could set a display name containing tags).
  - `ChatManagementListener.buildPlaceholders` rewired to both.
  Files: `core/.../util/text/MessageSanitizer.java`, `features/chat/.../listener/ChatManagementListener.java`.
- **Hub launchpad action-bar + join/respawn kit not region-correct on Folia.** Dispatched per-player
  via `runAtEntity` / `runAtEntityLater`. Files: `features/hub/.../launchpad/LaunchpadCooldownManager.java`,
  `features/hub/.../listener/HubJoinListener.java`.
- **Hologram interaction dispatched from global region on Folia.** `HologramPacketHandler` now uses
  `runAtEntity(viewer, …)` instead of `runNow`. Files: `features/hologram/.../packet/HologramPacketHandler.java`.
- **Mute state hit SQLite on every command from every player.** Added an in-memory mute cache
  (join-load / quit-evict / lazy-fill, mirroring jail) and reordered `MuteCommandListener` to check the
  cheap blocked-command set before the (now cached, UUID-keyed) mute lookup.
  Files: `features/moderation/.../service/ModerationService.java`, `features/moderation/.../listener/MuteCommandListener.java`.
- **Scoreboard `setPrefix` lacked the truncate fallback** its `setSuffix` has (silently dropped a line
  on pre-1.13 when too long). Mirrored the fallback. Files: `features/scoreboard/.../format/ScoreboardRenderer.java`.

## LOW

- **Kit — overflow item-loss after cooldown consumed.** Added `KitService.hasRoomFor(...)`; `/kit`
  refuses a full inventory with `kit.inventory-full` (EN/DE/ES) **before** claiming the cooldown.
- **Kit — removed dead `markUsed()`** (a loaded footgun that would re-open the just-fixed dupe race).
- **Mail — online-ignore path now social-spies** (consistent with the offline path; spies are invisible
  to the sender so the ignore still can't be detected). File: `features/mail/.../pm/PrivateMessageService.java`.
- **Commands — `/clear` no longer over-blocks `obx.clearinv.others`-only holders.** The override gate
  now defers to the executor's own checks for commands with finer-grained sub-permissions.
  File: `core/.../command/CommandOverrideListener.java`.
- **Warp GUI click** now uses the top-inventory holder (consistent with the new drag handler) instead
  of the ambiguous `getInventory()`. File: `features/warp/.../gui/WarpMenuListener.java`.
- **Nickname** impersonation check now also blocks an offline player's real name via Paper's
  non-blocking `getOfflinePlayerIfCached`. File: `features/nickname/.../service/NicknameService.java`.
- **Tablist** drops its now-inert main-board grouping teams once when the scoreboard feature takes over
  grouping. File: `features/tablist/.../format/TablistRenderer.java`.
- **Jail** offline cache edge: `jail()` only caches state for online targets now.

## Accepted tradeoffs (documented, not changed)
- **Bleed protection probe** fires a synthetic `EntityDamageEvent`; a third-party listener that *acts*
  on raw damage events could theoretically react to it. Using a real event is the most compatible way
  to respect region/PvP protection; the event is `CUSTOM`/no-damager so it isn't OBX-reprocessed.
- **`/eco give`** still reports success when the deposit clamps at the max balance (admin-only cosmetic).
- **CLAUDE.md** documents a `def(key,en,de)` / `spanishOverrides()` API that doesn't exist in the code
  (the real impl is three explicit `MessageDefaults{EN,DE,ES}` catalogues, parity enforced by test).
  Left as-is — it is your authoritative instruction file, not mine to rewrite.

## Strict-mode migration note (carry-over)
With `enchant.security.trust_unsigned_lore: false`, an existing **unsigned** (legacy) enchanted item —
gear *or* scroll minted before this update — reads as unenchanted, and applying a new enchant to an
unsigned item drops its other unsigned enchants (they can't be trusted). Flip to strict only after
items have been re-saved once (any legitimate edit re-signs). Default (`true`) is unaffected.

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` (incl. EN/DE/ES parity) + `:features:economy:test` pass.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` (obfuscated) + `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message
```
Fix (market-readiness r2): scroll signing, jail containment, Folia, Vault, mute cache

Round-2 audit fixes: sign enchant scrolls/books + drop unsigned sets on write (strict
anti-forge); hologram editor drag-guard + holder + holo-delete; jail movement containment;
honest Vault createPlayerAccount/depositPlayer; bleed/hub/hologram Folia region dispatch;
permission-gated chat color/MiniMessage; in-memory mute cache; scoreboard prefix truncate;
kit full-inventory pre-check; mail ignore social-spy; /clear .others gate; warp/nick/tablist polish.
```
