# Agent Instructions

## Project Goal
Work on this IntelliJ project as a **production-grade Minecraft plugin codebase**.

## Ground Rules
- Do **not** remove features unless explicitly requested.
- Keep code style consistent with existing files.
- Add null-safety and permission checks where appropriate.
- Avoid breaking API changes unless explicitly asked.
- Do **not** access the network outside of this terminal session unless the user gives explicit permission.
- Do **not** read from or modify files outside of this project directory unless the user gives explicit permission.
- Treat these access restrictions as persistent for every new session started in this project.

## Minecraft Targets
- Support **Paper / Spigot 1.12.x → 1.21.x** (and PurPur where applicable).
- Use **Adventure (MiniMessage)** for messages where present.
- GUI item hover tooltips must be:
    - Detailed
    - Cleanly formatted
    - Use dividers and new lines where appropriate
- **Anytime a message is added or edited**, update both base languages via
  `core/language/MessageDefaults.java` using `def(key, en, de)` — this guarantees
  EN/DE parity by construction and generates the runtime language files:
    - `lang/en.yml`
    - `lang/de.yml`
    - `lang/es.yml` — Spanish; English-fallback by default, with curated overrides
      in `MessageDefaults.spanishOverrides()`. Untranslated keys render in English.

## Output Expectations
- If instructions are ambiguous:
    - Make a best-effort assumption
    - Clearly document the assumption in code or docs

## Testing Checklist
- Gradle build completes with **no errors**
- Plugin builds successfully (both jars produced)

## Build Tooling
- **Gradle multi-module build** (Maven has been retired; there is no `pom.xml`).
- Use the committed Gradle **wrapper** — do NOT rely on a global `gradle` on PATH:
    - PowerShell: `.\gradlew.bat build`
    - Bash: `./gradlew build`
- Module layout: `:api` (public interfaces) ← `:core` (framework + platform seam +
  util) ← `:features:*` (one subproject per feature) ← `:plugin` (thin `OBX`
  bootstrap + Shadow aggregator). Convention plugins live in `buildSrc`
  (`obx.java-conventions`, `obx.feature-conventions`); versions are pinned in
  `gradle/libs.versions.toml`.
- `./gradlew build` runs the full pipeline: compile + test → `:plugin:shadowJar`
  merges everything into `OBX-<ver>-unobf.jar` → `:plugin:proguard` obfuscates it
  (via `proguard.pro`) into the shippable `OBX-<ver>.jar` (+ a mapping file).
- ProGuard `Note:` lines for reflective accesses (Adventure / Folia / MiniMessage
  detection) are informational, not build failures.

---

## Documentation & Change Tracking (Required)

### Docs Directory Usage
- Use the `/docs/` directory to track development changes.
- All changes must be documented unless explicitly told otherwise.

### Command & Permission Documentation
- Keep `docs/information/COMMANDS+PERMISSIONS.md` up to date whenever commands, permissions, or aliases change.
- Maintain the Player/Admin tables so they reflect the current command set.

### Change File Location & Naming
- Store change files in:
`/docs/changes/`
- File naming format:
`YYYY-MM-DD---short-name-of-change.md`
  - Example: `2025-12-29---spawn-gui-permissions.md`

### Required Metadata in Change Files
Each change file must include, near the top:

- **Created timestamp** (EST / America/Detroit)
- **Last updated timestamp** (if still in progress)

Example:

■ **Created:** 2025-12-29 5:00 pm 

■ **Last Updated:** 2025-12-29 7:00 pm  

## Change File Structure Requirements

Each change file must include:

- **Categories** to organize what was changed  
  *(examples: Commands, GUIs, Permissions, Config, API, Internal)*

- **Clear bullet points** under each category describing changes

- **Direct file paths** to every file modified  
  *(example: `src/main/java/dev/example/core/SpawnManager.java`)*

### Suggested Commit Message (Optional but Encouraged)

Each change file may include a **Suggested Commit Message** section.

- Keep it concise and descriptive

**Example:**

Suggested Commit Message
```
Feature (spawn): Add permission checks and GUI hover tooltips
```

## Consistency Rules

- Timestamps must always use America/Detroit (EST/ET)
- Do not rename or move change files once created
- If continuing work on the same change:
  - Update Last Updated
  - Do not change the Created timestamp