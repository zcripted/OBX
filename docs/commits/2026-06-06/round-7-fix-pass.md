# 🔧 Round 7 Fix Pass — 12 Audit Findings Closed

> Every blocker and new finding from the Round 6/7 market-readiness audits is fixed —
> except #1 (the shipped Discord webhook), deferred by request and still release-blocking.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-06 4:05 PM EST |
| **Last Updated** | 2026-06-06 4:05 PM EST |
| **Author** | zcripted |
| **Scope** | Audit remediation across 9 features |
| **Files changed** | 12 (1 new · 11 modified) |
| **Categories** | Fix · Threading · Storage · GUIs · Messages/i18n · Performance |
| **Verification** | ✅ `gradlew build` green · EN/DE/ES parity green · both jars produced |

---

## 📋 Summary (patch notes)

- **No more Folia format crashes** — TPS formatting in the tablist (and the /health
  report) uses thread-local formatters.
- **Backpacks can't lose items on a bad save** — a failed serialization now SKIPS the
  write (preserving the last good contents) and warns the console, instead of silently
  wiping the row. Creative middle-click can no longer mint surplus backpack keys — the
  key collapses to exactly one on every use. The F-key off-hand swap can no longer
  smuggle a backpack into itself.
- **Mutes actually stick in /msg** — checked by UUID (name-change proof) and the inbox
  draft-reply path is now gated too.
- **Warp prompts clean up after quitters** — no more permanent pending-input entries
  that swallowed a returning player's first chat line.
- **`teleport.cancel-on-move` now works** — moving a block during a warmup cancels the
  teleport (the config key previously did nothing).
- **Hologram reloads can't brick the service** — teardown failures fall back to
  per-hologram cleanup and the reload always completes.
- **Confirm menus act on the clicker** — no stale player references.
- **4 missing messages restored** (unbreakable toggle ×2, warp search-empty, warp
  console notice) in EN/DE/ES.
- **Admin economy screens are cheap again** — a 3-second stats cache replaces the
  6–10 SQLite queries/sec the live tiles fired per viewer.
- **Sell-GUI death exploit closed** — dying with the sell inventory open drops those
  items like any other inventory.

## 🔄 Changes

- [`TablistRenderer.java`](../../../features/tablist/src/main/java/dev/zcripted/obx/feature/tablist/format/TablistRenderer.java) · [`HealthCommand.java`](../../../core/src/main/java/dev/zcripted/obx/core/diagnostics/HealthCommand.java) — ThreadLocal DecimalFormat.
- [`BackpackService.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/service/BackpackService.java) — null-on-failure serialization + skip-save guard + `normalizeKeyItem`.
- [`BackpackListener.java`](../../../features/backpack/src/main/java/dev/zcripted/obx/feature/backpack/listener/BackpackListener.java) — SWAP_OFFHAND guard (name-compared, 1.8-safe) + key normalization on open.
- [`PrivateMessageService.java`](../../../features/mail/src/main/java/dev/zcripted/obx/feature/mail/pm/PrivateMessageService.java) — UUID mute lookups + draft-path mute gate.
- [`WarpMenuInputListener.java`](../../../features/warp/src/main/java/dev/zcripted/obx/feature/warp/gui/WarpMenuInputListener.java) — quit cleanup.
- [`AdminSubMenu.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java) — `Consumer<Player>` confirm actions; cached stats in the economy tiles.
- [`AdminMenu.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenu.java) — economy tile reads the cache.
- **NEW** [`EconomyStats.java`](../../../features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/EconomyStats.java) — 3s-TTL Folia-safe snapshot cache.
- [`TeleportManagerImpl.java`](../../../features/teleport/src/main/java/dev/zcripted/obx/feature/teleport/service/TeleportManagerImpl.java) — cancel-on-move handler.
- [`HologramService.java`](../../../features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/service/HologramService.java) — reload try/fallback/finally.
- [`ShopListener.java`](../../../features/economy/src/main/java/dev/zcripted/obx/feature/economy/shop/ShopListener.java) — death-drop guard + silent empty close.
- [`MessageDefaultsEN/DE/ES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java) — 4 missing keys ×3 languages.

### 📄 Change file
- [`docs/changes/2026-06-06---round-7-fix-pass.md`](../../changes/2026-06-06---round-7-fix-pass.md)

---

## ⚠️ Still open
- **Round 6 blocker #1**: the live Discord webhook + server/channel IDs in
  `config.yml` (deferred per request). Scrub + revoke before sale.

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Fix (round 7): thread-safe formats, backpack save+key guards, PM mute UUID, warp prompt cleanup, cancel-on-move, hologram reload, missing lang keys, economy stats cache, sell-GUI death guard
```
