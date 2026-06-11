# /unbreakable Fix — Flag Was Never Applied (Reflection Access Failure)

■ **Created:** 2026-06-07 4:20 pm

■ **Last Updated:** 2026-06-07 4:20 pm

`/unbreakable` reported "Unbreakable flag not supported on this server" on every server
version. Full breakdown in the commit log:
[docs/commits/2026-06-07/unbreakable-reflection-fix.md](../commits/2026-06-07/unbreakable-reflection-fix.md)

## Categories

### Commands (Fix)
- **Fix:** the command reflected on `meta.getClass()` — the package-private CraftBukkit
  `CraftMetaItem` — so `Method.invoke` threw `IllegalAccessException` on every server,
  both fallbacks were swallowed, and the command always replied "unsupported".
- Now calls `ItemMeta#isUnbreakable`/`#setUnbreakable` directly (in the compile API since
  1.11 / Spigot 1.12.2), with a `meta.spigot()` fallback for 1.8.8–1.10 runtimes.
- `features/item/src/main/java/dev/zcripted/obx/feature/item/command/UnbreakableCommand.java`

## Verification
- `.\gradlew.bat build` green — tests pass; both jars produced.

## Suggested Commit Message
```
Fix (item): /unbreakable never applied — reflect-on-impl-class IllegalAccessException; use direct ItemMeta API with 1.8 Spigot fallback
```
