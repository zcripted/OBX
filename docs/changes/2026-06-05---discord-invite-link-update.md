# Discord Invite Link Update (zN3UQyKdfD → UxktSyT9Ag)

■ **Created:** 2026-06-05 6:32 pm

■ **Last Updated:** 2026-06-05 6:40 pm

## Summary

Project-wide replacement of the Discord invite: `discord.gg/zN3UQyKdfD` →
**`https://discord.gg/UxktSyT9Ag`** — 13 occurrences across 10 tracked files (README,
config, language defaults, codebase fallback, tablist, and historical docs), verified
afterwards with a zero-hit sweep for the old code. The `LICENSE` file contains no Discord
reference (nothing to change there).

## Categories

### Docs / README
- `README.md` — 5 occurrences (badge, header links, API outreach note, community section).
- `docs/changes/2026-05-09---welcome-tooltips-discord-vanish-multiline.md` — 1.
- `docs/changes/2026-05-31---rebrand-sf-core-to-obx.md` — 1.
- `docs/changes/2026-06-05---ascii-wordmark-console-banner.md` — 1 (banner example).

### Config
- `plugin/src/main/resources/config.yml` — `links.discord` default.
- `plugin/src/main/resources/systems/tablist.yml` — gradient footer line.

### Codebase
- `plugin/src/main/java/dev/zcripted/obx/OBX.java` — banner `links.discord` fallback URL.

### Messages (EN/DE/ES in lock-step)
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java` — 3 (welcome
  MOTD Discord row: display text, hover scope, open-url click value).
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java` — 3 (same row).
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java` — 3 (same row).

### Local test-server artifacts (gitignored `plugin/run/`, refreshed for consistency)
- `plugin/run/plugins/OBX/config.yml` — stale `discord.gg/zcripted` → new invite.
- `plugin/run/plugins/OBX/systems/tablist.yml` — old invite → new invite.

## GitHub remote READMEs (added 6:40 pm) — amended in place, no new commits

Both GitHub repos' published READMEs carried the old invite (5 occurrences each) and were
fixed by **amending each repo's single "Initial commit"** and force-pushing
(`--force-with-lease`) — history kept at exactly one commit per repo:

- **Public** `zcripted/OBX` — `main`: `2c7a55a` → `98d9917` (forced update)
- **Private** `zcripted/OBX-private` — `main`: `b989975` → `f613a0f` (forced update)

Verified post-push via the GitHub API: both READMEs now show only
`discord.gg/UxktSyT9Ag`, and each repo still reports **1 commit**. The local working
repo's `origin/main` ref was refreshed via `git fetch`. (Note: this working tree's local
`main` branch predates the rewrite and now diverges from the new `origin/main` tip — to be
reconciled whenever `main` is next touched; the active `rebrand-obx` branch is unaffected.)

## Operator note (existing deployments)

OBX only **appends missing keys** to already-generated `config.yml` / `lang/*.yml` /
`systems/tablist.yml` files — it never overwrites existing values. Servers that generated
their files before this change keep the old invite until the admin edits
`links.discord` (config.yml), the tablist footer line, and the `welcome.motd-lines`
Discord row (lang files) — or deletes those files to regenerate them.

## Verification
- Repo-wide grep for `zN3UQyKdfD`: **0 matches** after the change.
- `.\gradlew.bat build` — **BUILD SUCCESSFUL** (incl. EN/DE/ES parity test); both jars produced.

## Suggested Commit Message
```
Chore (links): update Discord invite to discord.gg/UxktSyT9Ag project-wide
```
