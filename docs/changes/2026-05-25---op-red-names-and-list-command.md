# OP Red Names + /list Command

■ **Created:** 2026-05-25 6:12 pm
■ **Last Updated:** 2026-05-25 6:30 pm

## 1. OP/staff names render red (chat + tablist)

### Chat
- `ChatService.getStaffUsernameTemplate()` (new) — name template for OP, default
  `<red>{player}</red>`; config key `format.components.username.staff-template`.
- `ChatFormatter.compose` now picks the staff username template for OP (detected
  by the staff prefix being present) before prepending the `ѕᴛᴀꜰꜰ ┃ ` tag, so an
  OP line reads: red/bold tag + red name + separator + message.
- `chat_management.yml` — documented `username.staff-template: "<red>{player}</red>"`.

### Tablist
- `TablistService.getStaffPlayerFormat()` default name changed gray → red:
  `<red><bold>ѕᴛᴀꜰꜰ ┃ </bold></red><red>{player}</red>`; mirrored in `tablist.yml`.

## 2. `/list` command

New `command/core/ListCommand.java` (CommandExecutor), bound in `Main`.
- Usable by everyone: permission `sfcore.list` (default true).
- Aliases: `/players`, `/online`, `/who`, `/playerlist`.
- Lists online players split into a **Staff** (OP) section and a **Players**
  section — staff names rendered red, players white.

**Styled to match `/pl` (PluginListCommand)** — same report layout, not the
generic divider/prefix style. The command mirrors `PluginListCommand`'s
structure: it collects all lines into one block and emits them together, routing
console output through SF-Core's ANSI console writer (`plugin.writeConsoleLine`)
exactly like `/pl`. Layout:

```
(blank)
▍ 𝗦𝗙-𝗖𝗢𝗥𝗘  ›  Online Players  ·  {online}/{max}
──────────────────────────────  (30× U+2500)
  Staff  ·  {count}              (red, bold)
    Admin, Mod                   (indented, red, comma-joined)
  Players  ·  {count}            (yellow, bold)
    Steve, Alex                  (indented, white, comma-joined)
(blank)
──────────────────────────────
  Legend  ·  ▪ Staff   ▪ Players    ({online}/{max} online)
(blank)
```

- Empty sections show `player.list.none` (italic gray "none"/"keine").
- `MessageDefaults` — new keys `player.list.header` (line-list, EN+DE),
  `player.list.staff`, `player.list.players`, `player.list.line`,
  `player.list.none`, `player.list.footer` (line-list). Reuses the same glyphs
  as `/pl`: `▍` U+258D, math-bold `𝗦𝗙-𝗖𝗢𝗥𝗘`, `›` U+203A, `·` U+00B7, the 30×
  `─` U+2500 divider, `▪` U+25AA.
- `plugin.yml` — new `list` command (with aliases) + `sfcore.list` permission (true).
- `docs/information/about.md` — added the `/list` row to the Player table.

## Notes
- Staff styling (red name + tag) keys off OP status (`player.isOp()`), consistent
  with the chat tag and tablist grouping.
- All added glyphs verified correct UTF-8 in both source and the compiled
  `MessageDefaults.class`: the staff tag (U+0455 U+1D1B U+1D00 U+A730 U+A730
  U+0020 U+2503 U+0020), and the `/list` report glyphs `▍` (U+258D), math-bold
  `𝗦𝗙-𝗖𝗢𝗥𝗘` (surrogate `ED A0 B5 …`), `›` (U+203A), `·` (U+00B7), the 30× `─`
  (U+2500) divider, and `▪` (U+25AA). **0** mojibake.

## Verification
- `mvn -DskipTests clean package` builds exit 0, no errors / unmappable warnings;
  obfuscated + `-unobf` jars produced; `ListCommand.class` present in both jars.
- Compiled class confirmed: math-bold surrogate sequence present, the 30-char
  divider intact (U+2500 count = 30-char `/list` divider + 17-char `core.divider`),
  0 mojibake.

## Suggested Commit Message

```
Feature (chat/tablist): Red OP names; add /list (players/online/who) with staff & players sections
```
