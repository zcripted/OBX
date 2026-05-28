# Hover Sample Type Fix + WorldEdit Compass Conflict + Log Cleanup

■ **Created:** 2026-05-25 12:54 pm
■ **Last Updated:** 2026-05-25 12:54 pm

## 1. Player-count hover (ClassCastException)

Diagnostic from the server:
```
ping handled: event=...StandardPaperServerListPingEventImpl, sample=fail:getter-threw ClassCastException
```
The Paper event WAS received and `getPlayerSample()` WAS found, but `addAll(...)`
threw `ClassCastException`. Cause: `MotdPingListener.detectSampleType` hardcoded
the Paper `ListedPlayerInfo` class as the sample element type, but modern Paper's
`getPlayerSample()` returns a type-checked `List<PlayerProfile>`
(`com.destroystokyo.paper.profile.PlayerProfile`). Adding `ListedPlayerInfo`
objects to that list throws.

**Fix** (`MotdPingListener.detectSampleType`): detect the ACTUAL element type from
the getter/setter generics FIRST, only falling back to the hardcoded
`ListedPlayerInfo` when the generics can't be read. The existing `ProfileFactory`
already builds `PlayerProfile` instances (via `Bukkit.createProfile(UUID, String)`)
for that type, so the sample now populates and the hover renders.

The per-ping diagnostic was also changed to log (as a WARNING) only when the hover
is NOT applied — a working hover is silent.

## 2. Server-selector compass teleport — WorldEdit navigation wand

The reporter's plugins: PlaceholderAPI, SF-Core, Vault, ViaBackwards, ViaVersion,
**WorldEdit**, WorldGuard. WorldEdit's **navigation wand is a COMPASS** by default
(right-click → `/thru`, left-click → `/jumpto`), which teleports a player with
`worldedit.navigation.*` (ops by default) to their crosshairs. So clicking the
compass server-selector fired BOTH WorldEdit's teleport AND SF-Core's GUI — the
exact "teleports to crosshairs and opens the menu" symptom. SF-Core has no
teleport code on the selector path; the teleport was entirely WorldEdit's.

**Fixes:**
- `systems/hub.yml` default `items.server-selector.material` changed `COMPASS` →
  `NETHER_STAR` (fallback `NETHER_STAR`, then `COMPASS`), with an explanatory
  comment. New installs no longer clash with WorldEdit.
- `HubService` now logs a clear WARNING when the server-selector material is a
  COMPASS and WorldEdit is installed, pointing to the fix.

**Existing installs must update their config** (the deployed `hub.yml` still has
COMPASS): set `items.server-selector.material: "NETHER_STAR"` (or any non-compass
item) and run `/hub give`, OR disable WorldEdit's navigation wand.

## 3. `[gradient]` console logs removed

`AdventureMessageUtil` no longer logs the `[gradient] Adventure paths …` static
line or the `[gradient] runtime path: …` line. They were diagnostics: the gradient
renders correctly via the BungeeCord component path (the Adventure direct path
falls back because `sendMessage(Component)`/MiniMessage aren't resolving on this
Paper build, but the fallback produces a true gradient — confirmed working in
chat). No functional change; just quieter console.

## Verification
- `./maven/bin/mvn -DskipTests clean package` builds clean; obfuscated + `-unobf`
  jars produced.

## Suggested Commit Message

```
Fix (motd/hub): Detect PlayerProfile sample type for Paper hover, default selector to NETHER_STAR (WorldEdit compass clash), drop gradient diagnostics
```
