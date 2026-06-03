# OBX — Bug Tracker

A running log of bug reports, investigations, and patches, organized by the plugin
**feature/system** each one touches. Entries are ordered **most recent at the top,
descending to oldest at the bottom**.

> **Maintaining this file:** add new entries under a date heading at the very **top**.
> Each entry must **begin with a `🕒 YYYY-MM-DD · h:mm AM/PM ET` timestamp line** (local
> **America/Detroit (ET)** time, matching the project's change-tracking convention), then
> name its **System**, a **Status**, the **symptom**, the **cause**, the **fix** (or
> resolution), and a link to the detailed change doc in `docs/changes/` when one exists.

**Status legend:** ✅ Fixed · 🔵 By design / No action · 🟡 Open · 🔎 Investigating

**Systems covered so far:** Combat Enchantments · Enchantments (Lore/Glow) · Core / Logging · Localization · MOTD / Server List

---

## 2026-05-27

### ✅ [Localization / Language] "Added 1 missing keys" logged on every reload
**🕒 2026-05-27 · 4:02 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Low (repeating console noise; one validate message never persisted)
- **Symptom:** Every plugin reload logged `[OBX] Added 1 missing keys to language_en.yml`
  and `… sprache_de.yml`, instead of only once when new messages are introduced.
- **Cause:** `commands.obx.config.validation` was defined as both a message **and** the
  parent of `commands.obx.config.validation.data-missing`. YAML can't store a value and
  child keys at the same path, so the report value was dropped on disk; `LanguageFile.readValue`
  then returns null for a configuration section, so the language self-heal counted it as
  "missing" and re-added it on **every** reload (1 per language file).
- **Fix:** Moved the child to `commands.obx.config.data-missing` (a sibling, not a child),
  so `commands.obx.config.validation` is a pure leaf and persists. Verified no remaining
  leaf/parent key collisions exist in `MessageDefaults`.
- **Note:** The `[OBX][Arcanum] Loaded 100 custom enchantments…` line is normal
  informational load output, not an error.
- **Ref:** `docs/changes/2026-05-27---language-selfheal-key-collision-fix.md`

### ✅ [Combat Enchantments / Berserker's Rage] Strength effect never applied
**🕒 2026-05-27 · 1:15 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Medium (advertised effect missing)
- **Symptom:** Hitting a Piglin with a Berserker's Rage Netherite axe granted no Strength
  effect during combat.
- **Cause:** Strength was gated behind `strength_below` in `combat.yml` — `0.0` for Lv1–3
  and only `0.25` at Lv4 — so it only triggered at Lv4 while already below 25% HP. At full
  HP / lower levels it never fired.
- **Fix:** `OnHitDamageListener` now applies Strength whenever you fight with the enchant
  (refreshed each hit, so it lands from the first hit): Strength I normally, Strength II once
  below the level's `strength_below` threshold.
- **Ref:** `docs/changes/2026-05-27---combat-hud-and-feedback.md`

### ✅ [Combat Enchantments / Bloodletter] Bleed showed the grey "heart" particle, not blood
**🕒 2026-05-27 · 1:15 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Low (cosmetic / wrong effect)
- **Symptom:** Bloodletter's bleed showed an animating grey heart particle rather than blood.
- **Cause:** The bleed particle set was `{"DAMAGE_INDICATOR", "CRIT", "SMOKE_NORMAL"}` —
  `DAMAGE_INDICATOR` is the grey heart-shaped hit indicator.
- **Fix:** Added `Particles.block(...)` and now spawns red `REDSTONE_BLOCK` blood shards that
  fall, on the initial hit and each bleed tick, plus a blood splatter on death.
- **Ref:** `docs/changes/2026-05-27---combat-hud-and-feedback.md`

### ✅ [Enchantments / Commands] /obxench give & givebook chat name had no hover tooltip
**🕒 2026-05-27 · 1:15 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Low (missing convenience tooltip)
- **Symptom:** The `/obxench give` / `givebook` confirmation message showed the enchant name
  without the hover tooltip that `/obxench info`, `list`, and the apply feedback use.
- **Cause:** The confirmation used plain `languages.send`, not the hover-aware path.
- **Fix:** Now renders the line and attaches the enchant tooltip to the name via
  `EnchantHover.send`, matching the "Applied &lt;enchant&gt;" feedback.
- **Ref:** `docs/changes/2026-05-27---arcane-codex-guide-books.md` (give/givebook surface)

### ✅ [Combat Enchantments / Scheduler] Combat aura particles never stop (Folia)
**🕒 2026-05-27 · 12:36 PM ET**
- **Status:** ✅ Fixed
- **Severity:** High (visual + performance leak; one leaked task per kill)
- **Symptom:** After killing mobs with an Apex Predator (or any combat aura — Soulreaver,
  Endless Hunger milestone, Executioner's Cry / Devastator / Tempest shockwaves) the gold/
  flame particles followed the player **forever**, persisting even after the weapon was
  removed from the inventory.
- **Cause:** Combat auras run as **self-cancelling repeating tasks**. On **Folia**,
  `GlobalRegionScheduler.runAtFixedRate(...)` returns a `ScheduledTask` whose concrete
  class is **non-public**; `FoliaCancellableTask.cancel()` resolved `cancel()` off
  `delegate.getClass()` and invoking it threw `IllegalAccessException` (declaring class not
  accessible), which a `catch (Throwable ignored)` swallowed. The cancel **silently failed**,
  so the task kept emitting particles every 4 ticks for as long as the player was online.
- **Fix:** (1) `SchedulerAdapter.FoliaCancellableTask` now resolves `cancel`/`isCancelled`
  from the **public `ScheduledTask` interface** (+ `setAccessible(true)`), fixing cancel for
  every self-cancelling repeating task. (2) Defense-in-depth in `CombatParticleService`:
  `spawnAura`/`spawnShockwave` check expiry **before** spawning, wrap the body in try/catch,
  and are tracked + cancelled on disable (`clear()`, called from `Main.onDisable`).
- **Player remediation for stuck tasks:** server restart, `/obx reload`, or relog.
- **Ref:** `docs/changes/2026-05-27---folia-task-cancel-particle-leak-fix.md`

### 🔵 [Combat Enchantments / Apex Predator] No Rare/Epic scroll drop after ~20 kills
**🕒 2026-05-27 · 12:36 PM ET**
- **Status:** 🔵 By design / No action
- **Severity:** N/A (reported as a bug; confirmed working)
- **Report:** ~20 piglin kills with an Apex Predator sword produced no scroll drop.
- **Finding:** The harvest is **5% per kill**. P(at least one in 20) = `1 − 0.95²⁰ ≈ 64%`,
  so seeing **zero is ~36%** — ordinary variance. Code path verified
  (`OnKillListener.dropRandomScroll`): picks a random Rare/Epic enchant and drops a scroll
  at the **victim's death location** (easy to miss in a mob pile).
- **Resolution:** No change. The drop chance can be raised temporarily in
  `enchants/combat.yml` (`apex_predator → scroll_drop_chance`) for deterministic testing.

### 🔵 [Combat Enchantments / Apex Predator] "Only one can be active" warning has no enforcement
**🕒 2026-05-27 · 12:36 PM ET**
- **Status:** 🔵 By design
- **Severity:** N/A (behavior clarification)
- **Report:** A second Apex weapon in the inventory shows the "only one can be active"
  action-bar warning, but it's unclear whether it does anything.
- **Finding:** Combat enchants only read the **held (main-hand)** weapon, so a second Apex
  item in the inventory is **already inert** — the warning merely surfaces that you're
  carrying a redundant copy; there is no additional suppression logic.
- **Resolution:** Informational only by design. Optional future enhancement (not yet
  implemented): block applying a second Apex item, or hard-disable the held one while a
  duplicate exists.

---

## 2026-05-26

### ✅ [Enchantments / Lore + Glow] Custom enchant items leaked a vanilla Unbreaking I
**🕒 2026-05-26 · 12:59 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Medium (cosmetic/incorrect tooltip)
- **Symptom:** Applying a custom enchant (e.g. **Headhunter**) to an item also showed a
  vanilla **Unbreaking I** in the item's Vanilla lore section.
- **Cause:** The glow utility falls back to a hidden Unbreaking(DURABILITY) lvl-1 as a glint
  source when the reflective `setEnchantmentGlintOverride` fails; the reflective call lacked
  `setAccessible(true)`, so it fell back to the Unbreaking hack, which the new sectioned
  lore then rendered as a real Vanilla entry.
- **Fix:** `Glow.trySetGlint` now uses `setAccessible(true)`; `EnchantStorage` drops a lone
  Unbreaking I from the Vanilla section when custom enchants are present and applies
  `HIDE_ENCHANTS` whenever it manages the vanilla display.
- **Ref:** `docs/changes/2026-05-26---glow-unbreaking-leak-fix.md`

### ✅ [Core / Logging] Doubled console prefix `[OBX] [OBX]`
**🕒 2026-05-26 · 10:57 AM ET**
- **Status:** ✅ Fixed
- **Severity:** Low (cosmetic console noise)
- **Symptom:** Plugin log lines were printed with a duplicated `[OBX] [OBX]` prefix.
- **Cause:** The plugin's logger output combined Bukkit's auto-prefix with a manually-added
  one.
- **Fix:** Console logging pass — de-duplicated the prefix and added the ANSI color theme;
  also folded in MOTD/enchant fixes from the same pass.
- **Ref:** `docs/changes/2026-05-26---console-log-theme-and-fixes.md`

---

## 2026-05-25

### ✅ [Localization / Language] German messages rendered as mojibake
**🕒 2026-05-25 · 4:48 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Medium (garbled German text)
- **Symptom:** German (`sprache_de.yml`) messages showed corrupted characters instead of
  umlauts.
- **Cause:** Encoding mismatch when generating the language file from `MessageDefaults`.
- **Fix:** Language files are now written/read as UTF-8 with a self-heal in `LanguageFile`;
  German defaults use ASCII transliterations (ae/oe/ue/ss) in `MessageDefaults` to stay
  encoding-safe.
- **Ref:** `docs/changes/2026-05-25---german-message-encoding-fix.md`

### ✅ [MOTD / Server List] Player-list hover not displaying on Paper
**🕒 2026-05-25 · 12:54 PM ET**
- **Status:** ✅ Fixed
- **Severity:** Medium (feature not rendering)
- **Symptom:** The custom server-list (MOTD) hover tooltip did not appear on Paper.
- **Cause:** Paper requires an explicit `PaperServerListPingEvent` listener, and player-sample
  hover needs a properly-wrapped `GameProfile` (Paper name validation rejected raw profiles).
- **Fix:** Added the Paper ping listener and wrapped the raw `GameProfile` into a
  `CraftPlayerProfile` (gradient → §x hex legacy for the line itself).
- **Ref:** `docs/changes/2026-05-25---hover-playerprofile-and-worldedit-compass.md`,
  `docs/changes/2026-05-25---motd-gradient-and-hover.md`

---

> **Older history:** additional fixes (e.g. MOTD/welcome hover-rendering fixes and staff-menu
> head fixes around 2026-05-09, the `api-version` startup fix, and the `gamerule` world-controls
> fix on 2026-01-04) are recorded individually in `docs/changes/` (look for filenames ending
> in `-fix.md`). Migrate them into entries above as needed.
