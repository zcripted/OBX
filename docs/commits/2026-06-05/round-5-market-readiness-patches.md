# 🛠️ Round 5 — Market-Readiness Re-Audit Patches

> The post-re-audit hardening sweep. A second full 20-area audit found **0 critical / 0 high**
> issues and confirmed every prior fix was in place; this commit clears the remaining list —
> **1 Folia thread fix, 1 memory-leak closure, and 12 low-severity polish fixes**.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 3:19 PM EST |
| **Last Updated** | 2026-06-05 3:19 PM EST |
| **Author** | zcripted |
| **Scope** | Market-readiness re-audit fixes (Medium → Low) |
| **Files changed** | 20 (19 modified · 1 new) |
| **Categories** | Platform · Enchant · Jail · Chat/i18n · Moderation · Commands · Economy · Warp · Hologram |
| **Verification** | ✅ `gradlew build` green · unit tests green · run-paper (Paper 1.21.4) boot clean |

---

## 📋 Summary (patch notes)

This release closes out every remaining item from OBX's second market-readiness audit. In plain terms:

- **Better Folia support** — resource packs are now sent on the correct server thread, so
  region-threaded (Folia) servers won't trip thread-ownership checks on player join.
- **A slow memory leak is gone** — custom-enchant combat data tied to mobs is now cleaned up
  even when a mob *despawns* (not just when it dies), so long-uptime servers stay tidy.
- **Safer & more consistent text handling** — chat name hovers and the welcome MOTD now use the
  same bullet-proof tag-quoting, so unusual names (e.g. Bedrock gamertags with apostrophes)
  can't break formatting.
- **Sturdier admin & moderation paths** — `/obx reload <file>` can't be tricked into touching
  files outside the plugin folder, mutes are checked by UUID (faster, name-change proof), and an
  over-long `/jail` time can no longer wrap around and free a player instantly.
- **Economy polish** — `/sell` & `/worth` now work on legacy 1.8 hand APIs, and a `/pay` typo no
  longer triggers a full balance-table scan.
- **Cleanup** — a dead code branch, an unused import, and a stale doc comment were fixed.

No behavior was removed; nothing breaks existing setups. All changes are additive hardening.

---

## 🗂️ Changes — newest at top

### 🟠 MEDIUM · Platform — Folia resource-pack on the player's thread
- **What:** the join resource-pack send moved from the global region scheduler to the player's
  own entity scheduler (`runAtEntityLater`).
- **Why it matters:** on Folia, `setResourcePack` is a per-player operation that must run on that
  player's region thread; the old path could trip Folia's thread-ownership assertions.
- **File:** [ResourcePackListener.java](../../../core/src/main/java/dev/zcripted/obx/core/platform/resourcepack/ResourcePackListener.java)

### 🟡 LOW-MED · Enchant — combat-state despawn leak closed
- **What:** added a 2-minute janitor (`CombatState.sweepExpired`) plus remove-on-expiry and a
  per-player cooldown purge; wired the janitor into the module (cancelled on disable).
- **Why it matters:** mob-keyed combat data was only purged on `EntityDeathEvent`; mobs that
  *despawned* (chunk unload / distance) leaked entries for the server's lifetime.
- **Files:** [CombatState.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/effect/CombatState.java) ·
  [EnchantModule.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/EnchantModule.java)

### 🟢 LOW · Enchant — Curse of Echoes self-damage via `damage()`
- **What:** self-reflect now uses `player.damage()` instead of `setHealth()`.
- **Why it matters:** respects resistance/totems, fires `EntityDamageEvent` (reactive saves
  trigger), and a lethal reflect produces a properly attributed death.
- **File:** [CombatEnchantListener.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/listener/CombatEnchantListener.java)

### 🟢 LOW · Jail — duration overflow guard
- **What:** `parseDuration` now saturates (`Math.multiplyExact`/`addExact` + ~100-year cap).
- **Why it matters:** a huge `/jail` time used to overflow `long` negative and read as
  "already expired," freeing the player instantly. Mirrors the moderation fix.
- **File:** [JailService.java](../../../features/jail/src/main/java/dev/zcripted/obx/feature/jail/service/JailService.java)

### 🟢 LOW · Chat / i18n — shared tag-quoting + display-name sanitize
- **What:** extracted a shared `quoteArg` util; chat name hover/click and the MOTD both use it;
  `{displayname}` in join/leave broadcasts is now neutralized.
- **Why it matters:** an apostrophe in a name (Geyser/Bedrock) or a foreign display name can no
  longer spill or inject MiniMessage tags.
- **Files:** [MessageSanitizer.java](../../../core/src/main/java/dev/zcripted/obx/util/text/MessageSanitizer.java) ·
  [LanguageManager.java](../../../core/src/main/java/dev/zcripted/obx/core/language/LanguageManager.java) ·
  [ChatFormatter.java](../../../features/chat/src/main/java/dev/zcripted/obx/feature/chat/format/ChatFormatter.java) ·
  [JoinLeaveListener.java](../../../features/playerinfo/src/main/java/dev/zcripted/obx/feature/playerinfo/listener/JoinLeaveListener.java)

### 🟢 LOW · Moderation — chat-mute via UUID cache
- **What:** added UUID overloads to the public `ModerationApi`; chat-mute now checks the
  join-loaded UUID cache instead of a per-message name→UUID DB lookup.
- **Why it matters:** removes a synchronous DB hit on every chat message and is name-change proof.
- **Files:** [ModerationApi.java](../../../api/src/main/java/dev/zcripted/obx/api/moderation/ModerationApi.java) ·
  [ChatManagementListener.java](../../../features/chat/src/main/java/dev/zcripted/obx/feature/chat/listener/ChatManagementListener.java)

### 🟢 LOW · Commands / Util — traversal guard, UUID opt-in, response cap
- **What:** `/obx reload <file>` rejects paths escaping the data folder; update-notify opt-in is
  keyed by UUID (dead `ConsoleCommandSender` import removed); the update checker caps its read at 1 MB.
- **Files:** [ObxDiagnosticsView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java) ·
  [ObxModulesView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java) ·
  [UpdateChecker.java](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java)

### 🟢 LOW · Economy — `/pay` query cap + 1.8 hand shim
- **What:** the `/pay` typo-suggestion query is capped (`topBalances(200)`); a new
  `InventoryCompat` main-hand shim falls back to the legacy 1.8 hand API, applied to `/sell` & `/worth`.
- **Why it matters:** no main-thread full-table scan on a typo; `/sell` & `/worth` no longer throw on true 1.8.
- **Files:** [PayCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java) ·
  [InventoryCompat.java](../../../core/src/main/java/dev/zcripted/obx/util/compat/InventoryCompat.java) **(new)** ·
  [SellCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/SellCommand.java) ·
  [WorthCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/WorthCommand.java)

### 🟢 LOW · Cleanup — dead branch + stale doc
- **What:** fixed a `if (x == x)` tautology (dead branch) in the warp menu reopen path; corrected
  the hologram "dormant by default" javadoc to "enabled by default."
- **Files:** [WarpMenuListener.java](../../../features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuListener.java) ·
  [HologramService.java](../../../features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/service/HologramService.java)

---

## 📁 All files changed (20)

| Module | File | Change |
|--------|------|--------|
| platform | [ResourcePackListener.java](../../../core/src/main/java/dev/zcripted/obx/core/platform/resourcepack/ResourcePackListener.java) | entity-thread pack send |
| enchant | [CombatState.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/effect/CombatState.java) | sweep + expiry-remove + cooldown purge |
| enchant | [EnchantModule.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/EnchantModule.java) | 2-min janitor task |
| enchant | [CombatEnchantListener.java](../../../features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/listener/CombatEnchantListener.java) | self-damage via `damage()` |
| jail | [JailService.java](../../../features/jail/src/main/java/dev/zcripted/obx/feature/jail/service/JailService.java) | saturating duration parse |
| util/text | [MessageSanitizer.java](../../../core/src/main/java/dev/zcripted/obx/util/text/MessageSanitizer.java) | shared `quoteArg` |
| language | [LanguageManager.java](../../../core/src/main/java/dev/zcripted/obx/core/language/LanguageManager.java) | delegate to shared `quoteArg` |
| chat | [ChatFormatter.java](../../../features/chat/src/main/java/dev/zcripted/obx/feature/chat/format/ChatFormatter.java) | delimiter-safe hover/click |
| playerinfo | [JoinLeaveListener.java](../../../features/playerinfo/src/main/java/dev/zcripted/obx/feature/playerinfo/listener/JoinLeaveListener.java) | sanitize `{displayname}` |
| api/moderation | [ModerationApi.java](../../../api/src/main/java/dev/zcripted/obx/api/moderation/ModerationApi.java) | UUID mute overloads |
| chat | [ChatManagementListener.java](../../../features/chat/src/main/java/dev/zcripted/obx/feature/chat/listener/ChatManagementListener.java) | mute via UUID cache |
| command | [ObxDiagnosticsView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxDiagnosticsView.java) | path-traversal guard |
| command | [ObxModulesView.java](../../../core/src/main/java/dev/zcripted/obx/core/command/ObxModulesView.java) | UUID opt-in + import cleanup |
| util/update | [UpdateChecker.java](../../../core/src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java) | 1 MB response cap |
| economy | [PayCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java) | capped suggestion query |
| util/compat | [InventoryCompat.java](../../../core/src/main/java/dev/zcripted/obx/util/compat/InventoryCompat.java) | **new** — 1.8 hand shim |
| economy | [SellCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/SellCommand.java) | use hand shim |
| economy | [WorthCommand.java](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/WorthCommand.java) | use hand shim |
| warp | [WarpMenuListener.java](../../../features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuListener.java) | fix dead branch |
| hologram | [HologramService.java](../../../features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/service/HologramService.java) | doc fix |

---

## ✅ Verification
- `./gradlew build` → **BUILD SUCCESSFUL** — both jars produced (obfuscated + unobf via ProGuard).
- `:core:test` + `:features:economy:test` → green (MOTD/quoteArg, EN/DE/ES parity, economy sanitize).
- **run-paper boot (Paper 1.21.4, Folia scheduler + Adventure):** clean enable — platform detected,
  SQLite store opened (schema v1), 100 enchants loaded, hologram TextDisplay backend selected,
  `Done (14.471s)!` with **no exceptions / SEVERE / ERROR** from OBX.

## 🕓 Update history (newest first)
- **2026-06-05 3:19 PM EST** — Commit log created; all 20 files patched, built, and boot-verified.
