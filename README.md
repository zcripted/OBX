![SF-Core Plugin Logo Image](https://sergeantfuzzy.dev/wiki/assets/img/sf-core.png)

SF-Core is a lightweight essentials-style core plugin for Paper, Spigot, and PurPur servers from **1.12.x through 1.21.11 and 26.1**. It focuses on broad compatibility (compiled against the 1.12.2 API) and a clean, configurable foundation similar to EssentialsX and CMI.

## Features

### Player Mobility & Safety
- **Homes, spawn, and first-join routing:** `/home`, `/sethome`, `/delhome`, and `/homes` read/write `data.yml` with the per-player limits defined in `homes.max-per-player` (default `5`). `/setspawn` stores who set spawn (UUID, name, timestamp), while `spawn.teleport-on-first-join` and `spawn.teleport-on-join` let you automatically move new or returning players as soon as they join.
- **Backtracking & vertical escapes:** `BackListener` persistently tracks the previous death or teleport position so `/back` is always accurate, and `/top` safely teleports staff to the highest safe block while respecting permissions.
- **Teleport warmups with messaging:** Every teleport flows through `TeleportManager`, which enforces `teleport.warmup-seconds`, optionally broadcasts localized start/finish lines, and cancels pending tasks when players leave to prevent stuck timers.

### Warp Network & Navigation
- **Full warp lifecycle commands:** The `/warp` suite covers teleporting self/others, listing, info, categories, GUI entry, creation, moves, renames, icon editing, visibility toggles, deletion, and per-category filtering. Each warp is persisted in `warps.yml` with category, icon material, public/hidden flag, optional permission gate, setter metadata, and ISO timestamps so staff can audit changes.
- **Inventory-driven browsing:** `WarpMenu` renders paged lists, category explorers, per-warp detail sheets, and a dedicated Warp Manager board (create/delete/rename/move/icon/visibility/hidden view toggle). Gradient titles, filler fallbacks for pre-1.13 panes, and permission-aware buttons ensure a consistent experience across 1.12.x–1.21.x.
- **Chat-integrated editing:** `WarpMenuInputManager` captures chat to finish create/rename/search flows (with cancel/clear keywords) and reopens the correct GUI automatically, while `WarpAccess` ensures players only see warps they’re allowed to use or manage.

### GUI-First Control Panels
- **`/sf` menus for players and staff:** Non-admins land in the Main Menu (quick info, warp shortcut, reserved slots for upcoming systems) and anyone with `sfcore.admin.menu` opens the Admin Menu, both styled via `WarpMenuStyling` to keep titles, dividers, and tooltips uniform.
- **Server control submenus:** The Admin Menu opens contextual boards for server state, player access, performance, world tools, gamerules, and plugin systems. Actions cover stop/restart flows (with double-click confirmation), kicking non-ops into spectator-only windows, toggling whitelists, and surfacing TPS/entity cleanup stubs.
- **Operational toggles:** `ServerControlState` powers join-lock and redstone-freeze switches baked into these menus, giving staff click-to-freeze automation that instantly ripples to `JoinLockListener` and `RedstoneControlListener`.

### Utility & Moderation Essentials
- **Quick life-savers:** `/heal`, `/feed`, `/vital`, and `/god` sit atop `GodModeManager`, which cancels damage/combust events and resets toggles when players quit to prevent dangling invulnerability. `/gamemode` exposes per-mode self/others nodes to match granular staff policies.
- **Precision moderation:** `/kill` enables a scoped “crosshair kill mode” backed by `KillModeManager`, tracing along the player’s view vector (up to `targeting.kill.max-range`, default `40`) and emitting localized success/failure lines.
- **Knowledge tools:** `/craft` renders hoverable item breakdowns via `ComponentMessenger`, letting staff share recipe info without leaving chat, and `/language` + `/sprache` offer per-player localization control for the rest of the toolkit.

### Admin Diagnostics & Maintenance
- **Reloads & validation:** `/sf reload`, `/sf reload config`, and `/sf reload <file>` rehydrate configs, languages, data, warps, and the auto resource-pack manager without bouncing the server. `/sf config` + `/sf config validate` expose loaded files and YAML issues directly in chat.
- **Debug + health reports:** `/sf diagnostics`/`full` summarize platform details, loaded modules, and errors, `/sf debug` toggles verbose logging (with `/sf debug dump` writing YAML bundles under `/plugins/SF-Core/logs/<date>---<player>.yml`), and `/sf updates notify` preserves per-user toggle state so noisy alerts stay opt-in.
- **Environment locks:** Join-lock and redstone-freeze controls, daylight-cycle overrides (with legacy fallbacks), entity-clearing helpers, and max-player adjustments centralize high-risk operations inside guarded admin menus instead of raw commands.

### Localization & Messaging
- **Bilingual message packs:** English (`language_en.yml`) and German (`sprache_de.yml`) ship with full key coverage, comments, and auto-sync defaults. `LanguageManager` stores each player’s preference in `player-languages.yml`, and `/language` or `/sprache` swaps it instantly.
- **Rich chat output:** `ComponentMessenger` bridges Bukkit, Bungee, and Adventure APIs to provide hover/click navigation inside `/sf help`, `/spawn info`, `/craft`, and admin prompts, keeping help links and confirmations readable even on legacy clients.
- **Interactive prompts:** `WarpMenuInputListener` and contextual language keys power chat-driven prompts (“type a new name or `cancel`”), ensuring every confirmation or warning respects the player’s locale and prefix formatting from `config.yml`.
- **Configurable server-list presentation:** `motd.yml` controls both MOTD lines, supports `<center>...</center>` true-centering tags for the server list, and can override displayed online/max counts plus the player-count hover sample text.

### Resource Packs, Data Persistence & Compatibility
- **Auto-managed resource pack:** `AutoResourcePackManager` can copy the bundled pack (`bundled-resourcepack/sf-core-gui-pack.zip`) into `plugins/SF-Core/resourcepack/`, reuse or compute a SHA-1 hash, honor `resource-pack.public-url`, and automatically apply it to players on join while watching for repeated failures.
- **Durable storage with graceful reloads:** `DataService` (`data.yml`) keeps spawn/back/home data, and `WarpService` (`warps.yml`) handles the entire warp catalog; both reload automatically when `/sf reload` runs so edits never require a restart.
- **Version-spanning polish:** The plugin is compiled against Spigot 1.12.2 yet targets Paper/Spigot/PurPur 1.12.x–1.21.11 and 26.1, dynamically picking item materials, hex coloring, Adventure/Bungee APIs, and reflective MOTD hooks so GUIs, gradients, server-list text, and console banners behave correctly on every supported platform.
- **Console + link metadata:** Startup/shutdown banners list version, release date, and configurable BuiltByBit/wiki/Discord links defined in `config.yml`, giving operators immediate context the moment the jar loads.

## Installation + Setup
1. Download the latest SF-Core jar (or build locally with Maven if you are developing).
2. Drop the jar into your server's `plugins/` folder.
3. Start the server to generate `config.yml`, `data.yml`, and `motd.yml`.
4. Adjust settings in `config.yml`, grant permissions as needed (see `COMMANDS+PERMISSIONS.md`), then run `/sf reload` or restart the server.

## Configuration
- `config.yml` sets the chat prefix, teleport warmup options, home limits, and message templates.
- `data.yml` is created automatically to store homes, spawn, and back locations.
- `motd.yml` controls the two MOTD lines, true centering tags, displayed player counts, and custom player-count hover text in the server list.
- Commands and permissions are defined in `plugin.yml` (declares `api-version: 1.13` while staying compatible with 1.12).

## Compatibility Notes
- Compiled against `spigot-api 1.12.2` for maximum backwards compatibility; it runs on newer servers without using version-locked API calls.
- If you rely on modern-world names, make sure those worlds exist on startup; missing worlds will prevent related teleports.

## Next Steps
- Add warps/kits/economy hooks, scheduler-based tasks, and per-world settings as needed.
- Expand the command set to cover the remaining EssentialsX/CMI staples.
