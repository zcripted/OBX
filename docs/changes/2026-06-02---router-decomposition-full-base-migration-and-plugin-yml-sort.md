# Router decomposition, full base-class migration, and alphabetized plugin.yml

■ **Created:** 2026-06-02 7:00 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 7:00 pm (America/Detroit)

Follow-up to `2026-06-02---consistency-fixes-base-classes-gui-framework-and-tests.md`.
Completes the base-class migrations started there, alphabetizes `plugin.yml`,
renames the bundled GUI pack, and decomposes the two large command routers.

---

## Assets / automation

- Renamed the bundled GUI resource pack `obx-core-gui-pack.zip` → `obx-gui-pack.zip`
  and updated the loader reference. Verified the clean-build jar bundles exactly
  `bundled-resourcepack/obx-gui-pack.zip`.
  - `src/main/resources/bundled-resourcepack/obx-gui-pack.zip` (renamed)
  - `src/main/java/dev/zcripted/obx/platform/bukkit/resourcepack/AutoResourcePackManager.java`
  - (The separate served/data-folder pack name `obx-core-pack.zip` was left as-is — not part of this request.)

## Config organization

- Alphabetized **all** of `plugin.yml`: every `commands:` entry (121) and every
  `permissions:` node (143) sorted A–Z by key, plus the child-grant lists inside
  the wildcard nodes (e.g. `obx.*`, `obx.admin.*`). Wildcard parents sort just
  before their siblings (codepoint order), so `obx.admin.*` precedes
  `obx.admin.menu`. Verified the key set and full line multiset are unchanged —
  pure reordering, nothing added or dropped.
  - `src/main/resources/plugin.yml`

## Commands — base-class migration completed

- Migrated the remaining **103** command executors from `implements CommandExecutor`
  to `extends AbstractObxCommand` (preserving `implements TabCompleter` where
  present): constructors now call `super(plugin)` and the duplicated
  `private final Main plugin` / `private final LanguageManager languages` fields and
  their assignments are dropped in favour of the inherited `protected` fields. With
  the 5 migrated in the previous change, **all 108** command classes now share the
  base. Behaviour unchanged (the codemod moved no logic). One incidental fix:
  `ObxEnchantCommand` had a private `requirePermission` identical to the base
  method — removed it to use the inherited one.
  - All `src/main/java/dev/zcripted/obx/command/**/*Command.java` (+ enchant/hologram command packages)
  - `src/main/java/dev/zcripted/obx/enchant/command/ObxEnchantCommand.java`

## GUIs — holder-base migration completed

- Migrated the last two holders to `extends MenuHolder` (the `gui/` holders were
  done previously); these live in other modules and now share the same base.
  - `src/main/java/dev/zcripted/obx/message/InboxMenuHolder.java`
  - `src/main/java/dev/zcripted/obx/enchant/gui/EnchantMenuHolder.java`

## Routers decomposed (god-class reduction)

Pure behaviour-preserving extraction — handler methods moved **verbatim** into
cohesive package-private collaborators; the dispatch `switch`/if-chain and all
permission gating stay in the command class, which now delegates. No string
literal, language key, permission node, numeric/slot constant, or branch logic
was changed.

- **`ObxCommand`** 1031 → 188 lines, split into:
  - `ObxHelpView` — `help` / `info` / `about` / `permissions` / `commands` (+ the
    `Category` enum, `COMMANDS` list, `CommandEntry`, and all help-render helpers)
  - `ObxDiagnosticsView` — `reload` / `diagnostics` / `version` / `config` / `debug`
  - `ObxModulesView` — `updates` / `joinleave` / `joinmotd`
  - `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
  - `.../command/core/ObxHelpView.java`, `ObxDiagnosticsView.java`, `ObxModulesView.java` (new)
- **`WarpCommand`** 753 → 295 lines, split into:
  - `WarpQueryCommands` — read/navigation: `tp` / `info` / `list` / `category` /
    `categories` / `gui` (+ shared read helpers used by tab-complete)
  - `WarpAdminCommands` — mutating: `set` / `delete` / `rename` / `move` / `icon` /
    `public` (+ confirmation state)
  - `src/main/java/dev/zcripted/obx/command/teleportation/WarpCommand.java`
  - `.../command/teleportation/WarpQueryCommands.java`, `WarpAdminCommands.java` (new)

---

## Verification

- `./maven/bin/mvn test` → **14 tests, 0 failures, 0 skipped**.
- `./maven/bin/mvn -DskipTests clean package` → **BUILD SUCCESS** (both jars).
- Clean jar bundles exactly `bundled-resourcepack/obx-gui-pack.zip`.
- Decomposition behaviour checks: no `sf`/`sfcore`/`/sf` strings reintroduced in
  the new files; the `updates` `check`/`notify` tokens and every `/obx` and `/warp`
  subcommand still route; permission gating moved verbatim.

## Assumptions / notes

- "Catch regressions" was enforced structurally: verbatim method moves + green
  compile + the unit suite + a string-literal/branding audit of the new files. No
  running-server test was possible in this environment, so the decompositions were
  deliberately kept to mechanical extraction (no logic rewrites) to keep them safe.
- The codemod left a few now-unused `import …LanguageManager;` lines in migrated
  commands; these are harmless (javac/ProGuard do not fail on unused imports) and
  can be swept later if desired.

## Suggested Commit Message

```
Refactor: finish base-class migration, decompose Obx/Warp routers, sort plugin.yml

Migrate all commands onto AbstractObxCommand and both remaining holders onto
MenuHolder; split ObxCommand (1031->188) and WarpCommand (753->295) into
per-concern collaborators (verbatim moves); alphabetize all commands/permissions
in plugin.yml; rename bundled pack to obx-gui-pack.zip. Build + 14 tests green.
```
