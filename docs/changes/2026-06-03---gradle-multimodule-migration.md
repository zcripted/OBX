# Maven → Gradle multi-module migration

■ **Created:** 2026-06-03 5:26 am (America/Detroit)

■ **Last Updated:** 2026-06-03 5:26 am (America/Detroit)

Converts OBX from a single-module Maven build into a professional package-by-feature
**Gradle multi-module** build: one subproject per feature on a thin shared core,
Shadow-merged into a single jar, then ProGuard-obfuscated — shipping both the
obfuscated and unobfuscated jars. Done on branch `gradle-multimodule` in verified,
individually-committed phases.

## Categories

### Build system
- **Gradle 8.10.2 wrapper** committed (`gradlew`, `gradle/wrapper/*`); `pom.xml` and
  the portable `maven/` distribution removed.
- **buildSrc convention plugins**: `obx.java-conventions` (Java 8 bytecode, provided
  server APIs as compileOnly + on the test classpath, JUnit 5) and
  `obx.feature-conventions` (adds `:api` + `:core`).
- **Version catalog** `gradle/libs.versions.toml` pins Spigot/BungeeCord/Netty + Shadow
  + ProGuard.
- `settings.gradle.kts` includes `:api`, `:core`, `:features:<19>`, `:plugin`.

### Module structure (~22 modules)
- `:api` — stable public interfaces other plugins compile against (economy, chat,
  tablist, scoreboard, joinleave, hub, jail, moderation, vanish, afk, teleport, …).
- `:core` — platform-agnostic framework (module system, command/gui/storage/locale
  frameworks, the `Platform`/`Scheduler`/`ComponentBridge` abstraction seam +
  `PlatformResolver`, stateless `util`).
- `:features:*` — one Gradle subproject per feature; each owns its `*Module`, services,
  commands, listeners, GUIs. Acyclic feature DAG: `hub→staff`, `warp→staff`,
  `staff→{moderation,world}`, `playerinfo→tablist`.
- `:plugin` — thin `OBX` bootstrap + resources; depends on core + every feature; Shadow
  aggregator + ProGuard.

### Decoupling (prerequisite, also new architecture)
- `core/ObxPlugin` interface (extends Bukkit `Plugin`) — features program against it,
  never the concrete `OBX` (which lives in `:plugin`). 257+ references swapped.
- 26 feature-private services routed through the `ServiceRegistry`; 14 cross-feature
  services extracted to `:api` interfaces (full interfaces for clean services;
  minimal `…Api` interfaces for Moderation/Hub/Jail/Vanish to keep internal model
  types out of `:api`). `:core` ends up with **zero** feature imports.
- Platform abstraction: reflective `SchedulerAdapter` now implements a `Scheduler`
  interface; `PlatformInfo` implements `Platform`; `ComponentBridge` text seam added.
  The proven reflective impl is retained so one jar still spans Spigot 1.8.8 → Folia
  1.21+; native per-platform impls are additive drop-ins behind the interfaces.

### Packaging
- `:plugin:shadowJar` → `OBX-1.0.0-beta-b1-unobf.jar` (merged, unobfuscated).
- `:plugin:proguard` (proguard-gradle 7.5.0, `proguard.pro` + `java.base.jmod` +
  provided APIs as library jars) → `OBX-1.0.0-beta-b1.jar` (obfuscated) + mapping.

### Resources / locale
- `plugin.yml` de-Mavenized (literal `${plugin.name}`/`${plugin.fullversion}` →
  `OBX` / `1.0.0-beta-b1`, since Gradle doesn't filter Maven placeholders);
  `folia-supported: true` + `api-version: '1.13'` retained.
- **EN + DE + ES**: added `LanguageRegistry.ES`. ES uses English fallback by
  construction (`defaults()` layers `MessageDefaults.spanishOverrides()` on top of
  EN), with the universal permission/player-only messages translated to Spanish as a
  starter. Generates `lang/es.yml`.

### Files (representative)
- `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`,
  `buildSrc/**`, `build.gradle.kts` (root) + per-module `build.gradle.kts`
- `core/.../ObxPlugin.java`, `core/.../platform/{Platform,scheduler/Scheduler,scheduler/CancellableTask,text/ComponentBridge}.java`, `core/.../bootstrap/PlatformResolver.java`
- `api/.../*` (14 service interfaces), `features/*/.../*Module.java`
- `plugin/.../OBX.java`, `plugin/src/main/resources/plugin.yml`, `proguard.pro`

## Verification
- `./gradlew build` → **BUILD SUCCESS**; 20 unit tests pass; both jars produced
  (`OBX-1.0.0-beta-b1.jar` 0.93 MB obfuscated + `-unobf.jar` 1.48 MB). The obfuscated
  jar contains `plugin.yml`, the kept `OBX` class, `systems/*` configs, and 574
  repackaged classes.

## Decisions & caveats
- **No live-server runtime QA here.** Verified by compile + the unit suite + static
  review + jar inspection. A server smoke test on Spigot/Paper/Folia is recommended
  before release (esp. the scheduler seam and module enable/disable).
- **Platform split is interface-first, not native-API-per-module.** The example's
  `platform-{bukkit,paper,folia}` native modules would trade away the single-jar
  1.8→26.1 compat the product advertises; the reflective impl behind the `Scheduler`
  interface preserves it. Native impls can be added later without touching callers.
- **No NMS modules.** The current code has no NMS-against-mappings layer (it uses
  reflection); `nms/*` modules were intentionally not scaffolded.
- **Spanish is English-fallback + a translated starter set** (the 1087 `def()` keys
  are not machine-translated wholesale). Full ES coverage is a translator task —
  add to `spanishOverrides()` or edit `lang/es.yml`.
- **`paper-plugin.yml` not added** — it needs a Paper plugin-loader bootstrap class and
  changes plugin loading; deferred to avoid an unverifiable behavior change.
  `folia-supported: true` already provides Folia support.
- A split package (`core.command`, `core.gui.main`) spans `:core` + `:plugin` for the
  three relocated aggregator classes — fine on the classpath/Shadow (no JPMS).

## Suggested Commit Message
```
Migrate to a package-by-feature Gradle multi-module build

Retire Maven; add Gradle wrapper + buildSrc conventions + version catalog; split
into :api/:core/:features:*/:plugin; decouple features behind ObxPlugin + :api
interfaces; Shadow-merge then ProGuard-obfuscate into both jars; add ES locale
(English fallback). gradle build green, 20 tests, both jars.
```
