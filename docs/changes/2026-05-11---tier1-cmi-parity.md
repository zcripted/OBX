■ **Created:** 2026-05-11 3:00 pm

■ **Last Updated:** 2026-05-11 3:00 pm

# Tier 1 CMI parity — teleport requests, messaging, AFK, kits, economy, player info

Big-batch landing of CMI-style Tier 1 essentials. Each bucket ships with: dedicated service, command(s) under the appropriate `command/<category>/` package, EN+DE language keys in `MessageDefaults`, Adventure-based hover/click prompts where the user is presented with an actionable choice, and a clean permission tree.

## Goals & guard-rails

- **Minimalism:** each command file mirrors `HomeCommand` (compact, single responsibility).
- **Persistence:** every mutator routes through `AsyncYamlSaver` so disk writes never block a tick.
- **Folia-safe:** schedulers always go through `SchedulerAdapter`.
- **Adventure hover/click:** interactive prompts go through `ComponentMessenger.sendJoinedHoverMessages` so legacy + modern clients both work.
- **Permissions:** every command has a node, defaults follow CMI conventions (player commands → `true`, staff/admin → `op`).
- **Bilingual:** every new message key is added in both EN and DE inside `MessageDefaults`.

## Categories

### 1. Teleport requests (`command/teleportation/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/tpa <player>` | `call`, `tpask` | `obx.tpa` | true |
| `/tpahere <player>` | `tpaskhere` | `obx.tpahere` | true |
| `/tpaccept [player]` | `tpyes`, `tpac` | `obx.tpaccept` | true |
| `/tpdeny [player]` | `tpno`, `tpd` | `obx.tpdeny` | true |
| `/tpcancel [player]` | `tpc` | `obx.tpcancel` | true |
| `/tptoggle` | `tpt` | `obx.tptoggle` | true |
| `/tphere <player>` | `s`, `tphr` | `obx.tphere` | op |
| `/tppos <x> <y> <z> [world] [yaw] [pitch]` | `tpcoords` | `obx.tppos` | op |
| `/tpall` | `tpeveryone` | `obx.tpall` | op |

`TpaService` holds pending requests with a per-config expiry, dispatches Adventure-driven prompts (`[Accept]` / `[Deny]` clickable + hover), and routes accepted teleports through `TeleportManager` so warmup applies.

### 2. Messaging (`command/messaging/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/msg <player> <message>` | `tell`, `w`, `whisper`, `pm` | `obx.msg` | true |
| `/reply <message>` | `r` | `obx.msg` | true |
| `/ignore <player>` | `block` | `obx.ignore` | true |
| `/socialspy` | `spy` | `obx.socialspy` | op |
| `/mail send|read|clear|list` | — | `obx.mail` | true |
| `/me <action>` | `action` | `obx.me` | true |
| `/broadcast <message>` | `bc` | `obx.broadcast` | op |
| `/staffchat <message>` | `sc`, `achat` | `obx.staffchat` | op |

`MessageService` tracks last-recipient (for `/r`), ignore sets, socialspy listeners, and inbox mail. Mail is persisted in `messaging.yml`.

### 3. AFK (`util/control/`, `command/utility/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/afk [reason]` | `away` | `obx.afk` | true |
| `/afk <player>` | — | `obx.afk.others` | op |

`AfkService` tracks idle time via PlayerMove/Chat/Command/Interact listeners. Configurable thresholds in `config.yml` (`afk.idle-seconds`, `afk.kick-seconds`, `afk.broadcast`).

### 4. Kits (`command/utility/`, `storage/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/kit [name]` | `kits` | `obx.kit` | true |
| `/kit list` | — | `obx.kit.list` | true |
| `/kit info <name>` | — | `obx.kit.info` | true |
| `/kit give <player> <name>` | — | `obx.kit.give` | op |
| `/kit reload` | — | `obx.kit.reload` | op |

Per-kit gate: `obx.kit.<name>`. Cooldowns persisted in `kits-data.yml`. Kit definitions in `kits.yml`. Players can preview contents via `/kit info`.

### 5. Economy (`economy/`, `command/economy/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/balance [player]` | `bal`, `money` | `obx.balance` | true |
| `/baltop [page]` | `balancetop`, `moneytop` | `obx.baltop` | true |
| `/pay <player> <amount>` | — | `obx.pay` | true |
| `/eco give|take|set|reset <player> [amount]` | `economy` | `obx.eco` | op |
| `/worth [amount]` | `value` | `obx.worth` | true |
| `/sell hand\|all\|<material>` | — | `obx.sell` | true |
| `/sellall` | — | `obx.sellall` | true |

`EconomyService` owns balances; `VaultEconomyProvider` reflectively registers if Vault is present. Balances persist in `economy.yml`. Item prices live in `worth.yml`.

### 6. Player info (`command/info/`, `util/perf/`)
| Command | Aliases | Permission | Default |
| --- | --- | --- | --- |
| `/seen <player>` | `lastseen` | `obx.seen` | true |
| `/firstseen <player>` | — | `obx.firstseen` | true |
| `/playtime [player]` | `ptime` | `obx.playtime` | true |
| `/list` | `who`, `online`, `players` | `obx.list` | true |
| `/near [radius]` | `nearby` | `obx.near` | true |
| `/whois <player>` | — | `obx.whois` | true |
| `/realname <displayname>` | — | `obx.realname` | true |
| `/info <player>` | `profile` | `obx.info.player` | true |

`PlaytimeService` accumulates per-player playtime via join/quit listeners, persists to `playtime.yml` (per-UUID totals + first-seen + last-seen). Hover/click profile cards via `ComponentMessenger`.

## Files touched

### New
- `command/teleportation/{TpaCommand,TpaHereCommand,TpAcceptCommand,TpDenyCommand,TpCancelCommand,TpToggleCommand,TpHereCommand,TpPosCommand,TpAllCommand}.java`
- `command/messaging/{MsgCommand,ReplyCommand,IgnoreCommand,SocialSpyCommand,MailCommand,MeCommand,BroadcastCommand,StaffChatCommand}.java`
- `command/utility/{AfkCommand,KitCommand}.java`
- `command/economy/{BalanceCommand,BalTopCommand,PayCommand,EcoCommand,WorthCommand,SellCommand,SellAllCommand}.java`
- `command/info/{SeenCommand,FirstSeenCommand,PlaytimeCommand,ListCommand,NearCommand,WhoisCommand,RealnameCommand,InfoCommand}.java`
- `util/teleport/TpaService.java`
- `messaging/MessageService.java`, `messaging/listener/MessagingChatListener.java`
- `util/control/AfkService.java`, `util/control/AfkActivityListener.java`
- `kit/KitService.java`, `kit/Kit.java`
- `economy/EconomyService.java`, `economy/VaultEconomyProvider.java`, `economy/WorthService.java`
- `util/perf/PlaytimeService.java`

### Edited
- `Main.java` — register services, listeners, and commands.
- `language/MessageDefaults.java` — EN + DE keys for every new message.
- `resources/plugin.yml` — command + permission declarations.
- `resources/config.yml` — AFK, economy, and mail config sections.
- `resources/kits.yml` — default kit definitions.
- `resources/worth.yml` — default item prices.
- `docs/information/about.md` — updated command/permission tables.

## Suggested commit messages

```
Feature (teleport-requests): /tpa /tpahere /tpaccept /tpdeny /tpcancel /tptoggle /tphere /tppos /tpall with Adventure prompts and EN/DE messages
Feature (messaging): /msg /r /ignore /socialspy /mail /me /broadcast /staffchat with persistent ignore lists, mail inbox, and EN/DE messages
Feature (afk): /afk command, idle auto-detection, broadcast + optional kick, EN/DE messages
Feature (kits): /kit subcommands, kits.yml definitions, per-player cooldown persistence, EN/DE messages
Feature (economy): EconomyService with Vault provider, /balance /baltop /pay /eco /worth /sell /sellall, EN/DE messages
Feature (player-info): PlaytimeService + /seen /firstseen /playtime /list /near /whois /realname /info, EN/DE messages
```
