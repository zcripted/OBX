# Market-readiness hardening — Critical/High/Medium audit fixes

■ **Created:** 2026-06-04 6:27 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 6:27 pm (America/Detroit)

Implements the fixes for the Critical, High, and Medium findings from the full-codebase market-readiness
audit, in priority order. Build green; both jars produced; EN/DE/ES parity passes.

> **Live-server note:** a `run-paper` test harness was added and the plugin was **booted live on
> Paper 1.21.4** — see "Live test harness" below. It enabled cleanly with zero errors. (A dedicated
> Folia smoke test is still worth doing, since the live run was Paper, not Folia.)

---

## CRITICAL — deathdrop (item dupe / data loss)

All in `features/deathdrop/.../listener/DeathDropListener.java` (+ new `util/EntityPdc.java`,
`util/ItemSerialization.java`):

- **No pickup duplication** — the contents are now claimed with an atomic `contents.remove(id)` BEFORE
  `addItem`; a concurrently re-fired `EntityPickupItemEvent` gets `null` and bails, so an inventory can
  never be granted twice.
- **No merge loss/dupe** — a new `ItemMergeEvent` handler cancels merges of carry-all items, so two
  same-material piles can't merge and orphan one player's contents.
- **No void/lava loss** — `isUnsafeDropLocation` skips grouping when the death spot is in the void
  (below the world floor) or lava, leaving vanilla's scattered drops untouched instead of concentrating
  loot into one easily-destroyed entity after clearing vanilla's drops.
- **Survives restart** — contents are also serialized (Bukkit object streams → Base64) onto the dropped
  item's **persistent data container** (1.16+, via reflection so the 1.12 baseline still compiles);
  pickup restores from the PDC when the in-memory entry is gone. Older servers fall back to in-memory.
  Death handler moved to `HIGHEST` (it mutates the drop list; MONITOR is observe-only).

## HIGH

- **Kit first-join dupe** — new atomic `KitService.tryClaimFirstJoin` (`INSERT OR IGNORE` returning
  affected rows); the listener claims BEFORE granting, so a relog/double-join can't re-grant the kit.
- **Enchant admin GUI** — `EnchantMenuListener` re-checks `obx.enchants.admin` on EVERY click of an
  admin (non-browse) menu; a player who loses the permission (or is handed the inventory) can no longer
  mint/apply enchants. Same fix applied to the **warp** manage menu (`obx.warp.manage`).
- **Moderation mute bypass + hierarchy** — added `rply`/`staffchat`/`sc`/`adminchat`/`ac` to the muted
  command blocklist; `PrivateMessageService` now also checks `isMuted` directly (defense-in-depth) and
  caps PM length (256). New `obx.moderation.exempt` / `obx.moderation.exempt.bypass` permissions +
  `rejectProtected` guard on ban/tempban/kick/mute/warn so moderators can't punish exempt staff/owners.
  New `player.moderation.exempt-target` key (EN/DE/ES).
- **Jail PLUGIN-teleport escape** — `JailListener.onTeleport` no longer blanket-exempts PLUGIN
  teleports; it allows only destinations inside the jail containment (covers the jail-in teleport) and
  redirects everything else (`/tp`, `/spawn`, `/home`, `/back`, RTP, hub-on-join, …) back to the anchor.
- **Folia repeating loops + cancelAll** — `SchedulerAdapter.cancelAll` now also cancels the async
  scheduler on Folia; the AFK tick now dispatches its per-player set-AFK/kick work to each player's
  region thread (scoreboard refresh already did this). *(Residual: the vanish action-bar and freeze
  reminder loops still touch players from the global thread on Folia — see Remaining below.)*
- **Tablist shared-scoreboard race** — `TablistTeams` serializes all main-scoreboard mutations under a
  lock so concurrent Folia region threads can't CME/tear the team state.
- **Storage** — added `PRAGMA busy_timeout=5000` (waits for the lock instead of `SQLITE_BUSY`) and
  `synchronous=NORMAL`; the scoreboard now shows a neutral `<symbol>—` instead of a misleading `$0.00`
  when the data store is down; **periodic async playtime flush** (every 5 min) so a crash loses at most
  one interval instead of whole sessions.
- **Scoreboard cleanup** — `PlayerQuit` now detaches the board and the module clears every online
  player's board on disable, so a frozen sidebar can't linger.
- **Tablist refresh floor** — `getRefreshIntervalTicks` is clamped to a minimum of 5 ticks (positive
  values) so a misconfig can't drive a per-tick per-player MiniMessage re-render; shipped default 10.
- **Messaging** — `MessageStore` is now fully `synchronized` (no more unguarded read-modify-write-save);
  `/me` and `/broadcast` bodies are length-capped (256 / 512).

## MEDIUM

- `ObxModulesView.handleJoinLeave/handleJoinMotd` now null-guard `getJoinLeaveService()` (NPE when the
  playerinfo module is toggled off).
- Module lifecycle: a module that throws mid-`enable()` now has its partial registrations rolled back
  (`ModuleManager.enable` calls `disable` on failure).
- `/tempban` now accepts `mo` (month) and `y` (year) durations (`parseDurationMillis`).
- Nickname load isolates a single corrupt UUID row instead of dropping the whole cache.
- *(The `MessageSanitizer` "off-by-one" finding was a false positive — `i + 7 < length` correctly
  permits the last index `i+7 == length-1`; no change.)*

## Live test harness (run-paper)
- Added the `xyz.jpenilla.run-paper` Gradle plugin to `:plugin` → `./gradlew runServer` boots a real
  Paper server with the freshly shadow-built (unobfuscated) OBX jar.
- Added the foojay toolchain resolver to `settings.gradle.kts` so Gradle **auto-provisions JDK 21**
  (Paper 1.21.4's runtime) even though the project builds on JDK 17.
- Registered the JetBrains IDE MCP server in `.mcp.json` (`http://127.0.0.1:64342/sse`) — activate by
  restarting Claude Code and approving the `jetbrains` server.
- `/plugin/run/` + `run-harness*.log` added to `.gitignore` (local runtime state).
- **Live smoke test result (Paper 1.21.4, build 232, Java 21):** OBX initialized, remapped, and
  **enabled cleanly** — platform detected (`Paper 1.21.4 [folia-scheduler] [adventure]`), SQLite store
  opened (schema v1, with the new `busy_timeout`/`synchronous` pragmas), 100 enchants loaded, hologram
  TextDisplay backend selected, banner shown, `Done (16.5s)`. **Zero SEVERE/ERROR/Exception and zero
  relevant WARN lines.** (Run a `stop` console command rather than a force-kill if you also want to
  validate a clean `onDisable`.)

## Categories Touched
Features (deathdrop, kit, enchant, moderation, jail, scoreboard, tablist, playerinfo, playerstate, mail,
warp, nickname), Core (storage, scheduler, module, command, language), Permissions, Config, Messages.

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` EN/DE/ES parity (incl. the new key) passes;
  both jars produced (`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`).
- Live smoke test on Spigot/Paper/Folia still recommended (no live server was available here).

## Folia entity-region correctness — NOW COMPLETED (second pass)
The previously-deferred global-thread loops now dispatch their per-entity work to the owning region
thread under Folia (inline on regular Bukkit/Paper — zero overhead there):
- **AFK tick** (`AfkServiceImpl.tickPlayer` via `runAtEntity`) — set-AFK messages + kick.
- **Vanish action bars** (`VanishManager.refreshActionBars` via `runAtEntity`).
- **Freeze reminders** (`FreezeService.sendReminders` via `runAtEntity`).
- **Holograms** (`HologramRenderer.runForHologram` via `runAtLocation`) — the tick loop and
  spawn/spawnAll/destroy now run entity create/remove/move on the hologram's region, so the
  armor-stand backend renders correctly on Folia instead of silently failing the thread check.

## Remaining (documented, lower-severity / larger-rework)
These were assessed and intentionally deferred over making untestable, higher-risk changes right before
launch — none are dupe/loss/exploit vectors:
- **Storage**: deeper connection concurrency (a dedicated writer / small pool instead of one shared
  connection + global lock). `busy_timeout` + WAL + `synchronous=NORMAL` mitigate the contention; the
  full rework is a larger architectural change.
- **Scoreboard `applyNameTeams`** O(N²)-per-refresh online scan — a perf optimization for very large
  player counts (local SQLite + per-player boards make it acceptable at typical scales).

## Suggested Commit Message
```
Hardening: fix all Critical/High/Medium audit findings for market readiness

deathdrop: atomic pickup, merge-cancel, void/lava guard, PDC persistence; kit: atomic first-join;
enchant/warp GUIs: re-check perms on click; moderation: mute /rply bypass + exempt hierarchy;
jail: redirect PLUGIN teleports back to anchor; storage: busy_timeout + periodic playtime flush +
no silent $0; scoreboard: quit/disable cleanup; tablist: refresh floor + team-mutation lock;
messaging: MessageStore synchronized + length caps; module: roll back failed enables; +medium fixes.
```
