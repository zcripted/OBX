# Arcanum — Categorized Codex Guide Books

■ **Created:** 2026-05-27 (America/Detroit)

■ **Last Updated:** 2026-05-27 (America/Detroit)

---

## Summary

Adds admin-only, stylized **interactive guide books** ("Codex") for the Arcanum
enchantments, given per category via `/se give <player> book <category>`. Each book
lists its category's enchants alphabetically with hover tooltips, click-to-learn-in-chat,
and click-a-level-to-apply buttons.

## Categories

### Commands
- **`/sfench give <player> book <category>`** (aliases `/sfenchant`, `/sfe`) — gives a
  written-book Codex for the category. Executor needs `sfcore.enchants.give`; the
  **recipient** must hold `sfcore.enchants.book` (or be op). If the recipient lacks it,
  the executor gets a chat error **and** a title (`Access Denied`), and **no book is
  given**.
- **`/sfench bookinfo <id>`** (internal) — prints an enchant's full details in chat;
  the target of a Codex name-click.
- **`/sfench bookapply <id> <level>`** (internal) — applies the enchant to the item in
  hand; the target of a Codex level-click. **Only raises to a higher level** than what's
  already present (same/lower level is refused). Resolves the target as **main hand if
  applicable, else off-hand** — so reading the book in the off-hand applies to the weapon
  in the main hand.

### Internal
- `enchant/item/EnchantGuideBook.java` (new) — builds the interactive `WRITTEN_BOOK`.
  Enchants are sorted A–Z; each name carries the shared `EnchantHover.tooltip` (name,
  category, rarity, max level, applicable items, description) and a `RUN_COMMAND`
  click to `bookinfo`; each level is a `[N]` button with an apply hover + `bookapply`
  click. Built with the Spigot/BungeeCord chat-component API (works 1.8.8 → 1.21.x) and
  degrades to plain-text pages if components are unavailable on a fork.
- `enchant/command/SfEnchantCommand.java` — routes `give … book`, `bookinfo`, and
  `bookapply`; extracts `renderInfo(...)` (shared by `info` + `bookinfo`); adds the
  recipient-permission gate with a title, the held-item resolver, the upgrade-only rule,
  off-hand helpers, and tab completion for `book` + category.
- `language/MessageDefaults.java` — new EN+DE messages: `enchant.book.received`,
  `enchant.book.sent`, `enchant.book.recipient-no-permission` (+`-title`/`-subtitle`),
  `enchant.book.lower-level`, `enchant.book.no-item`.

### Permissions (plugin.yml)
- `sfcore.enchants.book` (default `op`) — required to **receive** a Codex and to use its
  click actions (`bookinfo`/`bookapply`); added as a child of `sfcore.enchants.*`.
- `/sfench` usage string updated; `docs/information/about.md` rows added.

## Behavior notes / assumptions
- **"Held item in hand":** after clicking a link the book closes and the main hand holds
  the book, so `bookapply` prefers the main hand only when it's a valid (non-book)
  target and otherwise falls back to the off-hand. Recommended flow: weapon in **main
  hand**, Codex in **off-hand**, right-click to open → clicks apply to the main-hand
  weapon. (On 1.8 with no off-hand, hold the target item and the apply still resolves
  via main hand once the book closes.)
- **Upgrade-only:** clicking a level that equals or is below the item's current level is
  refused (`enchant.book.lower-level`); conflicts/caps/applicability are still enforced
  by the normal `EnchantService.apply` path.
- Book interactivity uses the BungeeCord component API directly (same one
  `ComponentMessenger` uses); the deprecated `HoverEvent(BaseComponent[])` + `setLegacy`
  path matches the existing chat-hover code.

## Testing
- Maven build: exit 0, both jars produced (obf ~612 KB, unobf ~889 KB). ProGuard
  `Note:` lines only. Compile-verified only — in-game checks needed for: the recipient
  permission gate (chat + title, no book) vs. a permitted recipient receiving it;
  opening the book and confirming hover tooltips, name-click chat info, and level-click
  apply (including the upgrade-only refusal and the main-hand/off-hand resolution).

## Suggested Commit Message
```
Feature (enchants): Admin Codex guide books — /sfench give <player> book <category> (interactive hover/click-to-learn/click-to-apply)
```
