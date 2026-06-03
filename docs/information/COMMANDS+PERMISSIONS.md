# Commands and Permissions

Every OBX command is grouped into the same filterable categories used by the in-game
`/help` GUI: **Admin**, **Core**, **Language**, **Messaging**, **Moderation**, **Teleport**,
**Utility**, and **Other**. Each category below has its own table. In `/help`, a command
appears for any viewer who has permission to run it, listed alphabetically.

## Index

- [Admin](#admin)
- [Core](#core)
- [Language](#language)
- [Messaging](#messaging)
- [Moderation](#moderation)
- [Teleport](#teleport)
- [Utility](#utility)
- [Other](#other)

Each table uses these columns: **Command** · **Aliases** · **Usage (arguments)** ·
**Example usage** · **Description** · **OP/Default permission level** · **Permission node**.

---

## Admin

Operational, diagnostic, and admin-teleport commands. Most live under `/obx` and require op.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Admin teleport** | `--` | `--` |
| `/tp` | Command: /teleport | `/tp <player> [target]` | `/tp Notch` | Teleport to a player, or move one player to another. The console can move players but cannot teleport itself, and there is no console to teleport to. | op | `obx.teleport.admin` |
| `/tphere` | `--` | `/tphere <player>` | `/tphere Notch` | Bring a player to your location. | op | `obx.teleport.admin` |
| `/kill` | Command: /crosskill, /aimkill, /targetkill | `/kill` | `/kill` | Toggle kill mode; left-click to kill the entity in your crosshairs. | op | `obx.kill` |
| `--` | `--` | `--` | `--` | **Config & Debug** | `--` | `--` |
| `/obx config` | Command: /obsidian, /obc | `/obx config` | `/obx config` | Displays loaded config files and their status. | op | `obx.debug.config` |
| `/obx config validate` | Command: /obsidian, /obc | `/obx config validate` | `/obx config validate` | Validates config files and reports errors or deprecated keys. | op | `obx.debug.config.validate` |
| `/obx debug` | Command: /obsidian, /obc | `/obx debug` | `/obx debug` | Shows debug status and active debug flags. | op | `obx.debug` |
| `/obx debug disable` | Command: /obsidian, /obc | `/obx debug disable` | `/obx debug disable` | Disables debug logging. | op | `obx.debug.toggle` |
| `/obx debug dump` | Command: /obsidian, /obc | `/obx debug dump` | `/obx debug dump` | Dumps internal state to a log file for troubleshooting. | op | `obx.debug.dump` |
| `/obx debug enable` | Command: /obsidian, /obc | `/obx debug enable` | `/obx debug enable` | Enables debug logging temporarily. | op | `obx.debug.toggle` |
| `--` | `--` | `--` | `--` | **Reload & Diagnostics** | `--` | `--` |
| `/obx diagnostics` | Command: /obsidian, /obc | `/obx diagnostics` | `/obx diagnostics` | Runs a quick health check (config status, loaded modules, platform info). | op | `obx.admin.diagnostics` |
| `/obx diagnostics full` | Command: /obsidian, /obc | `/obx diagnostics full` | `/obx diagnostics full` | Outputs extended diagnostics including services, hooks, and errors. | op | `obx.admin.diagnostics.full` |
| `/obx reload` | Command: /obsidian, /obc | `/obx reload` | `/obx reload` | Reloads OBX configs and reinitializes all feature modules safely. | op | `obx.admin.reload` |
| `/obx reload <file>` | Command: /obsidian, /obc | `/obx reload <file>` | `/obx reload config.yml` | Reloads a specific file from the plugin data folder. | op | `obx.admin.reload.features` |
| `/obx reload config` | Command: /obsidian, /obc | `/obx reload config` | `/obx reload config` | Reloads configuration files only. | op | `obx.admin.reload.config` |
| `/tps` | Command: /lag, /mspt, /performance | `/tps` | `/tps` | Show server TPS (1m/5m/15m), MSPT, online player count, and uptime. Console output renders in ANSI color. | op | `obx.tps` |
| `/pl` | Command: /plugins | `/pl` | `/pl` | List loaded plugins grouped by Bukkit/Paper/Purpur/Folia with status colors (enabled/disabled/broken). | op | `obx.pl` |
| `--` | `--` | `--` | `--` | **Modules** | `--` | `--` |
| `/obx joinleave` | Command: /obsidian, /obc | `/obx joinleave <on/off/status>` | `/obx joinleave off` | Toggle the join/leave broadcast module at runtime. | op | `obx.admin.modules.joinleave` |
| `/obx joinmotd` | Command: /obsidian, /obc | `/obx joinmotd <on/off/status>` | `/obx joinmotd on` | Toggle the in-game welcome MOTD module at runtime. | op | `obx.admin.modules.joinmotd` |
| `--` | `--` | `--` | `--` | **Admin menu** | `--` | `--` |
| `/obx` | Command: /obsidian, /obc | `/obx` | `/obx` | Open the Admin Menu when using /obx with no args. | op | `obx.admin.menu` |
| `--` | `--` | `--` | `--` | **Updates & Version** | `--` | `--` |
| `/obx updates` | Command: /obsidian, /obc | `/obx updates` | `/obx updates` | Checks for available updates and displays a summary. | op | `obx.updates.check` |
| `/obx updates check` | Command: /obsidian, /obc | `/obx updates check` | `/obx updates check` | Forces an update check (placeholder). | op | `obx.updates.check` |
| `/obx updates notify` | Command: /obsidian, /obc | `/obx updates notify` | `/obx updates notify` | Toggles update notifications for the executor. | op | `obx.updates.notify` |
| `/obx version` | Command: /obsidian, /obc | `/obx version` | `/obx version` | Shows current OBX version and build tag. | op | `obx.version` |

## Core

The public-facing base commands: the help/menu/info surface that every player sees.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/obx` | Command: /obsidian, /obc | `/obx` | `/obx` | Open the main menu; opens the Admin Menu if permitted. | true | `none` |
| `/help` | Command: /?, /bukkit:help, /bukkit:?, /minecraft:help, /minecraft:? | `/help [page]` | `/help 2` | Open the OBX paginated GUI help menu listing default-true commands A-Z. | true | `obx.help.gui` |
| `/list` | Command: /players, /online, /who, /playerlist | `/list` | `/list` | Show online players split into Staff and Players sections (staff in red). | true | `obx.list` |
| `/obx about` | `--` | `/obx about` | `/obx about` | Shows extended plugin information, credits, and links. | true | `obx.about` |
| `/obx commands` | `--` | `/obx commands [category]` | `/obx commands information` | Lists available commands filtered by category and permissions. | true | `obx.commands.list` |
| `/obx help` | Command: /obsidian, /obc; Entry: h | `/obx help [page/category/command]` | `/obx help reload` | Shows OBX help pages. | true | `obx.help` |
| `/obx info` | `--` | `/obx info` | `/obx info` | Displays general information about OBX. | true | `obx.info` |
| `/obx permissions` | Command: /obsidian, /obc | `/obx permissions [command/category]` | `/obx permissions reload` | Lists permission nodes for a command or category. | false | `obx.permissions.view` |

## Language

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/language` | `--` | `/language <English/EN/German/DE>` | `/language EN` | Change your preferred OBX language. | true | `obx.language` |
| `/sprache` | `--` | `/sprache <Englisch/EN/Deutsch/DE>` | `/sprache Deutsch` | German alias for the language command. | true | `obx.language` |

## Messaging

Private player-to-player messaging with a clickable reply flow and an offline inbox.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/msg` | Command: /tell, /pm, /whisper | `/msg <player> <message>` | `/msg Notch hi` | Send a private message — live if online (with a clickable Reply), else queued to their inbox. Cannot message the console. | true | `obx.message` |
| `/rply` | Command: /reply, /r | `/rply [message]` | `/rply hey` | Reply to your most recent sender. No args opens a 60s reply draft (type in chat; `cancel` or the Cancel button aborts). | true | `obx.message` |
| `/inbox` | Command: /inbound | `/inbox` | `/inbox` | Open your private-message inbox GUI (messages received while offline; click one to read). | true | `obx.message` |

## Moderation

Punishments, the moderation audit pipeline, and staff oversight tools (vanish, invsee).

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Repeated griefing` | Permanently ban a player and log the action to the moderation audit/Discord webhook. | op | `obx.moderation.ban` |
| `/banlist` | Command: /blist | `/banlist` | `/banlist` | Show all active bans, including temporary bans that have not expired yet. | op | `obx.moderation.banlist` |
| `/kick` | `--` | `/kick <player> [reason]` | `/kick HotPotato Chat spam` | Kick an online player and write the event to the moderation log pipeline. | op | `obx.moderation.kick` |
| `/mute` | `--` | `/mute <player> [reason]` | `/mute HotPotato Caps spam` | Block a player from chatting until they are unmuted. | op | `obx.moderation.mute` |
| `/status` | `--` | `/status <player>` | `/status VeryPotter` | View a player's moderation profile card, current punishments, counts, and recent action history. | op | `obx.moderation.status` |
| `/tempban` | Command: /tban | `/tempban <player> [reason]` | `/tempban VeryPotter Exploit abuse` | Temporarily ban a player using the configured default duration in `config.yml` (`moderation.defaults.tempban-duration`). | op | `obx.moderation.tempban` |
| `/unban` | `--` | `/unban <player> [reason]` | `/unban ChanceDaRepper Appeal accepted` | Remove an active ban and log the pardon event. | op | `obx.moderation.unban` |
| `/unmute` | `--` | `/unmute <player> [reason]` | `/unmute HotPotato Time served` | Remove a chat mute from a player profile. | op | `obx.moderation.unmute` |
| `/warn` | `--` | `/warn <player> [reason]` | `/warn VeryPotter Respect chat rules` | Add a stored warning entry for a player profile and log it externally. | op | `obx.moderation.warn` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Open the staff overview GUI listing all online players (alphabetical, self excluded) as skin heads with hover profile cards (first-join date, total active time, current session, country, language, and a moderation report card of warnings/mutes/kicks/tempbans/bans). Click a head to open the per-player action sub-menu. The bottom row carries a search head (chat-prompted player lookup) and a custom red-X close button. Hidden from non-permitted players. | op | `obx.staff.menu` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | View a player's inventory. Basic tier limited to non-op, non-staff targets. Console-logged with ANSI tier marker. | op | `obx.invsee.basic` (lower tier) |
| `/invsee` (full tier) | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee SeniorStaff` | Same as above but bypasses target-restriction so any player including ops/staff can be viewed. | op | `obx.invsee.full` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish Notch` | Toggle staff vanish — hidden from other players, ignored by hostile mobs, immune to passive damage triggers. Console-logged with ANSI staff line. | op | `obx.vanish` (self) / `obx.vanish.others` (others) |
| `/vanish` (admin tier) | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish` | Higher-tier vanish — invisible to every other player including other vanished staff. Holder can still see lower-tier `[L]` vanished users without revealing themselves. | op | `obx.vanish.admin` |

## Teleport

Player teleportation: requests, homes, spawn, warps, and the hub/lobby. Spawn-set, warp
management, and hub administration are op-gated and grouped at the end of each block.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Requests & navigation** | `--` | `--` |
| `/back` | `--` | `/back` | `/back` | Return to your previous location. | true | `obx.back` |
| `/tpa` | `--` | `/tpa <player>` | `/tpa Notch` | Request to teleport to another player; they get a clickable Accept/Deny prompt (60s expiry). | true | `obx.teleport.request` |
| `/tpaccept` | Command: /tpyes | `/tpaccept` | `/tpaccept` | Accept a pending teleport request (also the request's Accept button). | true | `obx.teleport.request` |
| `/tpdeny` | Command: /tpno | `/tpdeny` | `/tpdeny` | Deny a pending teleport request (also the request's Deny button). | true | `obx.teleport.request` |
| `/pos` | Command: /position | `/pos` | `/pos` | Show your coordinates: a styled chat report with click-to-copy and a live action bar that tracks you for ~5s. Console has no position. | true | `obx.position` |
| `/top` | Command: /above, /ground, /up | `/top [player]` | `/top Notch` | Teleport to the highest safe block above the target or yourself. | op | `obx.top` |
| `--` | `--` | `--` | `--` | **Homes** | `--` | `--` |
| `/delhome` | `--` | `/delhome <name>` | `/delhome base` | Delete one of your homes. | true | `obx.home.delete` |
| `/home` | `--` | `/home [name]` | `/home base` | Teleport to one of your homes. | true | `obx.home` |
| `/homes` | `--` | `/homes` | `/homes` | List all of your homes. | true | `obx.home.list` |
| `/sethome` | `--` | `/sethome [name]` | `/sethome base` | Set a named home at your current location. | true | `obx.home.set` |
| `--` | `--` | `--` | `--` | **Spawn** | `--` | `--` |
| `/spawn` | Subcommands: tp, teleport, go, goto | `/spawn` | `/spawn` | Teleport to the server spawn point. | true | `obx.spawn.tp, obx.spawn` |
| `/spawn info` | Subcommands: information, details, about | `/spawn info` | `/spawn info` | Show spawn location details. | true | `obx.spawn.info` |
| `/setspawn` | Also: /spawn set | `/setspawn` | `/setspawn` | Set the server spawn point to your current location. | op | `obx.spawn.set` |
| `/spawn set` | Command: /setspawn; Subcommands: create, new, setup | `/spawn set` | `/spawn set` | Set the server spawn point to your current location. | op | `obx.spawn.set` |
| `/spawn delete` | Subcommands: remove, del, clear, reset | `/spawn delete` | `/spawn delete` | Delete/unset the server spawn point. Confirm by clicking the **confirm** prompt or typing `confirm` in chat within 10s (no `confirm` argument). | op | `obx.spawn.delete` |
| `--` | `--` | `--` | `--` | **Warps** | `--` | `--` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <name/subcommand>` | `/warp market` | Base permission required for all warp commands. | true | `obx.warp` |
| `/warp categories` | Subcommands: cats, groups, sections | `/warp categories` | `/warp categories` | List warp categories with counts. | true | `obx.warp.list` |
| `/warp category` | Subcommands: cat, group, section | `/warp category <name>` | `/warp category shops` | List warps in a category. | true | `obx.warp.list` |
| `/warp gui` | Subcommands: menu, open, view | `/warp gui [category]` | `/warp gui shops` | Open the warp GUI (also `/warp` with no args). | true | `obx.warp.gui` |
| `/warp info` | Subcommands: information, details, about, show | `/warp info <name>` | `/warp info market` | Show warp details. | true | `obx.warp.info` |
| `/warp list` | Subcommands: ls, all, browse, page | `/warp list [page] [category]` | `/warp list 2` | List available warps, optionally filtered by category. | true | `obx.warp.list` |
| `/warp tp` | Subcommands: teleport, go, goto, warp | `/warp tp <name>` | `/warp tp market` | Teleport to a warp (also `/warp <name>`). | true | `obx.warp.tp` |
| `/warp` (category gate) | Command: /warps, /w, /goto, /go, /travel | `/warp <name>` | `/warp market` | Optional per-category permission gate for warp visibility/teleport. | n/a | `obx.warp.category.<category>` |
| `/warp delete` | Subcommands: remove, del, clear, reset, unset | `/warp delete <name> [confirm]` | `/warp delete hub confirm` | Delete a warp (confirmation required). | op | `obx.warp.delete` |
| `/warp icon` | Subcommands: item, display | `/warp icon <name> [material]` | `/warp icon hub diamond` | Set or clear a warp icon for a warp. | op | `obx.warp.icon` |
| `/warp list` (hidden) | Subcommands: ls, all, browse, page | `/warp list` | `/warp list` | View hidden warps in lists/GUI and toggle hidden view. | op | `obx.warp.hidden.view` |
| `/warp manage` | `--` | `/warp <manage action>` | `/warp set hub` | Bypass permissions for warp management actions (set/delete/rename/move/icon/public). | op | `obx.warp.manage` |
| `/warp move` | Subcommands: reloc, relocate, update, here | `/warp move <name>` | `/warp move hub` | Move a warp to your current location. | op | `obx.warp.move` |
| `/warp public` | Subcommands: publish, visible, visibility | `/warp public <name> [true/false]` | `/warp public hub false` | Toggle warp visibility (public/hidden). | op | `obx.warp.public` |
| `/warp rename` | Subcommands: ren, name, setname | `/warp rename <old> <new>` | `/warp rename old new` | Rename a warp. | op | `obx.warp.rename` |
| `/warp set` | Subcommands: create, new, setup, add, define | `/warp set <name> [confirm]` | `/warp set hub` | Create or update a warp at your location (confirm to overwrite). | op | `obx.warp.set` |
| `/warp tp` (others) | Subcommands: teleport, go, goto, warp | `/warp tp <name> <player>` | `/warp tp market Notch` | Teleport another player to a warp. | op | `obx.warp.tp.others` |
| `--` | `--` | `--` | `--` | **Hub / Lobby** | `--` | `--` |
| `/hub` | Command: /lobby | `/hub` | `/hub` | Teleport to the configured hub world. Default players only see the bare command — sub-commands, tab completion, and admin args are hidden from them. | true | `obx.hub.use` |
| `/lobby` | Alias of /hub | `/lobby` | `/lobby` | Alias of `/hub`. Same hidden-admin-surface rules apply. | true | `obx.hub.use` |
| `(hub item)` | `--` | `--` | `--` | Receive the hub server-selector hotbar item and open its GUI. | true | `obx.hub.selector` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub jump-to fishing rod (reel-in teleport). | true | `obx.hub.jumprod` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub players-vanish-all toggle item. | true | `obx.hub.vanishall` |
| `(hub item)` | `--` | `--` | `--` | Receive the launchpad and use double-jump launches. | true | `obx.hub.launchpad` |
| `/hub on` | Subcommand | `/hub on` | `/hub on` | Enable hub mode (saves to `systems/hub.yml`; applies the kit to every player in a configured hub world). Hidden from default players. | op | `obx.hub.admin` |
| `/hub off` | Subcommand | `/hub off` | `/hub off` | Disable hub mode (system goes dormant). Hidden from default players. | op | `obx.hub.admin` |
| `/hub toggle` | Subcommand | `/hub toggle` | `/hub toggle` | Flip hub mode on/off. Hidden from default players. | op | `obx.hub.admin` |
| `/hub reload` | Subcommand | `/hub reload` | `/hub reload` | Re-read `systems/hub.yml` (useful after a server-panel save). Hidden from default players. | op | `obx.hub.admin` |
| `/hub give` | Subcommand | `/hub give [player]` | `/hub give Notch` | Force-apply the hub kit to yourself or another player (bypasses world check). Hidden from default players. | op | `obx.hub.admin` |
| `/hub selector` | Subcommand | `/hub selector` | `/hub selector` | Open the live server-selector GUI. Hidden from default players. | op | `obx.hub.admin` |
| `/hub menu` | Subcommand | `/hub menu` | `/hub menu` | Open the in-game admin hub panel directly. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world add` | Subcommand | `/hub world add <world>` | `/hub world add lobby` | Add a world to the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world remove` | Subcommand | `/hub world remove <world>` | `/hub world remove lobby` | Remove a world from the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world list` | Subcommand | `/hub world list` | `/hub world list` | List configured hub worlds. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world here` | Subcommand | `/hub world here` | `/hub world here` | Add the world you're currently in to the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |

## Utility

Virtual workstations and self/other quality-of-life commands. All op-gated.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/anvil` | Command: /forge | `/anvil` | `/anvil` | Open a virtual anvil (requires Spigot 1.14+). | op | `obx.anvil` |
| `/craft` | Command: /workbench, /crafting | `/craft` | `/craft` | Open a virtual 3x3 crafting table at your location. | op | `obx.craft` |
| `/enchant` | Command: /enchanting, /enchanttable | `/enchant` | `/enchant` | Open a virtual enchanting table forced to maximum power. | op | `obx.enchant` |
| `/smith` | Command: /smithing, /smithtable | `/smith` | `/smith` | Open a virtual smithing table (requires 1.16+; full functionality on Paper 1.19+). | op | `obx.smith` |
| `/stonecut` | Command: /chop, /cut, /scut | `/stonecut` | `/stonecut` | Open a virtual stonecutter (requires Spigot 1.14+). | op | `obx.stonecut` |
| `/loom` | `--` | `/loom` | `/loom` | Open a virtual loom (requires Spigot 1.14+). | op | `obx.loom` |
| `/grindstone` | Command: /gstone, /grind, /gs | `/grindstone` | `/grindstone` | Open a virtual grindstone (requires Spigot 1.14+). | op | `obx.grindstone` |
| `/cartography` | Command: /ctable, /cartograph | `/cartography` | `/cartography` | Open a virtual cartography table (requires Spigot 1.14+). | op | `obx.cartography` |
| `/map` | `--` | `/map` | `/map` | Open a map centered on your current location (placed into your hand) at closest scale — a "you are here" view of your surroundings. | op | `obx.map` |
| `/research` | Command: /discover, /itemprofile, /iteminfo | `/research [item]` | `/research Diamond Sword` | Display detailed information about a Minecraft item. | op | `obx.research` |
| `/feed` | `--` | `/feed` | `/feed` | Restore your hunger bar (self only). | op | `obx.feed` |
| `/heal` | `--` | `/heal` | `/heal` | Restore your health to full (self only). | op | `obx.heal` |
| `/vital` | Command: /restore, /regen | `/vital [player]` | `/vital Notch` | Restore health and hunger for yourself or a target. | op | `obx.vital` |
| `/god` | Command: /gmode, /godmode, /invincible, /immortal | `/god [player]` | `/god Notch` | Toggle complete invincibility for yourself or a target. | op | `obx.god` |
| `/afk` | `--` | `/afk` | `/afk` | Toggle your own AFK status. | true | `obx.afk` |
| `/afk` (others) | `--` | `/afk <player>` | `/afk Notch` | Toggle AFK status on another player. | op | `obx.afk.others` |
| `/afk` (exempt) | `--` | `--` | `--` | Exempts the holder from auto-AFK after the idle timeout. | false | `obx.afk.exempt` |
| `/afk` (exempt-kick) | `--` | `--` | `--` | Exempts the holder from the AFK idle auto-kick. | false | `obx.afk.exempt-kick` |
| `/flyspeed` | `--` | `/flyspeed <speed>` | `/flyspeed 5` | Set your own fly speed. | op | `obx.flyspeed` |
| `/flyspeed` (others) | `--` | `/flyspeed <player> <speed>` | `/flyspeed Notch 5` | Set another player's fly speed. | op | `obx.flyspeed.others` |
| `/clearinv` | Command: /ci, /clearinventory | `/clearinv` | `/clearinv` | Clear your own inventory. | op | `obx.clearinv` |
| `/clearinv` (others) | Command: /ci, /clearinventory | `/clearinv <player> [item] [maxCount]` | `/clearinv Notch` | Clear another player's inventory. | op | `obx.clearinv.others` |
| `--` | `--` | `--` | `--` | **Gamemode** | `--` | `--` |
| `/gamemode` | Command: /gm, /gmode, /mode, /gms, /gmc, /gma, /gmsp; Modes: survival/s/surv/gms/0, creative/c/crea/gmc/1, adventure/a/adv/gma/2, spectator/sp/spec/gmsp/3 | `/gamemode <mode> [player]` | `/gamemode creative` | Change your or another player's gamemode (requires mode-specific nodes). | op | `obx.gamemode` |
| `/gamemode` | `--` | `/gamemode <mode>` | `/gamemode survival` | Base self permission used if per-mode nodes are not set. | op | `obx.gamemode.self` |
| `/gamemode` | `--` | `/gamemode <mode> <player>` | `/gamemode creative Notch` | Base others permission used if per-mode nodes are not set. | op | `obx.gamemode.others` |
| `/gamemode adventure` | `--` | `/gamemode adventure` | `/gamemode adventure` | Allow setting your own gamemode to adventure. | op | `obx.gamemode.self.adventure` |
| `/gamemode adventure` | `--` | `/gamemode adventure <player>` | `/gamemode adventure Notch` | Allow setting another player's gamemode to adventure. | op | `obx.gamemode.others.adventure` |
| `/gamemode creative` | `--` | `/gamemode creative` | `/gamemode creative` | Allow setting your own gamemode to creative. | op | `obx.gamemode.self.creative` |
| `/gamemode creative` | `--` | `/gamemode creative <player>` | `/gamemode creative Notch` | Allow setting another player's gamemode to creative. | op | `obx.gamemode.others.creative` |
| `/gamemode spectator` | `--` | `/gamemode spectator` | `/gamemode spectator` | Allow setting your own gamemode to spectator. | op | `obx.gamemode.self.spectator` |
| `/gamemode spectator` | `--` | `/gamemode spectator <player>` | `/gamemode spectator Notch` | Allow setting another player's gamemode to spectator. | op | `obx.gamemode.others.spectator` |
| `/gamemode survival` | `--` | `/gamemode survival` | `/gamemode survival` | Allow setting your own gamemode to survival. | op | `obx.gamemode.self.survival` |
| `/gamemode survival` | `--` | `/gamemode survival <player>` | `/gamemode survival Notch` | Allow setting another player's gamemode to survival. | op | `obx.gamemode.others.survival` |

## Other

The Arcanum custom-enchantment module and the permission wildcard/bundle nodes. These are
not assigned to a `/help` filter and surface under **Other** in the GUI.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — players** | `--` | `--` |
| `/enchants` | Command: /scrolls | `/enchants` | `/enchants` | Open the read-only Arcanum enchantment browser GUI. | true | `obx.enchants.browse` |
| `/enchants settings` | Command: /scrolls | `/enchants settings` | `/enchants settings` | Toggle your personal combat effects (your kill banners + combat action-bar feedback). Persists per player. | true | none (any player) |
| `/recall` | `--` | `/recall` | `/recall` | Teleport to your Beacon's Memory recall point (set by the Beacon's Memory enchantment). | true | `obx.enchants.use` |
| `/satchel` | `--` | `/satchel` | `/satchel` | Open your Satchel personal storage (granted by the Satchel enchantment). | true | `obx.enchants.use` |
| `(scroll use)` | `--` | `--` | `--` | Base permission to use Arcanum scrolls at all (anvil + drag-and-drop application); also gates `/recall` and `/satchel`. | true | `obx.enchants.use` |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — admin** | `--` | `--` |
| `/obxench` | Command: /obxenchant, /obxe | `/obxench <subcommand>` | `/obxench list combat` | Arcanum custom-enchantment admin command. The base command itself needs no permission — every action is gated by the per-subcommand nodes below. | any | `none` (per-subcommand) |
| `/obxench admin` | Command: /obxenchant, /obxe | `/obxench admin` | `/obxench admin` | Open the Arcanum admin GUI (browse, give, and apply with full access). | op | `obx.enchants.admin` |
| `/obxench apply` | Command: /obxenchant, /obxe | `/obxench apply <enchant> <level>` | `/obxench apply frostbite 3` | Apply an Arcanum enchantment to the item in your main hand. | op | `obx.enchants.apply` |
| `/obxench remove` | Command: /obxenchant, /obxe | `/obxench remove <enchant>` | `/obxench remove frostbite` | Remove an Arcanum enchantment from the item in your main hand. | op | `obx.enchants.apply` |
| `/obxench give` | Command: /obxenchant, /obxe | `/obxench give <player> <enchant> <level> [amount]` | `/obxench give Notch frostbite 3` | Give an enchantment scroll to a player. | op | `obx.enchants.give` |
| `/obxench givebook` | Command: /obxenchant, /obxe | `/obxench givebook <player> <enchant> <level> [amount]` | `/obxench givebook Notch frostbite 3` | Give the enchanted-book form to a player. | op | `obx.enchants.give` |
| `/obxench give … book` | Command: /obxenchant, /obxe | `/obxench give <player> book <category>` | `/obxench give Notch book combat` | Give a stylized interactive **Codex** guide book for a category (hover names for details, click to learn more in chat, click a level to apply to your held item — upgrades only). Executor needs `obx.enchants.give`; the **recipient** must hold `obx.enchants.book` (or be op) or the book is refused with an error + title to the executor. | op | `obx.enchants.give` (recipient: `obx.enchants.book`) |
| `/obxench bookinfo` | Command: /obxenchant, /obxe | `/obxench bookinfo <enchant>` | *(internal — Codex name click)* | Print an enchant's full details in chat. Driven by clicking an enchant name in a Codex book. | op | `obx.enchants.book` |
| `/obxench bookapply` | Command: /obxenchant, /obxe | `/obxench bookapply <enchant> <level>` | *(internal — Codex level click)* | Apply an enchant to the item in hand (main hand, else off-hand); only raises to a higher level. Driven by clicking a level in a Codex book. | op | `obx.enchants.book` |
| `/obxench protect` | Command: /obxenchant, /obxe | `/obxench protect <player> [amount]` | `/obxench protect Notch 2` | Give Protection scroll(s) to a player. | op | `obx.enchants.give` |
| `/obxench success` | Command: /obxenchant, /obxe | `/obxench success <player> [amount]` | `/obxench success Notch 2` | Give Success scroll(s) to a player. | op | `obx.enchants.give` |
| `/obxench list` | Command: /obxenchant, /obxe | `/obxench list [category]` | `/obxench list combat` | List Arcanum enchantments, optionally filtered by category. | op | `obx.enchants.list` |
| `/obxench info` | Command: /obxenchant, /obxe | `/obxench info <enchant>` | `/obxench info frostbite` | Show full details for a specific enchantment. | op | `obx.enchants.list` |
| `/obxench reload` | Command: /obxenchant, /obxe | `/obxench reload` | `/obxench reload` | Reload the Arcanum config and category/roster files. | op | `obx.enchants.reload` |
| `/obxench loot` | Command: /obxenchant, /obxe | `/obxench loot <reload \| toggle <chest>>` | `/obxench loot reload` | Reload world-loot config, or toggle scroll generation for a chest type. | op | `obx.enchants.loot` |
| `/obxench debug` | Command: /obxenchant, /obxe | `/obxench debug [on/off]` | `/obxench debug on` | Toggle per-admin Arcanum proc-debug logging for yourself. | op | `obx.enchants.debug` |
| `--` | `--` | `--` | `--` | **Wildcards & Bundles** | `--` | `--` |
| `/obx` | Command: /obsidian, /obc | `/obx <admin subcommand>` | `/obx reload` | Bundle of admin-level OBX actions (reload/diagnostics/menu). | op | `obx.admin.*` |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Testing` | Wildcard bundle for all moderation actions, including banlist access. | op | `obx.moderation.*` |
| `/obx` | Command: /obsidian, /obc | `/obx <config/debug>` | `/obx debug` | Bundle for config and debug permissions. | op | `obx.debug.*` |
| `/obx updates` | Command: /obsidian, /obc | `/obx updates` | `/obx updates` | Bundle for update and version permissions. | op | `obx.updates.*` |
| `/spawn` | `--` | `/spawn <tp/set/delete/info>` | `/spawn set` | Wildcard for all spawn permissions. | op | `obx.spawn.*` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <subcommand>` | `/warp set hub` | Wildcard for all warp permissions. | op | `obx.warp.*` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish` | Wildcard for all vanish tiers (self + others + admin). | op | `obx.vanish.*` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | Wildcard for both inventory-see tiers (basic + full). | op | `obx.invsee.*` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Wildcard for the staff overview menu and its actions. | op | `obx.staff.*` |
| `/hub` | Command: /lobby | `/hub <subcommand>` | `/hub on` | Wildcard for every hub / lobby permission (use + all hotbar items + admin). | op | `obx.hub.*` |
| `/obxench` | Command: /obxenchant, /obxe | `/obxench <subcommand>` | `/obxench admin` | Wildcard for the full Arcanum module (admin/give/apply/list/reload/loot/debug/use/browse/book). | op | `obx.enchants.*` |
| `/holo` | Command: /hg, /hologram | `/holo <subcommand>` | `/holo create welcome` | Wildcard for the full hologram module (create/edit/delete/admin). | op | `obx.holo.*` |
| `N/A` | `--` | `--` | `--` | Wildcard for all OBX permissions. | op | `obx.*` |
| `--` | `--` | `--` | `--` | **Holograms — admin** | `--` | `--` |
| `/holo` | Command: /hg, /hologram | `/holo <subcommand>` | `/holo list` | Root command for the OBX holograms module. The base command itself only requires `obx.holo.use`; each subcommand is gated by its own node below. | true | `obx.holo.use` |
| `/holo create` | Command: /hg, /hologram | `/holo create <id> [text]` | `/holo create welcome &6Welcome` | Create a new hologram at the caller's location with an optional first line. Persists to `holograms.yml`. | op | `obx.holo.create` |
| `/holo delete` | Command: /hg, /hologram | `/holo delete <id>` | `/holo delete welcome` | Despawn and remove a hologram from disk. | op | `obx.holo.delete` |
| `/holo list` | Command: /hg, /hologram | `/holo list` | `/holo list` | List every registered hologram with its world / coordinates / line count. | op | `obx.holo.list` |
| `/holo info` | Command: /hg, /hologram | `/holo info [id]` | `/holo info welcome` | Show module info (no id) or full settings + lines for a single hologram. | op | `obx.holo.info` |
| `/holo tp` | Command: /hg, /hologram | `/holo tp <id>` | `/holo tp welcome` | Teleport to a hologram's stored location. | op | `obx.holo.tp` |
| `/holo move` | Command: /hg, /hologram | `/holo move <id> <x> <y> <z> [yaw] [world]` | `/holo move welcome 0 65 0 0 world` | Reposition a hologram to explicit coordinates. | op | `obx.holo.edit` |
| `/holo movehere` | Command: /hg, /hologram | `/holo movehere <id>` | `/holo movehere welcome` | Move a hologram to the caller's current location (+2y). | op | `obx.holo.edit` |
| `/holo copy` | Command: /hg, /hologram | `/holo copy <src> <dest>` | `/holo copy welcome welcome2` | Duplicate a hologram with all lines and settings, placed at the caller. | op | `obx.holo.create` |
| `/holo line add` | Command: /hg, /hologram | `/holo line add <id> <text…\|icon <mat>\|block <mat>>` | `/holo line add welcome &eHello` | Append a line. Use `icon <material>` or `block <material>` for non-text lines. | op | `obx.holo.edit` |
| `/holo line set` | Command: /hg, /hologram | `/holo line set <id> <index> <value…>` | `/holo line set welcome 1 &fNew` | Replace the line at 1-based index. | op | `obx.holo.edit` |
| `/holo line remove` | Command: /hg, /hologram | `/holo line remove <id> <index>` | `/holo line remove welcome 2` | Remove the line at 1-based index. Alias: `delete`. | op | `obx.holo.edit` |
| `/holo line insertbefore` | Command: /hg, /hologram | `/holo line insertbefore <id> <index> <value…>` | `/holo line insertbefore welcome 2 &7Inserted` | Insert before the given line. Alias: `before`. | op | `obx.holo.edit` |
| `/holo line insertafter` | Command: /hg, /hologram | `/holo line insertafter <id> <index> <value…>` | `/holo line insertafter welcome 1 &7Inserted` | Insert after the given line. Alias: `after`. | op | `obx.holo.edit` |
| `/holo line swap` | Command: /hg, /hologram | `/holo line swap <id> <a> <b>` | `/holo line swap welcome 1 3` | Swap two lines. | op | `obx.holo.edit` |
| `/holo scale` | Command: /hg, /hologram | `/holo scale <id> <factor>` | `/holo scale welcome 1.5` | Set the hologram scale factor (0.05–20.0). | op | `obx.holo.edit` |
| `/holo billboard` | Command: /hg, /hologram | `/holo billboard <id> <FIXED\|VERTICAL\|HORIZONTAL\|CENTER>` | `/holo billboard welcome CENTER` | Set the billboard mode. | op | `obx.holo.edit` |
| `/holo background` | Command: /hg, /hologram | `/holo background <id> <ARGB\|transparent\|none>` | `/holo background welcome 80FF0000` | Set the text background colour. | op | `obx.holo.edit` |
| `/holo textalpha` | Command: /hg, /hologram | `/holo textalpha <id> <0-255>` | `/holo textalpha welcome 200` | Set text opacity. | op | `obx.holo.edit` |
| `/holo shadow` | Command: /hg, /hologram | `/holo shadow <id> <true\|false>` | `/holo shadow welcome true` | Toggle drop shadow on text. | op | `obx.holo.edit` |
| `/holo alignment` | Command: /hg, /hologram | `/holo alignment <id> <CENTER\|LEFT\|RIGHT>` | `/holo alignment welcome LEFT` | Set text alignment within the line. | op | `obx.holo.edit` |
| `/holo seethrough` | Command: /hg, /hologram | `/holo seethrough <id> <true\|false>` | `/holo seethrough welcome true` | Toggle see-through-walls rendering. | op | `obx.holo.edit` |
| `/holo doublesided` | Command: /hg, /hologram | `/holo doublesided <id> <true\|false>` | `/holo doublesided welcome false` | Toggle front/back culling (false → only the front is visible). | op | `obx.holo.edit` |
| `/holo showrange` | Command: /hg, /hologram | `/holo showrange <id> <blocks>` | `/holo showrange welcome 32` | Visibility distance in blocks. | op | `obx.holo.edit` |
| `/holo updaterange` | Command: /hg, /hologram | `/holo updaterange <id> <blocks>` | `/holo updaterange welcome 48` | Per-viewer text refresh distance (≥ showrange). | op | `obx.holo.edit` |
| `/holo board` | Command: /hg, /hologram | `/holo board <id> <…>` | `/holo board welcome enable true` | Configure the block-display slab backing (Phase 5). | op | `obx.holo.edit` |
| `/holo aim` | Command: /hg, /hologram | `/holo aim` | `/holo aim` | Resolve the hologram closest to the caller's look direction (within 12 blocks). | op | `obx.holo.edit` |
| `/holo enable` | Command: /hg, /hologram | `/holo enable` | `/holo enable` | Flip the master flag in `systems/holograms.yml → enabled: true` and reload. | op | `obx.holo.admin` |
| `/holo disable` | Command: /hg, /hologram | `/holo disable` | `/holo disable` | Flip the master flag to `false` and tear down live entities. | op | `obx.holo.admin` |
| `/holo reload` | Command: /hg, /hologram | `/holo reload` | `/holo reload` | Reload `systems/holograms.yml` and re-spawn every hologram. | op | `obx.holo.admin` |
| `/holo debug` | Command: /hg, /hologram | `/holo debug [id]` | `/holo debug` | Spawn / toggle a built-in test hologram showing backend identity. | op | `obx.holo.admin` |
