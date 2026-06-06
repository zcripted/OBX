# Player List Layout Polish — Count in Header, Spaced Sections

■ **Created:** 2026-06-03 1:07 PM (America/Detroit)

■ **Last Updated:** 2026-06-03 1:07 PM (America/Detroit)

Refined the `/list` box layout: moved the online count into the header bar and separated
the Staff and Players sections with a blank line for a cleaner, more organized look.
`./gradlew build` is **green**; both jars produced; EN/DE/ES parity test passes.

## GUIs / Messages

- **Count moved to the header** (`info.list.header`): the top bar now reads
  `▍ 𝗢𝗕𝗫 › Players · {count}/{max}` (matching the `/pl` header style). The separate body
  summary line was removed.
- **Sections no longer touch**: a blank line is inserted before the **Players** section so
  the Staff and Players blocks are visually separated:

  ```
  ▍ 𝗢𝗕𝗫 › Players · 5/20
  ──────────────────────

    ▸ Staff (2)
      AdminA, ModB

    ▸ Players (3)
      Alice, Bob, Carl
  ──────────────────────
  ```

- Removed the now-unused `info.list.summary` and `info.list.entries` keys (EN/DE/ES); the
  Players-section key (`info.list.section.players`) became a 2-line list with a leading
  blank to carry the separator within the message file (themeable, consistent with the
  list-valued header/footer).

## Files

- `features/playerinfo/.../command/ListCommand.java` — dropped the body summary send.
- `core/.../language/MessageDefaultsEN.java` / `DE.java` / `ES.java` — header count, blank
  before Players section, removed `summary`/`entries` keys.

## Suggested Commit Message

```
Polish (/list): move the online count into the header bar and add a blank line between the Staff and Players sections for a cleaner, more professional layout
```
