# Server State live wiring, virtual stations, styled /spawn details, Treecapitator pile

■ **Created:** 2026-05-27 9:18 pm (America/Detroit)

■ **Last Updated:** 2026-05-27 9:18 pm (America/Detroit)

---

## Summary

Four changes: (1) the admin **Server State** GUI now fully wires Lock/Unlock Server
(live whitelist readout + padlock icons) and live-refreshes all server-state items while
open; (2) five new op-only virtual-workstation commands (`/stonecut`, `/loom`,
`/grindstone`, `/cartography`, `/map`); (3) `/spawn details` renders in the boxed `/pl` /
`/obx info` report style; (4) the Treecapitator enchant now drops a felled tree's logs as a
single holographic-named pile.

## Categories

### GUIs — Server Control / Server State
- `src/main/java/dev/zcripted/obx/gui/admin/AdminSubMenu.java`
  - **Lock Server** (Server State, slot 16): prefixed with a 🔒 icon; tooltip now shows the
    **live** whitelist + join-lock state (ENABLED/DISABLED) instead of a static
    "after action" line. Click still enables the whitelist, force-enables join lock, and
    kicks non-ops.
  - **Unlock Server** (slot 19): prefixed with a 🔓 icon; live whitelist/join-lock tooltip.
    Click disables the whitelist and clears join lock so non-ops can join again.
  - Padlock glyphs are built from code points (`Character.toChars(0x1F512/0x1F513)`) so the
    source stays ASCII/encoding-safe.
  - All server-state items (the **Server State** overview in Server Control + Lock/Unlock +
    Player Access whitelist/join-lock/max-players + Performance redstone) were extracted into
    shared builder methods and a new `refresh(Holder)` method, so they always reflect the
    real `Bukkit.hasWhitelist()` / join-lock / redstone-frozen / `getMaxPlayers()` values.
  - `handleAction(...)` re-renders + `updateInventory()` after every click for instant
    feedback to the clicking admin.
- `src/main/java/dev/zcripted/obx/gui/admin/AdminMenuRefreshTask.java` (new)
  - 1-second repeating task that re-renders any open Server Control / Server State /
    Player Access / Performance submenu, so the live state updates when **another** admin
    (or a reload) changes it while the menu is open. Folia-safe: per-player refresh is
    dispatched onto each player's entity region; on Paper/Spigot it runs inline on the main
    thread the task already uses.
- `src/main/java/dev/zcripted/obx/Main.java`
  - Constructs/starts/cancels `AdminMenuRefreshTask` alongside the tablist task.

### Commands — virtual workstations (op-only)
- `src/main/java/dev/zcripted/obx/command/utility/VirtualStationCommand.java` (new)
  - One parameterized command (`Station` enum) backing `/stonecut` (`/chop`, `/cut`,
    `/scut`), `/loom`, `/grindstone` (`/gstone`, `/grind`, `/gs`), and `/cartography`
    (`/ctable`, `/cartograph`). Opens the matching `InventoryType` (STONECUTTER / LOOM /
    GRINDSTONE / CARTOGRAPHY), resolved by name via `InventoryType.valueOf` since those
    types post-date the 1.12.2 compile target. Falls back to an untitled inventory if a
    fork rejects a custom title, and to an "unsupported" message on servers < 1.14.
- `src/main/java/dev/zcripted/obx/command/utility/MapCommand.java` (new)
  - `/map` creates a map (`Bukkit.createMap`) centered on the player at `CLOSEST` scale and
    gives it to them — a "you are here" view of the surroundings. Binding the `MapView` to
    the item is version-aware: `MapMeta#setMapView` → `#setMapId` (reflective) →
    legacy map-id durability.
- `src/main/java/dev/zcripted/obx/Main.java` — imports + binds the 5 commands.
- `src/main/java/dev/zcripted/obx/gui/player/HelpGuiMenu.java` — maps all 5 commands
  (and aliases) to the **Utility** `/help` category.

### Spawn — styled /spawn details
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
  - `teleport.spawn.info` (used by `/spawn info` / `details` / `information` / `about`) is
    now a boxed report in the `/pl` and `/obx info` style: `▍ 𝗢𝗕𝗫 › Spawn Point` header,
    `─` divider rule, and indented `Label › value` rows for World / Position / Facing /
    Set by / Set at (EN + DE). No `SpawnCommand` code change needed — `LanguageManager.send`
    already renders list-valued keys line by line.

### Arcane Enchantments — Treecapitator
- `src/main/java/dev/zcripted/obx/enchant/listener/ToolEnchantListener.java`
  - When Treecapitator fells a tree, the log drops are merged by item type (`isSimilar`) and
    dropped as **single** `Item` entities (placed exactly, no scatter), each labelled with a
    floating holographic name `&eName &7x<qty>` (`setCustomName` + `setCustomNameVisible`)
    that hovers along with the item. New helpers: `mergeDrop`, `dropCombined`, `prettyName`;
    a `feltTree` flag gates the behavior so only actual tree-fells (not single non-log
    breaks with co-present smelter/fortune) combine.

### Config / Docs
- `src/main/resources/plugin.yml` — `commands:` + `permissions:` entries for the 5 new
  commands (all `default: op`) and their `obx.*` wildcard children.
- `docs/information/COMMANDS+PERMISSIONS.md` — 5 new rows in the Utility table.

## Notes / assumptions
- Lock/Unlock padlock icons use the emoji code points 🔒/🔓 (supplementary plane), consistent
  with the math-bold `𝗢𝗕𝗫` glyphs already used elsewhere; clients that don't render SMP
  glyphs will show a placeholder box but the colored "Lock Server" / "Unlock Server" text is
  unaffected.
- `/map` renders client-side as the player holds it and surrounding chunks load, so a fresh
  map fills in over the first moment rather than instantly. Documented in the class javadoc.
- Combined Treecapitator piles can carry an amount > 64 on a single Item entity; the client
  shows one entity and the full amount is granted on pickup (split into stacks).
- The virtual stonecutter/loom/grindstone/cartography menus rely on the server's own
  container logic for these `InventoryType`s (functional on Paper/Spigot 1.14+), mirroring
  the existing `/anvil` and `/smith` approach.

## Testing
- Maven build: `& ".\maven\bin\mvn.cmd" -DskipTests package` → exit 0; both jars rebuilt
  (obf ~655 KB, unobf ~947 KB). ProGuard `Note:` lines only (informational). Compile-verified.
- In-game checks to run: Server State Lock/Unlock (whitelist toggles + non-ops kicked +
  live tooltip), two admins with the menu open seeing each other's changes live, max-players
  ±, redstone toggle; `/stonecut` `/loom` `/grindstone` `/cartography` `/map`; `/spawn info`
  styled output; Treecapitator felling a tree → one named log pile per type.

## Suggested Commit Message
```
Feature: wire Server State Lock/Unlock (live status + icons) & live menu refresh; add /stonecut /loom /grindstone /cartography /map; style /spawn details; Treecapitator single named log pile
```
