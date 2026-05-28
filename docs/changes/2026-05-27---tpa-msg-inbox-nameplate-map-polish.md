# /tpa & /tp polish, reply/inbox overhaul, nameplate colors, /map opens

■ **Created:** 2026-05-27 10:50 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 10:50 pm (America/Detroit)

---

## Summary

UX polish across teleport, messaging, and the inbox: `/tpa` Accept/Deny on their own line;
`/tp` & `/tphere` now show destination world + coordinates; OP nametags render light red and
default players light yellow above their heads; the `/msg` reply button moved to its own line
and relabeled; a dedicated `INBOX` chat prefix; a full read/unread/bookmark/delete/clear inbox;
and `/map` now opens in the player's hand.

## Categories

### Teleport
- `command/teleportation/TeleportCommand.java` — `/tp <target>` and `/tphere` messages now
  include the destination **world + X/Y/Z** (block coords) on a styled second line. New
  `putLocation(...)` helper; `moveTo` adds the destination location, `/tphere` adds the
  sender's location (where the target lands).
- `util/teleport/TeleportRequestService.java` — the `/tpa` prompt is now two lines: line 1 is
  "&lt;player&gt; wants to teleport to you." and line 2 reads
  "» Choose to [Accept] or [Deny] this request." with the clickable buttons. Button labels and
  the connective words are localized message keys.
- `language/MessageDefaults.java` — `teleport.tp.went/moved/moved-sender/brought/brought-self`
  converted to two-line styled lists with `{world}/{x}/{y}/{z}`; new
  `teleport.request.accept-button/deny-button/choose-lead/choose-or/choose-tail` (EN+DE).

### Nameplates (name above skin)
- `tablist/format/TablistTeams.java` — the existing staff/players sort teams now color their
  members' nametags: OP → light red (`§c`), default → light yellow (`§e`). Coloring uses the
  team **prefix** (a trailing color code the name inherits) for 1.8–1.12 compatibility, plus a
  reflective `Team#setColor` for 1.13+. New `applyColor(...)`.
- `listener/player/JoinListener.java` — assigns each joining player to the colored team on a
  1-tick delay, independent of whether the tablist display module is enabled, so the nameplate
  color always applies.

### Messaging (/msg, /rply)
- `message/MessageService.java` — the received-message reply button is now sent on its **own
  second line** (`» [Send reply message]`).
- `language/MessageDefaults.java` — `message.reply-button` → `&a&l[Send reply message]`
  (DE `[Antwort senden]`); new `message.reply-lead` ("» ").

### Inbox prefix + read/unread/bookmark/delete/clear
- `language/MessageDefaults.java` — new `inbox.prefix`: a `✉ 𝗜𝗡𝗕𝗢𝗫 ➠` wordmark in custom
  gold `#FFD93D` (same math-bold style as the SF-CORE prefix). `message.stored` and
  `inbox.join-notify` lost their inline `✉` (the prefix now carries it).
- `language/LanguageManager.java` — routes `{prefix}` for `message.stored` and all `inbox.*`
  keys to `inbox.prefix`.
- `message/InboxMessage.java` — added a stable `id` plus mutable `read` / `bookmarked` flags,
  persisted in `toMap`/`fromMap` (legacy entries get a generated id and default to unread).
- `message/MessageStore.java` — new `markRead`, `setBookmarked`, `delete`, and
  `clearNonBookmarked(...)` (returns count) operating by message id.
- `message/MessageService.java` — `readMessage` now marks the message read; new
  `deleteMessage`, `toggleBookmark`, and `clearInbox` (each refreshes the GUI next tick).
- `message/InboxMenu.java` — unread entries use a writable book, read entries a plain book;
  bookmarked entries glow and carry a `★`. Each entry's lore shows its status and the click
  options. New bottom-row **Clear Inbox** button (`CLEAR_SLOT = 49`) that removes all
  non-bookmarked messages.
- `listener/menu/InboxMenuListener.java` — left-click reads (marks read), right-click deletes,
  shift-click toggles bookmark, and the Clear button clears non-bookmarked messages.
- `language/MessageDefaults.java` — new `inbox.deleted/bookmarked/unbookmarked/cleared`,
  `inbox.entry.unread/read/bookmarked`, `inbox.entry.click-read/delete/bookmark/unbookmark`,
  and `inbox.clear-name/clear-lore1/clear-lore2` (EN+DE).

### /map
- `command/utility/MapCommand.java` — instead of just adding the map to the inventory, it now
  **opens** it: placed into the currently-held hand if empty, else an empty hotbar slot is
  selected, else stashed/dropped without destroying items. Message reworded to "Opened…".
- `docs/information/COMMANDS+PERMISSIONS.md` — `/map` row updated.

## Notes / assumptions
- Nametag coloring rides the existing tab sort teams (the standard Minecraft mechanism). Op
  status changes apply on rejoin or the next tablist refresh. Setting team color affects the
  name wherever vanilla draws it (nametag/tab); the SF-Core tablist name is still applied
  separately via `setPlayerListName`.
- "Light red/light yellow" map to the standard `RED` (`§c`) / `YELLOW` (`§e`) chat colors —
  the only nametag colors Minecraft teams support.
- The `INBOX` and tp coordinate prefixes use `#FFD93D` / math-bold glyphs (1.16+ for the hex;
  the wordmark glyphs match the existing SF-CORE/SYSTEM prefixes).
- `/map` tiles render client-side as the held map loads surrounding chunks (a fresh map fills
  in over the first moment), same as vanilla maps.

## Testing
- Maven build: `& ".\maven\bin\mvn.cmd" -DskipTests package` → exit 0; both jars rebuilt
  (obf ~660 KB, unobf ~954 KB). ProGuard `Note:` lines only. Compile-verified.
- In-game checks to run: `/tpa` two-line prompt + clickable Accept/Deny; `/tp <player>` and
  `/tphere` coordinate lines; OP vs default nametag colors above heads; `/msg` reply on its own
  line; "saved to inbox" / join-notify INBOX prefix; inbox left=read (then shows read material),
  right=delete, shift=bookmark (glow + ★), Clear button keeps bookmarked; `/map` opening in hand.

## Suggested Commit Message
```
Polish: /tpa accept-deny line + /tp coords, red/yellow nametags, /msg reply line, INBOX prefix + read/bookmark/delete/clear inbox, /map opens in hand
```
