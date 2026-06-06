# Market-Readiness Fixes

■ **Created:** 2026-06-03 2:51 am (America/Detroit)

■ **Last Updated:** 2026-06-03 8:19 am (America/Detroit)

A pass over the issues surfaced by the full market-readiness assessment: completing
half-built features, closing two abuse vectors, hardening a couple of edge cases, and
bringing the command/permission manifests and docs back into parity. `./gradlew build`
stays green and both jars (`OBX-1.0.0-beta-b1.jar` + `-unobf.jar`) are produced.

---

## Commands

- Added `/ipban` (aliases `/banip`) and `/ipunban` (aliases `/unbanip`, `/pardonip`)
  backed by native `BanList.Type.IP` (persists in `banned-ips.json`). `/ipban` resolves
  either an IP literal or an online player's current address and kicks everyone on it.
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/command/ModerationCommand.java`
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/service/ModerationService.java`
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/ModerationModule.java`
  - `plugin/src/main/resources/plugin.yml`, `plugin/src/main/resources/paper-plugin.yml`
- Removed duplicate aliases in the command-override map (`obx:heal`, `obx:god`,
  `obx:tps`, `obx:pl`/`obx:plugins`, and a duplicate `obx` namespace).
  - `core/src/main/java/dev/zcripted/obx/core/command/CommandOverrideListener.java`

## Aliases (collision fixes)

Two commands each had an alias that was also claimed elsewhere, making the shared label
resolve ambiguously. A full alias/command audit of both manifests now reports zero
collisions.

- **`/ptime` (personal time) vs `/playtime`** — `playtime` declared `ptime` as an alias
  while `/ptime` is its own command, so `/ptime` was ambiguous. Dropped `ptime` from
  `playtime`'s aliases and gave it the free aliases `pt` and `played`. `/ptime` is now an
  unambiguous standalone command.
  - `plugin/src/main/resources/plugin.yml`, `plugin/src/main/resources/paper-plugin.yml`
- **`/gmode` claimed by both `/gamemode` and `/god`** — `gmode` (short for "game mode")
  was in `god`'s alias list in both manifests *and* routed to the `god` executor in the
  command-override listener (at `LOWEST` priority, so it shadowed `/gamemode` entirely).
  Removed `gmode` from `god` everywhere; `/gmode` now opens `/gamemode` as intended.
  (Behavior change: `/gmode` no longer toggles god.)
  - `plugin/src/main/resources/plugin.yml`, `plugin/src/main/resources/paper-plugin.yml`
  - `core/src/main/java/dev/zcripted/obx/core/command/CommandOverrideListener.java`
- Doc rows for `/playtime` and `/god` updated to the corrected alias lists.
  - `docs/information/COMMANDS+PERMISSIONS.md`

## Permissions

- Defined the ~56 command permission nodes that were referenced by commands but never
  declared (e.g. `obx.fly`, `obx.balance`, `obx.pay`, `obx.kit`, `obx.nick`, `obx.sell`,
  `obx.give`, the `obx.jail*` family, …) with sensible `default:` values — self-service
  player commands `true`, admin/staff/privileged commands `op` — and added them to the
  `obx.*` wildcard children so granting the bundle now actually grants them. Added
  `obx.moderation.ipban` / `obx.moderation.ipunban` under `obx.moderation.*`.
  - `plugin/src/main/resources/plugin.yml`
  - `plugin/src/main/resources/paper-plugin.yml`

## Economy

- `/pay`: fixed a possible NPE when the target's name could not be resolved; the command
  now rejects unknown/never-joined accounts up front and reuses the resolved name.
  - `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/PayCommand.java`
- `/sell` (hand/material/all): each path now verifies the items were actually removed
  before crediting money, closing a theoretical "no-op removal mints currency" dupe.
  - `features/economy/src/main/java/dev/zcripted/obx/feature/economy/command/SellCommand.java`

## Kits

- Completed the first-join kit feature: a `PlayerJoinEvent` listener now grants every kit
  flagged `first-join: true` once per player (idempotent via the persisted
  `kit_first_join` claim flag), gated by a new global `first-join.enabled` toggle
  (default on). There is no kit admin GUI, so the config flag is the toggle surface.
  - `features/kit/src/main/java/dev/zcripted/obx/feature/kit/listener/KitFirstJoinListener.java` (new)
  - `features/kit/src/main/java/dev/zcripted/obx/feature/kit/service/KitService.java`
  - `features/kit/src/main/java/dev/zcripted/obx/feature/kit/KitModule.java`
  - `plugin/src/main/resources/kits.yml`

## Moderation

- Closed the mute bypass: muted players can no longer talk through commands. A new
  `PlayerCommandPreprocessEvent` listener blocks the configurable communication
  commands in `moderation.muted-blocked-commands` (msg/tell/mail/me/… , namespace-aware);
  non-chat commands like `/spawn` are unaffected.
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/listener/MuteCommandListener.java` (new)
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/service/ModerationService.java`
  - `features/moderation/src/main/java/dev/zcripted/obx/feature/moderation/ModerationModule.java`
- Added IP-ban support (see **Commands**).

## Holograms

- Added a chunk/world lifecycle listener so hologram entities despawn when their
  chunk/world unloads and respawn (scrubbing any saved orphan) when it loads — fixes the
  slow orphan-entity accumulation that previously only cleared on a full restart.
  - `features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/listener/HologramChunkListener.java` (new)
  - `features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/HologramTag.java` (new `scrubChunk`)
  - `features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/render/HologramRenderer.java` (per-hologram spawn/destroy)
  - `features/hologram/src/main/java/dev/zcripted/obx/feature/hologram/HologramModule.java`

## Enchant

- Documented the HIGH→HIGHEST event-priority ordering contract at both combat handlers so
  a future maintainer doesn't reorder the damage-modifier and proc passes.
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/listener/OnHitDamageListener.java`
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/listener/OnHitProcListener.java`

## Internal

- `ResourcePackListener` now re-checks the manager/player at execution time so its delayed
  pack-apply task can't act after a disable or a quit.
  - `core/src/main/java/dev/zcripted/obx/core/platform/resourcepack/ResourcePackListener.java`
- Note: `TpsService.PAPER_PROBED` was already `volatile`; no change needed.

## Config

- Added `moderation.muted-blocked-commands` (with the default communication-command list).
  - `plugin/src/main/resources/config.yml`
- Added the `first-join.enabled` toggle.
  - `plugin/src/main/resources/kits.yml`

## Messages (EN/DE/ES parity)

- Added via `MessageDefaults.def(...)`: `economy.sell.failed`, `kit.first-join-received`,
  `player.moderation.mute.command-blocked`, `player.moderation.usage.ipban`,
  `player.moderation.usage.ipunban`, `player.moderation.ipban.success` / `.already` /
  `.unresolved`, `player.moderation.ipunban.success` / `.not-banned`.
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaults.java`

## Language (full Spanish translation)

- Spanish (`es`) is now a **complete** translation, not English-fallback. Previously
  only 4 of 1,097 keys had Spanish; now all **977 translatable keys** (every key whose
  English and German differ) carry a curated Spanish override. The 120 language-neutral
  keys (prefixes, dividers, numeric labels) intentionally still fall back to English.
- Implemented in `MessageDefaults.spanishOverrides()`, split into `esBatch00()`–`esBatch13()`
  helper methods so no single method approaches the JVM 64 KB method-size limit. All
  color codes, `{placeholders}`, MiniMessage tags, `\u` escapes, `list(...)`/`map(...)`
  structure, and GUI tooltip line counts are preserved.
- Added a build-time completeness test (`MessageDefaultsTest#spanishOverridesComplete`):
  every key German translates must have a Spanish override, with a matching value type
  and (for lists) matching line count. Build is green (5/5 language tests pass).
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaults.java`
  - `core/src/test/java/dev/zcripted/obx/core/language/MessageDefaultsTest.java`
- **Deployment note:** a fresh install generates a fully-Spanish `plugins/OBX/lang/es.yml`.
  An existing server already has an `es.yml` containing every key (with the old English
  fallback values); `syncDefaults` never overwrites existing keys, so to pick up the new
  Spanish on an existing install, delete `plugins/OBX/lang/es.yml` and restart to regenerate.

## Language (Spanish polish + per-language restructure)

- **Closed the remaining Spanish gaps.** ~26 keys whose English and German happened to be
  byte-identical (so they fell back to English) but which a Spanish player would notice
  are now translated — e.g. the gamemode label `Survival → Supervivencia` (its siblings
  Creativo/Aventura/Espectador were already Spanish), the GUI category titles
  `Admin/Moderation/Teleport/Information/Config & Debug → Administración/Moderación/
  Teletransporte/Información/Configuración y depuración`, status values
  `Online/Offline/Permanent → Conectado/Desconectado/Permanente`, plus `Casas:`,
  `Bloque`, `Objeto`, `Lista blanca`, `Descargar`, etc. Truly technical tokens
  (command strings, `TPS`/`API`/`UUID`/`MSPT`, `/obx …` usages) and universal MC loanwords
  (`Kit`, `Staff`, `Mobs`, `AFK`) are intentionally left as-is.
- **Restructured the message catalogue into one self-contained class per language** so a
  translator can copy/edit a single file without cross-language mixup:
  - `MessageDefaultsEN.java` — English (base) + EN section headers
  - `MessageDefaultsDE.java` — German + DE section headers
  - `MessageDefaultsES.java` — Spanish (complete: all 1,097 keys; the 94 language-neutral
    keys copy English)
  - `MessageDefaults.java` is now a thin orchestrator: it owns the registry→map wiring,
    the public `defaults()` / `sectionComments()` / `requiredKeys()` API (unchanged
    signatures, so `LanguageManager` is untouched), and the `list()` / `slist()` / `map()`
    value constructors (now package-private so the language classes static-import them).
    The old `def(key,en,de)` coupling and `esBatch*()` / `spanishOverrides()` were removed.
  - Each language class splits its 1,097 `m.put(...)` calls across 16 `partNN()` helpers
    to stay under the JVM 64 KB method-size limit.
- Updated `MessageDefaultsTest` to enforce the tri-language contract: EN/DE/ES expose the
  exact same key set, `requiredKeys()` matches all three, no null/empty values, and value
  **shape** matches across languages (String vs List vs Map, and identical list line
  counts). 5/5 tests pass; full build green; both jars produced.
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaults.java`
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` (new)
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java` (new)
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java` (new)
  - `core/src/test/java/dev/zcripted/obx/core/language/MessageDefaultsTest.java`
- **Spanish section headers.** Gave `MessageDefaultsES` its own `sections()` method with the
  26 per-category YAML header comments translated to Spanish (previously ES reused the
  English headers), wired a `SEC_ES` map in the orchestrator, and routed
  `sectionComments(ES)` to it. All three language files are now symmetric (1,214 lines).
  Added a test (`sectionHeadersShareKeys`) asserting EN/DE/ES expose the same section-header
  keys. 6/6 language tests pass; build green.
- **Spanish consistency + naturalness pass** (`MessageDefaultsES.java`): standardized term
  variants and fixed literal calques —
  - `sólo → solo` (modern RAE, accent dropped) — 6 occurrences.
  - `Telepórtate → Teletranspórtate` (file uses `teletransport-` throughout) — 2.
  - `ítem → objeto` in the `utility.craft.*` block (`PERFIL DEL OBJETO`, `Objeto desconocido`, etc.).
  - `tu hogar → tu casa` in `welcome.motd-first-join-lines` (matches `teleport.home.*`).
  - `jail.default-reason`: `No se dio razón. → No se especificó ningún motivo.` (matches the
    moderation "Motivo" wording).
  - `admin.staff.action.*.lore`: `Marcador de posición - aún no implementado →
    Funcionalidad preliminar — aún no implementada` (×5).
  - `enchant.settings.enabled/disabled`: `pancartas de muerte y la respuesta de combate →
    mensajes de muerte y la información de combate`.
  - `player.moderation.unmute.target`: reordered to variable-first —
    `{sender} te quitó el silencio`.
  - `player.invsee.cannot-view-self`: `Solo el permiso… puede usar → Solo con permiso…
    puedes usar`.
  - Typo `PurPur → Purpur` in `commands.obx.about.lines`.
  All 15 standardizations verified (0 stragglers remain, 0 mojibake); 6/6 tests pass.
  - `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

## Docs

- Brought `docs/information/COMMANDS+PERMISSIONS.md` back to full parity (~120 commands):
  added Economy, Kits, Nickname, Player Info, Items & Inventory, World & Environment,
  Chat & Staff, Jail, and Flight & Movement sections, plus `/ipban`/`/ipunban`,
  `/mail`/`/ignore`, and `/tptoggle`/`/tpcancel`/`/tpall`/`/tppos` into existing tables.
  - `docs/information/COMMANDS+PERMISSIONS.md`

## Repo hygiene

- Stopped tracking `.idea/` (added to `.gitignore`) — IDE config shouldn't ship in a
  marketplace release.
  - `.gitignore`

---

## Polish (post-launch hardening)

A small pass on the LOW-severity items from the second assessment:

- **Declared 3 code-enforced permission nodes** in both manifests (definitions + `obx.*`
  wildcard children) so they surface in LuckPerms/permission managers: `obx.nick.color`,
  `obx.nick.others`, `obx.message.ignore.bypass` (all `default: op`). Manifests stay in
  sync (203 permission keys each).
  - `plugin/src/main/resources/plugin.yml`, `plugin/src/main/resources/paper-plugin.yml`
- **Explicit `EventPriority.LOWEST`** on the two `PlayerMoveEvent` handlers (Freeze, AFK)
  so a frozen player's position is reverted before anticheat/damage/teleport handlers see
  the rejected movement. Both already early-exit, so this is clarity, not a behavior change.
  - `features/staff/src/main/java/dev/zcripted/obx/feature/staff/service/FreezeService.java`
  - `features/playerstate/src/main/java/dev/zcripted/obx/feature/playerstate/service/AfkServiceImpl.java`
- **Escaped single quotes in the `/msg` suggest-command username** (`ChatFormatter.wrapClickable`)
  so a name with an apostrophe (Geyser/Bedrock gamertags) can't terminate the single-quoted
  MiniMessage argument early. Vanilla usernames can't contain quotes; defense-in-depth.
  - `features/chat/src/main/java/dev/zcripted/obx/feature/chat/format/ChatFormatter.java`
- **Null-safe enchant registry level lookups.** Added `levelDouble/levelInt/levelBoolean/
  levelString(id, level, key, def)` helpers to `EnchantRegistry` that return the default if
  the id isn't registered, and converted the 8 unguarded inline
  `registry.get("x").levelXxx()` combat derefs to use them — guarding the path where an
  admin removes a custom enchant from config while items still carry it.
  - `features/enchant/src/main/java/dev/zcripted/obx/feature/enchant/registry/EnchantRegistry.java`
  - `features/enchant/.../listener/OnHitDamageListener.java`, `OnHitProcListener.java`
- **Replaced the raw `printStackTrace()`** in `ModuleManager` with
  `logger.log(SEVERE, msg, t)` — same trace, routed through the logging framework.
  - `core/src/main/java/dev/zcripted/obx/core/module/ModuleManager.java`

Build green; both jars produced.

## Critical: Paper plugin-load failure (removed the Paper-native path)

On modern Paper (tested 1.21.11), the jar shipped **both** `plugin.yml` and `paper-plugin.yml`,
so Paper loaded it as a **Paper-native plugin** — which is fundamentally incompatible with
OBX's architecture and broke startup entirely:

- `OBXPaperLoader` called `MavenLibraryResolver.addRepository(... repo1.maven.org ...)`, which
  Paper now rejects with a `RuntimeException` ("Plugin used Maven Central for library resolution").
- Every module called `JavaPlugin#getCommand(...)` for its YAML-declared commands, which throws
  `UnsupportedOperationException` on Paper plugins → **all 18 modules failed to enable** and the
  plugin disabled itself.
- The `/obx about` + disable banner showed the literal `${release.date}` (unexpanded Maven
  placeholder from `build-info.properties`; Gradle does no resource filtering).

Fix — ship as a classic `plugin.yml` plugin (Paper runs these fully; `getCommand` works and
`plugin.yml`'s `libraries:` loads the SQLite driver via Paper's own mirror-backed resolver):

- Removed `plugin/src/main/resources/paper-plugin.yml` (the trigger for Paper-native loading).
- Removed the bootstrap/loader merge from the jar: dropped the `:platform:paper` jar-merge in
  `plugin/build.gradle.kts` and the `:platform:paper` include in `settings.gradle.kts`, so
  `OBXBootstrap`/`OBXPaperLoader` (the Maven-Central-violating loader) are no longer shipped.
  - `plugin/build.gradle.kts`, `settings.gradle.kts`
- Removed the obsolete ProGuard `-keep` rules for the bootstrap classes (kept `-dontwarn
  io.papermc.paper.**` for the reflective Folia/Adventure detection).
  - `proguard.pro`
- Deleted `plugin/src/main/resources/build-info.properties` (only `releaseDate` was ever read;
  it now cleanly defaults to "Unknown" — no unexpanded placeholder, no warning).

Verified on the built jar: contains only `plugin.yml` (no `paper-plugin.yml`, no
`build-info.properties`), zero bootstrap classes, no `bootstrapper:`/`loader:` keys, and still
declares `main` / `load: POSTWORLD` / `api-version` / the `org.xerial:sqlite-jdbc` library.
`clean build` green; both jars produced. (The `/pl` command's references to `paper-plugin.yml`
are unrelated — it inspects *other* plugins' jars to classify them.)

Note: `platform/paper/` source remains on disk but is no longer in the build (dead). It can be
deleted; reviving a Paper-native build would require migrating all command registration from
YAML to Paper's Brigadier `registerCommand` API.

## Cleanup (dead/old/unused removal)

Following the Paper-native removal, deleted the now-dead artifacts:

- **`platform/` module** (the `:platform:paper` module) — `OBXBootstrap`, `OBXPaperLoader`,
  its `build.gradle.kts`, and all build output. No longer in the build or referenced anywhere
  (the `/pl` command's `paper-plugin.yml` lookups inspect *other* plugins, not OBX).
- **`INSTRUCTIONS.md`** — the original "Add Multi-Language System (EN+DE)" task brief, fully
  implemented and superseded by `CLAUDE.md` + the live EN/DE/ES catalogue.
- **`planning/brainstorming.txt`** (+ the now-empty `planning/` dir) — pre-build design notes.
- **`.DS_Store`** — tracked macOS junk; also added to `.gitignore`.
- Three empty leftover source package dirs (`core/.../util/teleport`,
  `core/.../util/message` test dir, `features/jail/.../model`) — local on-disk cruft.

Git history still preserves all removed files. `clean build` green; both jars produced.

## Suggested Commit Message

```
Fixes (market-readiness): complete first-join kits, block mute bypass + IP bans, declare all perms, resolve alias collisions, hologram unload cleanup, full Spanish translation + per-language catalogue split, doc parity
```
