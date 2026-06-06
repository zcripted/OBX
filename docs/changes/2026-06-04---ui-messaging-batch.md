# UI / Messaging Batch — box-style, diagnostics, admin menu, fixes

■ **Created:** 2026-06-04 3:12 am (America/Detroit)

■ **Last Updated:** 2026-06-04 3:12 am (America/Detroit)

A batch of message redesigns (box-style theme), GUI tooltip work, and behavior fixes.
Build green (`./gradlew build`), both jars produced, EN/DE/ES parity test passes (all new
keys added to all three catalogues).

---

## Commands / Messages

- **`/obx config` pagination controls.** The config-file list now ends with a clickable
  `« Prev` / `Next »` row (greyed-out at the ends) that runs `/obx config <page±1>`, with a
  page indicator in the middle. New keys `commands.obx.config.nav.*`.
  Files: `core/.../command/ObxDiagnosticsView.java` (`sendConfigPagination`),
  `MessageDefaults{EN,DE,ES}`.
- **`/obx diagnostics` is now a real health check** (was duplicating `/obx info`). Shows
  **Platform** (server type + MC version, Folia tag), **Modules** (enabled/total, with a
  disabled count), **Config** (OK / N missing), **Storage** (SQLite ready / unavailable), and
  an overall **Health** verdict. **`/obx diagnostics full`** adds an EXTENDED section:
  **Services** (registered OBX API services), **Hooks** (detected Vault / PlaceholderAPI /
  ProtocolLib / LuckPerms), and **Errors** (recorded issues — disabled modules, missing
  configs, storage). New keys `commands.obx.diagnostics.{platform,modules,config-status,storage,health,extended,hooks,errors}`.
  File: `core/.../command/ObxDiagnosticsView.java` (`handleDiagnostics`).
- **`/banlist` redesigned** into the box-style theme (header bar + divider + clean per-ban
  rows + total footer). Files: `BanListCommand.java`, `player.moderation.banlist.*`.
- **IP-ban usage messages box-style** for `/ipban` (alias `/banip`) and `/ipunban` (aliases
  `/unbanip`, `/pardonip`). `player.moderation.usage.{ipban,ipunban}` are now boxed and list
  their aliases. *(No standalone `/` command exists; the family above is what those commands map to.)*
- **`/home` & `/homes` box-style.** The `/homes` list and the home not-found error (shared
  with `/delhome`) are now boxed. Transient confirmations (set/removed/teleporting) stay as
  single themed lines to avoid a multi-line box on every teleport.
- **TPS hover tooltip category.** Each TPS value's tooltip header now reads
  `Performance · <N>-Minute Average`, so hovering 1m / 5m / 15m is uniquely labeled.
  File: `core/.../diagnostics/TpsCommand.java`.

## Fixes

- **`/obx reload` hover scoping.** The hover/click is now attached ONLY to the
  "Reload complete (time)" segment (the prefix is a plain, non-hover part) via
  `sendJoinedHoverMessages`. New no-prefix key `commands.obx.reload.full.body`.
- **`/obx reload <file>` hover scoping + timestamp.** Same scoping; the file reload is now
  timed and shows "Reloaded file <file> (time)" in the same `Nms` format as the base reload.
  New key `commands.obx.reload.file.body`.
- **`/tptoggle` raw string fixed.** It referenced non-existent `tpa.toggle-on/off`. Added
  box-style `teleport.request.toggle-on` / `toggle-off` (EN/DE/ES) and pointed the command at
  them. File: `TpToggleCommand.java`.
- **`/pt` playtime accuracy + format.** Session playtime was wrong — `flushAll()` reset the
  session anchor every flush, so "session" only counted time since the last flush. Added a
  separate `sessionJoins` map (true join time, never reset by flush) for the session figure;
  total was already correct. The duration formatter is now full **y / mo / d / h / m / s**
  showing only the relevant (non-zero) fields, and the messages are box-style.
  File: `PlaytimeService.java`, `info.playtime.*`.
- **Inbox "Clear" on an empty inbox** now shows a formal inbox-prefixed error
  (`inbox.clear-empty`) instead of a fake "cleared 0". A bookmarked-only inbox gets a distinct
  message (`inbox.clear-bookmarked-only`). File: `PrivateMessageService.clearInbox`.
- **Silenced the "Added N missing keys to en/de/es.yml" reload spam** — keys are still merged,
  but the log line is now debug-gated (expected after a plugin update). File: `LanguageManager.reload`.

## Config / Startup

- **Holograms enabled by default on startup** — bundled `systems/holograms.yml` is now
  `enabled: true` and the runtime default flipped to `true`.
  Files: `plugin/.../systems/holograms.yml`, `HologramService.isEnabled`.

## Admin Menu (GUI)

- **Server Control — live players + uptime.** The main Admin Menu's Server Control item is now
  re-rendered by `AdminMenuRefreshTask` (~0.5s cadence, the same one driving the sub-menus), so
  its player count and uptime tick while the menu is open. New `AdminMenu.refreshLive(...)`.
  Files: `AdminMenu.java`, `AdminMenuRefreshTask.java`.
- **Fun Utilities tooltip redesigned** to the Server Control theme, with an accurate
  description of the sub-menu (Butcher / Spawn Mobs / Smite / Grow Trees) — the old
  "reserved for cosmetic toggles" text was wrong.
- **Warp Manager & Hub / Lobby Controls tooltips redesigned** to the same theme
  (category headers, `label » value` rows, divider, click hint). Hub keeps its live
  `{status}` / `{worlds}` values.
  Files: `admin.menu.item.fun-utilities.*`, `admin.menu.warp.lore`, `admin.menu.hub.lore` (EN/DE/ES).

## Notes / choices
- The redesigned messages live in `MessageDefaults{EN,DE,ES}`. Existing servers keep their
  on-disk `lang/*.yml` values until those files are regenerated (the defaults only fill missing
  keys) — delete the affected keys (or the lang files) to pick up the new look.
- "Live within milliseconds" for the Server Control item is served by the existing ~500ms
  refresh task; that's the cadence all admin menus already use.

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` (EN/DE/ES parity) passes.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` + `-unobf.jar`.

## Suggested Commit Message
```
Feature (ui): box-style messages, diagnostics health check, admin menu tooltips

/obx config pagination + /obx diagnostics health check (+full: services/hooks/errors);
box-style /banlist, ip-ban usage, /home+/homes, /pt, /tptoggle; scope /obx reload hovers
to the result segment (+file timestamp); inbox empty-clear error; holograms on by default;
live Server Control players/uptime + redesigned Fun Utilities/Warp/Hub tooltips; silence
missing-keys reload log; fix /pt session accuracy + full duration format.
```
