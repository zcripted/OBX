# Console Log De-duplication, ANSI Theme, Enchant & MOTD Fixes

■ **Created:** 2026-05-26 10:57 am (America/Detroit)

■ **Last Updated:** 2026-05-26 11:26 am (America/Detroit)

---

## Summary

Cleans up SF-Core's console output and fixes two functional bugs surfaced in the
boot log:

- The doubled `[SF-Core] [SF-Core]` prefix on every line.
- Three spurious `unknown category; skipping` warnings from the Arcanum module.
- The server-list (MOTD) player-count hover failing to build a player sample on
  modern Paper.

All SF-Core console lines now use a single, gold/aqua ANSI-themed prefix that
matches the startup banner.

---

## Root Causes

- **Doubled prefix:** Bukkit's `PluginLogger` already prepends `[SF-Core]` to
  every `getLogger()` call, and the message text *also* contained a literal
  `[SF-Core]` (or `[SF-Core][Arcanum]`, etc.), producing `[SF-Core] [SF-Core]…`.
- **Unknown category:** `EnchantRegistry` parsed *every* `.yml` in `enchants/` as
  an enchantment roster, including the module config `config.yml`. Its sections
  `apply`, `pvp`, and `conflict_groups` were misread as enchantments with no
  category.
- **MOTD hover:** the player-sample entries were built via `Bukkit.createProfile`
  / `createPlayerProfile`, which validate the profile *name*. The hover "names"
  are arbitrary coloured text (e.g. `§eOnline: 5/20`), so modern Paper rejected
  them (`StandardPaperServerListPingEventImpl` → `PlayerProfile:no-build`), and no
  custom hover was shown.

---

## Categories

### Internal — Console Logging
- New `ConsoleLog` utility routes SF-Core console output through the
  `ConsoleCommandSender` with a single colored `[SF-Core]` prefix and an optional
  subsystem tag (`[Arcanum]`, `[MOTD]`). The platform renders the legacy
  section-codes as ANSI on a color-capable terminal and strips them when piped to
  a file; the normal `[HH:mm:ss INFO]:` timestamp and `logs/latest.log` entry are
  preserved. Falls back to the plugin logger (colors stripped) if the console
  sender is unavailable.
  - `src/main/java/dev/sergeantfuzzy/sfcore/util/message/ConsoleLog.java` (new)
- Converted startup/INFO lines to `ConsoleLog` and stripped the redundant literal
  `[SF-Core]` from WARNING/SEVERE logger calls (which keep their proper log level
  and single framework prefix).
  - `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/command/core/SFCoreCommand.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/enchant/registry/EnchantRegistry.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/enchant/loot/EnchantLoot.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/listener/server/MotdPingListener.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/hub/HubService.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/hub/kit/HubKitApplier.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/hub/messaging/BungeeMessenger.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/listener/player/CommandOverrideListener.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/platform/bukkit/resourcepack/AutoResourcePackManager.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/util/message/ConsoleTimestamp.java`

### Internal — Language Files
- The two separate "Created default language file" lines are folded into one
  themed summary (`Generated default language files: language_en.yml, sprache_de.yml`),
  emitted only for files actually created on the run.
  - `LanguageFile.ensureExists` now returns whether it created the file; added
    `getFileName()`; the mojibake-repair line uses `ConsoleLog`.
  - `LanguageManager.reload` collects created files and prints one `ConsoleLog.list`.
  - `src/main/java/dev/sergeantfuzzy/sfcore/language/LanguageFile.java`
  - `src/main/java/dev/sergeantfuzzy/sfcore/language/LanguageManager.java`

### Bugfix — Arcanum Enchantments
- `EnchantRegistry` now skips reserved module-config files
  (`config.yml`, `scrolls.yml`, `loot.yml`) when scanning `enchants/`, so their
  sections are no longer parsed as enchantments. Eliminates the
  `apply` / `pvp` / `conflict_groups` "unknown category; skipping" warnings.
  - `src/main/java/dev/sergeantfuzzy/sfcore/enchant/registry/EnchantRegistry.java`

### Bugfix — MOTD / Server-List Hover
- **First attempt** (`createProfileExact`): preferred the exact factory before the
  validating ones. This did **not** resolve it — on modern Paper every public
  profile factory (`createProfile` / `createPlayerProfile` / `createProfileExact`)
  validates the profile NAME (≤16 chars, `[A-Za-z0-9_]`) and rejects the coloured
  MOTD hover text, so the player-sample never built (`PlayerProfile:no-build`).
- **Actual fix** — new `GAME_PROFILE_WRAP` strategy in `MotdPingListener`:
  builds a raw `com.mojang.authlib.GameProfile(uuid, line)` (which performs **no**
  name validation) and wraps it into the runtime `CraftPlayerProfile` (located via
  the live server's CraftBukkit package, covering the unversioned 1.20.5+ package
  and the older `v1_xx_Rn` form). `CraftPlayerProfile` implements `PlayerProfile`,
  so the sample list accepts it. Resolution prefers a `CraftPlayerProfile(GameProfile)`
  constructor, then a static factory taking a `GameProfile` (`asBukkitCopy` /
  `asBukkitMirror`); falls back to the public factories on non-Paper platforms.
  The "ping handled" diagnostic now also reports the resolved factory shape so any
  residual failure is self-explaining.
  - `src/main/java/dev/sergeantfuzzy/sfcore/listener/server/MotdPingListener.java`

---

## Assumptions

- "Combine any created default files … into one single message" is implemented
  for the language files (the only files that previously logged one line each).
  Other defaults (`config.yml`, the per-category enchant rosters, `motd.yml`, …)
  are written silently via `saveResource` and were never logged, so they produce
  no console spam to combine.
- Warnings/errors remain on `getLogger()` to preserve their WARN/SEVERE log level
  (important for operators grepping logs); only the redundant `[SF-Core]` literal
  was removed from them. Themed ANSI coloring is applied to the INFO lines that
  appear on every boot.

---

## Testing
- Maven build completes with no errors (in-project `./maven`).
- Obfuscated and unobfuscated jars build successfully.

---

## Suggested Commit Message
```
Fix (console): De-duplicate [SF-Core] log prefix + ANSI theme; fix enchant config parsing and MOTD hover sample
```
