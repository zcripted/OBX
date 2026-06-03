# Build: Reliable Obfuscated vs Unobfuscated JARs

■ **Created:** 2026-05-25 11:26 am
■ **Last Updated:** 2026-05-25 11:26 am

## Summary

Fixes the build so the two output JARs are always what their names say:

- `OBX-1.0.0-SNAPSHOT.jar` → **obfuscated** (ProGuard: shrunk + renamed), ~386 KB.
- `OBX-1.0.0-SNAPSHOT-unobf.jar` → **unobfuscated** full build (no ProGuard), ~566 KB.

Previously, on an **incremental (non-`clean`) build**, both JARs came out
obfuscated and identically sized.

## Root Cause

The `package` phase runs three steps in order: `maven-jar-plugin:jar` (writes
the plain `OBX.jar`) → antrun copy (`OBX.jar` → `OBX-unobf.jar`) →
ProGuard (`-injars OBX-unobf.jar -outjars OBX.jar`, overwriting the
plain jar with the obfuscated one).

`maven-jar-plugin` defaults to `forceCreation=false`, so on a rebuild where the
compiled classes look unchanged it **skips** regenerating the jar. After a prior
build, the file sitting at `OBX.jar` is the *obfuscated* output from that
run. So the incremental sequence became:

1. `jar:jar` — skipped → the obfuscated jar stays in place.
2. antrun copy → `OBX-unobf.jar` becomes a copy of the **obfuscated** jar.
3. ProGuard re-obfuscates it back into `OBX.jar`.

Result: both JARs obfuscated, same size. A clean build happened to work only
because `jar:jar` had no existing jar to skip.

Secondary issue: the `verify`-phase antrun step **deleted** `*-unobf.jar`, so
`mvn verify` / `mvn install` removed the unobfuscated deliverable entirely.

## Changes

- `pom.xml`
  - `maven-jar-plugin`: added `<forceCreation>true</forceCreation>` so the plain
    jar is **always** regenerated before the copy + ProGuard steps, regardless of
    incremental up-to-date checks. This guarantees `*-unobf.jar` is the freshly
    built, unobfuscated jar on every build (clean or incremental).
  - `maven-antrun-plugin` (`verify` execution): removed the `delete` of
    `*-unobf.jar` so the unobfuscated build is kept as a deliverable. The harmless
    `original-*.jar` scratch cleanup and the completion echo are retained (echo
    now names both produced artifacts).

## Verification

- Two consecutive **incremental** `package` builds (no `clean`) — the scenario
  that previously produced identical jars — now both yield:
  - `OBX-1.0.0-SNAPSHOT.jar` = 386,762 bytes, inner classes `…$a/$b/$c/$d`
    (obfuscated).
  - `OBX-1.0.0-SNAPSHOT-unobf.jar` = 566,653 bytes, inner classes
    `…$EventReflection/$ProfileFactory/$ProfileSource` (full source names).
- `mvn -DskipTests verify` now completes with both jars present (the unobf jar is
  no longer deleted).

## Suggested Commit Message

```
Build (proguard): Force plain-jar regen so incremental builds keep obf/unobf jars distinct; stop deleting -unobf deliverable
```
