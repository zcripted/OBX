# 🔐 String-Encryption Obfuscation Layer (per-build rotation + varied decoders) + Permission-Cycle Crash Fix

> Adds a post-ProGuard build step that encrypts every string literal in the shipped
> jar's bytecode, so the constant pool no longer reveals messages, SQL, permission
> nodes, NamespacedKeys, or webhook/license strings. Generated runtime decoders reverse
> each literal to its byte-identical original, so reflection / permission checks / config
> reads keep working. **Hardened against bulk recovery:** a per-build master key, a
> per-string salt, and a pool of varied decoder methods (different algorithms, names,
> and holder classes each build) mean no single "unwrap" script can restore the pool.
> Booting the obfuscated jar on a real Paper 1.21.4 server (the first time the jar has
> been runtime-tested, not just built) surfaced a **pre-existing StackOverflow crash**
> from a self-referential `plugin.yml` permission — fixed here.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 9:50 AM EST |
| **Last Updated** | 2026-06-07 6:35 PM EST |
| **Author** | zcripted |
| **Scope** | Build pipeline (obfuscation), plugin.yml permissions, verification harness |
| **Files changed** | 5 (1 new task, source decoder removed) |
| **Categories** | Build · Obfuscation · Fix (crash) · Permissions · Verification |
| **Verification** | ✅ `gradlew build` green · 25,390 literals encrypted across 439 classes via 9 varied decoders in 3 holders (per-build key); all sites round-trip-verified against the shipped decoders · obfuscated jar **enables clean on Paper 1.21.4** |

---

## 📋 Summary (patch notes)

- **Jar-size fix: ciphertext now stays in the Latin-1 char range (−27% shipped jar).**
  The first cipher mapped every char across the full 16-bit range; in the class file's
  modified-UTF-8 constant pool those chars cost 3 bytes each *and* the high-entropy
  output defeats the jar's zip compression — the shipped jar ballooned to 2,941 KB
  (vs 1,990 KB unobf). The transform now runs over the plaintext's **UTF-8 bytes** with
  8-bit keys, carrying each cipher byte as one char ≤ 0xFF (1–2 bytes in MUTF-8); the
  generated decoders reverse into a `byte[]` and decode `new String(buf, UTF_8)`.
  Literals that wouldn't survive a UTF-8 round trip (lone surrogates) are left
  unencrypted rather than corrupted. Shipped jar: **2,941 KB → 2,160 KB**; all
  25,836 sites round-trip-verified against the shipped decoders; obfuscated jar
  re-verified booting clean on Paper 1.21.4 via `runServerObf`.
- **String encryption added as a final obfuscation layer.** ProGuard already renames
  classes/methods and flattens packages; it does *not* (free version) hide string
  literals. A new `encryptStrings` Gradle step runs after ProGuard and rewrites every
  method-body `LDC "plaintext"` into `LDC "<ciphertext>"; SIPUSH <salt>; INVOKESTATIC <decoder>`.
  At runtime the decoder reverses it to the identical value (interned), so reflection,
  permission checks, config reads, and SQL are unaffected. Verified: code-only literals
  (`economy_server_account`, `SELECT …`, `economy.bank.tiers`, `AH_LISTING_FEE`, …) are
  present in the unobf jar and **absent** from the shipped jar.
- **Hardened against bulk recovery (per-build rotation + varied decoders).** Instead of one
  fixed XOR decoder, the task generates a per-build pool of decoder *variants* — distinct
  reversible algorithms (XOR / ADD / SUB, each with its own key schedule), as distinct
  methods spread across several generated holder classes whose names blend with ProGuard's
  output. Each call site is assigned a random variant **and** a random per-string salt; the
  effective key is `f(masterKey, salt, charIndex)` with `masterKey` random per build. So
  different strings use different keys, different sites may use different algorithms, and
  every build changes the keys, constants, decoder names, and bytecode. A single universal
  unwrapper can't restore the pool — an attacker must reverse every variant and the salt
  schedule for that specific build.
- **Honest scope:** still obfuscation, not cryptography — the keys live in the jar, so it
  raises the cost of bulk recovery but is not tamper-proof (true of all client-side
  protection). Annotation values, field `ConstantValue`s, and `invokedynamic` string-concat
  constants are deliberately left untouched so `@EventHandler` discovery and string
  concatenation can't break. **Every encrypted site is round-trip-verified by loading the
  actually-shipped decoders from the output jar and invoking them** (25,390/25,390) before
  the jar is accepted — which is what makes the hand-generated decoder bytecode safe to trust.
- **Crash fix (pre-existing):** `obx.shop.category.*` in `plugin.yml` listed *itself* as a
  child permission. Paper's `PermissibleBase.calculateChildPermissions` recurses into
  children, so a self-child = infinite recursion → `StackOverflowError` on plugin enable.
  The plugin could not actually load on Paper. Removed the self-referential child from it
  and from the new `obx.bank.tier.*`. This was only caught because we **booted the jar**;
  `gradlew build` (the prior testing bar) never exercises plugin load.
- **New verification task:** `./gradlew runServerObf` boots Paper with the obfuscated +
  string-encrypted jar (the normal `runServer` uses the readable unobf jar) — the real
  runtime proof that obfuscation didn't break class loading or the reflective
  version-compat paths. Confirmed: `Enabling OBX v1.0.0` → platform detected
  `[folia-scheduler] [adventure]` → SQLite + shop + 100 enchants + holograms loaded →
  `Done (9.904s)!`.

---

## 🔧 Changes

- **NEW** [buildSrc/src/main/kotlin/obx/build/StringEncryptTask.kt](../../../buildSrc/src/main/kotlin/obx/build/StringEncryptTask.kt)
  — ASM task: rewrites method-body string LDCs with a random variant + per-string salt,
  generates the per-build pool of varied decoder holder classes (ASM, `COMPUTE_FRAMES`),
  injects them, and verifies every site by loading the shipped decoders and invoking them.
  Fails the build on any mismatch.
- [buildSrc/build.gradle.kts](../../../buildSrc/build.gradle.kts) — ASM 9.7 (`asm`, `asm-tree`).
- [plugin/build.gradle.kts](../../../plugin/build.gradle.kts) — ProGuard now writes an
  intermediate `build/proguard/OBX-<ver>-obf.jar`; `encryptStrings` produces the final
  `build/libs/OBX-<ver>.jar`; `build` depends on it; added the `runServerObf` task.
- [proguard.pro](../../../proguard.pro) — no decoder keep-rule needed (decoders are
  generated and injected *after* ProGuard, with non-colliding names).
- *Removed* `plugin/src/main/java/dev/zcripted/obx/Strings.java` — the old single fixed-key
  decoder; superseded by the generated per-build variant pool.
- [plugin/src/main/resources/plugin.yml](../../../plugin/src/main/resources/plugin.yml) —
  removed the self-referential `children` from `obx.shop.category.*` and `obx.bank.tier.*`
  (the StackOverflow cause).

## ✅ Verification
- `.\gradlew.bat build` — green; `encryptStrings` reports 25,390 literals across 439 classes
  via 9 varied decoders in 3 holders (per-build key), and verifies all 25,390 sites round-trip
  through the shipped decoders.
- Plaintext code literals confirmed gone from the shipped jar (present in unobf); the old
  single decoder class is gone and the generated holders blend with ProGuard's root-level output.
- `.\gradlew.bat runServerObf` — obfuscated jar enables clean on Paper 1.21.4; reflective
  platform/Adventure detection and all modules load; `Done (9.481s)!`.

## ⚠️ Notes / follow-ups
- "Per-build" rotation happens whenever `encryptStrings` actually runs (clean build or any
  change to the obfuscated input) — i.e. every release build. Unchanged inputs reuse the
  cached jar rather than re-randomising; `gradlew clean` forces fresh keys.
- String-concat literals (Java `invokedynamic`) remain in the clear by design; if those
  matter, switching the compiler to `-XDstringConcat=inline` would route them through LDC.
- Next lever if needed: control-flow obfuscation (Skidfuscator/Bozar) to degrade decompiler
  output of the *logic* — the string layer hides data, not algorithms.
