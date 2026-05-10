# Commands & Permissions

## Information
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /sf help [page\|category\|command] | sfcore.help | true | Shows paginated help with category and command filters. |
| /sf info | sfcore.info | true | Displays plugin name, description, author, and website. |
| /sf about | sfcore.about | true | Shows extended plugin info, credits, and links. |
| /sf permissions [command\|category] | sfcore.permissions.view | false | Lists permission nodes for a specific command or category. |
| /sf commands [category] | sfcore.commands.list | true | Lists available commands filtered by category and your permissions. |

## Reload & Diagnostics
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /sf reload | sfcore.admin.reload | op | Reloads SF-Core configs and reinitializes feature modules. |
| /sf reload config | sfcore.admin.reload.config | op | Reloads configuration files only (`config.yml` + messages/features). |
| /sf reload <file> | sfcore.admin.reload.features | op | Reloads a specific file from the plugin data folder. |
| /sf diagnostics | sfcore.admin.diagnostics | op | Runs a quick health check (config status, loaded modules, platform info). |
| /sf diagnostics full | sfcore.admin.diagnostics.full | op | Outputs extended diagnostics including services, hooks, and errors. |

## Updates & Version
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /sf version | sfcore.version | op | Shows current SF-Core version and build tag. |
| /sf updates | sfcore.updates.check | op | Checks for available updates (placeholder output). |
| /sf updates check | sfcore.updates.check | op | Forces an update check (placeholder). |
| /sf updates notify | sfcore.updates.notify | op | Toggles update notifications for the executor. |

## Config & Debug
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /sf config | sfcore.debug.config | op | Displays loaded config files and their status. |
| /sf config validate | sfcore.debug.config.validate | op | Validates config files and reports errors or deprecated keys. |
| /sf debug | sfcore.debug | op | Shows debug status and active flags. |
| /sf debug enable | sfcore.debug.toggle | op | Enables debug logging temporarily. |
| /sf debug disable | sfcore.debug.toggle | op | Disables debug logging. |
| /sf debug dump | sfcore.debug.dump | op | Dumps internal state to `/plugins/SF-Core/logs/<date>---<player>.yml`. |

## Menus
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /sf (player) | sfcore.admin.menu (admin view) | op | Opens Admin Menu if permitted, otherwise opens the player Main Menu. |

## Admin / Moderation
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /ban <player> [reason] | sfcore.moderation.ban | op | Permanently ban a player and log the action to the moderation audit/Discord webhook. |
| /banlist | sfcore.moderation.banlist | op | Show all active bans, including temporary bans that have not expired yet. |
| /kill | sfcore.kill | op | Toggle kill mode; left-click to kill the entity in your crosshairs. |
| /kick <player> [reason] | sfcore.moderation.kick | op | Kick an online player and write the event to the moderation log pipeline. |
| /mute <player> [reason] | sfcore.moderation.mute | op | Block a player from chatting until they are unmuted. |
| /status <player> | sfcore.moderation.status | op | View a player's moderation profile card, current punishments, counts, and recent action history. |
| /tempban <player> [reason] | sfcore.moderation.tempban | op | Temporarily ban a player using the configured default duration in `config.yml` (`moderation.defaults.tempban-duration`). |
| /unban <player> [reason] | sfcore.moderation.unban | op | Remove an active ban and log the pardon event. |
| /unmute <player> [reason] | sfcore.moderation.unmute | op | Remove a chat mute from a player profile. |
| /warn <player> [reason] | sfcore.moderation.warn | op | Add a stored warning entry for a player profile and log it externally. |

## Teleportation
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /home [name] | sfcore.home | true | Teleport to one of your homes. |
| /sethome [name] | sfcore.home.set | true | Set a named home at your current location. |
| /delhome <name> | sfcore.home.delete | true | Delete one of your homes. |
| /homes | sfcore.home.list | true | List your homes. |
| /spawn | sfcore.spawn | true | Teleport to server spawn. |
| /setspawn | sfcore.spawn.set | op | Set server spawn to your current location. |
| /back | sfcore.back | true | Return to your previous location. |
| /top [player] | sfcore.top | op | Teleport to the highest safe block above your position. |

## Player Management
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /vital [player] | sfcore.vital | op | Restore health and hunger in one action. |
| /god [player] | sfcore.god | op | Toggle complete invincibility. |

## Utility
| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| /heal | sfcore.heal | op | Restore your health to full. |
| /feed | sfcore.feed | op | Restore your hunger bar. |
| /craft [item] | sfcore.craft | op | Show a detailed item profile. |

## Wildcards
- `sfcore.*` grants everything.
- `sfcore.admin.*` bundles reload/diagnostics/admin menu.
- `sfcore.debug.*` bundles all config/debug tools.
- `sfcore.moderation.*` bundles bans, kicks, mutes, warns, `/status`, and `/banlist`.
- `sfcore.updates.*` bundles version and update checks.
