# OBX Migration Manifest — Package-by-Layer → Package-by-Feature

**Created:** 2026-06-02 (America/Detroit)
**Source root:** `src/main/java/dev/zcripted/obx/`
**Total files:** 398 (393 main + 5 test)
**New base package:** `dev.zcripted.obx`

This manifest assigns every `.java` file to exactly one destination:
`core/<subpkg>`, `api/<subpkg>`, `util/<subpkg>`, or `feature/<name>/<subpkg>`.

---

## 1. Final Feature List

| Feature | One-line purpose |
|---|---|
| `feature/chat` | Server chat formatting + chat-management listener |
| `feature/economy` | Player balances, pay, sell/worth, baltop, eco admin (the *service* is API; impl + commands live here) |
| `feature/enchant` | Custom enchant system: registry, effects, scrolls, GUI, combat HUD, loot |
| `feature/hologram` | Holograms: backends, packets, anim, render, text, GUI, command tree, public facade API |
| `feature/hub` | Hub/lobby: hub items, launchpad, fall/fishing/use protection, server selector, BungeeCord transport |
| `feature/jail` | Jail/unjail, jail points, jail confinement listener |
| `feature/kit` | Kits (definition + service, applied by hub & command) |
| `feature/mail` | Offline mail, private `/msg`+reply inbox, ignore, social-spy, broadcast/me/staffchat |
| `feature/moderation` | Ban/mute/warn moderation service, status, banlist, Discord webhook |
| `feature/staff` | Vanish, freeze, invsee, staff menu, admin menu, staff-session tracking |
| `feature/nickname` | Nickname service + apply listener |
| `feature/scoreboard` | Sidebar scoreboard render/refresh |
| `feature/tablist` | Tablist header/footer + team nameplates |
| `feature/teleport` | tp/tpa/back/top/pos/home/spawn/sethome/tphere/tpall + warp menus & warp admin |
| `feature/warp` | Warp data + warp command tree + warp GUIs (split from teleport — large self-contained surface) |
| `feature/item` | Inventory/item utility commands (give, i, book, hat, more, skull, itemname/lore, unbreakable, repair, anvil, craft, smith, vanilla enchant table, research, map, virtualstation, clearinv, disposal, enderchest) |
| `feature/world` | time/day/weather + per-player time/weather, daylight fallback, redstone control, gamerule/server-control |
| `feature/playerinfo` | seen/firstseen/playtime/list/near/whois/realname/info + playtime service + join/leave broadcast data |
| `feature/playerstate` | god/fly/flyspeed/walkspeed/afk/heal/feed/vital/kill/butcher/smite/tree/spawnmob/spawner + their control services |

> Refinements vs. the proposed set: **warp** is broken out from **teleport** into its own feature (15+ GUI/menu files + warp service + warp admin/query commands form a cohesive self-contained surface). All other proposed features are kept.

---

## 2. File → Target Mapping

### core/  (cross-cutting framework)

**core/command** (dispatcher + abstract bases)
| Current | Target |
|---|---|
| `command/AbstractObxCommand.java` | `core/command/AbstractObxCommand.java` |
| `command/PlayerActionCommand.java` | `core/command/PlayerActionCommand.java` |
| `command/core/ObxCommand.java` | `core/command/ObxCommand.java` |
| `command/core/ObxAdminActions.java` | `core/command/ObxAdminActions.java` |
| `command/core/ObxDiagnosticsView.java` | `core/command/ObxDiagnosticsView.java` |
| `command/core/ObxHelpView.java` | `core/command/ObxHelpView.java` |
| `command/core/ObxModulesView.java` | `core/command/ObxModulesView.java` |
| `command/core/HelpGuiCommand.java` | `core/command/HelpGuiCommand.java` |
| `command/core/ListCommand.java` | `core/command/ListCommand.java` |

**core/gui** (generic menu plumbing + shared help/main GUI)
| Current | Target |
|---|---|
| `gui/MenuHolder.java` | `core/gui/MenuHolder.java` |
| `gui/shared/CustomHeadUtil.java` | `core/gui/CustomHeadUtil.java` |
| `gui/player/HelpGuiHolder.java` | `core/gui/help/HelpGuiHolder.java` |
| `gui/player/HelpGuiMenu.java` | `core/gui/help/HelpGuiMenu.java` |
| `gui/player/MainMenu.java` | `core/gui/main/MainMenu.java` |
| `gui/player/MainMenuHolder.java` | `core/gui/main/MainMenuHolder.java` |
| `listener/menu/HelpGuiListener.java` | `core/gui/help/HelpGuiListener.java` |
| `listener/menu/MainMenuListener.java` | `core/gui/main/MainMenuListener.java` |

**core/storage** (data layer)
| Current | Target |
|---|---|
| `storage/SqliteDataStore.java` | `core/storage/SqliteDataStore.java` |
| `storage/DataService.java` | `core/storage/DataService.java` |
| `storage/LocationSerializer.java` | `core/storage/LocationSerializer.java` |

**core/language** (locale)
| Current | Target |
|---|---|
| `language/LanguageFile.java` | `core/language/LanguageFile.java` |
| `language/LanguageManager.java` | `core/language/LanguageManager.java` |
| `language/LanguageRegistry.java` | `core/language/LanguageRegistry.java` |
| `language/MessageDefaults.java` | `core/language/MessageDefaults.java` |
| `command/language/LanguageCommand.java` | `core/language/LanguageCommand.java` |

**core/platform**
| Current | Target |
|---|---|
| `platform/PlatformInfo.java` | `core/platform/PlatformInfo.java` |
| `platform/scheduler/SchedulerAdapter.java` | `core/platform/scheduler/SchedulerAdapter.java` |
| `platform/bukkit/resourcepack/AutoResourcePackManager.java` | `core/platform/resourcepack/AutoResourcePackManager.java` |
| `platform/bukkit/resourcepack/ResourcePackListener.java` | `core/platform/resourcepack/ResourcePackListener.java` |

**core** (root plugin + cross-cutting motd/diagnostics)
| Current | Target |
|---|---|
| `Main.java` | `Main.java` (stays at root — plugin entry point) |
| `command/admin/PluginListCommand.java` | `core/command/PluginListCommand.java` |
| `command/admin/TpsCommand.java` | `core/diagnostics/TpsCommand.java` |
| `util/perf/TpsService.java` | `core/diagnostics/TpsService.java` |
| `storage/MotdService.java` | `core/motd/MotdService.java` |
| `listener/server/MotdPingListener.java` | `core/motd/MotdPingListener.java` |
| `listener/player/CommandOverrideListener.java` | `core/command/CommandOverrideListener.java` |

---

### api/  (stable public API)

| Current | Target | Note |
|---|---|---|
| `economy/EconomyService.java` | `api/economy/EconomyService.java` | central balance service other features + Vault hook depend on |
| `economy/VaultEconomyProvider.java` | `api/economy/VaultEconomyProvider.java` | exposes economy to Vault ecosystem |
| `hologram/api/HologramFacade.java` | `api/hologram/HologramFacade.java` | documented stable facade for integrators |
| `hologram/api/HologramInteractEvent.java` | `api/hologram/HologramInteractEvent.java` | public event |
| `hologram/api/HologramSpawnEvent.java` | `api/hologram/HologramSpawnEvent.java` | public event |

---

### util/  (stateless static helpers)

**util/message**
| Current | Target |
|---|---|
| `util/message/AdventureMessageUtil.java` | `util/message/AdventureMessageUtil.java` |
| `util/message/ConsoleLog.java` | `util/message/ConsoleLog.java` |
| `util/message/ConsoleTimestamp.java` | `util/message/ConsoleTimestamp.java` |
| `util/message/MotdMessageUtil.java` | `util/message/MotdMessageUtil.java` |

**util/text**
| Current | Target |
|---|---|
| `util/text/ComponentMessenger.java` | `util/text/ComponentMessenger.java` |
| `util/text/Placeholders.java` | `util/text/Placeholders.java` |

**util/update**
| Current | Target |
|---|---|
| `util/update/UpdateChecker.java` | `util/update/UpdateChecker.java` |

**util/perf**
| Current | Target |
|---|---|
| `util/perf/AsyncYamlSaver.java` | `util/perf/AsyncYamlSaver.java` |

**util/teleport** (stateless teleport math/helpers only)
| Current | Target |
|---|---|
| `util/teleport/WarpAccess.java` | `util/teleport/WarpAccess.java` |

> NOTE: `util/perf/PlaytimeService.java`, `util/perf/TpsService.java`, `util/teleport/TeleportManager.java`, `TeleportRequestService.java`, `TpaService.java`, and all `util/control/*` are **stateful services** → reassigned to their owning feature/core (see below), NOT kept in `util/`.

---

### feature/chat

| Current | Target |
|---|---|
| `chat/format/ChatFormatter.java` | `feature/chat/format/ChatFormatter.java` |
| `chat/service/ChatService.java` | `feature/chat/service/ChatService.java` |
| `chat/listener/ChatManagementListener.java` | `feature/chat/listener/ChatManagementListener.java` |

---

### feature/economy  (impl + commands; service/provider are in api/)

| Current | Target |
|---|---|
| `economy/WorthService.java` | `feature/economy/service/WorthService.java` |
| `command/economy/BalanceCommand.java` | `feature/economy/command/BalanceCommand.java` |
| `command/economy/BalTopCommand.java` | `feature/economy/command/BalTopCommand.java` |
| `command/economy/EcoCommand.java` | `feature/economy/command/EcoCommand.java` |
| `command/economy/PayCommand.java` | `feature/economy/command/PayCommand.java` |
| `command/economy/SellCommand.java` | `feature/economy/command/SellCommand.java` |
| `command/economy/SellAllCommand.java` | `feature/economy/command/SellAllCommand.java` |
| `command/economy/WorthCommand.java` | `feature/economy/command/WorthCommand.java` |

---

### feature/enchant

| Current | Target |
|---|---|
| `enchant/command/EnchantsBrowseCommand.java` | `feature/enchant/command/EnchantsBrowseCommand.java` |
| `enchant/command/ObxEnchantCommand.java` | `feature/enchant/command/ObxEnchantCommand.java` |
| `enchant/command/RecallCommand.java` | `feature/enchant/command/RecallCommand.java` |
| `enchant/command/SatchelCommand.java` | `feature/enchant/command/SatchelCommand.java` |
| `enchant/effect/BoundMovement.java` | `feature/enchant/effect/BoundMovement.java` |
| `enchant/effect/CombatState.java` | `feature/enchant/effect/CombatState.java` |
| `enchant/effect/EffectUtil.java` | `feature/enchant/effect/EffectUtil.java` |
| `enchant/effect/EnchantState.java` | `feature/enchant/effect/EnchantState.java` |
| `enchant/effect/EnchantTickTask.java` | `feature/enchant/effect/EnchantTickTask.java` |
| `enchant/effect/EndlessHunger.java` | `feature/enchant/effect/EndlessHunger.java` |
| `enchant/effect/SatchelCloseListener.java` | `feature/enchant/effect/SatchelCloseListener.java` |
| `enchant/gui/EnchantAdminMenu.java` | `feature/enchant/gui/EnchantAdminMenu.java` |
| `enchant/gui/EnchantMenuHolder.java` | `feature/enchant/gui/EnchantMenuHolder.java` |
| `enchant/gui/EnchantMenuListener.java` | `feature/enchant/gui/EnchantMenuListener.java` |
| `enchant/item/EnchantGuideBook.java` | `feature/enchant/item/EnchantGuideBook.java` |
| `enchant/item/EnchantItems.java` | `feature/enchant/item/EnchantItems.java` |
| `enchant/item/ScrollKind.java` | `feature/enchant/item/ScrollKind.java` |
| `enchant/listener/CombatEnchantListener.java` | `feature/enchant/listener/CombatEnchantListener.java` |
| `enchant/listener/CombatSupport.java` | `feature/enchant/listener/CombatSupport.java` |
| `enchant/listener/CursedEnchantListener.java` | `feature/enchant/listener/CursedEnchantListener.java` |
| `enchant/listener/DefenseEnchantListener.java` | `feature/enchant/listener/DefenseEnchantListener.java` |
| `enchant/listener/EnchantBookUseListener.java` | `feature/enchant/listener/EnchantBookUseListener.java` |
| `enchant/listener/EnchantLoreListener.java` | `feature/enchant/listener/EnchantLoreListener.java` |
| `enchant/listener/FarmingEnchantListener.java` | `feature/enchant/listener/FarmingEnchantListener.java` |
| `enchant/listener/MovementEnchantListener.java` | `feature/enchant/listener/MovementEnchantListener.java` |
| `enchant/listener/OnDeathListener.java` | `feature/enchant/listener/OnDeathListener.java` |
| `enchant/listener/OnHitDamageListener.java` | `feature/enchant/listener/OnHitDamageListener.java` |
| `enchant/listener/OnHitProcListener.java` | `feature/enchant/listener/OnHitProcListener.java` |
| `enchant/listener/OnKillListener.java` | `feature/enchant/listener/OnKillListener.java` |
| `enchant/listener/RangedListener.java` | `feature/enchant/listener/RangedListener.java` |
| `enchant/listener/ReactiveSpecialsListener.java` | `feature/enchant/listener/ReactiveSpecialsListener.java` |
| `enchant/listener/ToolEnchantListener.java` | `feature/enchant/listener/ToolEnchantListener.java` |
| `enchant/listener/UtilityEnchantListener.java` | `feature/enchant/listener/UtilityEnchantListener.java` |
| `enchant/loot/EnchantLoot.java` | `feature/enchant/loot/EnchantLoot.java` |
| `enchant/model/CustomEnchant.java` | `feature/enchant/model/CustomEnchant.java` |
| `enchant/model/EnchantCategory.java` | `feature/enchant/model/EnchantCategory.java` |
| `enchant/model/EnchantRarity.java` | `feature/enchant/model/EnchantRarity.java` |
| `enchant/model/ItemTag.java` | `feature/enchant/model/ItemTag.java` |
| `enchant/registry/EnchantRegistry.java` | `feature/enchant/registry/EnchantRegistry.java` |
| `enchant/scroll/AnvilEnchantListener.java` | `feature/enchant/scroll/AnvilEnchantListener.java` |
| `enchant/scroll/ScrollApplyService.java` | `feature/enchant/scroll/ScrollApplyService.java` |
| `enchant/scroll/ScrollDragListener.java` | `feature/enchant/scroll/ScrollDragListener.java` |
| `enchant/scroll/ScrollResult.java` | `feature/enchant/scroll/ScrollResult.java` |
| `enchant/service/ApplyResult.java` | `feature/enchant/service/ApplyResult.java` |
| `enchant/service/ApplyStatus.java` | `feature/enchant/service/ApplyStatus.java` |
| `enchant/service/CombatHudService.java` | `feature/enchant/service/CombatHudService.java` |
| `enchant/service/CombatParticleService.java` | `feature/enchant/service/CombatParticleService.java` |
| `enchant/service/CombatPrefs.java` | `feature/enchant/service/CombatPrefs.java` |
| `enchant/service/CombatSettings.java` | `feature/enchant/service/CombatSettings.java` |
| `enchant/service/EnchantFeedback.java` | `feature/enchant/service/EnchantFeedback.java` |
| `enchant/service/EnchantService.java` | `feature/enchant/service/EnchantService.java` |
| `enchant/service/HoloFXService.java` | `feature/enchant/service/HoloFXService.java` |
| `enchant/service/ReactiveSpecialsService.java` | `feature/enchant/service/ReactiveSpecialsService.java` |
| `enchant/service/ScrollSettings.java` | `feature/enchant/service/ScrollSettings.java` |
| `enchant/storage/EnchantStorage.java` | `feature/enchant/storage/EnchantStorage.java` |
| `enchant/storage/VanillaEnchantNames.java` | `feature/enchant/storage/VanillaEnchantNames.java` |
| `enchant/storage/WeaponAttributes.java` | `feature/enchant/storage/WeaponAttributes.java` |
| `enchant/util/EnchantHover.java` | `feature/enchant/util/EnchantHover.java` |
| `enchant/util/Glow.java` | `feature/enchant/util/Glow.java` |
| `enchant/util/Particles.java` | `feature/enchant/util/Particles.java` |
| `enchant/util/Potions.java` | `feature/enchant/util/Potions.java` |
| `enchant/util/SoundPalette.java` | `feature/enchant/util/SoundPalette.java` |
| `enchant/util/Sounds.java` | `feature/enchant/util/Sounds.java` |

> The `enchant/util/*` and `enchant/effect/*` helpers stay inside the feature (they are enchant-specific, not global static utils). Per CLAUDE memory note, the enchant module is excluded from the rebrand recolor but is still a first-class feature here.

---

### feature/hologram  (facade/events moved to api/ above)

| Current | Target |
|---|---|
| `hologram/HoloMessages.java` | `feature/hologram/HoloMessages.java` |
| `hologram/HologramTag.java` | `feature/hologram/HologramTag.java` |
| `hologram/anim/Animation.java` | `feature/hologram/anim/Animation.java` |
| `hologram/anim/AnimationConfig.java` | `feature/hologram/anim/AnimationConfig.java` |
| `hologram/anim/AnimationRegistry.java` | `feature/hologram/anim/AnimationRegistry.java` |
| `hologram/anim/FadeAnimation.java` | `feature/hologram/anim/FadeAnimation.java` |
| `hologram/anim/IconBobAnimation.java` | `feature/hologram/anim/IconBobAnimation.java` |
| `hologram/anim/RotateAnimation.java` | `feature/hologram/anim/RotateAnimation.java` |
| `hologram/backend/ArmorStandBackend.java` | `feature/hologram/backend/ArmorStandBackend.java` |
| `hologram/backend/BackendSelector.java` | `feature/hologram/backend/BackendSelector.java` |
| `hologram/backend/DisplayEntityBackend.java` | `feature/hologram/backend/DisplayEntityBackend.java` |
| `hologram/backend/HologramBackend.java` | `feature/hologram/backend/HologramBackend.java` |
| `hologram/command/HoloContext.java` | `feature/hologram/command/HoloContext.java` |
| `hologram/command/HoloSubCommand.java` | `feature/hologram/command/HoloSubCommand.java` |
| `hologram/command/HologramCommand.java` | `feature/hologram/command/HologramCommand.java` |
| `hologram/command/sub/AddLineSub.java` | `feature/hologram/command/sub/AddLineSub.java` |
| `hologram/command/sub/AimGuiSub.java` | `feature/hologram/command/sub/AimGuiSub.java` |
| `hologram/command/sub/AlignmentSub.java` | `feature/hologram/command/sub/AlignmentSub.java` |
| `hologram/command/sub/AnimSub.java` | `feature/hologram/command/sub/AnimSub.java` |
| `hologram/command/sub/BackgroundSub.java` | `feature/hologram/command/sub/BackgroundSub.java` |
| `hologram/command/sub/BillboardSub.java` | `feature/hologram/command/sub/BillboardSub.java` |
| `hologram/command/sub/BoardSub.java` | `feature/hologram/command/sub/BoardSub.java` |
| `hologram/command/sub/CopySub.java` | `feature/hologram/command/sub/CopySub.java` |
| `hologram/command/sub/CreateSub.java` | `feature/hologram/command/sub/CreateSub.java` |
| `hologram/command/sub/DeleteSub.java` | `feature/hologram/command/sub/DeleteSub.java` |
| `hologram/command/sub/DisableSub.java` | `feature/hologram/command/sub/DisableSub.java` |
| `hologram/command/sub/DoubleSidedSub.java` | `feature/hologram/command/sub/DoubleSidedSub.java` |
| `hologram/command/sub/EnableSub.java` | `feature/hologram/command/sub/EnableSub.java` |
| `hologram/command/sub/GuiSub.java` | `feature/hologram/command/sub/GuiSub.java` |
| `hologram/command/sub/HideSub.java` | `feature/hologram/command/sub/HideSub.java` |
| `hologram/command/sub/InfoSub.java` | `feature/hologram/command/sub/InfoSub.java` |
| `hologram/command/sub/InsertAfterSub.java` | `feature/hologram/command/sub/InsertAfterSub.java` |
| `hologram/command/sub/InsertBeforeSub.java` | `feature/hologram/command/sub/InsertBeforeSub.java` |
| `hologram/command/sub/InteractSub.java` | `feature/hologram/command/sub/InteractSub.java` |
| `hologram/command/sub/LineSub.java` | `feature/hologram/command/sub/LineSub.java` |
| `hologram/command/sub/ListSub.java` | `feature/hologram/command/sub/ListSub.java` |
| `hologram/command/sub/MoveHereSub.java` | `feature/hologram/command/sub/MoveHereSub.java` |
| `hologram/command/sub/MoveSub.java` | `feature/hologram/command/sub/MoveSub.java` |
| `hologram/command/sub/PageSub.java` | `feature/hologram/command/sub/PageSub.java` |
| `hologram/command/sub/ReloadSub.java` | `feature/hologram/command/sub/ReloadSub.java` |
| `hologram/command/sub/RemoveLineSub.java` | `feature/hologram/command/sub/RemoveLineSub.java` |
| `hologram/command/sub/ScaleSub.java` | `feature/hologram/command/sub/ScaleSub.java` |
| `hologram/command/sub/SeethroughSub.java` | `feature/hologram/command/sub/SeethroughSub.java` |
| `hologram/command/sub/SetLineSub.java` | `feature/hologram/command/sub/SetLineSub.java` |
| `hologram/command/sub/ShadowSub.java` | `feature/hologram/command/sub/ShadowSub.java` |
| `hologram/command/sub/ShowRangeSub.java` | `feature/hologram/command/sub/ShowRangeSub.java` |
| `hologram/command/sub/ShowSub.java` | `feature/hologram/command/sub/ShowSub.java` |
| `hologram/command/sub/SwapLineSub.java` | `feature/hologram/command/sub/SwapLineSub.java` |
| `hologram/command/sub/TextAlphaSub.java` | `feature/hologram/command/sub/TextAlphaSub.java` |
| `hologram/command/sub/TpSub.java` | `feature/hologram/command/sub/TpSub.java` |
| `hologram/command/sub/UpdateRangeSub.java` | `feature/hologram/command/sub/UpdateRangeSub.java` |
| `hologram/command/sub/ViewSub.java` | `feature/hologram/command/sub/ViewSub.java` |
| `hologram/gui/HologramEditorMenu.java` | `feature/hologram/gui/HologramEditorMenu.java` |
| `hologram/interact/CTextParser.java` | `feature/hologram/interact/CTextParser.java` |
| `hologram/interact/InteractionDispatcher.java` | `feature/hologram/interact/InteractionDispatcher.java` |
| `hologram/interact/ParticleFx.java` | `feature/hologram/interact/ParticleFx.java` |
| `hologram/interact/RaycastTargeter.java` | `feature/hologram/interact/RaycastTargeter.java` |
| `hologram/listener/HologramConnectionListener.java` | `feature/hologram/listener/HologramConnectionListener.java` |
| `hologram/listener/HologramJoinListener.java` | `feature/hologram/listener/HologramJoinListener.java` |
| `hologram/listener/HologramResourcePackListener.java` | `feature/hologram/listener/HologramResourcePackListener.java` |
| `hologram/model/Hologram.java` | `feature/hologram/model/Hologram.java` |
| `hologram/model/HologramId.java` | `feature/hologram/model/HologramId.java` |
| `hologram/model/HologramLine.java` | `feature/hologram/model/HologramLine.java` |
| `hologram/model/HologramSettings.java` | `feature/hologram/model/HologramSettings.java` |
| `hologram/packet/HologramPacketHandler.java` | `feature/hologram/packet/HologramPacketHandler.java` |
| `hologram/packet/InteractDecoder.java` | `feature/hologram/packet/InteractDecoder.java` |
| `hologram/packet/PacketAvailability.java` | `feature/hologram/packet/PacketAvailability.java` |
| `hologram/packet/PacketChannelInjector.java` | `feature/hologram/packet/PacketChannelInjector.java` |
| `hologram/render/HologramRenderer.java` | `feature/hologram/render/HologramRenderer.java` |
| `hologram/render/TickLoop.java` | `feature/hologram/render/TickLoop.java` |
| `hologram/render/ViewerTracker.java` | `feature/hologram/render/ViewerTracker.java` |
| `hologram/render/WallOcclusionCheck.java` | `feature/hologram/render/WallOcclusionCheck.java` |
| `hologram/service/HologramRegistry.java` | `feature/hologram/service/HologramRegistry.java` |
| `hologram/service/HologramService.java` | `feature/hologram/service/HologramService.java` |
| `hologram/storage/HologramSerializer.java` | `feature/hologram/storage/HologramSerializer.java` |
| `hologram/storage/HologramStorage.java` | `feature/hologram/storage/HologramStorage.java` |
| `hologram/storage/YamlHologramStorage.java` | `feature/hologram/storage/YamlHologramStorage.java` |
| `hologram/text/FillerExpander.java` | `feature/hologram/text/FillerExpander.java` |
| `hologram/text/HologramTextResolver.java` | `feature/hologram/text/HologramTextResolver.java` |
| `hologram/text/MiniMessageBridge.java` | `feature/hologram/text/MiniMessageBridge.java` |
| `hologram/text/PageState.java` | `feature/hologram/text/PageState.java` |
| `hologram/text/PlaceholderBridge.java` | `feature/hologram/text/PlaceholderBridge.java` |

---

### feature/hub

| Current | Target |
|---|---|
| `hub/HubService.java` | `feature/hub/service/HubService.java` |
| `hub/item/HubItems.java` | `feature/hub/item/HubItems.java` |
| `hub/kit/HubKitApplier.java` | `feature/hub/kit/HubKitApplier.java` |
| `hub/launchpad/LaunchpadCooldownManager.java` | `feature/hub/launchpad/LaunchpadCooldownManager.java` |
| `hub/messaging/BungeeMessenger.java` | `feature/hub/messaging/BungeeMessenger.java` |
| `command/teleportation/HubCommand.java` | `feature/hub/command/HubCommand.java` |
| `gui/player/ServerSelectorHolder.java` | `feature/hub/gui/ServerSelectorHolder.java` |
| `gui/player/ServerSelectorMenu.java` | `feature/hub/gui/ServerSelectorMenu.java` |
| `listener/menu/ServerSelectorListener.java` | `feature/hub/gui/ServerSelectorListener.java` |
| `listener/player/HubFallDamageListener.java` | `feature/hub/listener/HubFallDamageListener.java` |
| `listener/player/HubFishingListener.java` | `feature/hub/listener/HubFishingListener.java` |
| `listener/player/HubItemProtectionListener.java` | `feature/hub/listener/HubItemProtectionListener.java` |
| `listener/player/HubItemUseListener.java` | `feature/hub/listener/HubItemUseListener.java` |
| `listener/player/HubJoinListener.java` | `feature/hub/listener/HubJoinListener.java` |
| `listener/player/HubLaunchpadListener.java` | `feature/hub/listener/HubLaunchpadListener.java` |

> `BungeeMessenger` is hub-specific (imports `HubService`, used for hub server transport), so it stays in the hub feature rather than core, despite the proposal noting "if generic". It is not generic.

---

### feature/jail

| Current | Target |
|---|---|
| `jail/Jail.java` | `feature/jail/model/Jail.java` |
| `jail/JailService.java` | `feature/jail/service/JailService.java` |
| `jail/JailListener.java` | `feature/jail/listener/JailListener.java` |
| `command/admin/JailCommand.java` | `feature/jail/command/JailCommand.java` |
| `command/admin/UnjailCommand.java` | `feature/jail/command/UnjailCommand.java` |
| `command/admin/JailsCommand.java` | `feature/jail/command/JailsCommand.java` |
| `command/admin/JailTimeCommand.java` | `feature/jail/command/JailTimeCommand.java` |
| `command/admin/SetJailCommand.java` | `feature/jail/command/SetJailCommand.java` |
| `command/admin/DelJailCommand.java` | `feature/jail/command/DelJailCommand.java` |

---

### feature/kit

| Current | Target |
|---|---|
| `kit/Kit.java` | `feature/kit/model/Kit.java` |
| `kit/KitService.java` | `feature/kit/service/KitService.java` |
| `command/utility/KitCommand.java` | `feature/kit/command/KitCommand.java` |

---

### feature/mail  (private msg + mail + ignore + socialspy + broadcast/me/staffchat)

| Current | Target |
|---|---|
| `message/MessageService.java` | `feature/mail/pm/PrivateMessageService.java` |
| `message/MessageStore.java` | `feature/mail/pm/MessageStore.java` |
| `message/InboxMessage.java` | `feature/mail/pm/InboxMessage.java` |
| `message/InboxMenu.java` | `feature/mail/pm/gui/InboxMenu.java` |
| `message/InboxMenuHolder.java` | `feature/mail/pm/gui/InboxMenuHolder.java` |
| `listener/menu/InboxMenuListener.java` | `feature/mail/pm/gui/InboxMenuListener.java` |
| `command/message/MsgCommand.java` | `feature/mail/command/MsgCommand.java` |
| `command/message/ReplyCommand.java` | `feature/mail/command/ReplyCommand.java` |
| `command/message/InboxCommand.java` | `feature/mail/command/InboxCommand.java` |
| `messaging/MessageService.java` | `feature/mail/mail/MailService.java` |
| `command/messaging/MailCommand.java` | `feature/mail/command/MailCommand.java` |
| `command/messaging/IgnoreCommand.java` | `feature/mail/command/IgnoreCommand.java` |
| `command/messaging/SocialSpyCommand.java` | `feature/mail/command/SocialSpyCommand.java` |
| `command/messaging/BroadcastCommand.java` | `feature/mail/command/BroadcastCommand.java` |
| `command/messaging/MeCommand.java` | `feature/mail/command/MeCommand.java` |
| `command/messaging/StaffChatCommand.java` | `feature/mail/command/StaffChatCommand.java` |

> The TWO `MessageService` classes are disambiguated on move: `message/MessageService` (live private `/msg` + reply + inbox) → `feature/mail/pm/PrivateMessageService`; `messaging/MessageService` (persistent offline mailbox/ignore) → `feature/mail/mail/MailService`. Broadcast/Me/StaffChat are lightweight chat-dispatch commands grouped here with the rest of the `messaging/` command bucket; StaffChat could alternatively live under `feature/staff`, but it ships with the messaging command family and only sends a permission-gated chat broadcast.

---

### feature/moderation

| Current | Target |
|---|---|
| `moderation/ModerationService.java` | `feature/moderation/service/ModerationService.java` |
| `command/moderation/ModerationCommand.java` | `feature/moderation/command/ModerationCommand.java` |
| `command/moderation/ModerationStatusCommand.java` | `feature/moderation/command/ModerationStatusCommand.java` |
| `command/moderation/BanListCommand.java` | `feature/moderation/command/BanListCommand.java` |

---

### feature/staff  (vanish/freeze/invsee/staff menu/admin menu)

| Current | Target |
|---|---|
| `util/control/VanishManager.java` | `feature/staff/service/VanishManager.java` |
| `util/control/FreezeService.java` | `feature/staff/service/FreezeService.java` |
| `util/control/StaffSessionTracker.java` | `feature/staff/service/StaffSessionTracker.java` |
| `command/admin/VanishCommand.java` | `feature/staff/command/VanishCommand.java` |
| `command/admin/FreezeCommand.java` | `feature/staff/command/FreezeCommand.java` |
| `command/admin/InvSeeCommand.java` | `feature/staff/command/InvSeeCommand.java` |
| `command/admin/StaffCommand.java` | `feature/staff/command/StaffCommand.java` |
| `gui/admin/AdminMenu.java` | `feature/staff/gui/AdminMenu.java` |
| `gui/admin/AdminMenuHolder.java` | `feature/staff/gui/AdminMenuHolder.java` |
| `gui/admin/AdminMenuRefreshTask.java` | `feature/staff/gui/AdminMenuRefreshTask.java` |
| `gui/admin/AdminMenuRender.java` | `feature/staff/gui/AdminMenuRender.java` |
| `gui/admin/AdminSubMenu.java` | `feature/staff/gui/AdminSubMenu.java` |
| `gui/admin/InvSeeMenu.java` | `feature/staff/gui/InvSeeMenu.java` |
| `gui/admin/InvSeeMenuHolder.java` | `feature/staff/gui/InvSeeMenuHolder.java` |
| `gui/admin/InvSeeMenuManager.java` | `feature/staff/gui/InvSeeMenuManager.java` |
| `gui/admin/StaffActionMenu.java` | `feature/staff/gui/StaffActionMenu.java` |
| `gui/admin/StaffActionMenuHolder.java` | `feature/staff/gui/StaffActionMenuHolder.java` |
| `gui/admin/StaffMenu.java` | `feature/staff/gui/StaffMenu.java` |
| `gui/admin/StaffMenuHolder.java` | `feature/staff/gui/StaffMenuHolder.java` |
| `gui/admin/StaffMenuInputManager.java` | `feature/staff/gui/StaffMenuInputManager.java` |
| `listener/menu/InvSeeMenuListener.java` | `feature/staff/gui/InvSeeMenuListener.java` |
| `listener/menu/StaffMenuListener.java` | `feature/staff/gui/StaffMenuListener.java` |
| `listener/chat/StaffMenuInputListener.java` | `feature/staff/gui/StaffMenuInputListener.java` |

---

### feature/nickname

| Current | Target |
|---|---|
| `nickname/NicknameService.java` | `feature/nickname/service/NicknameService.java` |
| `nickname/NicknameApplyListener.java` | `feature/nickname/listener/NicknameApplyListener.java` |
| `command/utility/NickCommand.java` | `feature/nickname/command/NickCommand.java` |

---

### feature/scoreboard

| Current | Target |
|---|---|
| `scoreboard/format/ScoreboardRenderer.java` | `feature/scoreboard/format/ScoreboardRenderer.java` |
| `scoreboard/service/ScoreboardService.java` | `feature/scoreboard/service/ScoreboardService.java` |
| `scoreboard/scheduler/ScoreboardRefreshTask.java` | `feature/scoreboard/scheduler/ScoreboardRefreshTask.java` |
| `scoreboard/listener/ScoreboardJoinListener.java` | `feature/scoreboard/listener/ScoreboardJoinListener.java` |

---

### feature/tablist

| Current | Target |
|---|---|
| `tablist/format/TablistRenderer.java` | `feature/tablist/format/TablistRenderer.java` |
| `tablist/format/TablistTeams.java` | `feature/tablist/format/TablistTeams.java` |
| `tablist/service/TablistService.java` | `feature/tablist/service/TablistService.java` |
| `tablist/scheduler/TablistRefreshTask.java` | `feature/tablist/scheduler/TablistRefreshTask.java` |
| `tablist/listener/TablistJoinListener.java` | `feature/tablist/listener/TablistJoinListener.java` |

---

### feature/teleport  (tp/tpa/back/top/pos/home/spawn)

| Current | Target |
|---|---|
| `util/teleport/TeleportManager.java` | `feature/teleport/service/TeleportManager.java` |
| `util/teleport/TeleportRequestService.java` | `feature/teleport/service/TeleportRequestService.java` |
| `util/teleport/TpaService.java` | `feature/teleport/service/TpaService.java` |
| `command/teleportation/TeleportCommand.java` | `feature/teleport/command/TeleportCommand.java` |
| `command/teleportation/TpHereCommand.java` | `feature/teleport/command/TpHereCommand.java` |
| `command/teleportation/TpAllCommand.java` | `feature/teleport/command/TpAllCommand.java` |
| `command/teleportation/TpPosCommand.java` | `feature/teleport/command/TpPosCommand.java` |
| `command/teleportation/TpaCommand.java` | `feature/teleport/command/TpaCommand.java` |
| `command/teleportation/TpAcceptCommand.java` | `feature/teleport/command/TpAcceptCommand.java` |
| `command/teleportation/TpDenyCommand.java` | `feature/teleport/command/TpDenyCommand.java` |
| `command/teleportation/TpCancelCommand.java` | `feature/teleport/command/TpCancelCommand.java` |
| `command/teleportation/TpToggleCommand.java` | `feature/teleport/command/TpToggleCommand.java` |
| `command/teleportation/BackCommand.java` | `feature/teleport/command/BackCommand.java` |
| `command/teleportation/TopCommand.java` | `feature/teleport/command/TopCommand.java` |
| `command/teleportation/PositionCommand.java` | `feature/teleport/command/PositionCommand.java` |
| `command/teleportation/HomeCommand.java` | `feature/teleport/command/HomeCommand.java` |
| `command/teleportation/HomesCommand.java` | `feature/teleport/command/HomesCommand.java` |
| `command/teleportation/SetHomeCommand.java` | `feature/teleport/command/SetHomeCommand.java` |
| `command/teleportation/DelHomeCommand.java` | `feature/teleport/command/DelHomeCommand.java` |
| `command/teleportation/SpawnCommand.java` | `feature/teleport/command/SpawnCommand.java` |
| `command/teleportation/SetSpawnCommand.java` | `feature/teleport/command/SetSpawnCommand.java` |
| `listener/teleport/BackListener.java` | `feature/teleport/listener/BackListener.java` |

> Home/spawn data persistence lives in `core/storage/DataService` (shared SQLite). The teleport commands depend on it but it is core infrastructure, not teleport-owned.

---

### feature/warp  (split out from teleport)

| Current | Target |
|---|---|
| `storage/WarpService.java` | `feature/warp/service/WarpService.java` |
| `command/teleportation/WarpCommand.java` | `feature/warp/command/WarpCommand.java` |
| `command/teleportation/WarpAdminCommands.java` | `feature/warp/command/WarpAdminCommands.java` |
| `command/teleportation/WarpQueryCommands.java` | `feature/warp/command/WarpQueryCommands.java` |
| `gui/player/WarpCategoriesMenu.java` | `feature/warp/gui/WarpCategoriesMenu.java` |
| `gui/player/WarpConfirmDeleteMenu.java` | `feature/warp/gui/WarpConfirmDeleteMenu.java` |
| `gui/player/WarpConfirmMoveMenu.java` | `feature/warp/gui/WarpConfirmMoveMenu.java` |
| `gui/player/WarpConfirmOverwriteMenu.java` | `feature/warp/gui/WarpConfirmOverwriteMenu.java` |
| `gui/player/WarpCreateInputFlow.java` | `feature/warp/gui/WarpCreateInputFlow.java` |
| `gui/player/WarpDetailsMenu.java` | `feature/warp/gui/WarpDetailsMenu.java` |
| `gui/player/WarpIconPickerMenu.java` | `feature/warp/gui/WarpIconPickerMenu.java` |
| `gui/player/WarpMainMenu.java` | `feature/warp/gui/WarpMainMenu.java` |
| `gui/player/WarpManageMenu.java` | `feature/warp/gui/WarpManageMenu.java` |
| `gui/player/WarpMenu.java` | `feature/warp/gui/WarpMenu.java` |
| `gui/player/WarpMenuHolder.java` | `feature/warp/gui/WarpMenuHolder.java` |
| `gui/player/WarpMenuInputManager.java` | `feature/warp/gui/WarpMenuInputManager.java` |
| `gui/player/WarpRenameInputFlow.java` | `feature/warp/gui/WarpRenameInputFlow.java` |
| `gui/player/WarpVisibilityMenu.java` | `feature/warp/gui/WarpVisibilityMenu.java` |
| `gui/shared/WarpMenuStyling.java` | `feature/warp/gui/WarpMenuStyling.java` |
| `listener/menu/WarpMenuListener.java` | `feature/warp/gui/WarpMenuListener.java` |
| `listener/chat/WarpMenuInputListener.java` | `feature/warp/gui/WarpMenuInputListener.java` |

> `util/teleport/WarpAccess.java` is a stateless static helper (visibility/permission check) → kept in `util/teleport/` rather than the warp feature.

---

### feature/item  (inventory/item utility commands)

| Current | Target |
|---|---|
| `command/utility/GiveCommand.java` | `feature/item/command/GiveCommand.java` |
| `command/utility/ItemCommand.java` | `feature/item/command/ItemCommand.java` |
| `command/utility/BookCommand.java` | `feature/item/command/BookCommand.java` |
| `command/utility/HatCommand.java` | `feature/item/command/HatCommand.java` |
| `command/utility/MoreCommand.java` | `feature/item/command/MoreCommand.java` |
| `command/utility/SkullCommand.java` | `feature/item/command/SkullCommand.java` |
| `command/utility/ItemNameCommand.java` | `feature/item/command/ItemNameCommand.java` |
| `command/utility/ItemLoreCommand.java` | `feature/item/command/ItemLoreCommand.java` |
| `command/utility/UnbreakableCommand.java` | `feature/item/command/UnbreakableCommand.java` |
| `command/utility/RepairCommand.java` | `feature/item/command/RepairCommand.java` |
| `command/utility/AnvilCommand.java` | `feature/item/command/AnvilCommand.java` |
| `command/utility/CraftCommand.java` | `feature/item/command/CraftCommand.java` |
| `command/utility/SmithCommand.java` | `feature/item/command/SmithCommand.java` |
| `command/utility/EnchantCommand.java` | `feature/item/command/EnchantCommand.java` |
| `command/utility/ResearchCommand.java` | `feature/item/command/ResearchCommand.java` |
| `command/utility/MapCommand.java` | `feature/item/command/MapCommand.java` |
| `command/utility/VirtualStationCommand.java` | `feature/item/command/VirtualStationCommand.java` |
| `command/utility/ClearInvCommand.java` | `feature/item/command/ClearInvCommand.java` |
| `command/utility/DisposalCommand.java` | `feature/item/command/DisposalCommand.java` |
| `command/utility/EnderchestCommand.java` | `feature/item/command/EnderchestCommand.java` |

> `EnchantCommand` here is the **vanilla** enchanting-table opener (`Player.openEnchanting`), distinct from the custom-enchant feature. It is an inventory/station command → `feature/item`. `GamemodeCommand` is a player-state command → see `feature/playerstate`.

---

### feature/world  (time/day/weather + per-player + redstone + server control)

| Current | Target |
|---|---|
| `command/world/TimeCommand.java` | `feature/world/command/TimeCommand.java` |
| `command/world/DayCommand.java` | `feature/world/command/DayCommand.java` |
| `command/world/WeatherCommand.java` | `feature/world/command/WeatherCommand.java` |
| `command/world/PTimeCommand.java` | `feature/world/command/PTimeCommand.java` |
| `command/world/PWeatherCommand.java` | `feature/world/command/PWeatherCommand.java` |
| `util/control/PerPlayerTimeService.java` | `feature/world/service/PerPlayerTimeService.java` |
| `util/control/DaylightCycleFallback.java` | `feature/world/service/DaylightCycleFallback.java` |
| `util/control/ServerControlActions.java` | `feature/world/service/ServerControlActions.java` |
| `util/control/ServerControlState.java` | `feature/world/service/ServerControlState.java` |
| `listener/world/RedstoneControlListener.java` | `feature/world/listener/RedstoneControlListener.java` |
| `listener/player/JoinLockListener.java` | `feature/world/listener/JoinLockListener.java` |

> `ServerControlActions/State` (join-lock, redstone freeze, gamerule/server-control toggles) and the join-lock login listener are world/server-control concerns and group with the world feature. `JoinLockListener` reads `ServerControlState`, so it follows it here.

---

### feature/playerinfo  (seen/firstseen/playtime/list/near/whois/realname/info + join-leave data)

| Current | Target |
|---|---|
| `command/info/SeenCommand.java` | `feature/playerinfo/command/SeenCommand.java` |
| `command/info/FirstSeenCommand.java` | `feature/playerinfo/command/FirstSeenCommand.java` |
| `command/info/PlaytimeCommand.java` | `feature/playerinfo/command/PlaytimeCommand.java` |
| `command/info/ListCommand.java` | `feature/playerinfo/command/ListCommand.java` |
| `command/info/NearCommand.java` | `feature/playerinfo/command/NearCommand.java` |
| `command/info/WhoisCommand.java` | `feature/playerinfo/command/WhoisCommand.java` |
| `command/info/RealnameCommand.java` | `feature/playerinfo/command/RealnameCommand.java` |
| `command/info/InfoCommand.java` | `feature/playerinfo/command/InfoCommand.java` |
| `util/perf/PlaytimeService.java` | `feature/playerinfo/service/PlaytimeService.java` |
| `storage/JoinLeaveService.java` | `feature/playerinfo/service/JoinLeaveService.java` |
| `listener/player/JoinLeaveListener.java` | `feature/playerinfo/listener/JoinLeaveListener.java` |
| `listener/player/JoinListener.java` | `feature/playerinfo/listener/JoinListener.java` |

> `JoinLeaveService`/`JoinLeaveListener` handle join/leave broadcast + welcome MOTD lines; this is the player-presence domain → playerinfo. `JoinListener` (nameplate color + general join handling, depends on `TablistTeams`) is a generic per-player join hook; placed in playerinfo as the player-presence owner — alternatively core. `command/info/ListCommand` (the AFK/vanish-aware online list) is the canonical `/list`; the separate `command/core/ListCommand` went to `core/command`.

---

### feature/playerstate  (god/fly/speed/afk/heal/feed/vital/kill/butcher/smite/tree/spawnmob/spawner/gamemode)

| Current | Target |
|---|---|
| `util/control/GodModeManager.java` | `feature/playerstate/service/GodModeManager.java` |
| `util/control/FlightStateService.java` | `feature/playerstate/service/FlightStateService.java` |
| `util/control/KillModeManager.java` | `feature/playerstate/service/KillModeManager.java` |
| `util/control/AfkService.java` | `feature/playerstate/service/AfkService.java` |
| `command/utility/GodCommand.java` | `feature/playerstate/command/GodCommand.java` |
| `command/utility/FlyCommand.java` | `feature/playerstate/command/FlyCommand.java` |
| `command/utility/FlySpeedCommand.java` | `feature/playerstate/command/FlySpeedCommand.java` |
| `command/utility/WalkSpeedCommand.java` | `feature/playerstate/command/WalkSpeedCommand.java` |
| `command/utility/AfkCommand.java` | `feature/playerstate/command/AfkCommand.java` |
| `command/utility/HealCommand.java` | `feature/playerstate/command/HealCommand.java` |
| `command/utility/FeedCommand.java` | `feature/playerstate/command/FeedCommand.java` |
| `command/utility/VitalCommand.java` | `feature/playerstate/command/VitalCommand.java` |
| `command/utility/GamemodeCommand.java` | `feature/playerstate/command/GamemodeCommand.java` |
| `command/admin/KillCommand.java` | `feature/playerstate/command/KillCommand.java` |
| `command/admin/ButcherCommand.java` | `feature/playerstate/command/ButcherCommand.java` |
| `command/admin/SmiteCommand.java` | `feature/playerstate/command/SmiteCommand.java` |
| `command/admin/TreeCommand.java` | `feature/playerstate/command/TreeCommand.java` |
| `command/admin/SpawnMobCommand.java` | `feature/playerstate/command/SpawnMobCommand.java` |
| `command/admin/SpawnerCommand.java` | `feature/playerstate/command/SpawnerCommand.java` |

> `AfkService` is used by `playerinfo`'s `ListCommand` (AFK marker) but is owned by playerstate (the `/afk` toggle is a player-state). Cross-feature read is acceptable; the service's home is playerstate.

---

### Tests

| Current | Target |
|---|---|
| `test/.../economy/EconomySanitizeTest.java` | `test/.../feature/economy/EconomySanitizeTest.java` |
| `test/.../language/LanguageRegistryTest.java` | `test/.../core/language/LanguageRegistryTest.java` |
| `test/.../language/MessageDefaultsTest.java` | `test/.../core/language/MessageDefaultsTest.java` |
| `test/.../language/MotdRoundTripTest.java` | `test/.../core/motd/MotdRoundTripTest.java` |
| `test/.../util/text/PlaceholdersTest.java` | `test/.../util/text/PlaceholdersTest.java` |

> Tests mirror their subject's new package. `MotdRoundTripTest` follows `MotdService` into `core/motd`.

---

## 3. Decisions & Ambiguities

1. **Two messaging systems.** `message/MessageService` = live private `/msg`+reply+inbox; `messaging/MessageService` = persistent offline mailbox/ignore/socialspy. Both, plus their commands and the inbox GUI, consolidate into **feature/mail** with internal `pm/` and `mail/` subpackages and disambiguating renames (`PrivateMessageService`, `MailService`).

2. **`util/control/*` split.** This bucket mixed unrelated stateful services. Distributed by owning feature: vanish/freeze/staff-session → **staff**; god/flight/kill/afk → **playerstate**; per-player-time/daylight-fallback/server-control → **world**. None remain in `util/` (they are stateful, violating the "stateless static only" rule).

3. **`util/teleport/*` and `util/perf/*`.** `TeleportManager`/`TeleportRequestService`/`TpaService` are stateful → **feature/teleport**. `PlaytimeService` → **playerinfo**. `TpsService` → **core/diagnostics**. Only `WarpAccess` (stateless permission helper) and `AsyncYamlSaver` (generic) stay in `util/`.

4. **Economy in api/ vs feature/.** `EconomyService` and `VaultEconomyProvider` are the public-facing economy contract (Vault integration, depended on by pay/sell/shop logic) → **api/economy**. `WorthService` + all economy commands are feature impl → **feature/economy**.

5. **Hologram api/ vs feature/.** Only the documented `HologramFacade` and the two public events go to **api/hologram**; the entire engine (backends, packets, render, anim, text, command tree, GUI, storage) stays in **feature/hologram**.

6. **Warp split from teleport.** Warp has its own service, three command classes, ~17 GUI/menu/input classes, and shared styling — a self-contained surface large enough to be its own **feature/warp**, leaving **feature/teleport** focused on tp/home/spawn movement.

7. **JoinListener / JoinLeaveListener (cross-feature join hooks).** Both placed in **playerinfo** (the player-presence owner). `JoinListener` also touches `TablistTeams`; this cross-feature read is acceptable. `JoinLockListener` follows `ServerControlState` into **world**. Feature-specific join listeners (`HubJoinListener`, `HologramJoinListener`, `ScoreboardJoinListener`, `TablistJoinListener`) stay with their features.

8. **Two `ListCommand` classes.** `command/core/ListCommand` (generic) → **core/command**; `command/info/ListCommand` (AFK/vanish-aware online roster, the registered `/list`) → **feature/playerinfo**. Naming collision is resolved by package separation.

9. **`EnchantCommand` collision.** `command/utility/EnchantCommand` opens the **vanilla** enchant table → **feature/item**. The custom enchant system's command is `enchant/command/ObxEnchantCommand` → **feature/enchant**.

10. **BungeeMessenger.** Despite the proposal flagging it as "if generic," it imports `HubService` and exists for hub server transport → **feature/hub**, not core.

11. **MOTD + TPS as core.** `MotdService`/`MotdPingListener` (server-list ping, infrastructural) → **core/motd**; `TpsService`/`TpsCommand`/`PluginListCommand` (diagnostics) → **core/diagnostics** + **core/command**. `CommandOverrideListener` (global command interception) → **core/command**.

12. **Generic GUIs to core.** `MenuHolder`, `CustomHeadUtil`, `HelpGui*`, `MainMenu*` are framework/cross-cutting GUI → **core/gui**. Feature-specific menus (warp, staff/admin, hub selector, enchant, inbox, hologram editor) live with their features.

13. **`Main.java`** stays at the package root as the plugin bootstrap/entry point (it wires every feature together).
