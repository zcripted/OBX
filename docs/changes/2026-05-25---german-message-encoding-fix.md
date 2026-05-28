# German Message Encoding Fix (Mojibake) + Self-Heal

■ **Created:** 2026-05-25 4:48 pm
■ **Last Updated:** 2026-05-25 4:48 pm

## Problem

Every German default message in `MessageDefaults.java` was double-encoded
("mojibake"): the source had been UTF-8, re-read as CP1252, then re-saved as
UTF-8, so umlauts/ß were stored as two-character garbage. German players saw,
e.g., `Dafür` rendered as `DafÃ¼r`, `ausführen` as `ausfÃ¼hren`, `Menüs` as
`MenÃ¼s`.

Confirmed by byte analysis of the file: 56 broken characters — `Ã¼`→ü (31),
`Ã¤`→ä (13), `Ã¶`→ö (10), `ÃŸ`→ß (2). (The `𝗦𝗙-𝗖𝗢𝗥𝗘` math-bold prefix and the
box/arrow decoration characters were fine.)

## Fixes

### 1. Source corrected — `language/MessageDefaults.java`
Replaced each mojibake sequence with the correct German character and saved the
file as UTF-8 (no BOM). Result: ü:34, ä:18, ö:10, ß:4, Ü:1, and **0** `Ã`-prefixed
mojibake. The compiled `MessageDefaults.class` in the built jar now stores German
as correct UTF-8 (`ü`=`C3 BC`, `ä`=`C3 A4`, …) with **zero** double-encoded
sequences — verified directly against the class bytes.

### 2. Self-heal for already-generated files — `language/LanguageFile.java`
New `repairMojibake()` runs when an existing language file is loaded
(`ensureExists`). It rewrites the file (UTF-8) only if a known mojibake sequence
is present, converting `Ã¤/Ã¶/Ã¼/ÃŸ/Ã„/Ã–/Ãœ` back to `ä/ö/ü/ß/Ä/Ö/Ü`. This is
needed because `syncDefaults` never overwrites existing keys, so a server that
already generated `plugins/SF-Core/languages/sprache_de.yml` from an older build
would otherwise keep the broken values. The repair only touches these sequences
(never valid in real text), so server-owner customizations are preserved, and it
is a no-op on a clean file.

## Verification
- Source: 0 mojibake markers in `MessageDefaults.java`; full `src/` scan clean
  except the intentional search strings inside `repairMojibake()`.
- Compiled jar: `MessageDefaults.class` contains correct German UTF-8 and 0
  double-encoded sequences.
- `mvn -DskipTests clean package` builds with exit 0, no "unmappable character"
  warnings; obfuscated + `-unobf` jars produced.
- Runtime path is UTF-8 end-to-end: defaults → `sprache_de.yml` (written UTF-8) →
  Bukkit UTF-8 YAML loader → player.

## Note for maintainers
`MessageDefaults.java` contains literal UTF-8 German characters; always edit it
with a UTF-8-aware editor (the pom already sets `project.build.sourceEncoding=UTF-8`)
so the mojibake doesn't return. New entries should use proper umlauts directly.

## Suggested Commit Message

```
Fix (lang): Repair mojibake German defaults (UTF-8) + auto-heal already-generated language files
```
