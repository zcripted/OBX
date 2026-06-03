# Rebrand: SF-Core → OBX (Obsidian eXtended)

■ **Created:** 2026-05-31 6:51 pm (America/Detroit)

■ **Last Updated:** 2026-05-31 7:08 pm (America/Detroit)

Full rebrand of the plugin from **SF-Core** (by *SergeantFuzzy*) to **OBX —
Obsidian eXtended** (by *zcripted*). This is a **clean-break** rebrand: the
command, permission, and package namespaces all move to `obx`, with no
backward-compatibility aliases for the old `sf` / `sfcore` names. Existing
servers must re-grant permissions on upgrade.

## Brand identity (locked)
- **Brand mark:** `OBX` (pronounced "Obex")
- **Backronym / full name:** **Obsidian eXtended** (`OB` = OBsidian, `X` = eXtended)
- **Suite identity:** Obsidian (the family; future add-ons brand as `OBX-Combat`, `OBX-Hub`, …)
- **Tagline:** *"Forged from Obsidian."*
- **Palette:** obsidian black → violet → magenta fleck (`#0A0A0F → #2A0A45 → #6A1B9A`)
- **Author:** `zcripted`

## Categories

### Internal / Package
- Moved the entire Java package `dev.sergeantfuzzy.sfcore` → `dev.zcripted.obx`
  (≈250 source files relocated under the new path).
- Renamed main command class `SFCoreCommand` → `ObxCommand`:
  - `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
- Renamed Arcanum admin command class `SfEnchantCommand` → `ObxEnchantCommand`:
  - `src/main/java/dev/zcripted/obx/enchant/command/ObxEnchantCommand.java`
- Updated command bindings in `src/main/java/dev/zcripted/obx/Main.java`
  (`bind("obx", …)`, `bind("obxench", …)`) and all imports.

### Build / Maven
- `pom.xml`: `groupId` → `dev.zcripted.obx`, `artifactId` → `obx`,
  `<name>` → `OBX`, `finalName` → `OBX-${project.version}`, description now
  leads with "Obsidian eXtended (OBX) — Forged from Obsidian."
- Output artifact is now `OBX-1.0.0-SNAPSHOT.jar` (+ `-unobf` / `-obfuscated`).
- `proguard.pro`: `-keep` / `-dontnote` / `-dontwarn` rules repointed to
  `dev.zcripted.obx.*`; header comment rebranded.

### Commands
- Main command `/sf` (aliases `sfcore`, `sfc`) → `/obx` (aliases `obsidian`, `obc`).
- Arcanum command `/sfench` (aliases `sfenchant`, `sfe`) → `/obxench`
  (aliases `obxenchant`, `obxe`).
- Message-key namespace `commands.sf.*` → `commands.obx.*`.
- `src/main/resources/plugin.yml`: `name`, `main`, `author`, `website`,
  command keys, aliases, and all `/sf*` usage strings updated.

### Permissions
- All permission nodes `sfcore.*` → `obx.*` (e.g. `sfcore.home` → `obx.home`,
  `sfcore.teleport.admin` → `obx.teleport.admin`). Applied in `plugin.yml` and
  every command class.

### GUIs / Branding
- Styled brand glyphs `𝗦𝗙-𝗖𝗢𝗥𝗘` → `𝗢𝗕𝗫` (math-bold sans) in message
  prefixes and boxed report headers
  (`src/main/java/dev/zcripted/obx/language/MessageDefaults.java`,
  `src/main/resources/config.yml`, and related GUI/command classes).
- Console startup banner (`Main.java#buildBannerLines`):
  - Recolored the gradient to the obsidian palette
    (`GRADIENT_START {42,10,69}` → `GRADIENT_END {106,27,154}`).
  - Added subtitle line: `Obsidian eXtended — Forged from Obsidian.`

### Config / Resources
- All `*.yml` under `src/main/resources/` rebranded (prefixes, MOTD,
  scoreboard, tablist, hub, holograms, enchants, kits, jails, worth,
  moderation, chat_management).

### Docs
- Rewrote all `docs/changes/*` and `docs/information/*` (incl.
  `COMMANDS+PERMISSIONS.md`, `bug-tracker.md`, `enchantments.md`) from
  SF-Core → OBX, including command and alias listings.
- `README.md`, `INSTRUCTIONS.md` rebranded.
- Change-log filenames were **not** renamed (per project rule); only contents
  updated.

## Follow-up round (2026-05-31 7:08 pm) — links + palette

### Links (finalized)
- `links.wiki` / GitHub → `https://github.com/zcripted/OBX` (was `/wiki`).
- `links.builtbybit` → `https://builtbybit.com/resources/OBX` (was `/obx-core/`).
- Updated in both `src/main/java/dev/zcripted/obx/Main.java` (banner defaults)
  and `src/main/resources/config.yml`.
- Note: the Spigot link in `config.yml` (`spigotmc.org/resources/obx-core/`)
  is still a placeholder — not part of this request.

### Obsidian palette applied to gradients
Canonical MiniMessage gradient: `<gradient:#2A0A45:#6A1B9A>` (deep violet →
magenta, from the locked palette). Scoreboard uses a hand-tuned per-letter
`&#RRGGBB` gradient because its sidebar is legacy team text (no MiniMessage).
- **In-game MOTD** (`config.yml`): first-join broadcast star, the
  "Welcome to the Server" title, and the first-join "A Brand New Adventure!"
  line recolored to the obsidian gradient. Join/leave broadcasts left
  green/red (semantic).
- **Server-list MOTD** (`motd.yml`): `OBX` brand on `line-1` and the
  `OBX Network` hover header recolored to the obsidian gradient.
- **Scoreboard** (`systems/scoreboard.yml`): title changed from `&6&l{plugin}`
  to a per-letter obsidian gradient `&#2A0A45&lO&#4A126F&lB&#6A1B9A&lX`.
- **Holograms**: `holograms.yml` ships no default text gradient (holograms are
  user-authored), so the palette was applied to the HOLOGRAMS subsystem message
  prefix in `MessageDefaults.java` (`&#22D3EE` cyan → `&#6A1B9A` obsidian).

Rebuild after this round: **BUILD SUCCESS** (exit 0).

## Assumptions / Follow-ups
- **Website placeholder:** `website` is set to `https://obx.zcripted.dev` as a
  best-effort placeholder. Banner links (`links.builtbybit`, `links.wiki`,
  `links.discord`) derived to `…/obx-core/`, `github.com/zcripted/OBX/wiki`,
  `discord.gg/zcripted`. Replace with real URLs when available.
- **On-disk project folder** (`…/SF-Core`) and IDE/harness files (`.idea/`,
  `.claude/settings.local.json`) were intentionally left unchanged — they
  reference local/external paths, not the plugin brand.
- **`docs/information/Renaming.txt`** (active branding-decision scratchpad) was
  intentionally excluded from the find/replace to preserve the rationale.
- **Palette rollout:** only the console banner gradient was recolored. Applying
  the obsidian palette to in-game MOTD / scoreboard / hologram gradients is a
  separate visual-reskin pass, not yet done.
- The full name in code/description uses **"Obsidian eXtended"** (per the locked
  backronym); the earlier "Obsidian eXtensions" wording was treated as a typo.

## Verification
- `& ".\maven\bin\mvn.cmd" -DskipTests package` → **BUILD SUCCESS** (exit 0).
- Obfuscated jar verified: `plugin.yml` has `main: dev.zcripted.obx.Main`,
  `author: zcripted`, command `obx:`; `dev/zcripted/obx/Main.class` present.
- Repo-wide scan confirms zero remaining `sfcore` / `SergeantFuzzy` / `SF-Core`
  / `𝗦𝗙` / `sfench` tokens in shipped code, resources, or docs.

## Suggested Commit Message
```
Rebrand: SF-Core -> OBX (Obsidian eXtended), author zcripted

Move package dev.sergeantfuzzy.sfcore -> dev.zcripted.obx; clean-break
rename of perms (sfcore.* -> obx.*) and commands (/sf -> /obx,
/sfench -> /obxench). Rebrand plugin.yml, pom, proguard, configs, docs,
console banner (obsidian palette + "Forged from Obsidian." tagline).
```
