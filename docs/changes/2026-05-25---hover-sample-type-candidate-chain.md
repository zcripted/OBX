# Server-List Hover — Robust Sample-Type Candidate Chain

■ **Created:** 2026-05-25 3:47 pm
■ **Last Updated:** 2026-05-25 3:47 pm

## Problem

The player-count hover still didn't appear on Paper (1.21). The previous fix
detected the sample element type from the event generics, but that's brittle: if
the generic type can't be read (raw/erased) or the declared type differs from the
list's actual runtime-checked element type, building the wrong type still makes
`getPlayerSample().addAll(...)` throw `ClassCastException` and the hover stays
empty.

## Fix — `MotdPingListener.applyHoverSample`

Now TRIES each candidate element type until the event's sample list accepts one,
instead of committing to a single guessed type:

1. the type detected from the event generics (e.g. `PlayerProfile`),
2. `com.destroystokyo.paper.profile.PlayerProfile`,
3. `org.bukkit.profile.PlayerProfile`,
4. `com.mojang.authlib.GameProfile`,
5. Paper `ListedPlayerInfo`.

For each, it builds the matching objects (`ProfileFactory`: `Bukkit.createProfile`
for player profiles, the `(UUID,String)` constructor for GameProfile/ListedPlayerInfo)
and attempts to apply them via the setter or by mutating `getPlayerSample()`. On a
`ClassCastException` it **restores the list's original contents** and tries the next
candidate, so a failed attempt never wipes a real player sample. The first type the
list accepts wins (modern Paper → `PlayerProfile`).

The one-time-per-event-class diagnostic is back (INFO on success, WARNING on
failure) so the outcome is visible:
```
[OBX][MOTD] ping handled: event=...StandardPaperServerListPingEventImpl, hoverLines=4, sample=ok:mutate x4 type=PlayerProfile
```

## Verification
- `./maven/bin/mvn -DskipTests clean package` builds clean; obfuscated + `-unobf`
  jars produced.

## If the hover STILL doesn't show after this
The console line tells us where it stands:
- `sample=ok:...` → the sample reached the wire; any remaining "no hover" is then
  client-side ping cache (refresh the multiplayer list / restart the client) or a
  proxy/ViaVersion stripping the sample — not OBX. Note that some clients only
  render the sample tooltip when the shown player count is > 0, so testing with a
  player online (or `player-count.online.mode: fake`) is a useful check.
- `sample=fail:no-candidate-accepted [...]` → none of the known types fit; paste
  the bracketed detail and the event class.

## Suggested Commit Message

```
Fix (motd): Try multiple player-sample element types so the server-list hover applies on modern Paper (PlayerProfile) and older APIs
```
