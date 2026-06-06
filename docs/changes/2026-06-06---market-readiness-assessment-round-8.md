# Market-Readiness Assessment — Round 8 (Post-Fix Verification + Live Smoke Test)

■ **Created:** 2026-06-06 4:57 pm

■ **Last Updated:** 2026-06-06 6:27 pm

## Method

Round 8 = (a) adversarial line-by-line review of all 12 Round-7 fixes hunting
regressions, (b) a fresh-eyes sweep of corners earlier rounds covered lightly
(resource-pack hosting, MOTD ping path, language persistence, playtime, preview/help,
Vault proxy, command overrides, enchant GUI/book, reload coverage), and (c) a **live
Paper 1.21.4 boot smoke test** exercising the new code paths from console. All agent
claims re-triaged against actual code semantics before inclusion.

## Fix-pass verification: ALL 12 FIXES CORRECT, ZERO REGRESSIONS

Each fix independently verified at its call sites: ThreadLocal formats (all call sites),
backpack null-save guard + legacy-row load handling + key normalization placement,
SWAP_OFFHAND guard placement and 1.8 safety, UUID mute + draft gate + cast safety, warp
quit-cleanup listener registration, Consumer-based confirm actions + full economy-panel
slot map end-to-end (open/click/refresh/routing all on 4/11/12/13/14/15/22 + 30/32),
EconomyStats thread-safety + all three consumer sites, cancel-on-move null-safety +
key existence + registration, hologram reload try/fallback/finally + single load(),
sell-GUI death-drop legality + no double payout, and all 4 language keys present in
EN/DE/ES.

## Live boot smoke test: PASS (zero exceptions)

Paper 1.21.4, clean enable in 14.5s. Exercised from console: `pl` (grouped box ✓),
`health` (full report ✓), `eco log` (log box ✓), `shop reload` (6 categories ✓),
**`eco give TestOffline 100` — offline admin action end-to-end ✓** (account auto-seeded
at starting balance + deposit → "Balance: $200.00"), clean disable banner, clean stop.

## Fresh-eyes sweep — triaged results

**No criticals. No highs survived triage:**
- *Vault proxy Object-method gap* (claimed HIGH) → **false**: `getClass()`/`wait`/`notify`
  are final and never route through an InvocationHandler; `equals`/`hashCode`/`toString`
  are handled. Non-issue.
- *PlaytimeService flush ConcurrentModificationException* (claimed MEDIUM) → **false**:
  ConcurrentHashMap iterators are weakly consistent and never throw CME. A benign
  lost-longest-session sliver remains (LOW).
- *PreviewCommand parse exceptions* → op-only diagnostic; LOW polish (add try/catch + feedback).

**Genuine LOWs for a future patch:** EnchantGuideBook silent component fallback (add a
WARN log) · EnchantAdminMenu 32-char title truncation can split a color code ·
MOTD ProfileFactory reflection not cached per sample-type · `/language` whole-map async
save is last-write-wins under spam · resource-pack `/pack/<sha1>.zip` URL heuristic can
false-positive on lookalike CDN paths.

**Verified solid this round:** resource-pack manager opens NO network listener · MOTD
YAML parsing safe · enchant book pagination far under vanilla limits · help GUI
pagination clamped · language reads bulletproof · **`/obx reload` coverage complete**
(config, languages, data, motd, resource pack, all modules, update checker — no stale
store references).

## Verdict

**MARKET-READY, with exactly one remaining blocker: #1 — the live Discord webhook +
server/channel IDs in the default config.yml** (deferred by request in the fix pass; the
webhook should also be revoked in Discord since the URL is in git history of both repos).

Everything else from Rounds 6–7 is fixed and adversarially verified; the build is green,
EN/DE/ES parity holds, both jars produce, and the live boot is clean. Remaining LOWs are
post-launch polish, not release gates.

| Area | Status |
|---|---|
| chat · commands · economy · enchant · gui · hologram · hub · jail · kit · language · listener · message/messaging · moderation · nickname · platform · scoreboard · storage · tablist · util | ✅ all clear |
| config (webhook) | ✅ blocker #1 CLOSED 2026-06-06 6:27 pm — config scrubbed + shipped disabled (see [scrub-discord-webhook-config](2026-06-06---scrub-discord-webhook-config.md)); webhook revocation in Discord still required (manual, outside repo) |

**Update 2026-06-06 6:27 pm:** Blocker #1 resolved in-repo — live webhook URL +
server/channel IDs scrubbed from `config.yml`, Discord moderation logging now ships
`enabled: false` with in-line setup instructions. Build re-verified green, both jars
produced. The only remaining action is **manual**: revoke the webhook in Discord (the
URL persists in git history of both repos until the credential itself is retired).
