# MOTD Hover Display Mode Toggle

■ **Created:** 2026-04-06 5:46 pm

■ **Last Updated:** 2026-04-06 5:46 pm

## Categories

### Config
- Added `player-count.hover.display-mode` to `motd.yml`.
- Supported values:
  `profiles` = show sample player names in the server-list hover.
  `lines` = show the configured custom hover text lines instead.
- Defaulted the new setting to `profiles` to preserve the current behavior.

Files:
- `src/main/resources/motd.yml`

### Internal
- Updated the MOTD service to expose a parsed hover display mode enum.
- Updated the server list ping listener to respect the configured hover mode.
- Kept a safe fallback to custom lines when `profiles` is selected but no sample profile names are configured.

Files:
- `src/main/java/dev/sergeantfuzzy/sfcore/storage/MotdService.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/server/MotdPingListener.java`

Suggested Commit Message
```text
Feature (motd): add hover display mode toggle for profiles vs lines
```
