# 📒 OBX Commit Logs

Per-commit change logs, **newest at the top**. Each entry links to a detailed, easy-to-read
breakdown written as release/patch notes.

## How these work
- **One markdown per commit**, stored in a **date-stamped folder** named for the day it was
  worked on (Eastern): `docs/commits/YYYY-MM-DD/<slug>.md`.
- Every log carries a **header table** (status, created, **last updated** in EST) that is kept
  current whenever anything is added, modified, or removed.
- Each log has a **plain-English summary** for patch notes, a **categorized breakdown**
  (improvements / fixes / patches), and **direct links** to every file it touched.
- **Ordering:** newest commit at the top of this index; inside a log, newest changes at the top.

## Commits — newest → oldest

### 2026-06-07
| Commit | Status | One-liner |
|--------|--------|-----------|
| [Custom Player-Command Console Log](2026-06-07/custom-command-console-log.md) | 🟡 Ready to commit | Vanilla "issued server command" line replaced by a styled, configurable [COMMAND] log with world/date/time context; suppression via a version-tolerant log4j proxy filter; console.command-log config section. |
| [Command Log Hardcoded + Purple Consistency](2026-06-07/command-log-hardcoded-purple-fix.md) | 🟡 Ready to commit | Command log config section removed — values hardcoded; [COMMAND] now uses same ANSI pipeline as OBX brand tags so both render identical light purple. |
| [Unconfigured-Webhook Warnings](2026-06-07/webhook-unconfigured-warnings.md) | 🟡 Ready to commit | Console warns at startup + admins on join (above the welcome MOTD, hover lists the paths) while Discord webhook settings hold placeholder/blank values; stops once configured; toggle via /obx warn (instant), Admin GUI tile, or config (reload). |
| [Reply Button Centered](2026-06-07/reply-button-centered.md) | 🟡 Ready to commit | PM reply button un-bolded, » lead removed, and horizontally centered in chat via a new pixel-measured padding helper; still click-runs /rply. EN/DE/ES. |
| [Shop Quantity Menu](2026-06-07/shop-quantity-menu.md) | 🟡 Ready to commit | Clicking a shop item opens a per-item submenu: buy/sell toggle, ±1/5/10/30/50 amount steps, live receipt (unit price, total, balance, stock), guarded confirm; shift-click quick actions kept. EN/DE/ES. |
| [Diagnostics Fixes + Death Grouping Default](2026-06-07/diagnostics-fixes-deathdrop-default.md) | 🟡 Ready to commit | Per-issue hovers on the diagnostics Errors row; false "moderation.yml missing" removed (moderation is SQLite, the yml is migrate-once legacy); deathdrop module now enabled by default. |
| [Shop Result Hover Tooltips](2026-06-07/shop-result-hover-tooltips.md) | 🟡 Ready to commit | Buy/sell/bulk-sell result messages carry a full receipt hover (item, amount, unit price, boost, gain/cost, balance before → after, stock left); bulk sales list each item type sold. EN/DE/ES. |
| [Chat-Prompt Input Leak Fix](2026-06-07/chat-prompt-input-leak-fix.md) | 🟡 Ready to commit | Prompt replies (shop-editor prices/categories/cancel, warp & staff menu prompts, /spawn delete confirm) were broadcast to public chat before being swallowed — the chat formatter at the same HIGHEST priority registered earlier and hand-delivered the line first; prompt capture moved to LOWEST. |
| [/unbreakable Fix — Flag Was Never Applied](2026-06-07/unbreakable-reflection-fix.md) | 🟡 Ready to commit | Command always replied "not supported": reflection on the package-private CraftMetaItem threw IllegalAccessException on every server; now uses the direct ItemMeta API (1.11+) with a Spigot-legacy fallback for 1.8.8–1.10. |
| [String-Encryption Obfuscation Layer + Permission-Cycle Crash Fix](2026-06-07/string-encryption-obfuscation-layer.md) | 🟡 Ready to commit | Post-ProGuard ASM step encrypts all 25,390 string literals (decoded at runtime); booting the obfuscated jar caught + fixed a pre-existing self-referential plugin.yml permission that StackOverflowed on enable; new runServerObf verification task. |
| [Economy Round 11 — Verification Gap Closures](2026-06-07/economy-round-11-gap-closures.md) | 🟡 Ready to commit | Shop editor GUI made real (click handlers + YAML persistence), AH tax/fees → visible server account (/eco server), /ah search/confirm/bid/auction entry points, digest top movers + sinks + async webhook, real banknote craft redemption, rank-based bank tiers, claim-upkeep sink (GriefPrevention); bank-history SQL fix + withdrawn-all typo fix. |
| [Economy Round 10 — Feature-Complete](2026-06-07/economy-round-10-feature-complete.md) | 🟡 Ready to commit | Auction storage-race/claim-loss fixes, bank atomicity, PAPI caching, Vault unregister, craft arbitrage patches, shop stock limits + dynamic pricing, bank GUI, sell wand, shop editor, economy sinks (weekly-top, anvil fee), report service, pay confirm + tax, daily sell cap, rank multipliers, log filter, i18n parity. |

### 2026-06-06
| Commit | Status | One-liner |
|--------|--------|-----------|
| [Economy Round 9 — Edge Cases, Exploits & Hardening](2026-06-06/economy-round-9-edge-cases-exploits-hardening.md) | 🟡 Ready to commit | Completed shop stock limits; fixed auction list/claim/buy item-loss, bank deposit atomicity, transaction rollback, %obx_balance% caching, Vault unregister; wired obx.ah.admin; closed golden-carrot/baked-potato/cookie craft arbitrage; docs + i18n parity. |
| [Scrub Live Discord Webhook from Default Config](2026-06-06/scrub-discord-webhook-config.md) | 🟡 Ready to commit | Final release blocker closed: live webhook URL + dev server/channel IDs removed from config.yml, Discord logging ships disabled with in-line setup guide. Webhook must still be revoked in Discord. |
| [Round 7 Fix Pass — 12 Audit Findings Closed](2026-06-06/round-7-fix-pass.md) | 🟡 Ready to commit | Thread-safe formats, backpack save/key guards, PM mute UUID, warp prompt cleanup, cancel-on-move, hologram reload safety, 4 lang keys, economy stats cache, sell-GUI death guard. Webhook (#1) deferred. |
| [Economy Tile Theme + Panel Restructure](2026-06-06/economy-tile-theme-and-panel-restructure.md) | 🟡 Ready to commit | Live themed Economy hover preview in the Admin Menu; Economy Control tiles 10–14, shop centered at 22, Back/Close on their own row. |

### 2026-06-05
| Commit | Status | One-liner |
|--------|--------|-----------|
| [Economy Phases 1–3 — Control Panel, Audit Log & Player Shop](2026-06-05/economy-control-panel-shop-and-audit.md) | 🟡 Ready to commit | Live Economy Control Panel in the Admin GUI, economy_log audit trail + /eco log + offline admin actions, confirm menus, /shop with YAML categories, 1.8.8→latest worth database. |
| [Update Box — Download & Release Notes Buttons](2026-06-05/update-box-download-and-notes-buttons.md) | 🟡 Ready to commit | In-game [Download]/[Release Notes] buttons on the update box; plain text link rows on console; one shared renderer. |
| [Discord Invite Link Update](2026-06-05/discord-invite-link-update.md) | 🟡 Ready to commit | discord.gg/zN3UQyKdfD → discord.gg/UxktSyT9Ag project-wide (README, config, i18n, code, tablist, docs). |
| [/pl Console Fix — Grouped Names, No Timestamps](2026-06-05/pl-console-grouping-and-timestamps.md) | 🟡 Ready to commit | Console /pl rows render under their platform header via the direct writer — no logger timestamps in the box. |
| [ASCII Wordmark Console Banner](2026-06-05/ascii-wordmark-console-banner.md) | 🟡 Ready to commit | Gradient OBX logo banner with live hook/storage status markers and Docs/Support links. |
| [Backpack + Health Check + Box-Style Update Messages](2026-06-05/backpack-health-check-and-update-boxes.md) | 🟡 Ready to commit | Dupe-guarded portable backpack, staff /health report with hover/click actions, box-style update messages, 15-min release checks, first-join-only starter kit. |
| [Update Notifications — Default-ON + Startup/Periodic Checks](2026-06-05/update-notifications-default-on.md) | 🟡 Ready to commit | Startup console check, hourly once-per-release announce, default-ON staff notifications with SQLite-persisted opt-out. |
| [Round 5 — Market-Readiness Re-Audit Patches](2026-06-05/round-5-market-readiness-patches.md) | 🟡 Ready to commit | Folia pack-thread fix, combat-state leak closure, + 12 low-severity polish fixes. |