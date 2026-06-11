# String-Encryption Obfuscation Layer + Permission-Cycle Crash Fix

■ **Created:** 2026-06-07 9:50 am

■ **Last Updated:** 2026-06-07 6:35 pm

Adds a custom post-ProGuard string-encryption build step and fixes a pre-existing
crash found by runtime-booting the obfuscated jar. Full breakdown:
[docs/commits/2026-06-07/string-encryption-obfuscation-layer.md](../commits/2026-06-07/string-encryption-obfuscation-layer.md)

## Categories

### Build / Obfuscation
- **Jar-size fix:** the cipher now runs over UTF-8 bytes with 8-bit keys so ciphertext
  chars stay ≤ 0xFF (1–2 constant-pool bytes instead of 3, and less incompressible
  entropy); shipped jar **2,941 KB → 2,160 KB (−27%)**. Decoders rebuild via
  `new String(byte[], UTF_8)`; literals that wouldn't survive a UTF-8 round trip are
  skipped instead of corrupted.
- New `encryptStrings` Gradle task (ASM) rewrites every method-body string literal to
  `LDC <ciphertext>; INVOKESTATIC Strings.d`; runtime decoder restores the identical value.
- ProGuard now outputs an intermediate jar; `encryptStrings` produces the final shippable jar.
- `buildSrc/src/main/kotlin/obx/build/StringEncryptTask.kt` *(new)*
- `plugin/src/main/java/dev/zcripted/obx/Strings.java` *(new — runtime decoder)*
- `buildSrc/build.gradle.kts` (ASM 9.7), `plugin/build.gradle.kts`, `proguard.pro`

### Fix (crash) — Permissions
- `obx.shop.category.*` (pre-existing) and `obx.bank.tier.*` listed themselves as a child
  permission → infinite recursion in Paper's `calculateChildPermissions` →
  `StackOverflowError` on plugin enable. Removed the self-referential children.
- `plugin/src/main/resources/plugin.yml`

### Verification
- New `./gradlew runServerObf` task boots Paper with the obfuscated + encrypted jar.
- Confirmed clean enable on Paper 1.21.4 (reflective platform/Adventure detection, SQLite,
  shop, enchants, holograms all load).

## Verification
- `.\gradlew.bat build` green — 25,390 literals encrypted across 439 classes.
- Plaintext code literals absent from shipped jar, present in unobf.
- Obfuscated jar enables clean on a real Paper 1.21.4 server.

## Suggested Commit Message
```
Build: add post-ProGuard string-literal encryption layer; fix self-referential plugin.yml permission StackOverflow on enable
```
