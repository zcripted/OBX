# Clickable Usage Messages + Command Upgrades

■ **Created:** 2026-06-01 8:10 pm (ET)

■ **Last Updated:** 2026-06-01 11:24 pm (ET)

Adds a plugin-wide click-to-suggest + hover treatment to every command usage
message, fixes the raw `{prefix}afk.set-other-on` / AFK key leak, redesigns the
remaining single-line usages into the boxed `/ban`-style layout, and ships several
command upgrades (give safeguard, flyspeed per-player, clearinv item/count + hover,
`/clear` override, plugin-list hovers, gamemode clickable mode list).

---

## Internal / API

- **Interactive usage renderer.** `LanguageManager.send()` now detects usage keys
  (`*.usage`, `*-usage`, `*.usage.*`, `*.usage-*`; the `commands.obx.entry.*` help
  entries are excluded) and routes them through a new `sendUsage()` that turns the
  `/command` token into a click-to-suggest component carrying a shared hover tooltip.
  Works for both the boxed `usageBox` layout and single-line forms, and degrades to a
  plain line on console.
  - `src/main/java/dev/zcripted/obx/language/LanguageManager.java`
- The hover suggests the literal command up to its first `<`/`[` placeholder (e.g.
  `/give `), and an optional per-usage note can be appended via a `<key>.hint` entry.

## Messages / Language (language_en.yml + sprache_de.yml via MessageDefaults)

- Added shared hover key `core.usage-hint.hover` (suggest-vs-run explanation).
- **Fixed missing AFK keys** that leaked raw keys to chat: `afk.now-afk`,
  `afk.no-longer-afk`, `afk.set-other-on`, `afk.set-other-off`, `afk.kick-reason`.
- Migrated all remaining player-facing single-line usages to the boxed `usageBox`
  layout (teleport, economy, info, messaging, items, inventory, jail, mobs, nickname,
  world, message, `/obx permissions`, hub, freeze, firstseen, broadcast, flyspeed).
  `-console` variants stay single-line; they remain clickable but render plain on
  console.
- `/give`: boxed usage + `item.give.usage.hint`, new `item.give.partial` /
  `item.give.full` keys (and kept `item.give.overflow` for `/i`).
- `/flyspeed`: boxed usage with `[player]`, new `flight.speed.set-fly-other` /
  `flight.speed.set-fly-target`.
- `/clearinv`: `self`/`other` now include the cleared quantity in parentheses; added
  hover-template keys `inventory.clearinv.hover.*`.
- Gamemode usage rebuilt: `gamemode.usage.self` / `gamemode.usage.target` are boxed
  headers with `/gamemode <mode>`; new `gamemode.usage.row`, `row-hover`,
  `row-hover-other`, and `label.*` keys back the clickable mode list.
- Plugin list: added `commands.pl.hover.*` and `commands.pl.status.*` keys.
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Commands

- **/give** (`GiveCommand.java`): added `[amount]` tab suggestions (1/16/32/64 + the
  material's max stack); added a capacity safeguard — only what fits in the combined
  hotbar + storage (36 slots) is given, the remainder is skipped (never dropped) and
  the giver is warned. Reports the given amount.
  - `src/main/java/dev/zcripted/obx/command/utility/GiveCommand.java`
- **/flyspeed** (`FlySpeedCommand.java`): added optional `[player]` to set another
  player's fly speed (requires `obx.flyspeed.others`, op by default), with tab
  completion and per-target feedback.
  - `src/main/java/dev/zcripted/obx/command/utility/FlySpeedCommand.java`
- **/clearinv** (`ClearInvCommand.java`): now supports `[player] [item] [maxCount]`
  (vanilla `/clear` parity), counts what was removed, shows the quantity in the chat
  message, and attaches a hover tooltip listing the total cleared plus the five most
  recent item stacks. Tab-completes item + count.
  - `src/main/java/dev/zcripted/obx/command/utility/ClearInvCommand.java`
- **/gamemode** (`GamemodeCommand.java`): usage now shows `/gamemode <mode>` plus an
  indented, click-to-run list of Survival/Creative/Adventure/Spectator (targets the
  named player when applicable).
  - `src/main/java/dev/zcripted/obx/command/utility/GamemodeCommand.java`
- **/pl** (`PluginListCommand.java`): each plugin name now carries a hover tooltip
  (name, version, author, software/platform, API version when present, status) in the
  OBX violet theme; console falls back to the joined colored names.
  - `src/main/java/dev/zcripted/obx/command/admin/PluginListCommand.java`

## Listeners

- **/clear override** (`CommandOverrideListener.java`): `/clear` and its namespaced
  variants now route to OBX `/clearinv`, passing the `[player] [item] [maxCount]` args
  straight through (matches the existing `/pl`, `/heal`, `/god`, `/tps` overrides).
  - `src/main/java/dev/zcripted/obx/listener/player/CommandOverrideListener.java`

## Config

- `plugin.yml`: updated `flyspeed` usage to `<0-10> [player]` and `clearinv` usage to
  `[player] [item] [maxCount]`.
  - `src/main/resources/plugin.yml`

## Assumptions

- `obx.flyspeed.others` and `obx.clearinv.others` are intentionally not declared in
  `plugin.yml` (op-only by default), matching the other utility command permissions.
- The `/clear` full-replace drops vanilla selector/targeted syntax that OBX
  `/clearinv` does not model (e.g. `@s`); player/item/count forms are preserved.
- "5 most recent items" in the clearinv hover is approximated by reverse slot scan,
  since Bukkit exposes no real pickup order (documented in code).

---

## Follow-up Fixes (2026-06-01 11:24 pm ET)

- **Freeze raw-key leak.** `FreezeService.toggle()` sent the undefined
  `freeze.now-frozen` / `freeze.no-longer-frozen` keys to the target, leaking
  `{prefix}freeze.now-frozen` to chat (visible when freezing yourself). Repointed it
  to the already-defined `freeze.frozen-target` / `freeze.unfrozen-target` keys, and
  added the missing `freeze.actionbar` key (the repeating frozen reminder was leaking
  its raw key into the action bar).
  - `src/main/java/dev/zcripted/obx/util/control/FreezeService.java`
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- **Flyspeed feedback.** The admin/op confirmation (`flight.speed.set-fly-other`) now
  carries a `[STAFF]` tag so the targeted change reads as an admin action. The target's
  notification (`flight.speed.set-fly-target`) intentionally does **not** reveal who
  changed it — it only states the new value.
  - `src/main/java/dev/zcripted/obx/command/utility/FlySpeedCommand.java`
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

## Suggested Commit Message

```
Feature (usage/commands): clickable usage messages, AFK key fix, give/flyspeed/clearinv/gamemode/pl upgrades, /clear override
```
