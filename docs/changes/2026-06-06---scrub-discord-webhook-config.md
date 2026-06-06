# Scrub Live Discord Webhook + Server/Channel IDs from Default Config (Blocker #1)

■ **Created:** 2026-06-06 6:27 pm

■ **Last Updated:** 2026-06-06 6:27 pm

## Summary

Resolves the **final release blocker** from the Round 6–8 market-readiness assessments:
the default `config.yml` shipped with a **live Discord webhook URL** (a posting
credential) plus the real server/channel IDs of the development Discord. Any buyer could
have posted to the dev moderation channel using the shipped URL.

## Categories

### Config
- `discord.moderation.webhook-url`, `server-id`, and `channel-id` emptied to `""`.
- `discord.moderation.enabled` flipped to `false` — important because the code default
  is `true` when the key is absent (`ModerationService#postToDiscord`), and an enabled
  block with an empty webhook logs a warning on every moderation action. Disabled +
  empty = fully silent until the buyer opts in.
- Added setup comments above the block: how to create a webhook in Discord, where to
  paste it, and a note that the webhook URL is a **secret**.
- Console moderation logging (`log-to-console`) is unaffected and stays on by default.
- Branding `avatar-url` / `footer-icon-url` (public logo image) intentionally kept.

Files:
- [`plugin/src/main/resources/config.yml`](../../plugin/src/main/resources/config.yml)

### Docs
- Redacted the real server/channel IDs from the 2026-04-06 change log (the only other
  in-repo occurrence; the webhook URL itself appeared nowhere else in the tree).

Files:
- [`docs/changes/2026-04-06---moderation-discord-and-test-profiles.md`](../2026-04-06---moderation-discord-and-test-profiles.md)

## Required manual follow-up (outside the repo)

- **Revoke the webhook in Discord** (Server Settings → Integrations → Webhooks → delete
  it, or "Reset Token"). The URL lives in the **git history of both repos**, so scrubbing
  the working tree alone does not retire the credential — revocation does. After
  revocation, history rewriting is unnecessary.

## Verification
- `.\gradlew.bat build` — BUILD SUCCESSFUL; both jars produced.

## Suggested Commit Message
```
Security (config): scrub live Discord webhook + IDs from default config, ship disabled with setup docs
```
