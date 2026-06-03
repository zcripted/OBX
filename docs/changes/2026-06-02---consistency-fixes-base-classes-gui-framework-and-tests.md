# Consistency fixes, command base classes, GUI framework, and a test suite

■ **Created:** 2026-06-02 6:15 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 6:15 pm (America/Detroit)

Acts on the findings of the codebase health assessment. Two functional rebrand
leftovers are fixed, the missing permission tree is declared, the command and
GUI-holder layers gain shared base classes, the 1,400-line admin-menu god class
is trimmed, privileged menus re-check permission on click, and the project gets
its first automated tests.

---

## Bug fixes (functional)

- **Bundled resource pack never shipped.** `AutoResourcePackManager` loads
  `bundled-resourcepack/obx-core-gui-pack.zip`, but the committed asset was still
  named `sf-core-gui-pack.zip`, so `getResource(...)` returned `null` and the pack
  was silently absent. Renamed the asset to match. Verified the clean-build jar now
  contains exactly `bundled-resourcepack/obx-core-gui-pack.zip`.
  - `src/main/resources/bundled-resourcepack/obx-core-gui-pack.zip` (renamed from `sf-core-gui-pack.zip`)

## Permissions

- Declared the permission nodes that were live in code but absent from
  `plugin.yml` (Bukkit was treating them as op-only with no defaults). Added the
  base nodes and their variants, and listed them under the `obx.*` master grant:
  `obx.afk` (`true`), `obx.afk.others` (`op`), `obx.afk.exempt` (`false`),
  `obx.afk.exempt-kick` (`false`), `obx.flyspeed` / `obx.flyspeed.others` (`op`),
  `obx.clearinv` / `obx.clearinv.others` (`op`).
  - `src/main/resources/plugin.yml`

## Rebrand cleanup

- Removed the stale `/sf` and `/sfc` command mappings from the help-GUI category
  table (clean-break rebrand had no back-compat aliases).
  - `src/main/java/dev/zcripted/obx/gui/player/HelpGuiMenu.java`
- Deleted the orphaned IDE module file `sf-core-parent.iml` (superseded by `obx.iml`).
- Added `.gitattributes` (`* text=auto` + binary rules) to stop the CRLF/LF churn
  that was marking most files modified on every checkout.
  - `.gitattributes` (new)

## Commands (base classes — reduce god-routers / boilerplate)

- Added `AbstractObxCommand` (holds `plugin` + `languages`; `requirePlayer` and
  `requirePermission` helpers replace the copy-pasted player-only / no-permission
  guard clauses) and `PlayerActionCommand` (template for the common player-only,
  single-permission, self-acting shape).
  - `src/main/java/dev/zcripted/obx/command/AbstractObxCommand.java` (new)
  - `src/main/java/dev/zcripted/obx/command/PlayerActionCommand.java` (new)
- Migrated the clear self-action commands onto the template (behaviour unchanged):
  - `src/main/java/dev/zcripted/obx/command/admin/KillCommand.java`
  - `src/main/java/dev/zcripted/obx/command/utility/HealCommand.java`
  - `src/main/java/dev/zcripted/obx/command/utility/FeedCommand.java`
  - `src/main/java/dev/zcripted/obx/command/utility/CraftCommand.java`
  - `src/main/java/dev/zcripted/obx/command/utility/EnchantCommand.java`

## GUIs (framework + decomposition)

- Added a shared `MenuHolder` base that owns the inventory plumbing every holder
  used to repeat. Migrated all nine `gui/` holders to extend it (subclasses keep
  only their unique state; the subclass type is still the listener `instanceof`
  key).
  - `src/main/java/dev/zcripted/obx/gui/MenuHolder.java` (new)
  - `gui/admin/AdminMenuHolder.java`, `gui/admin/InvSeeMenuHolder.java`,
    `gui/admin/StaffActionMenuHolder.java`, `gui/admin/StaffMenuHolder.java`,
    `gui/player/MainMenuHolder.java`, `gui/player/HelpGuiHolder.java`,
    `gui/player/ServerSelectorHolder.java`, `gui/player/WarpMenuHolder.java`,
    and the nested `AdminSubMenu.Holder`
- Decomposed the 1,471-line `AdminSubMenu` god class: extracted its state-free
  rendering helpers (`createMenuItem`, `createPane`, `place`, `loreLines`,
  `fillWithFiller`, the back/close item builders, `resolveMaterial`, `statusLine`,
  `valueLine`) into a focused `AdminMenuRender` toolkit, pulled in via static import
  so existing call sites are unchanged. `AdminSubMenu` shed ~135 lines.
  - `src/main/java/dev/zcripted/obx/gui/admin/AdminMenuRender.java` (new)
  - `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`

## Permissions (defense-in-depth on click)

- The admin menu / admin sub-menus and the staff menu now re-verify their gating
  permission (`obx.admin.menu` / `obx.staff.menu`) on every click — covering
  revocation mid-session or an open inventory being handed off — instead of
  trusting the open-time check. The public main menu and the view-only `/invsee`
  mirror carry no privileged click action, so they are intentionally left ungated.
  - `src/main/java/dev/zcripted/obx/listener/menu/MainMenuListener.java`
  - `src/main/java/dev/zcripted/obx/listener/menu/StaffMenuListener.java`

## Tests (new — first automated coverage)

- Wired JUnit 5 (Jupiter, test scope) + `maven-surefire-plugin` into the build.
  Tests run on `mvn test`; the documented `-DskipTests` package build is unaffected.
  Targets pure-logic units that need no running server:
  - `MessageDefaultsTest` — enforces EN/DE key parity, no null/empty defaults, and
    `requiredKeys()` consistency (guards the CLAUDE.md bilingual mandate at build time).
  - `LanguageRegistryTest` — `/language` & `/sprache` input parsing (codes, names,
    aliases, whitespace, reject-unknown).
  - `PlaceholdersTest` — substitution-map stringification, null→"", non-destructive merge.
  - `pom.xml`
  - `src/test/java/dev/zcripted/obx/language/MessageDefaultsTest.java` (new)
  - `src/test/java/dev/zcripted/obx/language/LanguageRegistryTest.java` (new)
  - `src/test/java/dev/zcripted/obx/util/text/PlaceholdersTest.java` (new)

## Docs

- Added the `/afk`, `/flyspeed`, `/clearinv` rows (including the `.others` /
  `.exempt` variants) to the Utility table.
  - `docs/information/COMMANDS+PERMISSIONS.md`

---

## Verification

- `./maven/bin/mvn test` → **14 tests, 0 failures, 0 skipped**.
- `./maven/bin/mvn -DskipTests clean package` → **BUILD SUCCESS**; both
  `obx-1.0.0-SNAPSHOT.jar` and `obx-1.0.0-SNAPSHOT-obfuscated.jar` produced.
- Confirmed the clean jar bundles exactly `bundled-resourcepack/obx-core-gui-pack.zip`.

## Assumptions

- `obx.afk` defaults to `true` (self-AFK is a baseline player action); all other new
  nodes default to `op`, and the two `exempt` nodes default to `false` (must be granted).
- Command-base and holder-base migrations were applied to the clearly-conforming
  subset; the bases are now established for the remaining commands/holders to adopt
  incrementally without a risky big-bang.
- The `message` and `enchant.gui` holders were left on the old pattern to respect
  module boundaries (they live outside the core `gui/` package).

## Suggested Commit Message

```
Fix(rebrand+perms): ship resourcepack, declare AFK/flyspeed/clearinv nodes

Add command + menu-holder base classes, extract AdminMenuRender from the
admin god class, re-check admin/staff perms on click, and add the first
JUnit suite (message parity, language parsing, placeholders).
```
