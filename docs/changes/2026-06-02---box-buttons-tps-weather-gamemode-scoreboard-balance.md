# Box button rows, unified TPS, weather theming, gamemode buttons, scoreboard balance

■ **Created:** 2026-06-02 9:49 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 9:49 pm (America/Detroit)

## 1. Buttons on their own line in box messages

Toggle/selection buttons now render on a dedicated, indented action row beneath
the box body instead of inline. New `sendButtonRow(...)` helper renders
`  » [Button]  [Button]` (console senders, which can't click, just get the body).
Applied to whitelist, join-lock (`[Toggle]`), clear-entities (`[All] [Mobs]
[Items]`), and weather.
- `src/main/java/dev/zcripted/obx/util/control/ServerControlActions.java`

## 2. GUI "View TPS" uses the exact /tps message

Extracted the `/tps` report into `TpsCommand.sendReport(plugin, sender)` (public
static). The Performance + Health GUI's View TPS item now calls it, producing the
identical styled report (per-window TPS + hover tooltips) instead of a one-line box.
- `src/main/java/dev/zcripted/obx/command/admin/TpsCommand.java`
- `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`

## 3/4/5. Weather messages — themed value, boxed, button row, shared by /weather

- Weather type shows in a secondary colour and capitalised (e.g. `&bRain`) via the
  new `admin.weather.set` + `admin.weather.type.*` keys (EN + DE).
- New `ServerControlActions.weatherMessage(plugin, actor, mode)` renders the boxed
  message + an action row with a button for **every** weather event
  (`[Clear] [Rain] [Thunder]`, each running `/weather <mode>`), plus console mirror.
- The Weather Control GUI and the `/weather` command now both route through it, so
  they produce identical boxed output.
- `src/main/java/dev/zcripted/obx/util/control/ServerControlActions.java`
- `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`
- `src/main/java/dev/zcripted/obx/command/world/WeatherCommand.java`

## 6. /gamemode usage — button layout

`sendModeUsage` now sends the box followed by one organized action row of
clickable mode buttons (`[Survival] [Creative] [Adventure] [Spectator]`, each
running `/gamemode <mode> [target]`) instead of a bullet list of one-per-line
click-to-run rows. New `gamemode.usage.button` key. Interactive parts are
section-colorized for the BungeeCord component path.
- `src/main/java/dev/zcripted/obx/command/utility/GamemodeCommand.java`

## 7. Bank/balance field on the scoreboard

Added `{balance}` (and `{bank}` alias) scoreboard placeholders — the player's full
economy balance with the configured currency symbol (e.g. `$1,250.00`, default `$`
from `economy.symbol`). Added a `Bank` line to the default `scoreboard.yml` and
documented the placeholders.
- `src/main/java/dev/zcripted/obx/scoreboard/format/ScoreboardRenderer.java`
- `src/main/resources/systems/scoreboard.yml`

## Messages (EN + DE added)

`admin.weather.set`, `admin.weather.type.{clear,rain,thunder}`,
`admin.button.weather`, `admin.button.weather.hover`, `gamemode.usage.button`.
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 15 tests, 0 failures; both jars built.

## Notes / caveats

- Existing servers keep their `scoreboard.yml`; to show the Bank line on an
  existing board, add `"&7Bank &8» &a{balance}"` (the `{balance}` placeholder now
  works regardless). Fresh installs get it by default.
- Weather buttons run `/weather <mode>` (needs `obx.weather`); from chat that
  targets the clicker's current world.
- Runtime smoke test recommended for the in-game button rows (no server here;
  verified via compile + 15-test suite + the colorize path used by usage messages).

## Suggested Commit Message

```
Feature(ui): dedicated button rows, unified TPS, themed weather, gamemode buttons, scoreboard balance

Move box toggle/selection buttons to their own action row; GUI View TPS reuses
TpsCommand.sendReport; weather value colored+capitalized with a per-event button
row shared by /weather and the GUI; /gamemode usage shows clickable mode buttons;
add {balance}/{bank} scoreboard placeholder + default Bank line.
```
