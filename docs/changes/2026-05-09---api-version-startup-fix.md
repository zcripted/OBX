# Skip CraftLegacy startup init by declaring api-version

■ **Created:** 2026-05-09 12:59 am

■ **Last Updated:** 2026-05-09 12:59 am

## Symptom

Server boot stalled for 15–30+ seconds at:

```
[WARN]: [org.bukkit.craftbukkit.legacy.CraftLegacy] Initializing Legacy Material Support. Unless you have legacy plugins and/or data this is a bug!
[WARN]: Legacy plugin SF-Core v1.0.0-SNAPSHOT does not specify an api-version.
```

## Root cause

Without an `api-version` field in `plugin.yml`, Paper / Spigot 1.13+ classifies
the plugin as "legacy" (i.e. written before the 1.13 material flattening) and
bootstraps `org.bukkit.craftbukkit.legacy.CraftLegacy`. That class builds a
huge enum-to-enum translation table covering every pre-1.13 material/data
combination so that legacy plugins can keep calling
`Material.matchMaterial("WOOL")` with implicit translation. The table init is
JIT-cold and synchronous — that's the 15–30s stall.

SF-Core never relied on the legacy translator: every ambiguous material lookup
already cascades through name-fallback chains
(e.g. `WarpMenuStyling.resolveMaterial("GRAY_STAINED_GLASS_PANE",
"BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GLASS_PANE", "THIN_GLASS")`),
so the legacy init was paying a heavy cost for a feature we don't use.

## Fix

Declare `api-version: '1.13'` in `src/main/resources/plugin.yml`.

- 1.13+ servers see the field, classify SF-Core as a modern plugin, and skip
  the `CraftLegacy` init entirely.
- 1.8 – 1.12 servers don't recognise the field and silently ignore it, so the
  single-JAR cross-version target is preserved.
- 1.13 is intentionally the lowest non-legacy value. Setting it higher (e.g.
  `1.21`) would refuse to load on every server below that minor.

## Categories

### Config

- `src/main/resources/plugin.yml` — added `api-version: '1.13'` with a comment
  block explaining what it does, why 1.13 specifically, and that legacy
  servers ignore the field.

## Files Modified

- `src/main/resources/plugin.yml`

## Suggested Commit Message

```
Fix (startup): declare api-version 1.13 to skip CraftLegacy init
```
