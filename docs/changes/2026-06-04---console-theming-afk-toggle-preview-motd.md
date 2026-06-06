# Console theming, AFK master toggle, /preview, MOTD hover scoping

■ **Created:** 2026-06-04 10:34 am (America/Detroit)

■ **Last Updated:** 2026-06-04 10:34 am (America/Detroit)

A batch of console-logging polish, a new AFK master on/off system, a console color-preview
command, and a MOTD hover-scoping fix. Build green (`./gradlew build`), both jars produced,
EN/DE/ES parity test passes.

---

## Console / Startup

- **SQLite open log themed.** `SqliteDataStore` now logs through `ConsoleLog` (purple `[OBX]`
  prefix + light-gray body) with **SQLite** and the **db file** in light purple and **schema vN**
  in green. File: `core/.../storage/SqliteDataStore.java`.
- **Vault provider message reworded** to `Registered OBX as the economy system provider.` —
  light-gray text with a **purple bold OBX**. File: `features/economy/.../VaultEconomyProvider.java`.
- **Startup banner gradient made visible.** The OBX banner purple gradient was a near-black
  `#2A0A45 → #6A1B9A` that vanished on dark consoles; changed to a vivid `#9D4EDD → #C77DFF`.
  File: `plugin/.../OBX.java`.

## AFK system — master on/off toggle (new)

Adds a true master switch that turns off **auto-AFK detection, the idle timeout/kick, and AFK
messages** all at once, reachable three ways:
- **Config:** `afk.enabled: true` (new) in `config.yml`.
- **Command:** `/obx afk <on|off|status>` — box-style confirmation in-game + a console audit line.
  Permission `obx.admin.modules.afk` (op).
- **Admin GUI:** a new **AFK System** tile in the Module Toggles sub-menu (feather icon).

Mechanics: `AfkService.isEnabled()/setEnabled()` (persists to config); `AfkServiceImpl.tick()` and
`setAfk()` early-return when disabled; disabling clears everyone's AFK state, enabling re-arms the
idle timers; `/afk` itself reports `afk.system-disabled` when the system is off. New keys
(`admin.modules.afk.*`, `admin.gui.module.afk`, `afk.system-disabled`) in EN/DE/ES.
Files: `api/.../playerstate/AfkService.java`, `features/playerstate/.../service/AfkServiceImpl.java`,
`features/playerstate/.../command/AfkCommand.java`, `core/.../command/ObxModulesView.java`,
`plugin/.../core/command/ObxCommand.java`, `features/staff/.../gui/AdminSubMenu.java`,
`plugin/.../resources/config.yml`, `plugin.yml`, `MessageDefaults{EN,DE,ES}`.

## `/preview` console command (new)

`/preview <text>` (alias `/prev`) renders a MiniMessage / hex / gradient / legacy-`&` string with
**full 24-bit color → ANSI truecolor** straight to the server console (hex can't be carried by `§`
codes), or as a normal chat message in-game. No-args prints worked examples (legacy codes, hex,
named colors, attributes, gradient). Reuses `AdventureMessageUtil.renderAnsi` + the banner's
`writeConsoleLine` path, so it works on every platform/console. Permission `obx.preview` (op).
Example: `/preview <gradient:#ff0000:#00ff00>Rules:</gradient> <yellow>1. No Griefing`.
Files: `core/.../command/PreviewCommand.java` (new), `plugin/.../OBX.java`, `plugin.yml`,
`docs/information/COMMANDS+PERMISSIONS.md`.

## Fix — MOTD hover scoping

The welcome MOTD applied each line's hover/click to the **whole line**. Added an optional
`"scope"` field to the structured MOTD node format: when present, the hover/click wraps only that
substring. Scoped the three lines so the tooltip/click applies to **only the username** (Hello
line), **only `/obx help`** (Type line), and **only the link text** (Discord line).
Files: `core/.../language/LanguageManager.java` (`renderMotdNode`), `welcome.motd-lines` nodes in
`MessageDefaults{EN,DE,ES}`.

## Docs
- `COMMANDS+PERMISSIONS.md`: added `/preview` (Admin) and `/obx afk` (Admin → Modules), alphabetized.

## Testing
- `./gradlew build` — **BUILD SUCCESSFUL**; `:core:test` (EN/DE/ES parity incl. all new keys) passes.
- Both jars produced: `OBX-1.0.0-beta-b1.jar` + `-unobf.jar`.

## Suggested Commit Message
```
Feature: console theming, AFK master toggle, /preview, MOTD hover scoping

Theme the SQLite open log + reword Vault message; brighten the banner gradient; add a
master AFK on/off (config afk.enabled, /obx afk, Module Toggles GUI) that gates detection,
timeout/kick, and messages; add /preview (+/prev) rendering hex/gradient/MiniMessage to ANSI
truecolor in console; scope welcome-MOTD hovers to just the username/command/link via a new
node "scope" field.
```
