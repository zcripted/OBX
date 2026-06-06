# Remove JetBrains MCP Server Configuration

■ **Created:** 2026-06-04 4:15 pm (America/Detroit)

■ **Last Updated:** 2026-06-04 4:15 pm (America/Detroit)

## Summary
Completely removed the JetBrains IDE MCP server integration and all associated
settings. The plugin has no runtime dependency on the MCP server; it was only a
development convenience bridge into IntelliJ. Verified the build still passes via
the Gradle wrapper.

## Categories

### Config
- Deleted `.mcp.json` (root) — previously defined the `jetbrains` SSE server at
  `http://127.0.0.1:64342/sse`. This was its only entry, so the file was removed
  entirely.
  - File: `.mcp.json` (deleted)
- Removed MCP enablement keys from local Claude settings:
  - Dropped `"enabledMcpjsonServers": ["jetbrains"]`
  - Dropped `"enableAllProjectMcpServers": true`
  - File: `.claude/settings.local.json`

### Notes
- The historical change file
  `docs/changes/2026-06-04---market-readiness-hardening.md` still references the
  MCP server registration; per the project's change-file consistency rules, prior
  change files are not rewritten — this file documents the removal instead.

## Testing
- `.\gradlew.bat build` → **BUILD SUCCESSFUL in 17s** (63 actionable tasks).
- Both shippable jars produced:
  - `plugin/build/libs/OBX-1.0.0-beta-b1-unobf.jar`
  - `plugin/build/libs/OBX-1.0.0-beta-b1.jar` (ProGuard-obfuscated, regenerated)

## Suggested Commit Message
```
Chore (config): Remove JetBrains MCP server config and settings
```
