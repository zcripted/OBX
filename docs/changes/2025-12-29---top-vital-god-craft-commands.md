**Created:** 2025-12-29 8:46 pm
**Last Updated:** 2025-12-29 10:17 pm

## Commands
- Added /top safe-teleport command with target support and back-location handling. (`src/main/java/dev/zcripted/obx/command/teleportation/TopCommand.java`)
- Added /vital and /god player-management commands with targeting, messages, and tab completion. (`src/main/java/dev/zcripted/obx/command/utility/VitalCommand.java`, `src/main/java/dev/zcripted/obx/command/utility/GodCommand.java`)
- Added /craft item-profile command with rich hover sections and full material tab suggestions; usage/fallback messaging added. (`src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`)
- Updated /craft to use the held item when no argument is provided. (`src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`)
- Enhanced /craft recipe hover with a crafting-grid layout and output line. (`src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`)
- Condensed the /craft recipe hover into a single-line grid layout to reduce height. (`src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`)
- Added /kill crosshair targeting command with line-of-sight validation and instant kill. (`src/main/java/dev/zcripted/obx/command/admin/KillCommand.java`)
- Updated /kill to toggle kill mode and execute on left-click targeting. (`src/main/java/dev/zcripted/obx/command/admin/KillCommand.java`, `src/main/java/dev/zcripted/obx/util/control/KillModeManager.java`)

## Gameplay / Safety
- Added god-mode damage/combust immunity listener and state tracking. (`src/main/java/dev/zcripted/obx/util/control/GodModeManager.java`)
- Implemented safe top landing checks to avoid trees and hazardous/unstable blocks. (`src/main/java/dev/zcripted/obx/command/teleportation/TopCommand.java`)
- Assumption: /craft hover summaries use heuristic data for use/find/dimensions because Bukkit lacks reliable biome/obtain APIs. (`src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`)

## Wiring / Permissions
- Registered new commands and listeners. (`src/main/java/dev/zcripted/obx/Main.java`)
- Added command definitions and permission nodes for top/vital/god/craft. (`src/main/resources/plugin.yml`)
- Added command definitions and permission node for kill. (`src/main/resources/plugin.yml`)

## Config
- Added configurable max range for /kill targeting. (`src/main/resources/config.yml`)

## Language
- Added new language keys and sections for player management, /top, /craft, and /kill output. (`src/main/java/dev/zcripted/obx/language/MessageDefaults.java`, `src/main/resources/languages/language_en.yml`, `src/main/resources/languages/sprache_de.yml`)

## Documentation
- Documented new commands and permissions. (`COMMANDS+PERMISSIONS.md`)
- Logged change summary entry. (`docs/changes/2025-12-29---top-vital-god-craft-commands.md`)