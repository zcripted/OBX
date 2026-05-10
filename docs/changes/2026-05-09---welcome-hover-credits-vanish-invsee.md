# Welcome MOTD hover/credits + /vanish + /invsee staff tools

■ **Created:** 2026-05-09 12:55 am

■ **Last Updated:** 2026-05-09 12:55 am

## Summary

Three independent additions in one drop:

1. **Welcome MOTD enrichment** — the `Welcome to the Server` line now has a
   detailed hover tooltip pointing at the YAML location and toggle for the
   message; `/sf help` and the Discord URL got descriptive click-hover labels;
   a new last credits line links GitHub / Spigot / BuiltByBit with hover and
   click events. Each tooltip line stays at or below 45 visible characters
   and never breaks a word across lines.
2. **`/vanish`** — staff invisibility toggle backed by a new `VanishManager`.
   Implements true vanish: hidden from other players via reflective
   `Player.hidePlayer(Plugin, Player)` (1.13+) with legacy `hidePlayer(Player)`
   fallback (1.8 – 1.12), suppresses mob targeting, blocks passive damage
   (combust, food drain, ranged attacks), and silences pickup events. Toggles
   are mirrored to console as a single ANSI-coloured staff line.
3. **`/invsee`** — two-tier inventory viewer. `sfcore.invsee.basic` can only
   view non-op players that hold no `sfcore.invsee.*` permission;
   `sfcore.invsee.full` can view anyone. Tab-completion respects the tier and
   silently filters privileged players from suggestions for basic-tier
   staff. Each open is logged to console with the tier marker.

All staff-action console output is sourced from language keys — no message
text is hardcoded in the command classes. Templates, state labels, tier
markers, and detail labels live in `MessageDefaults` (English + German).

## Categories

### Config (Welcome MOTD)

- `src/main/resources/config.yml`
    - `join-motd.lines[1]` — wrapped the gradient line in `<hover:show_text:'…'>`
      with a 16-line tooltip describing config file path, YAML section,
      first-join variant, and toggle.
    - `join-motd.lines[3]` (`/sf help` line) — added `<hover:show_text:…>`
      describing the click action.
    - `join-motd.lines[4]` (Discord line) — added matching hover.
    - `join-motd.lines[5]` (NEW credits line) — `Made by SergeantFuzzy •
      [GitHub] [Spigot] [BuiltByBit]` with one click+hover pair per link.

### Commands (NEW)

- `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/VanishCommand.java`
    - `CommandExecutor + TabCompleter` for `/vanish [player]`. Permission-gated
      (`sfcore.vanish` self, `sfcore.vanish.others` for targeted use).
      Tab-completion only fires for users with the `others` permission.
- `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/InvSeeCommand.java`
    - `CommandExecutor + TabCompleter` for `/invsee <player>`. Two-tier
      permission check: a basic-tier executor sees only non-privileged
      targets in tab-completion and at execution; full-tier sees everyone.
- Both commands route their console log lines through
  `LanguageManager.formatConsole(key, replacements)` and
  `Main.writeConsoleLine(...)` so the line renders with ANSI truecolor on
  capable terminals and falls back to plain text on non-ANSI consoles.

### Internal / Infrastructure

- `src/main/java/dev/sergeantfuzzy/sfcore/util/control/VanishManager.java`
    - State: `Set<UUID>` of vanished staff (in-memory, session-local;
      `PlayerQuitEvent` clears the entry so a logout drops the flag).
    - Reflective lookup of `hidePlayer(Plugin, Player)` /
      `showPlayer(Plugin, Player)` (1.13+) and the legacy
      `hidePlayer(Player)` / `showPlayer(Player)` (1.8 – 1.12). The toggle
      tries the modern overload first and falls through to the legacy
      overload — keeps a single JAR working on every supported version.
    - Listener handlers:
        - `PlayerJoinEvent` — re-hides every currently-vanished player from
          the joiner's view.
        - `EntityTargetLivingEntityEvent` — cancels mob aggro on vanished
          players.
        - `EntityCombustEvent` — cancels passive fire damage and clears
          fire ticks.
        - `EntityDamageByEntityEvent` — cancels incoming damage on vanished
          players and resets the attacker's target via
          `Creature.setTarget(null)` (avoids hard reference to the 1.13+
          `Mob` interface so the code stays compile-clean against the
          1.8.8 spigot-api baseline).
        - `PlayerPickupItemEvent` — cancels item pickup while vanished.
        - `FoodLevelChangeEvent` — cancels food drain.

### Permissions (`plugin.yml`)

- New `sfcore.vanish.*` umbrella with `sfcore.vanish` (self) and
  `sfcore.vanish.others` children, default `op`.
- New `sfcore.invsee.*` umbrella with `sfcore.invsee.basic` (lower tier) and
  `sfcore.invsee.full` (higher tier) children, default `op`.
- Both umbrellas are added to `sfcore.*` so a single grant carries them.

### Wiring (`Main.java`)

- New imports for `VanishCommand`, `InvSeeCommand`, `VanishManager`.
- New field + getter for `vanishManager`.
- `vanishManager` instantiated next to `godModeManager` /
  `killModeManager` in `onEnable`.
- `vanishManager` registered as an event listener alongside the other
  managers.
- Two new `bind(...)` calls in `registerCommands()`.

### Language strings (`MessageDefaults.java`)

New keys, English + German:

- `player.vanish.usage-console`
- `player.vanish.target-not-found`
- `player.vanish.enabled`, `player.vanish.disabled`
- `player.vanish.enabled-other`, `player.vanish.disabled-other`
- `player.vanish.enabled-target`, `player.vanish.disabled-target`
- `player.vanish.console-log` — full ANSI-formattable log template
- `player.vanish.console-state-on`, `…-state-off`
- `player.vanish.console-detail-self`, `…-detail-other`
- `player.invsee.usage`, `player.invsee.usage-console`
- `player.invsee.target-not-found`, `player.invsee.cannot-view-self`
- `player.invsee.no-permission-target`, `player.invsee.opened`,
  `player.invsee.open-failed`
- `player.invsee.console-log`, `…-tier-basic`, `…-tier-full`

### Documentation

- `docs/information/about.md` — added `/vanish` and `/invsee` rows under
  Player Management with both permission tiers documented.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/util/control/VanishManager.java` (new)
- `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/VanishCommand.java` (new)
- `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/InvSeeCommand.java` (new)
- `src/main/java/dev/sergeantfuzzy/sfcore/Main.java` (imports, field, getter,
  init, command bind, listener register)
- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
  (vanish + invsee message keys)
- `src/main/resources/plugin.yml` (commands, permissions, umbrella inclusion)
- `src/main/resources/config.yml` (welcome MOTD: hover on welcome,
  /sf help, discord; new credits line)
- `docs/information/about.md` (new command rows)

## Suggested Commit Message

```
Feature (welcome+staff): MOTD hover/credits, /vanish, /invsee tier-aware
```
