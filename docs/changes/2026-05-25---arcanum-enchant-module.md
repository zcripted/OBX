# Arcanum Custom Enchantment Module

■ **Created:** 2026-05-25 7:25 pm
■ **Last Updated:** 2026-05-25 11:25 pm

A custom enchantment system ("Arcanum") added under `dev.zcripted.obx.enchant`.
Built in the design doc's 6 phases; this file tracks status and is updated as the
remaining phases land.

## Key architecture decisions (and why)
- **Storage = structured item lore, not PDC.** The plugin compiles against the
  1.12.2 API (PersistentDataContainer is 1.14+) but runs on 1.8.8 → 1.21.x. Lore
  works everywhere and doubles as the visible tooltip. `EnchantStorage` writes one
  lore line per enchant (`<name> <ROMAN>`) and parses it back via a registry
  reverse-lookup (`EnchantRegistry.byDisplayName`). Scrolls/books are identified by
  an italic dark-gray "Arcanum …" marker footer (`EnchantItems` / `ScrollKind`).
- **No post-1.12.2 enum references.** Materials, sounds, and potion types are
  resolved by name (`Material.matchMaterial`, `Sounds`, `Potions`), and
  `setCustomModelData` is reflective — so the single jar stays correct cross-version.
- **Messages use OBX's existing `MessageDefaults`/`LanguageManager` (EN+DE)**,
  not a separate `messages.yml` — consistent with the rest of the plugin and
  CLAUDE.md's i18n rule.
- **Open questions (§14) resolved to the doc's recommendations**: stack with vanilla;
  configurable cap (default 6) with cursed not counting; per-world PvP toggles;
  both anvil + drag-drop apply (drag-drop costs +50% XP); tradeable per tier.

## Status by phase

### ✅ Phase 1 — Core engine (complete)
- Model: `EnchantCategory` (7), `EnchantRarity` (6), `ItemTag` (base + composite,
  name-based item classification), `CustomEnchant` (data-driven level params).
- `EnchantRegistry` loads the full roster from `enchants/*.yml`.
- `EnchantStorage` — lore read/apply/remove/count (cross-version, no PDC).
- `EnchantService` — config, conflict groups, cap rules, validated `apply()`
  returning rich `ApplyResult`/`ApplyStatus`.
- Config data layer (bundled, written on first run): `config.yml`, `combat.yml`,
  `defense.yml`, `tools.yml`, `farming.yml`, `utility.yml`, `mystic.yml`,
  `cursed.yml`, `scrolls.yml`, `loot.yml` — **all 50 enchantments from §9** defined
  with per-level parameters.

### ✅ Phase 2 — Admin GUI (complete)
- `EnchantAdminMenu` (main console → category list → level selector) +
  `EnchantMenuHolder` + `EnchantMenuListener`.
- Full §7.4 error feedback via `EnchantFeedback`: **action bar + chat + sound** for
  applied / upgraded / wrong-type / empty-hand / invalid-level / already-applied /
  conflict / slot-cap, plus inventory-full on give.
- Level-selector click matrix: left = apply to held item, shift-left = scroll,
  right = enchanted book, shift-right = 64 scrolls.
- Held-item info icon, scroll shortcuts (protection/success/extraction), close button.
- Read-only browse mode powering `/enchants`.

### ✅ Commands (complete for non-anvil flow)
- `/obxench` (`/obxenchant`, `/obxe`): `admin`, `give`, `givebook`, `apply`, `remove`,
  `list`, `info`, `reload`, `loot toggle|reload`, `protect`, `success`, `debug` —
  all functional, with tab-completion and per-command permissions (§12).
- `/enchants` (`/scrolls`): read-only browser.
- `EnchantItems` builds scrolls (paper/book by tier), enchanted books, and the four
  utility scrolls, with glow + reflective custom-model-data.

### ✅ Phase 3 — Scroll application mechanics (complete)
- `ScrollApplyService` + `ScrollResult` + `ScrollSettings` (loads `scrolls.yml`).
- **Anvil path** (`AnvilEnchantListener`): gear + enchant scroll/book → preview on
  `PrepareAnvilEvent`, applied on taking the result (roll, XP cost, protection,
  destroy-on-failure).
- **Drag-and-drop path** (`ScrollDragListener`): pick a scroll onto a target.
  Enchant scroll/book → matching gear (convenience, +50% XP). **Protection /
  Success** scrolls imbue an enchant scroll (drag onto it → "Protected"/"Boosted"
  markers) — a clean reading of "place alongside". **Extraction** pulls one enchant
  back into a scroll (reduced level); **Transmutation** re-rolls one enchant
  in-category.
- Success rate from `scrolls.yml` per rarity; XP charged via levels (creative
  bypass); outcomes feed `EnchantFeedback` (action bar + chat + sound).

### ✅ Phase 4 — World loot (complete)
- `EnchantLoot` loads `loot.yml`, reflectively registers `LootGenerateEvent`
  (1.13+; no-ops on 1.8–1.12), maps vanilla loot-table keys → config chest types
  (incl. renames: `simple_dungeon`→dungeon, `nether_bridge`→nether_fortress,
  `bastion*`→bastion_remnant, trial-chamber/ominous, etc.), honors per-chest
  enable/weight/cap, rarity distribution, and category filter. Master switch ships
  **off**; `/obxench loot toggle|reload` works live.

### ✅ Phase 5 — Roster effects (complete for event-tractable enchants)
Live effect listeners now cover every category:
- **Combat** (`CombatEnchantListener`): Frostbite, Bloodthirst, Executioner, Chain
  Lightning, Pulverize, Vampiric Edge, Riposte, Headhunter, Soulfire, Soul Harvest,
  Summoner's Pact, Petrify-adjacent cursed procs (Brittleness, Hunger dmg, Echoes,
  Greed drops, Ravenous).
- **Defense** (`DefenseEnchantListener`): Aegis, Thornmail, Last Stand, Phoenix
  Feather, Second Wind, Phase Cloak, Soulbound.
- **Tools** (`ToolEnchantListener`): Vein Miner, Treecapitator, Excavator, Smelter,
  Fortune Strike.
- **Farming** (`FarmingEnchantListener`): Bountiful Yield, Harvest Wave, Angler's
  Luck, Beastmaster (shear).
- **Utility** (`UtilityEnchantListener` + tick): Featherfall, Bottomless Quiver,
  Hermes, Nightvision, Aquatic, Pack Mule (item magnet), Glide Boost, Satchel
  (`/satchel`), Beacon's Memory (bind + `/recall`).
- **Mystic** (`MovementEnchantListener` + combat + tick): Wind Step (double jump),
  Voidwalker (blink), Time Slip (sneak slow), Phoenix Feather, Soul Harvest,
  Summoner's Pact, Soulfire.
- **Cursed** (`CursedEnchantListener` + combat + tick): Glutton (food), Sleepless
  (no sleep + buffs), Bound (no sprint + no removal), Greed (reduced healing),
  Hunger (faster drain + combat dmg), Ravenous, Echoes, Brittleness.
- Passive armor/held effects run in `EnchantTickTask` (1 Hz); `EnchantState` holds
  cooldowns, recall points, satchels, and once-per-life flags.

### ✅ Phase 6 — Polish (complete)
- **Particles** (`enchant/util/Particles.java`, version-safe name resolution +
  guarded `spawnParticle`, no-ops on 1.8): Frostbite chill, Chain Lightning sparks,
  Soulfire flames, Pulverize/Petrify smoke, Wind Step cloud, Time Slip enchant
  swirl, Voidwalker portal burst (both ends), Last Stand / Phoenix totem burst,
  Composter / Geologist happy-villager, and a magic burst on a successful apply.
- **Custom model data** now set on both scrolls and enchanted books (reflective).
- **Tooltip polish** (per CLAUDE.md): scrolls, books, utility scrolls, and the
  GUI enchant/level icons rebuilt with `─` dividers and `Label » value` rows.
- **Previously deferred effects now LIVE**:
  - **Petrify** — freezes a mob's AI (`setAI(false)` + re-enable) with a stun
    window and damage amp.
  - **Hex Ward** — shortens active harmful potion effects each tick by the
    configured fraction (works on all versions; no `EntityPotionEffectEvent` needed).
  - **Shieldbreaker Resist** — chance to clear the shield disable cooldown when an
    axe would disable a blocking shield (`setCooldown(SHIELD, 0)`).
  - **Geologist** — samples nearby blocks and outlines ores with particles.
  - **Composter** — right-click a growing crop with the hoe to advance growth
    (instant at Lv2's double-tier chance).
  - **Curse of the Bound** — knockback reduction (post-hit velocity scaling) and
    armor toughness (modeled as a small flat damage reduction); no-sprint /
    no-removal were already active.

**Remaining design notes (intentional, not bugs):**
- **Pack Mule** is a worn item-magnet rather than raised vanilla stack sizes
  (stack size isn't changeable pre-1.20.5 without packets).
- Recall points & satchel contents are per-session (not persisted across restart).

### New player commands
- `/recall` — teleport to the Beacon's Memory point. `/satchel` — open Satchel
  storage. Both `obx.enchants.use` (default true). Registered in `Main` /
  `plugin.yml`.

## Wiring
- `Main`: constructs `EnchantService`/`EnchantItems`/`EnchantFeedback`/
  `EnchantAdminMenu`; binds `obxench`/`enchants`; registers `CombatEnchantListener`
  + `EnchantMenuListener`; reloads the service in `reloadPlugin()`.
- `plugin.yml`: `obxench` + `enchants` commands; `obx.enchants.*` permission tree.

## Verification
- `mvn -DskipTests clean package` → exit 0, no errors / unmappable warnings, at
  each phase checkpoint (3, 4, 5).
- Both jars rebuilt; all 10 `enchants/*.yml` bundled; **57** enchant classes
  compiled (incl. scroll/loot/effect/listener/command/util sets).
- Phase 6: `Particles` present; `─` dividers in item/GUI lore verified in the
  compiled classes; 0 mojibake.
- New message glyphs (✔ ✖ ↑, math-bold `𝗔𝗥𝗖𝗔𝗡𝗨𝗠`) verified in the compiled
  `MessageDefaults.class`; **0 mojibake**; German umlauts intact.

## Post-Phase-6 fixes (Curse of the Bound + glow-on-apply)
- **No-sprint now enforced via speed neutralization (reworked for 1.21).** On
  modern clients sprint is client-authoritative: cancelling
  `PlayerToggleSprintEvent` / `setSprinting(false)` are cosmetic, and in creative
  the hunger gate is bypassed by fly ability — which is why the earlier attempt
  did nothing. The fix (`effect/BoundMovement`) instead removes the sprint
  *benefit*: while a Bound player is sprinting, their abilities walk/fly speed is
  scaled down (×0.77 walk / ×0.5 fly, countering the ~×1.3 sprint and ×2 creative
  fly-sprint), and restored the instant they stop. Driven by the sprint-toggle
  event with the tick task as a safety net; original speeds are captured per
  player and restored exactly (and on quit/disable) so nothing is clobbered or
  left behind. Works in **survival and creative**. Honest limitation: the sprint
  *animation/FOV* may still show client-side (removing that needs ProtocolLib),
  but the player gains no speed — sprinting moves at ~walk pace.
- **Armor toughness is now the real attribute.** Replaced the flat-reduction
  stand-in with the actual `GENERIC_ARMOR_TOUGHNESS` modifier
  (`EffectUtil.setBoundToughness`, fixed UUID, idempotent remove-then-add),
  maintained each tick by `EnchantTickTask` and cleared on quit / disable so it
  never persists. Resolved by name (`GENERIC_ARMOR_TOUGHNESS` → `ARMOR_TOUGHNESS`)
  for cross-version safety; no-ops on 1.8.
- **Knockback reduction refined** to scale only horizontal velocity (preserves
  jumps/knock-up).
- **Glow on apply (all enchants/scrolls).** New `util/Glow`: on 1.20.5+ uses the
  clean `setEnchantmentGlintOverride` (no gameplay effect, removable); older
  servers fall back to a hidden Unbreaking + `HIDE_ENCHANTS`. `EnchantStorage.apply`
  calls `Glow.ensure(meta)` so every item enchanted by any path (command, GUI,
  scroll, anvil, transmute) glows if it wasn't already; `remove` clears the glint
  override when the last custom enchant is removed. Scrolls/books route their
  glow through the same util.
- Build: `mvn clean package` exit 0; 58 enchant classes; `Glow` present.

## Chat hover tooltips for enchantment names
- New `util/EnchantHover`: builds a tooltip (name, category, rarity, level [applied
  level when known, else max], item it was applied to [when known], valid item
  types, description) and sends a chat line with the **enchant name** made
  hoverable (splits the resolved line on the colored name; whole-line hover
  fallback; `ComponentMessenger` downgrades to plain text for console).
- Wired in: apply/upgrade/rejection feedback (`EnchantFeedback` — shows applied
  level + the item applied to on success), `/obxench list` entries, `/obxench info`
  header, and the `/obxench remove` confirmation. Works for all custom enchants.
- Build: exit 0; 60 enchant classes; `EnchantHover` present.

## Tab completion + cleaner usage
- `/obxench apply <enchant> <level>` now tab-completes the **level** (1..maxLevel of
  the named enchant); same for `give`/`givebook` at the `<level>` arg
  (`ObxEnchantCommand.levelsFor`, wired at arg 3 for apply and arg 4 for give).
- `enchant.usage` rewritten from a dense one-liner into a clean, grouped boxed help
  (Browse / Apply / Give / Manage sections, each command with a short description),
  EN + DE, matching the `▍ 𝗔𝗥𝗖𝗔𝗡𝗨𝗠 ›` styled-report format.

## Suggested Commit Message
```
Feature (enchants): Add Arcanum module — core engine, admin GUI, commands, scroll items, and combat effects
```
