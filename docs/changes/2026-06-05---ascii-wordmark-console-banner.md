# ASCII Wordmark Console Banner (Enable/Disable)

■ **Created:** 2026-06-05 5:52 pm

■ **Last Updated:** 2026-06-05 6:11 pm

## Summary

Replaced the line-list enable/disable console banner with a styled Unicode block-art banner
(rev 2, 5:56 pm: converted from plain ASCII to █ block + box-drawing characters with
✔ / ⚠ / ✗ status symbols and · separators):

```
 ██████╗ ██████╗ ██╗  ██╗
██╔═══██╗██╔══██╗╚██╗██╔╝   OBSIDIAN EXTENDED  v1.0.0
██║   ██║██████╔╝ ╚███╔╝    Forged from Obsidian
██║   ██║██╔══██╗ ██╔██╗    by zcripted
╚██████╔╝██████╔╝██╔╝ ██╗   Paper 1.21.4 · Java 21 · enabled in 1.13s
 ╚═════╝ ╚═════╝ ╚═╝  ╚═╝
══════════════════════════════════════════════════
 ✔ PlaceholderAPI   ✔ Vault   ⚠ ProtocolLib (optional)
 ✔ Storage SQLite   ✔ Loaded 14 holograms
══════════════════════════════════════════════════
 Docs     github.com/zcripted/OBX
 Support  discord.gg/UxktSyT9Ag
```

Styling per the banner spec:
- **Logo**: purple → magenta truecolor gradient (`#b794f6` → `#e879f9`) — the "obsidian"
  identity. Two-tone depth: solid `█` glyphs carry the full gradient; the box-drawing
  outline (`╔ ═ ║ ╚ ╝ ╗`) renders as the same hue dimmed to ~55% (shadow).
- **Name**: bold near-white; **tagline**: italic gray; **version**: light purple
- **Environment line**: gray labels, white values (platform + MC version, Java spec version,
  load time), dark-gray `·` separators
- **Status markers**: green `✔` found · amber `⚠` optional missing · red `✗` required
  missing (SQLite store — the one dependency OBX truly requires)
- **Section labels**: teal (`Docs`/`Support`); **URLs**: light blue (scheme stripped);
  **dividers**: dark-gray `═` rules

Dynamic content:
- Soft-dependency detection at enable time: PlaceholderAPI, Vault, ProtocolLib (presence via
  the plugin manager; all optional → amber when absent).
- SQLite store state (red `[✗] (unavailable)` when the store failed to open).
- Live hologram count from the hologram registry (row omitted when the service isn't up).
- Links come from `config.yml` `links.wiki` / `links.discord` as before.
- The **disable** banner prints the wordmark block only (env line shows `disabling…`);
  hook/storage rows are skipped because services are already torn down at that point.

Note: the previous banner's BuiltByBit link and release date rows are not part of the new
design (per the provided mock); `links.builtbybit` and the build-info release date remain in
config/metadata for other uses.

### Rev 3 (6:00 pm) — full/compact config gating + live-value audit
- **All banner values are computed at print time — nothing is hardcoded:** load time is the
  measured `System.nanoTime()` span of `onEnable` (set immediately before the banner prints);
  hook markers are live `PluginManager#getPlugin` presence probes; storage is the live
  `SqliteDataStore#isAvailable()` state; the hologram count is the live registry size;
  platform/MC version come from `PlatformInfo`, Java from the runtime, version/author from
  the plugin description.
- **New config gate `console.banner-style: full|compact`** (config.yml):
  - `full` — the gradient block logo, status rows, and Docs/Support links (default).
  - `compact` — a one-line summary (`OBX v1.0.0 · Paper 1.21.4 · Java 21 · enabled in 1.13s`)
    plus one status line running the same live checks.
  - The value is read **each time the banner prints**, so `/obx reload` (which re-reads
    config.yml) applies the new style to the next banner — the disable banner on stop and
    the enable banner on the following start.

### Rev 4 (6:05 pm) — clickable Docs/Support links
- The Docs/Support rows are now wrapped in **OSC 8 terminal hyperlinks**
  (`ESC ] 8 ; ; url ESC \`): the rows still display the clean scheme-less text
  (`github.com/zcripted/OBX`) but click through to the full URL on terminals that support
  it — Windows Terminal (Ctrl+Click), iTerm2, and most modern emulators / hosting web
  consoles. Terminals without support consume the escape and show plain text.
- New `hyperlink(url, text)` helper; `stripAnsi(...)` extended to remove OSC 8 wrappers on
  the plain-logger fallback path (alongside the existing CSI color stripping).

### Rev 5 (6:11 pm) — reflection-based FUNCTIONAL hook probes + softdepend
- The hook markers upgraded from presence-only to **three-state functional checks**
  (reflection only — OBX still compiles against no optional plugin):
  - `✔` green — plugin **enabled AND usable**: Vault = an `Economy` provider is actually
    registered with the services manager (normally OBX's own `VaultEconomyProvider`);
    PlaceholderAPI = `me.clip.placeholderapi.PlaceholderAPI` resolves;
    ProtocolLib = `com.comphenix.protocol.ProtocolLibrary` resolves.
  - `⚠` amber with a reason — installed but **not usable**: `(failed to enable)`,
    `(no economy provider)`, `(api unreachable)`.
  - `⚠ (optional)` — not installed.
- `plugin.yml` now declares `softdepend: [Vault, PlaceholderAPI, ProtocolLib]` so OBX
  enables **after** them — by banner time their enable state and service registrations are
  final, not racing. This also hardens the Vault economy-provider registration order.

## Categories

### Internal
- `plugin/src/main/java/dev/zcripted/obx/OBX.java`
  - `buildBannerLines(...)` rewritten (block wordmark + aligned info column + status rows + links);
    now gated on `console.banner-style` (read live at print time → reload-aware)
  - New: `buildCompactBannerLines(...)` (compact style), `gradientText(...)` (full-brightness
    gradient for the compact wordmark)
  - New helpers: `hookLabel(...)`, `storageStatus()`, `hologramCount()`, `stripScheme(...)`,
    `ansiRgb(r,g,b)` (24-bit ANSI), `gradientArt(...)` (two-tone █/outline tinting) and
    `ansiForRatio(ratio, brightness)`
  - `GRADIENT_START`/`GRADIENT_END` retuned to `#b794f6` → `#e879f9`

### Config
- `plugin/src/main/resources/config.yml` — new `console.banner-style: full` key with
  full/compact documentation and reload semantics.

## Verification
- `.\gradlew.bat build` — **BUILD SUCCESSFUL**; both jars produced.

## Suggested Commit Message
```
Style (console): ASCII OBX wordmark banner with gradient, hook/storage status, and links
```
