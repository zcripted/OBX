# Moderation profile — strip repeated prefix, clean "card" layout

■ **Created:** 2026-05-09 10:45 pm

## Summary

`/status <player>` (the moderation profile read-out) was rendering 12+
chat lines, each one stamped with the full `&6SF-CORE &8➠ &e` prefix.
That made the report visually noisy in chat and in console — every
factual line had to fight past the prefix before the operator could
read it.

This change drops the `{prefix}` placeholder from every body line and
keeps it only on the header. All body lines now share a single
left-border `│` plus a small section glyph (`●` for status indicators,
`▸` for section headers, `•` for list items, `└` for reason
continuations) so the whole report reads as one indented "card" block
instead of as N independent chat lines.

### Before (each line prefix-stamped)

```
SF-CORE ➠ Moderation Profile: PlayerName
SF-CORE ➠ UUID: xxx | Profile: Live | Session: Online | Updated: ...
SF-CORE ➠ Ban Status: ACTIVE | Type: Permanent | By: Admin | Expires: ...
SF-CORE ➠ Ban Reason: Cheating
SF-CORE ➠ Mute Status: Clear
SF-CORE ➠ Totals: Bans 0 | Tempbans 0 | Kicks 1 | Warnings 2
SF-CORE ➠ More: Mutes 0 | Unmutes 0 | Unbans 0
SF-CORE ➠ Last Warning: ... | By: ... | Reason: ...
SF-CORE ➠ Last Action: ... | At: ... | By: ... | Reason: ...
SF-CORE ➠ Recent Actions: 3
SF-CORE ➠ - Kicked | ... | By: ... | Reason: ...
...
```

### After (header keeps prefix; body indented under a single border)

```
SF-CORE ➠ Moderation Profile › PlayerName
 │ UUID: xxx  ·  Profile: Live Player  ·  Session: Online  ·  Updated: ...
 │ ● Ban  │  ACTIVE  ·  Permanent  ·  by Admin  ·  expires ...
 │     └ Reason: Cheating
 │ ● Mute  │  Clear
 │ ▸ Totals  │  Bans 0  ·  Tempbans 0  ·  Kicks 1  ·  Warnings 2
 │           Mutes 0  ·  Unmutes 0  ·  Unbans 0
 │ ▸ Last Warning  │  ...  ·  by ...  ·  Reason: ...
 │ ▸ Last Action  │  Kicked  ·  ...  ·  by ...  ·  Reason: ...
 │ ▸ Recent Actions  │  (3)
 │   • Kicked  ·  ...  ·  by ...  ·  AFK
 │   • Warned  ·  ...  ·  by ...  ·  Lang
 │   • Muted   ·  ...  ·  by ...  ·  Spam
```

The leading divider (`core.divider`) and trailing divider stay
unchanged — they were already prefix-free.

## Categories

### Language

All sixteen `player.moderation.status.*` body templates were rewritten
in `MessageDefaults`:

- **Header** (`status.header`) — keeps `{prefix}`. Single line,
  identifies the report and names the target.
- **Summary** (`status.summary`) — combined into one
  `│` -prefixed line carrying UUID + Profile + Session + Updated
  separated by `·` middots.
- **Ban / Mute** (`status.ban-active` / `status.ban-clear` /
  `status.ban-reason` / `status.mute-active` / `status.mute-clear` /
  `status.mute-reason`) — `●` status glyph, then `│` separator,
  then status detail. Reasons live on their own continuation line
  starting with `└` so long reasons wrap visibly under their parent.
- **Counts** (`status.counts-primary` / `status.counts-secondary`) —
  `▸` section glyph; secondary line aligns under the totals values
  with explicit indentation.
- **Last warning / action** (`status.last-warning`,
  `status.last-warning-none`, `status.last-action`,
  `status.last-action-none`) — kept reason inline so the headline
  block stays compact (one line per slot, not two), but dropped
  the prefix and tightened the separators.
- **Recent actions** (`status.recent-header`, `status.recent-entry`,
  `status.recent-none`) — header carries `▸`; entries use `•`
  with deeper indent so list items sit clearly under the section
  header.

EN + DE pairs both updated; the layout (`│ ● ▸ • └` glyphs, indent
positions, `·` separators) is identical across locales so the report
reads the same shape regardless of language.

### Internal

- **No code changes** to `ModerationStatusCommand.java` — the existing
  send sequence still calls the same key set in the same order. The
  command's logic (when to send `ban-active` vs `ban-clear`, when to
  emit a `*-reason` follow-up, when to skip `recent-entry`) is
  unchanged.
- The reason continuation lines (`status.ban-reason`,
  `status.mute-reason`) were already part of the existing key set
  and continue to be sent only when the corresponding action is
  active. No new keys were introduced.

## Files modified

- `src/main/java/dev/sergeantfuzzy/sfcore/language/MessageDefaults.java`

## Files added

- `docs/changes/2026-05-09---moderation-status-clean-layout.md`

## Verification

- `& ".\maven\bin\mvn.cmd" -DskipTests package` produced a fresh
  obfuscated `target/SF-Core-1.0.0-SNAPSHOT.jar` with no `[ERROR]`
  or `BUILD FAILURE` lines. Only ProGuard `Note:` lines for
  reflective accesses (informational per CLAUDE.md).
- `LanguageManager.resolveMessages` resolves `{prefix}` strictly as
  a placeholder substitution (line 134:
  `withPrefix.put("prefix", getPrefix(registry))`) — removing the
  placeholder from a template drops the prefix from that line, which
  is exactly what we want for the body lines.
- All Unicode glyphs used (`│`, `●`, `▸`, `•`, `└`, `·`, `›`) render
  in the vanilla Minecraft chat font and in standard ANSI terminals;
  they're the same family of box-drawing / dingbat characters
  already used elsewhere in the project (e.g. `─` in the spawn
  divider, `➠` in the core prefix).

## Notes / non-changes

- `core.prefix` and `core.divider` are unchanged — every other
  command in the plugin still gets its prefix as before.
- `docs/information/about.md` is unchanged — the `/status` command,
  permission, and aliases are unchanged.
- Console output benefits automatically: console messages flow
  through the same `LanguageManager.send` path, so the prefix-strip
  applies in the server log too. The `│ ● ▸` glyphs render fine in
  modern terminals (tested mentally against PowerShell + Windows
  Terminal); legacy `cmd.exe` may render fall-back boxes for some
  glyphs but that's an OS limitation, not a plugin issue.

## Suggested Commit Message

```
Style (moderation): strip the SF-Core prefix from /status body lines and reflow the report into a single │-bordered "card" with ● ▸ • └ section glyphs so the profile reads as one indented block instead of N prefix-stamped chat lines
```
