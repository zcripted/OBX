# Vanish — tiered visibility, action-bar indicator, above-head marker

■ **Created:** 2026-05-09 5:55 pm

## Summary

`/vanish` was a single-tier "invisible to everyone but yourself" toggle.
This change splits it into two tiers driven by permission, gates which
vanished players can see which other vanished players according to the
matrix below, paints a constant action-bar indicator while vanish is
active (refreshed once per second), and adds an above-head `[L]`
nameplate prefix on lower-tier vanished players so higher-tier admins
can supervise their staff at a glance.

### Tiers

| Permission | Tier | Visible to |
|---|---|---|
| `sfcore.vanish` | **Lower** | Other lower-tier vanished users + all higher-tier vanished users (with `[L]` above their head) |
| `sfcore.vanish.admin` | **Higher** | Nobody — including other higher-tier vanished users |

### Visibility matrix

```
Viewer →           Non-vanish  Lower-vanish  Higher-vanish
Target ↓
Non-vanish:           SEE         SEE           SEE
Lower-vanish:        HIDDEN       SEE           SEE (with [L] prefix)
Higher-vanish:       HIDDEN      HIDDEN        HIDDEN
```

Non-vanished viewers retain the existing "see no vanished anyone"
behaviour — the matrix only changes how vanished players see each
other.

## Categories

### Permissions

- New permission **`sfcore.vanish.admin`** (default `op`) — grants the
  higher tier. Holders are invisible to every other vanished player as
  well as to non-staff. Documented in `plugin.yml`.
- `sfcore.vanish.*` updated to include `sfcore.vanish.admin` as a
  child, so the existing `sfcore.*` umbrella still grants the full
  vanish surface to ops.
- `sfcore.vanish` description amended to flag it as the lower tier and
  to mention the `[L]` indicator visible to other vanished viewers.

### Internal

- `VanishManager` extended with a public `Tier` enum (`LOWER`,
  `HIGHER`) and a `Map<UUID, Tier>` replacing the previous
  `Set<UUID>`. Existing `isVanished(UUID|Player)` calls keep working;
  new `getTier(UUID|Player)` exposes the tier where needed.
- New `canSee(viewerTier, targetTier)` predicate centralises the
  visibility matrix — every `hide`/`show` decision in the manager
  routes through it.
- `rebuildVisibility(Player)` re-evaluates bidirectional visibility
  for one player against everyone and is called on toggle on/off.
- `start()` / `stop()` lifecycle methods run the action-bar refresh
  task and clean up vanish state on plugin disable (so a reload
  doesn't leave players permanently hidden under the previous Plugin
  instance's hide-key).
- The `Player#hidePlayer` / `showPlayer` reflective lookup, the
  legacy 1.8 fallback, and every other downstream behaviour
  (mob-aggro suppression, combust suppression, food suppression,
  pickup suppression, projectile/AOE damage cancel) are unchanged.

### GUIs / UX

- **Action-bar indicator** refreshed every 20 ticks (1 s) while a
  player is vanished:
  - LOWER → `● VANISHED | LOWER TIER | Visible to staff` (green dot)
  - HIGHER → `● VANISHED | HIGHER TIER | Invisible to all` (red dot)
  - Format is intentionally compact (`● LABEL | TIER | NOTE`) so it
    reads at a glance without crowding the action-bar slot.
  - On toggle off, the action bar is overwritten with an empty
    string so the stale "Vanished" line doesn't linger until vanilla
    fade.
- **Above-head `[L]` prefix** for lower-tier vanished players via a
  shared `sf_vanish_low` scoreboard team on the main scoreboard.
  Prefix is the constant `§7[§8L§7] ` (kept ≤ 16 chars to satisfy
  the legacy pre-1.13 team-prefix length cap). The prefix only
  becomes visible to viewers who can actually see the lower-tier
  player — i.e. other vanished operators per the matrix above —
  because non-vanished viewers don't render the player entity at all.

### Language

New EN + DE keys in `MessageDefaults`:

- `player.vanish.action-bar.lower` — green action-bar line.
- `player.vanish.action-bar.higher` — red action-bar line.

The above-head team prefix is intentionally kept as a Java constant
rather than a translatable string: scoreboard teams are server-global
(not per-viewer), and the legacy 16-char cap leaves no room for
locale-variant overrides without truncation. The "[L]" marker reads
identically across locales.

### Lifecycle wiring

- `Main.onEnable()` calls `vanishManager.start()` after the scheduler
  is available (alongside `tablistRefreshTask.start()` and
  `tpsService.start()`).
- `Main.onDisable()` calls `vanishManager.stop()` first thing — it
  reveals every currently-vanished player to every viewer and clears
  the team membership so a `/reload` doesn't strand players hidden
  behind the disabled plugin's hide-key.

## Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/control/VanishManager.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
- `src/main/resources/plugin.yml`

## Files added

- `docs/changes/2026-05-09---vanish-tiers-and-action-bar.md`

## Verification

- `& ".\maven\bin\mvn.cmd" -DskipTests package` produced a fresh
  obfuscated `target/SF-Core-1.0.0-SNAPSHOT.jar` with no `[ERROR]` or
  `BUILD FAILURE` lines. Only ProGuard `Note:` lines for reflective
  accesses (informational per CLAUDE.md).
- The visibility matrix is centralised in one pure predicate
  (`canSee`) so the test surface is one truth table — verified by
  inspection against the spec.
- Action-bar task is idempotent (`start()` no-ops on re-call) and
  cancellable via `stop()`.
- Scoreboard team add / remove are wrapped in `try/Throwable` so any
  platform that quirks `Team#addEntry` (e.g. an exotic 1.7-shaped
  fork) silently degrades to "no nameplate prefix" rather than
  breaking the toggle.

## Notes / non-changes

- `VanishCommand` is unchanged — it still calls `vanishManager.toggle`
  and gets the same boolean back. Tier resolution happens inside the
  manager so the command surface is identical.
- `docs/information/about.md` will need its admin-tier-permissions
  table refreshed in a follow-up doc pass — not done in this change
  because the per-command help and tab-completion behaviour didn't
  move.
- No new InvSee-style `/vanish admin` subcommand was added; the tier
  is purely permission-driven so the same `/vanish` invocation
  produces the higher tier when invoked by a holder of
  `sfcore.vanish.admin`. This keeps the operator UX identical and
  avoids splitting the command surface.

## Suggested Commit Message

```
Feature (vanish): split into lower/higher tiers with a tier-aware visibility matrix, a constant action-bar indicator, and an above-head [L] nameplate prefix on lower-tier vanished users so admins can supervise their staff
```
