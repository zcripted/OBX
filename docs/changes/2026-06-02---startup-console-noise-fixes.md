# Startup console-noise fixes

■ **Created:** 2026-06-02 9:05 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 9:05 pm (America/Detroit)

Three console-output bugs seen on a fresh startup.

## 1. "Added N missing keys" on a freshly-created language file

`LanguageManager.reload()` ran `syncDefaults()` on every language file, including
ones just written from defaults by `ensureExists()`. A fresh file already has
every key, but the sync pass miscounted 4 keys as "missing" (a `readValue`
round-trip quirk on certain structured keys) and logged "Added 4 missing keys".

Fix: only run the sync pass on **pre-existing** files. A freshly created file is
complete by construction, so it now logs only the normal "Generated default
language files" line — no missing-key notices.
- `src/main/java/dev/zcripted/obx/language/LanguageManager.java`

## 2. MOTD Paper-ping-listener line logged with debug off

`MotdPingListener` logged "Registered Paper ping listener … (enables the
player-count hover)." unconditionally on startup. Gated it behind the plugin
debug flag (`config "debug"`), so it only appears with debug enabled.
- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`

## 3. Vault economy line not using OBX console theme

`VaultEconomyProvider` logged "Registered as Vault economy provider." via the raw
`plugin.getLogger().info(...)`, bypassing the ANSI-themed OBX console prefix.
Routed it through `ConsoleLog.info(plugin, …)` so it matches the rest of the
`[OBX]` console output. (`ConsoleLog` has no warn variant, so the failure-path
`warning(...)` was left as-is.)
- `src/main/java/dev/zcripted/obx/economy/VaultEconomyProvider.java`

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 15 tests, 0 failures; both jars built.

## Suggested Commit Message

```
Fix(console): quiet fresh-file key sync, gate MOTD log on debug, theme Vault line

Skip syncDefaults on freshly-created language files (no false "Added N missing
keys"); only log the Paper ping-listener registration when debug is on; route the
Vault economy registration line through ConsoleLog for the OBX ANSI theme.
```
