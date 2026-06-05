<div align="center">

<a href="https://obx.zcripted.dev"><img src="https://i.ibb.co/HfyLH97P/obx-logo-1380.png" alt="OBX — Obsidian eXtended" width="440" /></a>

# OBX — Obsidian eXtended

**Forged from Obsidian.** One JAR. Every server.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8%2B-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](#-supported-platforms)
[![Platforms](https://img.shields.io/badge/Paper%20%7C%20Spigot%20%7C%20Purpur%20%7C%20Folia-6A1B9A?style=for-the-badge)](#-supported-platforms)
[![Java](https://img.shields.io/badge/Java-8%2B-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](#-developer-api)
[![Release](https://img.shields.io/badge/release-1.0.0-8A2BE2?style=for-the-badge)](https://github.com/zcripted/OBX/releases)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/zN3UQyKdfD)

[Discord](https://discord.gg/zN3UQyKdfD) • [BuiltByBit](https://builtbybit.com/resources/obx-obsidian-extended.111131/) • [Wiki](https://obx.zcripted.dev)

</div>

---

## What is OBX?

**OBX** is an all-in-one essentials core for Minecraft servers — a single, lightweight JAR that ships homes, warps, teleports, moderation, economy, staff tools, custom enchantments, holograms, and dozens of quality-of-life utilities in one cohesive package.

Built for compatibility, OBX runs from **Minecraft 1.8.8 through the latest releases** and **adapts itself at runtime** to **Paper, Spigot, PurPur, and Folia** — no per-version downloads, no extra dependencies. One drop-in JAR, every server.

> Stop stitching together fifteen plugins. OBX replaces your essentials, economy, moderation, enchantments, and holograms with one core that's designed to work together — and gets out of your way when you don't need a feature.

## ✨ Why OBX?

- **🧩 One cohesive core** — every module shares the same config, language, storage, and GUI conventions, so nothing fights anything else.
- **🔁 Runtime-adaptive** — detects your platform and version on boot (Folia-aware scheduling included) instead of shipping a jar per version.
- **🎨 GUI-first & MiniMessage-native** — clean control panels, detailed hover tooltips, gradients, and Adventure styling that gracefully downsample on legacy clients.
- **🌍 Trilingual out of the box** — English, German, and Spanish, with per-player language selection.
- **🧪 Modular & toggleable** — enable only the features you want; disabled modules stay dormant.
- **🧑‍💻 Built on a public API** — a dedicated `:api` module for clean integrations.

## 🎮 Supported Platforms

| Platform | Versions | Notes |
| --- | --- | --- |
| **Paper** | 1.8.8 → latest | Recommended. Adventure + Folia scheduler auto-detected. |
| **Spigot** | 1.8.8 → latest | Full support; Adventure features degrade gracefully. |
| **PurPur** | 1.8.8 → latest | Treated as Paper. |
| **Folia** | Supported | Region/entity-aware scheduling throughout. |

Java **8+** runtime. Storage is **SQLite**, created automatically — no external database to configure.

---

## ✨ Features

### 🏠 Movement & Teleportation
- **Homes** — set, list, and travel between multiple personal homes.
- **Spawn & Back** — server spawn, first-join routing, and a reliable `/back`.
- **Warps** — a full warp network with categories, icons, public/hidden flags, and a browsable GUI.
- **Hub / Lobby** — instant return to your server's hub world.
- **Teleport suite** — `/tp`, `/tphere`, `/tpa` requests, `/tpall`, `/tppos`, plus toggles, cancelling, and warmup-based teleports.

### 💬 Communication
- Private messaging with reply, inbox, and a persistent **mail** system.
- **Staff chat**, broadcasts, `/me` actions, and `/socialspy` oversight.
- **Ignore** unwanted players and set personal **nicknames** (impersonation-guarded).

### 🛡️ Moderation & Staff Tools
- Full kit: **ban, tempban, unban, kick, mute, unmute, warn**, ban lists, and player status profiles.
- UUID-authoritative bans enforced at login — name changes can't dodge a ban.
- **Vanish**, **Invsee & Ender Chest** viewing, player **freeze**, and a live **staff overview menu**.
- **Jail system** — jail anchors, durations, auto-release, and containment.

### 💰 Economy
- Player balances, **pay**, a **baltop** leaderboard, and staff economy management.
- **Sell, sellall, and worth** for item-based trading — with atomic, race-safe transactions.

### 🔮 Arcanum Custom Enchantments
A built-in custom-enchantment engine with browsable enchants, guide books, recall points, personal satchel storage, scrolls, and per-player combat-effect toggles — no coding required.

### 🪧 Holograms & Kits
- Create and manage **holograms** with animation support (TextDisplay on modern versions, armor-stand fallback on legacy).
- Claimable starter **kits** with cooldowns.

### ⚙️ Quality of Life
- Instant **heal, feed, vital, and god mode**; **flight** with adjustable fly/walk speeds.
- Item tools: **hat, repair, more, skull, rename, lore, unbreakable, give**, and item info.
- **Virtual workstations** — open crafting, anvil, enchanting, smithing, stonecutter, loom, grindstone, cartography tables and more, anywhere.
- **Disposal** trash bin and quick inventory clearing.

### 🌍 World & Environment Control
- Set **time, day, night, sun, and weather** — globally or per-player.
- World tools: **butcher** mobs, **spawn mobs**, edit **spawners**, **smite** lightning, and grow **trees** on demand.

### 📊 Player Insights & Presentation
- `/seen`, `/firstseen`, `/playtime`, `/near`, `/whois`, `/realname`, and profile cards with quick actions.
- Configurable **scoreboard**, **tablist**, join/leave broadcasts, a customizable in-game **welcome MOTD**, and a server-list MOTD.

---

## 📦 Installation

1. Download the latest `OBX-<version>.jar` from [Releases](https://github.com/zcripted/OBX/releases) or your marketplace download.
2. Drop it into your server's `plugins/` folder.
3. Start the server — OBX detects your platform and version automatically and generates its configs.
4. (Optional) Tune `config.yml`, the per-system files under `systems/`, and the language files under `languages/`.

That's it. One JAR, every supported version.

## 🧾 Commands & Permissions

OBX ships a large command set organized for both players and admins. The complete, always-current reference lives in the repo:

➡️ **[docs/information/COMMANDS+PERMISSIONS.md](docs/information/COMMANDS+PERMISSIONS.md)**

## 🌐 Localization

Fully **trilingual** out of the box — **English (`en`)**, **German (`de`)**, and **Spanish (`es`)** — with per-player language selection via `/language` (`/sprache`). Message text lives in `plugins/OBX/languages/<lang>.yml`; EN/DE parity is enforced by construction, and Spanish ships as a curated catalogue with English fallback for any untranslated key.

---

## 🧑‍💻 Developer API

OBX exposes a stable public API under `dev.zcripted.obx.api.*`. These interfaces ship **inside the OBX jar with their real (un-obfuscated) names**, so you compile directly against the same jar you run — no separate SDK required.

**1. Add OBX as a compile-only dependency** (compile against it, never bundle it):

```gradle
dependencies {
    compileOnly(files("libs/OBX-1.0.0.jar")) // the OBX jar you run on your server
    // plus your usual Paper/Spigot API compileOnly dependency
}
```

**2. Depend on OBX** in your `plugin.yml`:

```yaml
softdepend: [OBX]   # or: depend: [OBX]
```

**3. Obtain a service** from the running OBX instance and program against the API interface:

```java
import dev.zcripted.obx.api.OBXApi;
import dev.zcripted.obx.api.economy.EconomyService;

if (Bukkit.getPluginManager().getPlugin("OBX") instanceof OBXApi obx) {
    EconomyService economy = obx.getEconomyService();

    double balance = economy.getBalance(player.getUniqueId());
    economy.deposit(player.getUniqueId(), player.getName(), 250.0);
}
```

`OBXApi` (in `dev.zcripted.obx.api`) is the single public entry point. Its getters return the public service interfaces:

| Getter on `OBXApi` | Returns (`dev.zcripted.obx.api.*`) |
| --- | --- |
| `getEconomyService()` | `economy.EconomyService` |
| `getTeleportManager()` | `teleport.TeleportManager` |
| `getChatService()` | `chat.ChatService` |
| `getScoreboardService()` | `scoreboard.ScoreboardService` |
| `getTablistService()` | `tablist.TablistService` |
| `getJoinLeaveService()` | `playerinfo.JoinLeaveService` |
| `getAfkService()` | `playerstate.AfkService` |
| `getHubKitApplier()` | `hub.HubKitApplier` |
| `getHubItemUseListener()` | `hub.HubItemUseListener` |

Program against the interfaces — they're designed to stay stable across releases. The implementations stay internal and obfuscated; only the `api` contracts and these getters are part of the public surface.

> Questions or want the API packaged as a standalone artifact? Reach out on [Discord](https://discord.gg/zN3UQyKdfD) or [zcripted.dev](https://zcripted.dev).

---

## 💬 Support & Community

- **Discord** — questions, help, and announcements: [discord.gg/zN3UQyKdfD](https://discord.gg/zN3UQyKdfD)
- **Bug reports** — please open a reproducible [GitHub Issue](https://github.com/zcripted/OBX/issues).
- **Marketplace** — [BuiltByBit](https://builtbybit.com/resources/obx-obsidian-extended.111131/)

## 📄 License

OBX is **proprietary software** — Copyright © 2026 zcripted, **all rights reserved**. It is licensed, not sold, for use on Minecraft servers you own or operate; redistribution, reverse-engineering, deobfuscation, and derivative works are prohibited. See **[LICENSE](LICENSE)** for the full End User License Agreement.

For licensing inquiries, contact the Author at [zcripted.dev](https://zcripted.dev).

---

<div align="center">
<sub><b>OBX — Obsidian eXtended.</b> Forged from Obsidian.</sub>
</div>
