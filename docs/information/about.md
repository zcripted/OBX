# Commands and Permissions

> **Storage:** As of 2026-05-11, every per-player record SF-Core writes lives in SQLite
> (`plugins/SF-Core/sf-core.db`). Legacy YAML files (`data.yml`, `moderation.yml`,
> `playtime.yml`, `economy.yml`, `messaging.yml`, `kits-data.yml`) are migrated on first
> boot and renamed `<name>.migrated`. The only YAML files SF-Core still writes are
> admin-configurable definitions (`config.yml`, `motd.yml`, `kits.yml`, `worth.yml`,
> `jails.yml`, language packs).
>
> **/sf admin menu** now hosts a **Jail Center** sub-menu (under the Moderation slot) and
> a **Mob Tools** sub-menu (under the Fun Utilities slot) with click-through buttons for
> `/jails`, `/setjail`, `/deljail`, `/jailtime`, `/butcher 32`, `/spawnmob`, `/smite`,
> and `/tree`.

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
| `/tpa` | Command: /call, /tpask | `/tpa <player>` | `/tpa Notch` | Request to teleport to another player. Receiver gets clickable Accept/Deny buttons. | true | `sfcore.tpa` |
| `/tpahere` | Command: /tpaskhere | `/tpahere <player>` | `/tpahere Notch` | Request a player to teleport to you. | true | `sfcore.tpahere` |
| `/tpaccept` | Command: /tpyes, /tpac | `/tpaccept [player]` | `/tpaccept` | Accept the most recent (or named) pending teleport request. | true | `sfcore.tpaccept` |
| `/tpdeny` | Command: /tpno, /tpd | `/tpdeny [player]` | `/tpdeny` | Deny the most recent (or named) pending teleport request. | true | `sfcore.tpdeny` |
| `/tpcancel` | Command: /tpc | `/tpcancel` | `/tpcancel` | Cancel your outgoing teleport request. | true | `sfcore.tpcancel` |
| `/tptoggle` | Command: /tpt | `/tptoggle` | `/tptoggle` | Toggle whether you accept incoming teleport requests. | true | `sfcore.tptoggle` |
| `--` | `--` | `--` | `--` | **Messaging** | `--` | `--` |
| `/msg` | Command: /tell, /w, /whisper, /pm | `/msg <player> <message>` | `/msg Notch hi` | Send a private message. | true | `sfcore.msg` |
| `/reply` | Command: /r | `/r <message>` | `/r thanks!` | Reply to the most recent message partner. | true | `sfcore.msg` |
| `/ignore` | Command: /block | `/ignore [player]` | `/ignore Spammer` | Toggle ignoring messages and chat from a player; no args lists current ignores. | true | `sfcore.ignore` |
| `/mail` | `--` | `/mail <send|read|list|clear> [player] [message]` | `/mail send Notch See you later` | Send, read, or clear offline mail (max 50 entries per inbox). | true | `sfcore.mail` |
| `/me` | Command: /action | `/me <action>` | `/me waves` | Broadcast an action-style message. | true | `sfcore.me` |
| `--` | `--` | `--` | `--` | **Activity** | `--` | `--` |
| `/afk` | Command: /away | `/afk` | `/afk` | Toggle your AFK status. Idle players auto-flip after the configured threshold. | true | `sfcore.afk` |
| `--` | `--` | `--` | `--` | **Kits** | `--` | `--` |
| `/kit` | Command: /kits | `/kit [name|list|info]` | `/kit starter` | Claim a kit (gated by `sfcore.kit.<name>`), list available kits, or inspect contents. | true | `sfcore.kit` |
| `--` | `--` | `--` | `--` | **Economy** | `--` | `--` |
| `/balance` | Command: /bal, /money | `/balance [player]` | `/balance` | Show your balance, or another player's. | true | `sfcore.balance` |
| `/baltop` | Command: /balancetop, /moneytop | `/baltop [page]` | `/baltop` | View the wealth leaderboard. | true | `sfcore.baltop` |
| `/pay` | `--` | `/pay <player> <amount>` | `/pay Notch 50` | Pay another player from your balance. | true | `sfcore.pay` |
| `/worth` | Command: /value | `/worth [amount]` | `/worth 32` | Show the configured sell-price of the item in your hand. | true | `sfcore.worth` |
| `/sell` | `--` | `/sell <hand|all|material>` | `/sell hand` | Sell the item in your hand, every saleable item, or a specific material. | true | `sfcore.sell` |
| `/sellall` | `--` | `/sellall` | `/sellall` | Shorthand for `/sell all`. | true | `sfcore.sellall` |
| `--` | `--` | `--` | `--` | **Player Info** | `--` | `--` |
| `/seen` | Command: /lastseen | `/seen <player>` | `/seen Notch` | Show when a player was last seen. | true | `sfcore.seen` |
| `/firstseen` | `--` | `/firstseen <player>` | `/firstseen Notch` | Show a player's first join timestamp. | true | `sfcore.firstseen` |
| `/playtime` | Command: /ptime | `/playtime [player]` | `/playtime` | Show total playtime (and current session for online players). | true | `sfcore.playtime` |
| `/list` | Command: /who, /online, /players | `/list` | `/list` | List visible online players with AFK markers. | true | `sfcore.list` |
| `/near` | Command: /nearby | `/near [radius]` | `/near 32` | List players in range with distance markers. | true | `sfcore.near` |
| `/whois` | `--` | `/whois <player>` | `/whois Notch` | Detailed profile snapshot (status, world, mode, AFK, vanish, playtime). | true | `sfcore.whois` |
| `/realname` | `--` | `/realname <displayname>` | `/realname Mr.Cool` | Resolve a display/custom name back to its real account name. | true | `sfcore.realname` |
| `/info` | Command: /profile | `/info <player>` | `/info Notch` | Profile card with clickable Seen / Playtime / Tpa / Whois action row. | true | `sfcore.info.player` |
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
| `/afk <player>` | `--` | `/afk Notch` | `/afk Notch` | Toggle another player's AFK state. | op | `sfcore.afk.others` |
| `/broadcast` | Command: /bc | `/broadcast <message>` | `/broadcast Server restart in 5m` | Broadcast a server-wide message. | op | `sfcore.broadcast` |
| `/socialspy` | Command: /spy | `/socialspy` | `/socialspy` | Toggle staff visibility of every private message. | op | `sfcore.socialspy` |
| `/staffchat` | Command: /sc, /achat | `/staffchat <message>` | `/staffchat heads up` | Send a message visible only to staff. | op | `sfcore.staffchat` |
| `/tphere` | Command: /s, /tphr | `/tphere <player>` | `/tphere Notch` | Force a player to teleport to you. | op | `sfcore.tphere` |
| `/tppos` | Command: /tpcoords | `/tppos <x> <y> <z> [world] [yaw] [pitch]` | `/tppos 100 64 100` | Teleport to literal coordinates. Supports `~` for current. | op | `sfcore.tppos` |
| `/tpall` | Command: /tpeveryone | `/tpall` | `/tpall` | Teleport every online player to you. | op | `sfcore.tpall` |
| `/kit give` | `--` | `/kit give <player> <kit>` | `/kit give Notch starter` | Give a kit to another player (bypasses cooldown). | op | `sfcore.kit.give` |
| `/kit reload` | `--` | `/kit reload` | `/kit reload` | Reload `kits.yml` at runtime. | op | `sfcore.kit.reload` |
| `/eco` | Command: /economy | `/eco <give|take|set|reset> <player> [amount]` | `/eco give Notch 1000` | Staff balance management. | op | `sfcore.eco` |
| `--` | `--` | `--` | `--` | **Flight & Movement** | `--` | `--` |
| `/fly` | Command: /flight | `/fly [player]` | `/fly Notch` | Toggle flight; state persists across reconnects via SQLite. | op | `sfcore.fly` / `sfcore.fly.others` |
| `/flyspeed` | Command: /fspeed | `/flyspeed <0-10>` | `/flyspeed 5` | Set fly speed on a CMI-style 0-10 scale. | op | `sfcore.flyspeed` |
| `/walkspeed` | Command: /wspeed | `/walkspeed <0-10>` | `/walkspeed 4` | Set walk speed on a CMI-style 0-10 scale. | op | `sfcore.walkspeed` |
| `/freeze` | Command: /frz | `/freeze <player>` | `/freeze Griefer` | Toggle movement freeze with an action-bar reminder. | op | `sfcore.freeze` |
| `--` | `--` | `--` | `--` | **Inventory Utilities** | `--` | `--` |
| `/enderchest` | Command: /ec | `/enderchest [player]` | `/enderchest Notch` | Open your or another player's ender chest. | op | `sfcore.enderchest` / `sfcore.enderchest.others` |
| `/disposal` | Command: /trash | `/disposal` | `/disposal` | Open a one-shot trash inventory. | op | `sfcore.disposal` |
| `/hat` | Command: /head-on | `/hat` | `/hat` | Wear the held item as a hat. | op | `sfcore.hat` |
| `/clearinv` | Command: /ci, /clearinventory | `/clearinv [player]` | `/clearinv Notch` | Clear your or another player's inventory. | op | `sfcore.clearinv` / `sfcore.clearinv.others` |
| `/repair` | Command: /fix | `/repair [all]` | `/repair all` | Repair the held item or every repairable item. | op | `sfcore.repair` / `sfcore.repair.all` |
| `/more` | Command: /stack, /max | `/more` | `/more` | Fill the held stack to its maximum size. | op | `sfcore.more` |
| `/skull` | Command: /head | `/skull <player>` | `/skull Notch` | Receive a player head item. | op | `sfcore.skull` |
| `--` | `--` | `--` | `--` | **Item Editing** | `--` | `--` |
| `/itemname` | Command: /rename | `/itemname <name|clear>` | `/itemname &6King's Blade` | Rename the held item; `clear` removes the name. | op | `sfcore.itemname` |
| `/itemlore` | Command: /lore | `/itemlore <add|set|clear> [index] <text>` | `/itemlore add &7Fits like a glove` | Add, set, or clear lore on the held item. | op | `sfcore.itemlore` |
| `/unbreakable` | Command: /unb | `/unbreakable` | `/unbreakable` | Toggle unbreakable on the held item. | op | `sfcore.unbreakable` |
| `/give` | `--` | `/give <player> <material> [amount]` | `/give Notch DIAMOND 16` | Give items to another player. | op | `sfcore.give` |
| `/i` | Command: /item | `/i <material> [amount]` | `/i IRON_INGOT 32` | Give yourself items. | op | `sfcore.item` |
| `/book` | `--` | `/book [new|unsign|copy]` | `/book unsign` | Get a writable book, unsign a signed one, or copy held book. | op | `sfcore.book` |
| `--` | `--` | `--` | `--` | **Nicknames** | `--` | `--` |
| `/nick` | Command: /nickname | `/nick <name|off> [player]` | `/nick &b&lShadow` | Set or clear a nickname. Colour codes require `sfcore.nick.color`. | op | `sfcore.nick` / `sfcore.nick.others` / `sfcore.nick.color` |
| `--` | `--` | `--` | `--` | **World / Time / Weather** | `--` | `--` |
| `/time` | `--` | `/time <set|add> <value> [world]` | `/time set 1000` | Set or add to the world's time. | op | `sfcore.time` |
| `/day` | `--` | `/day [world]` | `/day` | Shortcut to morning. | op | `sfcore.time` |
| `/night` | `--` | `/night [world]` | `/night` | Shortcut to night. | op | `sfcore.time` |
| `/sun` | `--` | `/sun [world]` | `/sun` | Clear weather + noon. | op | `sfcore.weather` |
| `/weather` | `--` | `/weather <clear|rain|thunder> [world]` | `/weather rain` | Change weather. | op | `sfcore.weather` |
| `/ptime` | `--` | `/ptime <time|reset>` | `/ptime night` | Per-player time override; persisted to SQLite. | op | `sfcore.ptime` |
| `/pweather` | `--` | `/pweather <clear|rain|thunder|reset>` | `/pweather rain` | Per-player weather override (session-scoped). | op | `sfcore.pweather` |
| `--` | `--` | `--` | `--` | **Jail** | `--` | `--` |
| `/jail` | `--` | `/jail <player> <jail> [duration] [reason]` | `/jail Griefer downtown 1h Greifing` | Jail a player; success line includes a clickable `[Unjail]` button. | op | `sfcore.jail` |
| `/unjail` | `--` | `/unjail <player>` | `/unjail Griefer` | Release a jailed player. | op | `sfcore.unjail` |
| `/jails` | Command: /jaillist | `/jails` | `/jails` | List configured jail anchors. | op | `sfcore.jails` |
| `/setjail` | Command: /jailset | `/setjail <name>` | `/setjail downtown` | Create a jail anchor at your location. | op | `sfcore.setjail` |
| `/deljail` | Command: /jaildel | `/deljail <name>` | `/deljail downtown` | Delete a jail anchor. | op | `sfcore.deljail` |
| `/jailtime` | `--` | `/jailtime [player]` | `/jailtime Griefer` | Show remaining jail time. | op | `sfcore.jailtime` |
| `--` | `--` | `--` | `--` | **Mob / World Tools** | `--` | `--` |
| `/butcher` | Command: /killmobs | `/butcher [radius] [type]` | `/butcher 32 ZOMBIE` | Kill nearby mobs, optionally filtered by type. | op | `sfcore.butcher` |
| `/spawnmob` | Command: /mob | `/spawnmob <type> [count]` | `/spawnmob COW 4` | Spawn mobs at your crosshair. | op | `sfcore.spawnmob` |
| `/spawner` | Command: /setspawner | `/spawner <type>` | `/spawner SKELETON` | Change the spawned type of the looked-at spawner. | op | `sfcore.spawner` |
| `/smite` | Command: /lightning | `/smite [player]` | `/smite Notch` | Strike lightning at a player or your crosshair. | op | `sfcore.smite` |
| `/tree` | `--` | `/tree [type]` | `/tree REDWOOD` | Generate a tree at your crosshair. | op | `sfcore.tree` |
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
