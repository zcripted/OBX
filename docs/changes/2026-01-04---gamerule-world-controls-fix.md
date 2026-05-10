**Created:** 2026-01-04 6:16 pm
**Last Updated:** 2026-01-04 6:16 pm

## Compatibility
- Added reflective gamerule access with safe fallbacks to avoid unknown gamerule errors across 1.12.x-1.21.x. (`src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/AdminSubMenu.java`)

## GUI
- Marked unsupported gamerules as unavailable in the admin gamerule editor to prevent invalid toggles. (`src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/AdminSubMenu.java`)
- Updated daylight cycle toggles to use the safe gamerule accessors used by the GUI. (`src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/AdminSubMenu.java`)

## Documentation
- Logged change summary entry. (`docs/changes/2026-01-04---gamerule-world-controls-fix.md`)
