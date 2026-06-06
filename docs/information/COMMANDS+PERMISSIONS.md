# Commands &amp; Permissions

The complete OBX command and permission reference. Every command the plugin registers is
listed here, grouped into a category and **sorted A–Z within its section** (sub-grouped
sections — Admin, Teleport, Utility, Other — are alphabetized within each labeled sub-group).
The first eight categories mirror the filterable tabs in the in-game `/help` GUI — **Admin**,
**Core**, **Language**, **Messaging**, **Moderation**, **Teleport**, **Utility**, **Other** —
followed by the per-feature groups. In `/help`, a command appears for any viewer who has
permission to run it.

> This document is kept in sync with `plugin.yml` (the `commands:` and `permissions:` blocks)
> and the feature modules' `command(...)` registrations. There are no dead or undeclared
> commands — declaration and registration are 1:1.

## How to read this reference

| Column | Meaning |
| --- | --- |
| **Command** | The primary command. Type it in chat with a leading `/`. |
| **Aliases** | Alternative names that run the exact same command. |
| **Usage (arguments)** | Argument syntax: `<required>`, `[optional]`, `a\|b` = choose one, `…` = repeats. |
| **Example usage** | A concrete, copy-pasteable example. |
| **Description** | What the command does and any notable behavior. |
| **Default level** | Who can run it out of the box (see legend below). |
| **Permission node** | The node to grant/revoke in your permissions plugin. |

**Default level legend**

| Value | Meaning |
| --- | --- |
| `everyone` | Open to all players — no permission node at all. |
| `true` | Granted to everyone by default (the node exists but defaults on). |
| `op` | Operators only by default. Grant the node to extend it to non-ops. |
| `false` | Nobody by default. Must be explicitly granted. |

**Permission tips for server owners**

- **`.others` variants** — many self-commands have a paired `.others` node to act on other
  players (e.g. `obx.afk.others`, `obx.clearinv.others`, `obx.flyspeed.others`, `obx.vanish.others`).
- **Bundle wildcards** — grant a whole feature at once instead of node-by-node:
  `obx.admin.*`, `obx.debug.*`, `obx.updates.*`, `obx.moderation.*`, `obx.spawn.*`,
  `obx.warp.*`, `obx.hub.*`, `obx.vanish.*`, `obx.invsee.*`, `obx.staff.*`, `obx.enchants.*`,
  `obx.holo.*`, and the master `obx.*`.
- **Tiered nodes** — a few commands check extra nodes beyond the one in `plugin.yml`
  (e.g. `invsee` → `obx.invsee.basic` + `obx.invsee.full`; `gamemode` → per-mode self/others
  nodes). These are documented in their rows.
- **Passive nodes** — some nodes grant behavior without a command (e.g. `obx.chat.staff`,
  `obx.message.color`). See the bottom of **Chat &amp; Staff**.

## Index

- [Admin](#admin)
- [Core](#core)
- [Language](#language)
- [Messaging](#messaging)
- [Moderation](#moderation)
- [Teleport](#teleport)
- [Utility](#utility)
- [Other](#other)
- [Economy](#economy)
- [Kits](#kits)
- [Nickname](#nickname)
- [Player Info](#player-info)
- [Items &amp; Inventory](#items--inventory)
- [World &amp; Environment](#world--environment)
- [Chat &amp; Staff](#chat--staff)
- [Jail](#jail)
- [Flight &amp; Movement](#flight--movement)

---

## Admin

Operational, diagnostic, and admin-teleport commands. Most live under `/obx` and require op.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Admin teleport** | `--` | `--` |
| `/kill` | Command: /crosskill, /aimkill, /targetkill | `/kill [player]` | `/kill Notch` | With no argument, toggles kill mode (left-click the entity in your crosshair to execute it). With a player argument, kills that online player directly. Box-style messages. | op | `obx.kill` |
| `/tp` | Command: /teleport | `/tp <player> [target]` | `/tp Notch` | Teleport to a player, or move one player to another. The console can move players but cannot teleport itself, and there is no console to teleport to. | op | `obx.teleport.admin` |
| `/tphere` | `--` | `/tphere <player>` | `/tphere Notch` | Bring a player to your location (you can't target yourself). | op | `obx.teleport.admin` |
| `--` | `--` | `--` | `--` | **Config &amp; Debug** | `--` | `--` |
| `/obx config` | Command: /obsidian, /obc | `/obx config` | `/obx config` | List loaded config files and their status, with clickable `« Prev` / `Next »` pagination. | op | `obx.debug.config` |
| `/obx config validate` | Command: /obsidian, /obc | `/obx config validate` | `/obx config validate` | Validate config files and report errors or deprecated keys. | op | `obx.debug.config.validate` |
| `/obx debug` | Command: /obsidian, /obc | `/obx debug` | `/obx debug` | Show debug status and active debug flags. | op | `obx.debug` |
| `/obx debug disable` | Command: /obsidian, /obc | `/obx debug disable` | `/obx debug disable` | Disable debug logging. | op | `obx.debug.toggle` |
| `/obx debug dump` | Command: /obsidian, /obc | `/obx debug dump` | `/obx debug dump` | Dump internal state to a log file for troubleshooting. | op | `obx.debug.dump` |
| `/obx debug enable` | Command: /obsidian, /obc | `/obx debug enable` | `/obx debug enable` | Enable debug logging temporarily. | op | `obx.debug.toggle` |
| `--` | `--` | `--` | `--` | **Reload &amp; Diagnostics** | `--` | `--` |
| `/obx diagnostics` | Command: /obsidian, /obc | `/obx diagnostics` | `/obx diagnostics` | Run a quick health check (platform, modules, config status, storage, overall health). | op | `obx.admin.diagnostics` |
| `/obx diagnostics full` | Command: /obsidian, /obc | `/obx diagnostics full` | `/obx diagnostics full` | Health check plus extended detail: registered services, detected hooks, and recorded errors. | op | `obx.admin.diagnostics.full` |
| `/obx reload` | Command: /obsidian, /obc | `/obx reload` | `/obx reload` | Reload OBX configs and reinitialize all feature modules safely. Result shows a hover with per-module load times. | op | `obx.admin.reload` |
| `/obx reload <file>` | Command: /obsidian, /obc | `/obx reload <file>` | `/obx reload config.yml` | Reload a specific file from the plugin data folder (timed, with a hover). | op | `obx.admin.reload.features` |
| `/obx reload config` | Command: /obsidian, /obc | `/obx reload config` | `/obx reload config` | Reload configuration files only. | op | `obx.admin.reload.config` |
| `/pl` | Command: /plugins | `/pl` | `/pl` | List loaded plugins grouped by Bukkit/Paper/Purpur/Folia with status colors (enabled/disabled/broken). | op | `obx.pl` |
| `/preview` | Command: /prev | `/preview <text>` | `/preview <gradient:#ff0000:#00ff00>Rules:</gradient> <yellow>1. No Griefing` | Render a MiniMessage / hex / gradient / legacy `&`-code string to **ANSI truecolor in the console** (hex isn't expressible with `§` codes) — or to chat in-game. Run with no args for color/format examples. | op | `obx.preview` |
| `/tps` | Command: /lag, /mspt, /performance | `/tps` | `/tps` | Show server TPS (1m/5m/15m, each with a labeled hover tooltip), MSPT, online count, and uptime. Console output renders in ANSI color. | op | `obx.tps` |
| `/health` | Command: /healthcheck (also `/obx health`) | `/health` | `/health` | Staff-only **full server health check** in one clean box-style report: TPS windows, tick time vs the 50 ms budget, process/system CPU + cores, heap memory, entities & chunks across worlds, players & average/worst ping, sync queue & async workers, SQLite store state/size, disk capacity, and per-player averages (entities, ping, tick & memory share). Every row has a hover tooltip; key rows and the footer `[⟳ Re-run] [TPS Detail] [Diagnostics]` buttons carry click actions. Color-graded green/yellow/red. | op | `obx.admin.health` |
| `--` | `--` | `--` | `--` | **Modules** | `--` | `--` |
| `/obx afk` | Command: /obsidian, /obc | `/obx afk <on/off/status>` | `/obx afk off` | Toggle the **entire AFK system** (auto-AFK detection, the idle timeout/kick, and AFK messages) on/off at runtime. Box-style confirmation in-game + console; also settable via `config.yml` (`afk.enabled`) or the Module Toggles admin GUI. | op | `obx.admin.modules.afk` |
| `/obx deathdrop` | Command: /obsidian, /obc | `/obx deathdrop <on/off/status>` | `/obx deathdrop on` | Toggle **death-item grouping**: on death, all dropped items combine into one carry-all entity (a chest, or a bundle on 1.17+) with a floating `×<count>` hologram; walking over it restores the items (overflow stays inside). Box-style confirmation in-game (with a `[Toggle]` button) + console; also settable via `config.yml` (`modules.deathdrop`) or the Module Toggles admin GUI. | op | `obx.admin.modules.deathdrop` |
| `/obx joinleave` | Command: /obsidian, /obc | `/obx joinleave <on/off/status>` | `/obx joinleave off` | Toggle the join/leave broadcast module at runtime. | op | `obx.admin.modules.joinleave` |
| `/obx joinmotd` | Command: /obsidian, /obc | `/obx joinmotd <on/off/status>` | `/obx joinmotd on` | Toggle the in-game welcome MOTD module at runtime. | op | `obx.admin.modules.joinmotd` |
| `--` | `--` | `--` | `--` | **Admin menu** | `--` | `--` |
| `/obx` | Command: /obsidian, /obc | `/obx` | `/obx` | Open the Admin Menu when using `/obx` with no args. The Server Control tile shows live player count + uptime. | op | `obx.admin.menu` |
| `--` | `--` | `--` | `--` | **Updates &amp; Version** | `--` | `--` |
| `/obx updates` | Command: /obsidian, /obc | `/obx updates` | `/obx updates` | Check for available updates and display a **box-style** summary. The server also checks automatically: once at startup (console) and every `updates.check-interval-minutes` (default **15**) while running, announcing each new release once. | op | `obx.updates.check` |
| `/obx updates check` | Command: /obsidian, /obc | `/obx updates check` | `/obx updates check` | Force an update check (placeholder). | op | `obx.updates.check` |
| `/obx updates notify` | Command: /obsidian, /obc | `/obx updates notify` | `/obx updates notify` | Toggle update notifications for the executor. **ON by default** for `obx.updates.notify` holders (notified on join + once when a new release is found while the server runs); the opt-out is saved to the database and survives restarts. | op | `obx.updates.notify` |
| `/obx version` | Command: /obsidian, /obc | `/obx version` | `/obx version` | Show current OBX version and build tag. | op | `obx.version` |

## Core

The public-facing base commands: the help/menu/info surface that every player sees.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/help` | Command: /?, /bukkit:help, /bukkit:?, /minecraft:help, /minecraft:? | `/help [page]` | `/help 2` | Open the OBX paginated GUI help menu listing default-true commands A–Z. | true | `obx.help.gui` |
| `/list` | Command: /players, /online, /who, /playerlist | `/list` | `/list` | Show online players split into Staff and Players sections (staff in red); names are click-to-message (`/msg <name>`). Staff = op **or** `obx.staff`. Default command — no permission required (`obx.list.vanished` still gates showing vanished players). | everyone | _none_ |
| `/obx` | Command: /obsidian, /obc | `/obx` | `/obx` | Open the main menu; opens the Admin Menu if permitted. | true | `none` |
| `/obx about` | `--` | `/obx about` | `/obx about` | Show extended plugin information, credits, and links. | true | `obx.about` |
| `/obx commands` | `--` | `/obx commands [category]` | `/obx commands information` | List available commands filtered by category and permissions. | true | `obx.commands.list` |
| `/obx help` | Command: /obsidian, /obc; Entry: h | `/obx help [page/category/command]` | `/obx help reload` | Show OBX help pages. | true | `obx.help` |
| `/obx info` | `--` | `/obx info` | `/obx info` | Display general information about OBX. | true | `obx.info` |
| `/obx permissions` | Command: /obsidian, /obc | `/obx permissions [command/category]` | `/obx permissions reload` | List permission nodes for a command or category. | false | `obx.permissions.view` |
| `/stafflist` | Command: /sl, /os, /mods, /slist, /sonline | `/stafflist` | `/sl` | Show only the online op/staff (op **or** `obx.staff`) in the player-list box; names are click-to-message. | true | `obx.stafflist` |

## Language

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/idioma` | `--` | `/idioma <Inglés/EN/Alemán/DE/Español/ES>` | `/idioma Español` | Spanish alias for the language command. | true | `obx.language` |
| `/language` | `--` | `/language <English/EN/German/DE/Spanish/ES>` | `/language EN` | Change your preferred OBX language. | true | `obx.language` |
| `/sprache` | `--` | `/sprache <Englisch/EN/Deutsch/DE/Spanisch/ES>` | `/sprache Deutsch` | German alias for the language command. | true | `obx.language` |

## Messaging

Private player-to-player messaging with a clickable reply flow and an offline inbox.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/ignore` | Command: /block | `/ignore [player]` | `/ignore Notch` | Toggle ignoring another player's private messages. Bypassable with `obx.message.ignore.bypass`. | true | `obx.ignore` |
| `/inbox` | Command: /inbound | `/inbox` | `/inbox` | Open your private-message inbox GUI (messages received while offline; click one to read). Clearing an empty inbox now shows a formal error. | true | `obx.message` |
| `/mail` | `--` | `/mail <send\|read\|list\|clear> [player] [message]` | `/mail send Notch hi` | Send, read, list, or clear mail. | true | `obx.mail` |
| `/msg` | Command: /tell, /pm, /whisper | `/msg <player> <message>` | `/msg Notch hi` | Send a private message — live if online (with a clickable Reply), else queued to their inbox. Cannot message the console. | true | `obx.message` |
| `/rply` | Command: /reply, /r | `/rply [message]` | `/rply hey` | Reply to your most recent sender. No args opens a 60s reply draft (type in chat; `cancel` or the Cancel button aborts). | true | `obx.message` |

## Moderation

Punishments, the moderation audit pipeline, and staff oversight tools (vanish, invsee).

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/ban` | `--` | `/ban <player> [reason]` | `/ban ChanceDaRepper Repeated griefing` | Permanently ban a player and log the action to the moderation audit/Discord webhook. | op | `obx.moderation.ban` |
| `/banlist` | Command: /blist | `/banlist` | `/banlist` | Show all active bans (including not-yet-expired temp bans) in a box-style list. | op | `obx.moderation.banlist` |
| `/invsee` | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee Notch` | View a player's inventory. Basic tier limited to non-op, non-staff targets. Console-logged with ANSI tier marker. | op | `obx.invsee.basic` (lower tier) |
| `/invsee` (full tier) | Command: /inv, /isee, /inventory, /ivs, /inventorysee | `/invsee <player>` | `/invsee SeniorStaff` | Same as above but bypasses target restriction — any player including ops/staff can be viewed. | op | `obx.invsee.full` |
| `/ipban` | Command: /banip | `/ipban <player\|ip> [reason]` | `/ipban Notch alt-evasion` | Ban an IP address, or the current address of an online player, and kick everyone on it. Native IP bans persist in `banned-ips.json`. | op | `obx.moderation.ipban` |
| `/ipunban` | Command: /unbanip, /pardonip | `/ipunban <ip>` | `/ipunban 1.2.3.4` | Lift an IP ban. | op | `obx.moderation.ipunban` |
| `/kick` | `--` | `/kick <player> [reason]` | `/kick HotPotato Chat spam` | Kick an online player and write the event to the moderation log pipeline. | op | `obx.moderation.kick` |
| `/mute` | `--` | `/mute <player> [reason]` | `/mute HotPotato Caps spam` | Block a player from chatting until they are unmuted. | op | `obx.moderation.mute` |
| `/staff` | Command: /staffmenu, /sm | `/staff` | `/staff` | Open the staff overview GUI: online players as skin heads with hover profile cards (first-join, active time, session, country, language, and a warnings/mutes/kicks/tempbans/bans report card). Click a head for per-player actions; bottom row has search + close. Hidden from non-permitted players. | op | `obx.staff.menu` |
| `/status` | `--` | `/status <player>` | `/status VeryPotter` | View a player's moderation profile card, current punishments, counts, and recent action history. | op | `obx.moderation.status` |
| `/tempban` | Command: /tban | `/tempban <player> [duration] [reason]` | `/tempban VeryPotter 3d Exploit abuse` | Temporarily ban a player. An optional leading duration token (`3d`, `2h`, `30m`, `1d12h`, …) sets the length; omit it to use the configured default in `config.yml` (`moderation.defaults.tempban-duration`). The remaining text is the reason. | op | `obx.moderation.tempban` |
| `/unban` | `--` | `/unban <player> [reason]` | `/unban ChanceDaRepper Appeal accepted` | Remove an active ban and log the pardon event. | op | `obx.moderation.unban` |
| `/unmute` | `--` | `/unmute <player> [reason]` | `/unmute HotPotato Time served` | Remove a chat mute from a player profile. | op | `obx.moderation.unmute` |
| `/vanish` | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish Notch` | Toggle staff vanish — hidden from other players, ignored by hostile mobs, immune to passive damage triggers. Console-logged with ANSI staff line. | op | `obx.vanish` (self) / `obx.vanish.others` (others) |
| `/vanish` (admin tier) | Command: /v, /hide, /sneak, /ghost | `/vanish [player]` | `/vanish` | Higher-tier vanish — invisible to every other player including other vanished staff. Holder can still see lower-tier `[L]` vanished users without revealing themselves. | op | `obx.vanish.admin` |
| `/warn` | `--` | `/warn <player> [reason]` | `/warn VeryPotter Respect chat rules` | Add a stored warning entry for a player profile and log it externally. | op | `obx.moderation.warn` |

## Teleport

Player teleportation: requests, homes, spawn, warps, and the hub/lobby. Spawn-set, warp
management, and hub administration are op-gated.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Requests &amp; navigation** | `--` | `--` |
| `/back` | `--` | `/back` | `/back` | Return to your previous location. | true | `obx.back` |
| `/pos` | Command: /position | `/pos` | `/pos` | Show your coordinates: a styled chat report with click-to-copy and a live action bar that tracks you for ~5s. Console has no position. | true | `obx.position` |
| `/top` | Command: /above, /ground, /up | `/top [player]` | `/top Notch` | Teleport to the highest safe block above the target or yourself. | op | `obx.top` |
| `/tpa` | `--` | `/tpa <player>` | `/tpa Notch` | Request to teleport to another player; they get a clickable Accept/Deny prompt (60s expiry). | true | `obx.teleport.request` |
| `/tpaccept` | Command: /tpyes | `/tpaccept` | `/tpaccept` | Accept a pending teleport request (also the request's Accept button). | true | `obx.teleport.request` |
| `/tpall` | Command: /tpeveryone | `/tpall` | `/tpall` | Teleport every online player to you. | op | `obx.tpall` |
| `/tpcancel` | Command: /tpc | `/tpcancel` | `/tpcancel` | Cancel your outgoing teleport request. | true | `obx.tpcancel` |
| `/tpdeny` | Command: /tpno | `/tpdeny` | `/tpdeny` | Deny a pending teleport request (also the request's Deny button). | true | `obx.teleport.request` |
| `/tppos` | Command: /tpcoords | `/tppos <x> <y> <z> [world] [yaw] [pitch]` | `/tppos 0 64 0 world` | Teleport to specific coordinates. | op | `obx.tppos` |
| `/tptoggle` | Command: /tpt | `/tptoggle` | `/tptoggle` | Toggle whether you accept incoming teleport requests. | true | `obx.tptoggle` |
| `--` | `--` | `--` | `--` | **Homes** | `--` | `--` |
| `/delhome` | `--` | `/delhome <name>` | `/delhome base` | Delete one of your homes. | true | `obx.home.delete` |
| `/home` | `--` | `/home [name]` | `/home base` | Teleport to one of your homes. | true | `obx.home` |
| `/homes` | `--` | `/homes` | `/homes` | List all of your homes (box-style). | true | `obx.home.list` |
| `/sethome` | `--` | `/sethome [name]` | `/sethome base` | Set a named home at your current location. | true | `obx.home.set` |
| `--` | `--` | `--` | `--` | **Spawn** | `--` | `--` |
| `/setspawn` | Also: /spawn set | `/setspawn` | `/setspawn` | Set the server spawn point to your current location. | op | `obx.spawn.set` |
| `/spawn` | Subcommands: tp, teleport, go, goto | `/spawn` | `/spawn` | Teleport to the server spawn point. | true | `obx.spawn.tp, obx.spawn` |
| `/spawn delete` | Subcommands: remove, del, clear, reset | `/spawn delete` | `/spawn delete` | Delete/unset the server spawn point. Confirm by clicking the **confirm** prompt or typing `confirm` in chat within 10s. | op | `obx.spawn.delete` |
| `/spawn info` | Subcommands: information, details, about | `/spawn info` | `/spawn info` | Show spawn location details. | true | `obx.spawn.info` |
| `/spawn set` | Command: /setspawn; Subcommands: create, new, setup | `/spawn set` | `/spawn set` | Set the server spawn point to your current location. | op | `obx.spawn.set` |
| `--` | `--` | `--` | `--` | **Warps** | `--` | `--` |
| `/warp` | Command: /warps, /w, /goto, /go, /travel | `/warp <name/subcommand>` | `/warp market` | Base permission required for all warp commands (and teleporting to a warp by name). | true | `obx.warp` |
| `/warp` (category gate) | Command: /warps, /w, /goto, /go, /travel | `/warp <name>` | `/warp market` | Optional per-category permission gate for warp visibility/teleport. | n/a | `obx.warp.category.<category>` |
| `/warp categories` | Subcommands: cats, groups, sections | `/warp categories` | `/warp categories` | List warp categories with counts. | true | `obx.warp.list` |
| `/warp category` | Subcommands: cat, group, section | `/warp category <name>` | `/warp category shops` | List warps in a category. | true | `obx.warp.list` |
| `/warp delete` | Subcommands: remove, del, clear, reset, unset | `/warp delete <name> [confirm]` | `/warp delete hub confirm` | Delete a warp (confirmation required). | op | `obx.warp.delete` |
| `/warp gui` | Subcommands: menu, open, view | `/warp gui [category]` | `/warp gui shops` | Open the warp GUI (also `/warp` with no args). | true | `obx.warp.gui` |
| `/warp icon` | Subcommands: item, display | `/warp icon <name> [material]` | `/warp icon hub diamond` | Set or clear a warp icon. | op | `obx.warp.icon` |
| `/warp info` | Subcommands: information, details, about, show | `/warp info <name>` | `/warp info market` | Show warp details. | true | `obx.warp.info` |
| `/warp list` | Subcommands: ls, all, browse, page | `/warp list [page] [category]` | `/warp list 2` | List available warps, optionally filtered by category. | true | `obx.warp.list` |
| `/warp list` (hidden) | Subcommands: ls, all, browse, page | `/warp list` | `/warp list` | View hidden warps in lists/GUI and toggle hidden view. | op | `obx.warp.hidden.view` |
| `/warp manage` | `--` | `/warp <manage action>` | `/warp set hub` | Bypass permissions for warp management actions (set/delete/rename/move/icon/public). | op | `obx.warp.manage` |
| `/warp move` | Subcommands: reloc, relocate, update, here | `/warp move <name>` | `/warp move hub` | Move a warp to your current location. | op | `obx.warp.move` |
| `/warp public` | Subcommands: publish, visible, visibility | `/warp public <name> [true/false]` | `/warp public hub false` | Toggle warp visibility (public/hidden). | op | `obx.warp.public` |
| `/warp rename` | Subcommands: ren, name, setname | `/warp rename <old> <new>` | `/warp rename old new` | Rename a warp. | op | `obx.warp.rename` |
| `/warp set` | Subcommands: create, new, setup, add, define | `/warp set <name> [confirm]` | `/warp set hub` | Create or update a warp at your location (confirm to overwrite). | op | `obx.warp.set` |
| `/warp tp` | Subcommands: teleport, go, goto, warp | `/warp tp <name>` | `/warp tp market` | Teleport to a warp (also `/warp <name>`). | true | `obx.warp.tp` |
| `/warp tp` (others) | Subcommands: teleport, go, goto, warp | `/warp tp <name> <player>` | `/warp tp market Notch` | Teleport another player to a warp. | op | `obx.warp.tp.others` |
| `--` | `--` | `--` | `--` | **Hub / Lobby** | `--` | `--` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub jump-to fishing rod (reel-in teleport). | true | `obx.hub.jumprod` |
| `(hub item)` | `--` | `--` | `--` | Receive the launchpad and use double-jump launches. | true | `obx.hub.launchpad` |
| `(hub item)` | `--` | `--` | `--` | Receive the hub server-selector hotbar item and open its GUI. | true | `obx.hub.selector` |
| `(hub item)` | `--` | `--` | `--` | Receive and use the hub players-vanish-all toggle item. | true | `obx.hub.vanishall` |
| `/hub` | Command: /lobby | `/hub` | `/hub` | Teleport to the configured hub world. Default players see only the bare command — sub-commands, tab completion, and admin args are hidden from them. | true | `obx.hub.use` |
| `/hub give` | Subcommand | `/hub give [player]` | `/hub give Notch` | Force-apply the hub kit to yourself or another player (bypasses world check). Hidden from default players. | op | `obx.hub.admin` |
| `/hub menu` | Subcommand | `/hub menu` | `/hub menu` | Open the in-game admin hub panel directly. Hidden from default players. | op | `obx.hub.admin` |
| `/hub off` | Subcommand | `/hub off` | `/hub off` | Disable hub mode (system goes dormant). Hidden from default players. | op | `obx.hub.admin` |
| `/hub on` | Subcommand | `/hub on` | `/hub on` | Enable hub mode (saves to `systems/hub.yml`; applies the kit to every player in a hub world). Hidden from default players. | op | `obx.hub.admin` |
| `/hub reload` | Subcommand | `/hub reload` | `/hub reload` | Re-read `systems/hub.yml` (useful after a server-panel save). Hidden from default players. | op | `obx.hub.admin` |
| `/hub selector` | Subcommand | `/hub selector` | `/hub selector` | Open the live server-selector GUI. Hidden from default players. | op | `obx.hub.admin` |
| `/hub toggle` | Subcommand | `/hub toggle` | `/hub toggle` | Flip hub mode on/off. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world add` | Subcommand | `/hub world add <world>` | `/hub world add lobby` | Add a world to the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world here` | Subcommand | `/hub world here` | `/hub world here` | Add the world you're currently in to the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world list` | Subcommand | `/hub world list` | `/hub world list` | List configured hub worlds. Hidden from default players. | op | `obx.hub.admin` |
| `/hub world remove` | Subcommand | `/hub world remove <world>` | `/hub world remove lobby` | Remove a world from the hub-mode whitelist. Hidden from default players. | op | `obx.hub.admin` |

## Utility

Virtual workstations and self/other quality-of-life commands. All op-gated.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/afk` | Command: /away | `/afk` | `/afk` | Toggle your own AFK status. | true | `obx.afk` |
| `/afk` (others) | Command: /away | `/afk <player>` | `/afk Notch` | Toggle AFK status on another player. | op | `obx.afk.others` |
| `/afk` (exempt) | `--` | `--` | `--` | Exempts the holder from auto-AFK after the idle timeout. | false | `obx.afk.exempt` |
| `/afk` (exempt-kick) | `--` | `--` | `--` | Exempts the holder from the AFK idle auto-kick. | false | `obx.afk.exempt-kick` |
| `/anvil` | Command: /forge | `/anvil` | `/anvil` | Open a virtual anvil (requires Spigot 1.14+). | op | `obx.anvil` |
| `/cartography` | Command: /ctable, /cartograph | `/cartography` | `/cartography` | Open a virtual cartography table (requires Spigot 1.14+). | op | `obx.cartography` |
| `/clearinv` | Command: /ci, /clearinventory | `/clearinv` | `/clearinv` | Clear your own inventory. | op | `obx.clearinv` |
| `/clearinv` (others) | Command: /ci, /clearinventory | `/clearinv <player> [item] [maxCount]` | `/clearinv Notch` | Clear another player's inventory. | op | `obx.clearinv.others` |
| `/craft` | Command: /workbench, /crafting | `/craft` | `/craft` | Open a virtual 3x3 crafting table at your location. | op | `obx.craft` |
| `/enchant` | Command: /enchanting, /enchanttable | `/enchant` | `/enchant` | Open a virtual enchanting table forced to maximum power. | op | `obx.enchant` |
| `/feed` | `--` | `/feed` | `/feed` | Restore your hunger bar (self only). | op | `obx.feed` |
| `/flyspeed` | `--` | `/flyspeed <speed>` | `/flyspeed 5` | Set your own fly speed. | op | `obx.flyspeed` |
| `/flyspeed` (others) | `--` | `/flyspeed <player> <speed>` | `/flyspeed Notch 5` | Set another player's fly speed. | op | `obx.flyspeed.others` |
| `/god` | Command: /godmode, /invincible, /immortal | `/god [player]` | `/god Notch` | Toggle complete invincibility for yourself or a target. | op | `obx.god` |
| `/grindstone` | Command: /gstone, /grind, /gs | `/grindstone` | `/grindstone` | Open a virtual grindstone (requires Spigot 1.14+). | op | `obx.grindstone` |
| `/heal` | `--` | `/heal` | `/heal` | Restore your health to full (self only). | op | `obx.heal` |
| `/loom` | `--` | `/loom` | `/loom` | Open a virtual loom (requires Spigot 1.14+). | op | `obx.loom` |
| `/map` | `--` | `/map` | `/map` | Open a "you are here" map of your surroundings, placed into your hand at closest scale. | op | `obx.map` |
| `/research` | Command: /discover, /itemprofile, /iteminfo | `/research [item]` | `/research Diamond Sword` | Display detailed information about a Minecraft item. | op | `obx.research` |
| `/smith` | Command: /smithing, /smithtable | `/smith` | `/smith` | Open a virtual smithing table (requires 1.16+; full functionality on Paper 1.19+). | op | `obx.smith` |
| `/stonecut` | Command: /chop, /cut, /scut | `/stonecut` | `/stonecut` | Open a virtual stonecutter (requires Spigot 1.14+). | op | `obx.stonecut` |
| `/vital` | Command: /restore, /regen | `/vital [player]` | `/vital Notch` | Restore health and hunger for yourself or a target. | op | `obx.vital` |
| `--` | `--` | `--` | `--` | **Gamemode** | `--` | `--` |
| `/gamemode` | Command: /gm, /gmode, /mode, /gms, /gmc, /gma, /gmsp; Modes: survival/s/0, creative/c/1, adventure/a/2, spectator/sp/3 | `/gamemode <mode> [player]` | `/gamemode creative` | Change your or another player's gamemode (requires mode-specific nodes). | op | `obx.gamemode` |
| `/gamemode` (self base) | `--` | `/gamemode <mode>` | `/gamemode survival` | Base self permission used if per-mode nodes are not set. | op | `obx.gamemode.self` |
| `/gamemode` (others base) | `--` | `/gamemode <mode> <player>` | `/gamemode creative Notch` | Base others permission used if per-mode nodes are not set. | op | `obx.gamemode.others` |
| `/gamemode adventure` | `--` | `/gamemode adventure` | `/gamemode adventure` | Allow setting your own gamemode to adventure. | op | `obx.gamemode.self.adventure` |
| `/gamemode adventure` (others) | `--` | `/gamemode adventure <player>` | `/gamemode adventure Notch` | Allow setting another player's gamemode to adventure. | op | `obx.gamemode.others.adventure` |
| `/gamemode creative` | `--` | `/gamemode creative` | `/gamemode creative` | Allow setting your own gamemode to creative. | op | `obx.gamemode.self.creative` |
| `/gamemode creative` (others) | `--` | `/gamemode creative <player>` | `/gamemode creative Notch` | Allow setting another player's gamemode to creative. | op | `obx.gamemode.others.creative` |
| `/gamemode spectator` | `--` | `/gamemode spectator` | `/gamemode spectator` | Allow setting your own gamemode to spectator. | op | `obx.gamemode.self.spectator` |
| `/gamemode spectator` (others) | `--` | `/gamemode spectator <player>` | `/gamemode spectator Notch` | Allow setting another player's gamemode to spectator. | op | `obx.gamemode.others.spectator` |
| `/gamemode survival` | `--` | `/gamemode survival` | `/gamemode survival` | Allow setting your own gamemode to survival. | op | `obx.gamemode.self.survival` |
| `/gamemode survival` (others) | `--` | `/gamemode survival <player>` | `/gamemode survival Notch` | Allow setting another player's gamemode to survival. | op | `obx.gamemode.others.survival` |

## Other

The Arcanum custom-enchantment module, the holograms module, and the permission
wildcard/bundle nodes. These surface under **Other** in the `/help` GUI.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — players** | `--` | `--` |
| `(scroll use)` | `--` | `--` | `--` | Base permission to use Arcanum scrolls (anvil + drag-and-drop application); also gates `/recall` and `/satchel`. | true | `obx.enchants.use` |
| `/enchants` | Command: /scrolls | `/enchants` | `/enchants` | Open the read-only Arcanum enchantment browser GUI. | true | `obx.enchants.browse` |
| `/enchants settings` | Command: /scrolls | `/enchants settings` | `/enchants settings` | Toggle your personal combat effects (kill banners + combat action-bar feedback). Persists per player. | true | none (any player) |
| `/recall` | `--` | `/recall` | `/recall` | Teleport to your Beacon's Memory recall point (set by the Beacon's Memory enchantment). | true | `obx.enchants.use` |
| `/satchel` | `--` | `/satchel` | `/satchel` | Open your Satchel personal storage (granted by the Satchel enchantment). | true | `obx.enchants.use` |
| `--` | `--` | `--` | `--` | **Enchantments (Arcanum) — admin** | `--` | `--` |
| `/obxench` | Command: /obxenchant, /obxe | `/obxench <subcommand>` | `/obxench list combat` | Arcanum admin command. The base command needs no permission — every action is gated by its per-subcommand node below. | any | `none` (per-subcommand) |
| `/obxench admin` | Command: /obxenchant, /obxe | `/obxench admin` | `/obxench admin` | Open the Arcanum admin GUI (browse, give, apply with full access). | op | `obx.enchants.admin` |
| `/obxench apply` | Command: /obxenchant, /obxe | `/obxench apply <enchant> <level>` | `/obxench apply frostbite 3` | Apply an Arcanum enchantment to the item in your main hand. | op | `obx.enchants.apply` |
| `/obxench bookapply` | Command: /obxenchant, /obxe | `/obxench bookapply <enchant> <level>` | *(internal — Codex level click)* | Apply an enchant to the held item; only raises to a higher level. Driven by clicking a level in a Codex book. | op | `obx.enchants.book` |
| `/obxench bookinfo` | Command: /obxenchant, /obxe | `/obxench bookinfo <enchant>` | *(internal — Codex name click)* | Print an enchant's full details in chat. Driven by clicking an enchant name in a Codex book. | op | `obx.enchants.book` |
| `/obxench debug` | Command: /obxenchant, /obxe | `/obxench debug [on/off]` | `/obxench debug on` | Toggle per-admin Arcanum proc-debug logging for yourself. | op | `obx.enchants.debug` |
| `/obxench give` | Command: /obxenchant, /obxe | `/obxench give <player> <enchant> <level> [amount]` | `/obxench give Notch frostbite 3` | Give an enchantment scroll to a player. | op | `obx.enchants.give` |
| `/obxench give … book` | Command: /obxenchant, /obxe | `/obxench give <player> book <category>` | `/obxench give Notch book combat` | Give a stylized interactive **Codex** guide book for a category. Executor needs `obx.enchants.give`; the recipient must hold `obx.enchants.book` (or be op). | op | `obx.enchants.give` (recipient: `obx.enchants.book`) |
| `/obxench givebook` | Command: /obxenchant, /obxe | `/obxench givebook <player> <enchant> <level> [amount]` | `/obxench givebook Notch frostbite 3` | Give the enchanted-book form to a player. | op | `obx.enchants.give` |
| `/obxench info` | Command: /obxenchant, /obxe | `/obxench info <enchant>` | `/obxench info frostbite` | Show full details for a specific enchantment. | op | `obx.enchants.list` |
| `/obxench list` | Command: /obxenchant, /obxe | `/obxench list [category]` | `/obxench list combat` | List Arcanum enchantments, optionally filtered by category. | op | `obx.enchants.list` |
| `/obxench loot` | Command: /obxenchant, /obxe | `/obxench loot <reload \| toggle <chest>>` | `/obxench loot reload` | Reload world-loot config, or toggle scroll generation for a chest type. | op | `obx.enchants.loot` |
| `/obxench protect` | Command: /obxenchant, /obxe | `/obxench protect <player> [amount]` | `/obxench protect Notch 2` | Give Protection scroll(s) to a player. | op | `obx.enchants.give` |
| `/obxench reload` | Command: /obxenchant, /obxe | `/obxench reload` | `/obxench reload` | Reload the Arcanum config and category/roster files. | op | `obx.enchants.reload` |
| `/obxench remove` | Command: /obxenchant, /obxe | `/obxench remove <enchant>` | `/obxench remove frostbite` | Remove an Arcanum enchantment from the item in your main hand. | op | `obx.enchants.apply` |
| `/obxench success` | Command: /obxenchant, /obxe | `/obxench success <player> [amount]` | `/obxench success Notch 2` | Give Success scroll(s) to a player. | op | `obx.enchants.give` |
| `--` | `--` | `--` | `--` | **Holograms — admin** | `--` | `--` |
| `/holo` | Command: /hg, /hologram | `/holo <subcommand>` | `/holo list` | Root command for the holograms module (enabled by default). The base command needs `obx.holo.use`; each subcommand is gated below. | true | `obx.holo.use` |
| `/holo aim` | Command: /hg, /hologram | `/holo aim` | `/holo aim` | Resolve the hologram closest to your look direction (within 12 blocks). | op | `obx.holo.edit` |
| `/holo alignment` | Command: /hg, /hologram | `/holo alignment <id> <CENTER\|LEFT\|RIGHT>` | `/holo alignment welcome LEFT` | Set text alignment within the line. | op | `obx.holo.edit` |
| `/holo background` | Command: /hg, /hologram | `/holo background <id> <ARGB\|transparent\|none>` | `/holo background welcome 80FF0000` | Set the text background colour. | op | `obx.holo.edit` |
| `/holo billboard` | Command: /hg, /hologram | `/holo billboard <id> <FIXED\|VERTICAL\|HORIZONTAL\|CENTER>` | `/holo billboard welcome CENTER` | Set the billboard mode. | op | `obx.holo.edit` |
| `/holo board` | Command: /hg, /hologram | `/holo board <id> <…>` | `/holo board welcome enable true` | Configure the block-display slab backing. | op | `obx.holo.edit` |
| `/holo copy` | Command: /hg, /hologram | `/holo copy <src> <dest>` | `/holo copy welcome welcome2` | Duplicate a hologram with all lines and settings, placed at you. | op | `obx.holo.create` |
| `/holo create` | Command: /hg, /hologram | `/holo create <id> [text]` | `/holo create welcome &6Welcome` | Create a new hologram at your location with an optional first line. Persists to `holograms.yml`. | op | `obx.holo.create` |
| `/holo debug` | Command: /hg, /hologram | `/holo debug [id]` | `/holo debug` | Spawn / toggle a built-in test hologram showing backend identity. | op | `obx.holo.admin` |
| `/holo delete` | Command: /hg, /hologram | `/holo delete <id>` | `/holo delete welcome` | Despawn and remove a hologram from disk. | op | `obx.holo.delete` |
| `/holo disable` | Command: /hg, /hologram | `/holo disable` | `/holo disable` | Flip the master flag to `false` and tear down live entities. | op | `obx.holo.admin` |
| `/holo doublesided` | Command: /hg, /hologram | `/holo doublesided <id> <true\|false>` | `/holo doublesided welcome false` | Toggle front/back culling (false → only the front is visible). | op | `obx.holo.edit` |
| `/holo enable` | Command: /hg, /hologram | `/holo enable` | `/holo enable` | Flip the master flag in `systems/holograms.yml → enabled: true` and reload. | op | `obx.holo.admin` |
| `/holo info` | Command: /hg, /hologram | `/holo info [id]` | `/holo info welcome` | Show module info (no id) or full settings + lines for a single hologram. | op | `obx.holo.info` |
| `/holo line add` | Command: /hg, /hologram | `/holo line add <id> <text…\|icon <mat>\|block <mat>>` | `/holo line add welcome &eHello` | Append a line. Use `icon <material>` or `block <material>` for non-text lines. | op | `obx.holo.edit` |
| `/holo line insertafter` | Command: /hg, /hologram | `/holo line insertafter <id> <index> <value…>` | `/holo line insertafter welcome 1 &7Inserted` | Insert after the given line. Alias: `after`. | op | `obx.holo.edit` |
| `/holo line insertbefore` | Command: /hg, /hologram | `/holo line insertbefore <id> <index> <value…>` | `/holo line insertbefore welcome 2 &7Inserted` | Insert before the given line. Alias: `before`. | op | `obx.holo.edit` |
| `/holo line remove` | Command: /hg, /hologram | `/holo line remove <id> <index>` | `/holo line remove welcome 2` | Remove the line at 1-based index. Alias: `delete`. | op | `obx.holo.edit` |
| `/holo line set` | Command: /hg, /hologram | `/holo line set <id> <index> <value…>` | `/holo line set welcome 1 &fNew` | Replace the line at 1-based index. | op | `obx.holo.edit` |
| `/holo line swap` | Command: /hg, /hologram | `/holo line swap <id> <a> <b>` | `/holo line swap welcome 1 3` | Swap two lines. | op | `obx.holo.edit` |
| `/holo list` | Command: /hg, /hologram | `/holo list` | `/holo list` | List every registered hologram with world / coordinates / line count. | op | `obx.holo.list` |
| `/holo move` | Command: /hg, /hologram | `/holo move <id> <x> <y> <z> [yaw] [world]` | `/holo move welcome 0 65 0 0 world` | Reposition a hologram to explicit coordinates. | op | `obx.holo.edit` |
| `/holo movehere` | Command: /hg, /hologram | `/holo movehere <id>` | `/holo movehere welcome` | Move a hologram to your current location (+2y). | op | `obx.holo.edit` |
| `/holo reload` | Command: /hg, /hologram | `/holo reload` | `/holo reload` | Reload `systems/holograms.yml` and re-spawn every hologram. | op | `obx.holo.admin` |
| `/holo scale` | Command: /hg, /hologram | `/holo scale <id> <factor>` | `/holo scale welcome 1.5` | Set the hologram scale factor (0.05–20.0). | op | `obx.holo.edit` |
| `/holo seethrough` | Command: /hg, /hologram | `/holo seethrough <id> <true\|false>` | `/holo seethrough welcome true` | Toggle see-through-walls rendering. | op | `obx.holo.edit` |
| `/holo shadow` | Command: /hg, /hologram | `/holo shadow <id> <true\|false>` | `/holo shadow welcome true` | Toggle drop shadow on text. | op | `obx.holo.edit` |
| `/holo showrange` | Command: /hg, /hologram | `/holo showrange <id> <blocks>` | `/holo showrange welcome 32` | Visibility distance in blocks. | op | `obx.holo.edit` |
| `/holo textalpha` | Command: /hg, /hologram | `/holo textalpha <id> <0-255>` | `/holo textalpha welcome 200` | Set text opacity. | op | `obx.holo.edit` |
| `/holo tp` | Command: /hg, /hologram | `/holo tp <id>` | `/holo tp welcome` | Teleport to a hologram's stored location. | op | `obx.holo.tp` |
| `/holo updaterange` | Command: /hg, /hologram | `/holo updaterange <id> <blocks>` | `/holo updaterange welcome 48` | Per-viewer text refresh distance (≥ showrange). | op | `obx.holo.edit` |
| `--` | `--` | `--` | `--` | **Wildcards &amp; Bundles** | `--` | `--` |
| `obx.*` | `--` | `--` | `--` | Master wildcard — grants every OBX permission. | op | `obx.*` |
| `obx.admin.*` | `--` | `--` | `--` | Bundle of admin-level OBX actions (reload/diagnostics/health/menu). | op | `obx.admin.*` |
| `obx.ah.*` | `--` | `--` | `--` | Full auction house access (use/buy + sell + admin cancel). | true | `obx.ah.*` |
| `obx.backpack.*` | `--` | `--` | `--` | Full backpack access (use + convert + respawn). | true | `obx.backpack.*` |
| `obx.sell.multiplier.<n>` | `--` | `--` | `--` | Rank sell bonus — the highest granted `<n>` (e.g. `1.5`) multiplies all sale payouts (clamped 0.1–10). | false | `obx.sell.multiplier.1.5` |
| `obx.payday` | `--` | `--` | `--` | Receives the scheduled salary when `economy.payday.enabled` (paid every `interval-minutes`). | false | `obx.payday` |
| `obx.shop.*` | `--` | `--` | `--` | Full shop access (browse/buy + sell inventory + admin reload). | op | `obx.shop.*` |
| `obx.debug.*` | `--` | `--` | `--` | Bundle for config and debug permissions. | op | `obx.debug.*` |
| `obx.enchants.*` | `--` | `--` | `--` | Full Arcanum module (admin/give/apply/list/reload/loot/debug/use/browse/book). | op | `obx.enchants.*` |
| `obx.holo.*` | `--` | `--` | `--` | Full hologram module (create/edit/delete/admin). | op | `obx.holo.*` |
| `obx.hub.*` | `--` | `--` | `--` | Every hub / lobby permission (use + all hotbar items + admin). | op | `obx.hub.*` |
| `obx.invsee.*` | `--` | `--` | `--` | Both inventory-see tiers (basic + full). | op | `obx.invsee.*` |
| `obx.moderation.*` | `--` | `--` | `--` | All moderation actions, including banlist access. | op | `obx.moderation.*` |
| `obx.spawn.*` | `--` | `--` | `--` | All spawn permissions. | op | `obx.spawn.*` |
| `obx.staff.*` | `--` | `--` | `--` | The staff overview menu and its actions. | op | `obx.staff.*` |
| `obx.updates.*` | `--` | `--` | `--` | Update and version permissions. | op | `obx.updates.*` |
| `obx.vanish.*` | `--` | `--` | `--` | All vanish tiers (self + others + admin). | op | `obx.vanish.*` |
| `obx.warp.*` | `--` | `--` | `--` | All warp permissions. | op | `obx.warp.*` |

## Economy

Vault-compatible economy: balances, transfers, item selling, a category shop, banknotes, an interest bank, and a player auction house. Player-facing by default; `/eco` is staff-only.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/balance` | Command: /bal, /money | `/balance [player]` | `/balance Notch` | Show your or another player's balance. | true | `obx.balance` |
| `/baltop` | Command: /balancetop, /moneytop | `/baltop [page]` | `/baltop 1` | List the wealthiest players. | true | `obx.baltop` |
| `/eco` | Command: /economy | `/eco <give\|take\|set\|reset\|log> <player> [amount]` | `/eco give Notch 16` | Manage player balances (staff). **Works for offline players** (resolved from the economy database, name-change safe). Acting on a never-seen name **creates the account seeded at $0** (so `/eco give X` yields exactly X). Every action is written to the `economy_log` **audit trail**; `/eco log [player\|*] [page] [action]` shows the newest entries, 10/page, optionally filtered to one action type (GIVE/TAKE/SET/PAY/SELL/SHOP_BUY/BANK_*/AH_*/…). Retention is `economy.log-retention-days` (default 90). | op | `obx.eco` |
| `/pay` | `--` | `/pay <player> <amount>` · `/pay confirm` | `/pay Notch 100` | Pay another player from your balance (both sides audit-logged). Payments at/above `economy.pay.confirm-threshold` require **`/pay confirm`** within 30s (anti fat-finger). Optional transfer tax `economy.pay.tax-percent` is burned as a money sink. | true | `obx.pay` |
| `/sell` | `--` | `/sell <hand\|all\|material>` | `/sell hand` | Sell items from your inventory (audit-logged; prices from `worth.yml`, a **version-aware item database spanning 1.8.8 → latest** incl. legacy name aliases). Refuses (never clamps) when the wallet is at the cap; honours the optional per-player daily cap `economy.sell.daily-cap` and rank multipliers `obx.sell.multiplier.<n>`. | true | `obx.sell` |
| `/sellall` | `--` | `/sellall` | `/sellall` | Sell every saleable item in your inventory (thin alias of `/sell all` — same guards). | true | `obx.sellall` |
| `/shop` | Command: /market | `/shop [category\|sell\|reload]` | `/shop ores` | **Categorized buy/sell shop GUI** driven by `shop.yml` + `shops/*.yml` (bundled: blocks, ores, farming, food, mobdrops, redstone — every material version-checked so one catalog serves 1.8.8 → latest). Paginated: left-click buys a bundle, shift-left a stack, right-click sells a bundle, shift-right everything carried; live balance card. Optional **finite stock** per item (`stock:` + `restock-minutes`) and **dynamic pricing** (`dynamic-pricing.*`, sell side capped at base to block self-pump arbitrage). `sell` opens a dump inventory; `reload` re-reads the YAML (`obx.shop.admin`). All trades audit-logged. | true | `obx.shop` (+ `.sell`, `.admin`) |
| `/worth` | Command: /value | `/worth [amount]` | `/worth 16` | Look up the sell value of the item in your hand. | true | `obx.worth` |
| `/withdraw` | Command: /banknote, /cheque | `/withdraw <amount>` | `/withdraw 500` | Convert wallet money into a signed, single-use **banknote** paper item; right-click to redeem. Dupe-proof: the value lives in SQLite keyed by a random token, so cloned copies become worthless once any one is cashed. | true | `obx.withdraw` |
| `/bank` | `--` | `/bank [balance\|deposit\|withdraw] [amount]` | `/bank deposit 1000` | A second, **interest-bearing** balance. Interest accrues lazily (compounded daily, offline-safe) per `economy.bank.interest-percent-daily`, capped at `economy.bank.max-balance`. Wallet⇄bank moves are atomic and audit-logged. | true | `obx.bank` |
| `/ah` | Command: /auction, /auctionhouse | `/ah [sell <price>\|mine\|claim]` | `/ah sell 250` | Player **auction house** GUI. `sell` lists the held item; browse to buy; `mine` manages your listings; `claim` collects sold proceeds and expired/cancelled items from your returns ledger. Race-safe (one buyer per listing), money-before-goods with refunds. Config: `economy.auction.*` (max-listings, duration-hours, listing-fee, tax-percent). | true | `obx.ah` (+ `.sell`, `.admin`) |

## Kits

Configurable item kits with cooldowns and an optional auto-granted first-join kit (`kits.yml → first-join.enabled`).

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/kit` | Command: /kits | `/kit [name\|list\|info\|give\|reload]` | `/kit starter` | Claim or manage kits. Refuses with an error if your inventory is full (so the cooldown isn't wasted). Kits flagged `first-join: true` (e.g. the starter kit) are granted **once on first join only** and cannot be self-claimed afterwards — only staff can re-issue them via `/kit give`. | true | `obx.kit` |

## Nickname

Display-name customization. Color codes require `obx.nick.color`; `obx.nick.others` edits other players.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/nick` | Command: /nickname | `/nick <name\|off> [player]` | `/nick KingNotch` | Set your or another player's nickname (multi-word supported; impersonation of real/known names is blocked). | op | `obx.nick` |
| `/realname` | `--` | `/realname <displayname>` | `/realname KingNotch` | Look up the real name behind a display name. | true | `obx.realname` |

## Player Info

Read-only player lookups: playtime, seen / first-seen, nearby players, and profile cards.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/firstseen` | `--` | `/firstseen <player>` | `/firstseen Notch` | Show when a player first joined. | true | `obx.firstseen` |
| `/info` | Command: /profile | `/info <player>` | `/info Notch` | Show a player profile card with quick actions. | op | `obx.info.player` |
| `/near` | Command: /nearby | `/near [radius]` | `/near 50` | List players within range. | true | `obx.near` |
| `/playtime` | Command: /pt, /played | `/playtime [player]` | `/playtime Notch` | Show a player's **total**, current **session**, and **longest** recorded session (full y/mo/d/h/m/s, with a clean date/time for the longest session — same-day shows just the time). Works for online **or** offline players; target name appears in the box header. | true | `obx.playtime` |
| `/seen` | Command: /lastseen | `/seen <player>` | `/seen Notch` | Show when a player was last seen online. | true | `obx.seen` |
| `/topplaytime` | Command: /playtimetop, /topplay, /pttop | `/topplaytime` | `/topplaytime` | Box-style leaderboard of the top 10 players by total playtime (medal-ranked, with your own total below). | true | `obx.topplaytime` |
| `/whois` | `--` | `/whois <player>` | `/whois Notch` | Show a player's detailed profile snapshot. | op | `obx.whois` |

## Items &amp; Inventory

Item spawning, editing, and inventory utilities. Most are op-gated; `/disposal` is open to players.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/backpack` | Command: /bp, /pack | `/backpack [convert\|respawn\|virtual]` | `/backpack` | Open your personal **3-row (27-slot) portable backpack** (contents persist in the database). `convert` turns it into a physical item (version-appropriate material: bundle 1.17+, shulker box 1.11+, chest 1.8.8+) that opens on right-click; `respawn` re-issues a lost/destroyed item — every older copy becomes **void** (token dupe-guard), so neither the backpack nor its items can be duplicated; `virtual` switches back to command-opened storage. The item can't be placed or nested inside the backpack. | true | `obx.backpack.use` (`convert`/`respawn` sub-nodes) |
| `/book` | `--` | `/book [new\|unsign\|copy]` | `/book new` | Get a writable book, unsign, or copy held book. | op | `obx.book` |
| `/disposal` | Command: /trash | `/disposal` | `/disposal` | Open a trash inventory; items disappear when closed. | true | `obx.disposal` |
| `/enderchest` | Command: /ec | `/enderchest [player]` | `/enderchest Notch` | Open an ender chest, optionally another player's. | op | `obx.enderchest` |
| `/give` | `--` | `/give <player> <material> [amount]` | `/give Notch DIAMOND 16` | Give a material stack to a player. | op | `obx.give` |
| `/hat` | Command: /head-on | `/hat` | `/hat` | Put the held item on your head. | op | `obx.hat` |
| `/i` | Command: /item | `/i <material> [amount]` | `/i DIAMOND 16` | Give yourself a material stack. | op | `obx.item` |
| `/itemlore` | Command: /lore | `/itemlore <add\|set\|clear> [index] <text>` | `/itemlore add Legendary` | Add, set, or clear lore on the held item. | op | `obx.itemlore` |
| `/itemname` | Command: /rename | `/itemname <name\|clear>` | `/itemname Excalibur` | Rename the item in your hand. | op | `obx.itemname` |
| `/more` | Command: /stack, /max | `/more` | `/more` | Fill the held stack to its maximum size. | op | `obx.more` |
| `/repair` | Command: /fix | `/repair [all]` | `/repair all` | Repair the item in your hand or every repairable item. | op | `obx.repair` |
| `/skull` | Command: /head | `/skull <player>` | `/skull Notch` | Give yourself a player head. | op | `obx.skull` |
| `/unbreakable` | Command: /unb | `/unbreakable` | `/unbreakable` | Toggle unbreakable on the held item. | op | `obx.unbreakable` |

## World &amp; Environment

World time / weather control, per-player client overrides, and entity utilities.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/butcher` | Command: /killmobs | `/butcher [radius] [type]` | `/butcher 50` | Kill nearby mobs. | op | `obx.butcher` |
| `/day` | `--` | `/day [world]` | `/day` | Set a world's time to morning. | op | `obx.time` |
| `/night` | `--` | `/night [world]` | `/night` | Set a world's time to night. | op | `obx.time` |
| `/ptime` | `--` | `/ptime <time\|reset>` | `/ptime night` | Set or reset your personal time override. Box-style confirmation with a clickable `[Morning] [Noon] [Night] [Midnight] [reset]` button row. | true | `obx.ptime` |
| `/pweather` | `--` | `/pweather <clear\|rain\|thunder\|reset>` | `/pweather clear` | Set or reset your personal weather override. | true | `obx.pweather` |
| `/smite` | Command: /lightning | `/smite [player]` | `/smite Notch` | Strike lightning at a player or your crosshair. | op | `obx.smite` |
| `/spawner` | Command: /setspawner | `/spawner <type>` | `/spawner zombie` | Change the spawned type of the spawner you are looking at. | op | `obx.spawner` |
| `/spawnmob` | Command: /mob | `/spawnmob <type> [count]` | `/spawnmob cow 5` | Spawn mobs at your crosshair. | op | `obx.spawnmob` |
| `/sun` | `--` | `/sun [world]` | `/sun` | Clear weather and set a world's time to noon. | op | `obx.weather` |
| `/time` | `--` | `/time <set\|add> <value> [world]` | `/time set 1000 world` | Set or add to the world's time. | op | `obx.time` |
| `/tree` | `--` | `/tree [type]` | `/tree oak` | Generate a tree at your crosshair. | op | `obx.tree` |
| `/weather` | `--` | `/weather <clear\|rain\|thunder> [world]` | `/weather clear world` | Change the weather. | op | `obx.weather` |

## Chat &amp; Staff

Action messages, broadcasts, and staff-only communication / oversight tools.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/broadcast` | Command: /bc | `/broadcast <message>` | `/broadcast hi` | Broadcast a message to all online players. | op | `obx.broadcast` |
| `/freeze` | Command: /frz | `/freeze <player>` | `/freeze Notch` | Toggle a player's freeze state. | op | `obx.freeze` |
| `/me` | Command: /action | `/me <action>` | `/me waves` | Broadcast an action-message in third person. | true | `obx.me` |
| `/socialspy` | Command: /spy | `/socialspy` | `/socialspy` | Toggle staff visibility of all private messages. | op | `obx.socialspy` |
| `/staffchat` | Command: /sc, /achat | `/sc` (toggle) · `/sc <message>` (send) | `/sc hi` | Bare `/sc` toggles staff-chat mode (your normal chat routes to staff chat); `/sc <message>` sends a single line either way. `/rply` answers a staff-chat message back into staff chat. | op | `obx.staffchat` |

**Passive permission nodes** (no command of their own):

| Permission node | Default | Grants |
| -------------------- | -------------------- | -------------------- |
| `obx.chat.staff` | op | Applies the staff prefix/styling to your public chat (replaces the old `isOp()` check). |
| `obx.message.color` | op | Lets your messages keep `&`/`§` color &amp; format codes in `/msg`, `/me`, `/broadcast`, `/staffchat`, and mail; without it, codes are stripped. |
| `obx.message.ignore.bypass` | op | Lets your private messages reach a player who has `/ignore`d you. |

## Jail

Confine players to a jail anchor for a duration. `/jailtime` lets a jailed player check their remaining time.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/deljail` | Command: /jaildel | `/deljail <name>` | `/deljail home` | Delete a jail anchor. | op | `obx.deljail` |
| `/jail` | `--` | `/jail <player> <jail> [duration] [reason]` | `/jail Notch spawn 1d spam` | Jail a player (movement-contained to the jail area). | op | `obx.jail` |
| `/jails` | Command: /jaillist | `/jails` | `/jails` | List configured jail anchors. | op | `obx.jails` |
| `/jailtime` | `--` | `/jailtime [player]` | `/jailtime Notch` | Show remaining jail time. | true | `obx.jailtime` |
| `/setjail` | Command: /jailset | `/setjail <name>` | `/setjail home` | Create a jail anchor at your current location. | op | `obx.setjail` |
| `/unjail` | `--` | `/unjail <player>` | `/unjail Notch` | Release a jailed player. | op | `obx.unjail` |

## Flight &amp; Movement

Flight and movement-speed toggles.

| Command | Aliases | Usage (arguments) | Example usage | Description | Default level | Permission node |
| -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- | -------------------- |
| `/fly` | Command: /flight | `/fly [player]` | `/fly Notch` | Toggle flight for yourself or another player. | op | `obx.fly` |
| `/walkspeed` | Command: /wspeed | `/walkspeed <0-10>` | `/walkspeed 3` | Set your walk speed (0–10). | op | `obx.walkspeed` |
