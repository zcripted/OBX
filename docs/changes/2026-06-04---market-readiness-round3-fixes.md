# Market-Readiness Round 3 — Fix Batch

■ **Created:** 2026-06-04 2:13 am (America/Detroit)

■ **Last Updated:** 2026-06-04 2:13 am (America/Detroit)

Third assessment pass. All round-2 changes were independently re-verified as correct with no
regressions; this batch fixes the new findings (1 self-introduced HIGH + 3 MEDIUM + LOW polish)
and finalizes the intentional removal of the Paper-native bootstrap.

Build green (`./gradlew build`); both jars produced (`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`);
EN/DE/ES parity test passes (2 new keys: `messaging.socialspy.on`, `messaging.socialspy.off`).

> **Still out of scope:** the live Discord webhook token in `config.yml` (owner's external rotation).

---

## Platform — Paper-native bootstrap removal finalized (intentional)

Per owner decision, the revert to `plugin.yml`-based loading is intentional. The following were
already staged for deletion in HEAD and are now confirmed/finalized:
- `platform/paper/build.gradle.kts`, `OBXBootstrap.java`, `OBXPaperLoader.java`
- `plugin/src/main/resources/paper-plugin.yml`
- `settings.gradle.kts` no longer includes `:platform:paper`

Finalization in this batch: removed the stale IntelliJ module metadata dir `.idea/modules/platform/`
(the whole `.idea/` tree is now gitignored). Confirmed **no** source/build references to the removed
module remain (`platform:paper` / `OBXBootstrap` / `OBXPaperLoader` / `paper-plugin.yml`).

**Runtime note:** `sqlite-jdbc` is now provisioned via the `libraries:` block in `plugin.yml`
(`org.xerial:sqlite-jdbc:3.45.3.0`), resolved by Bukkit/Paper's runtime library loader on **1.16.5+**.
On **1.12–1.16.4** the `libraries:` block is not honored, so the jar must be placed in the server's
`libraries/` folder — `SqliteDataStore.open()` already logs that instruction and degrades gracefully
(this behavior is unchanged by the Paper removal).

## HIGH — chat interactive-tag sanitizer bypass (self-introduced in round 2)

`MessageSanitizer.stripInteractiveTags` previously used `(?i)</?(click|hover|insert)(:[^<>]*)?>`,
which failed to match a tag whose quoted argument contained a `<` (e.g.
`<hover:show_text:'<red>scam'>X</hover>`) — leaving the opener intact for the parser → arbitrary
hover / clickable-`run_command` injection (only when `allow-formatting-in-messages: true` AND the
sender has `obx.message.color`; the default path was always safe). Replaced with a quote-immune
lookahead `(?i)<(?=/?(?:click|hover|insert)\b)` that neutralizes only the tag's leading `<`,
regardless of argument content. Non-interactive tags (color/gradient/font) are untouched.

File: `core/.../util/text/MessageSanitizer.java`.

## MEDIUM

- **`/socialspy` raw-key leak.** `SocialSpyCommand` sent `messaging.socialspy.on`/`.off`, which were
  absent from every catalogue (the parity test only checks EN=DE=ES, so symmetric absence passed).
  Added both keys to EN/DE/ES. Files: `core/.../language/MessageDefaults{EN,DE,ES}.java`.
- **`{displayname}` unsanitized in scoreboard + tablist.** Both renderers fed raw `getDisplayName()`
  into a MiniMessage placeholder (a tag-bearing nick / 3rd-party display name could inject into the
  sidebar/tab). Now routed through `MessageSanitizer.neutralizeTags`, matching the round-2 chat fix.
  Files: `features/scoreboard/.../format/ScoreboardRenderer.java`, `features/tablist/.../format/TablistRenderer.java`.
- **Vault `withdrawPlayer` accepted a negative amount as no-op SUCCESS.** `depositPlayer` was guarded
  in round 2 but `withdrawPlayer` wasn't — `withdraw(-x)` sanitizes to 0 and returns a no-op `true`.
  Now rejects null player / non-positive / NaN / Inf with FAILURE. File: `features/economy/.../VaultEconomyProvider.java`.
- **Enchant strict-mode migration doc broadened.** The `enchant.security.trust_unsigned_lore` comment
  now spells out that, in strict mode, ANY lore re-render (including a vanilla anvil rename/repair or
  enchanting-table use) drops unsigned legacy enchants — not just a deliberate scroll apply. File:
  `plugin/src/main/resources/enchants/config.yml`.

## LOW

- **Mute cache no longer leaks offline-mute entries** — `mute()` only caches for online targets
  (mirrors the round-2 jail guard). File: `features/moderation/.../service/ModerationService.java`.
- **`jail.containment` message throttled** to once per 3s per player (the pull-back still happens every
  move); evicted on quit. File: `features/jail/.../listener/JailListener.java`.
- **Bleed `Bleed` fields marked `volatile`** — the refresh path (attacker region thread) and the tick
  (victim region thread) touch them concurrently on Folia. File: `features/enchant/.../listener/OnHitProcListener.java`.
- **Tablist scoreboard-handoff reset made atomic** — `lastScoreboardOwns` is now an `AtomicReference`
  with `getAndSet`, so concurrent Folia region threads can't double-fire the one-shot team reset.
  File: `features/tablist/.../format/TablistRenderer.java`.

## Accepted (documented, not changed)
- **Scoreboard quit "ghost"** — a quitter's name lingers in other players' nametag teams until the next
  refresh tick (≤ the refresh interval), then `removeStaleEntries` clears it. Self-healing; adding
  cross-board cleanup is disproportionate risk for a sub-second cosmetic gap.
- **`/eco` & Vault display balance** read non-atomically after a mutation (admin/display-only, no
  integrity impact).

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` (EN/DE/ES parity incl. the 2 new keys) +
  `:features:economy:test` pass. ProGuard `Note:` lines are informational.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` (obfuscated) + `OBX-1.0.0-beta-b1-unobf.jar`.
- `sqlite-jdbc` declared in `plugin.yml` `libraries:` (loads on 1.16.5+).

## Suggested Commit Message
```
Fix (market-readiness r3): chat tag-strip bypass, socialspy keys, displayname sanitize

Quote-immune interactive-tag stripper (close the click/hover injection in the round-2
sanitizer); add missing /socialspy messages (EN/DE/ES); neutralize {displayname} in
scoreboard/tablist; guard Vault withdrawPlayer against non-positive amounts; broaden
strict anti-forge migration docs; mute offline-cache guard, jail message throttle,
bleed volatile fields, atomic tablist handoff. Finalize Paper-native bootstrap removal
(plugin.yml libraries provisioning).
```
