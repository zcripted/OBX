# Moderation, Discord Logging, and Test Profiles

■ **Created:** 2026-04-06 5:33 pm

■ **Last Updated:** 2026-04-06 7:10 pm

## Categories

### Commands
- Added `/ban`, `/unban`, `/kick`, `/mute`, `/unmute`, `/tempban` (`/tban`), `/warn`, and `/banlist` (`/blist`) with dedicated permission nodes and tab completion.
- Added `/status <player>` to show a localized moderation profile card with active punishments, totals, and recent action history for live, stored, fake, and actively banned profiles.
- Added active-ban output for `/banlist`, plus mute/warn persistence for offline or configured fake test profiles.
- Updated OBX config diagnostics to include `moderation.yml` and moderation-service reload coverage.
- Added duplicate-state safeguards so repeated `ban`, `tempban`, `mute`, `unban`, and `unmute` actions are blocked instead of being re-applied.
- Fixed `/kick` so configured fake test profiles can be processed for moderation testing, while real non-fake targets still require an online player session.

Files:
- `src/main/java/dev/zcripted/obx/Main.java`
- `src/main/java/dev/zcripted/obx/command/moderation/ModerationCommand.java`
- `src/main/java/dev/zcripted/obx/command/moderation/BanListCommand.java`
- `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
- `src/main/resources/plugin.yml`

### Discord
- Added `discord.moderation` settings in `config.yml` with the requested server ID and moderation channel ID (real IDs scrubbed from the repo on 2026-06-06 for release).
- Added webhook-based Discord moderation posting with console fallback logging and a one-time warning when IDs are configured but `webhook-url` is still empty.
- Logged ban, unban, tempban, kick, mute, unmute, and warn actions through the moderation service.
- Reworked console moderation logs to use past-tense action phrasing and ANSI-colored professional formatting such as `Moderation: Unbanned HotPotato | By: zcripted | reason=...`.
- Refactored moderation console logging to use OBX's shared console formatting pipeline in `Main.java` instead of maintaining separate ANSI escape handling inside the moderation service.
- Replaced plain Discord webhook text posts with structured moderation embeds using action-specific colors, standardized audit fields, footer branding, and ISO timestamps.
- Added configurable webhook avatar/footer icon URL support so the Discord webhook profile avatar and embed footer icon can both use the OBX branding image once it is hosted at a public URL.
- Updated the Discord embed title to `Moderation | Action: <ACTION> <player>` and wrapped Action, Target, By, Duration, and Server ID field values in inline code formatting for clearer audit readability.

Files:
- `src/main/java/dev/zcripted/obx/moderation/ModerationService.java`
- `src/main/resources/config.yml`

### Moderation Data
- Added persistent mute and warning storage in `moderation.yml`.
- Expanded `moderation.yml` usage to store per-player moderation action history so status lookups can display kicks, bans, tempbans, mutes, unbans, unmutes, and warnings.
- Kept bans on Bukkit's native name ban list for cross-version compatibility, while using the new moderation file for warning/mute metadata and stored profile resolution.
- Blocked muted players from chatting through the chat listener.

Files:
- `src/main/java/dev/zcripted/obx/moderation/ModerationService.java`
- `src/main/java/dev/zcripted/obx/listener/chat/ChatListener.java`
- `src/main/resources/moderation.yml`

### Testing
- Added reusable fake test profiles for `ChanceDaRepper`, `HotPotato`, and `VeryPotter` in `config.yml`.
- Wired those fake profiles into MOTD sample-profile output so the server list can show realistic test names.

Files:
- `src/main/resources/config.yml`
- `src/main/java/dev/zcripted/obx/storage/MotdService.java`
- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
- `src/main/resources/motd.yml`

### GUI, Localization, and Docs
- Updated the admin moderation placeholder to reflect the new live moderation command set.
- Added English and German moderation messages, banlist output, config validation/list entries, and reload hover updates.
- Updated command and permission documentation tables.

Files:
- `src/main/java/dev/zcripted/obx/gui/admin/AdminMenu.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- `src/main/resources/languages/language_en.yml`
- `src/main/resources/languages/sprache_de.yml`
- `docs/information/about.md`
- `COMMANDS+PERMISSIONS.md`

### Assumptions
- `/tempban` uses the configured default duration from `config.yml` because the requested syntax did not include a duration argument. Current default: `7d`.
- Discord delivery requires `discord.moderation.webhook-url` to be filled with a webhook from the configured moderation channel. The requested server/channel IDs are stored now, but Discord cannot be posted to by ID alone.

Suggested Commit Message
```text
Feature (moderation): add Discord-backed staff actions and fake test profiles
```
