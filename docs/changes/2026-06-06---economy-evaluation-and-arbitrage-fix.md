# Economy System Evaluation + Arbitrage Price Fix

■ **Created:** 2026-06-06 5:33 pm

■ **Last Updated:** 2026-06-06 5:33 pm

## Arbitrage scan (new analysis) — 3 issues found, fixed, re-scan clean

Programmatic scan of every shop entry (per-unit buy vs per-unit sell) and every
shop-buy vs worth.yml-sell pair, plus manual verification of crafting loops
(nugget↔ingot↔block for gold/iron/diamond/emerald/coal, wheat↔hay, log→planks):

| Exploit | Loop | Fix |
|---|---|---|
| **GOLD_NUGGET money printer** | shop buy 16 @ $6.00 ($0.375/u) → sell @ $0.40/u = **+$0.40 per cycle, automatable** (both shop-internal and via worth.yml) | `shops/ores.yml` buy 16 → **$8.00** ($0.50/u), sell → **$0.30**; `worth.yml` gold_nugget 0.40 → **0.30** |
| **Legacy INK_SACK break-even loop** (1.8–1.12 only) | farming.yml buy $0.25/u = worth.yml sell $0.25 → zero-loss infinite trading | `worth.yml` legacy `ink_sack` 0.25 → **0.15** |

Post-fix scan: **0 arbitrage pairs**; all crafting loops verified lossy (sink-positive).

## Files
- `plugin/src/main/resources/worth.yml` — gold_nugget 0.30, legacy ink_sack 0.15.
- `plugin/src/main/resources/shops/ores.yml` — GOLD_NUGGET buy 8.00 / sell 0.30.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Balance (economy): close GOLD_NUGGET arbitrage loop + legacy INK_SACK break-even (shop/worth price fix)
```
