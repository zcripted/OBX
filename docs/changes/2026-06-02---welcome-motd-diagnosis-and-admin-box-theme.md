# Welcome-MOTD diagnosis + boxed theme for admin server-control messages

■ **Created:** 2026-06-02 8:59 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 8:59 pm (America/Detroit)

## Issue 1 — Welcome MOTD only showing two lines (diagnosis, not a code regression)

Investigated thoroughly and proved the current code is correct on a fresh install:
- `welcome.motd-lines` data is fully intact in `MessageDefaults` (8 lines incl. the
  4 structured `{text,hover,click}` nodes), byte-identical to before.
- The real value survives the YAML write→read path with all 4 structured nodes
  (test: `WelcomeMotdWriteTest`, since removed).
- `parseToSpans` produces correct visible spans for whole-line `<hover>/<click>`
  lines (test: `ParseSpansMotdTest`, since removed).
- No MOTD rendering code was changed; usage boxes (same renderer) render fine on
  the user's obfuscated jar.

**Conclusion:** the running server has a **stale `language_en.yml`** whose
`welcome.motd-lines` only contains the plain lines; key-sync never overwrites
existing keys, so the structured lines never returned. **Fix (no customization
loss):** delete the `welcome.motd-lines` (and `welcome.motd-first-join-lines`)
entries from `plugins/OBX/language_en.yml` (and `sprache_de.yml`), then run
`/obx reload` — key-sync re-adds the full structured MOTD from defaults.

- Kept `MotdRoundTripTest` as a regression guard for the structured-list round-trip.

## Issue 2 — Admin server-control messages now use the `/ban` boxed theme

Wrapped the eight admin messages in the same boxed layout as the `/ban` usage
messages (blank · `▍ 𝗢𝗕𝗫  ›  Server  ·  <Name>` · 30× `─` rule · blank · body ·
blank), via a shared frame in `ServerControlActions`:

- whitelist toggle, join-lock toggle — boxed, with the `[Toggle]` button on the body line.
- cleared entities — boxed, with `[All] [Mobs] [Items]` buttons on the body line.
- kick non-ops, spectator-only — boxed, with the listed roster (or "none") inside.
- TPS, weather change, redstone updates — boxed via `ServerControlActions.boxMessage(...)`.

Details:
- Dropped the now-redundant `{prefix}` wordmark from the eight body message keys
  (the box title carries the OBX identity) and indented them; updated EN + DE.
- Fixed interactive parts to section-colorize (`&`→§) before the BungeeCord
  `fromLegacyText` path, so button labels/hovers render styled (matching the usage
  message code path).
- Routed AdminSubMenu's weather / redstone / TPS sends through the boxed helper.
- Files:
  - `src/main/java/dev/zcripted/obx/util/control/ServerControlActions.java` (box frame + boxMessage)
  - `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java` (weather/redstone/TPS routed)
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java` (8 bodies de-prefixed/indented)

## Also fixed (from the prior round, confirmed wired)

- AFK auto-detect: `AfkService` is registered + started in `Main`, and clears on
  movement OR looking around. (See `2026-06-02---afk-fix-...md`.)

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 15 tests, 0 failures.
- Both jars built.

## Notes / caveats

- Box title category/name ("Server · Whitelist", …) are English literals (these
  are op-only admin panels); the translated message bodies remain per-locale.
- Runtime smoke test recommended for the in-game box/button rendering (no server
  available here; verified by compile + unit suite + the colorize-path fix).

## Suggested Commit Message

```
Style(admin): box the server-control messages like /ban usage; diagnose MOTD

Wrap whitelist/joinlock/kick/spectator/TPS/cleared/weather/redstone in the OBX
box theme with inline buttons; colorize interactive parts for the bungee path.
Welcome-MOTD truncation traced to a stale language file (fresh file verified
correct); fix = delete the welcome.motd-lines entries and /obx reload.
```
