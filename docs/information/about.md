# Commands and Permissions

## Player (Public)
| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Information** | `--` | `--` |
| `/sf about` | `--` | `/sf about` | `/sf about` | Shows extended plugin information, credits, and links. | true | `sfcore.about` |
| `/help` | Command: /?, /bukkit:help, /bukkit:?, /minecraft:help, /minecraft:? | `/help [page]` | `/help 2` | Open the SF-Core paginated GUI help menu listing default-true commands A-Z. | true | `sfcore.help.gui` |
| `/sf commands` | `--` | `/sf commands [category]` | `/sf commands information` | Lists available commands filtered by category and permissions. | true | `sfcore.commands.list` |
| `/sf help` | Command: /sfcore, /sfc; Entry: h | `/sf help [page/category/command]` | `/sf help reload` | Shows SF-Core help pages. | true | `sfcore.help` |
| `/sf info` | `--` | `/sf info` | `/sf info` | Displays general information about SF-Core. | true | `sfcore.info` |
| `--` | `--` | `--` | `--` | **Language** | `--` | `--` |
| `/language` | `--` | `/language <English/EN/German/DE>` | `/language EN` | Change your preferred SF-Core language. | true | `sfcore.language` |
| `/sprache` | `--` | `/sprache <Englisch/EN/Deutsch/DE>` | `/sprache Deutsch` | German alias for the language command. | true | `sfcore.language` |
| `--` | `--` | `--` | `--` | **Menus** | `--` | `--` |
| `/sf` | Command: /sfcore, /sfc | `/sf` | `/sf` | Open the main menu; opens the Admin Menu if permitted. | true | `none` |
| `--` | `--` | `--` | `--` | **Teleportation** | `--` | `--` |
| `/back` | `--` | `/back` | `/back` | Return to your previous location. | true | `sfcore.back` |
| `/delhome` | `--` | `/delhome <name>` | `/delhome base` | Delete one of your homes. | true | `sfcore.home.delete` |
| `/home` | `--` | `/home [name]` | `/home base` | Teleport to one of your homes. | true | `sfcore.home` |
| `/homes` | `--` | `/homes` | `/homes` | List all of your homes. | true | `sfcore.home.list` |
| `/sethome` | `--` | `/sethome [name]` | `/sethome base` | Set a named home at your current location. | true | `sfcore.home.set` |
| `/spawn` | Subcommands: tp, teleport, go, goto | `/spawn` | `/spawn` | Teleport to the server spawn point. | true | `sfcore.spawn.tp, sfcore.spawn` |
| `/spawn info` | Subcommands: information, details, about | `/spawn info` | `/spawn info` | Show spawn location details. | true | `sfcore.spawn.info` |
| `--` | `--` | `--` | `--` | **Warps** | `--` | `--` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <name/subcommand>` | `/warp market` | Base permission required for all warp commands. | true | `sfcore.warp` |
| `/warp categories` | Subcommands: cats, groups, sections | `/warp categories` | `/warp categories` | List warp categories with counts. | true | `sfcore.warp.list` |
| `/warp category` | Subcommands: cat, group, section | `/warp category <name>` | `/warp category shops` | List warps in a category. | true | `sfcore.warp.list` |
| `/warp gui` | Subcommands: menu, open, view | `/warp gui [category]` | `/warp gui shops` | Open the warp GUI (also `/warp` with no args). | true | `sfcore.warp.gui` |
| `/warp info` | Subcommands: information, details, about, show | `/warp info <name>` | `/warp info market` | Show warp details. | true | `sfcore.warp.info` |
| `/warp list` | Subcommands: ls, all, browse, page | `/warp list [page] [category]` | `/warp list 2` | List available warps, optionally filtered by category. | true | `sfcore.warp.list` |
| `/warp tp` | Subcommands: teleport, go, goto, warp | `/warp tp <name>` | `/warp tp market` | Teleport to a warp (also `/warp <name>`). | true | `sfcore.warp.tp` |

## Admin (Staff)
| Command | Aliases | Usage (arguments) | Example usage | Description | OP/Default permission level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Admin / Moderation** | `--` | `--` |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Repeated griefing` | Permanently ban a player and log the action to the moderation audit/Discord webhook. | op | `sfcore.moderation.ban` |
| `/banlist` | Command: /blist | `/banlist` | `/banlist` | Show all active bans, including temporary bans that have not expired yet. | op | `sfcore.moderation.banlist` |
| `/kill` | Command: /crosskill, /aimkill, /targetkill | `/kill` | `/kill` | Toggle kill mode; left-click to kill the entity in your crosshairs. | op | `sfcore.kill` |
| `/kick` | `--` | `/kick <player> [reason]` | `/kick HotPotato Chat spam` | Kick an online player and write the event to the moderation log pipeline. | op | `sfcore.moderation.kick` |
| `/mute` | `--` | `/mute <player> [reason]` | `/mute HotPotato Caps spam` | Block a player from chatting until they are unmuted. | op | `sfcore.moderation.mute` |
| `/status` | `--` | `/status <player>` | `/status VeryPotter` | View a player's moderation profile card, current punishments, counts, and recent action history. | op | `sfcore.moderation.status` |
| `/tempban` | Command: /tban | `/tempban <player> [reason]` | `/tempban VeryPotter Exploit abuse` | Temporarily ban a player using the configured default duration in `config.yml` (`moderation.defaults.tempban-duration`). | op | `sfcore.moderation.tempban` |
| `/unban` | `--` | `/unban <player> [reason]` | `/unban ChanceDaRepper Appeal accepted` | Remove an active ban and log the pardon event. | op | `sfcore.moderation.unban` |
| `/unmute` | `--` | `/unmute <player> [reason]` | `/unmute HotPotato Time served` | Remove a chat mute from a player profile. | op | `sfcore.moderation.unmute` |
| `/warn` | `--` | `/warn <player> [reason]` | `/warn VeryPotter Respect chat rules` | Add a stored warning entry for a player profile and log it externally. | op | `sfcore.moderation.warn` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Open the staff overview GUI listing all online players (alphabetical, self excluded) as skin heads with hover profile cards (first-join date, total active time, current session, country, language, and a moderation report card of warnings/mutes/kicks/tempbans/bans). Click a head to open the per-player action sub-menu. The bottom row carries a search head (chat-prompted player lookup) and a custom red-X close button. Hidden from non-permitted players. | op | `sfcore.staff.menu` |
| `--` | `--` | `--` | `--` | **Config & Debug** | `--` | `--` |
| `/sf config` | Command: /sfcore, /sfc | `/sf config` | `/sf config` | Displays loaded config files and their status. | op | `sfcore.debug.config` |
| `/sf config validate` | Command: /sfcore, /sfc | `/sf config validate` | `/sf config validate` | Validates config files and reports errors or deprecated keys. | op | `sfcore.debug.config.validate` |
| `/sf debug` | Command: /sfcore, /sfc | `/sf debug` | `/sf debug` | Shows debug status and active debug flags. | op | `sfcore.debug` |
| `/sf debug disable` | Command: /sfcore, /sfc | `/sf debug disable` | `/sf debug disable` | Disables debug logging. | op | `sfcore.debug.toggle` |
| `/sf debug dump` | Command: /sfcore, /sfc | `/sf debug dump` | `/sf debug dump` | Dumps internal state to a log file for troubleshooting. | op | `sfcore.debug.dump` |
| `/sf debug enable` | Command: /sfcore, /sfc | `/sf debug enable` | `/sf debug enable` | Enables debug logging temporarily. | op | `sfcore.debug.toggle` |
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
| `--` | `--` | `--` | `--` | **Information** | `--` | `--` |
| `/sf permissions` | Command: /sfcore, /sfc | `/sf permissions [command/category]` | `/sf permissions reload` | Lists permission nodes for a command or category. | false | `sfcore.permissions.view` |
| `--` | `--` | `--` | `--` | **Menus** | `--` | `--` |
| `/sf` | Command: /sfcore, /sfc | `/sf` | `/sf` | Open the Admin Menu when using /sf with no args. | op | `sfcore.admin.menu` |
| `--` | `--` | `--` | `--` | **Player Management** | `--` | `--` |
| `/god` | Command: /gmode, /godmode, /invincible, /immortal | `/god [player]` | `/god Notch` | Toggle complete invincibility for yourself or a target. | op | `sfcore.god` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | View a player's inventory. Basic tier limited to non-op, non-staff targets. Console-logged with ANSI tier marker. | op | `sfcore.invsee.basic` (lower tier) |
| `/invsee` (full tier) | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee SeniorStaff` | Same as above but bypasses target-restriction so any player including ops/staff can be viewed. | op | `sfcore.invsee.full` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish Notch` | Toggle staff vanish — hidden from other players, ignored by hostile mobs, immune to passive damage triggers. Console-logged with ANSI staff line. | op | `sfcore.vanish` (self) / `sfcore.vanish.others` (others) |
| `/vital` | Command: /restore, /regen | `/vital [player]` | `/vital Notch` | Restore health and hunger for yourself or a target. | op | `sfcore.vital` |
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
| `--` | `--` | `--` | `--` | **Spawn** | `--` | `--` |
| `/setspawn` | Also: /spawn set | `/setspawn` | `/setspawn` | Set the server spawn point to your current location. | op | `sfcore.spawn.set` |
| `/spawn delete` | Subcommands: remove, del, clear, reset | `/spawn delete [confirm]` | `/spawn delete confirm` | Delete/unset the server spawn point (confirmation required). | op | `sfcore.spawn.delete` |
| `/spawn set` | Command: /setspawn; Subcommands: create, new, setup | `/spawn set` | `/spawn set` | Set the server spawn point to your current location. | op | `sfcore.spawn.set` |
| `--` | `--` | `--` | `--` | **Teleportation** | `--` | `--` |
| `/top` | Command: /above, /ground, /up | `/top [player]` | `/top Notch` | Teleport to the highest safe block above the target or yourself. | op | `sfcore.top` |
| `--` | `--` | `--` | `--` | **Updates & Version** | `--` | `--` |
| `/sf updates` | Command: /sfcore, /sfc | `/sf updates` | `/sf updates` | Checks for available updates and displays a summary. | op | `sfcore.updates.check` |
| `/sf updates check` | Command: /sfcore, /sfc | `/sf updates check` | `/sf updates check` | Forces an update check (placeholder). | op | `sfcore.updates.check` |
| `/sf updates notify` | Command: /sfcore, /sfc | `/sf updates notify` | `/sf updates notify` | Toggles update notifications for the executor. | op | `sfcore.updates.notify` |
| `/sf version` | Command: /sfcore, /sfc | `/sf version` | `/sf version` | Shows current SF-Core version and build tag. | op | `sfcore.version` |
| `--` | `--` | `--` | `--` | **Utility** | `--` | `--` |
| `/research` | Command: /discover, /itemprofile, /iteminfo | `/research [item]` | `/research Diamond Sword` | Display detailed information about a Minecraft item. | op | `sfcore.research` |
| `/craft` | Command: /workbench, /crafting | `/craft` | `/craft` | Open a virtual 3x3 crafting table at your location. | op | `sfcore.craft` |
| `/anvil` | Command: /forge | `/anvil` | `/anvil` | Open a virtual anvil (requires Spigot 1.14+). | op | `sfcore.anvil` |
| `/enchant` | Command: /enchanting, /enchanttable | `/enchant` | `/enchant` | Open a virtual enchanting table forced to maximum power. | op | `sfcore.enchant` |
| `/smith` | Command: /smithing, /smithtable | `/smith` | `/smith` | Open a virtual smithing table (requires 1.16+; full functionality on Paper 1.19+). | op | `sfcore.smith` |
| `/feed` | `--` | `/feed` | `/feed` | Restore your hunger bar (self only). | op | `sfcore.feed` |
| `/heal` | `--` | `/heal` | `/heal` | Restore your health to full (self only). | op | `sfcore.heal` |
| `--` | `--` | `--` | `--` | **Warps** | `--` | `--` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <name>` | `/warp market` | Optional per-category permission gate for warp visibility/teleport. | n/a | `sfcore.warp.category.<category>` |
| `/warp delete` | Subcommands: remove, del, clear, reset, unset | `/warp delete <name> [confirm]` | `/warp delete hub confirm` | Delete a warp (confirmation required). | op | `sfcore.warp.delete` |
| `/warp icon` | Subcommands: item, display | `/warp icon <name> [material]` | `/warp icon hub diamond` | Set or clear a warp icon for a warp. | op | `sfcore.warp.icon` |
| `/warp list` | Subcommands: ls, all, browse, page | `/warp list` | `/warp list` | View hidden warps in lists/GUI and toggle hidden view. | op | `sfcore.warp.hidden.view` |
| `/warp manage` | `--` | `/warp <manage action>` | `/warp set hub` | Bypass permissions for warp management actions (set/delete/rename/move/icon/public). | op | `sfcore.warp.manage` |
| `/warp move` | Subcommands: reloc, relocate, update, here | `/warp move <name>` | `/warp move hub` | Move a warp to your current location. | op | `sfcore.warp.move` |
| `/warp public` | Subcommands: publish, visible, visibility | `/warp public <name> [true/false]` | `/warp public hub false` | Toggle warp visibility (public/hidden). | op | `sfcore.warp.public` |
| `/warp rename` | Subcommands: ren, name, setname | `/warp rename <old> <new>` | `/warp rename old new` | Rename a warp. | op | `sfcore.warp.rename` |
| `/warp set` | Subcommands: create, new, setup, add, define | `/warp set <name> [confirm]` | `/warp set hub` | Create or update a warp at your location (confirm to overwrite). | op | `sfcore.warp.set` |
| `/warp tp` | Subcommands: teleport, go, goto, warp | `/warp tp <name> <player>` | `/warp tp market Notch` | Teleport another player to a warp. | op | `sfcore.warp.tp.others` |
| `--` | `--` | `--` | `--` | **Wildcards & Bundles** | `--` | `--` |
| `/sf` | Command: /sfcore, /sfc | `/sf <admin subcommand>` | `/sf reload` | Bundle of admin-level SF-Core actions (reload/diagnostics/menu). | op | `sfcore.admin.*` |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Testing` | Wildcard bundle for all moderation actions, including banlist access. | op | `sfcore.moderation.*` |
| `/sf` | Command: /sfcore, /sfc | `/sf <config/debug>` | `/sf debug` | Bundle for config and debug permissions. | op | `sfcore.debug.*` |
| `/sf updates` | Command: /sfcore, /sfc | `/sf updates` | `/sf updates` | Bundle for update and version permissions. | op | `sfcore.updates.*` |
| `/spawn` | `--` | `/spawn <tp/set/delete/info>` | `/spawn set` | Wildcard for all spawn permissions. | op | `sfcore.spawn.*` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <subcommand>` | `/warp set hub` | Wildcard for all warp permissions. | op | `sfcore.warp.*` |
| `N/A` | `--` | `--` | `--` | Wildcard for all SF-Core permissions. | op | `sfcore.*` |
