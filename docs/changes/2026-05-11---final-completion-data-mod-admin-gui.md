■ **Created:** 2026-05-11 6:30 pm

■ **Last Updated:** 2026-05-11 6:30 pm

# Final completion batch — DataService + ModerationService SQLite migration, admin GUI hooks

Closes the three follow-up items flagged at the end of the Tier 2 batch:

1. `data.yml` (homes, back, spawn) → SQLite
2. `moderation.yml` (profiles, mutes, warns, action history) → SQLite
3. `/sf` admin GUI exposes Tier 2 staff commands

After this batch every per-player record SF-Core writes lives in SQLite, and the only YAML files the plugin still owns are admin-configured definitions (`config.yml`, `motd.yml`, `kits.yml`, `worth.yml`, `jails.yml`, language packs).

## SQLite schema additions

```
homes(uuid, name, world, x, y, z, yaw, pitch)         -- PK (uuid, name)
back_locations(uuid PRIMARY KEY, world, x, y, z, yaw, pitch)
server_spawn(id INTEGER PRIMARY KEY CHECK (id = 1), world, x, y, z, yaw, pitch,
             set_by_uuid, set_by_name, set_at)
moderation_profiles(uuid PRIMARY KEY, name, fake_profile, last_updated)
moderation_mutes(uuid PRIMARY KEY, active, reason, actor, issued_at)
moderation_warnings(id AUTOINCREMENT, uuid, actor, reason, issued_at)
moderation_history(id AUTOINCREMENT, uuid, action, actor, reason, issued_at,
                   duration, details)
```

## Migrations (one-shot on first SQLite boot)

- `data.yml` → reads `spawn`, `players.<uuid>.back`, and `players.<uuid>.homes.<name>` sections, bulk-inserts into the new tables, renames the file to `data.yml.migrated`.
- `moderation.yml` → reads every `players.<name>` section, resolves the stored UUID (or synthesises a deterministic one if missing), upserts profile + mute + warnings + history rows, renames the file to `moderation.yml.migrated`.

Legacy YAML schemas keyed by lowercase name; new SQLite schemas key by UUID and store the display name as a column. Name → UUID lookups go through `LOWER(name) = LOWER(?)` so existing call sites (most of which pass a name) keep working.

## Admin GUI additions

The `/sf` admin tree now hosts:

- **Jail Center** sub-menu — opens from the "Moderation" placeholder. Lists configured jails, shows the count of currently jailed players, and surfaces a "Set jail here" button that runs `/setjail <name>` after a chat prompt.
- **Mob Tools** sub-menu — opens from the "Fun Utilities" placeholder. Clickable buttons for `/butcher 32`, `/smite` (at crosshair), `/tree`, and a hint for `/spawnmob`.
- **Staff Action Menu** (per-player from `/staff`) — adds Tier 2 buttons: `/fly <player>` toggle, `/freeze <player>` toggle, `/unjail <player>` (visible when target is jailed).

## Files touched

### New
- `storage/SqliteSpawnInfo.java` (placeholder DTO — actually reuses `DataService.SpawnInfo`)
- `command/admin/JailCenterMenu.java` (admin GUI)
- `command/admin/MobToolsMenu.java` (admin GUI)

### Edited
- `storage/DataService.java` — full SQLite rewrite + legacy `data.yml` migration.
- `moderation/ModerationService.java` — full SQLite rewrite + legacy `moderation.yml` migration. Discord webhook + name resolution preserved.
- `gui/admin/AdminSubMenu.java` — new `JAIL_CENTER` and `MOB_TOOLS` enum values + openers.
- `gui/admin/StaffActionMenu.java` — new fly/freeze/unjail buttons in the per-player action panel.
- `listener/menu/MainMenuListener.java` — click dispatch for the new sub-menu types.

## Out-of-scope confirmation

All originally-deferred items are closed:

- [x] `data.yml` → SQLite
- [x] `moderation.yml` → SQLite
- [x] `/sf` admin GUI hooks for jail/freeze/fly/butcher/smite/tree

Remaining YAML files are admin configuration only (commit-tracked, hand-editable, no per-player blobs).

## Suggested commit messages

```
Foundation: data.yml homes/back/spawn migrated to SQLite
Foundation: moderation.yml profiles/mutes/warns/history migrated to SQLite
Feature (admin-gui): Jail Center, Mob Tools sub-menus + staff action buttons
```
