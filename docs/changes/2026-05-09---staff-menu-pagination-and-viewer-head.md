# Staff menu — pagination + operator accountability head

■ **Created:** 2026-05-09 5:45 pm

## Summary

The `/staff` overview previously capped the player listing at the first
45 slots and silently dropped any online player past that limit. This
change adds **unlimited pagination** (page size = 45, total pages =
`ceil(onlineExcludingSelf / 45)`) with prev/next buttons that only
appear when an adjacent page exists, and adds the **operator's own
player head** to the bottom row as an accountability marker — the
"desktop screen" concept where the open management interface visibly
identifies who is operating it.

## Categories

### GUIs

- `/staff` main menu now paginates the alphabetical online-player list
  across as many 45-slot pages as needed. Pagination is server-population
  driven — no upper bound.
- Bottom-row layout (slots 45 – 53):
  ```
   45  46  47  48  49  50  51  52  53
   ◀   ·   me  ·   ?   ·   ▶   ·   ✖
  ```
  - Slot 45 — previous-page arrow (rendered only when `currentPage > 0`)
  - Slot 47 — viewer's own player head (NEW; accountability marker)
  - Slot 49 — search head (unchanged)
  - Slot 51 — next-page arrow (rendered only when more pages remain)
  - Slot 53 — red-X close head (unchanged)
- Title shows `(page/pages)` only when the server has more than one
  page of staffable players; single-page views keep the original
  `&6&lStaff Menu` title verbatim.
- Clicking the viewer's own head is a no-op (it's a marker, not a
  button) so the operator can never accidentally open an action menu
  against themselves.
- Search round-trip preserves the page: clicking search on page 4,
  then cancelling / typing an offline name / typing your own name
  reopens the menu on page 4 instead of jumping back to page 1.
- Player-offline refresh (when the clicked target logged off between
  render and click) also preserves the current page.

### Internal

- `StaffMenuHolder` now carries `currentPage`, `totalPages`,
  `viewerSlot`, `prevPageSlot`, and `nextPageSlot` (the latter two use
  `StaffMenuHolder.NO_SLOT = -1` when not rendered) so the click router
  can dispatch without re-walking the inventory.
- New `StaffMenu.open(Main, Player, int page)` overload; the existing
  `open(Main, Player)` delegates to `open(plugin, viewer, 0)` so all
  prior call sites (StaffCommand, AdminMenu wiring, etc.) keep
  working.
- `buildPlayerHead` was generalised to take a language-key prefix —
  the same lookup feeds both the per-target heads
  (`admin.staff.player-head`) and the new viewer self-head
  (`admin.staff.viewer-head`).
- Prev/next buttons use `Material.ARROW` (universal across 1.8.x →
  1.21.x) to avoid another texture-base64 maintenance burden.
  Click-routing falls through cleanly when the holder reports
  `NO_SLOT` for those positions.

### Language

- New keys (EN + DE) added to `MessageDefaults`:
  - `admin.staff.menu.title-paginated` — title used only when
    `totalPages > 1`. Placeholders: `{page}`, `{pages}`.
  - `admin.staff.viewer-head.name` — viewer-head display name. Carries
    a `(You)` / `(Du)` tag.
  - `admin.staff.viewer-head.lore` — viewer-head lore. Same field set
    as the per-target head plus an "Operator" header and an explicit
    "shown for accountability — click does nothing" footer.
  - `admin.staff.prev-page.name`, `admin.staff.prev-page.lore` — prev
    button. Lore placeholders: `{page}`, `{pages}`, `{target}`.
  - `admin.staff.next-page.name`, `admin.staff.next-page.lore` — next
    button. Same placeholder set.

## Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenu.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuHolder.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuInputManager.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/menu/StaffMenuListener.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`

## Files added

- `docs/changes/2026-05-09---staff-menu-pagination-and-viewer-head.md`

## Verification

- `& ".\maven\bin\mvn.cmd" -DskipTests package` produced a fresh
  obfuscated `target/SF-Core-1.0.0-SNAPSHOT.jar` with no `[ERROR]` or
  `BUILD FAILURE` lines. Only ProGuard `Note:` lines for reflective
  accesses (informational per CLAUDE.md).
- Pagination math is symmetric: `totalPages = max(1, ceil(n/45))`
  always renders at least one page so an empty server still shows the
  bottom-row controls + viewer head.
- `currentPage` is clamped into `[0, totalPages - 1]` on every open so
  a stale click after players log off can't strand the operator on a
  non-existent page.

## Notes / non-changes

- No `docs/information/about.md` update — the `/staff` command itself,
  its permission, and its aliases are unchanged. Only the GUI it
  opens was extended.
- No new permissions introduced — pagination is available to anyone
  who can already open `/staff`.

## Suggested Commit Message

```
Feature (staff GUI): paginate the online-player list with on-demand prev/next buttons and add the operator's own head to the bottom row as an accountability marker
```
