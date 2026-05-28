# about.md Command & Permission Reconciliation

■ **Created:** 2026-05-26 5:42 pm (America/Detroit)

■ **Last Updated:** 2026-05-26 5:42 pm (America/Detroit)

---

## Summary

Audited `docs/information/about.md` against the two sources of truth —
`src/main/resources/plugin.yml` and the actual `hasPermission(...)` checks /
subcommand dispatch in the command classes — and added every command and
permission that was missing. The doc had drifted: the Arcanum enchantment module
(added 2026-05-25) and several hub/vanish permissions were never documented.

Verified programmatically afterward:
- **All 44** commands declared in `plugin.yml` are now in about.md.
- **All** static permission nodes in `plugin.yml` are now in about.md (a node-set
  diff returns empty apart from grep false-positives on the main-class path and
  the website URL).
- The only doc-only node, `sfcore.warp.category.<category>`, is the legitimate
  dynamic per-category node enforced in `WarpAccess` (not a static `plugin.yml`
  entry), so it is correctly documented as dynamic.

## Categories

### Documentation
- **Player table — Hub / Lobby:** added the four default-true hotbar-item
  permissions: `sfcore.hub.selector`, `sfcore.hub.jumprod`,
  `sfcore.hub.vanishall`, `sfcore.hub.launchpad`.
- **Player table — new "Enchantments (Arcanum)" section:** `/enchants`
  (alias `/scrolls`, `sfcore.enchants.browse`), `/recall` and `/satchel`
  (`sfcore.enchants.use`), plus a note row for the `sfcore.enchants.use`
  scroll-use base permission.
- **Admin table — Player Management:** added the higher-tier
  `sfcore.vanish.admin` row.
- **Admin table — new "Enchantments (Arcanum)" section:** `/sfench` (aliases
  `/sfenchant`, `/sfe`) and all twelve subcommands mapped to their exact nodes —
  `admin`→`sfcore.enchants.admin`; `apply`/`remove`→`sfcore.enchants.apply`;
  `give`/`givebook`/`protect`/`success`→`sfcore.enchants.give`;
  `list`/`info`→`sfcore.enchants.list`; `reload`→`sfcore.enchants.reload`;
  `loot`→`sfcore.enchants.loot`; `debug`→`sfcore.enchants.debug`. The base
  command itself requires no permission (gated per-subcommand) and is noted as
  such.
- **Admin table — Wildcards & Bundles:** added the five missing bundle nodes:
  `sfcore.vanish.*`, `sfcore.invsee.*`, `sfcore.staff.*`, `sfcore.hub.*`,
  `sfcore.enchants.*`.
- Direct file: `docs/information/about.md`

## Notes
- No code changed; this is a documentation-accuracy pass only.
- Everything already present in about.md was checked and found accurate against
  `plugin.yml` and the command classes.

## Suggested Commit Message
```
Docs (about): reconcile commands/permissions — add Arcanum module, hub item perms, vanish.admin, and missing wildcard bundles
```
