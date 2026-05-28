# Bloodletter bleed-out, Treecapitator warning, styled /status, /sf config pagination, click-to-message, scoreboard module

■ **Created:** 2026-05-27 11:30 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 11:30 pm (America/Detroit)

---

## Summary

Adds a Bloodletter bleed-out HUD + death/gravestone, a Treecapitator creative warning,
restyles `/status` to the boxed report look, makes `/sf config` a dynamic paginated list with
no links, adds click-a-name-to-message in `/list` and public chat, and introduces a new
FeatherBoard-style **Scoreboard** module configured in `systems/scoreboard.yml`.

## Categories

### Arcane Enchantments
- **Bloodletter** (`enchant/listener/OnHitProcListener.java`): the victim now gets a live
  "&4❤ Bleeding out » Ns left" action-bar countdown on hit and on each bleed tick. On a
  bleed-out **death** the victim gets a chat notification + title, and a **private gravestone**
  — a per-player particle marker (`Particles.atPlayer`, new) at the death spot that only they
  can see, lasting a configurable duration (default 35s), plus a chat line with its
  coordinates. Toggle + duration in `enchants/config.yml` → `combat_global`
  (`bloodletter_gravestone`, `bloodletter_gravestone_seconds`), surfaced via new
  `CombatSettings` getters.
- **Treecapitator** (`enchant/listener/ToolEnchantListener.java`): a creative-mode player
  swinging a Treecapitator tool now gets a quick action-bar warning each break that the
  enchant is survival-only (instead of silently doing nothing).
- New messages (EN+DE): `enchant.tool.creative-warning`, `enchant.bloodletter.actionbar`,
  `enchant.bloodletter.death-title/-subtitle/-message`, `enchant.bloodletter.gravestone`.
- `enchant/util/Particles.java`: new `atPlayer(...)` (player-scoped `spawnParticle`).

### Other commands
- **`/gamemode <mode> <player>`** (`MessageDefaults`): success line reworded to
  "Set {player}'s gamemode to {mode}." (`gamemode.changed-other` + `-console`).
- **`/status <player>`** (`command/moderation/ModerationStatusCommand.java` + `MessageDefaults`):
  restyled to match `/pl` and `/sf info` — boxed `▍ 𝗦𝗙-𝗖𝗢𝗥𝗘 › Player Status · <name>` header,
  `─` divider, indented `Label › value` rows (the old `&8│` left-bar prefixes removed); closes
  with the slim `core.divider-line`.
- **`/sf config`** (`command/core/SFCoreCommand.java` + `MessageDefaults`): now **dynamically
  lists every `.yml`** under the data folder (recursively, sorted), as plain text with **no
  clickable links**, **paginated** (`/sf config <page>`, 10 per page) so output never exceeds
  15 lines. `/sf config validate` gained `tablist.yml` + `scoreboard.yml` rows. Old static
  `commands.sf.config.list` replaced by `list-header` / `list-entry` / `list-footer` /
  `list-empty`.

### Click-to-message
- **`/list`** (`command/core/ListCommand.java`): each online player's name is now a
  click-to-message — clicking suggests `/msg <name> ` in the chat box, with a "Click to
  message <name>" hover (`player.list.message-hover`). Console falls back to plain text.
- **Public chat** (`chat/format/ChatFormatter.java`, `chat/listener/ChatManagementListener.java`):
  the sender's name in normal chat is clickable for every other viewer (suggests
  `/msg <sender> ` + hover, MiniMessage `chat.message-hover`); the sender themselves and the
  console see the plain line. `ChatFormatter.compose` gained a clickable-name overload.

### Scoreboard module (new — `dev.sergeantfuzzy.sfcore.scoreboard`)
- `service/ScoreboardService.java` — loads `systems/scoreboard.yml` (enabled, refresh
  interval, title, server-ip, server-website, lines); `reload()` re-reads on `/sf reload`.
- `format/ScoreboardRenderer.java` — per-player sidebar via scoreboard teams (line text in
  team prefix/suffix, unique color-token entries; works 1.8→1.21). Placeholders: `{plugin}`
  (title = plugin name), `{player}`, `{online}`/`{max}` (live), `{health}`/`{health_percent}`
  (e.g. 93%) and `{hearts}` (live 10-icon ❤/♡ bar), `{ip}`, `{website}`, `{world}`,
  `{displayname}`. Because the sidebar needs a per-player board, the OP-red / player-yellow
  **nametag teams are replicated** onto each board (repopulated with online players) so the
  name colors + TAB grouping keep working alongside the sidebar.
- `scheduler/ScoreboardRefreshTask.java` — repeating re-render so live fields stay current;
  returns players to the main board when disabled.
- `listener/ScoreboardJoinListener.java` — shows the sidebar on join.
- `src/main/resources/systems/scoreboard.yml` — bundled default config (title, IP/website,
  player/health+hearts/online/IP/website lines, separators).
- `Main.java` — constructs/loads/starts/cancels the service+task, registers the join listener,
  reloads it in `reloadPlugin()` (logged as `scoreboard.yml`), and exposes
  `getScoreboardService()`. The dynamic `/sf config` list automatically includes
  `systems/scoreboard.yml`.

## Notes / assumptions
- "Light red / light yellow" nametags = `RED` / `YELLOW`; the scoreboard's per-player board
  replicates those teams so the earlier nameplate feature isn't lost when the sidebar is on.
- The gravestone is a per-player particle marker (true per-player block/entity visibility
  needs packets, which SF-Core avoids); it renders only while the victim is in that world and
  near enough for the client to draw particles. A chat line gives the exact coordinates.
- Scoreboard lines use legacy `&`/`&#hex` codes (team prefix/suffix are legacy text, not the
  MiniMessage set used by tablist/chat); hex renders on 1.16+ and degrades gracefully.
- Per-refresh team repopulation is O(players²) across the server; fine at the default 20-tick
  interval for typical player counts.
- Chat name-click uses MiniMessage `<click:suggest_command>`/`<hover>` which the existing
  chat transport (Adventure / Bungee component fallback) already carries.

## Testing
- Maven build: `& ".\maven\bin\mvn.cmd" -DskipTests package` → exit 0; both jars rebuilt
  (obf ~673 KB, unobf ~973 KB). ProGuard `Note:` lines only. Compile-verified.
- In-game checks to run: Bloodletter hit → victim bleed-out action bar; bleed-out death →
  victim title/message + private gravestone for 35s; Treecapitator in creative → warning;
  `/gamemode creative <player>` wording; `/status <player>` boxed layout; `/sf config` (+
  `/sf config 2`) plain paginated list incl. all yml; clicking a name in `/list` and in chat
  opens `/msg`; the sidebar shows title/player/health%+hearts/online/IP/website and updates
  live; `/sf reload` re-applies scoreboard changes.

## Suggested Commit Message
```
Feature: Bloodletter bleed-out HUD + gravestone, Treecapitator creative warning, styled /status, dynamic paginated /sf config, click-to-message (/list + chat), and a configurable sidebar scoreboard module
```
