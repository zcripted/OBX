# Market-Readiness Assessment — Round 7 (Full Re-Audit + Delta Verification)

■ **Created:** 2026-06-06 3:51 pm

■ **Last Updated:** 2026-06-06 3:51 pm

## Method

Round 7 = (a) point-by-point re-verification of every Round 6 blocker at its exact
location, (b) adversarial re-audit of all code changed since Round 6 (Economy tile/panel
restructure, shop, backpack, GUI routing), (c) a repo-wide cross-cutting integrity sweep
(language keys used-vs-defined incl. dynamic families, config reads, resource bundling),
and (d) carry-forward attestation of the eight Round 6 area audits whose code is
unchanged. Every agent claim was re-verified against source before inclusion.

## Fixed DURING this round

- **Economy Control panel slot desync (CRITICAL → fixed)** — a manual layout edit moved
  the action tiles to slots 11–15 (centered) while the click dispatcher and refresh task
  still targeted 10–14, so clicks fired the wrong actions (Overview opened the Manage
  panel, Manage opened the worth-reload confirm, Transactions did nothing…).
  `handleEconomyMenuClick` + `refresh(ECONOMY)` realigned to the 11–15 layout
  (`AdminSubMenu.java`); build green. The user's centered layout was kept.

## Blocker status (carried from Round 6 — ALL re-verified still present)

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | CRITICAL | Live Discord webhook + IDs in default config.yml (152-154) — also revoke server-side | ❌ unchanged |
| 2 | CRITICAL | TablistRenderer static `DecimalFormat` (line 37) thread-unsafe on Folia | ❌ unchanged |
| 3 | CRITICAL | Backpack `toBase64` returns `""` on failure → overwrites good contents | ❌ unchanged |
| 4 | HIGH | PM mute check by NAME (PrivateMessageService:137) + draft path skips mute | ❌ unchanged |
| 5 | HIGH | Warp chat-input manager: no quit cleanup (leak + chat swallow) | ❌ unchanged |
| 6 | HIGH | Backpack F-key/off-hand swap bypasses nesting guard | ❌ unchanged |
| 7 | HIGH | CONFIRM-menu runnables capture live `Player` reference | ❌ unchanged |
| 8 | HIGH | `teleport.cancel-on-move` config key has NO implementation (re-confirmed: zero PlayerMoveEvent in teleport module) | ❌ unchanged |
| 9 | HIGH | Hologram reload orphan path; Folia sweep disabled | ❌ unchanged |

## NEW findings (Round 7)

| Severity | Finding | Location |
|---|---|---|
| HIGH | **4 missing language keys** render as raw `{prefix}key` text in-game: `item.unbreakable.enabled`, `item.unbreakable.disabled` (every `/unbreakable` toggle), `teleport.warp.gui.search-empty` (empty warp search), `teleport.warp.gui.console` (3 console call-sites). Verified used-in-code + absent from EN/DE/ES. | UnbreakableCommand.java:49,53 · WarpMenu.java:79 · WarpAdminCommands.java:40,148 · WarpQueryCommands.java:227 |
| HIGH | **Creative middle-click clones a VALID backpack key** — the clone carries the same owner+token, so two valid keys exist; two concurrent open views of one backpack make last-save-wins content loss/dupe possible (creative players/staff only; contents otherwise single-copy). Guard: cancel creative CLONE_STACK of tagged items, or save-merge. | BackpackService.isValidItem + BackpackListener (no creative-clone guard) |
| MEDIUM | **Admin-menu Economy tile + panel run synchronous SQLite stats on the 0.5s refresh tick** (~6 queries/sec per viewer on the Admin Menu, ~10/sec on the panel). Cache stats for a few seconds or slow the economy cadence. | AdminMenuRefreshTask (10-tick) + AdminMenu.economyTileLore + AdminSubMenu.refresh(ECONOMY) |
| MEDIUM | Sell-GUI close payout on player death/respawn returns unsellables at the respawn location; full-inventory path drops at feet. Edge, acceptable, but worth a death-event guard. | ShopListener.onClose |
| LOW | Long top-player names in the Economy tile lore truncate at the 45-char cap (by design, matches SCP tile — cosmetic). | AdminMenu.economyTileLore |

## Integrity sweep results

- **Language keys**: ~863 distinct keys used in code vs 1,633 defined — the 4 above are
  the ONLY gaps; all dynamic key families (`economy.eco.<action>`, `admin.scp.label.*`,
  `admin.ecp.label.*`, `commands.pl.status.*`, gamerule names, enchant suffixes) verified
  complete.
- **Resource bundling**: every `saveResource` target (worth.yml, shop.yml, shops/*,
  kits.yml, jails.yml, motd.yml, enchants/*) is bundled — no runtime
  IllegalArgumentException possible. 29 resources inventoried.
- **YAML shapes**: shop.yml / shops/* / worth.yml / kits.yml parse-shapes match loaders.

## Areas re-attested from Round 6 (code unchanged since that audit)

chat ✅ · commands (102/102 bound) ✅ · moderation ✅ · jail ✅ (LOW pearl/chorus note) ·
kit ✅ · language system ✅ · storage ✅ · platform/scheduler ✅ · enchant ✅ (grindstone
behavior decision pending) · hologram ⚠ (blocker #9) · hub ✅ · scoreboard ✅ ·
tablist ⚠ (blocker #2) · nickname ✅ (length-inflation note) · messaging ⚠ (blocker #4) ·
listeners ⚠ (blocker #5) · util ✅ · gui ✅ after this round's slot fix.

## Verdict

**Unchanged from Round 6: NOT ready to ship — one focused fix-day away.** The work
queue is now precisely: blockers 1–9 + the 4 missing language keys + the creative-clone
guard (≈13 point fixes, no architecture work), then a `runServer` smoke pass.
Round 7 added confidence: configs, resources, dynamic key families, and the command/
permission registry are fully verified clean, and the one regression introduced since
Round 6 (panel slot desync) was caught and fixed inside this round.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** after the slot realignment; both jars produced.
