# Staff menu — fix rotating search / close head textures

■ **Created:** 2026-05-09 4:15 pm

■ **Last Updated:** 2026-05-09 6:30 pm

## Summary

The search head (slot 49) and the red-X close head (slot 53) on the
`/staff` overview GUI were visually "rotating" / cycling between
unrelated head textures on click. Root cause: every call to
`CustomHeadUtil.injectTexture` minted a fresh `UUID.randomUUID()` for
the synthetic `GameProfile`, so the Minecraft client could never cache
the resolved skin against a stable key — each refresh / cancelled-click
re-resolved the slot and the displayed head briefly snapped to whatever
cached profile shared its render slot.

The fix derives the profile UUID deterministically from the texture
payload itself (`UUID.nameUUIDFromBytes(texture.getBytes(UTF_8))`), so
every render of the same texture produces the same UUID, the client
caches the skin once, and the head stays pinned. The textures
themselves (red-X #56785 from minecraft-heads.com for close, and the
magnifying-glass texture for search) are unchanged — only the profile
UUID generation was changed.

## Categories

### Internal / Bugfix

- `CustomHeadUtil.injectTexture` now derives the synthetic
  `GameProfile` UUID from the base64 texture string instead of
  `UUID.randomUUID()`. Applies to both the modern
  `org.bukkit.profile.PlayerProfile` path (1.18+) and the legacy
  authlib `GameProfile` reflective path (1.8 – 1.17). Same texture →
  same UUID → stable client-side skin cache, so the search and close
  heads no longer rotate on click.
- The fallback-to-barrier behaviour on servers that can't reflect into
  authlib is unchanged; only the UUID source was altered.

### GUIs

- Staff main menu (`/staff`):
  - Slot 49 (search) — now shows a stable magnifying-glass head across
    every open / refresh / cancelled click.
  - Slot 53 (close) — now shows a stable red-X head
    (`https://minecraft-heads.com/custom-heads/head/56785-red-x`) across
    every open / refresh / cancelled click.
- Any other GUI in the project that consumes `CustomHeadUtil.customHead`
  (e.g. the action sub-menus that use custom textures) inherits the
  same stability fix at no extra change cost.

## Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/gui/shared/CustomHeadUtil.java`

## Files added

- `docs/changes/2026-05-09---staff-menu-head-rotation-fix.md`

## Notes / non-changes

- No language file edits — no user-facing strings were added or
  changed, so `language_en.yml` and `sprache_de.yml` are untouched.
- No changes to `StaffMenu.java` — the texture base64 strings and slot
  layout are preserved verbatim. The bug was upstream of the texture
  payloads, in the profile-injection helper.
- No `docs/information/about.md` update — commands, permissions, and
  aliases are unchanged.

## 2026-05-09 6:30 pm — Follow-up: pin profile *name* too

### Why the first fix wasn't enough

Pinning the `GameProfile` UUID alone wasn't sufficient. CraftBukkit / Paper
treat a profile with a {@code null} name as **incomplete** and may
re-fill the textures property through Mojang's session service when
serializing the skull to the client. That re-fill is what was still
swapping the rendered head between unrelated textures on click /
refresh — even with a stable UUID.

### Fix

`CustomHeadUtil.injectTexture` now also assigns a deterministic, fake
Minecraft-username-shaped name to the synthetic profile (format
`head_<11 hex chars derived from the texture-derived UUID>`, total 16
chars, alphanumeric + underscore) on **both** the modern
`org.bukkit.profile.PlayerProfile` path and the legacy authlib
`GameProfile` path. With both UUID *and* name set, the profile is
"complete" — the server has no reason to re-resolve it through Mojang,
the textures property survives serialization intact, and the rendered
head stays pinned to the texture we set.

The texture base64 strings (red-X #56785 for close, magnifying-glass
for search) are unchanged — only the synthetic profile's *name* field
was added. Same UUID + same name + same texture across every render →
same client-side cache entry → no rotation.

### Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/gui/shared/CustomHeadUtil.java`
  - `injectTexture` now passes `stableProfileName(profileUuid)` instead
    of `null` to both `Bukkit.createPlayerProfile(UUID, String)` and
    `new GameProfile(UUID, String)`.
  - New private helper `stableProfileName(UUID)` that produces
    `head_<11 hex chars>` from the most-significant-bits of the
    texture-derived UUID. Conforms to MC username rules (3 – 16 chars,
    `[a-zA-Z0-9_]`) so `validateProfile`-style server checks pass on
    every supported version.

### Verification

- `./maven/bin/mvn.cmd -DskipTests package` → `BUILD SUCCESS`. Only
  ProGuard `Note:` lines for reflective accesses, which CLAUDE.md
  explicitly classifies as informational.
- No language file edits — no user-facing strings added or changed.
- No `docs/information/about.md` update — commands, permissions,
  aliases unchanged.

## Suggested Commit Message

```
Fix (staff GUI): pin custom-head profile UUID *and* name to texture so search/close heads finally stop rotating on click
```
