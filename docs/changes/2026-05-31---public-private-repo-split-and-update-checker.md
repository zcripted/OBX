# Public/Private Repo Split & GitHub Update Checker

■ **Created:** 2026-05-31 7:46 pm (America/Detroit)

■ **Last Updated:** 2026-05-31 7:46 pm (America/Detroit)

---

## Summary

Wires OBX to its two-repository GitHub layout and implements a real update
checker:

- **Public repo** `https://github.com/zcripted/OBX` — README, docs, wiki,
  changelog, issues, and **releases** (no source code). All user-facing links
  already point here.
- **Private repo** `https://github.com/zcripted/OBX-private` — the real source
  code. The git `origin` remote of this working tree now points here.

The `/obx updates` checker (previously a stub) now queries the **public** repo's
latest release and compares it against the running version.

---

## Categories

### Internal — Git / Repo Wiring
- Repointed the source remote from the old `SergeantFuzzy/SF-Core.git` to the
  private source repo:
  - `origin → https://github.com/zcripted/OBX-private.git` (fetch + push)
  - Local config only — nothing was pushed.
- `.gitignore`: removed the `.idea/` line. Per request, `.idea` is to be **neither
  tracked nor ignored**; it remains untracked (it was never committed). Final
  ignore list: `target/`, `*.iml`, `maven/`.
  - `.gitignore`

### API / Internal — Update Checker (new)
- New `UpdateChecker` utility that checks the **public** OBX repo for the latest
  release and compares versions:
  - Endpoint: `https://api.github.com/repos/zcripted/OBX/releases/latest`
    (the private repo is never contacted — releases live on the public repo).
  - Network I/O runs off the main thread via the plugin's Folia-aware
    `SchedulerAdapter.runAsync`; the result is delivered back on the main/global
    thread via `SchedulerAdapter.runNow`, so chat replies from the callback are
    always thread-safe on Bukkit/Spigot/Paper/Purpur and Folia alike.
  - Dependency-free: the response is scanned for `"tag_name"` with a small regex
    instead of pulling in a JSON library.
  - `normalize()` strips a leading `v` and any `-SNAPSHOT`/`+build` qualifier;
    `isNewer()` does a numeric, dot-segmented comparison (missing/non-numeric
    segments treated as 0 so a malformed tag never false-positives).
  - 5s connect/read timeouts; any failure (incl. HTTP 404 = no releases yet) maps
    to `Status.FAILED` and a graceful message.
  - Exposes `RELEASES_URL` (`https://github.com/zcripted/OBX/releases`) for the
    "update available" download line.
  - `src/main/java/dev/zcripted/obx/util/update/UpdateChecker.java` (new)

### Commands — `/obx updates`
- `handleUpdates` was a stub that printed static text (and referenced `{version}`
  with no placeholder map, so it rendered literally). It now:
  - sends the "Checking for updates…" header,
  - runs `UpdateChecker.checkAsync`,
  - and replies based on the result: up-to-date, update-available (+ download
    link), or a graceful failure message.
  - `/obx updates notify` (toggle) and the `obx.updates.check` / `obx.updates.notify`
    permission gates are unchanged. `/obx updates check` still works (falls through
    to the same check path).
  - `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`

### Internal — Messages
- Replaced the stub keys `commands.obx.updates.listing` / `.forced` / `.hint`
  with result keys (EN + DE):
  - `commands.obx.updates.current` — "You are running the latest version {version}."
  - `commands.obx.updates.available` — "A new version is available: {latest} (you have {current})."
  - `commands.obx.updates.available-link` — "Download: {url}"
  - `commands.obx.updates.failed` — graceful "could not check" message.
  - `commands.obx.updates.header` and `commands.obx.updates.notify.*` retained.
  - `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`

---

## Notes / Assumptions

- The checker reads the latest **published GitHub Release** tag. Until the first
  release is cut on the public repo, the API returns 404 and the command reports
  the graceful "could not check" message — expected pre-release behavior.
- Tag format is assumed to be a semver-ish `vX.Y.Z` / `X.Y.Z` (optionally with a
  qualifier), matching the plugin's `getDescription().getVersion()`.
- No automatic startup check was added in this change; the checker is invoked by
  the `/obx updates` command. (A startup/login-notify hook can reuse
  `UpdateChecker.checkAsync` later if desired.)

---

## Testing
- In-project Maven build completes with **no errors** (`./maven/bin/mvn -DskipTests package`).
- Both obfuscated and unobfuscated jars build successfully.
- Verified: `UpdateChecker` targets `zcripted/OBX` (public), 0 references to the
  private repo in the checker; `.idea` not tracked and not in `.gitignore`;
  `origin` remote = `OBX-private`.

---

## Suggested Commit Message
```
Feature (updates): Real /obx update checker against public zcripted/OBX releases; point source remote at OBX-private; stop ignoring .idea
```
