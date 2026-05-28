# Hub Vanish Debounce, Launchpad Action-Bar, Instant Selector, Hover Hardening

■ **Created:** 2026-05-25 12:31 pm
■ **Last Updated:** 2026-05-25 12:31 pm

## Issues & Fixes

### Player-visibility item double-toggles
- The 1.9+ off-hand guard alone wasn't stopping it, so added a hard **debounce**:
  `HubItemUseListener` now ignores a repeat fire of the SAME hub item from the
  same player within 150 ms (`isDuplicateFire`, keyed by `uuid|itemId`). The
  duplicate copy of a dual-fire arrives in the same tick, so it's dropped while a
  genuine later click still registers. The jump rod is exempt (it acts via
  `PlayerFishEvent`, not here). One click = one toggle, guaranteed.
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/player/HubItemUseListener.java`

### Launchpad cooldown showing in chat instead of the action bar
- **Cause:** `ComponentMessenger.sendActionBar` only tried the Spigot
  `ChatMessageType.ACTION_BAR` reflection, which no longer resolves on recent
  Paper; on failure it fell back to `player.sendMessage(...)` → chat.
- **Fix:** It now tries Adventure's `Audience#sendActionBar(Component)` first (the
  modern Paper path), then the Spigot reflection, and — crucially — **no chat
  fallback** (the countdown is sent several times per second; a chat fallback
  would flood chat). The launchpad countdown/timer now renders in the action bar.
- `src/main/java/dev/sergeantfuzzy/sfcore/util/text/ComponentMessenger.java`

### Server selector — instant open, no teleport on that path
- `ServerSelectorMenu.open` now opens the inventory **immediately** on the click
  tick (player-count request still fires in the background; counts fill in on the
  next open) instead of after a 6-tick delay. No delayed window between click and
  menu.
- The server-selector handler only cancels the interaction and opens the GUI —
  there is **no teleport code anywhere on the server-selector path**. The only
  interaction-driven teleport in the plugin is the jump rod, and it is gated on
  the jump-rod item id, so a compass cannot trigger it. If a teleport still
  occurs, it is coming from another plugin reacting to the compass right-click
  (navigator / spawn-compass / second selector plugin) or from clicking the jump
  rod by mistake — see "Testing notes".
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/player/ServerSelectorMenu.java`

### Server-list player-count hover not appearing
- Replaced the lambda `EventExecutor` used to bridge `PaperServerListPingEvent`
  with a **named class** (`PaperPingExecutor`) so the dispatch can't depend on a
  lambda surviving obfuscation, and added a startup log
  (`[SF-Core][MOTD] Registered Paper ping listener …`) so registration is visible.
- The existing per-event-class ping diagnostic still logs the outcome on the
  first ping (`[SF-Core][MOTD] ping handled: event=…, sample=…`). On Paper this
  prints two lines — the base event (`fail:no-sample-api`) and the Paper event
  (`ok:mutate getPlayerSample …`). The Paper line confirms the hover was applied.
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/server/MotdPingListener.java`

## Testing Notes
- Server-list pings are **client-cached** — after updating the jar and restarting
  the server, re-open the multiplayer screen (or restart the client) to force a
  fresh ping; a stale cache shows the old result.
- Console confirms the hover wiring: look for `Registered Paper ping listener`
  at startup and `ping handled: event=…PaperServerListPingEvent…, sample=ok:…`
  on the first ping. If that line shows `ok:…`, the sample is set server-side.
- Server-selector teleport: confirm the compass is in the slot you expect
  (`items.server-selector.slot` in `systems/hub.yml`) and that no other
  compass/navigator plugin is installed; SF-Core's compass handler has no
  teleport.

## Verification
- `./maven/bin/mvn -DskipTests clean package` builds clean; obfuscated and
  `-unobf` jars produced.

## Suggested Commit Message

```
Fix (hub): Debounce vanish toggle, action-bar launchpad countdown, instant selector open, harden Paper hover registration
```
