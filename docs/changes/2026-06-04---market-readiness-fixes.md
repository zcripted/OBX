# Market-Readiness Fix Batch

■ **Created:** 2026-06-04 1:08 am (America/Detroit)

■ **Last Updated:** 2026-06-04 1:47 am (America/Detroit) — added jail-rationale correction (see round-2)

Applies the full list of blocker / HIGH / MEDIUM / LOW-polish fixes drawn from the
market-readiness assessment. Build is green (`./gradlew build`), both jars produced
(`OBX-1.0.0-beta-b1.jar` obfuscated + `-unobf.jar`), and the EN/DE/ES parity test passes.

> **Deliberately out of scope:** the live Discord webhook token in
> `plugin/src/main/resources/config.yml` (rotation is an external owner action and was
> not authorized). Not touched.

---

## Economy

- **Pay error-vs-decline message.** `PayCommand` now distinguishes a *declined* transfer
  (insufficient funds → `economy.pay.insufficient`) from a genuine *failure* with funds
  present (→ new `economy.pay.failed`), instead of showing one ambiguous message.
- **`getBalance` no longer fabricates a starting balance for unknown UUIDs.** It returns
  `0.0` for accounts that don't exist (`.orElse(0.0)`); accounts are seeded explicitly via
  the new `EconomyService.ensureAccount(...)` on join (new `EconomyJoinListener`), so
  unknown players can't read a phantom balance.
- **`/eco take` respects the withdraw result.** The `take` path checks the withdraw
  boolean and reports `economy.eco.take-failed` on failure instead of silently "succeeding".

Files: `features/economy/.../command/PayCommand.java`,
`features/economy/.../command/EcoCommand.java`,
`features/economy/.../service/EconomyServiceImpl.java`,
`api/.../api/economy/EconomyService.java`,
`features/economy/.../listener/EconomyJoinListener.java` (new),
`features/economy/.../EconomyModule.java`.

## Chat / Messaging

- **Staff styling keyed off a permission, not `isOp()`.** `ChatManagementListener`
  gates the staff prefix on `obx.chat.staff` (was `player.isOp()`).
- **Color / format-code injection blocked.** New `MessageSanitizer` strips `&`/`§`
  (incl. `&#RRGGBB`) for senders without `obx.message.color`, and always neutralizes
  `<`/`>` to prevent MiniMessage tag injection. Wired through `/msg`, `/me`,
  `/broadcast`, `/staffchat`, mail and inbox.

Files: `core/.../util/text/MessageSanitizer.java` (new),
`features/chat/.../listener/ChatManagementListener.java`,
`features/mail/.../pm/PrivateMessageService.java`,
`features/mail/.../staffchat/StaffChatService.java`,
`features/mail/.../command/{MeCommand,BroadcastCommand,MailCommand}.java`.

## Mail / Private Messaging

- **Staff-chat reply hijack fixed.** Reading staff chat no longer overwrites the PM
  reply target; `/r` routes to whichever (PM vs staff-chat) was most recent via
  `staffChatIsMostRecent(id)`.
- **`PrivateMessageService.clear()` wired to `PlayerQuitEvent`** so per-session reply/
  ignore state doesn't leak across logins.
- **`/ignore` leak + offline bypass fixed.** Offline PMs now honor ignore (faked success
  to the sender, no delivery) and still notify social-spy; the ignore check is centralized.
- **Inbox GUI null-guard.** `InboxMenuListener` guards `top == null` before the holder
  `instanceof` check.

Files: `features/mail/.../pm/PrivateMessageService.java`,
`features/mail/.../pm/gui/InboxMenuListener.java`.

## Nickname

- **Multi-word nicks + impersonation/uniqueness.** `/nick` treats the optional target as
  the *last* arg (requires `obx.nick.others`); otherwise all args join into a multi-word
  nick. Impersonation/uniqueness is checked via `isNameTaken(...)` → `nickname.taken`.
- **`<`/`>` stripping.** `setNickname` sanitizes through `MessageSanitizer`; `applyToPlayer`
  neutralizes residual `<`/`>`.

Files: `features/nickname/.../service/NicknameService.java`,
`features/nickname/.../command/NickCommand.java`.

## Moderation

- **NAME → UUID PROFILE bans on 1.20.1+.** `ban()`/`tempBan()` additively apply a UUID
  PROFILE ban (reflective, no-op pre-1.20.1) alongside the legacy NAME ban, so enforcement
  survives renames. `unban()` pardons both lists.
- **`/tempban` honors its duration argument.** A leading duration token (`3d`, `2h`,
  `1d12h`, …) is parsed via `parseDurationMillis(...)`; the remainder is the reason.

Files: `features/moderation/.../service/ModerationService.java`,
`features/moderation/.../command/ModerationCommand.java`.

## Enchant

- **Bleed DoT respects PvP/region protection (#4).** The non-lethal bleed tick previously
  bypassed all protection via raw `setHealth`. It now fires a plain `EntityDamageEvent`
  (cause `CUSTOM`, **not** `ByEntity` — so it can't re-trigger combat enchants) and only
  applies the armor-bypassing tick if that event isn't cancelled. Bleed no longer ticks in
  spawn / PvP-disabled / godmode-protected areas.
- **Anti-forge signing (#15).** Custom enchants are stored as parseable lore (the only
  cross-version store), which made them forgeable. Every write now stamps an **invisible
  HMAC-SHA256 signature** (rendered as a no-glyph color-code run, so `stripColor` reduces it
  to `""` and the existing parser ignores it) over the canonical enchant set, keyed by a
  per-server secret in `plugins/OBX/enchant-signing.key`. Gated by
  `enchant.security.trust_unsigned_lore` (**default true** = legacy behavior, signatures
  written but not enforced; set **false** for strict enforcement, where unsigned/forged
  enchant sets are ignored). Works on 1.8 → 1.21 with no reflection on the read hot path.
- **Per-hit lore re-parse eliminated (#24).** `OnHitProcListener` and `OnHitDamageListener`
  now read the weapon's enchants **once** per event (`storage.read(weapon)`), replacing the
  ~40 `storage.level(weapon, ...)` calls that each re-parsed the entire lore.
- **Folia race on the secondary-proc guard (#25).** `OnHitProcListener.inSecondary` is now a
  `ThreadLocal<Boolean>` instead of a shared field, so per-region threads can't corrupt it.

Files: `features/enchant/.../listener/OnHitProcListener.java`,
`features/enchant/.../listener/OnHitDamageListener.java`,
`features/enchant/.../storage/EnchantStorage.java`,
`features/enchant/.../service/EnchantService.java`,
`plugin/src/main/resources/enchants/config.yml`.

## Kit

- **Item-dupe race fixed.** Cooldown is now claimed atomically
  (`tryClaimCooldown` — `INSERT … ON CONFLICT … DO UPDATE … WHERE` returns rows-affected)
  before items are granted, closing the read-sync / record-async gap.

Files: `features/kit/.../service/KitService.java`,
`features/kit/.../command/KitCommand.java`.

## Warp GUI

- **Item-loss via drag fixed.** Added an `InventoryDragEvent` handler that cancels +
  `DENY`s drags onto the warp menu (clicks were already cancelled).

Files: `features/warp/.../gui/WarpMenuListener.java`.

## Scoreboard / Tablist

- **Folia region-correct refresh.** Refresh tasks dispatch each player through
  `runAtEntity` when running on Folia (a global-region task never fires there).
- **Team-name collision resolved.** When the scoreboard feature is active, the tablist
  **defers** sorting/grouping to it (scoreboard's `np_*` teams already sort and color
  identically), avoiding team ping-pong/flicker.
- **Hex split-at-boundary cosmetic fixed.** `ScoreboardRenderer.safeSplit` treats `§<code>`
  as 2 chars and a `§x` hex run as an atomic 14-char unit so a line never splits mid-color.

Files: `features/scoreboard/.../scheduler/ScoreboardRefreshTask.java`,
`features/tablist/.../scheduler/TablistRefreshTask.java`,
`features/tablist/.../format/TablistRenderer.java`,
`features/scoreboard/.../format/ScoreboardRenderer.java`.

## Jail

- **No main-thread SQLite on the hot path + no blocking offline lookup.** Jail state is
  cached in memory (loaded on join, evicted on quit, with a negative `notJailedCache`).
  `clearState` is synchronous (no stale record after release). Offline UUID resolution uses
  Paper's non-blocking `getOfflinePlayerIfCached` (reflective), falling back to the blocking
  lookup only on non-Paper forks.
  > **Correction (see round-2 change file):** the original wording here referred to a
  > `PlayerMoveEvent` handler that did not yet exist — jail had no movement containment at all.
  > The cache is real and benefits the join/teleport/command paths; the actual move handler
  > (and the containment it enables) was added in
  > `2026-06-04---market-readiness-round2-fixes.md`.

Files: `features/jail/.../service/JailService.java`,
`features/jail/.../listener/JailListener.java`.

## Hub

- **Dead code removed.** Deleted the unused `snapshot` map, its empty-check block, the
  `touch()` method, and a now-unused import from `LaunchpadCooldownManager`.

Files: `features/hub/.../launchpad/LaunchpadCooldownManager.java`.

## Hologram

- **Type-based channel resolution.** `PacketChannelInjector` falls back to scanning for the
  field whose value *is* a Netty `Channel` (walking the class hierarchy) when the name
  guesses miss — far more robust across remapped/obfuscated builds.

Files: `features/hologram/.../packet/PacketChannelInjector.java`.

## Commands / Core

- **Override path respects the Bukkit permission gate.** `CommandOverrideListener` now
  `testPermissionSilent` before re-dispatching `/heal`, `/god`, `/tps`, `/pl`, `/clearinv`
  — denied senders get `core.no-permission` instead of a silently-run command.
- **Permission nodes.** Added `obx.message.color` and `obx.chat.staff` (default op) to
  `plugin.yml`. (`/pl` already declared `obx.pl`; `/list` intentionally stays
  permission-free per prior owner instruction.)

Files: `core/.../core/command/CommandOverrideListener.java`,
`plugin/src/main/resources/plugin.yml`.

## Storage / Lifecycle

- **Clean shutdown.** `OBX.onDisable` now `beginShutdown()`s the data store (async writes
  run inline during shutdown), saves, then `close()`s — and `SqliteDataStore.close()` runs
  `PRAGMA wal_checkpoint(TRUNCATE)` before closing the connection so the WAL is folded back
  into the main DB file.

Files: `core/.../core/storage/SqliteDataStore.java`,
`plugin/.../OBX.java`.

## Language

- New keys added to **all three** base languages (`MessageDefaultsEN/DE/ES`):
  `economy.eco.take-failed`, `economy.pay.failed`, `nickname.taken`. ES untranslated keys
  fall back to English by design; parity enforced by `MessageDefaultsTest` (passing).

Files: `core/.../core/language/MessageDefaultsEN.java`,
`MessageDefaultsDE.java`, `MessageDefaultsES.java`.

---

## Testing

- `./gradlew build` — **BUILD SUCCESSFUL**.
- `:core:test` (incl. `MessageDefaultsTest` EN/DE/ES parity) — pass.
- `:features:economy:test` — pass.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` (obfuscated, shippable) and
  `OBX-1.0.0-beta-b1-unobf.jar`.

## Suggested Commit Message

```
Fix (market-readiness): security, dupe, Folia, jail perf + enchant anti-forge

Batch of blocker/HIGH/MEDIUM/polish fixes: message-code injection sanitizer,
economy phantom-balance + withdraw-result, kit dupe race, nickname impersonation,
moderation UUID PROFILE bans + tempban duration, enchant bleed protection-gating +
invisible HMAC anti-forge signing + per-hit read-once + Folia ThreadLocal, jail
in-memory cache (no main-thread SQLite), scoreboard/tablist Folia + team collision,
warp drag, command permission gate, WAL checkpoint on shutdown.
```
