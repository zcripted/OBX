# Join/Leave Module, Welcome MOTD, and GUI Help Override

■ **Created:** 2026-05-06 12:30 am

■ **Last Updated:** 2026-05-06 2:52 am

## Summary

Three coordinated additions:

1. A toggleable join/leave broadcast module with full Adventure / MiniMessage formatting (hex, gradients, hover, click, shadow, decorations, legacy `&` codes).
2. A toggleable in-game welcome MOTD that fires for the joining player only, with first-join overrides and the same Adventure formatting capabilities.
3. A `/help`, `/?`, `/bukkit:help`, `/bukkit:?` override that opens a 54-slot paginated GUI listing every default-true command on the server, sorted A–Z.

Both modules can be flipped at runtime via `/obx joinleave on|off|status` and `/obx joinmotd on|off|status` (no plugin reload required) and via the `join-leave.enabled` / `join-motd.enabled` keys in `config.yml`.

## Categories

### Commands
- Added `/obx joinleave <on|off|status>` — toggles the join/leave broadcast module. Permission: `obx.admin.modules.joinleave`.
- Added `/obx joinmotd <on|off|status>` — toggles the in-game welcome MOTD module. Permission: `obx.admin.modules.joinmotd`.
- Added `/help` (aliases `/?`, `/bukkit:?`, `/bukkit:help`, `/minecraft:help`, `/minecraft:?` via the preprocess listener) — opens the OBX paginated GUI help menu. Permission: `obx.help.gui` (default true).
- Tab completion updated for both `/obx` toggles.
- `/god` now refuses to toggle in Creative or Spectator gamemode (matching the existing `/feed`, `/heal`, and `/vital` gating). Self-target shows `player.god.invalid-gamemode`; targeting another player who is in Creative/Spectator shows `player.god.invalid-gamemode-other`. New EN/DE strings added to `MessageDefaults`.
- `/heal` and `/god` now hard-override any Paper/Bukkit/vanilla command of the same label. `CommandOverrideListener` intercepts `PlayerCommandPreprocessEvent` at `LOWEST` priority for the bare label and every namespaced variant (`bukkit:`, `minecraft:`, `essentials:`, `obx:`, `obx:`) — cancelling the dispatch and routing to the OBX executor directly so other plugins (or an Essentials install) cannot win the registration race. `/god` aliases (gmode, godmode, invincible, immortal) are covered as well. Note: namespaced aliases are *not* declared in `plugin.yml` because Paper rejects colons in plugin alias names with "Illegal Characters" — the preprocess listener handles namespacing entirely on its own.
- New chat management module replaces the bare-bones `ChatListener`. Default in-game format is `<gray>{player}</gray> <yellow>»</yellow> <white>{message}</white>` (light gray name, light-yellow `»` separator, white message). Format is fully customisable via Adventure / MiniMessage in a new YAML, and every message is mirrored to the server console using truecolor ANSI escapes that match the in-game styling. Console mirror lines are prefixed with a Paper-style timestamp (`[HH:mm:ss INFO]: ` by default) — toggle with `console-timestamp` and customise via the `console-timestamp-format` `DateTimeFormatter` pattern in `chat_management.yml`.
- The same timestamp prefix is now applied to the ANSI-mirrored join/leave broadcasts. `JoinLeaveListener` reads `ChatService.isConsoleTimestampEnabled()` / `ChatService.getConsoleTimestampFormat()` so the toggle and pattern live in a single place. Both call sites route through the new `dev.zcripted.obx.util.message.ConsoleTimestamp` helper which validates the pattern and falls back to the default with a one-line operator warning if the pattern is malformed.
- `/craft` is now a virtual crafting table. The previous item-profile lookup moved to `/research` (aliases `discover`, `itemprofile`, `iteminfo`) — same behaviour, new name. New commands `/anvil`, `/enchant`, `/smith` open virtual anvil, enchanting table, and smithing table interfaces respectively. All four live in the **Utility** category. Permission rename: `obx.craft` (still default `op`) now means "open virtual crafting table"; `obx.research`, `obx.anvil`, `obx.enchant`, `obx.smith` are new and inherit `op` defaults. `/anvil` requires Spigot 1.14+ and `/smith` requires 1.16+ — operators on older platforms see a localised "feature unavailable" message instead of a non-functional UI.
- Help GUI now filters by viewer permission instead of strictly default-`TRUE`. OPs see every command they can run (the new workstations, `/heal`, `/god`, `/top`, `/kill`, etc.) while normal players still see the default-true subset. The list is built dynamically from every loaded plugin's `plugin.yml`, so future additions appear automatically without GUI code changes.
- New tablist module under `chat`-style folder branching (`tablist/service`, `tablist/format`, `tablist/listener`, `tablist/scheduler`). Header / footer / per-player entry name are all configured in `systems/tablist.yml` with full Adventure / MiniMessage support. A `BukkitRunnable` refresh task (default 40 ticks) keeps `{online}`, `{ping}`, `{time}`, and `{tps}` current. Adventure's `sendPlayerListHeaderAndFooter` is used when available (Paper 1.16+) with a Bungee `setPlayerListHeaderFooter` fallback for 1.12+ servers.

### GUIs
- New 54-slot paginated `HelpGuiMenu` lists every command whose permission default is `TRUE` (or has no permission requirement) sorted alphabetically.
  - 45 command items per page (rows 1–5).
  - Last row: previous-page button at slot 45 (omitted on page 1), info star at slot 49, next-page button at slot 53 (omitted on the final page).
  - Each item is a `BOOK` displaying `&6/{command}` with hover lore showing command, usage, permission, description (word-wrapped at 45 visible chars without breaking words), and a click-to-suggest hint.
  - Clicking a command item closes the menu and runs `/{command}` for the player.
  - Drags and item movements are blocked.

### Permissions
- New: `obx.admin.modules.joinleave` (default `op`).
- New: `obx.admin.modules.joinmotd` (default `op`).
- New: `obx.help.gui` (default `true`).
- Both new admin nodes added to the `obx.admin.*` wildcard children.

### Config (`config.yml`)
- New `join-leave` section with `enabled`, `suppress-vanilla`, `join-message`, `leave-message`, and `first-join.enabled` / `first-join.message`.
- New `join-motd` section with `enabled`, `lines`, and `first-join.enabled` / `first-join.lines`.
- Both sections include a top comment block linking to the Adventure MiniMessage reference: <https://docs.advntr.dev/minimessage/format.html>.
- Default messages demonstrate gradient, hover, click, decorations, and the available placeholders (`{player}`, `{displayname}`, `{world}`, `{online}`, `{max}`, `{uuid}`).

### Config (`systems/chat_management.yml`)
- New file housing the chat management module configuration.
- Top-level keys: `enabled`, `console-mirror`, `allow-formatting-in-messages`.
- `format.template` is the master string combining `{username}`, `{separator}`, and `{message}`. Each component has its own MiniMessage template under `format.components.*` so server owners can change the username color, the separator character (default `»`), or wrap the message in any Adventure tags.
- Header comment links to the Adventure MiniMessage reference and lists every available placeholder.
- Player-typed `<` and `>` are sanitised to typographic angle quotes (`‹`, `›`) by default so untrusted users cannot inject MiniMessage tags; flip `allow-formatting-in-messages` to true to permit it.

### API / Utilities
- New `AdventureMessageUtil` formatter at `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`. Renders MiniMessage-style input via Adventure when available (Paper 1.16+); falls back to BungeeCord chat components otherwise. Supports legacy color codes, `&#RRGGBB`, `<#RRGGBB>`, named colors, gradients, hover (`<hover:show_text:'...'>`), click events, decorations, `<reset>`, and `<shadow:#RRGGBB>` (renders on 1.21.4+ Adventure path; silently skipped on legacy paths). Hex colors degrade to the nearest legacy palette entry for 1.12 clients.
- `AdventureMessageUtil.renderAnsi(raw, placeholders)` converts the same MiniMessage spans into truecolor ANSI escapes (`[38;2;R;G;Bm` plus bold/italic/underline/strikethrough/obfuscated). Hover/click annotations are dropped since terminals can't render them. Console join/leave broadcasts route through `Main.writeConsoleLine` (the same direct-console writer used by the OBX boot banner) so the colors actually print instead of being stripped by the standard logger.
- New `JoinLeaveService` at `src/main/java/dev/zcripted/obx/storage/JoinLeaveService.java` provides typed accessors and runtime toggles for the join/leave and join-MOTD config keys.
- New paginated GUI at `src/main/java/dev/zcripted/obx/gui/player/HelpGuiMenu.java` with companion holder `HelpGuiHolder.java`.

### Internal
- New listener `JoinLeaveListener` at `src/main/java/dev/zcripted/obx/listener/player/JoinLeaveListener.java` handles `PlayerJoinEvent` and `PlayerQuitEvent`. Suppresses vanilla join/leave messages when the module is enabled and dispatches the configured Adventure-styled broadcast to players plus an ANSI-encoded copy to the server console. Welcome MOTD is sent on a 10-tick delay so it lands after vanilla join handling.
- New listener `HelpGuiListener` at `src/main/java/dev/zcripted/obx/listener/menu/HelpGuiListener.java` cancels click/drag in the help GUI, handles pagination, runs the clicked command, and intercepts `PlayerCommandPreprocessEvent` for `/help`, `/?`, `/bukkit:help`, `/bukkit:?`, `/minecraft:help`, `/minecraft:?` so the GUI wins regardless of plugin load order.
- New executor `HelpGuiCommand` at `src/main/java/dev/zcripted/obx/command/core/HelpGuiCommand.java` opens the GUI for players and falls back to a console-friendly hint.
- `Main` registers the new service, listener, and command, exposes `getJoinLeaveService()`, and reloads the service on `/obx reload`.
- `ObxCommand` adds `joinleave` and `joinmotd` subcommand handlers and tab completions.
- `MessageDefaults` adds entries for `admin.modules.joinleave.*`, `admin.modules.joinmotd.*`, `admin.modules.state.*`, and the full `commands.help.gui.*` family in English and German. Bundled `language_en.yml` and `sprache_de.yml` resync from defaults at startup via `LanguageManager.reload()`.

## Files Modified
- `pom.xml` — *(no changes; existing build pipeline handles new sources)*
- `src/main/resources/config.yml`
- `src/main/resources/plugin.yml`
- `src/main/java/dev/zcripted/obx/Main.java`
- `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Files Added
- `src/main/java/dev/zcripted/obx/util/message/AdventureMessageUtil.java`
- `src/main/java/dev/zcripted/obx/storage/JoinLeaveService.java`
- `src/main/java/dev/zcripted/obx/listener/player/JoinLeaveListener.java`
- `src/main/java/dev/zcripted/obx/gui/player/HelpGuiMenu.java`
- `src/main/java/dev/zcripted/obx/gui/player/HelpGuiHolder.java`
- `src/main/java/dev/zcripted/obx/listener/menu/HelpGuiListener.java`
- `src/main/java/dev/zcripted/obx/command/core/HelpGuiCommand.java`
- `src/main/java/dev/zcripted/obx/listener/player/CommandOverrideListener.java`
- `src/main/java/dev/zcripted/obx/chat/service/ChatService.java`
- `src/main/java/dev/zcripted/obx/chat/format/ChatFormatter.java`
- `src/main/java/dev/zcripted/obx/chat/listener/ChatManagementListener.java`
- `src/main/resources/systems/chat_management.yml`
- `src/main/java/dev/zcripted/obx/tablist/service/TablistService.java`
- `src/main/java/dev/zcripted/obx/tablist/format/TablistRenderer.java`
- `src/main/java/dev/zcripted/obx/tablist/listener/TablistJoinListener.java`
- `src/main/java/dev/zcripted/obx/tablist/scheduler/TablistRefreshTask.java`
- `src/main/resources/systems/tablist.yml`

## Files Removed
- `src/main/java/dev/zcripted/obx/listener/chat/ChatListener.java` — superseded by `ChatManagementListener` which subsumes the mute check, custom layout, Adventure dispatch, and ANSI console mirror.

## Verification
- `mvn clean package -DskipTests` — BUILD SUCCESS (78 source files, exit 0, ProGuard pass clean).
- ProGuard "Note:" lines for `net.kyori.adventure.*`, `org.bukkit.GameRule`, and `com.mojang.authlib.GameProfile` are expected: those classes are detected and used reflectively at runtime, not at compile time.

## Suggested Commit Message

```
Feature (modules): Add join/leave broadcasts, welcome MOTD, and GUI help override

Adds two toggleable feature modules and a /help replacement GUI:

- /obx joinleave + join-leave config section: Adventure-styled join, leave,
  and first-join broadcasts with gradient, hover, click, and shadow support.
- /obx joinmotd + join-motd config section: multi-line in-game welcome MOTD
  shown to the joining player with first-join overrides.
- /help, /?, /bukkit:help, /bukkit:? open a 54-slot paginated GUI listing
  every default-true command alphabetically. Hover tooltips wrap at 45
  visible chars without breaking words; pagination buttons hide on edge
  pages.

Adds AdventureMessageUtil with a manual MiniMessage subset parser that
falls back to BungeeCord chat components when Adventure is not on the
runtime classpath.
```
