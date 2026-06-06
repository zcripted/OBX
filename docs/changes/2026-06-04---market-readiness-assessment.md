# Market-Readiness Assessment & Hardening

■ **Created:** 2026-06-04 4:15 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 4:58 pm (America/Detroit)

## Summary
A full market-readiness audit of the OBX codebase (459 Java files, 20 areas) was run via
six parallel deep-dive reviews. **No CRITICAL defects** were found. The audit surfaced
**6 HIGH blockers** and **5 MEDIUM** issues — all concentrated and surgical, no architectural
rework. **Every one of those 11 items has been fixed** and the project builds green with both
jars produced and the EN/DE/ES language-parity test passing.

## Per-Area Readiness (post-fix)
chat ✅ · commands ✅ · economy ✅ · enchant ✅ (was 🔴) · gui ✅ · hologram ✅ · hub ✅ (was 🟡) ·
jail ✅ (was 🔴) · kit ✅ · language ✅ (was 🔴) · listener ✅ · message ✅ · messaging ✅ ·
moderation ✅ (was 🟡) · nickname ✅ (was 🔴) · platform ✅ · scoreboard ✅ · storage ✅ (was 🟡) ·
tablist ✅ · util ✅

---

## Categories

### Internal / Concurrency
- **(HIGH) language — async `HashMap` corruption.** `playerLanguages`/`languageFiles` are read from
  the async chat thread while written on the main thread. Switched both to `volatile ConcurrentHashMap`
  with rebuild-and-swap on reload so readers never see a resizing/empty map.
  - `core/src/main/java/dev/zcripted/obx/core/language/LanguageManager.java`

### Performance / Memory (enchant)
- **(HIGH) combat hot-path re-parse.** `CombatEnchantListener` re-parsed weapon lore ~12×/swing.
  Now reads `EnchantStorage.read(weapon)` once per hit (and a single merged armor map) and resolves
  every effect via `getOrDefault` — matching the sibling listeners.
  - `features/enchant/.../listener/CombatEnchantListener.java`
- **(HIGH) unbounded mob-keyed state leak.** `CombatState` maps keyed by victim UUID (mobs) were only
  cleared on player quit. Added `CombatState.removeEntity(uuid)` purge wired to `EntityDeathEvent`
  (runs regardless of killer), made the `vampiric` map `ConcurrentHashMap`, and purge it on quit.
  - `features/enchant/.../effect/CombatState.java`
  - `features/enchant/.../listener/OnKillListener.java`
  - `features/enchant/.../listener/CombatEnchantListener.java`

### Features
- **(HIGH) jail release never relocated the player.** Unjail and term-expiry cleared state but left
  the player inside the jail. Added `getReleaseLocation()` / `teleportToRelease()` (Folia-safe,
  immediate), teleport-out on `/unjail`, and a 5s expiry sweep that frees + notifies lapsed players.
  New message `jail.expired` (EN/DE/ES).
  - `features/jail/.../service/JailService.java`, `.../command/UnjailCommand.java`, `.../JailModule.java`
- **(HIGH) nickname impersonation.** Length-only validation let `"Notch "` (padding) and Unicode
  homoglyphs impersonate real players. Added whitespace `normalize()` + a configurable allowed-charset
  (`nickname.allowed-pattern`, default ASCII letters/digits/underscore/space) checked before the
  taken-check. New message `nickname.invalid-chars` (EN/DE/ES).
  - `features/nickname/.../service/NicknameService.java`, `.../command/NickCommand.java`
- **(MEDIUM) hub inventory-clear data loss.** `kit.clear-inventory` wiped (irreversibly) on every
  hub-world entry, deleting survival inventories on mixed/single servers. Gated the wipe behind a new
  `dedicated-server` flag (default false/safe), with a one-time console warning when skipped.
  - `features/hub/.../service/HubService.java`, `.../kit/HubKitApplierImpl.java`, `plugin/src/main/resources/systems/hub.yml`

### Economy
- **(MEDIUM) capped transfer destroyed money.** `/pay` debited the sender fully but `MIN()`-clamped
  the credit at `MAX_BALANCE`, vanishing the excess on a "successful" commit. The credit is now a
  guarded UPDATE that adds the full amount only when the recipient has headroom, else the transaction
  rolls back. Payer gets a precise `economy.pay.recipient-full` message (EN/DE/ES).
  - `features/economy/.../service/EconomyServiceImpl.java`, `.../command/PayCommand.java`

### Moderation
- **(MEDIUM) tempban duration overflow.** A huge duration overflowed `long` to negative and silently
  became the 7-day default. `parseDurationMillis` now uses `Math.multiply/addExact` and saturates at a
  ~100-year cap instead of wrapping.
  - `features/moderation/.../service/ModerationService.java`
- **(MEDIUM) pre-1.20.1 name-change ban bypass.** Below 1.20.1 only a NAME ban was applied, bypassable
  by a name change. Added a UUID-keyed `moderation_bans` ledger (written on ban/tempban, cleared on
  unban) enforced on `AsyncPlayerPreLoginEvent` — bans are now authoritative across 1.12→1.21.
  - `features/moderation/.../service/ModerationService.java`, `.../listener/BanLoginListener.java` (new), `.../ModerationModule.java`
  - `core/.../language/LanguageManager.java` (`format(UUID, key, …)` helper for the pre-login screen)

### Storage
- **(HIGH) schema versioning was decorative.** The version table existed but no runner read/applied it.
  Added `runMigrations()` in `open()`: stamps a fresh DB, forward-migrates an existing one through
  ordered `applyMigration(conn, version)` steps (each transactional, version advanced only on commit),
  and warns on a newer-than-build DB.
  - `core/src/main/java/dev/zcripted/obx/core/storage/SqliteDataStore.java`

### Commands
- **(MEDIUM) dead `/obx updates notify` toggle.** The toggle set was written but never read. Made it
  functional: `ObxModulesView` is now a join listener that, for opted-in permitted players, runs an
  async update check on join and reports an available release.
  - `core/.../command/ObxModulesView.java`, `plugin/src/main/java/dev/zcripted/obx/core/command/ObxCommand.java`

### Language (parity-maintained)
- Added (EN/DE/ES) `economy.pay.recipient-full`, `jail.expired`, `nickname.invalid-chars`.
  - `core/.../language/MessageDefaultsEN.java`, `MessageDefaultsDE.java`, `MessageDefaultsES.java`

---

## Testing
- `.\gradlew.bat build` → **BUILD SUCCESSFUL** (63 tasks).
- Both shippable jars produced:
  - `plugin/build/libs/OBX-1.0.0-beta-b1-unobf.jar`
  - `plugin/build/libs/OBX-1.0.0-beta-b1.jar` (ProGuard-obfuscated)
- `:core:test --tests *MessageDefaultsTest*` → **PASS** (EN/DE/ES key parity holds after the new keys).

## Notes / Assumptions
- **nickname:** default allowed charset is ASCII letters/digits/underscore/space; widen via
  `nickname.allowed-pattern` in config.yml. Multi-word nicknames still work; homoglyphs/padding are blocked.
- **hub:** the inventory wipe now requires `dedicated-server: true`; mixed servers are protected by default.
- **moderation:** UUID-ban enforcement is additive to the native ban lists (belt-and-suspenders on
  1.20.1+, authoritative below it).
- Remaining audit items were LOW (polish) and left as-is; none are launch blockers.

## Suggested Commit Message
```
Hardening: fix 6 HIGH + 5 MEDIUM market-readiness blockers across enchant, jail,
nickname, language, storage, economy, moderation, hub, and the updates-notify command
```
