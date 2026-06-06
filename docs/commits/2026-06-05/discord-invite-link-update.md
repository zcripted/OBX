# 🔗 Discord Invite Link Update

> Project-wide swap of the Discord invite — `discord.gg/zN3UQyKdfD` →
> **`https://discord.gg/UxktSyT9Ag`** — across README, config, language defaults,
> codebase fallback, tablist, and historical docs. Verified with a zero-hit sweep.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 6:32 PM EST |
| **Last Updated** | 2026-06-05 6:40 PM EST |
| **Author** | zcripted |
| **Scope** | Link/branding maintenance — 13 occurrences in 10 tracked files |
| **Files changed** | 10 modified (tracked) + 2 gitignored test-server artifacts refreshed |
| **Categories** | Chore · Docs · Config · Messages/i18n |
| **Verification** | ✅ repo grep for old invite = 0 hits · `gradlew build` green (EN/DE/ES parity) |

---

## 📋 Summary (patch notes)

Every place OBX points players at Discord now uses the new invite
**discord.gg/UxktSyT9Ag**: the README badge and links, the console banner's Support row
(via the `links.discord` config default and its code fallback), the default tablist
footer, and the welcome MOTD's clickable Discord row in **all three languages**.

## 🔄 Changes

### 📚 Docs / README
- [`README.md`](../../../README.md) — badge, header link row, API note, community section (5).
- [`2026-05-09---welcome-tooltips-discord-vanish-multiline.md`](../../changes/2026-05-09---welcome-tooltips-discord-vanish-multiline.md),
  [`2026-05-31---rebrand-sf-core-to-obx.md`](../../changes/2026-05-31---rebrand-sf-core-to-obx.md),
  [`2026-06-05---ascii-wordmark-console-banner.md`](../../changes/2026-06-05---ascii-wordmark-console-banner.md) — 1 each.

### ⚙️ Config & resources
- [`config.yml`](../../../plugin/src/main/resources/config.yml) — `links.discord` default.
- [`systems/tablist.yml`](../../../plugin/src/main/resources/systems/tablist.yml) — footer line.

### 🧩 Codebase
- [`OBX.java`](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java) — banner Support-row fallback URL.

### 🌐 Messages (EN/DE/ES in lock-step — welcome MOTD Discord row: text + hover scope + click URL)
- [`MessageDefaultsEN.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java)
  · [`MessageDefaultsDE.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java)
  · [`MessageDefaultsES.java`](../../../core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java)

### 🧪 Local test server (gitignored, refreshed so `runServer` shows the new link)
- `plugin/run/plugins/OBX/config.yml` (had an even older `discord.gg/zcripted`) and
  `plugin/run/plugins/OBX/systems/tablist.yml`.

> **Operator note:** OBX appends missing keys to existing generated files but never
> overwrites values — servers generated before this change keep the old invite until the
> admin updates `links.discord`, the tablist footer, and the MOTD Discord row (or deletes
> the files to regenerate).

---

### 🌐 GitHub remote READMEs (6:40 PM — amended in place, NO new commits)
- **Public `zcripted/OBX`** `main`: `2c7a55a` → `98d9917` — README's 5 old invites fixed by
  amending the single "Initial commit" + `--force-with-lease` push.
- **Private `zcripted/OBX-private`** `main`: `b989975` → `f613a0f` — same procedure.
- Post-push API verification: both READMEs show only `discord.gg/UxktSyT9Ag`; each repo
  still has exactly **1 commit**. Working repo's `origin/main` ref fetched/refreshed.

## ✅ Verification
- Repo-wide grep for `zN3UQyKdfD` → **0 matches**.
- GitHub API readme check on both repos → only the new invite remains.
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Chore (links): update Discord invite to discord.gg/UxktSyT9Ag project-wide
```
