# Round 4 — Wiring, Folia, and Missing-Key Fixes (+ COMMANDS doc reorg)

■ **Created:** 2026-06-04 9:39 am (America/Detroit)

■ **Last Updated:** 2026-06-04 9:39 am (America/Detroit)

Fixes from the fourth market-readiness assessment: a dead-feature BLOCKER, two service-wiring
HIGH gaps, a Folia crash path, a family of missing teleport message keys, and LOW hardening —
plus a full reorganization of the commands/permissions reference document.

Build green (`./gradlew build`); both jars produced; EN/DE/ES parity test passes (19 new keys).

---

## BLOCKER

- **Playtime was completely non-functional — it never recorded.** `PlaytimeService implements
  Listener` (its `onJoin`/`onQuit` write the playtime sessions), but `PlayerInfoModule` only
  registered it as a *service* — never `listener(...)` — so its `@EventHandler`s never fired and
  no playtime ever accumulated (`/playtime` always showed `0`). Added `listener(playtime);`.
  *(This also means the round-4 session-accuracy fix was operating on never-populated maps.)*
  File: `features/playerinfo/.../PlayerInfoModule.java`.

## HIGH

- **TpaService never wired.** Same class of bug — registered as a service with
  `onDisable(stop)`, but never `listener(...)` and `start()` was never called, so the expiry
  sweeper never ran and the quit-cleanup handler never fired (pending-request state leaked).
  Added `listener(tpaService); tpaService.start();`. File: `features/teleport/.../TeleportModule.java`.
- **Moderation kicks weren't Folia region-dispatched.** `ban`/`tempBan`/`kick`/`ipBan` called
  `Player.kickPlayer(...)` directly (ipBan looped it over all online players). On Folia, kicking
  a *different* player is a cross-region access that throws `IllegalStateException`. Added a
  `kickSafe(player, msg)` helper that dispatches via `runAtEntity` on Folia (inline on Bukkit)
  and routed all four call sites through it. File: `features/moderation/.../service/ModerationService.java`.

## MEDIUM

- **Missing teleport message keys → raw-key leaks.** A whole family of `tpa.*` and
  `teleport.tphere/tpall/tppos.*` keys was referenced in code but absent from all three
  catalogues, so players saw literal text like `{prefix}tpa.no-pending` on `/tpa accept/deny`,
  `/tpcancel`, `/tphere`, `/tpall`, and `/tppos`. Added **19 keys** to EN/DE/ES:
  `tpa.{no-pending, sender-offline, accepted-receiver, accepted-sender, denied-receiver,
  denied-sender, teleporting, cancelled-sender, cancelled-receiver, cancel-none}`,
  `teleport.tphere.{success, target-message, usage}`, `teleport.tpall.success`,
  `teleport.tppos.{usage, invalid, unknown-world, teleporting}`. (`/tphere` and `/tppos` usage
  are box-style.) Files: `core/.../language/MessageDefaults{EN,DE,ES}.java`.
  *(`tpa.expiry-seconds` was a config key, not a message — correctly left alone.)*

## LOW

- **`<insertion>` sanitizer gap closed** — `sanitizeChat`'s interactive-tag stripper now covers
  both `insert` and the real `insertion` tag (`insert(?:ion)?`). File: `core/.../util/text/MessageSanitizer.java`.
- **Jail respawn containment** — added a `PlayerRespawnEvent` handler that redirects a jailed
  player's respawn to the jail anchor (cache-backed), closing the brief at-spawn window after
  death. File: `features/jail/.../listener/JailListener.java`.
- **`unban` now lifts a PROFILE-only ban** — if the NAME ban was cleared out-of-band, `unban`
  previously returned "not banned" and skipped the UUID/PROFILE pardon. Added a reflective
  `isProfileBanned(...)` check so a desynced profile ban is still pardoned.
  File: `features/moderation/.../service/ModerationService.java`.

## Documentation — `COMMANDS+PERMISSIONS.md` reorganized

- **Every section sorted A–Z** (alphabetized within labeled sub-groups for Admin / Teleport /
  Utility / Other).
- **Added the 3 missing commands** that were registered + declared but undocumented: `/day`,
  `/night`, `/sun` (World &amp; Environment).
- **New navigation/usability elements:** a "How to read this reference" column guide, a
  **Default-level legend** (everyone / true / op / false), server-owner **permission tips**
  (`.others` variants, bundle wildcards, tiered nodes, passive nodes), and the wildcard bundles
  broken into their own rows. Renamed the column header to "Default level" for clarity.
- Cross-checked against `plugin.yml` + every `command(...)` registration: **zero dead, zero
  undeclared** commands.

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` (EN/DE/ES parity incl. the 19 new keys) passes.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` + `-unobf.jar`.

## Suggested Commit Message
```
Fix (round 4): wire dead Playtime/Tpa services, Folia kicks, missing tpa keys

BLOCKER: register PlaytimeService as a listener (playtime never recorded). HIGH: wire
TpaService listener + sweeper; Folia region-dispatch moderation kicks. MEDIUM: add 19
missing tpa/tphere/tpall/tppos message keys (EN/DE/ES). LOW: <insertion> sanitizer,
jail respawn containment, unban profile-only pardon. Docs: reorganize COMMANDS+PERMISSIONS
A-Z with legend/tips + add /day /night /sun.
```
