# Hub / Lobby System

■ **Created:** 2026-05-24 6:00 pm
■ **Last Updated:** 2026-05-24 10:22 pm

## Summary

Introduces a per-world hub / lobby system to SF-Core. When enabled in the
hub-tagged worlds, players join with a configurable hotbar kit (Server
Selector, Jump-To Rod, Vanish-All toggle, Launchpad), and SF-Core handles
BungeeCord / Velocity Connect messaging, live player-count caching, fall
damage suppression on launches, and per-player launchpad cooldown with a
live action-bar countdown above the hotbar. Outside hub worlds (or when
the master toggle is off) the system is dormant — listener overhead drops
to a single boolean read per event.

## Categories

### Config / Files
- New config file: `src/main/resources/systems/hub.yml` (master toggle,
  per-world whitelist, kit options, four item definitions with per-item
  enable + slot + material chain + name + lore, selector layout + server
  list + ping cache TTL, launchpad cooldown / forward-power / up-power).

### Service Layer
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/HubService.java` — loads and
  persists `systems/hub.yml`; exposes typed getters with safe defaults and
  setters that save live; per-world whitelist with case-insensitive lookup.

### Items (Hotbar Kit)
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/item/HubItems.java` — central
  registry. Identifies items via Bukkit's `PersistentDataContainer` (1.14+,
  reflective) with an invisible color-code sentinel lore line as the
  1.8 – 1.13 fallback. Builders for all four default items.
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/kit/HubKitApplier.java` —
  applies the kit on join / respawn / world-change / mode-enable, gated
  per-item by `sfcore.hub.<itemId>` permissions.

### Launchpad
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/launchpad/LaunchpadCooldownManager.java`
  — per-player cooldown with a live action-bar countdown rendered above
  the hotbar (refreshes every 4 ticks; bar visible only while cooldown
  is active, then overwritten with empty string to avoid the vanilla
  fade-tail).

### Multi-server Messaging
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/messaging/BungeeMessenger.java`
  — registers the `BungeeCord` plugin-message channel (Velocity-compatible
  via the proxy's `bungee-plugin-message-channel: true` flag). Handles the
  `Connect` and `PlayerCount` subchannels with a TTL cache keyed by lower-
  case server id.

### GUIs
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/player/ServerSelectorMenu.java`
  — server selector inventory, dynamic rows from config, optional outline,
  per-server icon with auto-injected "Online: N" line populated from the
  live ping cache.
- `src/main/java/dev/sergeantfuzzy/sfcore/gui/player/ServerSelectorHolder.java`
  — marker holder + slot→serverId binding for the click listener.

### Listeners (`listener/player/`)
- `HubJoinListener.java` — kit application on join, respawn, and
  world-change.
- `HubItemUseListener.java` — right-click dispatcher; cross-version hide /
  show calls via deprecated single-arg overload (1.8.8 → modern).
- `HubItemProtectionListener.java` — cancels drops, container shuffles,
  and pickups for hub items while in a hub world with `kit.lock-hotbar`.
- `HubFishingListener.java` — IN_GROUND / CAUGHT_ENTITY → teleport to the
  hook landing site, range-guarded by `items.jump-rod.max-distance`.
- `HubLaunchpadListener.java` — `PlayerToggleFlightEvent` double-space
  trick: cancels the flight request, applies launch velocity from the
  player's look-direction, sets the cooldown + launched mark, plays the
  firework-launch sound (cross-version Sound enum probe).
- `HubFallDamageListener.java` — cancels fall damage for players with an
  active launched mark, then clears the mark on first ground touch.

### Listeners (`listener/menu/`)
- `ServerSelectorListener.java` — click → `BungeeMessenger.connect()`.

### Commands (`command/teleportation/`)
- `HubCommand.java` — handles `/hub` and `/lobby` (alias).
  - Default players (perm `sfcore.hub.use`): bare command only, tab
    completion returns empty list, subcommand input is silently dropped
    so the admin surface is never exposed.
  - Admins (perm `sfcore.hub.admin`): full subcommand set
    `on | off | toggle | reload | give | selector | menu | world add|remove|list|here`
    + tab completion of every branch.

### Admin GUI
- `gui/admin/AdminMenu.java` — new slot 43 "Hub / Lobby Controls" item
  (symmetric with Warp Manager at slot 37). Shows enabled / disabled
  status + hub-world count.
- `gui/admin/AdminSubMenu.java` — new `SubMenuType.HUB` and
  `openHubMenu(plugin, player)` + `handleHubMenuClick(...)` for the in-GUI
  toggle, add/remove current world, reload hub.yml, preview selector,
  re-apply kit, and launchpad / item info readouts.
- `listener/menu/MainMenuListener.java` — slot 43 click route to
  `AdminSubMenu.openHubMenu` and HUB sub-menu case in the click switch.

### Plugin Wiring
- `Main.java` — fields, init, getters, start/stop, register listeners,
  bind `/hub`, hook into `reloadPlugin()`.

### Permissions
- `plugin.yml`:
  - New command `/hub` (alias `/lobby`).
  - New nodes: `sfcore.hub.*` (op), `sfcore.hub.use` (true),
    `sfcore.hub.admin` (op), `sfcore.hub.selector` (true),
    `sfcore.hub.jumprod` (true), `sfcore.hub.vanishall` (true),
    `sfcore.hub.launchpad` (true).
  - `sfcore.*` updated to include `sfcore.hub.*: true`.

### Language
- `language/MessageDefaults.java`:
  - New section: `hub`.
  - New keys (EN + DE): `hub.teleport.teleporting`, `hub.teleport.missing`,
    `hub.vanishall.hidden`, `hub.vanishall.visible`,
    `hub.launchpad.cooldown`, `hub.selector.proxy-unavailable`,
    `hub.admin.usage`, `hub.admin.give-usage`, `hub.admin.enabled`,
    `hub.admin.disabled`, `hub.admin.already-on`, `hub.admin.already-off`,
    `hub.admin.reloaded`, `hub.admin.gave`, `hub.admin.target-offline`,
    `hub.admin.world.list`, `hub.admin.world.added`,
    `hub.admin.world.removed`, `hub.admin.world.already-listed`,
    `hub.admin.world.not-listed`.

## Bug Fixes & Hardening (2026-05-24 10:22 pm)

Follow-up pass addressing reported issues (server selector behaving like the
jump rod / "teleporting"; launchpad double-jump not working) and a sweep for
slot edge-cases and flight exploits.

### Slots — collision-proof kit assignment
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/kit/HubKitApplier.java`
  - Root cause of the "server selector teleports" report: the kit placed each
    item directly at its configured slot with **no uniqueness check**, so two
    items resolving to the same hotbar slot (a duplicate `items.*.slot`, or two
    values clamping to the same slot) silently overwrote each other. When the
    jump rod (`FISHING_ROD`) landed on the server-selector slot, the "selector"
    became a rod that casts and teleports instead of opening the GUI.
  - New `claimSlot(...)` guarantees every enabled+permitted item gets a **unique**
    hotbar slot: it keeps the requested slot when free, otherwise relocates to the
    next free slot and logs a warning naming the offending item + slot.
  - First-declared item wins its configured slot (order: selector, jump rod,
    vanish-all, launchpad), so the server selector always keeps its slot.

### Launchpad — reliability + free-fly exploit fix
- `src/main/java/dev/sergeantfuzzy/sfcore/listener/player/HubLaunchpadListener.java`
  - Previously, a launchpad-eligible player who double-tapped space while **not**
    holding the launchpad fell through without the flight request being
    cancelled — granting them real creative-style flight (free-fly exploit) and
    making the feature feel like "double-jump doesn't launch, it just flies".
  - The flight request is now cancelled for **every** survival/adventure
    double-tap by an eligible player; the launch still only fires while the
    launchpad is actually held (matches the lore "Hold and double-tap SPACE").
  - After cancelling we re-affirm `setAllowFlight(true)` + `setFlying(false)` so
    the client resyncs and the next double-tap reliably fires another event.
- `src/main/java/dev/sergeantfuzzy/sfcore/hub/kit/HubKitApplier.java`
  - Flight is granted only to launchpad-eligible players and set **after** the
    gamemode change (which resets ability flags); flight is only managed for
    survival/adventure players so creative/spectator flight is never clobbered.

### Disable cleanup — no leaked state / exploits
- `HubKitApplier#revokeFlightInHubWorlds()` — strips `allowFlight` from hub-world
  players so the launchpad grant can't be abused after hub-mode is turned off.
- `HubItemUseListener#resetVisibilityForAll()` — re-reveals everyone hidden by
  the vanish-all toggle (respecting staff vanish) so disabling doesn't strand
  players invisible.
- Wired into both disable surfaces:
  - `src/main/java/dev/sergeantfuzzy/sfcore/command/teleportation/HubCommand.java`
    (`/hub off`, `/hub toggle`).
  - `src/main/java/dev/sergeantfuzzy/sfcore/gui/admin/AdminSubMenu.java`
    (Admin GUI → Hub controls toggle slot).

### Verification
- Maven build (`./maven/bin/mvn -DskipTests package`) completes with no errors;
  both `SF-Core-1.0.0-SNAPSHOT.jar` (obfuscated) and `-unobf.jar` produced.

## Suggested Commit Message

```
Feature (hub): Add per-world hub / lobby system with kit, server selector, jump rod, vanish-all toggle, and launchpad

Fix (hub): Collision-proof hotbar slots, fix launchpad double-jump + free-fly exploit, clean up flight/vanish state on disable
```
