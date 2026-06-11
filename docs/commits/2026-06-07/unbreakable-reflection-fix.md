# 🔨 /unbreakable Fix — Reflection Always Failed, Flag Was Never Applied

> `/unbreakable` replied "Unbreakable flag not supported on this server" on **every**
> server version. The reflective lookup targeted the CraftBukkit implementation class
> (`CraftMetaItem`), which is package-private — `Method.invoke` threw
> `IllegalAccessException`, both the modern and the Spigot-legacy fallback were silently
> swallowed, and the command always reported "unsupported". Since the compile API is
> Spigot 1.12.2 and `ItemMeta#setUnbreakable` exists since 1.11, the reflection was
> unnecessary: the flag is now set with a direct API call, with a direct
> `ItemMeta.Spigot` fallback for 1.8.8–1.10 runtimes.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-07 4:20 PM EST |
| **Last Updated** | 2026-06-07 4:20 PM EST |
| **Author** | zcripted |
| **Scope** | Item feature — /unbreakable command |
| **Files changed** | 1 code + 2 docs |
| **Categories** | Fix · Commands |
| **Verification** | ✅ `gradlew build` green (tests + shadowJar + ProGuard + encryptStrings, both jars) |

---

## 📋 Summary (patch notes)

- **Fixed: `/unbreakable` now works.** It previously failed with "Unbreakable flag not
  supported on this server" no matter the server version. The toggle now applies (and
  reads back) the unbreakable flag correctly on 1.11+ via the Bukkit API, and on
  1.8.8–1.10 via the legacy `ItemMeta.Spigot` accessor.

## 🐛 Root cause

`isUnbreakable`/`applyUnbreakable` reflected via `meta.getClass().getMethod(...)`.
`meta.getClass()` is the server's `CraftMetaItem` (or a subclass) — a **package-private**
CraftBukkit class. `Class.getMethod` happily returns the public method, but
`Method.invoke` then performs an access check against the *declaring class* and throws
`IllegalAccessException` for callers outside its package. Both the modern path and the
`spigot()` fallback hit the same wall (the `spigot()` method is also declared on
`CraftMetaItem`), every exception was swallowed, and the command concluded "unsupported".

The classic fix is to look the method up on the public **interface** (`ItemMeta.class`)
instead of the implementation class — but here even that is overkill: the project
compiles against Spigot **1.12.2**, and `ItemMeta#isUnbreakable`/`setUnbreakable` are
part of the API since **1.11**, so direct calls compile and link on every supported
modern server. Only the 1.8.8–1.10 fallback needs the deprecated-but-present
`meta.spigot().setUnbreakable(...)` (a `NoSuchMethodError` catch routes there).

> ⚠️ **Heads-up for a future pass:** the same `meta.getClass().getMethod(...)` pattern
> exists in the enchant module (`Glow.setEnchantmentGlintOverride`,
> `EnchantItems.setCustomModelData`, `EnchantGuideBook.setGeneration`, …) and likely
> fails silently the same way wherever the declaring CraftBukkit class is
> package-private. Not touched in this commit.

## 🔧 Changes (newest at top → oldest)

### Item feature
- [features/item/src/main/java/dev/zcripted/obx/feature/item/command/UnbreakableCommand.java](../../../features/item/src/main/java/dev/zcripted/obx/feature/item/command/UnbreakableCommand.java)
  — `isUnbreakable()`/`applyUnbreakable()` now call `meta.isUnbreakable()` /
  `meta.setUnbreakable(value)` directly (API ≥ 1.11), falling back to
  `meta.spigot()` on `NoSuchMethodError` for 1.8.8–1.10; removed the dead
  `java.lang.reflect.Method` machinery.

### Docs
- [docs/commits/README.md](../README.md) — index entry.
- [docs/changes/2026-06-07---unbreakable-command-fix.md](../../changes/2026-06-07---unbreakable-command-fix.md) — change file.

## ✅ Verification
- `.\gradlew.bat build` green — tests pass, both jars produced
  (`OBX-1.0.0-unobf.jar`, `OBX-1.0.0.jar`). The build's deprecation `Note:` on this file
  is expected (the intentional `ItemMeta.Spigot` legacy fallback).
