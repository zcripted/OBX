# Staff overview GUI + `/staff` command

■ **Created:** 2026-05-09 3:00 pm

■ **Last Updated:** 2026-05-09 3:25 pm

## Summary

New `/staff` command that opens a GUI overview of every currently-online
player (excluding the viewer) for ops and staff. Each player renders as a
skin-head with a detailed hover profile card and a per-player moderator
action sub-menu. The bottom row carries a search head (chat-prompted
player lookup with success / not-online / cancelled / empty / self
feedback) and a custom red-X close button.

## Permission gating

- Command is gated by **`sfcore.staff.menu`** (default: `op`). Non-permitted
  players cannot run, see, or tab-complete the command — the
  `onTabComplete` returns an empty list whenever the sender lacks the
  permission, and the executor sends `core.no-permission` and exits.
- New `sfcore.staff.*` umbrella (default: `op`) carries
  `sfcore.staff.menu` so a single grant covers the whole feature.
- Both the umbrella and `sfcore.staff.menu` are linked into the
  existing `sfcore.*` and `sfcore.admin.*` umbrella permissions so an
  `*`-grant continues to work.

## Main GUI (`StaffMenu`)

- 6-row inventory (54 slots).
- **Slots 0 – 44** (the first five rows) carry one player skull per
  online player, alphabetical (case-insensitive). The viewer is skipped
  so they never see themselves in the listing.
- **Bottom row (slots 45 – 53)** is filler glass plus:
  - Slot 49 — search head (custom magnifying-glass texture; falls back
    to a barrier on servers that can't inject the texture).
  - Slot 53 — close head (custom red-X texture from
    `https://minecraft-heads.com/custom-heads/head/56785-red-x`; same
    barrier fallback).
- Each player skull's hover lore renders a card with:
  - Username
  - First joined (formatted `yyyy-MM-dd hh:mm a`, sourced from
    `OfflinePlayer.getFirstPlayed()`).
  - Total time active on server (years / months / days / hours /
    minutes / seconds — fields with value `0` for `years`, `months`,
    `days` are omitted from the display per the spec; the all-time
    counter comes from the Bukkit `Statistic` enum, resolved
    reflectively through `PLAY_TIME` → `PLAY_ONE_MINUTE` →
    `PLAY_ONE_TICK` so the same JAR works on every supported server
    version including the 1.12.2 API baseline this project compiles
    against, where the constant name is `PLAY_ONE_TICK`).
  - Time online since recent join (days / hours / minutes / seconds —
    `0d` is omitted; the session start is read from a new in-memory
    `StaffSessionTracker` listener).
  - Country (best-effort from the player's client locale via
    `Player.getLocale()` reflectively, falling back to
    `Player.spigot().getLocale()` on 1.8 – 1.11; the country segment
    of `en_us`-style locales is mapped to a display name through a
    static ISO-3166 table; per CLAUDE.md no GeoIP network call is
    performed).
  - Language (resolved from `LanguageManager.getLanguage(uuid)`).
  - Report card with counts of warnings, mutes, kicks, tempbans, and
    bans, sourced from `ModerationService.getStatusProfile(name)
    .getActionCount(...)` for each action key.

Click a player skull → opens that player's action sub-menu. Click the
search head → closes the GUI and prompts in chat for a player name with
`success` / `not-online` / `cancelled` / `empty` / `self` feedback.
Click the close head → closes the GUI.

## Action sub-menu (`StaffActionMenu`)

- 3-row inventory (27 slots).
- Middle row carries the five placeholder action buttons (slots 11
  through 15): Warn, Mute, Kick, Tempban, Ban. Each carries a localized
  display name, a lore that names the target player, and a placeholder
  click that sends an `admin.staff.action.<name>.placeholder` message
  to the staff member. The action buttons reuse the existing
  `WarpMenuStyling.item(...)` builder for cross-version material
  resolution.
- Bottom row carries the back-to-main-staff-menu arrow (slot 18) and
  the same red-X close head (slot 26) used on the main menu. Both
  reuse `StaffMenu.buildCloseHead` so a future texture swap touches
  one method.

## Search input flow

- New `StaffMenuInputManager` keyed by `UUID`, mirroring the existing
  `WarpMenuInputManager` shape so a single chat-input pattern is
  consistent across the codebase. State is purely in-memory.
- Click on the search head closes the GUI (so the chat prompt isn't
  obscured), records the staff member as pending, and sends the
  `admin.staff.search.prompt` message.
- New `StaffMenuInputListener` cancels the
  `AsyncPlayerChatEvent` for pending staff members and forwards the
  message body to the manager.
- Resolution is exact-name first, then a longest-shortest-name prefix
  match (case-insensitive) across the online roster. The viewer is
  excluded from prefix matches so a partial input can't accidentally
  resolve to themselves; an exact-self match is rejected with
  `admin.staff.search.self`.
- Feedback messages: `admin.staff.search.success`,
  `admin.staff.search.not-online`, `admin.staff.search.cancelled`
  (when the staff member types `cancel`), `admin.staff.search.empty`,
  and `admin.staff.search.self`.
- After resolution, `StaffActionMenu.open(...)` is dispatched on the
  staff member's region thread (via `SchedulerAdapter.runAtEntity`)
  so Folia accepts the `openInventory` call.

## Custom-head infrastructure

New `gui/shared/CustomHeadUtil.java`:

- `playerHead(OfflinePlayer, name, lore)` — builds the player's skin
  skull. Tries `SkullMeta.setOwningPlayer(OfflinePlayer)` first
  (1.13+); falls back to the deprecated `setOwner(String)` on 1.8 –
  1.12. Cross-version material lookup picks `PLAYER_HEAD` (1.13+) or
  `SKULL_ITEM:3` (legacy) automatically.
- `customHead(textureBase64, name, lore)` — builds a custom-textured
  head from the base64 `textures` payload that minecraft-heads.com
  publishes (the field is the value of the `textures` property on a
  Mojang `GameProfile`). Two paths:
  1. **Modern** (Paper / Spigot 1.18.1+): builds a Bukkit
     `PlayerProfile` via `Bukkit.createPlayerProfile`, sets the skin
     URL on its `PlayerTextures`, and calls
     `SkullMeta.setOwnerProfile(...)`. The URL is decoded from the
     base64 JSON without pulling in a JSON parser — a substring scan
     for the inner `"url"` value is enough.
  2. **Legacy** (1.8 – 1.17): constructs an authlib `GameProfile`
     reflectively, attaches a `Property("textures", base64)`, and
     sets the private `profile` field on `SkullMeta`. The class
     lookup tries both `com.mojang.authlib.GameProfile` and the
     relocated `net.minecraft.util.com.mojang.authlib.GameProfile`
     for the very-old Bukkit forks that shipped a relocated copy.
- If both injection paths fail (exotic forks, hardened reflection),
  the helper returns a barrier item with the same display name and
  lore so the slot is still clickable. The base64 textures used in
  `StaffMenu` (red-X close, magnifying-glass search) live as
  constants there with comments naming their minecraft-heads.com
  source so they're easy to swap.

## Time tracking

New `util/control/StaffSessionTracker.java`:

- Implements `Listener`. Records `System.currentTimeMillis()` on
  `PlayerJoinEvent` (priority `LOWEST`, so the session start beats
  any later listener that might query it) and removes the entry on
  `PlayerQuitEvent` (priority `MONITOR`).
- Backing storage is a `ConcurrentHashMap<UUID, Long>`. Lookups are
  free-threaded.
- `getSessionDuration(UUID)` returns elapsed milliseconds since
  session start (clamped at zero), defaulting to `0` if the tracker
  has not yet observed a join — covers the edge case where `/staff`
  is queried during the same tick the staff member joined.

## Categories

### Commands

- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/StaffCommand.java`
  — `/staff` executor + tab-completer. Permission-gated by
  `sfcore.staff.menu`. Tab-completer returns empty for non-permitted
  senders so the command stays hidden.

### GUIs

- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenu.java`
  — main 54-slot GUI builder: alphabetical online-player heads,
  hover profile cards, search head, custom red-X close head, and
  cross-version statistic / locale resolution.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuHolder.java`
  — `InventoryHolder` carrying the slot-to-UUID mapping, search slot,
  and close slot so the click handler can route by raw slot without
  re-walking the GUI.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffActionMenu.java`
  — 27-slot per-player action sub-menu (Warn / Mute / Kick / Tempban
  / Ban placeholders) with back arrow and reused close head.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffActionMenuHolder.java`
  — `InventoryHolder` carrying the target UUID + cached display name.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuInputManager.java`
  — chat-prompt manager for the search flow.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/gui/shared/CustomHeadUtil.java`
  — cross-version player-skull and custom-textured-head builder.

### Listeners

- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/listener/menu/StaffMenuListener.java`
  — handles clicks and drags on both staff GUI types. All clicks are
  cancelled (no item movement); drags too. Routes by raw slot to
  open the action menu, prompt the search input, run the close, or
  fire the placeholder action callback.
- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/listener/chat/StaffMenuInputListener.java`
  — cancels `AsyncPlayerChatEvent` for pending staff members and
  forwards the message body to the input manager.

### Internal

- **NEW** `src/main/java/dev/sergeantfuzzy/sfcore/util/control/StaffSessionTracker.java`
  — in-memory session-start tracker (UUID → ms timestamp).

### Wiring

- `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
  — new `staffMenuInputManager` and `staffSessionTracker` fields,
  initialized in `onEnable()` next to `warpMenuInputManager`. New
  public getters `getStaffMenuInputManager()` and
  `getStaffSessionTracker()`. `registerCommands()` adds
  `bind("staff", new StaffCommand(this))`. `registerListeners()`
  registers the session tracker, the menu listener, and the chat
  input listener.

### plugin.yml

- New `staff` command entry with aliases `staffmenu` and `sm`,
  permission `sfcore.staff.menu`.
- New `sfcore.staff.*` (default: op, with `sfcore.staff.menu` as a
  child) and `sfcore.staff.menu` (default: op) permission entries.
- Both linked into the existing `sfcore.*` and `sfcore.admin.*`
  umbrellas so a single grant carries them.

### Language strings (English + German)

- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
  — added all `admin.staff.*` keys covering the menu title, the
  per-player head display name and lore, the search head and close
  head display names and lore, the search prompt / success /
  not-online / cancelled / empty / self feedback, the action sub-
  menu title, and the per-action display name + lore + placeholder
  message for each of `warn`, `mute`, `kick`, `tempban`, and `ban`.

### Documentation

- `docs/information/about.md`
  — added a `/staff` row under **Admin (Staff)** describing the
  command, its alias list, permission node, and what each layer of
  the GUI shows.

## Verification

Maven build (`./maven/bin/mvn.cmd -DskipTests package`) completes
cleanly — only the standard ProGuard `Note:` lines for reflective
accesses remain, which are informational per `CLAUDE.md`. Output JARs:

- `target/SF-Core-1.0.0-SNAPSHOT.jar` (328 KB obfuscated)
- `target/SF-Core-1.0.0-SNAPSHOT-unobf.jar` (483 KB unobfuscated)

The reflection paths in `CustomHeadUtil` and `StaffMenu.activePlayMillis`
go through `Class.forName` and `Statistic.valueOf(String)`, so neither
ProGuard's renaming nor a missing constant on the 1.12.2 API baseline
breaks them — both jars exercise the same code paths.

## Files Modified

- `src/main/java/dev/sergeantfuzzy/sfcore/Main.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`
- `src/main/resources/plugin.yml`
- `docs/information/about.md`

## Files Added

- `src/main/java/dev/sergeantfuzzy/sfcore/command/admin/StaffCommand.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenu.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuHolder.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffActionMenu.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffActionMenuHolder.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/StaffMenuInputManager.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/shared/CustomHeadUtil.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/menu/StaffMenuListener.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/chat/StaffMenuInputListener.java`
- `src/main/java/dev/sergeantfuzzy/sfcore/util/control/StaffSessionTracker.java`

## Assumptions

- **Country detection** is per-CLAUDE.md best-effort with no network
  calls. The client locale's country segment is mapped through a
  small ISO-3166 table; unknown codes are surfaced as-is. A future
  enhancement could ship an offline GeoIP database for IP-based
  country lookup.
- **Action buttons are placeholders** for this drop. They render the
  correct localized labels and report the selected target on click,
  but no moderation pipeline fires; the user explicitly asked for
  placeholders here.
- **Pagination is not yet implemented.** With 45 player-slot capacity
  and self exclusion, servers running fewer than 46 concurrent
  players see every online player in one page. Larger servers will
  see the listing truncate at 45; pagination is a natural follow-up.

## Suggested Commit Message

```
Feature (staff): /staff overview GUI with hover profiles, report card,
search-by-name input flow, and per-player action sub-menu placeholders
```
