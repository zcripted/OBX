■ **Created:** 2026-04-05 9:01 PM
■ **Last Updated:** 2026-04-05 9:39 PM

## Config
- Added a new default `motd.yml` resource for server-list MOTD management.
- Added configurable MOTD line 1 / line 2 text, pixel-centering support, displayed online-count mode (`real` or `fake`), configurable max count, and configurable player-count hover lines.

## Internal
- Added `MotdService` to load and reload `motd.yml` with placeholder-aware MOTD rendering.
- Added `MotdPingListener` to apply MOTD text, displayed player counts, and hover sample overrides during server list pings.
- Added `MotdMessageUtil` under `dev.sergeantfuzzy.sfcore.util.message` to support `<center>...</center>` tags with pixel-based MOTD centering.
- Used reflective profile/sample hooks so custom hover text can work across mixed legacy and modern server implementations without changing the 1.12.2 compile target.
- Followed up with a centering fix by correcting the effective MOTD center width and upgrading the original `123` default to the intended width in code.
- Followed up with an additional centering fix by moving MOTD padding behind leading color/format codes so the server-list line no longer begins with raw spaces.
- Followed up with a true-centering fix by replacing the rough MOTD character-width approximation with a fuller Minecraft font-width table so line width is calculated from the actual visible characters and formatting mix.
- Followed up with a hover fix by widening the reflective ping-sample hooks to cover more server implementations, including explicit player-list visibility toggles and additional sample/list setter paths.
- Followed up with a Paper 1.21.11 crash fix by detecting `PaperServerListPingEvent$ListedPlayerInfo` explicitly and constructing the expected sample-wrapper type instead of falling back to raw `GameProfile` entries.

## Admin Output
- Updated `/sf reload config` and `/sf reload` internals to reload `motd.yml`.
- Updated `/sf config` and `/sf config validate` output to include `motd.yml`.
- Synced the related English/German language entries and Java message defaults.

## Compatibility
- Updated project metadata and user-facing support strings to include Minecraft `26.1`.

## Documentation
- Updated the README to document `motd.yml`, centering tags, player-count customization, and the extended version-support range.

## Files
- `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/command/core/SFCoreCommand.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/server/MotdPingListener.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/storage/MotdService.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/util/message/MotdMessageUtil.java`
- `src/main/resources/languages/language_en.yml`
- `src/main/resources/languages/sprache_de.yml`
- `src/main/resources/motd.yml`
- `src/main/resources/plugin.yml`
- `pom.xml`
- `README.md`
- `docs/changes/2026-04-05---motd-and-26-1-support.md`

Suggested Commit Message
```
Feature (motd): add configurable MOTD system and 26.1 support metadata
```
