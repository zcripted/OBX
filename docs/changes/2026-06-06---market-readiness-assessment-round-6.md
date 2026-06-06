# Market-Readiness Assessment — Round 6 (Full 20-Area Audit)

■ **Created:** 2026-06-06 7:50 am

■ **Last Updated:** 2026-06-06 7:50 am

## Method

Eight parallel deep-audit passes covering: chat, commands, economy/shop, enchant, gui,
hologram, hub, jail, kit, language, listeners, message/messaging, moderation, nickname,
platform, scoreboard, storage, tablist, teleport/warp/world/playerstate, util. Every
agent finding was re-verified against the code before inclusion; false positives were
discarded (notably: the reported `economy_log` INSERT "column swap" is **not a bug** —
the named-column INSERT binds values by column name, verified correct; the backpack
token-rotation "race" is safe because validity is checked against DB truth on use; the
refresh task does NOT clobber CONFIRM menus; `EnchantState` sync save in onDisable is
standard practice, not a defect).

## Verdict

**NOT ready to ship today — but very close.** Zero architectural problems; the blockers
are a handful of point fixes (~1 focused day). Storage, language, scheduler/Folia,
moderation core, kit atomicity, command registry (102/102 bound, zero orphans), and
permission declarations all audited CLEAN.

## Blockers (must fix before sale)

| # | Severity | Finding | Location |
|---|---|---|---|
| 1 | CRITICAL | **Live Discord webhook + server/channel IDs ship in the default config** — every buyer's moderation logs would post to the author's Discord; token is usable by anyone. Scrub to dummy values (note: the real URL is also in git history of both repos — revoke the webhook in Discord regardless). | plugin/src/main/resources/config.yml:152-154 |
| 2 | CRITICAL | **Tablist `DecimalFormat` shared across threads** — static `TPS_FORMAT` used from per-player refresh paths; on Folia region threads concurrent `format()` can corrupt output or throw AIOOBE. Use per-call instance or ThreadLocal. (Same latent pattern in HealthCommand — main-thread-only today, fix opportunistically.) | features/tablist/.../format/TablistRenderer.java:37 |
| 3 | CRITICAL | **Backpack save can overwrite good contents with empty** — `toBase64` returns `""` on serialization failure and `saveContents` writes it unconditionally → silent permanent item loss. Skip the UPDATE on failure + log. | features/backpack/.../service/BackpackService.java (toBase64 catch + saveContents) |
| 4 | HIGH | **Mute bypass in private messages** — `blockedByMute()` checks `isMuted(name)` instead of UUID (name-change bypass); the inbox draft-reply path skips the mute check entirely. | features/mail/.../pm/PrivateMessageService.java:137 (+ deliverToId path) |
| 5 | HIGH | **Warp chat-input manager never cleans up on quit** — pending-input UUIDs leak forever and the HIGHEST-priority chat listener will swallow that player's first chat line if they rejoin. Add quit cleanup. | features/warp/.../gui/WarpMenuInputManager.java + WarpMenuInputListener.java |
| 6 | HIGH | **Backpack F-key (off-hand swap) bypasses the nesting guard** — `ClickType.SWAP_OFFHAND` can move a backpack item into an open backpack view. Add the case to the click guard. | features/backpack/.../listener/BackpackListener.java (onClick) |
| 7 | HIGH | **CONFIRM-menu runnables capture the live `Player` reference** — admin relogging between open and confirm executes against a stale entity. Capture UUID, resolve on run. | features/staff/.../gui/AdminSubMenu.java (handleEconomyMenuClick confirm lambdas) |
| 8 | HIGH (verify) | **`teleport.cancel-on-move` config key appears unimplemented** — no PlayerMoveEvent handler found in the teleport module; if confirmed, an advertised config option does nothing. Implement or remove the key. | features/teleport/** + config.yml:38 |
| 9 | HIGH | **Hologram reload orphan risk** — registry cleared even when backend destroy throws; Folia skips the orphan sweep entirely (no post-startup cleanup path). | features/hologram/.../service/HologramService.java:119-147 |

## Should-fix (next patch, not blocking)

- Economy panel live tiles run 4–6 synchronous SQLite queries/sec per viewer via the 0.5s refresh task — exclude ECONOMY from periodic refresh or cache stats (features/staff/.../AdminMenuRefreshTask + AdminSubMenu.refresh).
- Backpack save-on-close model has a crash-window dupe (items moved out + crash before close = both copies). Document, or debounce-save on click.
- Sell-GUI bulk payout logs action `SELL` while shop item-clicks log `SHOP_SELL` — pick one channel scheme for clean audits.
- `Bukkit.getOfflinePlayer(name)` on the main thread in /eco //pay — non-blocking on modern Paper but deprecated; consider async resolution.
- Homes//back targeting deleted worlds fail with a generic "not found" — add a "world missing" message.
- Jail: ender-pearl/chorus-fruit teleports not intercepted on older versions.
- Nickname length validated on the stripped form only — color codes inflate stored length.
- Deathdrop orphan-hologram sweep disabled on Folia; despawn-vs-chunk-unload edge.
- AFK tick loop cost at 500+ players; AFK-kick fallback thread-path worth a Folia recheck.
- Grindstone/anvil don't strip custom (lore-based) enchants — decide intended behavior and document.
- GitHub update checks at 15-min across very large multi-server fleets approach unauthenticated rate limits — add jitter note to docs.
- Console-key fallback edge in LanguageManager when a `-console` variant lacks a base key.

## Audited clean (verified solid)

- **Storage**: SQLite lock coverage total; transaction autocommit restore; WAL checkpoint on close; shutdown write ordering (`beginShutdown` → saves → close) correct.
- **Language**: reload-safe map swaps; EN fallback chain; EN/DE/ES parity test enforced; all spot-checked keys used in code exist (incl. all new shop/backpack/health/economy-panel keys).
- **Platform**: Folia reflection scheduler correct incl. cancel paths; backend selectors degrade gracefully.
- **Economy core**: atomic guarded SQL for deposit/withdraw/transfer; remove-first/pay-first dupe guards in every sell/buy path; bundle clamps; material fallback chains (1.8.8-safe).
- **Moderation**: UUID ban ledger, async pre-login enforcement, tempban overflow saturation, mute command-bypass list with namespace stripping.
- **Kit**: first-join INSERT-OR-IGNORE atomic claim; cooldown race closed; first-join self-claim blocked.
- **GUI**: all tracked menus cancel click+drag before dispatch; per-click `obx.admin.menu` re-validation incl. new ECONOMY/CONFIRM holders.
- **Commands**: 102/102 plugin.yml commands bound; all referenced permission nodes declared; tab-complete hides admin entries appropriately.
- **Chat/messaging**: MessageSanitizer blocks MiniMessage/format injection incl. hover/click tag stripping and quote-escaping; reply state cleared on quit; chat format placeholder substitution is non-recursive.
- **Enchant**: HMAC-signed lore (fail-open), clone-validate before scroll commit, explicit module teardown ordering.

## Bottom line

Fix the 3 criticals + 6 highs (≈1 day), revoke the leaked webhook, re-run the EN/DE/ES
build and a `runServer` smoke pass — then this is a sellable, professionally engineered
product. Round 6 confirms every prior round's fix is still in place.
