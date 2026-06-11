# Reply Button — Non-Bold, Lead Removed, Centered in Chat

■ **Created:** 2026-06-07 5:08 pm

■ **Last Updated:** 2026-06-07 5:08 pm

The `[Send reply message]` button under incoming PMs is now non-bold, loses its `»`
lead, and renders horizontally centered in chat (all languages). Full breakdown in the
commit log:
[docs/commits/2026-06-07/reply-button-centered.md](../commits/2026-06-07/reply-button-centered.md)

## Categories

### Chat (PM reply button)
- `message.reply-button` un-bolded in EN/DE/ES; `message.reply-lead` (`&8» `) removed.
- Button line is centered via a plain leading-space pad computed from the button's pixel
  width — the button itself remains a clickable (`/rply`) hover component.
- `features/mail/src/main/java/dev/zcripted/obx/feature/mail/pm/PrivateMessageService.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsEN.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsDE.java`
- `core/src/main/java/dev/zcripted/obx/core/language/MessageDefaultsES.java`

### Internal (util)
- New `MotdMessageUtil.chatCenterPadding(String)`; pixel measuring extracted to
  `measureLegacyPx` (shared with the MOTD `<center>` path, no behavior change).
- `core/src/main/java/dev/zcripted/obx/util/message/MotdMessageUtil.java`

## Verification
- `.\gradlew.bat build` green — tests (incl. EN/DE parity) pass; both jars produced.

## Suggested Commit Message
```
Polish (pm): reply button non-bold, » lead removed, centered in chat (EN/DE/ES)
```
