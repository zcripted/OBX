# ProGuard jar-size reduction (lever ① kept; lever ② explored + reverted)

■ **Created:** 2026-06-02 7:48 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 7:48 pm (America/Detroit)

Analyzed the obfuscated jar (94% bytecode, 6% resources) and applied the one
config-only lever that meaningfully shrinks it.

## Build (ProGuard) — lever ① APPLIED

The previous `-keep class * implements …` rules preserved the full names of 261
of 537 classes (every Listener / CommandExecutor / InventoryHolder) and blocked
their renaming + merging. Those classes are all instantiated directly and matched
by `instanceof` (which ProGuard renames consistently), and Bukkit never looks
them up by name — so only their members need keeping. Changes:

- `-keep class * implements {Listener,CommandExecutor,InventoryHolder}` →
  `-keepclassmembers …` (allow class-name obfuscation + merging).
- Removed `!class/merging/*` from `-optimizations`.
- Added `-repackageclasses ''` (collapse package paths in the constant pool).
- Trimmed `-keepattributes` to `*Annotation*,Signature,InnerClasses` (dropped the
  runtime-unneeded `EnclosingMethod`,`Exceptions`).
- File: `proguard.pro`

Safety: `HelpGuiMenu`'s command-package probe (`getClass().getPackage()`) is an
explicit fallback — its category MAP is authoritative precisely *because* the
package is renamed under obfuscation — so renaming command classes is safe. No
code reflects on our own class names for behaviour.

## Messages — lever ② EXPLORED then REVERTED

Externalizing `MessageDefaults`' ~3,500 embedded strings into bundled `/lang/`
YAML was measured to be **net-zero on the shipped (obfuscated) jar** (the
compressed YAML ≈ the compressed class constant pool) and only −1.4% on the
unobfuscated jar, while adding a runtime YAML-parse step on a core path. Not
worth it — reverted to keep the defaults in code as the single source of truth.

- During the revert, a `git checkout` mistake clobbered the (uncommitted,
  rebranded) in-code `MessageDefaults`. It was faithfully **regenerated from the
  round-trip-verified resource data** and proven byte-for-byte equal (content +
  key order, EN+DE, defaults + section comments) before the resources were
  deleted. The regenerated file is data-identical but structurally flatter than
  the original (flat `def(key,en,de)` calls + `list()/map()` builders instead of
  the old `usageBox()/addInteractive()` helpers), which costs ~11 KB on the
  obfuscated jar vs a perfectly-reconstructed original.
- File: `src/main/java/dev/zcripted/obx/language/MessageDefaults.java` (1157 lines)
- No `/lang/` resources, no pom resource entries, no runtime SnakeYAML dependency.

## Measured results

| Artifact | Baseline | ①+② (interim) | **Final (① only)** |
|---|---|---|---|
| Obfuscated jar (ships) | 1,011,533 | 928,433 | **939,643 (−7.1%)** |
| Unobfuscated jar | 1,459,362 | 1,438,711 | 1,455,382 (−0.3%) |

The honest ceiling: the jar is ~530 classes of real code plus irreducible EN+DE
message text — **there is no "tremendous" reduction available** without cutting
features. ①'s ~7% is the realistic config-only win.

## Verification

- `./maven/bin/mvn clean package` → **BUILD SUCCESS**; 14 tests, 0 failures.
- In-code↔resource byte-equality confirmed before resource removal (temporary
  `VerifyInCode` test, since removed).

## Follow-up / caveat

- ⚠️ The loosened keeps rename listeners/commands/holders in the **obfuscated**
  jar. This was validated statically (consistent `instanceof`, the HelpGuiMenu
  fallback) but **should get a runtime smoke test** on a server before shipping
  the obfuscated artifact. The unobfuscated jar is unaffected by ①.

## Suggested Commit Message

```
Build(proguard): loosen keeps + repackage to shrink obfuscated jar ~7%

Switch Listener/CommandExecutor/InventoryHolder -keep to -keepclassmembers,
re-enable class merging, repackage classes, trim keepattributes. Keep message
defaults in code (externalization measured net-zero on the shipped jar).
```
