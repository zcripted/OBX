# SF-Core — Arcanum Enchantment Reference

A complete catalogue of every **Arcane** and **Cursed** enchantment in the SF-Core
"Arcanum" module. Enchantments are grouped by **category (A–Z)**, and within each
category listed **alphabetically (A–Z)**. Each entry lists its description, effects,
applicable items, available levels, and a short in-game test.

> **Source of truth:** generated from `src/main/resources/enchants/*.yml`. There are
> **100 enchantments** total: Combat (53), Cursed (8), Defense (8), Farming (6),
> Mystic (8), Tools (8), Utility (9).

---

## How to obtain & apply (for testing)

- **Direct apply (op):** hold the target item and run `/sfench apply <id> <level>`
  (aliases `/sfenchant`, `/sfe`). For armor/boots/elytra, apply while holding it, then
  equip it.
- **As a scroll:** `/sfench give <player> <id> <level> [amount]` — then drag the scroll
  onto the item (or combine in an anvil).
- **Admin GUI:** `/sfench admin` to browse, give, and apply with full access.
- **Browse (players):** `/enchants` (read-only). `/enchants settings` toggles your own
  combat effects (kill banners + combat action-bar feedback).
- **Inspect:** `/sfench info <id>` and `/sfench list [category]`.

Use the `id` shown in each entry's header for commands.

> Most combat feedback (action bars, damage banners, particles, sounds) is gated by the
> `combat_global` block in `enchants/config.yml` and by each player's `/enchants settings`
> preference. If you don't see feedback while testing, confirm both are enabled.

---

## Legend

**Rarity tiers (low → high):** `Common` · `Uncommon` · `Rare` · `Epic` · `Legendary` · `Mythic`

**Item tags** (what "applies to" expands to):

| Tag | Items it matches |
|-----|------------------|
| `WEAPON` | Swords, Axes, Tridents, Maces |
| `RANGED` | Bows, Crossbows, Tridents |
| `ARMOR` | Helmets, Chestplates, Leggings, Boots |
| `TOOL` | Pickaxes, Axes, Shovels, Hoes, Shears |
| `SWORD`/`AXE`/`PICKAXE`/`SHOVEL`/`HOE` | the matching tool type |
| `BOW`/`CROSSBOW`/`TRIDENT`/`MACE` | the matching item |
| `HELMET`/`CHESTPLATE`/`LEGGINGS`/`BOOTS` | the matching armor piece |
| `SHIELD`/`ELYTRA`/`FISHING_ROD`/`SHEARS` | the matching item |
| `FOOD` | edible items |
| `CROP_SEED` | seeds & plantable crops (wheat/beetroot/melon/pumpkin seeds, carrots, potatoes, nether wart, …) |
| `ANY` | any item |

> **Cross-version note:** some target items only exist on newer servers (Trident 1.13+,
> Mace 1.21+, Elytra 1.9+). The enchant can still be defined everywhere; it simply has
> nothing to attach to on versions that lack the item. **Cursed** enchants do not count
> toward the per-item enchant cap by default.

---

## Table of Contents

1. [Combat](#1-combat) — 53
2. [Cursed](#2-cursed) — 8
3. [Defense](#3-defense) — 8
4. [Farming](#4-farming) — 6
5. [Mystic](#5-mystic) — 8
6. [Tools](#6-tools) — 8
7. [Utility](#7-utility) — 9

---

# 1. Combat

Red category. On-hit, kill-trigger, ranged, and reactive combat power. Most damage
bonuses combine into one multiplier applied to the hit. Hitting a target with **any**
Combat-category enchant floats a live health bar above it for the duration of combat
(a second line shows Weakness/Slowness cooldowns when applied).

### Apex Predator · `apex_predator`
- **Rarity:** Mythic · **Max Level:** 1 · **Applies to:** any Weapon (Swords, Axes, Tridents, Maces)
- **Description:** The ultimate capstone — raw power, lethal crits, and a chance to harvest scrolls from the slain. Only one Apex Predator can be active per player.
- **Effects:** +15% damage, +10% crit chance, +10% crit damage, killstreak buffs amplified ×1.5, and a 5% chance on kill to drop a random Rare/Epic scroll. Gold burst on every kill. Carrying a second Apex item is inert and warns you (throttled).
- **Levels:** 1 only (fixed values above).
- **Test:** Apply to a sword and attack mobs — expect noticeably higher, frequently-critical damage and gold sparks on kills. Kill ~20 mobs to see a scroll drop. Put a second Apex weapon in your inventory and hit something to get the "only one can be active" warning.

### Battle Roar · `battle_roar`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Swords, Axes, Maces
- **Description:** Sneak + right-click to roar: buff yourself and debuff nearby enemies (on a cooldown).
- **Effects:** Self Strength (I→II) + Resistance (Lv3), and Weakness/Slowness on enemies within 6→10 blocks. Cooldown 60s → 45s → 30s. Affected foes show a floating Weakness/Slowness readout (level + remaining time) above their head.
- **Levels:** 3 — radius 6/8/10, buff 100/120/160 ticks, debuff 60/80/120 ticks, cooldown 60/45/30s.
- **Test:** Apply to a sword, stand near mobs/another player, **sneak and right-click**. You gain Strength (and Resistance at Lv3); nearby foes get Weakness/Slowness; a roar sound + shockwave plays. Re-trigger immediately to confirm the cooldown action-bar message.

### Berserker's Rage · `berserkers_rage`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** Axes, Maces
- **Description:** Damage scales as your own HP drops.
- **Effects:** +damage per missing 20% of your health, up to a cap (+20%→+60%). Grants Strength while fighting with it (refreshed each hit) — Strength I normally, Strength II once below the Lv4 25% HP threshold. A damage-% action-bar HUD and the target's floating health bar show during the ~6 s combat window.
- **Levels:** 4 — per-missing-fifth 0.05/0.08/0.12/0.15, cap +20/32/48/60%.
- **Test:** Apply to an axe, take damage to drop low on HP, then hit a mob at full vs. low HP and compare damage — it should rise as you get hurt. At Lv4, drop below 25% HP and check for a Strength effect.

### Bloodletter · `bloodletter`
- **Rarity:** Rare · **Max Level:** 5 · **Applies to:** Swords, Axes
- **Description:** Hits cause bleeding — damage over time that ignores armor.
- **Effects:** Applies a bleed DoT (1→3 dmg per tick interval) over 80→200 ticks; Lv5 has a 25% refresh chance to re-apply. Spawns red blood shards on hit and each tick, floats a live blood-loss HUD (health bar + `-X/s`) above the target, and erupts a blood splatter + a `☠ SLAIN` title to the killer on death.
- **Levels:** 5 — damage 1/1/2/2/3, period 40/40/40/30/30 ticks, duration 80/120/120/160/200 ticks.
- **Test:** Apply to a sword, hit a mob once, then **stop hitting** — it keeps taking periodic damage (with blood particles) until the bleed expires. Works through armor.

### Bloodthirst · `bloodthirst`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Heal a portion of the damage you deal.
- **Effects:** Heals 5%→20% of damage dealt; Lv4 also restores +1 heart on a kill. While actively healing in combat, an animated "Restoring +N%" action-bar bar shows; the Lv4 kill bonus fires a `❤ BLOODTHIRST` title.
- **Levels:** 4 — heal 5/10/15/20%, kill bonus 0/0/0/1 heart.
- **Test:** Take some damage, apply to a sword, then attack mobs — your health ticks up as you deal damage. At Lv4, secure a kill at low HP and watch for the extra heart.

### Bonecrusher · `bonecrusher`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Maces, Axes
- **Description:** Devastates the undead and shatters armor durability on critical hits.
- **Effects:** +25%→+80% damage vs. undead; crits chip 5→25 durability off the target's armor. Lv3+ adds brief Weakness; Lv4 has a 30% shield-disable chance. Shows the swing damage on your action bar, a chat popup when a shield is shattered, and the target's health bar + a live Weakness cooldown on the floating HUD.
- **Levels:** 4 — undead bonus 0.25/0.40/0.60/0.80, crit durability 5/10/15/25.
- **Test:** Apply to an axe and hit zombies/skeletons for clearly higher damage. Crit (fall onto) an armored target and inspect their armor durability dropping. At Lv4, hit a shield-raised player to test the disable.

### Brawler's Grit · `brawlers_grit`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Taking a hit briefly empowers your next attack.
- **Effects:** After being hit, your next attack within the window deals +10%→+45%. Lv3 adds Speed, Lv4 adds Resistance on that empowered swing.
- **Levels:** 4 — window 3/4/4/4s, bonus 0.10/0.20/0.30/0.45.
- **Test:** Apply to a sword, let a mob hit you, then immediately strike back — that hit lands harder (and grants Speed/Resistance at Lv3/4). Wait past the window and the bonus is gone.

### Chain Lightning · `chain_lightning`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** Swords, Tridents
- **Description:** Strikes arc to nearby enemies on hit.
- **Effects:** Lightning jumps to 1→4 nearby enemies within 3→6 blocks, each dealing 25%→60% of the hit; Lv4 adds particles.
- **Levels:** 4 — jumps 1/2/3/4, range 3/4/5/6, damage 25/35/45/60%.
- **Test:** Apply to a sword, gather 3+ mobs close together, and hit one — damage should arc to the neighbors (visible arcs at Lv4).

### Cleave · `cleave`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Axes, Swords
- **Description:** Strikes carve a forward cone, hitting everything in front of you.
- **Effects:** Damages all enemies in a 60°→90° cone out to 3→5 blocks for 50%→80% of the hit; Lv3 also applies bleed. Conflicts with Whirlwind/Pulverize.
- **Levels:** 3 — cone 60/75/90°, range 3/4/5, damage 50/65/80%.
- **Test:** Apply to an axe, line up several mobs in front of you, and swing once — all foes in the forward arc take damage.

### Combo Strike · `combo_strike`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords
- **Description:** Consecutive hits on one target build to a powerful finisher.
- **Effects:** Each hit on the same target ramps damage; at the hit threshold (3→6) a finisher bursts (+50%→+150%) and resets. Conflicts with Killstreak/Soulreaver.
- **Levels:** 4 — hits 3/4/5/6, step +0/25/20/20%, finisher +50/75/100/150%.
- **Test:** Apply to a sword and repeatedly hit the **same** mob — watch the combo counter on the action bar climb, then the finisher hit. Switching targets resets the combo.

### Concussion · `concussion`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Maces, Axes
- **Description:** Hits may daze the target with Slowness and Mining Fatigue.
- **Effects:** 20%→50% chance to apply Slowness + Mining Fatigue for 40→80 ticks; Lv4 adds Nausea.
- **Levels:** 4 — chance 20/30/40/50%, slow I/I/II/II, fatigue I/I/I/II.
- **Test:** Apply to a mace/axe and hit a mob repeatedly; expect frequent Slowness/Mining Fatigue (and Nausea on a player at Lv4).

### Crit Mastery · `crit_mastery`
- **Rarity:** Rare · **Max Level:** 5 · **Applies to:** any Weapon
- **Description:** Increased critical hit chance and critical hit damage.
- **Effects:** +5%→+30% bonus crit chance and +10%→+60% crit damage, stacking on top of vanilla crits.
- **Levels:** 5 — crit chance 0.05/0.10/0.15/0.20/0.30, crit bonus 0.10/0.15/0.25/0.40/0.60.
- **Test:** Apply to a weapon and attack a target dummy/mob — crits (gold sparks + sound) should land far more often and hit harder than vanilla.

### Devastator · `devastator`
- **Rarity:** Legendary · **Max Level:** 4 · **Applies to:** Maces
- **Description:** Falling smash attacks hit far harder and shake the ground on impact.
- **Effects:** While falling, +50%→+150% smash damage plus a ground shockwave (radius 2→5, 2→6 damage) with knockback.
- **Levels:** 4 — smash bonus 0.50/0.75/1.00/1.50, shockwave radius 2/3/4/5.
- **Test:** Apply to a mace (1.21+), jump/fall onto a mob — the smash deals bonus damage and knocks back nearby enemies with a shockwave.

### Endless Hunger · `endless_hunger`
- **Rarity:** Mythic · **Max Level:** 3 · **Applies to:** Swords, Axes
- **Description:** Feeds on every kill — each milestone grants a permanent +1% damage stack on the weapon (persists across logins).
- **Effects:** Every 10/8/5 kills = a permanent +1% damage stack, capped at +15%/+25%/+40%. The weapon's name shows `[Hunger ×N]`; a milestone fires a flame aura + banner.
- **Levels:** 3 — kills/stack 10/8/5, cap +15/25/40%.
- **Test:** Apply to a sword and farm kills; every Nth kill the `[Hunger ×N]` suffix increments (milestone aura plays). Relog and confirm the count/name persists, then verify higher damage.

### Executioner · `executioner`
- **Rarity:** Rare · **Max Level:** 5 · **Applies to:** Axes, Maces
- **Description:** Deal bonus damage to wounded targets.
- **Effects:** +10%→+60% damage to targets below the HP threshold (50%→25%); Lv5 adds a heavy bonus to targets under 10% HP.
- **Levels:** 5 — threshold 50/50/40/30/25%, bonus +10/15/25/40/60%.
- **Test:** Apply to an axe, wound a mob below the threshold, then hit it — the finishing blows deal extra damage vs. low-HP targets.

### Executioner's Cry · `executioners_cry`
- **Rarity:** Legendary · **Max Level:** 3 · **Applies to:** Axes, Maces
- **Description:** Each kill lets out a terrifying cry that weakens and slows nearby foes.
- **Effects:** On kill, foes within 4→8 blocks get Slowness (and Weakness at Lv2+) for 60→140 ticks, with a smoke shockwave, fear sound, and an EXECUTIONER banner.
- **Levels:** 3 — radius 4/6/8, duration 60/100/140 ticks, slow I/I/II, weakness 0/I/I.
- **Test:** Apply to an axe, gather a group of mobs, and **kill one** — survivors nearby are slowed/weakened and an EXECUTIONER banner shows over you.

### Frostbite · `frostbite`
- **Rarity:** Rare · **Max Level:** 5 · **Applies to:** any Weapon
- **Description:** Hits chill the target, stacking slowness and eventually mining fatigue. Conflicts with Soulfire.
- **Effects:** 20%→60% chance on hit to apply Slowness (I→III) for 40→100 ticks; Lv4+ adds Mining Fatigue.
- **Levels:** 5 — chance 20/30/40/50/60%, slow I/I/II/II/III, fatigue at Lv4+.
- **Test:** Apply to a weapon and hit a mob repeatedly — it should frequently gain Slowness (chill particles); at Lv4-5 also Mining Fatigue.

### Frostbrand · `frostbrand`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords
- **Description:** Adds frost damage and chills the target, bypassing fire resistance. Conflicts with Soulfire/Frostbite/Hellforged.
- **Effects:** +1→+4 flat frost damage + Slowness; Lv2+ applies freeze ticks (powder-snow shiver on 1.17+).
- **Levels:** 4 — frost dmg 1/2/3/4, slow ticks 20/40/40/60, freeze 0/60/80/100.
- **Test:** Apply to a sword and hit a mob — extra frost damage lands and the target slows; on 1.17+ watch for the freeze/ice overlay at Lv2+.

### Glasscutter · `glasscutter`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Armor penetration — ignores part of the target's armor.
- **Effects:** Ignores 15%→55% of armor mitigation; Lv4 has a 10% chance to shatter a worn armor piece's durability.
- **Levels:** 4 — pierce 0.15/0.25/0.40/0.55, shatter chance at Lv4 (10%).
- **Test:** Put armor on a test player/armor stand target, apply to a sword, and compare damage vs. an unarmored target — armor should matter much less. At Lv4, hit repeatedly to see armor durability shatter.

### Headhunter · `headhunter`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** any Weapon
- **Description:** Chance to drop the target's head on kill.
- **Effects:** On kill, drops the victim's head — players 2%→10%, mobs 5%→20%.
- **Levels:** 3 — player chance 2/5/10%, mob chance 5/10/20%.
- **Test:** Apply to a weapon and kill many mobs (e.g., zombies/skeletons/creepers) — their heads occasionally drop.

### Headsplitter · `headsplitter`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Axes
- **Description:** Higher head-drop chance and bonus damage to bare-headed foes.
- **Effects:** Increased head-drop chance (5%→18%) and +10%→+35% damage to targets with no helmet.
- **Levels:** 3 — head chance 5/10/18%, bare-head bonus +10/20/35%.
- **Test:** Apply to an axe; hit a helmetless mob/player for extra damage, and kill many for head drops. Equip a helmet on the target and the bonus damage goes away.

### Hellforged · `hellforged`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Ignites foes and burns harder against non-Nether mobs. Conflicts with Frostbrand.
- **Effects:** Sets the target on fire 3→6s and deals +10%→+45% to non-Nether mobs.
- **Levels:** 4 — fire 3/4/5/6s, overworld bonus +10/20/30/45%.
- **Test:** Apply to a sword; hit an overworld mob (e.g., cow) — it ignites and takes the bonus. Hit a Nether mob (e.g., blaze) to confirm no overworld bonus applies.

### Hunter's Mark · `hunters_mark`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Bows, Crossbows
- **Description:** Marked targets glow and take more damage from all sources.
- **Effects:** A hit marks the target (glow) for 5→12s; while marked it takes +10%→+30% from any attacker.
- **Levels:** 4 — duration 5/7/10/12s, bonus +10/15/20/30%.
- **Test:** Apply to a bow, shoot a mob — it glows and a "Marked" message shows. Hit it with anything during the window and confirm increased damage.

### Killstreak · `killstreak`
- **Rarity:** Rare · **Max Level:** 5 · **Applies to:** any Weapon
- **Description:** Chaining kills builds a streak that boosts your damage — RAMPAGE at ×5. Conflicts with Combo Strike/Soulreaver.
- **Effects:** Each kill within 8s adds a streak (max 3→10); damage scales by per-step (0.05→0.08) × streak. Banner at ×2+, **RAMPAGE** at ×5+.
- **Levels:** 5 — max streak 3/4/5/7/10, per-step 0.05/0.05/0.06/0.07/0.08.
- **Test:** Apply to a weapon and kill mobs rapidly (within 8s of each) — the KILLSTREAK ×N banner climbs and your damage rises; reach ×5 for the RAMPAGE banner. Pause >8s to reset.

### Lifesteal · `lifesteal`
- **Rarity:** Common · **Max Level:** 5 · **Applies to:** Swords
- **Description:** Heal a percentage of the damage you deal. Conflicts with Bloodthirst/Vampiric Edge.
- **Effects:** Heals 3%→15% of final damage dealt (heart particles + sound).
- **Levels:** 5 — percent 3/5/7/10/15%.
- **Test:** Take damage first, apply to a sword, and attack mobs — your HP recovers proportionally to damage dealt.

### Manaburn · `manaburn`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Swords
- **Description:** Drains the target and banks the energy into your next swing.
- **Effects:** Drains target hunger (players) and stores a next-swing bonus (+5%→+20%); Lv3 also applies brief Weakness.
- **Levels:** 3 — drain 1/2/3, next-swing +5/10/20%, weakness at Lv3.
- **Test:** Apply to a sword, hit a target once (banking the bonus), then hit again — the second swing is stronger. Against a player, watch their hunger drop.

### Mirror Edge · `mirror_edge`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Swords
- **Description:** A landed hit may ready a mirror that reflects the next blow you take.
- **Effects:** 8%→18% chance on hit to "ready" a reflect; the next incoming blow is reflected at 100%→200% back to the attacker.
- **Levels:** 3 — chance 8/12/18%, reflect ×1.0/1.5/2.0.
- **Test:** Apply to a sword, hit a mob until "Mirror Edge » ready" shows, then let a mob/player hit you — the damage bounces back to them.

### Momentum · `momentum`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Swords, Axes, Maces
- **Description:** Damage scales with how long you've been sprinting before the hit.
- **Effects:** +per-second damage (0.05→0.10) while sprinting, up to +20%→+60%.
- **Levels:** 4 — per-second 0.05/0.06/0.08/0.10, cap +20/30/40/60%.
- **Test:** Apply to a sword, sprint in a straight line for several seconds, then strike — damage is higher than a standing hit (and peaks once you hold sprint long enough).

### Phantom Edge · `phantom_edge`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Swords
- **Description:** Chance for hits to phase through armor.
- **Effects:** 5%→15% chance to fully ignore armor on a hit; Lv3 adds +50% damage on the phasing hit.
- **Levels:** 3 — chance 5/10/15%, extra damage at Lv3 (+50%).
- **Test:** Put armor on the target, apply to a sword, and attack repeatedly — occasional hits ("✦ Phase" feedback) ignore armor entirely (and hit extra hard at Lv3).

### Phantom Volley · `phantom_volley`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Bows
- **Description:** Firing summons ghostly arrows at nearby visible enemies. Conflicts with Volley.
- **Effects:** On firing, launches 1→3 spectral arrows at nearby visible enemies within 15 blocks, each at 50%→70% damage.
- **Levels:** 3 — arrows 1/2/3, range 15, damage 50/60/70%.
- **Test:** Apply to a bow, stand near several mobs, and fire one arrow — extra ghostly arrows seek nearby enemies.

### Piercing Shot · `piercing_shot`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Bows, Crossbows
- **Description:** Arrows pass through multiple targets. Conflicts with Ricochet.
- **Effects:** Sets arrow pierce level 1→3 (1.14+ servers).
- **Levels:** 3 — pierce 1/2/3.
- **Test:** Apply to a bow (1.14+), line up several mobs, and fire through the row — one arrow hits multiple in a line.

### Plunderer · `plunderer`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** any Weapon
- **Description:** More mob loot, with a chance at extra drops and enchant scrolls. Conflicts with Curse of Greed.
- **Effects:** +20%→+100% common drops, 5%→25% rare extra-drop chance; Lv3+ adds a scroll-drop chance (5%→10%).
- **Levels:** 4 — common +20/40/60/100%, rare chance 5/10/15/25%.
- **Test:** Apply to a weapon and farm a stack of mobs — noticeably more drops, occasional rare extras, and (Lv3+) the rare enchant scroll.

### Predator · `predator`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Bows, Swords
- **Description:** Paints a tracking line to your last target. Higher levels read their HP and speed.
- **Effects:** Draws a red tracking line to your last-hit target for 5/8/12s; Lv2 shows the target's HP on your action bar, Lv3 adds its speed. (Trails show through walls.)
- **Levels:** 3 — duration 100/160/240 ticks, +HP at Lv2, +speed at Lv3.
- **Test:** Apply to a bow/sword, hit a mob, then move away — a red line points to it (visible through walls), with HP/speed read-outs at Lv2/3.

### Pulverize · `pulverize`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Maces, Axes
- **Description:** Deal area damage around the target on hit.
- **Effects:** Hits splash 20%→50% damage to enemies within 2→3.5 blocks of the target; Lv4 adds knockback.
- **Levels:** 4 — radius 2.0/2.5/3.0/3.5, damage 20/30/40/50%.
- **Test:** Apply to a mace, hit one mob in a cluster — neighbors take splash damage (and get knocked back at Lv4).

### Quickdraw · `quickdraw`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Swords, Axes
- **Description:** Your first hit after drawing this weapon strikes harder.
- **Effects:** The first hit within 2→3s of switching to the weapon deals +25%→+60%; Lv3 also grants brief Speed.
- **Levels:** 3 — window 2/2.5/3s, bonus +25/40/60%.
- **Test:** Apply to a sword, switch to it from another hotbar slot, then immediately hit a mob — that first strike is empowered ("Quickdraw"). Wait past the window to lose it.

### Quickshot · `quickshot`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Bows, Crossbows
- **Description:** Rapid fire — right-click looses a fully charged arrow on a short cooldown.
- **Effects:** Right-click instantly fires a fully-charged arrow; cooldown 30→8 ticks by level (Lv4 adds slight spread). Consumes one arrow (free in Creative / with Infinity). On-hit bow enchants still apply.
- **Levels:** 4 — cooldown 30/20/14/8 ticks, spread at Lv4.
- **Test:** Apply to a bow with arrows in inventory, then **rapidly right-click** — each click fires an instant charged arrow on the cooldown. During cooldown, the normal hold-to-draw shot still works.

### Reaper's Toll · `reapers_toll`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Kills release a soul that heals you when collected.
- **Effects:** 25%→100% chance on kill to drop a soul orb; walk over it to heal 1→4 hearts (Lv4 grants Absorption).
- **Levels:** 4 — chance 25/40/60/100%, heal 1/2/3/4 hearts.
- **Test:** Apply to a sword, kill mobs, and walk into the dropped soul marker ("Arcanum Soul") to heal. At Lv4 every kill drops one and grants Absorption.

### Ricochet · `ricochet`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Bows, Crossbows
- **Description:** Arrows bounce to additional targets on hit, with falloff. Conflicts with Piercing Shot.
- **Effects:** On hit, the arrow bounces to 1→3 nearby targets within 5→8 blocks at 60%→70% damage, decreasing per bounce.
- **Levels:** 3 — bounces 1/2/3, range 5/6/8, damage 60/65/70%.
- **Test:** Apply to a bow, gather mobs in a group, and shoot one — the hit chains to nearby targets (visible bounce trail).

### Riposte · `riposte`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Swords
- **Description:** Counter-attack when struck while holding a sword.
- **Effects:** 20%→40% chance when hit to reflect 25%→60% back at the attacker; Lv3 also applies brief Weakness.
- **Levels:** 3 — chance 20/30/40%, reflect 25/40/60%.
- **Test:** Hold a sword with Riposte and let a mob/player hit you — sometimes the damage is countered back at them (Weakness at Lv3).

### Sniper's Eye · `snipers_eye`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Bows, Crossbows
- **Description:** Arrows deal more damage the farther they travel.
- **Effects:** +per-10-blocks damage (0.05→0.12) up to +25%→+60%; Lv4 adds a long-range crit beyond 30 blocks ("Long Shot").
- **Levels:** 4 — per-ten 0.05/0.07/0.10/0.12, cap +25/35/50/60%.
- **Test:** Apply to a bow; hit a target point-blank vs. across a large distance and compare damage — far shots hit harder. At Lv4 land a 30+ block shot for the Long Shot crit.

### Soul Tether · `soul_tether`
- **Rarity:** Legendary · **Max Level:** 3 · **Applies to:** Swords, Bows
- **Description:** Tethers your target: it takes more damage from all, but siphons some of its damage to you.
- **Effects:** A hit tethers the target for 160/240/300 ticks; while tethered it takes +25%/+40%/+60% from weapon attacks, and 10%/15%/20% of its outgoing damage is siphoned back to you. Lv3 glows the target. Purple tether line.
- **Levels:** 3 — duration 160/240/300 ticks, backlash 10/15/20%, vulnerability +25/40/60%.
- **Test:** Apply to a sword, hit a target (purple line appears), then hit it again to confirm extra damage. Let the tethered target hit something/you and watch a portion siphon back to your HP.

### Soulreaver · `soulreaver`
- **Rarity:** Epic · **Max Level:** 5 · **Applies to:** Swords
- **Description:** Each kill grants a stacking damage buff that decays over time. Conflicts with Vampiric Edge.
- **Effects:** Each kill adds a stack (max 5→10) worth +per-stack damage (0.02→0.06); stacks decay after 10→20s.
- **Levels:** 5 — per-stack 0.02/0.03/0.04/0.05/0.06, max 5/5/7/7/10, decay 10/12/15/18/20s.
- **Test:** Apply to a sword and kill mobs in quick succession — the Soulreaver stack count climbs (action bar) and damage rises; stop killing and stacks decay away.

### Spectral Bind · `spectral_bind`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Swords, Bows
- **Description:** Hits chain the target — slowing them and yanking them back when they try to flee. Conflicts with Voidstrike.
- **Effects:** Applies Slowness (II→IV) for the bind; Lv2+ periodically pulls the target back toward you; Lv3 blocks ender-pearl escapes. Cyan chain line + chain sound.
- **Levels:** 3 — duration 60/80/100 ticks, slow II/III/IV, pull at Lv2+, pearl-block at Lv3.
- **Test:** Apply to a sword, hit a fleeing mob/player — it slows and (Lv2+) gets yanked toward you via a cyan chain. At Lv3, hit a player and have them try an ender pearl — it's blocked.

### Stunlock · `stunlock`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Maces, Axes
- **Description:** Chance to briefly stun the target, locking them in place. Conflicts with Petrify.
- **Effects:** 3%→10% chance to stun for 0.75→1.5s; Lv3 adds +25% damage to stunned targets.
- **Levels:** 3 — chance 3/6/10%, stun 0.75/1.0/1.5s.
- **Test:** Apply to a mace, hit a mob repeatedly until it freezes in place (stun particles). At Lv3, follow up while stunned for bonus damage.

### Tempest Strike · `tempest_strike`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Critical hits release a knockback wave around the target.
- **Effects:** On a crit, a wave (radius 2→4) knocks back nearby enemies; Lv2+ adds adjacent damage, Lv4 adds Slowness.
- **Levels:** 4 — radius 2.0/2.5/3.0/4.0, knockback 0.5/0.7/1.0/1.4.
- **Test:** Apply to a sword, land a critical hit (jump-attack) on a mob in a cluster — nearby foes are blown back (and damaged/slowed at higher levels).

### Tidecaller · `tidecaller`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Tridents
- **Description:** Trident impacts surge water outward, pushing enemies back.
- **Effects:** A thrown trident's impact pushes nearby enemies outward (radius 1→3, push 0.5→1.5) with water particles; Lv2+ adds Slowness. (Places no water — no griefing.)
- **Levels:** 3 — radius 1/2/3, push 0.5/1.0/1.5, slow at Lv2+.
- **Test:** Apply to a trident (1.13+), throw it into a group of mobs — on impact they're pushed back (and slowed at Lv2+).

### Trident Storm · `trident_storm`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Tridents
- **Description:** Thrown tridents call lightning in the rain, or a shockwave when clear.
- **Effects:** On impact during a storm, calls 1→2 lightning bolts (+follow-ups); when clear, a shockwave (radius 2→4, 2→4 damage) + Slowness at Lv3.
- **Levels:** 3 — lightning 1/1/2, shockwave radius 2/3/4.
- **Test:** Apply to a trident; throw it during rain/thunder to call lightning, and on a clear day to produce the shockwave instead.

### Vampiric Edge · `vampiric_edge`
- **Rarity:** Legendary · **Max Level:** 5 · **Applies to:** Swords
- **Description:** Consecutive hits on the same target stack lifesteal.
- **Effects:** Lifesteal that grows with consecutive hits on one target: base 2%→6% + per-stack 2%→6%, up to 5→8 stacks.
- **Levels:** 5 — base 2/3/4/5/6%, per-stack 2/3/4/5/6%, max stacks 5/5/6/7/8.
- **Test:** Take damage, apply to a sword, and keep hitting the **same** mob — your heal-per-hit grows the longer the combo lasts.

### Vengeance · `vengeance`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Swords, Axes
- **Description:** Deal bonus damage to whoever last hit you.
- **Effects:** +15%→+40% damage to your last attacker for 10→20s; Lv2+ marks them with a glow; Lv3 guarantees the first crit against them.
- **Levels:** 3 — bonus +15/25/40%, duration 10/15/20s.
- **Test:** Hold the weapon, let a mob/player hit you, then strike them back — that retaliation deals bonus damage (target glows at Lv2+; first hit always crits at Lv3).

### Voidstrike · `voidstrike`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Swords, Tridents
- **Description:** Chance to blink the target a short distance forward, disorienting them. Conflicts with Spectral Bind.
- **Effects:** 5%→15% chance to teleport the target 2→4 blocks forward; Lv2+ adds Nausea, Lv3 adds Blindness.
- **Levels:** 3 — chance 5/10/15%, distance 2/3/4.
- **Test:** Apply to a sword and hit a mob repeatedly — occasionally it blinks forward (with Nausea/Blindness at higher levels).

### Volley · `volley`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Bows, Crossbows
- **Description:** Fire additional arrows in a spread. Conflicts with Phantom Volley.
- **Effects:** Fires +1→+4 extra arrows in a 15°→30° spread; consumes 1→2 arrows.
- **Levels:** 3 — extra arrows 1/2/4, spread 15/20/30°, arrow cost 1/1/2.
- **Test:** Apply to a bow with plenty of arrows and fire — multiple arrows fan out in a spread.

### Whirlwind · `whirlwind`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Swords, Axes
- **Description:** Sweep attacks deal real damage to everything they touch. Conflicts with Cleave/Pulverize.
- **Effects:** Makes sweep attacks deal 60%→120% of weapon damage; Lv4 adds a visible ring.
- **Levels:** 4 — sweep 60/80/100/120%.
- **Test:** Apply to a sword, stand among several mobs, and perform a sweep attack (walk-attack while not crit) — all swept enemies take meaningful damage.

### Wrath of the Wild · `wrath_of_the_wild`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Axes
- **Description:** Fight harder when outnumbered — damage scales with nearby hostile mobs.
- **Effects:** +per-mob damage (0.05→0.10) for hostiles within 6 blocks, up to +20%→+60%; Lv4 heals 1 heart on a kill while surrounded.
- **Levels:** 4 — per-mob 0.05/0.06/0.08/0.10, cap +20/30/40/60%.
- **Test:** Apply to an axe, surround yourself with several hostile mobs, and attack — damage rises with the crowd (and Lv4 heals on kills while outnumbered).

---

# 2. Cursed

Dark-red category. Powerful upsides paired with real drawbacks. By default these do
**not** count toward the per-item enchant cap.

### Curse of Brittleness · `curse_of_brittleness`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Weapons & Tools
- **Description:** Wears out faster, but strikes much harder.
- **Effects:** Durability drains 2×→4× faster in exchange for a flat effective-damage bonus (+1→+3); Lv3 always crits targets below 25% HP.
- **Levels:** 3 — durability ×2/3/4, effective bonus +1/2/3.
- **Test:** Apply to a sword/tool, use it on mobs/blocks — damage/effectiveness is higher but durability falls noticeably faster. At Lv3, hit a low-HP target for a guaranteed crit.

### Curse of Echoes · `curse_of_echoes`
- **Rarity:** Mythic · **Max Level:** 2 · **Applies to:** any Armor
- **Description:** You bleed when you strike — but all hits crit. Conflicts with Curse of Hunger / Curse of the Sleepless.
- **Effects:** You take 10%→15% of the damage you deal as recoil, in exchange for a high crit rate (50%→100%); Lv2 makes **every** hit crit.
- **Levels:** 2 — reflect 10/15%, crit rate 50/100%.
- **Test:** Equip the cursed armor and attack mobs — your hits crit constantly while you take a little self-damage each strike (Lv2 = all hits crit).

### Curse of Greed · `curse_of_greed`
- **Rarity:** Rare · **Max Level:** 2 · **Applies to:** Weapons
- **Description:** More loot from kills, but you heal less. Conflicts with Plunderer.
- **Effects:** +30%→+60% drops, but all your healing is reduced 25%→50%.
- **Levels:** 2 — drop bonus +30/60%, healing reduction 25/50%.
- **Test:** Apply to a weapon, farm mobs for the extra drops, then try to heal (food/regen) — recovery is clearly slower.

### Curse of Hunger · `curse_of_hunger`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** any Armor
- **Description:** Drains hunger faster, but boosts your damage. Conflicts with Curse of Echoes / Curse of the Sleepless.
- **Effects:** Hunger depletes 2×→4× faster in exchange for +10%→+35% damage; Lv3 adds +5% crit.
- **Levels:** 3 — hunger ×2/3/4, damage +10/20/35%.
- **Test:** Equip the cursed armor and watch your hunger bar drain quickly while your attacks hit harder.

### Curse of Ravenous · `curse_of_ravenous`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Weapons
- **Description:** Slain mobs erupt — harming nearby foes and you.
- **Effects:** Kills detonate (radius 2→4, ~2→4 hearts) hitting nearby enemies **and you**; Lv3 doubles XP from the kill.
- **Levels:** 3 — radius 2/3/4, damage ~2/3/4 hearts.
- **Test:** Apply to a weapon, kill a mob inside a group while standing close — the explosion damages neighbors and chips your own HP (extra XP at Lv3).

### Curse of the Bound · `curse_of_the_bound`
- **Rarity:** Rare · **Max Level:** 1 · **Applies to:** Boots
- **Description:** Cannot be removed and you cannot sprint — but you resist knockback and gain toughness.
- **Effects:** Boots can't be removed and sprinting is suppressed, in exchange for 50% knockback reduction and +4 armor toughness.
- **Levels:** 1 only.
- **Test:** Equip the boots — you can't take them off and sprinting gives no speed, but knockback barely moves you and you take less damage. Confirm everything restores cleanly on plugin disable.

### Curse of the Glutton · `curse_of_the_glutton`
- **Rarity:** Epic · **Max Level:** 2 · **Applies to:** Food
- **Description:** Eating grants strong buffs and a strong debuff.
- **Effects:** Eating the enchanted food grants Strength (II→III) for 60→90s plus a Hunger debuff (II→III) for 30→45s.
- **Levels:** 2 — Strength II/III, Hunger II/III.
- **Test:** Apply to an edible item, then eat it — you gain a big Strength boost alongside a strong Hunger drain effect.

### Curse of the Sleepless · `curse_of_the_sleepless`
- **Rarity:** Rare · **Max Level:** 2 · **Applies to:** Helmet
- **Description:** You cannot sleep, but gain Speed and Night Vision. Conflicts with Curse of Hunger / Curse of Echoes.
- **Effects:** Blocks sleeping; grants Speed (I→II) and Night Vision while worn; Lv2 also reduces phantom spawns.
- **Levels:** 2 — Speed I/II, Night Vision both, phantom reduction at Lv2.
- **Test:** Wear the helmet at night — you have Speed + Night Vision but cannot use a bed. At Lv2, confirm fewer phantoms despite not sleeping.

---

# 3. Defense

Blue category. Damage mitigation, survival saves, and reactive armor effects (worn).

### Aegis · `aegis`
- **Rarity:** Uncommon · **Max Level:** 5 · **Applies to:** any Armor
- **Description:** Flat reduction to all incoming damage. Conflicts with Thornmail.
- **Effects:** Reduces all incoming damage by 3%→15%.
- **Levels:** 5 — reduction 3/5/8/11/15%.
- **Test:** Equip an Aegis piece, take a measured hit (e.g., set fall, or mob hit), and compare HP loss with and without the armor — damage should be lower.

### Hex Ward · `hex_ward`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** any Armor
- **Description:** Shorten the duration of harmful potion effects.
- **Effects:** Cuts incoming harmful-effect duration by 15%→75%.
- **Levels:** 4 — reduction 15/30/50/75%.
- **Test:** Wear a Hex Ward piece and get hit by a Poison/Slowness potion (or `/effect`) — the debuff wears off noticeably sooner than normal.

### Last Stand · `last_stand`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Chestplate
- **Description:** Once per life, survive a lethal blow with a burst of resistance and regeneration.
- **Effects:** A would-be-fatal hit leaves you alive with Resistance + Regeneration for 5→10s; Lv3 adds Absorption. Resets on death.
- **Levels:** 3 — Resistance II/II/III, Regen I/II/II, duration 5/7/10s.
- **Test:** Wear the chestplate and take lethal damage (e.g., a strong fall) — you survive at low HP with Resistance/Regen. Die and respawn to re-arm it.

### Phase Cloak · `phase_cloak`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Chestplate
- **Description:** Brief invisibility when you take damage.
- **Effects:** 5%→15% chance when hit to go invisible for 2→4s; Lv3 also grants Speed.
- **Levels:** 3 — chance 5/10/15%, invis 2/3/4s.
- **Test:** Wear the chestplate and let mobs hit you repeatedly — you occasionally flicker invisible (with Speed at Lv3).

### Second Wind · `second_wind`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Boots, Leggings
- **Description:** A healing sprint burst when your health drops low.
- **Effects:** Dropping below 30%→40% HP triggers Speed + a 1→3 heart heal (cooldown 60→40s).
- **Levels:** 3 — threshold 30/35/40%, heal 1/2/3 hearts, cooldown 60/50/40s.
- **Test:** Equip the boots/leggings, take damage to drop below the threshold — you get a Speed burst and heal. Re-trigger to confirm the cooldown.

### Shieldbreaker Resist · `shieldbreaker_resist`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Shield
- **Description:** Chance to ignore axe shield-disable.
- **Effects:** 30%→100% chance to ignore an axe's shield-disable effect.
- **Levels:** 3 — ignore chance 30/60/100%.
- **Test:** Hold the shield and have someone axe-hit it (which normally disables the shield) — at higher levels it stays usable.

### Soulbound · `soulbound`
- **Rarity:** Legendary · **Max Level:** 1 · **Applies to:** any Armor
- **Description:** Item stays with you on death (single use). Consumed when it saves you.
- **Effects:** On death, the Soulbound item is kept instead of dropped; the enchant is consumed in the process.
- **Levels:** 1 only.
- **Test:** Apply to an armor piece, equip/carry it, then die — that item stays in your inventory on respawn (and the enchant is spent).

### Thornmail · `thornmail`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Chestplate, Leggings
- **Description:** Reflect a share of damage taken back at attackers. Conflicts with Aegis.
- **Effects:** Reflects 10%→35% of melee damage taken back to the attacker; Lv4 also reflects 5% of ranged damage.
- **Levels:** 4 — reflect 10/18/25/35%, ranged reflect at Lv4.
- **Test:** Wear the armor and let a mob/player melee you — they take a share of the damage back (Lv4 reflects arrows too).

---

# 4. Farming

Green category. Crop, animal, and fishing productivity (held tool or seed).

### Angler's Luck · `anglers_luck`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Fishing Rod
- **Description:** Better fishing loot, less junk.
- **Effects:** +15%→+75% treasure chance, 0%→50% less junk; Lv4 has a 5% scroll chance.
- **Levels:** 4 — treasure +15/30/50/75%, junk −0/10/25/50%.
- **Test:** Apply to a fishing rod and fish for a while — more treasure, less junk, and (Lv4) an occasional enchant scroll.

### Beastmaster · `beastmaster`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Shears, Hoes
- **Description:** Improved yields from animals.
- **Effects:** +wool/leather/porkchop yields when shearing/breeding/harvesting animals.
- **Levels:** 3 — wool +1/2/3, leather +1/2/2, porkchop +0/1/1.
- **Test:** Apply to shears, shear sheep — extra wool drops. Apply to a hoe and interact with farm animals to see the bonus yields.

### Bountiful Yield · `bountiful_yield`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Hoes, Crop Seeds
- **Description:** Crops drop extra produce.
- **Effects:** Chance (25%→75%) to drop +1→+2 extra produce per crop; Lv4 has a 5% golden-produce chance.
- **Levels:** 4 — extra +1/1/2/2, chance 25/50/50/75%.
- **Test:** Apply to a hoe (or the seed), harvest mature crops — you frequently get extra produce (golden variant at Lv4).

### Composter · `composter`
- **Rarity:** Uncommon · **Max Level:** 2 · **Applies to:** Hoes
- **Description:** Freshly tilled soil comes pre-fertilized.
- **Effects:** Tilling soil pre-applies bonemeal to crops planted there; Lv2 has a 25% double-tier (extra growth) chance.
- **Levels:** 2 — bonemeal both, double-tier at Lv2.
- **Test:** Apply to a hoe, till dirt/grass, and plant a seed — the new crop starts pre-fertilized (occasionally jumping growth stages at Lv2).

### Greenthumb · `greenthumb`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Hoes, Crop Seeds
- **Description:** Speed up crop growth around you while held.
- **Effects:** While held, accelerates crop growth ×1.5→×3 within 4→8 blocks; Lv3 has a 5% instant-grow chance.
- **Levels:** 3 — multiplier 1.5/2/3, radius 4/6/8.
- **Test:** Plant a field of crops, hold the Greenthumb hoe/seed nearby, and watch them mature faster than normal.

### Harvest Wave · `harvest_wave`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Hoes
- **Description:** Harvest (and replant) crops in an area.
- **Effects:** Breaking one mature crop harvests a 3×3→7×7 area; Lv2+ replants, Lv3 auto-collects drops.
- **Levels:** 3 — size 3/5/7, replant at Lv2+, auto-collect at Lv3.
- **Test:** Apply to a hoe, break one mature crop in a field — the surrounding crops harvest at once (and replant/auto-collect at Lv2/3).

---

# 5. Mystic

Purple category. Arcane/magical effects — summons, revives, blinks, and crowd control.

### Petrify · `petrify`
- **Rarity:** Mythic · **Max Level:** 3 · **Applies to:** any Weapon
- **Description:** Chance to fully stun a mob for a short window. Conflicts with Stunlock.
- **Effects:** 3%→8% chance to fully stun a mob for 1.5→3s; Lv3 adds +30% damage to petrified targets.
- **Levels:** 3 — chance 3/5/8%, stun 1.5/2.0/3.0s.
- **Test:** Apply to a weapon and attack a mob repeatedly until it freezes in place (its AI halts), then strike for bonus damage at Lv3.

### Phoenix Feather · `phoenix_feather`
- **Rarity:** Legendary · **Max Level:** 1 · **Applies to:** Chestplate
- **Description:** Once per in-game day, revive on death at 50% HP.
- **Effects:** On a fatal blow, revive in place at 50% HP; cooldown ~24h (one in-game day).
- **Levels:** 1 only.
- **Test:** Wear the chestplate and take lethal damage — you revive at half health instead of dying. Try again before the cooldown elapses to confirm it's on cooldown.

### Soul Harvest · `soul_harvest`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** any Weapon
- **Description:** Kills grant bonus experience.
- **Effects:** +25%→+100% XP from kills; Lv4 has a 5% chance for a shard bonus.
- **Levels:** 4 — XP +25/50/75/100%, shard at Lv4.
- **Test:** Apply to a weapon, note your XP, kill several mobs, and confirm you gain more XP than normal per kill.

### Soulfire · `soulfire`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** Swords, Bows, Tridents
- **Description:** Hits ignite with blue soul-fire. Conflicts with Frostbite.
- **Effects:** Sets targets ablaze 3→6s, partly ignoring fire resistance (25%→80%); Lv4 adds an explosion.
- **Levels:** 4 — duration 3/4/5/6s, ignore 25/40/60/80%.
- **Test:** Apply to a sword and hit a mob — it burns with soul-fire. Hit a fire-resistant mob (e.g., blaze) to confirm partial bypass; Lv4 adds an explosive burst.

### Summoner's Pact · `summoners_pact`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** any Weapon
- **Description:** Chance to summon a brief allied iron golem on kill.
- **Effects:** 2%→6% chance on kill to spawn a temporary allied iron golem for 15→30s; Lv3 the golem inherits 25% of your power.
- **Levels:** 3 — chance 2/4/6%, duration 15/20/30s.
- **Test:** Apply to a weapon and farm many kills — occasionally an allied iron golem appears and fights for you, then despawns.

### Time Slip · `time_slip`
- **Rarity:** Legendary · **Max Level:** 2 · **Applies to:** Boots
- **Description:** Sneak to slow nearby enemies in a bubble.
- **Effects:** Sneaking emits a slow-field (Slowness IV→V) within 5→7 blocks for 2→3s; cooldown 90→60s.
- **Levels:** 2 — radius 5/7, slow IV/V, cooldown 90/60s.
- **Test:** Equip the boots, stand near mobs/players, and **sneak** — nearby foes are heavily slowed for a moment. Re-trigger to confirm the cooldown.

### Voidwalker · `voidwalker`
- **Rarity:** Legendary · **Max Level:** 2 · **Applies to:** Boots
- **Description:** Sneak + jump to blink a short distance forward. Conflicts with Beacon's Memory.
- **Effects:** Sneak + jump teleports you 5→8 blocks forward (cooldown 30→20s); Lv2 grants brief Resistance after the blink.
- **Levels:** 2 — distance 5/8, cooldown 30/20s.
- **Test:** Equip the boots, then **sneak and jump** — you blink forward. Re-trigger immediately to confirm the cooldown (Resistance after the blink at Lv2).

### Wind Step · `wind_step`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Boots
- **Description:** Grants a mid-air double jump.
- **Effects:** 1→2 extra mid-air jumps; cooldown 30→10s.
- **Levels:** 3 — extra jumps 1/1/2, cooldown 30/15/10s.
- **Test:** Equip the boots, jump, then press jump again in mid-air — you gain extra height. At Lv3, chain two air jumps.

---

# 6. Tools

Yellow category. Mining, woodcutting, and gathering utility (held tool).

### Auto-Repair · `auto_repair`
- **Rarity:** Epic · **Max Level:** 4 · **Applies to:** Tools, Weapons, Armor
- **Description:** Slowly mend durability while carried.
- **Effects:** Restores +1 durability every 30→5s while the item is carried/worn.
- **Levels:** 4 — interval 30/20/10/5s.
- **Test:** Apply to a damaged tool/weapon/armor, carry it, and watch its durability tick back up over time (faster at higher levels).

### Excavator · `excavator`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Shovels
- **Description:** Break a wide area of soft blocks at once. Conflicts with Vein Miner/Treecapitator.
- **Effects:** Breaks a 3×3 plane (Lv1-2) or 3×3×3 cube (Lv3) of soft blocks; Lv2+ faster.
- **Levels:** 3 — plane/plane/cube, speed ×1.0/1.5/1.5.
- **Test:** Apply to a shovel and dig dirt/sand/gravel — a wide area breaks per dig (a cube at Lv3).

### Fortune Strike · `fortune_strike`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Pickaxes
- **Description:** Chance to double ore drops (stacks with Fortune).
- **Effects:** 10%→28% chance to double an ore's drops, on top of vanilla Fortune.
- **Levels:** 3 — chance 10/18/28%.
- **Test:** Apply to a pickaxe and mine a vein of ore — drops occasionally double (stacking with a Fortune enchant if present).

### Geologist · `geologist`
- **Rarity:** Rare · **Max Level:** 2 · **Applies to:** Pickaxes
- **Description:** Reveal nearby ores with outline particles when held.
- **Effects:** While held, highlights ores within 6→10 blocks with particles; Lv2 includes rare ores.
- **Levels:** 2 — radius 6/10, rare ores at Lv2.
- **Test:** Hold the Geologist pickaxe underground near ores — outline particles mark nearby ore blocks (rare ores included at Lv2).

### Smelter · `smelter`
- **Rarity:** Uncommon · **Max Level:** 1 · **Applies to:** Pickaxes
- **Description:** Auto-smelt ore drops as you mine.
- **Effects:** Mined ores drop their smelted form (with a small XP bonus).
- **Levels:** 1 only.
- **Test:** Apply to a pickaxe and mine iron/gold ore — you receive ingots directly instead of raw ore.

### Treecapitator · `treecapitator`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Axes
- **Description:** Fell an entire tree from a single log break. Conflicts with Vein Miner/Excavator.
- **Effects:** Breaking one log fells up to 24→96 connected logs; Lv2+ decays leaves, Lv3 replants a sapling.
- **Levels:** 3 — max logs 24/48/96.
- **Test:** Apply to an axe and break the bottom log of a tree — the whole trunk drops (leaves decay at Lv2+, sapling replants at Lv3).

### Vein Miner · `vein_miner`
- **Rarity:** Rare · **Max Level:** 4 · **Applies to:** Pickaxes, Axes
- **Description:** Break connected blocks of the same type at once. Conflicts with Excavator/Treecapitator.
- **Effects:** Breaks up to 8→64 connected same-type blocks in one go; Lv4 auto-collects drops.
- **Levels:** 4 — max blocks 8/16/32/64.
- **Test:** Apply to a pickaxe and break one block of an ore vein — the whole connected vein breaks (drops go to inventory at Lv4).

### XP Magnet · `xp_magnet`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** any Tool
- **Description:** Pull nearby experience orbs toward you.
- **Effects:** Attracts XP orbs within 4→12 blocks; Lv3 adds +10% XP.
- **Levels:** 3 — radius 4/7/12, +XP at Lv3.
- **Test:** Hold an XP Magnet tool, break ores or kill mobs that drop XP from a distance — the orbs stream toward you.

---

# 7. Utility

Aqua category. Mobility, vision, storage, and quality-of-life (worn/held).

### Aquatic · `aquatic`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Helmet
- **Description:** Underwater vision, breath, and comfort.
- **Effects:** Clear underwater vision + water breathing; Lv3 adds night vision and unlimited breath.
- **Levels:** 3 — visibility/breath all levels; night vision + unlimited breath at Lv3.
- **Test:** Wear the helmet and submerge — you see clearly and don't drown quickly (permanent breath + night vision at Lv3).

### Beacon's Memory · `beacons_memory`
- **Rarity:** Epic · **Max Level:** 2 · **Applies to:** Boots
- **Description:** Bind a recall point; teleport back with `/recall`. Conflicts with Voidwalker.
- **Effects:** Stores 1→2 recall points; `/recall` teleports back (cooldown 600→300s).
- **Levels:** 2 — slots 1/2, cooldown 600/300s.
- **Test:** Equip the boots, set a recall point, travel away, then run `/recall` to teleport back. Re-run to confirm the cooldown.

### Bottomless Quiver · `bottomless_quiver`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Bows, Crossbows
- **Description:** Chance not to consume an arrow when firing.
- **Effects:** 20%→60% chance a shot doesn't consume its arrow.
- **Levels:** 3 — chance 20/40/60%.
- **Test:** Apply to a bow with a known arrow count and fire repeatedly — your arrow stock depletes slower than shots fired.

### Featherfall · `featherfall`
- **Rarity:** Uncommon · **Max Level:** 3 · **Applies to:** Boots
- **Description:** Reduce fall damage beyond vanilla.
- **Effects:** Reduces fall damage 30%→95%; Lv3 also silences the fall (no fall sound).
- **Levels:** 3 — reduction 30/60/95%.
- **Test:** Equip the boots and fall from a measured height — you take far less fall damage than barefoot (and land silently at Lv3).

### Glide Boost · `glide_boost`
- **Rarity:** Rare · **Max Level:** 3 · **Applies to:** Elytra
- **Description:** Improved elytra speed and control.
- **Effects:** +10%→+30% glide speed and better pitch control; Lv2+ reduces firework consumption.
- **Levels:** 3 — speed +10/20/30%, pitch +0.05/0.10/0.20.
- **Test:** Apply to an elytra (1.9+), fly, and compare glide speed/handling vs. a plain elytra; check fewer fireworks burned at Lv2+.

### Hermes · `hermes`
- **Rarity:** Uncommon · **Max Level:** 4 · **Applies to:** Boots
- **Description:** Increased movement speed while worn.
- **Effects:** +5%→+20% movement speed; Lv4 reduces hunger drain from moving.
- **Levels:** 4 — speed +5/10/15/20%.
- **Test:** Equip the boots and run — you move faster than normal (and exhaust slower at Lv4).

### Nightvision · `nightvision`
- **Rarity:** Rare · **Max Level:** 1 · **Applies to:** Helmet
- **Description:** Permanent night vision while worn.
- **Effects:** Constant Night Vision as long as the helmet is worn.
- **Levels:** 1 only.
- **Test:** Wear the helmet in a dark area/at night — you see clearly; remove it and darkness returns.

### Pack Mule · `pack_mule`
- **Rarity:** Epic · **Max Level:** 3 · **Applies to:** Chestplate, Leggings
- **Description:** Grants a bonus pickup radius and an item-magnet while worn.
- **Effects:** +2→+6 block pickup radius; Lv2+ actively pulls dropped items toward you.
- **Levels:** 3 — pickup radius 2/4/6, magnet at Lv2+.
- **Test:** Wear the armor and drop/scatter items nearby — they're picked up from farther away (and stream toward you at Lv2+).

### Satchel · `satchel`
- **Rarity:** Epic · **Max Level:** 1 · **Applies to:** Helmet, Chestplate
- **Description:** A 9-slot personal storage, opened with `/satchel`.
- **Effects:** Grants a 9-slot personal storage inventory while the enchanted piece is worn/owned.
- **Levels:** 1 only.
- **Test:** Apply to a helmet/chestplate, then run `/satchel` to open the 9-slot storage; stash items, close, and reopen to confirm they persist.

---

*Generated from the live enchant roster in `src/main/resources/enchants/`. Effect
values reflect the per-level parameters in those files; in-game feedback (action bars,
particles, banners, sounds) is gated by `enchants/config.yml → combat_global` and each
player's `/enchants settings`.*
