package dev.sergeantfuzzy.sfcore.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageDefaults {

    private static final LinkedHashMap<String, Translation> DEFAULTS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, SectionComment> SECTION_COMMENTS = new LinkedHashMap<>();

    // Shared boxed-report header pieces, matching the /list and /sf info style
    // (see styled-info-report-format). The math-bold "SF-CORE", ▍, ›, and the
    // ─ rule are written as escapes so they are encoding-independent. Declared
    // before the static block because usageBox() is called from it.
    private static final String BOX_TITLE =
            "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &f";
    private static final String BOX_RULE = boxRule();

    static {
        // Comments for top-level categories (alphabetical)
        section("admin",
                "Administration and server control feedback.",
                "Administration und Serverkontrolle.");
        section("commands",
                "SF-Core command outputs and help text.",
                "SF-Core Befehle und Hilfetexte.");
        section("core",
                "Common system messages and prefixes.",
                "Allgemeine Systemnachrichten und Prefix.");
        section("gamemode",
                "Gamemode change feedback and errors.",
                "Rückmeldungen zum Wechsel des Spielmodus.");
        section("hologram",
                "Hologram module feedback (creation, editing, errors, interaction).",
                "Hologramm-Modul-Rückmeldungen (Erstellen, Bearbeiten, Fehler, Interaktion).");
        section("hub",
                "Hub / lobby system messages.",
                "Nachrichten des Hub- / Lobby-Systems.");
        section("language",
                "Language selection and preferences.",
                "Sprachauswahl und Einstellungen.");
        section("menus",
                "Chat feedback triggered by menus (non-GUI text).",
                "Chat-Rückmeldungen aus Menüs (keine GUI-Texte).");
        section("player",
                "Player management commands and status messages.",
                "Spieler-Management-Befehle und Statusmeldungen.");
        section("teleport",
                "Teleportation, homes, spawn, and warp messages.",
                "Teleportation, Homes, Spawn- und Warp-Nachrichten.");
        section("utility",
                "Utility commands and quick actions.",
                "Hilfsbefehle und Schnellaktionen.");
        section("tpa",
                "Player-to-player teleport-request flow (/tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tptoggle).",
                "Spieler-zu-Spieler Teleport-Anfragen (/tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tptoggle).");
        section("messaging",
                "Private messages, mail, broadcasts, and staff chat.",
                "Privatnachrichten, Mail, Broadcasts und Staff-Chat.");
        section("afk",
                "AFK toggle, broadcast lines, and idle-kick messages.",
                "AFK-Umschalter, Broadcasts und Auto-Kick-Nachrichten.");
        section("kit",
                "Kit claim, cooldown, info, and staff messages.",
                "Kit-Anforderung, Cooldown, Info- und Staff-Nachrichten.");
        section("economy",
                "Balance, baltop, pay, eco, worth, and sell flows.",
                "Kontostand, Bestenliste, Bezahlen, Eco-Befehle, Wert und Verkauf.");
        section("info",
                "Player lookup commands (/seen, /firstseen, /playtime, /list, /near, /whois, /realname, /info).",
                "Spieler-Suchbefehle (/seen, /firstseen, /playtime, /list, /near, /whois, /realname, /info).");
        section("flight",
                "Flight, fly-speed, and walk-speed commands.",
                "Flug, Fluggeschwindigkeit und Laufgeschwindigkeit.");
        section("freeze",
                "Movement-freeze command and broadcast lines.",
                "Bewegungs-Einfrieren — Befehle und Hinweise.");
        section("inventory",
                "Inventory utilities (/enderchest, /disposal, /hat, /clearinv, /repair, /more, /skull).",
                "Inventar-Hilfsbefehle (/enderchest, /disposal, /hat, /clearinv, /repair, /more, /skull).");
        section("item",
                "Item-editing commands (/itemname, /itemlore, /unbreakable, /give, /i, /book).",
                "Item-Bearbeitung (/itemname, /itemlore, /unbreakable, /give, /i, /book).");
        section("nickname",
                "Nickname command and feedback.",
                "Nickname-Befehl und Hinweise.");
        section("world",
                "World, time, and weather control commands.",
                "Welt-, Zeit- und Wetterbefehle.");
        section("jail",
                "Jail command suite and listener feedback.",
                "Jail-Befehle und Hinweise.");
        section("mob",
                "Mob- and world-tool commands (/butcher, /spawnmob, /spawner, /smite, /tree).",
                "Mob- und Welt-Werkzeuge (/butcher, /spawnmob, /spawner, /smite, /tree).");

        // Core
        add("core.prefix", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8 &8➠ &e", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8 &8➠ &e");
        // Custom prefix for the Arcanum enchantment module — every enchant.* chat
        // message resolves {prefix} to this instead of core.prefix (see
        // LanguageManager.resolveMessages). Mystic light-purple wordmark so the
        // module reads as its own premium feature. ✦ U+2726, math-bold ARCANUM, ➠ U+27A0.
        add("enchant.prefix",
                "&5✦ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠 &8➠ &7",
                "&5✦ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠 &8➠ &7");
        // Spawn wordmark — every teleport.spawn.* message resolves {prefix} to this
        // (see LanguageManager.resolveMessages). Same character style as core.prefix
        // (math-bold wordmark + ➠) but light-yellow. Math-bold SPAWN: S P A W N.
        add("teleport.spawn.prefix",
                "&e𝗦𝗣𝗔𝗪𝗡 &8➠ &e",
                "&e𝗦𝗣𝗔𝗪𝗡 &8➠ &e");
        // System wordmark for /sf reload (+ config / file) and /sf debug. Cog ⚙ (U+2699)
        // + math-bold SYSTEM in hex &#FF4526 (renders on 1.16+), same style as core.prefix.
        add("system.prefix",
                "&#FF4526⚙ 𝗦𝗬𝗦𝗧𝗘𝗠 &8➠ &7",
                "&#FF4526⚙ 𝗦𝗬𝗦𝗧𝗘𝗠 &8➠ &7");
        // Inbox wordmark — "saved to inbox" / "N messages" / inbox actions resolve
        // {prefix} to this (see LanguageManager.resolveMessages). Envelope ✉ U+2709,
        // math-bold INBOX, ➠ U+27A0, in custom gold #FFD93D (renders on 1.16+).
        add("inbox.prefix",
                "&#FFD93D✉ 𝗜𝗡𝗕𝗢𝗫 &8➠ &7",
                "&#FFD93D✉ 𝗜𝗡𝗕𝗢𝗫 &8➠ &7");
        add("core.divider", "&6&l------------------------------", "&6&l------------------------------");
        // Slim dark-gray box divider used by the styled "plugin info" reports (/pl, /sf info, /sf about, /sf help, /sf version, /sf commands, /sf permissions). 30x U+2500.
        add("core.divider-line", "&8──────────────────────────────", "&8──────────────────────────────");
        add("core.no-permission", "{prefix}&eYou don't have permission to do that.", "{prefix}&eDafür hast du keine Berechtigung.");
        add("core.no-permission-console", "&eYou don't have permission to do that.", "&eDu hast dafür keine Berechtigung.");
        add("core.player-only", "{prefix}&eOnly players can run this command.", "{prefix}&eNur Spieler können diesen Befehl ausführen.");
        add("core.player-only-console", "&eOnly players can run this command.", "&eNur Spieler können diesen Befehl ausführen.");

        // Language selection
        add("language.already", "{prefix}&eYou are already using &6{language}&e.", "{prefix}&eDu verwendest bereits &6{language}&e.");
        add("language.changed", "{prefix}&eYour language is now set to &6{language}&e.", "{prefix}&eDeine Sprache wurde auf &6{language}&e gestellt.");
        add("language.changed-console", "&eLanguage set to &6{language}&e for this command.", "&eSprache für diesen Befehl auf &6{language}&e gesetzt.");
        add("language.console-only", "{prefix}&eConsole messages default to English.", "{prefix}&eDie Konsole nutzt standardmäßig Englisch.");
        add("language.current", "{prefix}&eCurrent language: &6{language}&e.", "{prefix}&eAktuelle Sprache: &6{language}&e.");
        add("language.invalid", "{prefix}&cUnknown language. Options: &eEnglish &7/ &eDeutsch", "{prefix}&cUnbekannte Sprache. Optionen: &eEnglisch &7/ &eDeutsch");
        add("language.usage",
                usageBox("Language", "Language", "/language <English|EN|German|DE>", "Change your preferred SF-Core language."),
                usageBox("Sprache", "Sprache", "/sprache <Englisch|EN|Deutsch|DE>", "Ändert deine bevorzugte SF-Core-Sprache."));

        // Gamemode
        add("gamemode.already", "{prefix}&eYou are already in &6{mode}&e.", "{prefix}&eDu bist bereits im Modus &6{mode}&e.");
        add("gamemode.changed-self", "{prefix}&eSet your gamemode to &6{mode}&e.", "{prefix}&eDein Spielmodus wurde auf &6{mode}&e gesetzt.");
        add("gamemode.changed-other", "{prefix}&eSet &6{target}&e's gamemode to &6{mode}&e.", "{prefix}&eSpielmodus von &6{target}&e auf &6{mode}&e gesetzt.");
        add("gamemode.changed-other-console", "&eSet {target}'s gamemode to {mode}.", "&eSpielmodus von {target} auf {mode} gesetzt.");
        add("gamemode.changed-target", "{prefix}&eYour gamemode was set to &6{mode}&e by &6{sender}&e.", "{prefix}&e{sender} hat deinen Spielmodus auf &6{mode}&e gesetzt.");
        add("gamemode.current", "{prefix}&eCurrent gamemode: &6{mode}&e.", "{prefix}&eAktueller Spielmodus: &6{mode}&e.");
        add("gamemode.unknown-mode-or-player", "{prefix}&cUnknown mode or player. Valid: &esurvival&7, &ecreative&7, &eadventure&7, &espectator", "{prefix}&cUnbekannter Modus oder Spieler. G\u00fcltig: &esurvival&7, &ecreative&7, &eadventure&7, &espectator");
        add("gamemode.usage.console", "&eUsage: /gamemode <mode> <player>", "&eBenutzung: /gamemode <Modus> <Spieler>");
        add("gamemode.usage.self",
                usageBox("Gamemode", "Gamemode", "/gamemode <survival|creative|adventure|spectator>", "Change your own gamemode."),
                usageBox("Spielmodus", "Spielmodus", "/gamemode <survival|creative|adventure|spectator>", "Ändert deinen eigenen Spielmodus."));
        add("gamemode.usage.target",
                usageBox("Gamemode", "Gamemode", "/gamemode <mode> {target}", "Pick a mode for {target} (e.g. creative)."),
                usageBox("Spielmodus", "Spielmodus", "/gamemode <Modus> {target}", "Waehle einen Modus fuer {target} (z. B. creative)."));

        // Teleport + homes + spawn
        add("teleport.back.missing", "{prefix}&eNo previous location recorded.", "{prefix}&eEs wurde kein vorheriger Ort gespeichert.");
        add("teleport.back.teleporting", "{prefix}&eTeleporting you back.", "{prefix}&eTeleportiere dich zurück.");
        add("teleport.top.usage",
                usageBox("Teleportation", "Top", "/top [player]", "Teleport to the highest safe block above you."),
                usageBox("Teleportation", "Top", "/top [Spieler]", "Teleportiert zum höchsten sicheren Block über dir."));
        add("teleport.top.usage-console", "&eUsage: /top <player>", "&eBenutzung: /top <Spieler>");
        add("teleport.top.no-safe", "{prefix}&cNo safe block found above &6{target}&c.", "{prefix}&cKein sicherer Block ueber &6{target}&c gefunden.");
        add("teleport.top.teleporting", "{prefix}&eTeleporting to the highest safe block.", "{prefix}&eTeleportiere zum hoechsten sicheren Block.");
        add("teleport.top.sent-other", "{prefix}&eTeleported &6{target}&e to the highest safe block.", "{prefix}&e&6{target}&e zum hoechsten sicheren Block teleportiert.");
        add("teleport.top.sent-target", "{prefix}&eYou were sent to the highest safe block by &6{sender}&e.", "{prefix}&eDu wurdest von &6{sender}&e zum hoechsten sicheren Block teleportiert.");
        add("teleport.top.target-not-found", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("teleport.home.limit", "{prefix}&eYou cannot create more than &6{limit} &ehomes.", "{prefix}&eDu kannst nicht mehr als &6{limit} &eHomes erstellen.");
        add("teleport.home.not-found", "{prefix}&eHome &6{home} &edoes not exist.", "{prefix}&eDas Home &6{home} &eexistiert nicht.");
        add("teleport.home.delete-usage",
                usageBox("Teleportation", "Delete Home", "/{command} <name>", "Delete one of your homes."),
                usageBox("Teleportation", "Home löschen", "/{command} <Name>", "Löscht eines deiner Homes."));
        add("teleport.home.removed", "{prefix}&eHome &6{home} &ehas been removed.", "{prefix}&eDas Home &6{home} &ewurde entfernt.");
        add("teleport.home.set", "{prefix}&eHome &6{home} &ehas been set.", "{prefix}&eDas Home &6{home} &ewurde gesetzt.");
        add("teleport.home.teleporting", "{prefix}&eTeleporting to home &6{home}&e.", "{prefix}&eTeleportiere zum Home &6{home}&e.");
        add("teleport.homes.list", "{prefix}&eHomes: &6{homes}", "{prefix}&eHomes: &6{homes}");
        add("teleport.homes.none", "none", "keine");
        add("teleport.spawn.delete-confirm", "{prefix}&eClick {click}&e or type &6confirm&e in chat to delete the spawn. &7(10s)", "{prefix}&eKlicke {click}&e oder tippe &6confirm&e in den Chat, um den Spawn zu löschen. &7(10s)");
        add("teleport.spawn.delete-confirm-hover", "&eClick to delete the spawn", "&eKlicke, um den Spawn zu löschen");
        add("teleport.spawn.delete-timeout", "{prefix}&cSpawn deletion timed out. &7Click {click}&7 to delete again.", "{prefix}&cSpawn-Löschung abgelaufen. &7Klicke {click}&7, um erneut zu löschen.");
        add("teleport.spawn.delete-timeout-hover", "&eClick to re-run /spawn del", "&eKlicke, um /spawn del erneut auszuführen");
        add("teleport.spawn.delete-confirm-needed", "{prefix}&cPlease confirm spawn deletion.", "{prefix}&cBitte bestätige das Löschen des Spawns.");
        add("teleport.spawn.deleted", "{prefix}&eSpawn has been unset.", "{prefix}&eSpawn wurde entfernt.");
        // Styled boxed report matching /pl and /sf info (see styled-info-report-format).
        add("teleport.spawn.info", Arrays.asList(
                "",
                BOX_TITLE + "Spawn Point",
                BOX_RULE,
                "",
                "  &7World      &8›  &f{world}",
                "  &7Position   &8›  &f{x}, {y}, {z}",
                "  &7Facing     &8›  &f{yaw} / {pitch}",
                "  &7Set by     &8›  &f{setBy}",
                "  &7Set at     &8›  &f{setAt}",
                ""
        ), Arrays.asList(
                "",
                BOX_TITLE + "Spawn-Punkt",
                BOX_RULE,
                "",
                "  &7Welt        &8›  &f{world}",
                "  &7Position    &8›  &f{x}, {y}, {z}",
                "  &7Blick       &8›  &f{yaw} / {pitch}",
                "  &7Gesetzt von &8›  &f{setBy}",
                "  &7Gesetzt am  &8›  &f{setAt}",
                ""
        ));
        add("teleport.spawn.info-missing", "{prefix}&eSpawn is not set.", "{prefix}&eDer Spawn ist nicht gesetzt.");
        add("teleport.spawn.info-unknown", "unknown", "unbekannt");
        add("teleport.spawn.missing", "{prefix}&eSpawn has not been set yet.", "{prefix}&eDer Spawn wurde noch nicht gesetzt.");
        add("teleport.spawn.missing-op", "{prefix}&cSpawn is not set. Use &e/spawn set &cto create one.", "{prefix}&cSpawn ist nicht gesetzt. Nutze &e/spawn set &cum einen zu erstellen.");
        add("teleport.spawn.missing-op-hover", "&eClick to suggest /spawn set", "&eKlicke, um /spawn set vorzuschlagen");
        add("teleport.spawn.set", "{prefix}&eServer spawn updated.", "{prefix}&eServer-Spawn aktualisiert.");
        add("teleport.spawn.teleporting", "{prefix}&eTeleporting to spawn.", "{prefix}&eTeleportiere zum Spawn.");
        // ── Direct teleport (/tp, /tphere) ──────────────────────────────────────
        add("teleport.tp.usage", "{prefix}&eUsage: &6/tp <player> [target]", "{prefix}&eNutzung: &6/tp <Spieler> [Ziel]");
        add("teleport.tp.usage-here", "{prefix}&eUsage: &6/tphere <player>", "{prefix}&eNutzung: &6/tphere <Spieler>");
        add("teleport.tp.console-needs-two", "{prefix}&cConsole has no location — use &6/tp <player> <target>&c.", "{prefix}&cDie Konsole hat keine Position — nutze &6/tp <Spieler> <Ziel>&c.");
        add("teleport.tp.not-online", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("teleport.tp.went", Arrays.asList(
                "{prefix}&aTeleported to &6{target}&a.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ), Arrays.asList(
                "{prefix}&aZu &6{target}&a teleportiert.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ));
        add("teleport.tp.moved", Arrays.asList(
                "{prefix}&aYou were teleported to &6{target}&a.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ), Arrays.asList(
                "{prefix}&aDu wurdest zu &6{target}&a teleportiert.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ));
        add("teleport.tp.moved-sender", Arrays.asList(
                "{prefix}&aTeleported &6{player}&a to &6{target}&a.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ), Arrays.asList(
                "{prefix}&a&6{player}&a zu &6{target}&a teleportiert.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ));
        add("teleport.tp.brought", Arrays.asList(
                "{prefix}&aYou were summoned to &6{target}&a.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ), Arrays.asList(
                "{prefix}&aDu wurdest zu &6{target}&a gerufen.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ));
        add("teleport.tp.brought-self", Arrays.asList(
                "{prefix}&aBrought &6{player}&a to you.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ), Arrays.asList(
                "{prefix}&a&6{player}&a zu dir gebracht.",
                "  &8» &7{world} &8· &fX &7{x}  &fY &7{y}  &fZ &7{z}"
        ));
        // ── Teleport requests (/tpa) ────────────────────────────────────────────
        add("teleport.request.usage", "{prefix}&eUsage: &6/tpa <player>", "{prefix}&eNutzung: &6/tpa <Spieler>");
        add("teleport.request.self", "{prefix}&cYou cannot request to teleport to yourself.", "{prefix}&cDu kannst dich nicht zu dir selbst teleportieren.");
        add("teleport.request.sent", "{prefix}&aTeleport request sent to &6{player}&a. &7(expires in 60s)", "{prefix}&aTeleport-Anfrage an &6{player}&a gesendet. &7(läuft in 60s ab)");
        add("teleport.request.received", "{prefix}&6{player}&e wants to teleport to you.", "{prefix}&6{player}&e möchte sich zu dir teleportieren.");
        add("teleport.request.accept-hover", "&aClick to accept", "&aKlicke zum Akzeptieren");
        add("teleport.request.deny-hover", "&cClick to deny", "&cKlicke zum Ablehnen");
        // Second line of the /tpa prompt: "Choose to [Accept] or [Deny] this request."
        add("teleport.request.accept-button", "&a&l[Accept]", "&a&l[Annehmen]");
        add("teleport.request.deny-button", "&c&l[Deny]", "&c&l[Ablehnen]");
        add("teleport.request.choose-lead", "&8» &7Choose to", "&8» &7Wähle");
        add("teleport.request.choose-or", "&7or", "&7oder");
        add("teleport.request.choose-tail", "&7this request.", "&7diese Anfrage.");
        add("teleport.request.none", "{prefix}&eYou have no pending teleport requests.", "{prefix}&eDu hast keine offenen Teleport-Anfragen.");
        add("teleport.request.requester-offline", "{prefix}&cThat player is no longer online.", "{prefix}&cDieser Spieler ist nicht mehr online.");
        add("teleport.request.expired", "{prefix}&eYour teleport request to &6{player}&e expired.", "{prefix}&eDeine Teleport-Anfrage an &6{player}&e ist abgelaufen.");
        add("teleport.request.accepted", "{prefix}&aRequest accepted — teleporting to &6{player}&a.", "{prefix}&aAnfrage akzeptiert — teleportiere zu &6{player}&a.");
        add("teleport.request.accepted-target", "{prefix}&aAccepted &6{player}&a's teleport request.", "{prefix}&aTeleport-Anfrage von &6{player}&a akzeptiert.");
        add("teleport.request.denied", "{prefix}&c{player} denied your teleport request.", "{prefix}&c{player} hat deine Teleport-Anfrage abgelehnt.");
        add("teleport.request.denied-target", "{prefix}&eDenied &6{player}&e's teleport request.", "{prefix}&eTeleport-Anfrage von &6{player}&e abgelehnt.");
        // ── Position (/pos) ─────────────────────────────────────────────────────
        add("position.console", "{prefix}&eThe console has no in-world position.", "{prefix}&eDie Konsole hat keine Position in der Welt.");
        add("position.header", "&b▍ &b𝗣𝗢𝗦𝗜𝗧𝗜𝗢𝗡  &8›  &fYour Coordinates", "&b▍ &b𝗣𝗢𝗦𝗜𝗧𝗜𝗢𝗡  &8›  &fDeine Koordinaten");
        add("position.coords-line", "  &7X &8› &f{x}    &7Y &8› &f{y}    &7Z &8› &f{z}", "  &7X &8› &f{x}    &7Y &8› &f{y}    &7Z &8› &f{z}");
        add("position.world-line", "  &7World &8› &f{world}  &8·  &7Facing &8› &f{facing}", "  &7Welt &8› &f{world}  &8·  &7Blickrichtung &8› &f{facing}");
        add("position.copy-label", "&b&l⧉ Click to copy coordinates", "&b&l⧉ Klicke zum Kopieren der Koordinaten");
        add("position.copy-hover", "&7Copies &f{coords}&7 to your clipboard", "&7Kopiert &f{coords}&7 in deine Zwischenablage");
        add("position.actionbar", "&7X&8:&b{x} &7Y&8:&b{y} &7Z&8:&b{z}", "&7X&8:&b{x} &7Y&8:&b{y} &7Z&8:&b{z}");
        // ── Private messages (/msg, /rply, /inbox) ──────────────────────────────
        add("message.usage", "{prefix}&eUsage: &6/msg <player> <message>", "{prefix}&eNutzung: &6/msg <Spieler> <Nachricht>");
        add("message.console-blocked", "{prefix}&cYou cannot send messages to the console.", "{prefix}&cDu kannst keine Nachrichten an die Konsole senden.");
        add("message.self", "{prefix}&cYou cannot message yourself.", "{prefix}&cDu kannst dir nicht selbst schreiben.");
        add("message.not-found", "{prefix}&cPlayer &6{player}&c was not found.", "{prefix}&cSpieler &6{player}&c wurde nicht gefunden.");
        add("message.stored", "{prefix}&6{player}&7 is offline — saved to their inbox.", "{prefix}&6{player}&7 ist offline — im Postfach gespeichert.");
        add("message.format.sent", "&8[&dme &7→ &d{player}&8] &f{message}", "&8[&dich &7→ &d{player}&8] &f{message}");
        add("message.format.received", "&8[&d{player} &7→ &dme&8] &f{message}", "&8[&d{player} &7→ &dich&8] &f{message}");
        add("message.reply-lead", "&8» ", "&8» ");
        add("message.reply-button", "&a&l[Send reply message]", "&a&l[Antwort senden]");
        add("message.reply-hover", "&7Click to reply to &f{player}", "&7Klicke, um &f{player}&7 zu antworten");
        add("reply.no-target", "{prefix}&cYou have no one to reply to.", "{prefix}&cDu hast niemanden zum Antworten.");
        add("reply.cant-console", "{prefix}&cYou cannot reply to the console.", "{prefix}&cDu kannst der Konsole nicht antworten.");
        add("reply.draft-started", "{prefix}&7Replying to &6{player}&7 — type your message in chat. &8(60s)", "{prefix}&7Antwort an &6{player}&7 — tippe deine Nachricht in den Chat. &8(60s)");
        add("reply.cancel-button", "&8[&cCancel&8]", "&8[&cAbbrechen&8]");
        add("reply.cancel-hover", "&7Click to cancel your reply", "&7Klicke, um die Antwort abzubrechen");
        add("reply.draft-expired", "{prefix}&eYour reply draft expired.", "{prefix}&eDein Antwortentwurf ist abgelaufen.");
        add("reply.draft-cancelled", "{prefix}&eReply cancelled.", "{prefix}&eAntwort abgebrochen.");
        add("inbox.console", "{prefix}&eThe console has no inbox.", "{prefix}&eDie Konsole hat kein Postfach.");
        add("inbox.title", "&8Inbox &7({count})", "&8Postfach &7({count})");
        add("inbox.empty", "&7Your inbox is empty.", "&7Dein Postfach ist leer.");
        add("inbox.join-notify", "{prefix}&7You have &f{count}&7 message(s) in your inbox.", "{prefix}&7Du hast &f{count}&7 Nachricht(en) im Postfach.");
        add("inbox.open-button", "&8[&aOpen&8]", "&8[&aÖffnen&8]");
        add("inbox.open-hover", "&7Click to open your inbox", "&7Klicke, um dein Postfach zu öffnen");
        // Inbox action feedback (carries the INBOX prefix via routing).
        add("inbox.deleted", "{prefix}&7Message deleted.", "{prefix}&7Nachricht gelöscht.");
        add("inbox.bookmarked", "{prefix}&6★ &7Message bookmarked — it won't be cleared.", "{prefix}&6★ &7Nachricht markiert — sie wird nicht gelöscht.");
        add("inbox.unbookmarked", "{prefix}&7Bookmark removed.", "{prefix}&7Markierung entfernt.");
        add("inbox.cleared", "{prefix}&7Cleared &f{count}&7 message(s). &8(bookmarked kept)", "{prefix}&7&f{count}&7 Nachricht(en) gelöscht. &8(markierte behalten)");
        // Inbox GUI item labels.
        add("inbox.entry.unread", "&e● Unread", "&e● Ungelesen");
        add("inbox.entry.read", "&8● Read", "&8● Gelesen");
        add("inbox.entry.bookmarked", "&6★ Bookmarked", "&6★ Markiert");
        add("inbox.entry.click-read", "&eLeft-click &7to read", "&eLinksklick &7zum Lesen");
        add("inbox.entry.click-delete", "&cRight-click &7to delete", "&cRechtsklick &7zum Löschen");
        add("inbox.entry.click-bookmark", "&6Shift-click &7to bookmark", "&6Shift-Klick &7zum Markieren");
        add("inbox.entry.click-unbookmark", "&6Shift-click &7to remove bookmark", "&6Shift-Klick &7zum Entmarkieren");
        add("inbox.clear-name", "&c&lClear Inbox", "&c&lPostfach leeren");
        add("inbox.clear-lore1", "&7Removes all non-bookmarked messages.", "&7Entfernt alle nicht markierten Nachrichten.");
        add("inbox.clear-lore2", "&8Bookmarked (★) messages are kept.", "&8Markierte (★) Nachrichten bleiben.");
        add("inbox.read", Arrays.asList(
                "",
                "&d✉ &7From &f{player}",
                "&8{date}  ·  {time}",
                "&8──────────────────────",
                "&f{message}",
                ""), Arrays.asList(
                "",
                "&d✉ &7Von &f{player}",
                "&8{date}  ·  {time}",
                "&8──────────────────────",
                "&f{message}",
                ""));
        add("teleport.warmup.cancelled", "{prefix}&eTeleport cancelled.", "{prefix}&eTeleport abgebrochen.");
        add("teleport.warmup.start", "{prefix}&eTeleport commencing in &6{seconds} &eseconds. Do not move!", "{prefix}&eTeleport startet in &6{seconds} &eSekunden. Nicht bewegen!");
        add("teleport.warp.categories.list", "{prefix}&eWarp categories: &f{categories}", "{prefix}&eWarp-Kategorien: &f{categories}");
        add("teleport.warp.categories.none", "{prefix}&eNo warp categories available.", "{prefix}&eKeine Warp-Kategorien vorhanden.");
        add("teleport.warp.delete.confirm", "{prefix}&eType &6confirm &eto delete warp &6{warp}&e.", "{prefix}&eTippe &6confirm &eum den Warp &6{warp}&e zu löschen.");
        add("teleport.warp.delete.confirm-needed", "{prefix}&cPlease confirm deletion of &6{warp}&c.", "{prefix}&cBitte bestätige das Löschen von &6{warp}&c.");
        add("teleport.warp.deleted", "{prefix}&eWarp &6{warp} &ehas been deleted.", "{prefix}&eWarp &6{warp} &ewurde gelöscht.");
        add("teleport.warp.icon.cleared", "{prefix}&eCleared icon for warp &6{warp}&e.", "{prefix}&eIcon für Warp &6{warp}&e zurückgesetzt.");
        add("teleport.warp.icon.invalid", "{prefix}&cUnknown icon &6{icon}&c.", "{prefix}&cUnbekanntes Icon &6{icon}&c.");
        add("teleport.warp.icon.updated", "{prefix}&eSet icon for warp &6{warp}&e to &6{icon}&e.", "{prefix}&eIcon für Warp &6{warp}&e auf &6{icon}&e gesetzt.");
        add("teleport.warp.info", Arrays.asList(
                "{prefix}&6Warp Info: &e{warp}",
                "{prefix}&eWorld: &f{world}",
                "{prefix}&eXYZ: &f{x}, {y}, {z}",
                "{prefix}&eYaw/Pitch: &f{yaw}/{pitch}",
                "{prefix}&eCategory: &f{category}",
                "{prefix}&eVisibility: &f{visibility}",
                "{prefix}&ePermission: &f{permission}",
                "{prefix}&eIcon: &f{icon}",
                "{prefix}&eSet by: &f{setBy}",
                "{prefix}&eSet at: &f{setAt}"
        ), Arrays.asList(
                "{prefix}&6Warp-Info: &e{warp}",
                "{prefix}&eWelt: &f{world}",
                "{prefix}&eXYZ: &f{x}, {y}, {z}",
                "{prefix}&eYaw/Pitch: &f{yaw}/{pitch}",
                "{prefix}&eKategorie: &f{category}",
                "{prefix}&eSichtbarkeit: &f{visibility}",
                "{prefix}&eBerechtigung: &f{permission}",
                "{prefix}&eIcon: &f{icon}",
                "{prefix}&eGesetzt von: &f{setBy}",
                "{prefix}&eGesetzt am: &f{setAt}"
        ));
        add("teleport.warp.info-unknown", "unknown", "unbekannt");
        add("teleport.warp.invalid-location", "{prefix}&cWarp &6{warp}&c has no valid location.", "{prefix}&cWarp &6{warp}&c hat keinen gültigen Standort.");
        add("teleport.warp.invalid-name", "{prefix}&cWarp names may only use letters, numbers, underscores, and dashes (max 32).", "{prefix}&cWarp-Namen dürfen nur Buchstaben, Zahlen, Unterstrich und Bindestrich nutzen (max 32).");
        add("teleport.warp.list.empty", "{prefix}&eNo warps available.", "{prefix}&eKeine Warps verfügbar.");
        add("teleport.warp.list.empty-category", "{prefix}&eNo warps in category &6{category}&e.", "{prefix}&eKeine Warps in der Kategorie &6{category}&e.");
        add("teleport.warp.list.page", "{prefix}&eWarps &7(Page {page}/{pages}): &f{warps}", "{prefix}&eWarps &7(Seite {page}/{pages}): &f{warps}");
        add("teleport.warp.list.page-category", "{prefix}&eWarps in &6{category}&7 (Page {page}/{pages}): &f{warps}", "{prefix}&eWarps in &6{category}&7 (Seite {page}/{pages}): &f{warps}");
        add("teleport.warp.move.success", "{prefix}&eWarp &6{warp}&e moved to your location.", "{prefix}&eWarp &6{warp}&e zu deinem Standort verschoben.");
        add("teleport.warp.no-access", "{prefix}&cYou cannot access warp &6{warp}&c.", "{prefix}&cDu kannst den Warp &6{warp}&c nicht nutzen.");
        add("teleport.warp.no-warps", "{prefix}&eNo warps have been created yet.", "{prefix}&eEs wurden noch keine Warps erstellt.");
        add("teleport.warp.no-warps-category", "{prefix}&eNo warps in category &6{category}&e.", "{prefix}&eKeine Warps in der Kategorie &6{category}&e.");
        add("teleport.warp.not-found", "{prefix}&cWarp &6{warp}&c does not exist.", "{prefix}&cWarp &6{warp}&c existiert nicht.");
        add("teleport.warp.permission.none", "none", "keine");
        add("teleport.warp.public.updated", "{prefix}&eWarp &6{warp}&e visibility set to &6{state}&e.", "{prefix}&eSichtbarkeit von Warp &6{warp}&e auf &6{state}&e gesetzt.");
        add("teleport.warp.rename.conflict", "{prefix}&cA warp named &6{warp}&c already exists.", "{prefix}&cEin Warp namens &6{warp}&c existiert bereits.");
        add("teleport.warp.rename.success", "{prefix}&eWarp renamed from &6{old} &eto &6{new}&e.", "{prefix}&eWarp von &6{old} &ezu &6{new}&e umbenannt.");
        add("teleport.warp.sent-other", "{prefix}&eSent &6{target}&e to warp &6{warp}&e.", "{prefix}&e&6{target}&e zum Warp &6{warp}&e gesendet.");
        add("teleport.warp.sent-target", "{prefix}&eYou were sent to warp &6{warp}&e by &6{sender}&e.", "{prefix}&eDu wurdest von &6{sender}&e zum Warp &6{warp}&e teleportiert.");
        add("teleport.warp.set.confirm", "{prefix}&eWarp &6{warp}&e already exists. Run again with confirm to overwrite.", "{prefix}&eWarp &6{warp}&e existiert bereits. Mit confirm überschreiben.");
        add("teleport.warp.set.created", "{prefix}&eWarp &6{warp}&e created.", "{prefix}&eWarp &6{warp}&e erstellt.");
        add("teleport.warp.set.updated", "{prefix}&eWarp &6{warp}&e updated.", "{prefix}&eWarp &6{warp}&e aktualisiert.");
        add("teleport.warp.target-no-access", "{prefix}&c{target} cannot access warp &6{warp}&c.", "{prefix}&c{target} kann den Warp &6{warp}&c nicht nutzen.");
        add("teleport.warp.teleporting", "{prefix}&eTeleporting to warp &6{warp}&e.", "{prefix}&eTeleportiere zum Warp &6{warp}&e.");
        add("teleport.warp.unknown", "{prefix}&cUnknown warp or subcommand. Try &e/warp list&c.", "{prefix}&cUnbekannter Warp oder Unterbefehl. Versuche &e/warp list&c.");
        add("teleport.warp.usage.category",
                usageBox("Warps", "Category", "/warp category <name>", "List the warps in a category."),
                usageBox("Warps", "Kategorie", "/warp category <Name>", "Listet die Warps einer Kategorie auf."));
        add("teleport.warp.usage.console", "&eUsage: /warp <name|subcommand>", "&eBenutzung: /warp <Name|Unterbefehl>");
        add("teleport.warp.usage.delete",
                usageBox("Warps", "Delete", "/warp delete <name>", "Delete a warp."),
                usageBox("Warps", "Löschen", "/warp delete <Name>", "Löscht einen Warp."));
        add("teleport.warp.usage.icon",
                usageBox("Warps", "Icon", "/warp icon <name> [material]", "Set or clear a warp's icon."),
                usageBox("Warps", "Icon", "/warp icon <Name> [Material]", "Setzt oder entfernt das Icon eines Warps."));
        add("teleport.warp.usage.info",
                usageBox("Warps", "Info", "/warp info <name>", "Show a warp's details."),
                usageBox("Warps", "Info", "/warp info <Name>", "Zeigt die Details eines Warps."));
        add("teleport.warp.usage.move",
                usageBox("Warps", "Move", "/warp move <name>", "Move a warp to your current location."),
                usageBox("Warps", "Verschieben", "/warp move <Name>", "Verschiebt einen Warp zu deiner Position."));
        add("teleport.warp.usage.public",
                usageBox("Warps", "Public", "/warp public <name> [true|false]", "Toggle a warp's public visibility."),
                usageBox("Warps", "Öffentlich", "/warp public <Name> [true|false]", "Schaltet die Sichtbarkeit eines Warps um."));
        add("teleport.warp.usage.rename",
                usageBox("Warps", "Rename", "/warp rename <old> <new>", "Rename a warp."),
                usageBox("Warps", "Umbenennen", "/warp rename <Alt> <Neu>", "Benennt einen Warp um."));
        add("teleport.warp.usage.set",
                usageBox("Warps", "Set", "/warp set <name> [confirm]", "Create or update a warp at your location."),
                usageBox("Warps", "Setzen", "/warp set <Name> [confirm]", "Erstellt oder aktualisiert einen Warp an deiner Position."));
        add("teleport.warp.usage.tp",
                usageBox("Warps", "Teleport", "/warp tp <name> [player]", "Teleport to a warp."),
                usageBox("Warps", "Teleport", "/warp tp <Name> [Spieler]", "Teleportiert zu einem Warp."));
        add("teleport.warp.usage.tp-console", "&eUsage: /warp tp <name> <player>", "&eBenutzung: /warp tp <Name> <Spieler>");
        add("teleport.warp.visibility.hidden", "Hidden", "Versteckt");
        add("teleport.warp.visibility.public", "Public", "Oeffentlich");
        add("teleport.warp.gui.title", "&6Warps &7(Page {page}/{pages})", "&6Warps &7(Seite {page}/{pages})");
        add("teleport.warp.gui.world", "&7World: &e{world}", "&7Welt: &e{world}");
        add("teleport.warp.gui.xyz", "&7XYZ: &e{x}&7, &e{y}&7, &e{z}", "&7XYZ: &e{x}&7, &e{y}&7, &e{z}");
        add("teleport.warp.gui.yawpitch", "&7Yaw/Pitch: &e{yaw}&7 / &e{pitch}", "&7Yaw/Pitch: &e{yaw}&7 / &e{pitch}");
        add("teleport.warp.gui.category", "&7Category: &e{category}", "&7Kategorie: &e{category}");
        add("teleport.warp.gui.visibility", "&7Visibility: &e{visibility}", "&7Sichtbarkeit: &e{visibility}");
        add("teleport.warp.gui.permission", "&7Permission: &e{permission}", "&7Berechtigung: &e{permission}");
        add("teleport.warp.gui.setBy", "&7Set by: &e{setBy}", "&7Gesetzt von: &e{setBy}");
        add("teleport.warp.gui.setAt", "&7Set at: &e{setAt}", "&7Gesetzt am: &e{setAt}");
        add("teleport.warp.gui.location-missing", "&cLocation missing.", "&cStandort fehlt.");
        add("teleport.warp.gui.action", "&aClick to teleport", "&aKlicke zum Teleportieren");
        add("teleport.warp.gui.button.close", "&cClose", "&cSchliessen");
        add("teleport.warp.gui.button.close-lore", "&7Close this menu", "&7Dieses Menue schliessen");
        add("teleport.warp.gui.button.previous", "&ePrevious Page", "&eVorherige Seite");
        add("teleport.warp.gui.button.previous-lore", "&7Go to page {page}", "&7Gehe zu Seite {page}");
        add("teleport.warp.gui.button.next", "&eNext Page", "&eNaechste Seite");
        add("teleport.warp.gui.button.next-lore", "&7Go to page {page}", "&7Gehe zu Seite {page}");

        // Utility
        add("utility.feed.success", "{prefix}&eYou are no longer hungry.", "{prefix}&eDu bist nicht mehr hungrig.");
        add("utility.feed.invalid-gamemode", "{prefix}&eYou can't use /feed in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst /feed im Kreativ- oder Zuschauermodus nicht nutzen. Wechsle in Survival oder Adventure.");
        add("utility.heal.success", "{prefix}&eYou feel rejuvenated.", "{prefix}&eDu fühlst dich erfrischt.");
        add("utility.heal.invalid-gamemode", "{prefix}&eYou can't use /heal in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst /heal im Kreativ- oder Zuschauermodus nicht nutzen. Wechsle in Survival oder Adventure.");
        add("utility.fly.invalid-gamemode", "{prefix}&eYou can't toggle fly while in Creative or Spectator. Try Survival or Adventure.", "{prefix}&eDu kannst Flug im Kreativ- oder Zuschauermodus nicht umschalten. Versuche Survival oder Adventure.");

        add("utility.craft.usage",
                usageBox("Utility", "Research", "/research [item]", "Show detailed information about an item."),
                usageBox("Werkzeuge", "Research", "/research [Item]", "Zeigt detaillierte Informationen zu einem Item."));
        add("utility.craft.usage-console", "&eUsage: /research <item>", "&eBenutzung: /research <Item>");
        add("utility.craft.hand-empty", "{prefix}&eHold an item to use /research without arguments.", "{prefix}&eHalte ein Item in der Hand, um /research ohne Argumente zu nutzen.");
        add("utility.craft.opened", "{prefix}&eOpened a virtual crafting table.", "{prefix}&eVirtueller Werktisch geoeffnet.");
        add("utility.craft.unsupported", "{prefix}&cThe virtual crafting table is unavailable on this server version.", "{prefix}&cDer virtuelle Werktisch ist auf dieser Server-Version nicht verfuegbar.");
        add("utility.anvil.opened", "{prefix}&eOpened a virtual anvil.", "{prefix}&eVirtueller Amboss geoeffnet.");
        add("utility.anvil.unsupported", "{prefix}&cThe virtual anvil requires Spigot 1.14 or newer.", "{prefix}&cDer virtuelle Amboss benoetigt Spigot 1.14 oder neuer.");
        add("utility.enchant.opened", "{prefix}&eOpened a virtual enchanting table.", "{prefix}&eVirtueller Verzauberungstisch geoeffnet.");
        add("utility.enchant.unsupported", "{prefix}&cThe virtual enchanting table is unavailable on this server version.", "{prefix}&cDer virtuelle Verzauberungstisch ist auf dieser Server-Version nicht verfuegbar.");
        add("utility.smith.opened", "{prefix}&eOpened a virtual smithing table.", "{prefix}&eVirtueller Schmiedetisch geoeffnet.");
        add("utility.smith.unsupported", "{prefix}&cThe virtual smithing table requires 1.16 or newer.", "{prefix}&cDer virtuelle Schmiedetisch benoetigt 1.16 oder neuer.");
        add("utility.stonecut.opened", "{prefix}&eOpened a virtual stonecutter.", "{prefix}&eVirtueller Steinsaege geoeffnet.");
        add("utility.stonecut.unsupported", "{prefix}&cThe virtual stonecutter requires Spigot 1.14 or newer.", "{prefix}&cDie virtuelle Steinsaege benoetigt Spigot 1.14 oder neuer.");
        add("utility.loom.opened", "{prefix}&eOpened a virtual loom.", "{prefix}&eVirtueller Webstuhl geoeffnet.");
        add("utility.loom.unsupported", "{prefix}&cThe virtual loom requires Spigot 1.14 or newer.", "{prefix}&cDer virtuelle Webstuhl benoetigt Spigot 1.14 oder neuer.");
        add("utility.grindstone.opened", "{prefix}&eOpened a virtual grindstone.", "{prefix}&eVirtueller Schleifstein geoeffnet.");
        add("utility.grindstone.unsupported", "{prefix}&cThe virtual grindstone requires Spigot 1.14 or newer.", "{prefix}&cDer virtuelle Schleifstein benoetigt Spigot 1.14 oder neuer.");
        add("utility.cartography.opened", "{prefix}&eOpened a virtual cartography table.", "{prefix}&eVirtueller Kartentisch geoeffnet.");
        add("utility.cartography.unsupported", "{prefix}&cThe virtual cartography table requires Spigot 1.14 or newer.", "{prefix}&cDer virtuelle Kartentisch benoetigt Spigot 1.14 oder neuer.");
        add("utility.map.opened", "{prefix}&eCreated a map centered on your location.", "{prefix}&eKarte zentriert auf deine Position erstellt.");
        add("utility.map.unsupported", "{prefix}&cMaps are unavailable on this server version.", "{prefix}&cKarten sind auf dieser Server-Version nicht verfuegbar.");
        add("utility.craft.not-found", "{prefix}&cUnknown item &6{item}&c.", "{prefix}&cUnbekanntes Item &6{item}&c.");
        add("utility.craft.header", "&6&lITEM PROFILE:", "&6&lITEM-PROFIL:");
        add("utility.craft.item-name", "&e{item}", "&e{item}");
        add("utility.craft.section.about.label", "&6ABOUT", "&6INFO");
        add("utility.craft.section.about.hint", "&7Hover to view item overview", "&7Hovern fuer Item-Uebersicht");
        add("utility.craft.section.recipe.label", "&6RECIPE", "&6REZEPT");
        add("utility.craft.section.recipe.hint", "&7Hover to view crafting recipe", "&7Hovern fuer Crafting-Rezept");
        add("utility.craft.section.use.label", "&6HOW TO USE", "&6VERWENDUNG");
        add("utility.craft.section.use.hint", "&7Hover to view usage and mechanics", "&7Hovern fuer Nutzung und Mechanik");
        add("utility.craft.section.find.label", "&6WHERE TO FIND", "&6FUNDORTE");
        add("utility.craft.section.find.hint", "&7Hover to view locations and biomes", "&7Hovern fuer Fundorte und Biome");
        add("utility.craft.section.dimensions.label", "&6DIMENSIONS", "&6DIMENSIONEN");
        add("utility.craft.section.dimensions.hint", "&7Hover to view world availability", "&7Hovern fuer Welt-Verfuegbarkeit");
        add("utility.craft.section.extra.label", "&6EXTRA INFO", "&6EXTRA INFO");
        add("utility.craft.section.extra.hint", "&7Hover for advanced details", "&7Hovern fuer Details");
        add("utility.craft.hover.about", Arrays.asList("&6Item Overview","&7Name: &f{item}","&7Type: &f{type}","&7Max Stack: &f{stack}","&7Durability: &f{durability}"), Arrays.asList("&6Item-Uebersicht","&7Name: &f{item}","&7Typ: &f{type}","&7Max Stack: &f{stack}","&7Haltbarkeit: &f{durability}"));
        add("utility.craft.hover.recipe.header", Arrays.asList("&6Recipe Details","&7Type: &f{type}"), Arrays.asList("&6Rezept-Details","&7Typ: &f{type}"));
        add("utility.craft.hover.recipe.none", Arrays.asList("&6Recipe Details","&7No crafting recipe found."), Arrays.asList("&6Rezept-Details","&7Kein Crafting-Rezept gefunden."));
        add("utility.craft.hover.recipe.grid-title", "&6Crafting Grid", "&6Herstellungsraster");
        add("utility.craft.hover.recipe.grid-row", "&7[{a}] [{b}] [{c}]", "&7[{a}] [{b}] [{c}]");
        add("utility.craft.hover.recipe.output", "&7Output: &f{output}", "&7Ergebnis: &f{output}");
        add("utility.craft.hover.recipe.grid-line", "&7Grid: &f{row1} &7/ &f{row2} &7/ &f{row3} &7=> &f{output}", "&7Raster: &f{row1} &7/ &f{row2} &7/ &f{row3} &7=> &f{output}");
        add("utility.craft.hover.recipe.row", "&7Row {row}: &f{items}", "&7Zeile {row}: &f{items}");
        add("utility.craft.hover.recipe.ingredients", "&7Ingredients: &f{items}", "&7Zutaten: &f{items}");
        add("utility.craft.hover.recipe.smelt", "&7Smelt: &f{input}", "&7Schmelzen: &f{input}");
        add("utility.craft.hover.use", Arrays.asList("&6Usage","&7{summary}"), Arrays.asList("&6Verwendung","&7{summary}"));
        add("utility.craft.hover.find", Arrays.asList("&6Where to Find","&7{summary}"), Arrays.asList("&6Fundorte","&7{summary}"));
        add("utility.craft.hover.dimensions", Arrays.asList("&6Dimensions","&7{summary}"), Arrays.asList("&6Dimensionen","&7{summary}"));
        add("utility.craft.hover.extra", Arrays.asList("&6Extra Info","&7Key: &f{key}","&7Legacy ID: &f{id}","&7Fuel: &f{fuel}","&7Edible: &f{edible}"), Arrays.asList("&6Extra Info","&7Key: &f{key}","&7Legacy ID: &f{id}","&7Brennstoff: &f{fuel}","&7Essbar: &f{edible}"));
        add("utility.craft.value.block", "Block", "Block");
        add("utility.craft.value.item", "Item", "Item");
        add("utility.craft.value.none", "N/A", "k.A.");
        add("utility.craft.value.empty", "-", "-");
        add("utility.craft.value.yes", "Yes", "Ja");
        add("utility.craft.value.no", "No", "Nein");
        add("utility.craft.value.unknown", "Unknown", "Unbekannt");
        add("utility.craft.recipe-type.shaped", "Shaped", "Geformt");
        add("utility.craft.recipe-type.shapeless", "Shapeless", "Formlos");
        add("utility.craft.recipe-type.smelting", "Smelting", "Schmelzen");
        add("utility.craft.recipe-type.unknown", "Unknown", "Unbekannt");
        add("utility.craft.use.block", "Place to build or decorate.", "Zum Bauen oder Dekorieren platzieren.");
        add("utility.craft.use.tool", "Use as a tool or weapon.", "Als Werkzeug oder Waffe nutzen.");
        add("utility.craft.use.armor", "Equip for protection.", "Zum Schutz anlegen.");
        add("utility.craft.use.consume", "Consume for nutrition or effects.", "Zum Essen oder fuer Effekte.");
        add("utility.craft.use.misc", "Use in crafting or trading.", "Zum Craften oder Handeln verwenden.");
        add("utility.craft.find.craftable", "Craftable at a workbench; may also appear in loot or trades.", "Am Werktisch craftbar; ggf. auch als Loot oder Handel.");
        add("utility.craft.find.ore", "Mined from ore veins in the world.", "Aus Erzen in der Welt abbaubar.");
        add("utility.craft.find.natural", "Found naturally in the world or by mining.", "Natuerlich in der Welt vorhanden oder abbaubar.");
        add("utility.craft.find.loot", "Common in loot, trades, or commands.", "Hauefig in Loot, Handel oder per Befehl.");
        add("utility.craft.dimensions.overworld", "Primarily Overworld.", "Hauptsaechlich Oberwelt.");
        add("utility.craft.dimensions.nether", "Primarily Nether.", "Hauptsaechlich Nether.");
        add("utility.craft.dimensions.end", "Primarily The End.", "Hauptsaechlich Das Ende.");
        add("utility.craft.dimensions.multi", "Overworld / Nether / End.", "Oberwelt / Nether / Ende.");

        // Menu feedback (non-GUI text)
        add("menus.main.thanks", "{prefix}&eThanks for checking out SF-Core!", "{prefix}&eDanke, dass du dir SF-Core anschaust!");
        add("menus.admin.placeholder", "{prefix}&eAdmin panel placeholder clicked.", "{prefix}&eAdmin-Panel-Platzhalter angeklickt.");

        // Player management
        add("player.vital.usage",
                usageBox("Players", "Vital", "/vital [player]", "Restore your or a target's health and hunger."),
                usageBox("Spieler", "Vital", "/vital [Spieler]", "Stellt Leben und Hunger wieder her."));
        add("player.vital.usage-console", "&eUsage: /vital <player>", "&eBenutzung: /vital <Spieler>");
        add("player.vital.success", "{prefix}&eYou feel fully restored.", "{prefix}&eDu fuehlst dich vollstaendig wiederhergestellt.");
        add("player.vital.success-other", "{prefix}&eRestored &6{target}&e's health and hunger.", "{prefix}&eGesundheit und Hunger von &6{target}&e wiederhergestellt.");
        add("player.vital.success-target", "{prefix}&eYour health and hunger were restored by &6{sender}&e.", "{prefix}&eDeine Gesundheit und dein Hunger wurden von &6{sender}&e wiederhergestellt.");
        add("player.vital.invalid-gamemode", "{prefix}&eYou can't use /vital in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst /vital im Kreativ- oder Zuschauermodus nicht nutzen. Wechsle zu Survival oder Adventure.");
        add("player.vital.invalid-gamemode-other", "{prefix}&eYou can't use /vital on &6{target}&e while they are in Creative or Spectator.", "{prefix}&eDu kannst /vital nicht auf &6{target}&e anwenden, solange der Spieler im Kreativ- oder Zuschauermodus ist.");
        add("player.vital.target-not-found", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        // Online player list (/list) — styled to match /pl (PluginListCommand)
        add("player.list.header", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fOnline Players  &8·  &f{online}&7/&f{max}",
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fSpieler online  &8·  &f{online}&7/&f{max}",
                "&8──────────────────────────────",
                ""
        ));
        add("player.list.staff", "  &c&lStaff  &8·  &f{count}", "  &c&lTeam  &8·  &f{count}");
        add("player.list.players", "  &e&lPlayers  &8·  &f{count}", "  &e&lSpieler  &8·  &f{count}");
        add("player.list.line", "    {names}", "    {names}");
        add("player.list.none", "&8&onone", "&8&okeine");
        add("player.list.message-hover", "&7Click to message &f{player}", "&7Klicke, um &f{player}&7 zu schreiben");
        // Hover shown on a clickable chat name (MiniMessage; no single quotes).
        add("chat.message-hover", "<gray>Click to message <white>{player}</white>", "<gray>Klicke, um <white>{player}</white> zu schreiben");
        add("player.list.footer", Arrays.asList(
                "",
                "&8──────────────────────────────",
                "  &7Legend  &8·  &c▪ Staff   &e▪ Players    &8(&f{online}&8/&f{max}&8 online)",
                ""
        ), Arrays.asList(
                "",
                "&8──────────────────────────────",
                "  &7Legende  &8·  &c▪ Team   &e▪ Spieler    &8(&f{online}&8/&f{max}&8 online)",
                ""
        ));
        add("player.god.usage",
                usageBox("Players", "God", "/god [player]", "Toggle complete invincibility."),
                usageBox("Spieler", "God", "/god [Spieler]", "Schaltet vollständige Unverwundbarkeit um."));
        add("player.god.usage-console", "&eUsage: /god <player>", "&eBenutzung: /god <Spieler>");
        add("player.god.enabled", "{prefix}&eGod mode enabled.", "{prefix}&eGod-Mode aktiviert.");
        add("player.god.disabled", "{prefix}&eGod mode disabled.", "{prefix}&eGod-Mode deaktiviert.");
        add("player.god.enabled-other", "{prefix}&eEnabled god mode for &6{target}&e.", "{prefix}&eGod-Mode fuer &6{target}&e aktiviert.");
        add("player.god.disabled-other", "{prefix}&eDisabled god mode for &6{target}&e.", "{prefix}&eGod-Mode fuer &6{target}&e deaktiviert.");
        add("player.god.enabled-target", "{prefix}&eGod mode enabled by &6{sender}&e.", "{prefix}&eGod-Mode wurde von &6{sender}&e aktiviert.");
        add("player.god.disabled-target", "{prefix}&eGod mode disabled by &6{sender}&e.", "{prefix}&eGod-Mode wurde von &6{sender}&e deaktiviert.");
        add("player.god.target-not-found", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("player.god.invalid-gamemode", "{prefix}&eYou can't toggle god mode in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst God-Mode im Kreativ- oder Zuschauermodus nicht umschalten. Wechsle zu Survival oder Adventure.");
        add("player.god.invalid-gamemode-other", "{prefix}&eYou can't toggle god mode on &6{target}&e while they are in Creative or Spectator.", "{prefix}&eDu kannst God-Mode bei &6{target}&e nicht umschalten, solange der Spieler im Kreativ- oder Zuschauermodus ist.");

        // Vanish (staff invisibility) — every player-facing line carries the
        // [STAFF] badge so it's unambiguous that a staff tool fired. The
        // descriptive trailer ("You are now invisible.", etc.) sits on its own
        // chat line so the toggle line stays compact at a glance.
        add("player.vanish.usage-console", "&eUsage: /vanish <player>", "&eBenutzung: /vanish <Spieler>");
        add("player.vanish.target-not-found", "{prefix}&8[&dSTAFF&8] &cPlayer &6{player}&c is not online.", "{prefix}&8[&dSTAFF&8] &cSpieler &6{player}&c ist nicht online.");
        add("player.vanish.enabled",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lEnabled", "{prefix}&7You are now invisible to other players."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lAktiviert", "{prefix}&7Du bist jetzt fuer andere Spieler unsichtbar."));
        add("player.vanish.disabled",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDisabled", "{prefix}&7You are visible again."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDeaktiviert", "{prefix}&7Du bist wieder sichtbar."));
        add("player.vanish.enabled-other",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lEnabled", "{prefix}&7Vanish enabled for &6{target}&7."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lAktiviert", "{prefix}&7Vanish fuer &6{target}&7 aktiviert."));
        add("player.vanish.disabled-other",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDisabled", "{prefix}&7Vanish disabled for &6{target}&7."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDeaktiviert", "{prefix}&7Vanish fuer &6{target}&7 deaktiviert."));
        add("player.vanish.enabled-target",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lEnabled &7by &6{sender}", "{prefix}&7You are now invisible to other players."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &a&lAktiviert &7von &6{sender}", "{prefix}&7Du bist jetzt fuer andere Spieler unsichtbar."));
        add("player.vanish.disabled-target",
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDisabled &7by &6{sender}", "{prefix}&7You are visible again."),
                Arrays.asList("{prefix}&8[&dSTAFF&8] &eVanish &8→ &c&lDeaktiviert &7von &6{sender}", "{prefix}&7Du bist wieder sichtbar."));
        add("player.vanish.console-log", "&8[&b{time}&8] &6[STAFF] &eVANISH &7| &f{target} &8» {state} &7({detail})", "&8[&b{time}&8] &6[STAFF] &eVANISH &7| &f{target} &8» {state} &7({detail})");
        add("player.vanish.console-state-on", "&aenabled", "&aaktiviert");
        add("player.vanish.console-state-off", "&cdisabled", "&cdeaktiviert");
        add("player.vanish.console-detail-self", "self", "selbst");
        add("player.vanish.console-detail-other", "by {sender}", "von {sender}");
        // Action-bar indicators refreshed once per second while the operator
        // is vanished. Kept short and column-aligned ("● TAG | TIER | NOTE")
        // so they read at a glance without crowding the screen.
        add("player.vanish.action-bar.lower",
                "&a● &7VANISHED &8| &fLOWER TIER &8| &7Visible to staff",
                "&a● &7UNSICHTBAR &8| &fNIEDRIGE STUFE &8| &7Sichtbar fuer Stab");
        add("player.vanish.action-bar.higher",
                "&c● &7VANISHED &8| &fHIGHER TIER &8| &7Invisible to all",
                "&c● &7UNSICHTBAR &8| &fHOEHERE STUFE &8| &7Unsichtbar fuer alle");

        // Inventory see (staff) — same [STAFF] badge applied so the tool
        // origin is obvious in chat.
        // Usage line drops the [STAFF] tag — it's the same form most other
        // commands use for usage hints, so keeping the styling consistent
        // matters more than re-flagging the staff tool here.
        // Invsee live-mirror chest GUI title. Bukkit pre-1.13 caps inventory
        // titles at 32 visible chars, and the title is built as
        // "{player}'s Inventory" in EN / "Inventar von {player}" in DE — both
        // stay under the cap for usernames up to ~16 chars (Minecraft
        // username limit) including formatting, with InvSeeMenu trimming
        // any overflow defensively.
        add("player.invsee.menu.title",
                "&6&l{player}&6's Inventory",
                "&6&lInventar von {player}");
        add("player.invsee.usage",
                usageBox("Players", "InvSee", "/invsee <player>", "View another player's inventory."),
                usageBox("Spieler", "InvSee", "/invsee <Spieler>", "Zeigt das Inventar eines anderen Spielers."));
        add("player.invsee.usage-console", "&eOnly players can run /invsee.", "&eNur Spieler koennen /invsee verwenden.");
        add("player.invsee.target-not-found", "{prefix}&8[&dSTAFF&8] &cPlayer &6{player}&c is not online.", "{prefix}&8[&dSTAFF&8] &cSpieler &6{player}&c ist nicht online.");
        add("player.invsee.cannot-view-self", "{prefix}&8[&dSTAFF&8] &eOnly the full-tier permission can /invsee yourself.", "{prefix}&8[&dSTAFF&8] &eNur die volle Berechtigungsstufe kann /invsee auf sich selbst anwenden.");
        add("player.invsee.no-permission-target", "{prefix}&8[&dSTAFF&8] &cYou cannot view &6{player}&c's inventory at your permission tier.", "{prefix}&8[&dSTAFF&8] &cDu kannst das Inventar von &6{player}&c auf deiner Berechtigungsstufe nicht ansehen.");
        add("player.invsee.opened", "{prefix}&8[&dSTAFF&8] &eInvsee &8→ &fViewing &6{player}&f's inventory.", "{prefix}&8[&dSTAFF&8] &eInvsee &8→ &fInventar von &6{player}&f wird angesehen.");
        add("player.invsee.opened-self", "{prefix}&8[&dSTAFF&8] &eInvsee &8→ &fViewing your own inventory.", "{prefix}&8[&dSTAFF&8] &eInvsee &8→ &fEigenes Inventar wird angesehen.");
        add("player.invsee.open-failed", "{prefix}&8[&dSTAFF&8] &cCould not open &6{player}&c's inventory.", "{prefix}&8[&dSTAFF&8] &cInventar von &6{player}&c konnte nicht geoeffnet werden.");
        add("player.invsee.console-log", "&8[&b{time}&8] &6[STAFF] &eINVSEE &7({tier}&7) &7| &f{viewer} &8» &fopened inventory of &6{target}", "&8[&b{time}&8] &6[STAFF] &eINVSEE &7({tier}&7) &7| &f{viewer} &8» &fInventar geoeffnet von &6{target}");
        add("player.invsee.console-tier-basic", "&7BASIC", "&7BASIS");
        add("player.invsee.console-tier-full", "&dFULL", "&dVOLL");
        add("player.moderation.usage.ban",
                usageBox("Moderation", "Ban", "/ban <player> [reason]", "Permanently ban a player from the server."),
                usageBox("Moderation", "Bann", "/ban <Spieler> [Grund]", "Bannt einen Spieler dauerhaft vom Server."));
        add("player.moderation.usage.unban",
                usageBox("Moderation", "Unban", "/unban <player> [reason]", "Remove an active ban from a player."),
                usageBox("Moderation", "Entbannen", "/unban <Spieler> [Grund]", "Hebt einen aktiven Bann eines Spielers auf."));
        add("player.moderation.usage.kick",
                usageBox("Moderation", "Kick", "/kick <player> [reason]", "Kick an online player from the server."),
                usageBox("Moderation", "Kick", "/kick <Spieler> [Grund]", "Wirft einen Online-Spieler vom Server."));
        add("player.moderation.usage.mute",
                usageBox("Moderation", "Mute", "/mute <player> [reason]", "Prevent a player from chatting."),
                usageBox("Moderation", "Stummschalten", "/mute <Spieler> [Grund]", "Verhindert, dass ein Spieler chatten kann."));
        add("player.moderation.usage.unmute",
                usageBox("Moderation", "Unmute", "/unmute <player> [reason]", "Remove an active mute from a player."),
                usageBox("Moderation", "Entstummen", "/unmute <Spieler> [Grund]", "Hebt die Stummschaltung eines Spielers auf."));
        add("player.moderation.usage.tempban",
                usageBox("Moderation", "Temp-Ban", "/tempban <player> [reason]", "Temporarily ban a player using the default duration."),
                usageBox("Moderation", "Temp-Bann", "/tempban <Spieler> [Grund]", "Bannt einen Spieler vorübergehend (Standarddauer)."));
        add("player.moderation.usage.warn",
                usageBox("Moderation", "Warn", "/warn <player> [reason]", "Issue a staff warning to a player."),
                usageBox("Moderation", "Verwarnen", "/warn <Spieler> [Grund]", "Verwarnt einen Spieler."));
        add("player.moderation.usage.status",
                usageBox("Moderation", "Status", "/status <player>", "View a player's moderation profile and history."),
                usageBox("Moderation", "Status", "/status <Spieler>", "Zeigt das Moderationsprofil eines Spielers."));
        add("player.moderation.target-not-found", "{prefix}&cPlayer profile &6{player}&c was not found.", "{prefix}&cSpielerprofil &6{player}&c wurde nicht gefunden.");
        add("player.moderation.self-target", "{prefix}&cYou cannot target yourself with that moderation action.", "{prefix}&cDu kannst dich mit dieser Moderationsaktion nicht selbst anvisieren.");
        add("player.moderation.ban.success", "{prefix}&cBanned &6{target}&c. Reason: &f{reason}", "{prefix}&c&6{target}&c wurde gebannt. Grund: &f{reason}");
        add("player.moderation.ban.already", "{prefix}&e&6{target} &eis already banned.", "{prefix}&e&6{target} &eist bereits gebannt.");
        add("player.moderation.ban.kick-message", "&cYou have been banned from this server.\n&7Reason: &f{reason}", "&cDu wurdest von diesem Server gebannt.\n&7Grund: &f{reason}");
        add("player.moderation.tempban.success", "{prefix}&cTemp-banned &6{target}&c for &6{duration}&c.", "{prefix}&c&6{target}&c wurde fuer &6{duration} &ctemporär gebannt.");
        add("player.moderation.tempban.already", "{prefix}&e&6{target} &ealready has an active ban.", "{prefix}&eFuer &6{target} &eist bereits ein aktiver Bann vorhanden.");
        add("player.moderation.tempban.kick-message", "&cYou have been temporarily banned.\n&7Duration: &f{duration}\n&7Reason: &f{reason}", "&cDu wurdest temporaer gebannt.\n&7Dauer: &f{duration}\n&7Grund: &f{reason}");
        add("player.moderation.unban.success", "{prefix}&aRemoved the ban for &6{target}&a.", "{prefix}&aDer Bann fuer &6{target} &awurde entfernt.");
        add("player.moderation.unban.not-banned", "{prefix}&e&6{target} &eis not currently banned.", "{prefix}&e&6{target} &eist aktuell nicht gebannt.");
        add("player.moderation.kick.success", "{prefix}&eKicked &6{target}&e. Reason: &f{reason}", "{prefix}&e&6{target}&e wurde gekickt. Grund: &f{reason}");
        add("player.moderation.kick.kick-message", "&cYou were kicked from this server.\n&7Reason: &f{reason}", "&cDu wurdest von diesem Server gekickt.\n&7Grund: &f{reason}");
        add("player.moderation.mute.success", "{prefix}&eMuted &6{target}&e. Reason: &f{reason}", "{prefix}&e&6{target}&e wurde stummgeschaltet. Grund: &f{reason}");
        add("player.moderation.mute.target", "{prefix}&cYou were muted by &6{sender}&c. Reason: &f{reason}", "{prefix}&cDu wurdest von &6{sender}&c stummgeschaltet. Grund: &f{reason}");
        add("player.moderation.mute.already", "{prefix}&e&6{target} &eis already muted.", "{prefix}&e&6{target} &eist bereits stummgeschaltet.");
        add("player.moderation.mute.chat-blocked", "{prefix}&cYou are muted. Reason: &f{reason}", "{prefix}&cDu bist stummgeschaltet. Grund: &f{reason}");
        add("player.moderation.unmute.success", "{prefix}&aUnmuted &6{target}&a.", "{prefix}&a&6{target}&a wurde entstummt.");
        add("player.moderation.unmute.target", "{prefix}&aYou were unmuted by &6{sender}&a.", "{prefix}&aDu wurdest von &6{sender}&a entstummt.");
        add("player.moderation.unmute.not-muted", "{prefix}&e&6{target} &eis not muted.", "{prefix}&e&6{target} &eist nicht stummgeschaltet.");
        add("player.moderation.warn.success", "{prefix}&eWarned &6{target}&e. Total warnings: &6{count}", "{prefix}&e&6{target}&e wurde verwarnt. Gesamtwarnungen: &6{count}");
        add("player.moderation.warn.target", "{prefix}&cYou have been warned by &6{sender}&c. Reason: &f{reason}", "{prefix}&cDu wurdest von &6{sender}&c verwarnt. Grund: &f{reason}");
        add("player.moderation.banlist.header", "{prefix}&6Active bans: &e{count}", "{prefix}&6Aktive Baenne: &e{count}");
        add("player.moderation.banlist.none", "{prefix}&eThere are no active bans.", "{prefix}&eEs gibt keine aktiven Baenne.");
        add("player.moderation.banlist.entry", "{prefix}&e- &6{target} &7| &eBy: &f{source} &7| &eExpires: &f{expires} &7| &eReason: &f{reason}", "{prefix}&e- &6{target} &7| &eVon: &f{source} &7| &eEndet: &f{expires} &7| &eGrund: &f{reason}");
        add("player.moderation.banlist.permanent", "Permanent", "Permanent");
        // /status report layout — clean "card" style: the header line is the
        // ONLY one that carries the {prefix}, every other line drops it and
        // uses a left-border "│" plus a small section glyph (●, ▸, •) so the
        // whole report reads as one indented block with visible structure
        // instead of as N independent prefix-stamped chat lines. Reasons for
        // the active ban / mute / last warning / last action live on their
        // own indented "└" continuation line so long reasons wrap cleanly
        // and the headline stays scannable.
        add("player.moderation.status.header", Arrays.asList(
                "",
                BOX_TITLE + "Player Status  &8·  &f{target}",
                BOX_RULE
        ), Arrays.asList(
                "",
                BOX_TITLE + "Spielerstatus  &8·  &f{target}",
                BOX_RULE
        ));
        add("player.moderation.status.summary",
                "  &7UUID &8› &f{uuid}  &8·  &7Profile &8› &f{profileType}  &8·  &7Session &8› &f{online}  &8·  &7Updated &8› &f{updated}",
                "  &7UUID &8› &f{uuid}  &8·  &7Profil &8› &f{profileType}  &8·  &7Status &8› &f{online}  &8·  &7Aktualisiert &8› &f{updated}");
        add("player.moderation.status.ban-active",
                "  &c● &7Ban  &8›  &cACTIVE  &8·  &f{type}  &8·  &7by &f{actor}  &8·  &7expires &f{expires}",
                "  &c● &7Bann  &8›  &cAKTIV  &8·  &f{type}  &8·  &7von &f{actor}  &8·  &7endet &f{expires}");
        add("player.moderation.status.ban-clear",
                "  &a● &7Ban  &8›  &aClear",
                "  &a● &7Bann  &8›  &aFrei");
        add("player.moderation.status.ban-reason",
                "      &8└ &7Reason &8› &f{reason}",
                "      &8└ &7Grund &8› &f{reason}");
        add("player.moderation.status.mute-active",
                "  &e● &7Mute  &8›  &eACTIVE  &8·  &7by &f{actor}  &8·  &7issued &f{issuedAt}",
                "  &e● &7Mute  &8›  &eAKTIV  &8·  &7von &f{actor}  &8·  &7seit &f{issuedAt}");
        add("player.moderation.status.mute-clear",
                "  &a● &7Mute  &8›  &aClear",
                "  &a● &7Mute  &8›  &aFrei");
        add("player.moderation.status.mute-reason",
                "      &8└ &7Reason &8› &f{reason}",
                "      &8└ &7Grund &8› &f{reason}");
        add("player.moderation.status.counts-primary",
                "  &6▸ &7Totals  &8›  &fBans {bans}  &8·  &fTempbans {tempbans}  &8·  &fKicks {kicks}  &8·  &fWarnings {warnings}",
                "  &6▸ &7Summen  &8›  &fBanns {bans}  &8·  &fTempbanns {tempbans}  &8·  &fKicks {kicks}  &8·  &fWarnungen {warnings}");
        add("player.moderation.status.counts-secondary",
                "        &fMutes {mutes}  &8·  &fUnmutes {unmutes}  &8·  &fUnbans {unbans}",
                "        &fMutes {mutes}  &8·  &fEntstummt {unmutes}  &8·  &fEntbannt {unbans}");
        add("player.moderation.status.last-warning",
                "  &e▸ &7Last Warning  &8›  &f{issuedAt}  &8·  &7by &f{actor}  &8·  &7Reason &8› &f{reason}",
                "  &e▸ &7Letzte Warnung  &8›  &f{issuedAt}  &8·  &7von &f{actor}  &8·  &7Grund &8› &f{reason}");
        add("player.moderation.status.last-warning-none",
                "  &7▸ Last Warning  &8›  &8None on record",
                "  &7▸ Letzte Warnung  &8›  &8Keine gespeichert");
        add("player.moderation.status.last-action",
                "  &6▸ &7Last Action  &8›  &f{action}  &8·  &f{issuedAt}  &8·  &7by &f{actor}  &8·  &7Reason &8› &f{reason}",
                "  &6▸ &7Letzte Aktion  &8›  &f{action}  &8·  &f{issuedAt}  &8·  &7von &f{actor}  &8·  &7Grund &8› &f{reason}");
        add("player.moderation.status.last-action-none",
                "  &7▸ Last Action  &8›  &8No actions recorded",
                "  &7▸ Letzte Aktion  &8›  &8Keine Aktionen gespeichert");
        add("player.moderation.status.recent-header",
                "  &6▸ &7Recent Actions  &8›  &e({count})",
                "  &6▸ &7Letzte Aktionen  &8›  &e({count})");
        add("player.moderation.status.recent-entry",
                "    &8• &f{action}  &8·  &7{issuedAt}  &8·  &7by &f{actor}  &8·  &f{reason}",
                "    &8• &f{action}  &8·  &7{issuedAt}  &8·  &7von &f{actor}  &8·  &f{reason}");
        add("player.moderation.status.recent-none",
                "    &8• &7No recent moderation history.",
                "    &8• &7Keine aktuelle Moderationshistorie gespeichert.");
        add("player.moderation.status.value.unknown", "Unknown", "Unbekannt");
        add("player.moderation.status.value.online", "Online", "Online");
        add("player.moderation.status.value.offline", "Offline", "Offline");
        add("player.moderation.status.value.profile-online", "Live Player", "Live-Spieler");
        add("player.moderation.status.value.profile-offline", "Stored Profile", "Gespeichertes Profil");
        add("player.moderation.status.value.profile-fake", "Fake Test Profile", "Fake-Testprofil");
        add("player.moderation.status.value.temporary", "Temporary", "Temporär");
        add("player.moderation.status.value.permanent", "Permanent", "Permanent");
        add("player.moderation.status.value.action-ban", "Banned", "Gebannt");
        add("player.moderation.status.value.action-tempban", "Temp-Banned", "Temporär gebannt");
        add("player.moderation.status.value.action-unban", "Unbanned", "Entbannt");
        add("player.moderation.status.value.action-kick", "Kicked", "Gekickt");
        add("player.moderation.status.value.action-mute", "Muted", "Stummgeschaltet");
        add("player.moderation.status.value.action-unmute", "Unmuted", "Entstummt");
        add("player.moderation.status.value.action-warn", "Warned", "Verwarnt");
        add("player.moderation.status.value.action-updated", "Updated", "Aktualisiert");

        // Admin + control actions
        add("admin.confirm.stop", "{prefix}&cStopping server...", "{prefix}&cServer wird gestoppt...");
        add("admin.confirm.stop-prompt", "{prefix}&eClick again to confirm server stop.", "{prefix}&eKlicke erneut, um den Server-Stop zu bestätigen.");
        add("admin.joinlock.enabled", "{prefix}&eJoin lock {state}.", "{prefix}&eJoin-Sperre {state}.");
        add("admin.joinlock.state.enabled", "enabled", "aktiviert");
        add("admin.joinlock.state.disabled", "disabled", "deaktiviert");
        add("admin.joinlock.kick-reason", "Server temporarily locked. Try again later.", "Server vorübergehend gesperrt. Versuche es später erneut.");
        add("admin.joinlock.locked-kick", "Server is temporarily locked. Please try again later.", "Der Server ist vorübergehend gesperrt. Bitte später erneut versuchen.");
        add("admin.whitelist.toggled", "{prefix}&eWhitelist {state}.", "{prefix}&eWhitelist {state}.");
        add("admin.whitelist.state.enabled", "enabled", "aktiviert");
        add("admin.whitelist.state.disabled", "disabled", "deaktiviert");
        add("admin.max-players.updated", "{prefix}&eAdjusted max players by &6{delta}&e. New max: &6{max}", "{prefix}&eMaximale Spieler um &6{delta} &eangepasst. Neuer Wert: &6{max}");
        add("admin.kick-nonops.reason", "Kicked by administrator.", "Vom Administrator gekickt.");
        add("admin.kill.no-target", "{prefix}&eNo valid target in sight within &6{range}&e blocks.", "{prefix}&eKein gueltiges Ziel in Sichtweite (&6{range}&e Bloecke).");
        add("admin.kill.success", "{prefix}&eKilled &6{target}&e.", "{prefix}&e&6{target}&e wurde getoetet.");
        add("admin.kill.mode.enabled", "{prefix}&eKill mode enabled. Left-click to execute.", "{prefix}&eKill-Modus aktiviert. Linksklick zum Ausfuehren.");
        add("admin.kill.mode.disabled", "{prefix}&eKill mode disabled.", "{prefix}&eKill-Modus deaktiviert.");
        add("admin.kick-nonops.done", "{prefix}&cKicked all non-ops.", "{prefix}&cAlle Nicht-OPs wurden gekickt.");
        add("admin.spectator.applied", "{prefix}&bSet non-ops to Spectator mode.", "{prefix}&bNicht-OPs in den Zuschauermodus gesetzt.");
        add("admin.performance.tps", "{prefix}&eTPS: &6{tps}", "{prefix}&eTPS: &6{tps}");
        add("admin.performance.tps-missing", "{prefix}&cTPS not available on this server.", "{prefix}&cTPS sind auf diesem Server nicht verfügbar.");
        add("admin.performance.cleared", "{prefix}&eCleared &6{removed} &eentities (&6{mode}&e).", "{prefix}&e&6{removed} &eEntitäten gelöscht (&6{mode}&e).");
        add("admin.performance.mode.all", "all entities", "alle Entitäten");
        add("admin.performance.mode.mobs", "mobs only", "nur Mobs");
        add("admin.performance.mode.items", "items only", "nur Items");
        add("admin.world.saved", "{prefix}&eSaved all worlds.", "{prefix}&eAlle Welten gespeichert.");
        add("admin.world.autosave.status", "{prefix}&eAuto-save {state} for all worlds.", "{prefix}&eAuto-Speichern für alle Welten {state}.");
        add("admin.world.autosave.state.enabled", "enabled", "aktiviert");
        add("admin.world.autosave.state.disabled", "disabled", "deaktiviert");
        add("admin.world.border-placeholder", "{prefix}&eWorld border controls are a placeholder.", "{prefix}&eWelträndern-Steuerung ist noch ein Platzhalter.");
        add("admin.plugin.reloaded", "{prefix}&eReloaded SF-Core configs.", "{prefix}&eSF-Core-Konfigurationen neu geladen.");
        add("admin.plugin.reloaded-console", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8  &8➠ &6SF-Core &econfigs were reloaded by &6{player}&e.", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8  &8➠ &6SF-Core &eKonfigurationen wurden von &6{player}&e neu geladen.");
        add("admin.plugin.modules-placeholder", "{prefix}&eModule toggles are a placeholder.", "{prefix}&eModul-Umschalter sind noch ein Platzhalter.");
        add("admin.modules.joinleave.enabled", "{prefix}&eJoin/leave broadcasts &aenabled&e.", "{prefix}&eJoin/Leave-Nachrichten &aaktiviert&e.");
        add("admin.modules.joinleave.disabled", "{prefix}&eJoin/leave broadcasts &cdisabled&e.", "{prefix}&eJoin/Leave-Nachrichten &cdeaktiviert&e.");
        add("admin.modules.joinleave.status", "{prefix}&eJoin/leave broadcasts: &f{state}", "{prefix}&eJoin/Leave-Nachrichten: &f{state}");
        add("admin.modules.joinleave.usage",
                usageBox("Admin", "Join/Leave", "/sf joinleave <on|off|status>", "Toggle the join/leave broadcast module."),
                usageBox("Admin", "Join/Leave", "/sf joinleave <an|aus|status>", "Schaltet die Join-/Leave-Nachrichten um."));
        add("admin.modules.joinmotd.enabled", "{prefix}&eIn-game join MOTD &aenabled&e.", "{prefix}&eIn-Game Join-MOTD &aaktiviert&e.");
        add("admin.modules.joinmotd.disabled", "{prefix}&eIn-game join MOTD &cdisabled&e.", "{prefix}&eIn-Game Join-MOTD &cdeaktiviert&e.");
        add("admin.modules.joinmotd.status", "{prefix}&eIn-game join MOTD: &f{state}", "{prefix}&eIn-Game Join-MOTD: &f{state}");
        add("admin.modules.joinmotd.usage",
                usageBox("Admin", "Join MOTD", "/sf joinmotd <on|off|status>", "Toggle the in-game welcome MOTD module."),
                usageBox("Admin", "Join MOTD", "/sf joinmotd <an|aus|status>", "Schaltet die Willkommens-MOTD um."));
        add("admin.modules.state.enabled", "enabled", "aktiviert");
        add("admin.modules.state.disabled", "disabled", "deaktiviert");

        // Staff overview menu (/staff) — main GUI titles, head lore, search,
        // and per-player action sub-menu strings.
        add("admin.staff.menu.title", "&6&lStaff Menu", "&6&lStab-Menue");
        add("admin.staff.menu.title-paginated",
                "&6&lStaff Menu &8(&e{page}&8/&e{pages}&8)",
                "&6&lStab-Menue &8(&e{page}&8/&e{pages}&8)");
        add("admin.staff.action.title", "&6&l{player} &7- &eActions", "&6&l{player} &7- &eAktionen");
        add("admin.staff.player-head.name", "&6&l{player}", "&6&l{player}");
        add("admin.staff.player-head.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Username: &f{player}",
                "&7First Joined: &f{joined}",
                "&7Total Active: &f{active}",
                "&7This Session: &f{session}",
                "&7Country: &f{country}",
                "&7Language: &f{language}",
                "&8&m─────────────────",
                "&6Report Card",
                "&7Warnings: &f{warnings}",
                "&7Mutes: &f{mutes}",
                "&7Kicks: &f{kicks}",
                "&7Tempbans: &f{tempbans}",
                "&7Bans: &f{bans}",
                "&8&m─────────────────",
                "&eClick&7 to open action menu."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Benutzername: &f{player}",
                "&7Erstmals beigetreten: &f{joined}",
                "&7Gesamt-Spielzeit: &f{active}",
                "&7Diese Sitzung: &f{session}",
                "&7Land: &f{country}",
                "&7Sprache: &f{language}",
                "&8&m─────────────────",
                "&6Ahndungs-Bilanz",
                "&7Verwarnungen: &f{warnings}",
                "&7Stummschaltungen: &f{mutes}",
                "&7Kicks: &f{kicks}",
                "&7Tempbans: &f{tempbans}",
                "&7Banns: &f{bans}",
                "&8&m─────────────────",
                "&eKlick&7 oeffnet das Aktionsmenue."
        ));
        add("admin.staff.viewer-head.name", "&6&l{player} &7(&aYou&7)", "&6&l{player} &7(&aDu&7)");
        add("admin.staff.viewer-head.lore", Arrays.asList(
                "&8&m─────────────────",
                "&aOperator &7- this menu is yours.",
                "&8&m─────────────────",
                "&7Username: &f{player}",
                "&7First Joined: &f{joined}",
                "&7Total Active: &f{active}",
                "&7This Session: &f{session}",
                "&7Country: &f{country}",
                "&7Language: &f{language}",
                "&8&m─────────────────",
                "&6Report Card",
                "&7Warnings: &f{warnings}",
                "&7Mutes: &f{mutes}",
                "&7Kicks: &f{kicks}",
                "&7Tempbans: &f{tempbans}",
                "&7Bans: &f{bans}",
                "&8&m─────────────────",
                "&7Shown for accountability.",
                "&8Click does nothing."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&aOperator &7- dieses Menue gehoert dir.",
                "&8&m─────────────────",
                "&7Benutzername: &f{player}",
                "&7Erstmals beigetreten: &f{joined}",
                "&7Gesamt-Spielzeit: &f{active}",
                "&7Diese Sitzung: &f{session}",
                "&7Land: &f{country}",
                "&7Sprache: &f{language}",
                "&8&m─────────────────",
                "&6Ahndungs-Bilanz",
                "&7Verwarnungen: &f{warnings}",
                "&7Stummschaltungen: &f{mutes}",
                "&7Kicks: &f{kicks}",
                "&7Tempbans: &f{tempbans}",
                "&7Banns: &f{bans}",
                "&8&m─────────────────",
                "&7Angezeigt zur Nachvollziehbarkeit.",
                "&8Klick hat keine Wirkung."
        ));
        add("admin.staff.prev-page.name",
                "&a◀ Previous Page",
                "&a◀ Vorherige Seite");
        add("admin.staff.prev-page.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Currently on page &f{page}&7/&f{pages}&7.",
                "&7Click to jump to page &f{target}&7."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Du bist auf Seite &f{page}&7/&f{pages}&7.",
                "&7Klicke, um zu Seite &f{target} &7zu springen."
        ));
        add("admin.staff.next-page.name",
                "&aNext Page ▶",
                "&aNaechste Seite ▶");
        add("admin.staff.next-page.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Currently on page &f{page}&7/&f{pages}&7.",
                "&7Click to jump to page &f{target}&7."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Du bist auf Seite &f{page}&7/&f{pages}&7.",
                "&7Klicke, um zu Seite &f{target} &7zu springen."
        ));
        add("admin.staff.search.name", "&eSearch Player...", "&eSpieler suchen...");
        add("admin.staff.search.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Click to be prompted in chat",
                "&7for an online player name.",
                " ",
                "&7Type &fcancel&7 to abort."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Klicke, um im Chat nach",
                "&7einem Online-Spielernamen gefragt zu werden.",
                " ",
                "&7Tippe &fcancel&7 zum Abbrechen."
        ));
        add("admin.staff.search.prompt",
                "{prefix}&7Type a player name to open their action menu, or &fcancel&7 to abort.",
                "{prefix}&7Tippe einen Spielernamen, um sein Aktionsmenue zu oeffnen, oder &fcancel&7 zum Abbrechen.");
        add("admin.staff.search.success",
                "{prefix}&aOpening action menu for &f{player}&a...",
                "{prefix}&aOeffne Aktionsmenue fuer &f{player}&a...");
        add("admin.staff.search.not-online",
                "{prefix}&cPlayer &f{player}&c is not online.",
                "{prefix}&cSpieler &f{player}&c ist nicht online.");
        add("admin.staff.search.cancelled",
                "{prefix}&7Search cancelled.",
                "{prefix}&7Suche abgebrochen.");
        add("admin.staff.search.empty",
                "{prefix}&cYou must enter a player name.",
                "{prefix}&cDu musst einen Spielernamen eingeben.");
        add("admin.staff.search.self",
                "{prefix}&cYou cannot open the staff action menu for yourself.",
                "{prefix}&cDu kannst das Stab-Aktionsmenue nicht fuer dich selbst oeffnen.");
        add("admin.staff.close.name", "&cClose", "&cSchliessen");
        add("admin.staff.close.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Click to close this menu."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Klicke, um dieses Menue zu schliessen."
        ));
        add("admin.staff.back.name", "&eBack", "&eZurueck");
        add("admin.staff.back.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Return to the staff overview."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Zurueck zum Stab-Ueberblick."
        ));
        add("admin.staff.action.warn.name", "&eWarn &f{player}", "&eVerwarne &f{player}");
        add("admin.staff.action.warn.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Issue a staff warning to",
                "&f{player}&7.",
                " ",
                "&8Placeholder &7- not yet wired."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Sende &f{player}&7 eine",
                "&7Stab-Verwarnung.",
                " ",
                "&8Platzhalter &7- noch nicht aktiv."
        ));
        add("admin.staff.action.mute.name", "&eMute &f{player}", "&eStumm &f{player}");
        add("admin.staff.action.mute.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Mute &f{player}&7 in chat.",
                " ",
                "&8Placeholder &7- not yet wired."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Stumme &f{player}&7 im Chat.",
                " ",
                "&8Platzhalter &7- noch nicht aktiv."
        ));
        add("admin.staff.action.kick.name", "&eKick &f{player}", "&eKicke &f{player}");
        add("admin.staff.action.kick.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Kick &f{player}&7 from the server.",
                " ",
                "&8Placeholder &7- not yet wired."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Kicke &f{player}&7 vom Server.",
                " ",
                "&8Platzhalter &7- noch nicht aktiv."
        ));
        add("admin.staff.action.tempban.name", "&eTempban &f{player}", "&eTempban &f{player}");
        add("admin.staff.action.tempban.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Apply a temporary ban to",
                "&f{player}&7.",
                " ",
                "&8Placeholder &7- not yet wired."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Verhaenge einen temporaeren",
                "&7Bann gegen &f{player}&7.",
                " ",
                "&8Platzhalter &7- noch nicht aktiv."
        ));
        add("admin.staff.action.ban.name", "&cBan &f{player}", "&cBann &f{player}");
        add("admin.staff.action.ban.lore", Arrays.asList(
                "&8&m─────────────────",
                "&7Permanently ban",
                "&f{player}&7.",
                " ",
                "&8Placeholder &7- not yet wired."
        ), Arrays.asList(
                "&8&m─────────────────",
                "&7Verbanne &f{player}&7",
                "&7dauerhaft.",
                " ",
                "&8Platzhalter &7- noch nicht aktiv."
        ));
        add("admin.staff.action.warn.placeholder",
                "{prefix}&eWarn flow for &f{player}&e is not yet implemented.",
                "{prefix}&eVerwarn-Ablauf fuer &f{player}&e ist noch nicht implementiert.");
        add("admin.staff.action.mute.placeholder",
                "{prefix}&eMute flow for &f{player}&e is not yet implemented.",
                "{prefix}&eStumm-Ablauf fuer &f{player}&e ist noch nicht implementiert.");
        add("admin.staff.action.kick.placeholder",
                "{prefix}&eKick flow for &f{player}&e is not yet implemented.",
                "{prefix}&eKick-Ablauf fuer &f{player}&e ist noch nicht implementiert.");
        add("admin.staff.action.tempban.placeholder",
                "{prefix}&eTempban flow for &f{player}&e is not yet implemented.",
                "{prefix}&eTempban-Ablauf fuer &f{player}&e ist noch nicht implementiert.");
        add("admin.staff.action.ban.placeholder",
                "{prefix}&eBan flow for &f{player}&e is not yet implemented.",
                "{prefix}&eBann-Ablauf fuer &f{player}&e ist noch nicht implementiert.");

        add("commands.help.gui.title", "&6Command Help &7(&e{page}&7/&e{pages}&7) &8· &e{category}", "&6Befehlshilfe &7(&e{page}&7/&e{pages}&7) &8· &e{category}");
        add("commands.help.gui.item-name", "&6/{command}", "&6/{command}");
        add("commands.help.gui.hover-command", "&7Command: &e/{command}", "&7Befehl: &e/{command}");
        add("commands.help.gui.hover-category", "&7Category: &e{category}", "&7Kategorie: &e{category}");
        add("commands.help.gui.hover-description", "&7Description:", "&7Beschreibung:");
        add("commands.help.gui.hover-permission", "&7Permission: &f{permission}", "&7Berechtigung: &f{permission}");
        add("commands.help.gui.hover-usage", "&7Usage: &f{usage}", "&7Benutzung: &f{usage}");
        add("commands.help.gui.hover-click", "&aClick to suggest the command", "&aZum Vorschlagen klicken");
        add("commands.help.gui.button.previous", "&ePrevious Page", "&eVorherige Seite");
        add("commands.help.gui.button.previous-lore", "&7Go to page &f{page}", "&7Zur Seite &f{page} wechseln");
        add("commands.help.gui.button.next", "&eNext Page", "&eNaechste Seite");
        add("commands.help.gui.button.next-lore", "&7Go to page &f{page}", "&7Zur Seite &f{page} wechseln");
        add("commands.help.gui.button.info", "&6Help", "&6Hilfe");
        add("commands.help.gui.button.close", "&cClose", "&cSchließen");
        add("commands.help.gui.button.close-lore", Arrays.asList(
                "&7Close this menu."
        ), Arrays.asList(
                "&7Schließe dieses Menü."
        ));
        add("commands.help.gui.button.info-lore", Arrays.asList(
                "&7Browse every command",
                "&7you can run on this server.",
                " ",
                "&8Sorted A-Z",
                "&8Category: &f{category}",
                "&8Page &f{page}&8/&f{pages}"
        ), Arrays.asList(
                "&7Durchstoebere alle Befehle",
                "&7die du nutzen kannst.",
                " ",
                "&8Alphabetisch sortiert",
                "&8Kategorie: &f{category}",
                "&8Seite &f{page}&8/&f{pages}"
        ));
        add("commands.help.gui.button.category", "&6Category: &e{category}", "&6Kategorie: &e{category}");
        add("commands.help.gui.button.category-lore", Arrays.asList(
                "&7Filter the visible commands",
                "&7by their category.",
                " ",
                "&7Showing &f{count}&7/&f{total} &7commands.",
                " ",
                "&eLeft-click: &7next category",
                "&eRight-click: &7previous category",
                "&eMiddle-click: &7reset to All",
                " ",
                "&8Categories:"
        ), Arrays.asList(
                "&7Filtere die sichtbaren Befehle",
                "&7nach Kategorie.",
                " ",
                "&7Zeige &f{count}&7/&f{total} &7Befehle.",
                " ",
                "&eLinksklick: &7naechste Kategorie",
                "&eRechtsklick: &7vorherige Kategorie",
                "&eMittelklick: &7zuruecksetzen auf Alle",
                " ",
                "&8Kategorien:"
        ));
        add("commands.help.gui.button.category-line", "&8 • &7{entry}", "&8 • &7{entry}");
        add("commands.help.gui.button.category-line-active", "&6 ▶ &e{entry}", "&6 ▶ &e{entry}");
        add("commands.help.gui.category.all", "All", "Alle");
        add("commands.help.gui.category.admin", "Admin", "Admin");
        add("commands.help.gui.category.core", "Core", "Kern");
        add("commands.help.gui.category.language", "Language", "Sprache");
        add("commands.help.gui.category.messaging", "Messaging", "Nachrichten");
        add("commands.help.gui.category.moderation", "Moderation", "Moderation");
        add("commands.help.gui.category.teleport", "Teleport", "Teleport");
        add("commands.help.gui.category.utility", "Utility", "Werkzeuge");
        add("commands.help.gui.category.other", "Other", "Sonstige");
        add("commands.pl.header", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fServer Plugins  &8·  &f{total} &7loaded",
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fServer-Plugins  &8·  &f{total} &7geladen",
                "&8──────────────────────────────",
                ""
        ));
        add("commands.pl.section", "  &e{platform}  &8·  &f{count}", "  &e{platform}  &8·  &f{count}");
        add("commands.pl.line", "    {plugins}", "    {plugins}");
        add("commands.pl.empty", "  &7No plugins are currently loaded.", "  &7Keine Plugins geladen.");
        add("commands.pl.footer", Arrays.asList(
                "",
                "&8──────────────────────────────",
                "  &7Status  &8·  &6▪ Active   &7▪ Disabled   &c▪ Broken    &8(&6{enabled}&8 / &7{disabled}&8 / &c{broken}&8)",
                ""
        ), Arrays.asList(
                "",
                "&8──────────────────────────────",
                "  &7Status  &8·  &6▪ Aktiv   &7▪ Deaktiviert   &c▪ Defekt    &8(&6{enabled}&8 / &7{disabled}&8 / &c{broken}&8)",
                ""
        ));
        add("commands.tps.report", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fServer Performance",
                "&8──────────────────────────────",
                "",
                "  &7TPS       &8›  <hover:show_text:'{tps_1m_tooltip}'>{tps_1m_color}{tps_1m}</hover> &7(1m)  <hover:show_text:'{tps_5m_tooltip}'>{tps_5m_color}{tps_5m}</hover> &7(5m)  <hover:show_text:'{tps_15m_tooltip}'>{tps_15m_color}{tps_15m}</hover> &7(15m)",
                "  &7MSPT      &8›  <hover:show_text:'{mspt_tooltip}'>{mspt_color}{mspt}</hover>&7ms",
                "  &7Players   &8›  &f{players} &7/ &f{max_players}",
                "  &7Uptime    &8›  &f{uptime}",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fServerleistung",
                "&8──────────────────────────────",
                "",
                "  &7TPS       &8›  <hover:show_text:'{tps_1m_tooltip}'>{tps_1m_color}{tps_1m}</hover> &7(1m)  <hover:show_text:'{tps_5m_tooltip}'>{tps_5m_color}{tps_5m}</hover> &7(5m)  <hover:show_text:'{tps_15m_tooltip}'>{tps_15m_color}{tps_15m}</hover> &7(15m)",
                "  &7MSPT      &8›  <hover:show_text:'{mspt_tooltip}'>{mspt_color}{mspt}</hover>&7ms",
                "  &7Spieler   &8›  &f{players} &7/ &f{max_players}",
                "  &7Laufzeit  &8›  &f{uptime}",
                ""
        ));
        add("admin.weather.clear", "{prefix}&bWeather set to clear.", "{prefix}&bWetter auf klar gestellt.");
        add("admin.weather.rain", "{prefix}&bWeather set to rain.", "{prefix}&bWetter auf Regen gestellt.");
        add("admin.weather.thunder", "{prefix}&bWeather set to thunder.", "{prefix}&bWetter auf Gewitter gestellt.");
        add("admin.time.morning", "{prefix}&6Time set to morning.", "{prefix}&6Zeit auf Morgen gesetzt.");
        add("admin.time.noon", "{prefix}&6Time set to noon.", "{prefix}&6Zeit auf Mittag gesetzt.");
        add("admin.time.night", "{prefix}&6Time set to night.", "{prefix}&6Zeit auf Nacht gesetzt.");
        add("admin.time.daylight.status", "{prefix}&6Daylight cycle {state}.", "{prefix}&6Tag-Nacht-Zyklus {state}.");
        add("admin.time.daylight.state.enabled", "enabled", "aktiviert");
        add("admin.time.daylight.state.frozen", "frozen", "eingefroren");
        add("admin.redstone.state-message", "{prefix}&eRedstone updates {state}.", "{prefix}&eRedstone-Updates {state}.");
        add("admin.redstone.state.frozen", "frozen", "eingefroren");
        add("admin.redstone.state.resumed", "resumed", "fortgesetzt");
        add("admin.gamerule.toggled", "{prefix}&e{rule} set to {state}.", "{prefix}&e{rule} auf {state} gesetzt.");
        add("admin.gamerule.state.on", "ON", "AN");
        add("admin.gamerule.state.off", "OFF", "AUS");
        add("admin.restart.label.quick", "Server restart", "Serverneustart");
        add("admin.restart.label.safe", "Safe restart", "Sicherer Neustart");
        add("admin.restart.countdown-start", "{prefix}&e{label} in &c{seconds} &eseconds.", "{prefix}&e{label} in &c{seconds} &eSekunden.");
        add("admin.restart.countdown-now", "{prefix}&c{label} now.", "{prefix}&c{label} jetzt.");
        add("admin.restart.actionbar", "&6{label}&7 in &c{seconds}s", "&6{label}&7 in &c{seconds}s");
        add("admin.restart.title", "&c{label}", "&c{label}");
        add("admin.restart.subtitle", "&eRestarting in {seconds}s", "&eNeustart in {seconds}s");

        // SF-Core command outputs
        add("commands.sf.unknown", "{prefix}&cUnknown subcommand. Use /sf help.", "{prefix}&cUnbekannter Unterbefehl. Nutze /sf help.");
        add("commands.sf.help.header", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fHelp  &8·  &7Page &f{page}&7/&f{pages}{category}", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fHilfe  &8·  &7Seite &f{page}&7/&f{pages}{category}");
        add("commands.sf.help.header-category", "  &8·  &e{category}", "  &8·  &e{category}");
        add("commands.sf.help.detail.title", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &f{usage}", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &f{usage}");
        add("commands.sf.help.detail.description", "  &7Description  &8›  &f{description}", "  &7Beschreibung  &8›  &f{description}");
        add("commands.sf.help.detail.category", "  &7Category     &8›  &f{category}", "  &7Kategorie     &8›  &f{category}");
        add("commands.sf.help.detail.permission", "  &7Permission   &8›  &f{permission}", "  &7Berechtigung  &8›  &f{permission}");
        add("commands.sf.help.line-suffix", "  &8›  &7{description}", "  &8›  &7{description}");
        add("commands.sf.help.hover", "&e{description}\n&6Permission: &f{permission}", "&e{description}\n&6Berechtigung: &f{permission}");
        add("commands.sf.help.nav.previous", "&6[\u25c4 Previous Page]", "&6[\u25c4 Vorherige Seite]");
        add("commands.sf.help.nav.previous-hover", Arrays.asList(
                "&ePrevious Page",
                "&7Go to page &f{page}"
        ), Arrays.asList(
                "&eVorherige Seite",
                "&7Gehe zu Seite &f{page}"
        ));
        add("commands.sf.help.nav.next", "&6[Next Page \u25ba]", "&6[Naechste Seite \u25ba]");
        add("commands.sf.help.nav.next-hover", Arrays.asList(
                "&eNext Page",
                "&7Go to page &f{page}"
        ), Arrays.asList(
                "&eNaechste Seite",
                "&7Gehe zu Seite &f{page}"
        ));
        add("commands.sf.permissions.usage", "{prefix}&eUsage: &f/sf permissions &7<&ecmd|category&7>", "{prefix}&eBenutzung: &f/sf permissions &7<&eBefehl|Kategorie&7>");
        add("commands.sf.permissions.suggestion", "{prefix}&7Try: &f/sf permissions reload", "{prefix}&7Versuche: &f/sf permissions reload");
        add("commands.sf.permissions.no-match", "{prefix}&cNo matching command or category.", "{prefix}&cKein passender Befehl oder Kategorie gefunden.");
        add("commands.sf.permissions.title", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fPermissions", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fBerechtigungen");
        add("commands.sf.permissions.entry", "  &6{usage}  &8›  &f{permission}", "  &6{usage}  &8›  &f{permission}");
        add("commands.sf.commands.none", "{prefix}&cNo commands available for you in that category.", "{prefix}&cKeine Befehle in dieser Kategorie verfügbar.");
        add("commands.sf.commands.header", "{prefix}&6Loaded commands:", "{prefix}&6Geladene Befehle:");
        add("commands.sf.commands.title", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fCommands", "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fBefehle");
        add("commands.sf.commands.entry", "  &6{usage}  &8›  &7{description}", "  &6{usage}  &8›  &7{description}");
        add("commands.sf.info.lines", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fPlugin Information",
                "&8──────────────────────────────",
                "",
                "  &f{description}",
                "",
                "  &7Version  &8›  &f{version}",
                "  &7Author   &8›  &f{authors}",
                "  &7Website  &8›  &f{website}",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fPlugin-Informationen",
                "&8──────────────────────────────",
                "",
                "  &f{description}",
                "",
                "  &7Version  &8›  &f{version}",
                "  &7Autor    &8›  &f{authors}",
                "  &7Webseite &8›  &f{website}",
                ""
        ));
        add("commands.sf.about.lines", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fAbout",
                "&8──────────────────────────────",
                "",
                "  &fEssentials-style utilities for modern & legacy servers.",
                "",
                "  &7Maintainer  &8›  &f{authors}",
                "  &7Supports    &8›  &fPaper / Spigot / PurPur / Folia  &8·  &f1.8.8 → 26.1.x",
                "  &7Website     &8›  &f{website}",
                "  &7Wiki        &8›  &fhttps://github.com/SergeantFuzzy/SF-Core/wiki",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fÜber",
                "&8──────────────────────────────",
                "",
                "  &fEssentials-ähnliche Tools für moderne & ältere Server.",
                "",
                "  &7Entwickler  &8›  &f{authors}",
                "  &7Unterstützt &8›  &fPaper / Spigot / PurPur / Folia  &8·  &f1.8.8 → 26.1.x",
                "  &7Webseite    &8›  &f{website}",
                "  &7Wiki        &8›  &fhttps://github.com/SergeantFuzzy/SF-Core/wiki",
                ""
        ));
        add("commands.sf.version", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fVersion",
                "&8──────────────────────────────",
                "",
                "  &7Version  &8›  &f{version}",
                "  &7Build    &8›  &f{tag}",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fVersion",
                "&8──────────────────────────────",
                "",
                "  &7Version  &8›  &f{version}",
                "  &7Build    &8›  &f{tag}",
                ""
        ));
        add("commands.sf.reload.config.base", "{prefix}&eConfig reloaded successfully. &7({duration})", "{prefix}&eConfig erfolgreich neu geladen. &7({duration})");
        add("commands.sf.reload.config.hover", Arrays.asList(
                "&eReloaded:",
                "&7- config.yml",
                "&7- motd.yml",
                "&7- moderation.yml",
                "&7- languages",
                "&7- data.yml (homes/spawn cache)",
                "&7Time: &f{duration}"
        ), Arrays.asList(
                "&eNeu geladen:",
                "&7- config.yml",
                "&7- motd.yml",
                "&7- moderation.yml",
                "&7- Sprachdateien",
                "&7- data.yml (Homes/Spawn Cache)",
                "&7Zeit: &f{duration}"
        ));
        add("commands.sf.reload.full.base", "{prefix}&aReload complete &8(&f{duration}&8)", "{prefix}&aNeuladung abgeschlossen &8(&f{duration}&8)");
        // Hover is built dynamically (per-component load times) from these fragments.
        add("commands.sf.reload.full.header", "&#FF4526▍ 𝗦𝗬𝗦𝗧𝗘𝗠  &8›  &fReload Complete", "&#FF4526▍ 𝗦𝗬𝗦𝗧𝗘𝗠  &8›  &fNeuladung abgeschlossen");
        add("commands.sf.reload.full.entry", "  &8• &7{file}  &8(&f{time}&8)", "  &8• &7{file}  &8(&f{time}&8)");
        add("commands.sf.reload.full.total-line", "  &7Total  &8›  &f{duration}", "  &7Gesamt  &8›  &f{duration}");
        add("commands.sf.reload.full.tip-line", "  &e&lℹ &eTip &8» &7run &f/sf diagnostics&7 for a health check.", "  &e&lℹ &eTipp &8» &7nutze &f/sf diagnostics&7 fuer einen Statuscheck.");
        add("commands.sf.reload.notify", "{prefix}&7Plugin reloaded by &f{who}&7 &8(&f{duration}&8)", "{prefix}&7Plugin neu geladen von &f{who}&7 &8(&f{duration}&8)");
        add("commands.sf.reload.file.missing", "{prefix}&cFile not found: &f{file}", "{prefix}&cDatei nicht gefunden: &f{file}");
        add("commands.sf.reload.file.success", "{prefix}&eReloaded file &6{file}&e.", "{prefix}&eDatei &6{file} &eneu geladen.");
        add("commands.sf.reload.file.hover", Arrays.asList(
                "&eReloaded file:",
                "&7Path: &f{path}",
                "&7Size: &f{size} bytes"
        ), Arrays.asList(
                "&eDatei neu geladen:",
                "&7Pfad: &f{path}",
                "&7Größe: &f{size} Bytes"
        ));
        add("commands.sf.reload.file.failure", "{prefix}&cFailed to reload &f{file}&c: &f{error}", "{prefix}&cFehler beim Neuladen von &f{file}&c: &f{error}");
        add("commands.sf.diagnostics.header", Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fDiagnostics",
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "",
                "&6▍ &6𝗦𝗙-𝗖𝗢𝗥𝗘  &8›  &fDiagnose",
                "&8──────────────────────────────",
                ""
        ));
        add("commands.sf.diagnostics.version", "  &7Version      &8›  &f{version}", "  &7Version      &8›  &f{version}");
        add("commands.sf.diagnostics.server", "  &7Server       &8›  &f{server}", "  &7Server       &8›  &f{server}");
        add("commands.sf.diagnostics.bukkit", "  &7Bukkit API   &8›  &f{bukkit}", "  &7Bukkit-API   &8›  &f{bukkit}");
        add("commands.sf.diagnostics.debug", "  &7Debug        &8›  &f{debug}", "  &7Debug        &8›  &f{debug}");
        add("commands.sf.diagnostics.data", "  &7Data folder  &8›  &f{data}", "  &7Datenordner  &8›  &f{data}");
        add("commands.sf.diagnostics.services", "  &7Services     &8›  &f{services}", "  &7Dienste      &8›  &f{services}");
        add("commands.sf.diagnostics.players", "  &7Players      &8›  &f{players}", "  &7Spieler      &8›  &f{players}");
        add("commands.sf.diagnostics.keys", "  &7Config keys  &8›  &f{keys}", "  &7Config-Keys  &8›  &f{keys}");
        add("commands.sf.updates.header", "{prefix}&6Update checks &7(placeholder)", "{prefix}&6Update-Checks &7(Platzhalter)");
        add("commands.sf.updates.listing", "{prefix}&eBuiltByBit listing is not yet live; GitHub/Spigot checks are coming soon.", "{prefix}&eBuiltByBit-Eintrag ist noch nicht live; GitHub/Spigot-Checks folgen bald.");
        add("commands.sf.updates.forced", "{prefix}&7Forced check triggered (simulated).", "{prefix}&7Erzwungener Check ausgelöst (simuliert).");
        add("commands.sf.updates.hint", "{prefix}&7Use /sf updates check to force a check or /sf updates notify to toggle notices.", "{prefix}&7Nutze /sf updates check für einen erzwungenen Check oder /sf updates notify zum Umschalten von Hinweisen.");
        add("commands.sf.updates.notify.enabled", "{prefix}&eUpdate notifications enabled.", "{prefix}&eUpdate-Hinweise aktiviert.");
        add("commands.sf.updates.notify.disabled", "{prefix}&eUpdate notifications disabled.", "{prefix}&eUpdate-Hinweise deaktiviert.");
        // NOTE: keep this a sibling of (not a child of) commands.sf.config.validation —
        // a key that is both a value and a parent becomes a YAML section on disk, reads
        // back as null, and gets perpetually "re-added" by the language self-heal.
        add("commands.sf.config.data-missing", "&cmissing", "&cfehlt");
        add("commands.sf.config.validation", Arrays.asList(
                "",
                BOX_TITLE + "Config  &8·  &fValidation",
                BOX_RULE,
                "",
                "  &econfig.yml  &8›  {config_state}",
                "  &edata.yml  &8›  {data_state}",
                "  &emotd.yml  &8›  {motd_state}",
                "  &emoderation.yml  &8›  {moderation_state}",
                "  &etablist.yml  &8›  {tablist_state}",
                "  &escoreboard.yml  &8›  {scoreboard_state}",
                ""), Arrays.asList(
                "",
                BOX_TITLE + "Config  &8·  &fValidierung",
                BOX_RULE,
                "",
                "  &econfig.yml  &8›  {config_state}",
                "  &edata.yml  &8›  {data_state}",
                "  &emotd.yml  &8›  {motd_state}",
                "  &emoderation.yml  &8›  {moderation_state}",
                "  &etablist.yml  &8›  {tablist_state}",
                "  &escoreboard.yml  &8›  {scoreboard_state}",
                ""));
        // Dynamic, paginated, link-free config-file listing (see SFCoreCommand.handleConfig).
        add("commands.sf.config.list-header", Arrays.asList(
                "",
                BOX_TITLE + "Config  &8·  &fConfiguration Files  &8·  &f{count}",
                BOX_RULE
        ), Arrays.asList(
                "",
                BOX_TITLE + "Config  &8·  &fKonfigurationsdateien  &8·  &f{count}",
                BOX_RULE
        ));
        add("commands.sf.config.list-entry", "  &8• &e{file}", "  &8• &e{file}");
        add("commands.sf.config.list-footer", "  &7Page &f{page}&7/&f{pages}  &8·  &7/sf config <page>", "  &7Seite &f{page}&7/&f{pages}  &8·  &7/sf config <Seite>");
        add("commands.sf.config.list-empty", "{prefix}&7No configuration files found.", "{prefix}&7Keine Konfigurationsdateien gefunden.");
        add("commands.sf.debug.toggled", "{prefix}&eDebug logging {state}.", "{prefix}&eDebug-Logging {state}.");
        add("commands.sf.debug.state.enabled", "enabled", "aktiviert");
        add("commands.sf.debug.state.disabled", "disabled", "deaktiviert");
        add("commands.sf.debug.report", Arrays.asList(
                "",
                BOX_TITLE + "Debug  &8·  &fStatus",
                BOX_RULE,
                "",
                "  &7Debug Logging  &8›  &f{state}",
                ""), Arrays.asList(
                "",
                BOX_TITLE + "Debug  &8·  &fStatus",
                BOX_RULE,
                "",
                "  &7Debug-Logging  &8›  &f{state}",
                ""));
        add("commands.sf.debug.dump.saved", "{prefix}&eDebug dump saved to &6{file}", "{prefix}&eDebug-Dump gespeichert als &6{file}");
        add("commands.sf.debug.dump.failed", "{prefix}&cFailed to write dump: &f{error}", "{prefix}&cFehler beim Schreiben des Dumps: &f{error}");

        // Command categories and entries (usage + description)
        add("commands.sf.category.information", "Information", "Information");
        add("commands.sf.category.reload", "Reload & Diagnostics", "Reload & Diagnose");
        add("commands.sf.category.updates", "Updates & Version", "Updates & Version");
        add("commands.sf.category.config", "Config & Debug", "Config & Debug");

        add("commands.sf.entry.help.usage", "/sf help [page|category|command]", "/sf help [Seite|Kategorie|Befehl]");
        add("commands.sf.entry.help.description", "Shows SF-Core help pages.", "Zeigt die SF-Core Hilfeseiten.");
        add("commands.sf.entry.info.usage", "/sf info", "/sf info");
        add("commands.sf.entry.info.description", "Displays general information about SF-Core.", "Zeigt allgemeine Informationen zu SF-Core.");
        add("commands.sf.entry.about.usage", "/sf about", "/sf about");
        add("commands.sf.entry.about.description", "Shows extended plugin information, credits, and links.", "Zeigt erweiterte Plugin-Informationen, Danksagungen und Links.");
        add("commands.sf.entry.permissions.usage", "/sf permissions [command|category]", "/sf permissions [Befehl|Kategorie]");
        add("commands.sf.entry.permissions.description", "Lists permission nodes for a command or category.", "Listet Berechtigungen für einen Befehl oder eine Kategorie auf.");
        add("commands.sf.entry.commands.usage", "/sf commands [category]", "/sf commands [Kategorie]");
        add("commands.sf.entry.commands.description", "Lists available commands filtered by category and permissions.", "Listet verfügbare Befehle nach Kategorie und Rechten gefiltert auf.");

        add("commands.sf.entry.reload.usage", "/sf reload", "/sf reload");
        add("commands.sf.entry.reload.description", "Reloads SF-Core configs and reinitializes all feature modules safely.", "Lädt SF-Core Configs neu und initialisiert Module neu.");
        add("commands.sf.entry.reload-config.usage", "/sf reload config", "/sf reload config");
        add("commands.sf.entry.reload-config.description", "Reloads configuration files only (core.yml + feature configs).", "Lädt nur Konfigurationsdateien neu (core.yml + Feature-Configs).");
        add("commands.sf.entry.reload-file.usage", "/sf reload <file>", "/sf reload <Datei>");
        add("commands.sf.entry.reload-file.description", "Reloads a specific file from the plugin data folder.", "Lädt eine bestimmte Datei aus dem Plugin-Datenordner neu.");
        add("commands.sf.entry.diagnostics.usage", "/sf diagnostics", "/sf diagnostics");
        add("commands.sf.entry.diagnostics.description", "Runs a quick health check (config status, loaded modules, platform info).", "Führt einen schnellen Gesundheitscheck durch (Config-Status, Module, Plattforminfo).");
        add("commands.sf.entry.diagnostics-full.usage", "/sf diagnostics full", "/sf diagnostics full");
        add("commands.sf.entry.diagnostics-full.description", "Outputs extended diagnostics including services, hooks, and errors.", "Gibt erweiterte Diagnosedaten zu Diensten, Hooks und Fehlern aus.");

        add("commands.sf.entry.version.usage", "/sf version", "/sf version");
        add("commands.sf.entry.version.description", "Shows current SF-Core version and build tag.", "Zeigt die aktuelle SF-Core Version und den Build-Tag.");
        add("commands.sf.entry.updates.usage", "/sf updates", "/sf updates");
        add("commands.sf.entry.updates.description", "Checks for available updates and displays a summary.", "Prüft auf verfügbare Updates und zeigt eine Zusammenfassung.");
        add("commands.sf.entry.updates-check.usage", "/sf updates check", "/sf updates check");
        add("commands.sf.entry.updates-check.description", "Forces an update check (placeholder).", "Erzwingt einen Update-Check (Platzhalter).");
        add("commands.sf.entry.updates-notify.usage", "/sf updates notify", "/sf updates notify");
        add("commands.sf.entry.updates-notify.description", "Toggles update notifications for the executor.", "Schaltet Update-Benachrichtigungen um.");

        add("commands.sf.entry.config.usage", "/sf config", "/sf config");
        add("commands.sf.entry.config.description", "Displays loaded config files and their status.", "Zeigt geladene Configs und deren Status.");
        add("commands.sf.entry.config-validate.usage", "/sf config validate", "/sf config validate");
        add("commands.sf.entry.config-validate.description", "Validates config files and reports errors or deprecated keys.", "Validiert Config-Dateien und meldet Fehler oder veraltete Schlüssel.");
        add("commands.sf.entry.debug.usage", "/sf debug", "/sf debug");
        add("commands.sf.entry.debug.description", "Shows debug status and active debug flags.", "Zeigt Debug-Status und aktive Debug-Flags.");
        add("commands.sf.entry.debug-enable.usage", "/sf debug enable", "/sf debug enable");
        add("commands.sf.entry.debug-enable.description", "Enables debug logging temporarily.", "Aktiviert temporär Debug-Logging.");
        add("commands.sf.entry.debug-disable.usage", "/sf debug disable", "/sf debug disable");
        add("commands.sf.entry.debug-disable.description", "Disables debug logging.", "Deaktiviert Debug-Logging.");
        add("commands.sf.entry.debug-dump.usage", "/sf debug dump", "/sf debug dump");
        add("commands.sf.entry.debug-dump.description", "Dumps internal state to a log file for troubleshooting.", "Schreibt den internen Status in eine Log-Datei für Fehleranalyse.");

        // Hub / Lobby
        add("hub.teleport.teleporting", "{prefix}&eTeleporting to the hub.", "{prefix}&eTeleportiere zum Hub.");
        add("hub.teleport.missing", "{prefix}&cNo hub world is loaded. Ask an admin to configure one.", "{prefix}&cKeine Hub-Welt geladen. Bitte einen Admin, eine zu konfigurieren.");
        add("hub.vanishall.hidden", "{prefix}&7Other players are now &fhidden&7.", "{prefix}&7Andere Spieler sind jetzt &fversteckt&7.");
        add("hub.vanishall.visible", "{prefix}&aOther players are now &fvisible&a.", "{prefix}&aAndere Spieler sind jetzt &fsichtbar&a.");
        add("hub.launchpad.cooldown", "&eLaunchpad &7» &f{seconds}s &8{bar}", "&eLaunchpad &7» &f{seconds}s &8{bar}");
        add("hub.selector.proxy-unavailable", "{prefix}&cThis server isn't connected to a proxy.", "{prefix}&cDieser Server ist mit keinem Proxy verbunden.");
        add("hub.selector.close.name", "&c&lClose", "&c&lSchließen");
        add("hub.selector.close.lore", Arrays.asList(
                "&8──────────────",
                "&7Close this menu.",
                "&8──────────────",
                "&8› &7Click to close"
        ), Arrays.asList(
                "&8──────────────",
                "&7Dieses Menü schließen.",
                "&8──────────────",
                "&8› &7Zum Schließen klicken"
        ));
        add("hub.admin.usage", "{prefix}&eUsage: &f/hub &7<on|off|toggle|reload|give|selector|menu|world ...>", "{prefix}&eBenutzung: &f/hub &7<on|off|toggle|reload|give|selector|menu|world ...>");
        add("hub.admin.give-usage", "{prefix}&eUsage: &f/hub give &7<player>", "{prefix}&eBenutzung: &f/hub give &7<Spieler>");
        add("hub.admin.enabled", "{prefix}&aHub mode &lENABLED&a.", "{prefix}&aHub-Modus &lAKTIVIERT&a.");
        add("hub.admin.disabled", "{prefix}&cHub mode &lDISABLED&c.", "{prefix}&cHub-Modus &lDEAKTIVIERT&c.");
        add("hub.admin.already-on", "{prefix}&eHub mode is already enabled.", "{prefix}&eHub-Modus ist bereits aktiviert.");
        add("hub.admin.already-off", "{prefix}&eHub mode is already disabled.", "{prefix}&eHub-Modus ist bereits deaktiviert.");
        add("hub.admin.reloaded", "{prefix}&eReloaded &fsystems/hub.yml&e.", "{prefix}&esystems/hub.yml &fneu geladen&e.");
        add("hub.admin.gave", "{prefix}&eGave hub kit to &6{player}&e.", "{prefix}&eHub-Kit an &6{player}&e ausgegeben.");
        add("hub.admin.target-offline", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("hub.admin.world.list", "{prefix}&eHub worlds: &f{worlds}", "{prefix}&eHub-Welten: &f{worlds}");
        add("hub.admin.world.added", "{prefix}&eAdded &6{world}&e to hub worlds.", "{prefix}&e&6{world}&e zu Hub-Welten hinzugefuegt.");
        add("hub.admin.world.removed", "{prefix}&eRemoved &6{world}&e from hub worlds.", "{prefix}&e&6{world}&e aus Hub-Welten entfernt.");
        add("hub.admin.world.already-listed", "{prefix}&eWorld &6{world}&e is already a hub world.", "{prefix}&eWelt &6{world}&e ist bereits eine Hub-Welt.");
        add("hub.admin.world.not-listed", "{prefix}&eWorld &6{world}&e is not a hub world.", "{prefix}&eWelt &6{world}&e ist keine Hub-Welt.");

        // ── Arcanum enchantment module ─────────────────────────────────────
        add("enchant.module-disabled", "{prefix}&cThe Arcanum enchantment module is currently disabled.", "{prefix}&cDas Arcanum-Verzauberungsmodul ist derzeit deaktiviert.");
        add("enchant.unknown-enchant", "{prefix}&cUnknown enchantment: &f{name}&c. Use &f/sfench list&c.", "{prefix}&cUnbekannte Verzauberung: &f{name}&c. Nutze &f/sfench list&c.");
        add("enchant.unknown-level", "{prefix}&cInvalid level. &6{enchant}&c goes from &f1&c to &f{maxlevel}&c.", "{prefix}&cUngültige Stufe. &6{enchant}&c geht von &f1&c bis &f{maxlevel}&c.");
        add("enchant.unknown-category", "{prefix}&cUnknown category: &f{name}&c.", "{prefix}&cUnbekannte Kategorie: &f{name}&c.");
        add("enchant.usage", Arrays.asList(
                "",
                "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  &fCommands",
                "&8──────────────────────────────",
                "",
                "  &6Browse",
                "  &e/sfench list &7[category]            &8» &7List every enchantment",
                "  &e/sfench info &7<enchant>             &8» &7Full details for one",
                "  &e/sfench admin                      &8» &7Open the admin GUI",
                "",
                "  &6Apply",
                "  &e/sfench apply &7<enchant> <level>    &8» &7Enchant the item in your hand",
                "  &e/sfench remove &7<enchant>           &8» &7Remove it from your hand",
                "",
                "  &6Give",
                "  &e/sfench give &7<player> <enchant> <level> [amt]      &8» &7A scroll",
                "  &e/sfench givebook &7<player> <enchant> <level> [amt]  &8» &7An enchanted book",
                "  &e/sfench protect &7<player> [amount]  &8» &7Protection Scrolls",
                "  &e/sfench success &7<player> [amount]  &8» &7Success Scrolls",
                "",
                "  &6Manage",
                "  &e/sfench loot &7toggle <chest> | reload  &8» &7World-loot spawning",
                "  &e/sfench reload                     &8» &7Reload all configs",
                "  &e/sfench debug &7<on|off>             &8» &7Log procs to yourself",
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "",
                "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  &fBefehle",
                "&8──────────────────────────────",
                "",
                "  &6Ansehen",
                "  &e/sfench list &7[Kategorie]           &8» &7Alle Verzauberungen auflisten",
                "  &e/sfench info &7<Verzauberung>        &8» &7Alle Details zu einer",
                "  &e/sfench admin                      &8» &7Admin-GUI öffnen",
                "",
                "  &6Anwenden",
                "  &e/sfench apply &7<Verz.> <Stufe>      &8» &7Item in der Hand verzaubern",
                "  &e/sfench remove &7<Verz.>             &8» &7Aus der Hand entfernen",
                "",
                "  &6Geben",
                "  &e/sfench give &7<Spieler> <Verz.> <Stufe> [Anz.]      &8» &7Eine Schriftrolle",
                "  &e/sfench givebook &7<Spieler> <Verz.> <Stufe> [Anz.]  &8» &7Ein Buch",
                "  &e/sfench protect &7<Spieler> [Anzahl] &8» &7Schutz-Schriftrollen",
                "  &e/sfench success &7<Spieler> [Anzahl] &8» &7Erfolg-Schriftrollen",
                "",
                "  &6Verwalten",
                "  &e/sfench loot &7toggle <Truhe> | reload  &8» &7Welt-Loot-Spawning",
                "  &e/sfench reload                     &8» &7Alle Configs neu laden",
                "  &e/sfench debug &7<on|off>             &8» &7Procs dir protokollieren",
                "&8──────────────────────────────",
                ""
        ));

        // Apply feedback — chat (full) channel.
        add("enchant.apply.applied", "{prefix}&aApplied {enchant} &7{roman}&a to your item.", "{prefix}&a{enchant} &7{roman}&a auf dein Item angewandt.");
        add("enchant.apply.upgraded", "{prefix}&aReplaced {enchant} &7{oldroman} &awith {enchant} &7{roman}&a.", "{prefix}&a{enchant} &7{oldroman} &adurch {enchant} &7{roman}&a ersetzt.");
        add("enchant.apply.wrong-type", "{prefix}&cThat enchantment can only be applied to &f{tags}&c. You are holding &f{item}&c.", "{prefix}&cDiese Verzauberung passt nur auf &f{tags}&c. Du hältst &f{item}&c.");
        add("enchant.apply.empty-hand", "{prefix}&cHold an item to apply this directly. Shift-click in the menu to get a scroll instead.", "{prefix}&cHalte ein Item, um dies direkt anzuwenden. Shift-Klick im Menü für eine Schriftrolle.");
        add("enchant.apply.invalid-level", "{prefix}&c{enchant} &conly goes up to level &f{maxlevel}&c.", "{prefix}&c{enchant} &cgeht nur bis Stufe &f{maxlevel}&c.");
        add("enchant.apply.already-applied", "{prefix}&cThis item already has {enchant} &7{roman}&c.", "{prefix}&cDieses Item hat bereits {enchant} &7{roman}&c.");
        add("enchant.apply.conflict", "{prefix}&c{enchant} &ccannot coexist with {conflict} &con the same item.", "{prefix}&c{enchant} &ckann nicht mit {conflict} &cauf demselben Item bestehen.");
        add("enchant.apply.cap", "{prefix}&cThis item already has the maximum number of enchantments (&f{cap}&c). Remove one first.", "{prefix}&cDieses Item hat bereits die maximale Anzahl Verzauberungen (&f{cap}&c). Entferne zuerst eine.");
        add("enchant.apply.disabled", "{prefix}&cThe Arcanum module is currently disabled.", "{prefix}&cDas Arcanum-Modul ist derzeit deaktiviert.");
        // Tool-enchant action-bar warning (no {prefix} so it stays a clean one-liner).
        add("enchant.tool.creative-warning", "&cTreecapitator only works in Survival mode.", "&cTreecapitator funktioniert nur im Überlebensmodus.");
        // ── Bloodletter bleed-out HUD + death ───────────────────────────────────
        add("enchant.bloodletter.actionbar", "&4❤ &cBleeding out &8» &f{seconds}s &7left", "&4❤ &cVerblutung &8» &f{seconds}s &7übrig");
        add("enchant.bloodletter.death-title", "&4&l☠ YOU BLED OUT", "&4&l☠ DU BIST VERBLUTET");
        add("enchant.bloodletter.death-subtitle", "&cSlain by &f{killer}", "&cErschlagen von &f{killer}");
        add("enchant.bloodletter.death-message", "&4☠ &cYou bled out from &f{killer}&c's wounds.", "&4☠ &cDu bist an den Wunden von &f{killer}&c verblutet.");
        add("enchant.bloodletter.gravestone", "&7⚑ A gravestone marks your death at &f{x}&7, &f{y}&7, &f{z} &8({world})", "&7⚑ Ein Grabstein markiert deinen Tod bei &f{x}&7, &f{y}&7, &f{z} &8({world})");

        // Per-player combat-FX toggle (/enchants settings).
        add("enchant.settings.enabled", "{prefix}&aCombat effects &2enabled&a — you'll see your kill banners and combat feedback.", "{prefix}&aKampfeffekte &2aktiviert&a — du siehst deine Kill-Banner und Kampf-Rueckmeldungen.");
        add("enchant.settings.disabled", "{prefix}&7Combat effects &8disabled&7 — your kill banners and combat feedback are now hidden.", "{prefix}&7Kampfeffekte &8deaktiviert&7 — deine Kill-Banner und Kampf-Rueckmeldungen sind nun ausgeblendet.");

        // Apply feedback — action bar (transient) channel.
        add("enchant.actionbar.applied", "&a✔ Enchantment applied", "&a✔ Verzauberung angewandt");
        add("enchant.actionbar.upgraded", "&a↑ Upgraded to {roman}", "&a↑ Erhöht auf {roman}");
        add("enchant.actionbar.wrong-type", "&c✖ Wrong item type", "&c✖ Falscher Item-Typ");
        add("enchant.actionbar.empty-hand", "&c✖ No item in hand", "&c✖ Kein Item in der Hand");
        add("enchant.actionbar.invalid-level", "&c✖ Invalid level", "&c✖ Ungültige Stufe");
        add("enchant.actionbar.already-applied", "&c✖ Already applied", "&c✖ Bereits vorhanden");
        add("enchant.actionbar.conflict", "&c✖ Conflict", "&c✖ Konflikt");
        add("enchant.actionbar.cap", "&c✖ Slot cap reached", "&c✖ Slot-Limit erreicht");
        add("enchant.actionbar.disabled", "&c✖ Module disabled", "&c✖ Modul deaktiviert");

        // Remove / give / admin.
        add("enchant.removed", "{prefix}&aRemoved {enchant} &afrom your held item.", "{prefix}&a{enchant} &avon deinem Item entfernt.");
        add("enchant.remove.not-present", "{prefix}&cYour held item does not have {enchant}&c.", "{prefix}&cDein Item hat {enchant}&c nicht.");
        add("enchant.remove.locked", "{prefix}&c{enchant} &ccannot be removed.", "{prefix}&c{enchant} &ckann nicht entfernt werden.");
        add("enchant.inventory-full", "{prefix}&cThere is no free inventory slot. Free up space and try again.", "{prefix}&cKein freier Inventarplatz. Schaffe Platz und versuche es erneut.");
        add("enchant.give.received", "{prefix}&aYou received {enchant} &7{roman}&a {kind}&a.", "{prefix}&aDu hast {enchant} &7{roman}&a {kind}&a erhalten.");
        add("enchant.give.sent", "{prefix}&aGave {enchant} &7{roman}&a {kind} &ato &6{player}&a.", "{prefix}&a{enchant} &7{roman}&a {kind} &aan &6{player}&a gegeben.");
        add("enchant.give.utility-received", "{prefix}&aYou received &f{amount}x {kind}&a.", "{prefix}&aDu hast &f{amount}x {kind}&a erhalten.");
        add("enchant.give.utility-sent", "{prefix}&aGave &f{amount}x {kind} &ato &6{player}&a.", "{prefix}&a&f{amount}x {kind} &aan &6{player}&a gegeben.");
        add("enchant.target-offline", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        // Categorized codex book (/sfench give <player> book <category>).
        add("enchant.book.received", "{prefix}&dYou received the {category} &dCodex &7({count} enchantments)&d. Right-click to open.", "{prefix}&dDu hast das {category} &dKompendium &7({count} Verzauberungen)&d erhalten. Rechtsklick zum Oeffnen.");
        add("enchant.book.sent", "{prefix}&aGave the {category} &aCodex to &6{player}&a.", "{prefix}&aDas {category} &aKompendium an &6{player}&a gegeben.");
        add("enchant.book.recipient-no-permission", "{prefix}&c{player} lacks permission to receive an Arcanum codex. No book was given.", "{prefix}&c{player} hat keine Berechtigung, ein Arcanum-Kompendium zu erhalten. Es wurde kein Buch gegeben.");
        add("enchant.book.recipient-no-permission-title", "&4&lAccess Denied", "&4&lZugriff verweigert");
        add("enchant.book.recipient-no-permission-subtitle", "&7{player} has no codex permission", "&7{player} hat keine Kompendium-Berechtigung");
        add("enchant.book.lower-level", "{prefix}&cThis item already has &6{enchant} &7{roman}&c — the codex can only raise it higher.", "{prefix}&cDieses Item hat bereits &6{enchant} &7{roman}&c — das Kompendium kann sie nur hoeher setzen.");
        add("enchant.book.no-item", "{prefix}&cHold an applicable item to apply this. Valid: &f{tags}&c.", "{prefix}&cHalte ein passendes Item, um dies anzuwenden. Gueltig: &f{tags}&c.");
        // Action-bar hint when a scroll/book is right-clicked in hand (it does nothing there).
        add("enchant.book.use-hint", "&e✦ &7This must be applied to an item &8— &7right-clicking does nothing.", "&e✦ &7Dies muss auf ein Item angewendet werden &8— &7Rechtsklick bewirkt nichts.");
        add("enchant.reload.done", "{prefix}&aArcanum reloaded &7({count} enchantments)&a.", "{prefix}&aArcanum neu geladen &7({count} Verzauberungen)&a.");
        add("enchant.debug.on", "{prefix}&aArcanum proc-debug &aenabled&a. Procs will be logged to you.", "{prefix}&aArcanum-Proc-Debug &aaktiviert&a. Procs werden dir protokolliert.");
        add("enchant.debug.off", "{prefix}&eArcanum proc-debug disabled.", "{prefix}&eArcanum-Proc-Debug deaktiviert.");
        add("enchant.loot.toggled", "{prefix}&aLoot for &6{chest}&a is now {state}&a.", "{prefix}&aLoot für &6{chest}&a ist jetzt {state}&a.");
        add("enchant.loot.unknown", "{prefix}&cUnknown chest type: &f{chest}&c.", "{prefix}&cUnbekannter Truhentyp: &f{chest}&c.");
        add("enchant.loot.reloaded", "{prefix}&aReloaded &fenchants/loot.yml&a.", "{prefix}&aenchants/loot.yml &aneu geladen&a.");
        add("enchant.state.enabled", "&aenabled", "&aaktiviert");
        add("enchant.state.disabled", "&cdisabled", "&cdeaktiviert");
        add("enchant.kind.scroll", "&fscroll", "&fSchriftrolle");
        add("enchant.kind.book", "&fbook", "&fBuch");

        // /sfench list — boxed report (matches /sf, /pl styling).
        add("enchant.list.header", Arrays.asList(
                "",
                "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  &fEnchantments  &8·  &f{count}",
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "",
                "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  &fVerzauberungen  &8·  &f{count}",
                "&8──────────────────────────────",
                ""
        ));
        add("enchant.list.category", "  {color}&l{category}  &8·  &f{count}", "  {color}&l{category}  &8·  &f{count}");
        add("enchant.list.entry", "    {enchant} &7{maxlevel} &8·  &7{tags}", "    {enchant} &7{maxlevel} &8·  &7{tags}");
        add("enchant.list.footer", Arrays.asList(
                "&8──────────────────────────────",
                ""
        ), Arrays.asList(
                "&8──────────────────────────────",
                ""
        ));

        // /sfench info — boxed detail.
        add("enchant.info.header", "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  {enchant}", "&5▍ &d𝗔𝗥𝗖𝗔𝗡𝗨𝗠  &8›  {enchant}");
        add("enchant.info.meta", "  &7Category &8›  {color}{category}   &7Rarity &8›  {rarity}   &7Max &8›  &f{maxlevel}", "  &7Kategorie &8›  {color}{category}   &7Seltenheit &8›  {rarity}   &7Max &8›  &f{maxlevel}");
        add("enchant.info.tags", "  &7Applies to &8›  &f{tags}", "  &7Passt auf &8›  &f{tags}");
        add("enchant.info.desc-line", "  &7{line}", "  &7{line}");
        add("enchant.info.level-line", "  {color}{roman} &8›  &7{params}", "  {color}{roman} &8›  &7{params}");
        add("enchant.browse.soon", "{prefix}&eThe player browse menu opens with &f/enchants&e.", "{prefix}&eDas Spieler-Browsemenü öffnet sich mit &f/enchants&e.");

        // Scroll application outcomes.
        add("enchant.scroll.no-xp", "{prefix}&cYou need &f{cost}&c XP levels to apply this scroll.", "{prefix}&cDu brauchst &f{cost}&c XP-Level für diese Schriftrolle.");
        add("enchant.scroll.failed-destroyed", "{prefix}&cThe application of {enchant} &7{roman}&c failed — the item was destroyed.", "{prefix}&cDas Anwenden von {enchant} &7{roman}&c schlug fehl — das Item wurde zerstört.");
        add("enchant.scroll.failed-kept", "{prefix}&cThe application of {enchant} &7{roman}&c failed, but the item survived.", "{prefix}&cDas Anwenden von {enchant} &7{roman}&c schlug fehl, aber das Item blieb erhalten.");
        add("enchant.scroll.failed-protected", "{prefix}&eThe application failed, but a Protection Scroll saved your item.", "{prefix}&eDas Anwenden schlug fehl, aber eine Schutz-Schriftrolle rettete dein Item.");
        add("enchant.scroll.imbued-protect", "{prefix}&bThis enchant scroll is now &lProtected&b.", "{prefix}&bDiese Verzauberungs-Schriftrolle ist jetzt &lgeschützt&b.");
        add("enchant.scroll.imbued-boost", "{prefix}&aThis enchant scroll is now &lBoosted&a.", "{prefix}&aDiese Verzauberungs-Schriftrolle ist jetzt &lverstärkt&a.");
        add("enchant.scroll.extracted", "{prefix}&aExtracted {enchant} &7{roman}&a into a scroll.", "{prefix}&a{enchant} &7{roman}&a in eine Schriftrolle extrahiert.");
        add("enchant.scroll.no-enchants", "{prefix}&cThat item has no Arcanum enchantments to work with.", "{prefix}&cDieses Item hat keine Arcanum-Verzauberungen.");
        add("enchant.scroll.transmuted", "{prefix}&dTransmuted {old} &dinto {enchant} &7{roman}&d.", "{prefix}&d{old} &din {enchant} &7{roman}&d umgewandelt.");
        add("enchant.scroll.transmute-failed", "{prefix}&cNo other enchantment in that category fits this item.", "{prefix}&cKeine andere Verzauberung dieser Kategorie passt auf dieses Item.");

        // Triggered-effect notices and recall / satchel commands.
        add("enchant.effect.last-stand", "{prefix}&6Last Stand &esaved you from a lethal blow!", "{prefix}&6Letztes Gefecht &erettete dich vor einem tödlichen Schlag!");
        add("enchant.effect.phoenix", "{prefix}&6Phoenix Feather &erevived you!", "{prefix}&6Phönixfeder &ehat dich wiederbelebt!");
        add("enchant.effect.sleepless", "{prefix}&cThe Curse of the Sleepless keeps you from sleeping.", "{prefix}&cDer Fluch der Schlaflosen hält dich vom Schlafen ab.");
        add("enchant.recall.no-enchant", "{prefix}&cYou must be wearing boots with &6Beacon's Memory&c.", "{prefix}&cDu musst Stiefel mit &6Bakens Erinnerung&c tragen.");
        add("enchant.recall.none", "{prefix}&cYou have not bound a recall point yet. Sneak + right-click to bind one.", "{prefix}&cDu hast noch keinen Rückrufpunkt gesetzt. Schleichen + Rechtsklick zum Setzen.");
        add("enchant.recall.cooldown", "{prefix}&cRecall is on cooldown for &f{seconds}s&c.", "{prefix}&cRückruf ist noch &f{seconds}s&c im Cooldown.");
        add("enchant.recall.bound", "{prefix}&aRecall point bound to your current location.", "{prefix}&aRückrufpunkt auf deine aktuelle Position gesetzt.");
        add("enchant.recall.teleported", "{prefix}&aRecalled to your bound location.", "{prefix}&aZu deinem Rückrufpunkt teleportiert.");
        add("enchant.satchel.none", "{prefix}&cYou must be wearing a helmet or chestplate with &5Satchel&c.", "{prefix}&cDu musst einen Helm oder Brustpanzer mit &5Satchel&c tragen.");

        // ── Holograms ────────────────────────────────────────────────────
        // Module prefix — mirrors enchant.prefix / inbox.prefix styling so the
        // module reads as its own surface. Math-bold HOLOGRAMS in cyan
        // (&#22D3EE renders on 1.16+), holographic glyph ✦, ➠ arrow.
        add("hologram.prefix",
                "&#22D3EE✦ 𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠𝗦 &8➠ &7",
                "&#22D3EE✦ 𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠𝗠𝗘 &8➠ &7");
        add("hologram.module.dormant",
                "{prefix}&7Hologram module is &edormant&7 — set &fenabled: true&7 in &fsystems/holograms.yml&7.",
                "{prefix}&7Hologramm-Modul ist &einaktiv&7 — setze &fenabled: true&7 in &fsystems/holograms.yml&7.");
        add("hologram.module.enabled",
                "{prefix}&aHologram module enabled. Backend: &f{backend}&a.",
                "{prefix}&aHologramm-Modul aktiviert. Backend: &f{backend}&a.");
        add("hologram.module.disabled",
                "{prefix}&eHologram module disabled.",
                "{prefix}&eHologramm-Modul deaktiviert.");
        add("hologram.error.module_disabled",
                "{prefix}&cThe hologram module is disabled. Enable it in &fsystems/holograms.yml&c first.",
                "{prefix}&cDas Hologramm-Modul ist deaktiviert. Aktiviere es zuerst in &fsystems/holograms.yml&c.");
        add("hologram.error.backend_unavailable",
                "{prefix}&cNo hologram backend is available on this server. Reason: &f{reason}&c.",
                "{prefix}&cAuf diesem Server ist kein Hologramm-Backend verfügbar. Grund: &f{reason}&c.");
        add("hologram.error.invalid_id",
                "{prefix}&cInvalid hologram name. Use lowercase letters, digits, &7_&c or &7-&c (1–32 chars).",
                "{prefix}&cUngültiger Hologramm-Name. Erlaubt: Kleinbuchstaben, Ziffern, &7_&c oder &7-&c (1–32 Zeichen).");
        add("hologram.error.not_found",
                "{prefix}&cNo hologram named &f{name}&c.",
                "{prefix}&cKein Hologramm mit dem Namen &f{name}&c.");
        add("hologram.error.already_exists",
                "{prefix}&cA hologram named &f{name}&c already exists.",
                "{prefix}&cEin Hologramm mit dem Namen &f{name}&c existiert bereits.");
        add("hologram.debug.spawned",
                "{prefix}&aDebug hologram &f{name}&a spawned at your location.",
                "{prefix}&aDebug-Hologramm &f{name}&a an deiner Position erstellt.");
        add("hologram.debug.removed",
                "{prefix}&eDebug hologram &f{name}&e removed.",
                "{prefix}&eDebug-Hologramm &f{name}&e entfernt.");
        // Phase 2 — command feedback keys.
        add("hologram.create.success",
                "{prefix}&aCreated hologram &f{name}&a.",
                "{prefix}&aHologramm &f{name}&a erstellt.");
        add("hologram.delete.success",
                "{prefix}&eRemoved hologram &f{name}&e.",
                "{prefix}&eHologramm &f{name}&e entfernt.");
        add("hologram.copy.success",
                "{prefix}&aCopied to hologram &f{name}&a.",
                "{prefix}&aZu Hologramm &f{name}&a kopiert.");
        add("hologram.tp.success",
                "{prefix}&aTeleported to hologram &f{name}&a.",
                "{prefix}&aZu Hologramm &f{name}&a teleportiert.");
        add("hologram.move.success",
                "{prefix}&aMoved hologram &f{name}&a.",
                "{prefix}&aHologramm &f{name}&a verschoben.");
        add("hologram.line.added",
                "{prefix}&aLine added to &f{name}&a.",
                "{prefix}&aZeile zu &f{name}&a hinzugefügt.");
        add("hologram.line.updated",
                "{prefix}&aLine updated on &f{name}&a.",
                "{prefix}&aZeile auf &f{name}&a aktualisiert.");
        add("hologram.line.removed",
                "{prefix}&eLine removed from &f{name}&e.",
                "{prefix}&eZeile von &f{name}&e entfernt.");
        add("hologram.line.inserted",
                "{prefix}&aLine inserted on &f{name}&a.",
                "{prefix}&aZeile auf &f{name}&a eingefügt.");
        add("hologram.line.swapped",
                "{prefix}&aLines swapped on &f{name}&a.",
                "{prefix}&aZeilen auf &f{name}&a vertauscht.");
        add("hologram.setting.updated",
                "{prefix}&aSetting updated on &f{name}&a.",
                "{prefix}&aEinstellung auf &f{name}&a aktualisiert.");
        add("hologram.error.invalid_index",
                "{prefix}&cInvalid line index. Use the number shown by &f/holo info&c.",
                "{prefix}&cUngültiger Zeilenindex. Nutze die Nummer aus &f/holo info&c.");
        add("hologram.error.invalid_material",
                "{prefix}&cUnknown material. Use a Bukkit material name (e.g. &fDIAMOND&c).",
                "{prefix}&cUnbekanntes Material. Nutze einen Bukkit-Materialnamen (z. B. &fDIAMOND&c).");
        add("hologram.error.invalid_number",
                "{prefix}&cInvalid number.",
                "{prefix}&cUngültige Zahl.");
        add("hologram.error.invalid_value",
                "{prefix}&cInvalid value.",
                "{prefix}&cUngültiger Wert.");
        add("hologram.error.world_unknown",
                "{prefix}&cUnknown world.",
                "{prefix}&cUnbekannte Welt.");
        add("hologram.error.no_aim_target",
                "{prefix}&eNo hologram in your crosshairs (within 12 blocks).",
                "{prefix}&eKein Hologramm im Fadenkreuz (innerhalb 12 Blöcken).");
        // Phase 3 — pages + text-feature feedback.
        add("hologram.page.changed",
                "{prefix}&aPage set to &f{page}&a / &f{total}&a.",
                "{prefix}&aSeite auf &f{page}&a / &f{total}&a gesetzt.");
        add("hologram.page.unknown",
                "{prefix}&cThat page does not exist for this hologram.",
                "{prefix}&cDiese Seite existiert für dieses Hologramm nicht.");
        add("hologram.page.first",
                "{prefix}&eAlready on the first page.",
                "{prefix}&eBereits auf der ersten Seite.");
        add("hologram.page.last",
                "{prefix}&eAlready on the last page.",
                "{prefix}&eBereits auf der letzten Seite.");
        add("hologram.error.player_not_found",
                "{prefix}&cPlayer &f{name}&c is not online.",
                "{prefix}&cSpieler &f{name}&c ist nicht online.");
        // Phase 4 — interaction feedback.
        add("hologram.interact.cooldown",
                "{prefix}&eOn cooldown. &f{ms}&e ms remaining.",
                "{prefix}&eCooldown aktiv. &f{ms}&e ms verbleibend.");
        add("hologram.interact.no_permission",
                "{prefix}&cYou don't have permission to interact with that hologram.",
                "{prefix}&cKeine Berechtigung, mit diesem Hologramm zu interagieren.");
        add("hologram.interact.disabled",
                "{prefix}&7Interaction is disabled on that hologram.",
                "{prefix}&7Interaktion ist auf diesem Hologramm deaktiviert.");
        // Phase 5 — animations.
        add("hologram.anim.added",
                "{prefix}&aAnimation added to &f{name}&a.",
                "{prefix}&aAnimation zu &f{name}&a hinzugefügt.");
        add("hologram.anim.removed",
                "{prefix}&eAnimation removed from &f{name}&e.",
                "{prefix}&eAnimation von &f{name}&e entfernt.");
        // Phase 6 — visibility refinements.
        add("hologram.visibility.hidden",
                "{prefix}&7Hologram &f{name}&7 hidden for you.",
                "{prefix}&7Hologramm &f{name}&7 für dich ausgeblendet.");
        add("hologram.visibility.shown",
                "{prefix}&aHologram &f{name}&a visible again.",
                "{prefix}&aHologramm &f{name}&a wieder sichtbar.");
    }

    private MessageDefaults() {
    }

    public static Map<String, Object> defaults(LanguageRegistry registry) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Translation> entry : DEFAULTS.entrySet()) {
            Object value = registry == LanguageRegistry.DE ? entry.getValue().de() : entry.getValue().en();
            values.put(entry.getKey(), value);
        }
        return values;
    }

    public static Map<String, List<String>> sectionComments(LanguageRegistry registry) {
        LinkedHashMap<String, List<String>> comments = new LinkedHashMap<>();
        for (Map.Entry<String, SectionComment> entry : SECTION_COMMENTS.entrySet()) {
            List<String> lines = registry == LanguageRegistry.DE ? entry.getValue().deLines : entry.getValue().enLines;
            comments.put(entry.getKey(), lines);
        }
        return comments;
    }

    public static int requiredKeys() {
        return DEFAULTS.size();
    }

    private static void add(String key, String en, String de) {
        DEFAULTS.put(key, new Translation(en, de));
    }

    private static void add(String key, List<String> en, List<String> de) {
        DEFAULTS.put(key, new Translation(new ArrayList<>(en), new ArrayList<>(de)));
    }

    /** The 30× U+2500 dark-gray rule shared by the boxed reports. */
    private static String boxRule() {
        StringBuilder builder = new StringBuilder("&8");
        for (int i = 0; i < 30; i++) {
            builder.append('─');
        }
        return builder.toString();
    }

    /**
     * Renders a single command usage in the shared boxed-report style — a blank
     * line, the {@code ▍ SF-CORE › <Category> · <Command>} title bar, the rule, a
     * blank, the indented {@code <usage> › <description>} row, and a trailing
     * blank. Matches the layout of {@code /list} and {@code /sf info}.
     */
    private static List<String> usageBox(String category, String command, String usage, String description) {
        return Arrays.asList(
                "",
                BOX_TITLE + category + "  &8·  &f" + command,
                BOX_RULE,
                "",
                "  &6" + usage + "  &8›  &7" + description,
                "");
    }

    private static void section(String category, String en, String de) {
        SECTION_COMMENTS.put(category, new SectionComment(commentLines(en), commentLines(de)));
    }

    private static List<String> commentLines(String description) {
        List<String> lines = new ArrayList<>();
        lines.add("# =============================================================================");
        lines.add("# " + description);
        lines.add("# =============================================================================");
        return lines;
    }

    private static final class Translation {
        private final Object en;
        private final Object de;

        private Translation(Object en, Object de) {
            this.en = en;
            this.de = de;
        }

        private Object en() {
            return en;
        }

        private Object de() {
            return de;
        }
    }

    private static final class SectionComment {
        private final List<String> enLines;
        private final List<String> deLines;

        private SectionComment(List<String> enLines, List<String> deLines) {
            this.enLines = enLines;
            this.deLines = deLines;
        }
    }
}
