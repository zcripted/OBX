**Created:** 2026-01-04 6:21 pm
**Last Updated:** 2026-01-04 6:21 pm

## Compatibility
- Added internal daylight-cycle fallback runner when doDaylightCycle is not available on the server. (`src/main/java/dev/zcripted/obx/util/control/DaylightCycleFallback.java`)

## GUIs
- Routed daylight-cycle reads/writes through the fallback when the server gamerule is missing. (`src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`)

## Documentation
- Logged change summary entry. (`docs/changes/2026-01-04---daylightcycle-fallback.md`)
