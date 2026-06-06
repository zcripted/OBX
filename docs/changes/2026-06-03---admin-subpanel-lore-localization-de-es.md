# Admin Sub-Panel Interior Lore Localization (EN/DE/ES)

■ **Created:** 2026-06-03 9:46 AM (America/Detroit)

■ **Last Updated:** 2026-06-03 9:46 AM (America/Detroit)

Completes the GUI-lore localization that the previous pass
(`2026-06-03---gui-lore-localization-and-interactive-fixes.md`) had deferred: every
**deeper Admin sub-panel interior** now renders per-player in **English, German, and
Spanish** — titles, item names, lore, live status readouts, and navigation buttons. No
hardcoded English strings remain in the admin submenu chrome. `./gradlew build` is
**green**; both jars produced (`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`); EN/DE/ES
message-parity test passes.

## Scope (now localized)

- **Server Control** panel (Server State / Player Access / Performance + Health / World
  Controls overview tiles, Plugin + Systems tile).
- **Server State** (Stop / Restart / Safe Restart / Lock / Unlock — with live whitelist &
  join-lock readouts).
- **Player Access** (Toggle Whitelist / Join Lock / Max Players / Kick Non-Ops / Spectator
  Only).
- **Performance + Health** (View TPS / Clear Entities / Toggle Redstone).
- **World Controls** (Save Worlds / Autosave Toggle / World Border / Weather / Time /
  Game Rule Editor) — including the live World-Border detail lore (diameter, center,
  warning, damage with localized "blocks/buffer/block" units) and the enabled-gamerule
  preview list.
- **Weather Control** sub-menu (Clear / Rain / Thunder + localized current-weather word).
- **Time Control** sub-menu (Morning / Noon / Night / Freeze-Unfreeze).
- **Game Rule Editor** — editor chrome: title, "Click to toggle / Current / Unavailable"
  lore, the `[ON]/[OFF]/[N/A]` badge, the enabled-count preview, and nav. (See note below
  on the rule display names.)
- **Plugin + Systems** (Reload Configs / Toggle Modules).
- **Module Toggles** (the 6 module names — Chat Formatting, Scoreboard, Tablist,
  Join/Leave, Welcome MOTD, Hub/Lobby — plus the State / Click-to-enable-disable lore and
  the toggle confirmation chat message).
- **Hub / Lobby Controls** (Hub Mode / Hub Worlds / Server Selector / Reload / Re-apply Kit
  / Launchpad / Jump-To Rod / Players-Visibility).
- **World Border** editor (± buttons, info tile, Center On Me, Reset).
- **Jail Center** (Jail Anchors / Set Jail Here / Delete Jail / Check Jail Time).
- **Mob Tools** (Butcher / Spawn Mob / Smite / Grow Tree).
- **Back / Close** navigation items (Back to Admin Panel / Server Control / World Controls /
  Plugin + Systems, Close).

## Implementation

- All menu/item builders now resolve text per-player from the language files instead of
  hardcoded `ChatColor + "..."`. Static lore moved into `list()` keys; live values
  (whitelist state, max players, diameter, cooldown, etc.) flow in as placeholders.
- Status readouts use a shared localized **ENABLED/DISABLED** word (`admin.gui.common.*`)
  injected as `{whitelist}` / `{joinlock}` / `{redstone}` / `{autosave}` / `{daylight}` /
  `{current}` / `{state}` placeholders, so each line's label is translated in the message
  and only the colored state word is computed in Java.
- A feature module can't reference the bootstrap (`OBX`, which lives in `:plugin`) or the
  `ObxPlugin` interface statically, so the menus resolve the language manager via
  `JavaPlugin.getProvidingPlugin(...)` cast to `ObxPlugin` (`AdminSubMenu.languages()` /
  `AdminMenuRender.languages()`). Small `t()` / `tl()` / `onOff()` / `title()` / `map()`
  helpers keep the call sites compact.
- `AdminSubMenu.refresh(...)` (the once-per-second live re-render driven by
  `AdminMenuRefreshTask`) now takes the viewing `Player` so its re-rendered status tiles
  stay in that player's language.
- `ModuleEntry` gained a `nameKey()`; `GameruleEntry` keeps its English `display` (used for
  stable A–Z layout ordering and as the localized item's `{name}`).

## Documented decision

The **40 individual game-rule display names** (e.g. *Keep Inventory*, *Mob Griefing*) are
intentionally **kept in canonical English**. They mirror the exact vanilla Minecraft
gamerule identifiers that admins type and reference across the ecosystem, so translating
them would reduce recognizability. Everything *around* them in the Game Rule Editor (title,
lore, badges, counts, nav, the toggle chat message's state word) is fully localized.

## Files

- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminSubMenu.java`
  — every `open*`/builder/lore method localized; `languages()`/`t()`/`tl()`/`onOff()`/
  `title()`/`map()` helpers; `weatherStateText(player)`; `refresh(holder, player)`;
  `ModuleEntry.nameKey()`.
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenuRender.java`
  — `createBackItem*` / `createCloseItem` now per-player & localized (`navItem` + own
  `languages()` helper).
- `features/staff/src/main/java/dev/zcripted/obx/feature/staff/gui/AdminMenuRefreshTask.java`
  — passes the player to `AdminSubMenu.refresh(...)`.
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` / `DE.java` /
  `ES.java` — ~150 new `admin.gui.*` keys per language (titles, nav, common states, units,
  and every sub-panel's item names/lore).

## Suggested Commit Message

```
i18n (admin GUI): localize all deeper admin sub-panel interiors into EN/DE/ES (server control/state/access/performance, world controls, weather/time/gamerule editors, hub, modules, world border, jail center, mob tools); per-player rendering via providing-plugin language lookup
```
