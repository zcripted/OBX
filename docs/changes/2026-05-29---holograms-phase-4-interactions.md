# Holograms — Phase 4: Custom packet layer + interactions

■ **Created:** 2026-05-29 2:30 pm
■ **Last Updated:** 2026-05-29 2:30 pm

Adds the optional Netty channel injector (custom packet path — no ProtocolLib
or PacketEvents) plus a raycast fallback so click / hover dispatch works on
every supported server. CText markup parser, dispatcher with cooldowns and
console/player execution, and an `interact` subcommand round out the
interaction surface.

## Categories

### Dependencies

* `pom.xml` — `io.netty:netty-all:4.1.97.Final` added with `<scope>provided
  </scope>`. Justification: every Paper / Spigot / Folia runtime bundles
  netty (it's how the server speaks the Minecraft protocol). Provided
  scope keeps the jar size unchanged. NOT a plugin dependency.

### Internal — packet layer

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/packet/PacketAvailability.java`
  — promoted from Phase 0 stub to a real probe. Checks for `CraftPlayer`
  via the server package, `io.netty.channel.Channel`, and
  `ChannelDuplexHandler`. Caches the result. Includes a 60-second
  rate-limited `noteFailure` logger so packet storms can't flood the log.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/packet/PacketChannelInjector.java`
  — installs / removes the SF-Core handler in each player's Netty
  pipeline. Walks the reflection chain
  `CraftPlayer → getHandle → ServerPlayer → connection → ServerGame
  PacketListenerImpl → connection → Connection → channel` with
  per-step candidate name lists (`connection`/`playerConnection`/`b`/`c`,
  `connection`/`networkManager`/`h`/`a`, `channel`/`k`/`m`/`f`).
  First success per class is cached so subsequent installs are
  reflection-light. Insert and remove run on the channel's event loop to
  avoid CME.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/packet/HologramPacketHandler.java`
  — inbound-only `ChannelDuplexHandler`. Checks each packet's
  `getSimpleName()` (no NMS imports) for `ServerboundInteractPacket` or
  `PacketPlayInUseEntity`; on match, decodes and dispatches via
  `SchedulerAdapter.runNow` so the click runs on the main thread.
  **Always calls `super.channelRead`** so vanilla behavior is 100%
  preserved.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/packet/InteractDecoder.java`
  — version-tolerant reflection-based decoder. Walks the packet class's
  declared fields once, caches the field handles, extracts the entity id,
  the action enum value, and the sneaking flag. Returns a small
  `Decoded` DTO so the dispatcher knows nothing about NMS.

### Internal — interaction layer

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/interact/CTextParser.java`
  — hand-rolled parser for the `<T>label</T><C>command</C><H>hover</H>`
  syntax. Returns a list of `Segment{label, command, hover}` so the
  renderer can re-assemble label text and the dispatcher can pick the
  click action.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/interact/InteractionDispatcher.java`
  — main-thread entry. Enforces per-(player, hologram) cooldowns sourced
  from `HologramSettings.interactionCooldownMs`. Runs commands either
  as the player (default) or as console (prefix the command with `!`).
  Supports `{player}` / `{uuid}` token substitution.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/interact/RaycastTargeter.java`
  — fallback when the packet layer is unavailable. Ticks at the
  configured rate (`interaction.raycast-hz`, default 4 Hz), raycasts
  every player's look direction against every interactable hologram,
  requires a 250 ms sustained hover before firing an `INTERACT` action.
* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/interact/ParticleFx.java`
  — `Player#spawnParticle` wrapper. Used by future polish for click /
  hover particles.

### Listeners + service wiring

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/listener/HologramConnectionListener.java`
  — Netty injector lifecycle. `PlayerJoinEvent` schedules an inject
  2 ticks later (Paper finishes handshake mid-event). `PlayerQuitEvent`
  ejects immediately.
* `HologramService` now constructs and starts a `RaycastTargeter` in
  `load()`, stops it in `reload()`/`shutdown()`, alongside the existing
  tick loop. The raycast targeter runs even when the packet layer is
  available — sustained hovers are useful for hover-particle / hover-color
  fast paths even when clicks come via packets.
* `Main` registers the new connection listener after the join/RP listeners.

### Commands

* `src/main/java/dev/sergeantfuzzy/sfcore/hologram/command/sub/InteractSub.java`
  — `/sfholo interact <id> <enable|disable|cooldown|width|height> [arg]`.
  Permission `sfcore.holo.interact`. Registered in `HologramCommand`.

### Language

* `MessageDefaults.java` — 3 new keys: `hologram.interact.cooldown`,
  `hologram.interact.no_permission`, `hologram.interact.disabled`. Both EN + DE.

### Docs

* `docs/information/COMMANDS+PERMISSIONS.md` — covered by the Phase 2
  block (the `sfcore.holo.interact` permission was already documented
  with `sfcore.holo.*`).

## Verification

* `mvn -DskipTests compile` — green.
* Manual checks (Paper 1.21):
  1. `/sfholo create test`, then `/sfholo addline test <T>Click me</T><C>say hi</C>`.
  2. `/sfholo interact test enable`.
  3. Left-clicking the hologram runs `/say hi` as the player. The
     packet handler decoded the click and dispatched on the main thread.
  4. `/sfholo interact test cooldown 2000` then click twice quickly — the
     second click prints the cooldown message and does not fire.
  5. `/sfholo addline test <T>As console</T><C>!give {player} diamond</C>`
     → left-click gives the player a diamond (executed from console).
* On a server where the reflection chain fails (1.7-style fork), the
  raycast targeter takes over automatically — verified by setting
  `PacketAvailability.reset()` then `probe(plugin)` then forcing
  `cached = Boolean.FALSE`. Hovering for >250 ms still triggers the same
  `InteractionDispatcher.dispatch`.

## Suggested Commit Message

```
SF-Core Holograms: Phase 4 — custom Netty packet layer + raycast + CText
```
