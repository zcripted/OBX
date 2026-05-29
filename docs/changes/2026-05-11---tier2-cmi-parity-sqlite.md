■ **Created:** 2026-05-11 5:00 pm

■ **Last Updated:** 2026-05-11 5:00 pm

# Tier 2 CMI parity + SQLite migration

Second batch of CMI-style essentials, plus a foundational move of all player data to SQLite. Tier 1 services (playtime, economy, mail/ignore, kit cooldowns) are migrated as part of this batch. Pre-Tier-1 player data (homes/back in `data.yml`, `moderation.yml`) is left in YAML for a separate migration batch to keep this change reviewable.

## SQLite foundation

- **Driver:** `org.xerial:sqlite-jdbc 3.45.3.0`, declared via Paper's `libraries:` field in `plugin.yml` (auto-downloaded on Paper/Folia 1.16.5+). For Spigot/PurPur where `libraries:` is ignored, the driver must be present on the server classpath (drop the jar into `plugins/` or the server `libraries/` folder). The plugin logs a clear error and refuses to enable if the driver is missing.
- **File:** `plugins/SF-Core/sf-core.db` — single SQLite file.
- **Threading:** all writes go through `SchedulerAdapter#runAsync`. Reads from command handlers run synchronously (SQLite is in-process and fast). Frequently-read state (nicknames, fly flags, jail flags) is cached in memory on join and write-through on mutation.
- **Schema bootstrap:** each service creates its own tables via `CREATE TABLE IF NOT EXISTS` on `load()`. Schema migrations are additive (`ALTER TABLE … ADD COLUMN`) when needed.
- **Migration:** services with legacy YAML files (`playtime.yml`, `economy.yml`, `messaging.yml`, `kits-data.yml`) read the YAML once on first SQLite boot, bulk-insert into SQLite, then rename the YAML file to `<name>.migrated` so the migration never repeats.

## Categories shipped

### 1. Flight & movement (`command/utility/`, `command/admin/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/fly [player]` | `flight` | `sfcore.fly` / `sfcore.fly.others` | op |
| `/flyspeed <0-10>` | `fspeed` | `sfcore.flyspeed` | op |
| `/walkspeed <0-10>` | `wspeed` | `sfcore.walkspeed` | op |
| `/freeze <player>` | `frz` | `sfcore.freeze` | op |

`FlightStateService` persists fly/fly-speed/walk-speed per player to SQLite so toggling carries across reconnects. `FreezeService` is in-memory only (transient state, intentionally not persisted).

### 2. Inventory utilities (`command/utility/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/enderchest [player]` | `ec` | `sfcore.enderchest` / `sfcore.enderchest.others` | op |
| `/disposal` | `trash` | `sfcore.disposal` | op |
| `/hat` | `head-on` | `sfcore.hat` | op |
| `/clearinv [player]` | `ci`, `clearinventory` | `sfcore.clearinv` / `sfcore.clearinv.others` | op |
| `/repair [all]` | `fix` | `sfcore.repair` / `sfcore.repair.all` | op |
| `/more` | `stack`, `max` | `sfcore.more` | op |
| `/skull <player>` | `head` | `sfcore.skull` | op |

### 3. Item editing (`command/utility/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/itemname <name>` | `rename` | `sfcore.itemname` | op |
| `/itemlore <line>` | `lore` | `sfcore.itemlore` | op |
| `/unbreakable` | `unb` | `sfcore.unbreakable` | op |
| `/give <player> <material> [amount]` | — | `sfcore.give` | op |
| `/i <material> [amount]` | `item` | `sfcore.item` | op |
| `/book [unsign\|copy\|new]` | — | `sfcore.book` | op |

### 4. Nicknames (`nickname/`, `command/utility/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/nick <name\|off> [player]` | `nickname` | `sfcore.nick` / `sfcore.nick.others` | op |
| `/nick color` (perm gate) | — | `sfcore.nick.color` | op |

`NicknameService` is SQLite-backed and writes to `Player#setDisplayName` + `setPlayerListName` on join (`NicknameApplyListener`). Colors require `sfcore.nick.color`.

### 5. World, time, weather (`command/world/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/time <set\|add> <value>` | — | `sfcore.time` | op |
| `/day` | — | `sfcore.time` | op |
| `/night` | — | `sfcore.time` | op |
| `/sun` | — | `sfcore.weather` | op |
| `/weather <clear\|rain\|thunder>` | — | `sfcore.weather` | op |
| `/ptime <time\|reset>` | — | `sfcore.ptime` | op |
| `/pweather <clear\|rain\|thunder\|reset>` | — | `sfcore.pweather` | op |

`PerPlayerTimeService` persists ptime overrides per player to SQLite and applies them on join.

### 6. Jail (`jail/`, `command/admin/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/jail <player> <jail> [duration] [reason]` | — | `sfcore.jail` | op |
| `/unjail <player>` | — | `sfcore.unjail` | op |
| `/jails` | `jaillist` | `sfcore.jails` | op |
| `/setjail <name>` | `jailset` | `sfcore.setjail` | op |
| `/deljail <name>` | `jaildel` | `sfcore.deljail` | op |
| `/jailtime [player]` | — | `sfcore.jailtime` | op |

`JailService` persists jail state to SQLite (`jail_state` table) and jail location definitions to `jails.yml` (admin config). `JailListener` re-teleports jailed players to their jail on join, blocks teleport-out, and clears state when the sentence expires.

### 7. Mob & world tools (`command/admin/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/butcher [radius] [type]` | `killmobs` | `sfcore.butcher` | op |
| `/spawnmob <type> [count]` | `mob` | `sfcore.spawnmob` | op |
| `/spawner <type>` | `setspawner` | `sfcore.spawner` | op |
| `/smite [player]` | `lightning` | `sfcore.smite` | op |
| `/tree [type]` | — | `sfcore.tree` | op |

## Adventure-driven prompts

- `/jail` confirmation: target receives a hover-tooltip line showing jail name, duration, and reason; staff get a clickable `[Unjail]` button on the success line.
- `/freeze`: subject sees a recurring action-bar reminder while frozen.
- `/nick`: when staff sets another player's nickname, the success line surfaces a hover with the resolved real name.

## Files touched

### New
- `storage/SqliteDataStore.java`
- `util/control/FlightStateService.java`, `util/control/FreezeService.java`
- `nickname/NicknameService.java`, `nickname/NicknameApplyListener.java`
- `util/control/PerPlayerTimeService.java`
- `jail/Jail.java`, `jail/JailService.java`, `jail/JailListener.java`
- ~33 new command files across `command/utility/`, `command/admin/`, `command/world/`

### Edited
- `Main.java` — wire SQLite + 7 new services + listener registrations.
- `language/MessageDefaults.java` — full EN+DE keys for Tier 2.
- `resources/plugin.yml` — `libraries:` declaration, 30+ new commands, full permission tree.
- `resources/config.yml` — flight defaults, jail defaults, time-format options.
- `resources/jails.yml` — initial empty jail definitions file.
- `pom.xml` — `sqlite-jdbc` as `provided` (resolved at runtime by `libraries:`).
- `docs/information/about.md` — Tier 2 command/permission tables.

### Migrated (player data — YAML → SQLite)
- `playtime.yml` → `playtime` table
- `economy.yml` → `economy` table
- `messaging.yml` (ignores + mail) → `ignores` and `mail` tables
- `kits-data.yml` → `kit_cooldowns` and `kit_first_join` tables

Source YAML files are renamed to `<name>.migrated` after a one-shot import on first boot.

### Not migrated (out of scope this batch)
- `data.yml` (homes, back, spawn) — pre-Tier-1 data, follow-up migration.
- `moderation.yml` — complex profile/warns/history schema, follow-up migration.

## Suggested commit messages

```
Foundation: SQLite player-data store + one-shot YAML migration for Tier 1 services
Feature (flight): /fly /flyspeed /walkspeed /freeze with SQLite-persisted toggles
Feature (inventory): /enderchest /disposal /hat /clearinv /repair /more /skull
Feature (item-edit): /itemname /itemlore /unbreakable /give /i /book
Feature (nicknames): /nick + SQLite-backed NicknameService with tablist + chat reflection
Feature (world-time-weather): /time /day /night /sun /weather /ptime /pweather
Feature (jail): /jail /unjail /jails /setjail /deljail /jailtime + JailService
Feature (mob-tools): /butcher /spawnmob /spawner /smite /tree
```
