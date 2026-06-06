# Market-Readiness Round 5 — re-audit patch sweep

■ **Created:** 2026-06-05 (America/Detroit)

■ **Last Updated:** 2026-06-05 (America/Detroit)

Patches for every finding from the second full market-readiness re-audit (0 critical,
0 high; 1 medium, 1 low-medium, and a set of lows). The re-audit confirmed all prior
hardening fixes were correctly in place; this round clears the residual items so the
remaining list is empty.

## Categories

### Platform (MEDIUM — Folia)
- **Resource pack now sent on the player's own region thread.** `ResourcePackListener`
  used `runLater` (global region thread on Folia) to call `setResourcePack` — a per-player
  op that must run on the entity thread. Switched to `runAtEntityLater(player, …, null, 20L)`.
  - `core/src/main/java/dev/zcripted/obx/core/platform/resourcepack/ResourcePackListener.java`

### Enchant (LOW-MED — memory)
- **CombatState despawn leak closed.** `removeEntity` was wired only to `EntityDeathEvent`,
  so mobs that *despawn* (chunk unload / distance / removal) never purged their mob-keyed
  entries. Added `CombatState.sweepExpired()` (drops time-expired entries from
  marks/stun/tethers/vengeance/cooldowns/pearlLock/grit) and a 2-minute repeating janitor in
  `EnchantModule` (cancelled on disable). `isStunned`/`markBonus` now remove on expiry, and
  `clear(UUID)` purges the player's `cooldowns` entries.
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/effect/CombatState.java`
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/EnchantModule.java`
- **Curse of Echoes self-damage routed through `player.damage()`** (was `setHealth`), so it
  respects resistance/totems, fires `EntityDamageEvent` (reactive saves trigger), and a lethal
  reflect produces a proper, attributed death.
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/listener/CombatEnchantListener.java`

### Jail (LOW — overflow)
- **`parseDuration` now saturates** (`Math.multiplyExact`/`addExact` + ~100-year cap) instead
  of overflowing `long` negative — an oversized `/jail` duration no longer wraps to an
  "already expired" state that frees the player instantly. Mirrors the moderation fix.
  - `features/jail/src/main/java/dev/zcripted/obx/feature/jail/service/JailService.java`

### Chat / i18n (LOW — tag quoting + sanitize)
- **Shared `MessageSanitizer.quoteArg`** — extracted the delimiter-picking MOTD quoting into a
  public util; `LanguageManager.quoteArg` now delegates to it (MOTD test still passes).
- **`ChatFormatter` hover/click hardened** — both the `/msg` suggest value and the hover text
  are wrapped via `quoteArg` instead of a `\'` escape (which the Adventure-core regex ignores),
  so a Geyser/Bedrock apostrophe gamertag can't spill the tag.
- **`{displayname}` neutralized** in join/leave broadcasts (parity with chat).
  - `core/src/main/java/dev/zcripted/obx/util/text/MessageSanitizer.java`
  - `core/src/main/java/dev/zcripted/obx/core/language/LanguageManager.java`
  - `features/chat/src/main/java/dev/zcripted/obx/feature/chat/format/ChatFormatter.java`
  - `features/playerinfo/src/main/java/dev/zcripted/obx/feature/playerinfo/listener/JoinLeaveListener.java`

### Moderation (LOW — perf/consistency)
- **Chat-mute now checks the UUID cache** (`isMuted(UUID)`/`getMuteReason(UUID)`) instead of a
  per-message name→UUID DB lookup. Added the UUID overloads to the public `ModerationApi`
  (additive; `ModerationService` already implemented them). Service fetched once per message.
  - `api/src/main/java/dev/zcripted/obx/api/moderation/ModerationApi.java`
  - `features/chat/src/main/java/dev/zcripted/obx/feature/chat/listener/ChatManagementListener.java`

### Commands / Util (LOW)
- **`/obx reload <file>` path-traversal guard** — rejects any target whose canonical path
  escapes the data folder.
  - `core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java`
- **Update-notify opt-in keyed by UUID** (was player name) so it survives a name change; removed
  the now-unused `ConsoleCommandSender` import.
  - `core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java`
- **`UpdateChecker` response read capped at 1 MB** (defensive vs a hostile proxy).
  - `core/src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java`

### Economy (LOW — perf/compat)
- **`/pay` typo-suggestion query capped** (`topBalances(200)`) — no full-table scan on the main
  thread.
  - `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java`
- **1.8 main-hand shim** — new `InventoryCompat.mainHand/setMainHand` (falls back to the legacy
  single-hand API on 1.8); applied to `SellCommand` + `WorthCommand`.
  - `core/src/main/java/dev/zcripted/obx/util/compat/InventoryCompat.java` (new)
  - `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/SellCommand.java`
  - `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/WorthCommand.java`

### Dead code / docs
- **WarpMenuListener tautology fixed** — `reopenAfterEdit` had `if (x == x)` (dead else); now
  reopens the details screen when the back-context is `DETAILS`, else the manage list.
  - `features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuListener.java`
- **Hologram dormancy javadoc** updated to reflect "enabled by default."
  - `features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/service/HologramService.java`

## Verified (no change needed)
- **Folia `cancelAll` / region-entity tasks:** the scheduler exposes no repeating entity/region
  method — only `runRepeating` (global) and `runAsync*`, both cancelled by `cancelTasks`. No
  uncancellable repeating task can exist; invariant already documented at the cancel site.
- **Storage main-thread reads:** all access is serialized under one lock with per-statement hold
  (ms-scale), and reads sit on command paths (not per-tick). Left as a documented
  launch-acceptable tradeoff rather than a risky async-read refactor.

## Verification
- `./gradlew build` → **BUILD SUCCESSFUL** (both jars produced via shadowJar + ProGuard).
- `:core:test` + `:features:economy:test` (MOTD/quoteArg, EN/DE/ES parity, economy sanitize) → green.
- run-paper (Paper 1.21.4, Folia scheduler + Adventure) boot: **clean enable** — platform
  detected, SQLite store opened (schema v1), 100 enchants loaded, hologram TextDisplay backend
  selected, `Done (14.471s)!` with **no exceptions/SEVERE/ERROR** from OBX. (Server force-stopped
  to end the run; enable path — all new wiring — verified.)

## Suggested Commit Message
```
Fix (market-readiness r5): Folia pack thread, combat-state despawn sweep,
jail overflow, chat/MOTD quoteArg, mute UUID cache, path-traversal guard,
econ 1.8 hand + pay cap, update-notify UUID, dead-branch + doc cleanup
```
