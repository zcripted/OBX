# Commands and Permissions

Every SF-Core command is grouped into the same filterable categories used by the in-game
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

Operational, diagnostic, and admin-teleport commands. Most live under `/sf` and require op.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Admin teleport** | `--` | `--` |
| `/tp` | Command: /teleport | `/tp <player> [target]` | `/tp Notch` | Teleport to a player, or move one player to another. The console can move players but cannot teleport itself, and there is no console to teleport to. | op | `sfcore.teleport.admin` |
| `/tphere` | `--` | `/tphere <player>` | `/tphere Notch` | Bring a player to your location. | op | `sfcore.teleport.admin` |
| `/kill` | Command: /crosskill, /aimkill, /targetkill | `/kill` | `/kill` | Toggle kill mode; left-click to kill the entity in your crosshairs. | op | `sfcore.kill` |
| `--` | `--` | `--` | `--` | **Config & Debug** | `--` | `--` |
| `/sf config` | Command: /sfcore, /sfc | `/sf config` | `/sf config` | Displays loaded config files and their status. | op | `sfcore.debug.config` |
| `/sf config validate` | Command: /sfcore, /sfc | `/sf config validate` | `/sf config validate` | Validates config files and reports errors or deprecated keys. | op | `sfcore.debug.config.validate` |
| `/sf debug` | Command: /sfcore, /sfc | `/sf debug` | `/sf debug` | Shows debug status and active debug flags. | op | `sfcore.debug` |
| `/sf debug disable` | Command: /sfcore, /sfc | `/sf debug disable` | `/sf debug disable` | Disables debug logging. | op | `sfcore.debug.toggle` |
| `/sf debug dump` | Command: /sfcore, /sfc | `/sf debug dump` | `/sf debug dump` | Dumps internal state to a log file for troubleshooting. | op | `sfcore.debug.dump` |
| `/sf debug enable` | Command: /sfcore, /sfc | `/sf debug enable` | `/sf debug enable` | Enables debug logging temporarily. | op | `sfcore.debug.toggle` |
| `--` | `--` | `--` | `--` | **Reload & Diagnostics** | `--` | `--` |
| `/sf diagnostics` | Command: /sfcore, /sfc | `/sf diagnostics` | `/sf diagnostics` | Runs a quick health check (config status, loaded modules, platform info). | op | `sfcore.admin.diagnostics` |
| `/sf diagnostics full` | Command: /sfcore, /sfc | `/sf diagnostics full` | `/sf diagnostics full` | Outputs extended diagnostics including services, hooks, and errors. | op | `sfcore.admin.diagnostics.full` |
| `/sf reload` | Command: /sfcore, /sfc | `/sf reload` | `/sf reload` | Reloads SF-Core configs and reinitializes all feature modules safely. | op | `sfcore.admin.reload` |
| `/sf reload <file>` | Command: /sfcore, /sfc | `/sf reload <file>` | `/sf reload config.yml` | Reloads a specific file from the plugin data folder. | op | `sfcore.admin.reload.features` |
| `/sf reload config` | Command: /sfcore, /sfc | `/sf reload config` | `/sf reload config` | Reloads configuration files only. | op | `sfcore.admin.reload.config` |
| `/tps` | Command: /lag, /mspt, /performance | `/tps` | `/tps` | Show server TPS (1m/5m/15m), MSPT, online player count, and uptime. Console output renders in ANSI color. | op | `sfcore.tps` |
| `/pl` | Command: /plugins | `/pl` | `/pl` | List loaded plugins grouped by Bukkit/Paper/Purpur/Folia with status colors (enabled/disabled/broken). | op | `sfcore.pl` |
| `--` | `--` | `--` | `--` | **Modules** | `--` | `--` |
| `/sf joinleave` | Command: /sfcore, /sfc | `/sf joinleave <on/off/status>` | `/sf joinleave off` | Toggle the join/leave broadcast module at runtime. | op | `sfcore.admin.modules.joinleave` |
| `/sf joinmotd` | Command: /sfcore, /sfc | `/sf joinmotd <on/off/status>` | `/sf joinmotd on` | Toggle the in-game welcome MOTD module at runtime. | op | `sfcore.admin.modules.joinmotd` |
| `--` | `--` | `--` | `--` | **Admin menu** | `--` | `--` |
| `/sf` | Command: /sfcore, /sfc | `/sf` | `/sf` | Open the Admin Menu when using /sf with no args. | op | `sfcore.admin.menu` |
| `--` | `--` | `--` | `--` | **Updates & Version** | `--` | `--` |
| `/sf updates` | Command: /sfcore, /sfc | `/sf updates` | `/sf updates` | Checks for available updates and displays a summary. | op | `sfcore.updates.check` |
| `/sf updates check` | Command: /sfcore, /sfc | `/sf updates check` | `/sf updates check` | Forces an update check (placeholder). | op | `sfcore.updates.check` |
| `/sf updates notify` | Command: /sfcore, /sfc | `/sf updates notify` | `/sf updates notify` | Toggles update notifications for the executor. | op | `sfcore.updates.notify` |
| `/sf version` | Command: /sfcore, /sfc | `/sf version` | `/sf version` | Shows current SF-Core version and build tag. | op | `sfcore.version` |

## Core

The public-facing base commands: the help/menu/info surface that every player sees.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/sf` | Command: /sfcore, /sfc | `/sf` | `/sf` | Open the main menu; opens the Admin Menu if permitted. | true | `none` |
| `/help` | Command: /?, /bukkit:help, /bukkit:?, /minecraft:help, /minecraft:? | `/help [page]` | `/help 2` | Open the SF-Core paginated GUI help menu listing default-true commands A-Z. | true | `sfcore.help.gui` |
| `/list` | Command: /players, /online, /who, /playerlist | `/list` | `/list` | Show online players split into Staff and Players sections (staff in red). | true | `sfcore.list` |
| `/sf about` | `--` | `/sf about` | `/sf about` | Shows extended plugin information, credits, and links. | true | `sfcore.about` |
| `/sf commands` | `--` | `/sf commands [category]` | `/sf commands information` | Lists available commands filtered by category and permissions. | true | `sfcore.commands.list` |
| `/sf help` | Command: /sfcore, /sfc; Entry: h | `/sf help [page/category/command]` | `/sf help reload` | Shows SF-Core help pages. | true | `sfcore.help` |
| `/sf info` | `--` | `/sf info` | `/sf info` | Displays general information about SF-Core. | true | `sfcore.info` |
| `/sf permissions` | Command: /sfcore, /sfc | `/sf permissions [command/category]` | `/sf permissions reload` | Lists permission nodes for a command or category. | false | `sfcore.permissions.view` |

## Language

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/language` | `--` | `/language <English/EN/German/DE>` | `/language EN` | Change your preferred SF-Core language. | true | `sfcore.language` |
| `/sprache` | `--` | `/sprache <Englisch/EN/Deutsch/DE>` | `/sprache Deutsch` | German alias for the language command. | true | `sfcore.language` |

## Messaging

Private player-to-player messaging with a clickable reply flow and an offline inbox.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/msg` | Command: /tell, /pm, /whisper | `/msg <player> <message>` | `/msg Notch hi` | Send a private message — live if online (with a clickable Reply), else queued to their inbox. Cannot message the console. | true | `sfcore.message` |
| `/rply` | Command: /reply, /r | `/rply [message]` | `/rply hey` | Reply to your most recent sender. No args opens a 60s reply draft (type in chat; `cancel` or the Cancel button aborts). | true | `sfcore.message` |
| `/inbox` | Command: /inbound | `/inbox` | `/inbox` | Open your private-message inbox GUI (messages received while offline; click one to read). | true | `sfcore.message` |

## Moderation

Punishments, the moderation audit pipeline, and staff oversight tools (vanish, invsee).

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Repeated griefing` | Permanently ban a player and log the action to the moderation audit/Discord webhook. | op | `sfcore.moderation.ban` |
| `/banlist` | Command: /blist | `/banlist` | `/banlist` | Show all active bans, including temporary bans that have not expired yet. | op | `sfcore.moderation.banlist` |
| `/kick` | `--` | `/kick <player> [reason]` | `/kick HotPotato Chat spam` | Kick an online player and write the event to the moderation log pipeline. | op | `sfcore.moderation.kick` |
| `/mute` | `--` | `/mute <player> [reason]` | `/mute HotPotato Caps spam` | Block a player from chatting until they are unmuted. | op | `sfcore.moderation.mute` |
| `/status` | `--` | `/status <player>` | `/status VeryPotter` | View a player's moderation profile card, current punishments, counts, and recent action history. | op | `sfcore.moderation.status` |
| `/tempban` | Command: /tban | `/tempban <player> [reason]` | `/tempban VeryPotter Exploit abuse` | Temporarily ban a player using the configured default duration in `config.yml` (`moderation.defaults.tempban-duration`). | op | `sfcore.moderation.tempban` |
| `/unban` | `--` | `/unban <player> [reason]` | `/unban ChanceDaRepper Appeal accepted` | Remove an active ban and log the pardon event. | op | `sfcore.moderation.unban` |
| `/unmute` | `--` | `/unmute <player> [reason]` | `/unmute HotPotato Time served` | Remove a chat mute from a player profile. | op | `sfcore.moderation.unmute` |
| `/warn` | `--` | `/warn <player> [reason]` | `/warn VeryPotter Respect chat rules` | Add a stored warning entry for a player profile and log it externally. | op | `sfcore.moderation.warn` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Open the staff overview GUI listing all online players (alphabetical, self excluded) as skin heads with hover profile cards (first-join date, total active time, current session, country, language, and a moderation report card of warnings/mutes/kicks/tempbans/bans). Click a head to open the per-player action sub-menu. The bottom row carries a search head (chat-prompted player lookup) and a custom red-X close button. Hidden from non-permitted players. | op | `sfcore.staff.menu` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | View a player's inventory. Basic tier limited to non-op, non-staff targets. Console-logged with ANSI tier marker. | op | `sfcore.invsee.basic` (lower tier) |
| `/invsee` (full tier) | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee SeniorStaff` | Same as above but bypasses target-restriction so any player including ops/staff can be viewed. | op | `sfcore.invsee.full` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish Notch` | Toggle staff vanish — hidden from other players, ignored by hostile mobs, immune to passive damage triggers. Console-logged with ANSI staff line. | op | `sfcore.vanish` (self) / `sfcore.vanish.others` (others) |
| `/vanish` (admin tier) | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish` | Higher-tier vanish — invisible to every other player including other vanished staff. Holder can still see lower-tier `[L]` vanished users without revealing themselves. | op | `sfcore.vanish.admin` |

## Teleport

Player teleportation: requests, homes, spawn, warps, and the hub/lobby. Spawn-set, warp
management, and hub administration are op-gated and grouped at the end of each block.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Requests & navigation** | `--` | `--` |
| `/back` | `--` | `/back` | `/back` | Return to your previous location. | true | `sfcore.back` |
| `/tpa` | `--` | `/tpa <player>` | `/tpa Notch` | Request to teleport to another player; they get a clickable Accept/Deny prompt (60s expiry). | true | `sfcore.teleport.request` |
| `/tpaccept` | Command: /tpyes | `/tpaccept` | `/tpaccept` | Accept a pending teleport request (also the request's Accept button). | true | `sfcore.teleport.request` |
| `/tpdeny` | Command: /tpno | `/tpdeny` | `/tpdeny` | Deny a pending teleport request (also the request's Deny button). | true | `sfcore.teleport.request` |
| `/pos` | Command: /position | `/pos` | `/pos` | Show your coordinates: a styled chat report with click-to-copy and a live action bar that tracks you for ~5s. Console has no position. | true | `sfcore.position` |
| `/top` | Command: /above, /ground, /up | `/top [player]` | `/top Notch` | Teleport to the highest safe block above the target or yourself. | op | `sfcore.top` |
| `--` | `--` | `--` | `--` | **Homes** | `--` | `--` |
| `/delhome` | `--` | `/delhome <name>` | `/delhome base` | Delete one of your homes. | true | `sfcore.home.delete` |
| `/home` | `--` | `/home [name]` | `/home base` | Teleport to one of your homes. | true | `sfcore.home` |
| `/homes` | `--` | `/homes` | `/homes` | List all of your homes. | true | `sfcore.home.list` |
| `/sethome` | `--` | `/sethome [name]` | `/sethome base` | Set a named home at your current location. | true | `sfcore.home.set` |
| `--` | `--` | `--` | `--` | **Spawn** | `--` | `--` |
| `/spawn` | Subcommands: tp, teleport, go, goto | `/spawn` | `/spawn` | Teleport to the server spawn point. | true | `sfcore.spawn.tp, sfcore.spawn` |
| `/spawn info` | Subcommands: information, details, about | `/spawn info` | `/spawn info` | Show spawn location details. | true | `sfcore.spawn.info` |
| `/setspawn` | Also: /spawn set | `/setspawn` | `/setspawn` | Set the server spawn point to your current location. | op | `sfcore.spawn.set` |
| `/spawn set` | Command: /setspawn; Subcommands: create, new, setup | `/spawn set` | `/spawn set` | Set the server spawn point to your current location. | op | `sfcore.spawn.set` |
| `/spawn delete` | Subcommands: remove, del, clear, reset | `/spawn delete` | `/spawn delete` | Delete/unset the server spawn point. Confirm by clicking the **confirm** prompt or typing `confirm` in chat within 10s (no `confirm` argument). | op | `sfcore.spawn.delete` |
| `--` | `--` | `--` | `--` | **Warps** | `--` | `--` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <name/subcommand>` | `/warp market` | Base permission required for all warp commands. | true | `sfcore.warp` |
| `/warp categories` | Subcommands: cats, groups, sections | `/warp categories` | `/warp categories` | List warp categories with counts. | true | `sfcore.warp.list` |
| `/warp category` | Subcommands: cat, group, section | `/warp category <name>` | `/warp category shops` | List warps in a category. | true | `sfcore.warp.list` |
| `/warp gui` | Subcommands: menu, open, view | `/warp gui [category]` | `/warp gui shops` | Open the warp GUI (also `/warp` with no args). | true | `sfcore.warp.gui` |
| `/warp info` | Subcommands: information, details, about, show | `/warp info <name>` | `/warp info market` | Show warp details. | true | `sfcore.warp.info` |
| `/warp list` | Subcommands: ls, all, browse, page | `/warp list [page] [category]` | `/warp list 2` | List available warps, optionally filtered by category. | true | `sfcore.warp.list` |
| `/warp tp` | Subcommands: teleport, go, goto, warp | `/warp tp <name>` | `/warp tp market` | Teleport to a warp (also `/warp <name>`). | true | `sfcore.warp.tp` |
| `/warp` (category gate) | Command: /warps, /w, /goto, /go, /travel | `/warp <name>` | `/warp market` | Optional per-category permission gate for warp visibility/teleport. | n/a | `sfcore.warp.category.<category>` |
| `/warp delete` | Subcommands: remove, del, clear, reset, unset | `/warp delete <name> [confirm]` | `/warp delete hub confirm` | Delete a warp (confirmation required). | op | `sfcore.warp.delete` |
| `/warp icon` | Subcommands: item, display | `/warp icon <name> [material]` | `/warp icon hub diamond` | Set or clear a warp icon for a warp. | op | `sfcore.warp.icon` |
| `/warp list` (hidden) | Subcommands: ls, all, browse, page | `/warp list` | `/warp list` | View hidden warps in lists/GUI and toggle hidden view. | op | `sfcore.warp.hidden.view` |
| `/warp manage` | `--` | `/warp <manage action>` | `/warp set hub` | Bypass permissions for warp management actions (set/delete/rename/move/icon/public). | op | `sfcore.warp.manage` |
| `/warp move` | Subcommands: reloc, relocate, update, here | `/warp move <name>` | `/warp move hub` | Move a warp to your current location. | op | `sfcore.warp.move` |
| `/warp public` | Subcommands: publish, visible, visibility | `/warp public <name> [true/false]` | `/warp public hub false` | Toggle warp visibility (public/hidden). | op | `sfcore.warp.public` |
| `/warp rename` | Subcommands: ren, name, setname | `/warp rename <old> <new>` | `/warp rename old new` | Rename a warp. | op | `sfcore.warp.rename` |
| `/warp set` | Subcommands: create, new, setup, add, define | `/warp set <name> [confirm]` | `/warp set hub` | Create or update a warp at your location (confirm to overwrite). | op | `sfcore.warp.set` |
| `/warp tp` (others) | Subcommands: teleport, go, goto, warp | `/warp tp <name> <player>` | `/warp tp market Notch` | Teleport another player to a warp. | op | `sfcore.warp.tp.others` |
| `--` | `--` | `--` | `--` | **Hub / Lobby** | `--` | `--` |
| `/hub` | Command: /lobby | `/hub` | `/hub` | Teleport to the configured hub world. Default players only see the bare command — sub-commands, tab completion, and admin args are hidden from them. | true | `sfcore.hub.use` |
| `/lobby` | Alias of /hub | `/lobby` | `/lobby` | Alias of `/hub`. Same hidden-admin-surface rules apply. | true | `sfcore.hub.use` |
| `(hub item)` | `--` | `--` | `--` | Receive the hub server-selector hotbar item and open its GUI. | true | `sfcore.hub.selector` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub jump-to fishing rod (reel-in teleport). | true | `sfcore.hub.jumprod` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub players-vanish-all toggle item. | true | `sfcore.hub.vanishall` |
| `(hub item)` | `--` | `--` | `--` | Receive the launchpad and use double-jump launches. | true | `sfcore.hub.launchpad` |
| `/hub on` | Subcommand | `/hub on` | `/hub on` | Enable hub mode (saves to `systems/hub.yml`; applies the kit to every player in a configured hub world). Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub off` | Subcommand | `/hub off` | `/hub off` | Disable hub mode (system goes dormant). Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub toggle` | Subcommand | `/hub toggle` | `/hub toggle` | Flip hub mode on/off. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub reload` | Subcommand | `/hub reload` | `/hub reload` | Re-read `systems/hub.yml` (useful after a server-panel save). Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub give` | Subcommand | `/hub give [player]` | `/hub give Notch` | Force-apply the hub kit to yourself or another player (bypasses world check). Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub selector` | Subcommand | `/hub selector` | `/hub selector` | Open the live server-selector GUI. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub menu` | Subcommand | `/hub menu` | `/hub menu` | Open the in-game admin hub panel directly. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub world add` | Subcommand | `/hub world add <world>` | `/hub world add lobby` | Add a world to the hub-mode whitelist. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub world remove` | Subcommand | `/hub world remove <world>` | `/hub world remove lobby` | Remove a world from the hub-mode whitelist. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub world list` | Subcommand | `/hub world list` | `/hub world list` | List configured hub worlds. Hidden from default players. | op | `sfcore.hub.admin` |
| `/hub world here` | Subcommand | `/hub world here` | `/hub world here` | Add the world you're currently in to the hub-mode whitelist. Hidden from default players. | op | `sfcore.hub.admin` |

## Utility

Virtual workstations and self/other quality-of-life commands. All op-gated.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/anvil` | Command: /forge | `/anvil` | `/anvil` | Open a virtual anvil (requires Spigot 1.14+). | op | `sfcore.anvil` |
| `/craft` | Command: /workbench, /crafting | `/craft` | `/craft` | Open a virtual 3x3 crafting table at your location. | op | `sfcore.craft` |
| `/enchant` | Command: /enchanting, /enchanttable | `/enchant` | `/enchant` | Open a virtual enchanting table forced to maximum power. | op | `sfcore.enchant` |
| `/smith` | Command: /smithing, /smithtable | `/smith` | `/smith` | Open a virtual smithing table (requires 1.16+; full functionality on Paper 1.19+). | op | `sfcore.smith` |
| `/stonecut` | Command: /chop, /cut, /scut | `/stonecut` | `/stonecut` | Open a virtual stonecutter (requires Spigot 1.14+). | op | `sfcore.stonecut` |
| `/loom` | `--` | `/loom` | `/loom` | Open a virtual loom (requires Spigot 1.14+). | op | `sfcore.loom` |
| `/grindstone` | Command: /gstone, /grind, /gs | `/grindstone` | `/grindstone` | Open a virtual grindstone (requires Spigot 1.14+). | op | `sfcore.grindstone` |
| `/cartography` | Command: /ctable, /cartograph | `/cartography` | `/cartography` | Open a virtual cartography table (requires Spigot 1.14+). | op | `sfcore.cartography` |
| `/map` | `--` | `/map` | `/map` | Open a map centered on your current location (placed into your hand) at closest scale — a "you are here" view of your surroundings. | op | `sfcore.map` |
| `/research` | Command: /discover, /itemprofile, /iteminfo | `/research [item]` | `/research Diamond Sword` | Display detailed information about a Minecraft item. | op | `sfcore.research` |
| `/feed` | `--` | `/feed` | `/feed` | Restore your hunger bar (self only). | op | `sfcore.feed` |
| `/heal` | `--` | `/heal` | `/heal` | Restore your health to full (self only). | op | `sfcore.heal` |
| `/vital` | Command: /restore, /regen | `/vital [player]` | `/vital Notch` | Restore health and hunger for yourself or a target. | op | `sfcore.vital` |
| `/god` | Command: /gmode, /godmode, /invincible, /immortal | `/god [player]` | `/god Notch` | Toggle complete invincibility for yourself or a target. | op | `sfcore.god` |
| `--` | `--` | `--` | `--` | **Gamemode** | `--` | `--` |
| `/gamemode` | Command: /gm, /gmode, /mode, /gms, /gmc, /gma, /gmsp; Modes: survival/s/surv/gms/0, creative/c/crea/gmc/1, adventure/a/adv/gma/2, spectator/sp/spec/gmsp/3 | `/gamemode <mode> [player]` | `/gamemode creative` | Change your or another player's gamemode (requires mode-specific nodes). | op | `sfcore.gamemode` |
| `/gamemode` | `--` | `/gamemode <mode>` | `/gamemode survival` | Base self permission used if per-mode nodes are not set. | op | `sfcore.gamemode.self` |
| `/gamemode` | `--` | `/gamemode <mode> <player>` | `/gamemode creative Notch` | Base others permission used if per-mode nodes are not set. | op | `sfcore.gamemode.others` |
| `/gamemode adventure` | `--` | `/gamemode adventure` | `/gamemode adventure` | Allow setting your own gamemode to adventure. | op | `sfcore.gamemode.self.adventure` |
| `/gamemode adventure` | `--` | `/gamemode adventure <player>` | `/gamemode adventure Notch` | Allow setting another player's gamemode to adventure. | op | `sfcore.gamemode.others.adventure` |
| `/gamemode creative` | `--` | `/gamemode creative` | `/gamemode creative` | Allow setting your own gamemode to creative. | op | `sfcore.gamemode.self.creative` |
| `/gamemode creative` | `--` | `/gamemode creative <player>` | `/gamemode creative Notch` | Allow setting another player's gamemode to creative. | op | `sfcore.gamemode.others.creative` |
| `/gamemode spectator` | `--` | `/gamemode spectator` | `/gamemode spectator` | Allow setting your own gamemode to spectator. | op | `sfcore.gamemode.self.spectator` |
| `/gamemode spectator` | `--` | `/gamemode spectator <player>` | `/gamemode spectator Notch` | Allow setting another player's gamemode to spectator. | op | `sfcore.gamemode.others.spectator` |
| `/gamemode survival` | `--` | `/gamemode survival` | `/gamemode survival` | Allow setting your own gamemode to survival. | op | `sfcore.gamemode.self.survival` |
| `/gamemode survival` | `--` | `/gamemode survival <player>` | `/gamemode survival Notch` | Allow setting another player's gamemode to survival. | op | `sfcore.gamemode.others.survival` |

## Other

The Arcanum custom-enchantment module and the permission wildcard/bundle nodes. These are
not assigned to a `/help` filter and surface under **Other** in the GUI.

| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — players** | `--` | `--` |
| `/enchants` | Command: /scrolls | `/enchants` | `/enchants` | Open the read-only Arcanum enchantment browser GUI. | true | `sfcore.enchants.browse` |
| `/enchants settings` | Command: /scrolls | `/enchants settings` | `/enchants settings` | Toggle your personal combat effects (your kill banners + combat action-bar feedback). Persists per player. | true | none (any player) |
| `/recall` | `--` | `/recall` | `/recall` | Teleport to your Beacon's Memory recall point (set by the Beacon's Memory enchantment). | true | `sfcore.enchants.use` |
| `/satchel` | `--` | `/satchel` | `/satchel` | Open your Satchel personal storage (granted by the Satchel enchantment). | true | `sfcore.enchants.use` |
| `(scroll use)` | `--` | `--` | `--` | Base permission to use Arcanum scrolls at all (anvil + drag-and-drop application); also gates `/recall` and `/satchel`. | true | `sfcore.enchants.use` |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — admin** | `--` | `--` |
| `/sfench` | Command: /sfenchant, /sfe | `/sfench <subcommand>` | `/sfench list combat` | Arcanum custom-enchantment admin command. The base command itself needs no permission — every action is gated by the per-subcommand nodes below. | any | `none` (per-subcommand) |
| `/sfench admin` | Command: /sfenchant, /sfe | `/sfench admin` | `/sfench admin` | Open the Arcanum admin GUI (browse, give, and apply with full access). | op | `sfcore.enchants.admin` |
| `/sfench apply` | Command: /sfenchant, /sfe | `/sfench apply <enchant> <level>` | `/sfench apply frostbite 3` | Apply an Arcanum enchantment to the item in your main hand. | op | `sfcore.enchants.apply` |
| `/sfench remove` | Command: /sfenchant, /sfe | `/sfench remove <enchant>` | `/sfench remove frostbite` | Remove an Arcanum enchantment from the item in your main hand. | op | `sfcore.enchants.apply` |
| `/sfench give` | Command: /sfenchant, /sfe | `/sfench give <player> <enchant> <level> [amount]` | `/sfench give Notch frostbite 3` | Give an enchantment scroll to a player. | op | `sfcore.enchants.give` |
| `/sfench givebook` | Command: /sfenchant, /sfe | `/sfench givebook <player> <enchant> <level> [amount]` | `/sfench givebook Notch frostbite 3` | Give the enchanted-book form to a player. | op | `sfcore.enchants.give` |
| `/sfench give … book` | Command: /sfenchant, /sfe | `/sfench give <player> book <category>` | `/sfench give Notch book combat` | Give a stylized interactive **Codex** guide book for a category (hover names for details, click to learn more in chat, click a level to apply to your held item — upgrades only). Executor needs `sfcore.enchants.give`; the **recipient** must hold `sfcore.enchants.book` (or be op) or the book is refused with an error + title to the executor. | op | `sfcore.enchants.give` (recipient: `sfcore.enchants.book`) |
| `/sfench bookinfo` | Command: /sfenchant, /sfe | `/sfench bookinfo <enchant>` | *(internal — Codex name click)* | Print an enchant's full details in chat. Driven by clicking an enchant name in a Codex book. | op | `sfcore.enchants.book` |
| `/sfench bookapply` | Command: /sfenchant, /sfe | `/sfench bookapply <enchant> <level>` | *(internal — Codex level click)* | Apply an enchant to the item in hand (main hand, else off-hand); only raises to a higher level. Driven by clicking a level in a Codex book. | op | `sfcore.enchants.book` |
| `/sfench protect` | Command: /sfenchant, /sfe | `/sfench protect <player> [amount]` | `/sfench protect Notch 2` | Give Protection scroll(s) to a player. | op | `sfcore.enchants.give` |
| `/sfench success` | Command: /sfenchant, /sfe | `/sfench success <player> [amount]` | `/sfench success Notch 2` | Give Success scroll(s) to a player. | op | `sfcore.enchants.give` |
| `/sfench list` | Command: /sfenchant, /sfe | `/sfench list [category]` | `/sfench list combat` | List Arcanum enchantments, optionally filtered by category. | op | `sfcore.enchants.list` |
| `/sfench info` | Command: /sfenchant, /sfe | `/sfench info <enchant>` | `/sfench info frostbite` | Show full details for a specific enchantment. | op | `sfcore.enchants.list` |
| `/sfench reload` | Command: /sfenchant, /sfe | `/sfench reload` | `/sfench reload` | Reload the Arcanum config and category/roster files. | op | `sfcore.enchants.reload` |
| `/sfench loot` | Command: /sfenchant, /sfe | `/sfench loot <reload \| toggle <chest>>` | `/sfench loot reload` | Reload world-loot config, or toggle scroll generation for a chest type. | op | `sfcore.enchants.loot` |
| `/sfench debug` | Command: /sfenchant, /sfe | `/sfench debug [on/off]` | `/sfench debug on` | Toggle per-admin Arcanum proc-debug logging for yourself. | op | `sfcore.enchants.debug` |
| `--` | `--` | `--` | `--` | **Wildcards & Bundles** | `--` | `--` |
| `/sf` | Command: /sfcore, /sfc | `/sf <admin subcommand>` | `/sf reload` | Bundle of admin-level SF-Core actions (reload/diagnostics/menu). | op | `sfcore.admin.*` |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Testing` | Wildcard bundle for all moderation actions, including banlist access. | op | `sfcore.moderation.*` |
| `/sf` | Command: /sfcore, /sfc | `/sf <config/debug>` | `/sf debug` | Bundle for config and debug permissions. | op | `sfcore.debug.*` |
| `/sf updates` | Command: /sfcore, /sfc | `/sf updates` | `/sf updates` | Bundle for update and version permissions. | op | `sfcore.updates.*` |
| `/spawn` | `--` | `/spawn <tp/set/delete/info>` | `/spawn set` | Wildcard for all spawn permissions. | op | `sfcore.spawn.*` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <subcommand>` | `/warp set hub` | Wildcard for all warp permissions. | op | `sfcore.warp.*` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish` | Wildcard for all vanish tiers (self + others + admin). | op | `sfcore.vanish.*` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | Wildcard for both inventory-see tiers (basic + full). | op | `sfcore.invsee.*` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Wildcard for the staff overview menu and its actions. | op | `sfcore.staff.*` |
| `/hub` | Command: /lobby | `/hub <subcommand>` | `/hub on` | Wildcard for every hub / lobby permission (use + all hotbar items + admin). | op | `sfcore.hub.*` |
| `/sfench` | Command: /sfenchant, /sfe | `/sfench <subcommand>` | `/sfench admin` | Wildcard for the full Arcanum module (admin/give/apply/list/reload/loot/debug/use/browse/book). | op | `sfcore.enchants.*` |
| `/holo` | Command: /hg, /hologram | `/holo <subcommand>` | `/holo create welcome` | Wildcard for the full hologram module (create/edit/delete/admin). | op | `sfcore.holo.*` |
| `N/A` | `--` | `--` | `--` | Wildcard for all SF-Core permissions. | op | `sfcore.*` |
| `--` | `--` | `--` | `--` | **Holograms — admin** | `--` | `--` |
| `/holo` | Command: /hg, /hologram | `/holo <subcommand>` | `/holo list` | Root command for the SF-Core holograms module. The base command itself only requires `sfcore.holo.use`; each subcommand is gated by its own node below. | true | `sfcore.holo.use` |
| `/holo create` | Command: /hg, /hologram | `/holo create <id> [text]` | `/holo create welcome &6Welcome` | Create a new hologram at the caller's location with an optional first line. Persists to `holograms.yml`. | op | `sfcore.holo.create` |
| `/holo delete` | Command: /hg, /hologram | `/holo delete <id>` | `/holo delete welcome` | Despawn and remove a hologram from disk. | op | `sfcore.holo.delete` |
| `/holo list` | Command: /hg, /hologram | `/holo list` | `/holo list` | List every registered hologram with its world / coordinates / line count. | op | `sfcore.holo.list` |
| `/holo info` | Command: /hg, /hologram | `/holo info [id]` | `/holo info welcome` | Show module info (no id) or full settings + lines for a single hologram. | op | `sfcore.holo.info` |
| `/holo tp` | Command: /hg, /hologram | `/holo tp <id>` | `/holo tp welcome` | Teleport to a hologram's stored location. | op | `sfcore.holo.tp` |
| `/holo move` | Command: /hg, /hologram | `/holo move <id> <x> <y> <z> [yaw] [world]` | `/holo move welcome 0 65 0 0 world` | Reposition a hologram to explicit coordinates. | op | `sfcore.holo.edit` |
| `/holo movehere` | Command: /hg, /hologram | `/holo movehere <id>` | `/holo movehere welcome` | Move a hologram to the caller's current location (+2y). | op | `sfcore.holo.edit` |
| `/holo copy` | Command: /hg, /hologram | `/holo copy <src> <dest>` | `/holo copy welcome welcome2` | Duplicate a hologram with all lines and settings, placed at the caller. | op | `sfcore.holo.create` |
| `/holo line add` | Command: /hg, /hologram | `/holo line add <id> <text…\|icon <mat>\|block <mat>>` | `/holo line add welcome &eHello` | Append a line. Use `icon <material>` or `block <material>` for non-text lines. | op | `sfcore.holo.edit` |
| `/holo line set` | Command: /hg, /hologram | `/holo line set <id> <index> <value…>` | `/holo line set welcome 1 &fNew` | Replace the line at 1-based index. | op | `sfcore.holo.edit` |
| `/holo line remove` | Command: /hg, /hologram | `/holo line remove <id> <index>` | `/holo line remove welcome 2` | Remove the line at 1-based index. Alias: `delete`. | op | `sfcore.holo.edit` |
| `/holo line insertbefore` | Command: /hg, /hologram | `/holo line insertbefore <id> <index> <value…>` | `/holo line insertbefore welcome 2 &7Inserted` | Insert before the given line. Alias: `before`. | op | `sfcore.holo.edit` |
| `/holo line insertafter` | Command: /hg, /hologram | `/holo line insertafter <id> <index> <value…>` | `/holo line insertafter welcome 1 &7Inserted` | Insert after the given line. Alias: `after`. | op | `sfcore.holo.edit` |
| `/holo line swap` | Command: /hg, /hologram | `/holo line swap <id> <a> <b>` | `/holo line swap welcome 1 3` | Swap two lines. | op | `sfcore.holo.edit` |
| `/holo scale` | Command: /hg, /hologram | `/holo scale <id> <factor>` | `/holo scale welcome 1.5` | Set the hologram scale factor (0.05–20.0). | op | `sfcore.holo.edit` |
| `/holo billboard` | Command: /hg, /hologram | `/holo billboard <id> <FIXED\|VERTICAL\|HORIZONTAL\|CENTER>` | `/holo billboard welcome CENTER` | Set the billboard mode. | op | `sfcore.holo.edit` |
| `/holo background` | Command: /hg, /hologram | `/holo background <id> <ARGB\|transparent\|none>` | `/holo background welcome 80FF0000` | Set the text background colour. | op | `sfcore.holo.edit` |
| `/holo textalpha` | Command: /hg, /hologram | `/holo textalpha <id> <0-255>` | `/holo textalpha welcome 200` | Set text opacity. | op | `sfcore.holo.edit` |
| `/holo shadow` | Command: /hg, /hologram | `/holo shadow <id> <true\|false>` | `/holo shadow welcome true` | Toggle drop shadow on text. | op | `sfcore.holo.edit` |
| `/holo alignment` | Command: /hg, /hologram | `/holo alignment <id> <CENTER\|LEFT\|RIGHT>` | `/holo alignment welcome LEFT` | Set text alignment within the line. | op | `sfcore.holo.edit` |
| `/holo seethrough` | Command: /hg, /hologram | `/holo seethrough <id> <true\|false>` | `/holo seethrough welcome true` | Toggle see-through-walls rendering. | op | `sfcore.holo.edit` |
| `/holo doublesided` | Command: /hg, /hologram | `/holo doublesided <id> <true\|false>` | `/holo doublesided welcome false` | Toggle front/back culling (false → only the front is visible). | op | `sfcore.holo.edit` |
| `/holo showrange` | Command: /hg, /hologram | `/holo showrange <id> <blocks>` | `/holo showrange welcome 32` | Visibility distance in blocks. | op | `sfcore.holo.edit` |
| `/holo updaterange` | Command: /hg, /hologram | `/holo updaterange <id> <blocks>` | `/holo updaterange welcome 48` | Per-viewer text refresh distance (≥ showrange). | op | `sfcore.holo.edit` |
| `/holo board` | Command: /hg, /hologram | `/holo board <id> <…>` | `/holo board welcome enable true` | Configure the block-display slab backing (Phase 5). | op | `sfcore.holo.edit` |
| `/holo aim` | Command: /hg, /hologram | `/holo aim` | `/holo aim` | Resolve the hologram closest to the caller's look direction (within 12 blocks). | op | `sfcore.holo.edit` |
| `/holo enable` | Command: /hg, /hologram | `/holo enable` | `/holo enable` | Flip the master flag in `systems/holograms.yml → enabled: true` and reload. | op | `sfcore.holo.admin` |
| `/holo disable` | Command: /hg, /hologram | `/holo disable` | `/holo disable` | Flip the master flag to `false` and tear down live entities. | op | `sfcore.holo.admin` |
| `/holo reload` | Command: /hg, /hologram | `/holo reload` | `/holo reload` | Reload `systems/holograms.yml` and re-spawn every hologram. | op | `sfcore.holo.admin` |
| `/holo debug` | Command: /hg, /hologram | `/holo debug [id]` | `/holo debug` | Spawn / toggle a built-in test hologram showing backend identity. | op | `sfcore.holo.admin` |
