# Staff menu — fix malformed custom-head texture hashes

■ **Created:** 2026-05-09 5:30 pm

## Summary

The `/staff` GUI's search head (slot 49) and red-X close head (slot 53)
were rendering as the default Steve skin instead of the intended custom
textures. Root cause: the base64 `value` strings hard-coded in
`StaffMenu.java` decoded to `textures.minecraft.net` URLs whose SHA-256
hashes were the wrong length:

- `RED_X_TEXTURE`  → hash was **63 hex chars** (1 char short)
- `SEARCH_TEXTURE` → hash was **65 hex chars** (1 char extra)

Mojang texture URLs are SHA-256 hashes (always exactly 64 hex chars).
Both URLs returned 404 from `textures.minecraft.net`, so the client fell
back to the default skin. The previous "head rotation" fix
(`2026-05-09---staff-menu-head-rotation-fix.md`) correctly stabilised
the synthetic profile UUID/name, but the textures themselves were always
unfetchable — the rotation fix masked the symptom rather than fixing the
underlying broken URLs.

## Categories

### GUIs / Bugfix

- `/staff` slot 49 (search) — base64 payload replaced with the verified
  minecraft-heads.com #27534 ("Loupe") texture. Hash is now exactly 64
  hex chars and resolves on `textures.minecraft.net`.
- `/staff` slot 53 (close) — base64 payload replaced with the verified
  minecraft-heads.com #56785 ("Red X") texture. Hash is now exactly 64
  hex chars and resolves on `textures.minecraft.net`.
- The same close-head texture is reused by `StaffActionMenu` (slot 26)
  via `StaffMenu.buildCloseHead`, so the action sub-menu's close button
  picks up the fix at no extra change cost.

### Internal

- No changes to `CustomHeadUtil.injectTexture` — the reflective
  `PlayerProfile` / `GameProfile` injection paths and the deterministic
  UUID/name pinning from the prior rotation fix are unchanged. The bug
  was purely in the texture payload constants.
- Updated the doc-comment for `SEARCH_TEXTURE` to record the
  minecraft-heads.com ID (#27534) for parity with `RED_X_TEXTURE`'s
  existing #56785 reference.

## Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenu.java`
  - `RED_X_TEXTURE` constant — base64 replaced (decoded URL hash now 64
    chars).
  - `SEARCH_TEXTURE` constant — base64 replaced (decoded URL hash now
    64 chars); doc-comment now names the source head (#27534 "Loupe").

## Files added

- `docs/changes/2026-05-09---staff-menu-head-texture-hash-fix.md`

## Verification

- Decoded both new payloads with `[Convert]::FromBase64String` and
  confirmed each URL ends in a 64-hex-char hash.
- `& ".\maven\bin\mvn.cmd" -DskipTests package` produced a fresh
  obfuscated `target/SF-Core-1.0.0-SNAPSHOT.jar`. Only ProGuard `Note:`
  lines for reflective accesses, which CLAUDE.md classifies as
  informational.
- No language file edits — no user-facing strings added or changed, so
  `language_en.yml` and `sprache_de.yml` are untouched.
- No `docs/information/about.md` update — commands, permissions, and
  aliases are unchanged.

## Notes

- Texture sources verified by the user from minecraft-heads.com
  give-command output (the `value=` field of the
  `profile.properties.textures` component).

## Suggested Commit Message

```
Fix (staff GUI): replace malformed search/close head textures with valid 64-char hashes so heads render instead of falling back to default skin
```
