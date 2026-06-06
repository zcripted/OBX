# 🎨 ASCII Wordmark Console Banner

> The enable/disable console banner is now a purple→magenta gradient **OBX wordmark** with an
> aligned info column, live hook/storage status markers, and teal Docs/Support link rows —
> replacing the old line-list banner.

| Field | Value |
|-------|-------|
| **Status** | 🟡 Ready to commit (uncommitted) |
| **Created** | 2026-06-05 5:52 PM EST |
| **Last Updated** | 2026-06-05 6:11 PM EST |
| **Author** | zcripted |
| **Scope** | Console banner styling (OBX bootstrap only) |
| **Files changed** | 3 modified |
| **Categories** | Internal · Console/UX |
| **Verification** | ✅ `gradlew build` green · both jars produced |

---

## 📋 Summary (patch notes)

The server console now greets you with the OBX logo instead of a plain list:

- **Gradient block logo** — the OBX letters render in Unicode block/box-drawing art
  (`█ ╔ ═ ╗ …`) tinted with the purple→magenta "obsidian" gradient (`#b794f6` → `#e879f9`,
  truecolor ANSI); solid `█` glyphs get the full color, the outline a dimmed shadow of the
  same hue.
- **At-a-glance info column** — bold plugin name + version, italic tagline, author, and an
  environment line (`Paper 1.21.4 | Java 21 | enabled in 1.13s`).
- **Live status row** — the hook markers are **functional** checks, not just "is the jar
  there": `✔` means the plugin is enabled *and* its integration is usable (Vault → an
  economy provider is registered; PlaceholderAPI/ProtocolLib → the API class resolves);
  `⚠` carries a reason (`failed to enable` / `no economy provider` / `api unreachable` /
  `optional` when absent); red `✗` if the SQLite store failed to open — plus the number of
  holograms loaded. `softdepend: [Vault, PlaceholderAPI, ProtocolLib]` ensures OBX enables
  after them so these states are final. Environment values separated by `·`, dividers
  drawn with `═`.

### ⚙️ Plugin metadata
- [`plugin/src/main/resources/plugin.yml`](../../../plugin/src/main/resources/plugin.yml)
  — `softdepend: [Vault, PlaceholderAPI, ProtocolLib]` (also hardens the Vault
  economy-provider registration order).
- **Quick links** — teal `Docs` / `Support` labels with light-blue URLs (from `config.yml`),
  now **clickable** via OSC 8 terminal hyperlinks (Ctrl+Click in Windows Terminal, iTerm2,
  most modern consoles); the display stays clean (`github.com/…`) while the click opens the
  full URL. Unsupported terminals simply show the text.
- The shutdown banner prints the logo block with a red `disabling…` state.
- **Full or compact, your choice** — `console.banner-style: full|compact` in config.yml.
  Compact is a one-line summary plus a single status line with the same live checks. The
  setting is read each time the banner prints, so `/obx reload` applies a change to the
  next banner (stop → start).
- **Everything is live data** — load time is genuinely measured (`onEnable` start → banner),
  hook markers are real plugin-manager probes, storage/hologram values come straight from
  the running services. No canned text.

### ⚙️ Config
- [`plugin/src/main/resources/config.yml`](../../../plugin/src/main/resources/config.yml)
  — new `console.banner-style: full` key (full/compact + reload semantics documented).

## 🔄 Changes

### 🎨 Banner
- [`plugin/src/main/java/dev/zcripted/obx/OBX.java`](../../../plugin/src/main/java/dev/zcripted/obx/OBX.java)
  — `buildBannerLines(...)` rewritten; new `hookLabel`/`storageStatus`/`hologramCount`/
  `stripScheme`/`ansiRgb` helpers; gradient constants retuned to `#b794f6 → #e879f9`.
  BuiltByBit/release-date rows dropped per the new design (config + build metadata retained).

### 📄 Change file
- [`docs/changes/2026-06-05---ascii-wordmark-console-banner.md`](../../changes/2026-06-05---ascii-wordmark-console-banner.md)

---

## ✅ Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; `OBX-<ver>-unobf.jar` + `OBX-<ver>.jar` produced.

## Suggested Commit Message
```
Style (console): ASCII OBX wordmark banner with gradient, hook/storage status, and links
```
