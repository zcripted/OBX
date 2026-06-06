# 🔐 Scrub Live Discord Webhook from Default Config — Final Blocker Closed

> The last release blocker from the market-readiness audits (Rounds 6–8): the shipped
> `config.yml` contained a **live Discord webhook URL** plus the dev server/channel IDs.
> Scrubbed, shipped disabled-by-default, with buyer setup instructions in-line.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-06 6:27 PM EST |
| **Last Updated** | 2026-06-06 6:27 PM EST |
| **Author** | zcripted |
| **Scope** | Default config security scrub (Discord moderation logging) |
| **Files changed** | 2 modified (+ this log + change file) |
| **Categories** | Security · Config · Docs |
| **Verification** | ✅ `gradlew build` green · both jars produced |

---

## 📋 Summary (patch notes)

- **Removed the live Discord webhook URL and real server/channel IDs** from the default
  `config.yml`. A webhook URL is a posting credential — anyone holding it can write to
  the channel. Buyers now receive empty placeholders instead of the dev server's values.
- **Discord moderation logging now ships `enabled: false`.** The code defaults the flag
  to `true` when absent, and an enabled block with an empty webhook warns the console on
  every moderation action — so the shipped file disables it explicitly. Fresh installs
  are silent until the owner opts in.
- **Setup guide added in-line:** the config block now walks the owner through creating a
  webhook (Server Settings → Integrations → Webhooks), pasting the URL, filling their
  IDs, and flipping `enabled: true` — and flags the URL as a secret to keep private.
- **Console moderation logging is untouched** (`log-to-console: true` stays the default).
- The real IDs were also redacted from the one historical change-log doc that quoted them.

## 🔧 Changes (newest → oldest)

### Config scrub — `discord.moderation`
- `webhook-url`, `server-id`, `channel-id` → `""`.
- `enabled: true` → `enabled: false` (explicit, because the code-side default is `true`).
- Added 6 comment lines: webhook creation steps + secret warning.
- Kept the public branding `avatar-url` / `footer-icon-url` (logo image, not a secret).
- File: [`plugin/src/main/resources/config.yml`](../../../plugin/src/main/resources/config.yml)

### Docs redaction
- Removed the literal server/channel IDs from the 2026-04-06 moderation change log —
  the only other place in the tree that contained them (the webhook URL appeared nowhere
  else).
- File: [`docs/changes/2026-04-06---moderation-discord-and-test-profiles.md`](../../changes/2026-04-06---moderation-discord-and-test-profiles.md)

### Change file
- [`docs/changes/2026-06-06---scrub-discord-webhook-config.md`](../../changes/2026-06-06---scrub-discord-webhook-config.md)

## ⚠️ Required manual follow-up (cannot be done from the repo)

**Revoke the webhook in Discord** — Server Settings → Integrations → Webhooks → delete
(or "Reset Token") the `OBX Moderation` webhook. The URL is in the **git history of both
repos**, so removing it from the working tree does not retire the credential; revoking it
does, and makes history rewriting unnecessary.

## ✅ Verification
- `.\gradlew.bat build` — BUILD SUCCESSFUL; `OBX-<ver>-unobf.jar` + obfuscated
  `OBX-<ver>.jar` both produced.
- Behavior check: with `enabled: false`, `ModerationService#postToDiscord` returns before
  any network call or warning; console moderation logs still emit.

## 💬 Suggested Commit Message
```
Security (config): scrub live Discord webhook + IDs from default config, ship disabled with setup docs
```
