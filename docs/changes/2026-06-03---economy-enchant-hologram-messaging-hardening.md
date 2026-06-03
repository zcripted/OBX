# Economy/Enchant/Hologram/Messaging hardening + should-fix cleanup

■ **Created:** 2026-06-03 1:21 am (America/Detroit)

■ **Last Updated:** 2026-06-03 1:21 am (America/Detroit)

Fixes the release-blocking correctness/data-safety issues and the should-fix list
from the market-readiness assessment.

## Blockers

### Economy money-safety
- **Atomic transfer**: `transfer()` now runs a single SQL transaction (guarded
  debit → credit). The guarded `UPDATE … WHERE balance >= ?` affects 0 rows on
  insufficient funds and throws to roll back, so the recipient is never credited
  from an empty account.
- **No more read-modify-write races**: `deposit`/`withdraw` are now atomic
  (`balance = balance ± ?` / `MIN(balance + ?, cap)` in one statement); concurrent
  `/pay`/`/eco` can no longer dupe or destroy money.
- **Sanitisation**: new `EconomyService.sanitize()` drops `NaN`/`Infinity`,
  clamps to `[0, MAX_BALANCE]` (1e12 cap), and rounds to 2 decimals on every
  store. `/pay` and `/eco` reject non-finite amounts up front.
- `storage/SqliteDataStore.java` gained statement-level locking, a
  `transaction(...)` helper, `executeUpdateRows(...)`, and a `obx_schema_version`
  table (migration foundation).
- Files: `economy/EconomyService.java`, `command/economy/{PayCommand,EcoCommand}.java`, `storage/SqliteDataStore.java`

### Enchant
- **Scroll dupe closed**: scroll/book/protection/success applications now require
  a single-item target (`amount == 1`) — one scroll can no longer enchant a stack.
  (`enchant/scroll/ScrollDragListener.java`)
- **Satchel persistence**: satchel contents now persist to `satchels.yml` (loaded
  on open, saved on close via a new `SatchelCloseListener`, and on disable via
  `EnchantState.saveAll()`) — no more item loss on restart.
  (`enchant/effect/EnchantState.java`, `enchant/effect/SatchelCloseListener.java`, `Main.java`)
- **Folia**: `EnchantTickTask` now schedules through `SchedulerAdapter` and
  dispatches per-player work onto each player's region thread.
  (`enchant/effect/EnchantTickTask.java`)

### Hologram crash-duplication
- Spawned hologram entities are now tagged (restart-surviving scoreboard tag,
  reflective for 1.13+), and `HologramRenderer.spawnAll()` scrubs any tagged
  orphans before spawning — so a hard crash can't multiply holograms across boots.
  (`hologram/HologramTag.java`, `hologram/backend/{ArmorStandBackend,DisplayEntityBackend}.java`, `hologram/render/HologramRenderer.java`)

### Messaging dead commands
- `/ignore` and `/socialspy` are now **enforced in the real `/msg` delivery path**:
  ignored recipients block the PM (with an `obx.message.ignore.bypass` override),
  and social-spy staff receive a copy of delivered PMs.
  (`message/MessageService.java`, new keys `message.ignored` / `message.socialspy.format`)
  - Note: the two PM/mail systems (`message/` YAML inbox vs `messaging/` SQLite
    mail) remain separate by design; only the functional gap was wired. A future
    unification is a larger refactor, not a correctness issue.

## Should-fix cleanup
- **Scoreboard nametag O(N²)**: `applyNameTeams` is now incremental (move only on
  actual team change + drop offline entries) — steady-state renders send zero team
  packets. (`scoreboard/format/ScoreboardRenderer.java`)
- **Nickname color truncation**: long colored names fall back to the
  color-stripped tab name instead of a `substring` that could split a `§x` code.
  (`nickname/NicknameService.java`)
- **Jail bypass**: command allow-list now matches the exact base command (no more
  loose `startsWith`, so `/helpme` / `/obxescape` can't slip through).
  (`jail/JailListener.java`)
- **parseDuration**: now supports `s`, `m`, `h`, `d`, `w`, `mo`, `y` (was only m/h/d/w).
  (`moderation/ModerationService.java`)
- **MainMenu**: removed the player-facing "Coming Soon" tiles (now neutral filler)
  and the dead `createPlaceholderItem()`. (`gui/player/MainMenu.java`)
- **Dev defaults**: scoreboard default IP/website are now neutral
  (`play.example.net` / `example.net`). (`scoreboard/service/ScoreboardService.java`, `systems/scoreboard.yml`)
- **/obx updates check** help text no longer says "(placeholder)" — the command is
  real. (`language/MessageDefaults.java`)
- **Staff buttons**: each action now checks `obx.moderation.<action>` before
  prompting (no dead-end prompt). (`listener/menu/StaffMenuListener.java`)
- Also removed the 8 unused `*-placeholder` language keys and fixed 3 stale GUI
  lore strings (prior turn's cleanup, included here).

## Verification
- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; **20 tests, 0 failures**
  (added `EconomySanitizeTest`); both jars built (`OBX-1.0.0-beta-b1.jar` + `-unobf`).

## Caveats
- Runtime smoke test still recommended (no live server here): the economy
  transaction path, satchel persistence, hologram scrub, and ignore/spy flow are
  verified by compile + the unit suite + static review.
- Hologram tagging/scrub uses scoreboard tags (1.13+); on 1.8–1.12 it degrades to
  the prior behaviour (tagging/scrub are no-ops there).

## Suggested Commit Message

```
Harden economy/enchant/hologram/messaging + should-fix cleanup

Atomic transactional economy with NaN/cap/round sanitisation; close scroll dupe;
persist satchels; Folia-safe enchant tick; tag+scrub hologram entities to stop
crash duplication; enforce /ignore + /socialspy in /msg; incremental scoreboard
nametags; safer nick truncation, jail bypass, parseDuration units; drop
Coming-Soon tiles + dev defaults; per-action staff permission gating.
```
