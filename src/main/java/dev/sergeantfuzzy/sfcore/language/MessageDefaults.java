package dev.sergeantfuzzy.sfcore.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageDefaults {

    private static final LinkedHashMap<String, Translation> DEFAULTS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, SectionComment> SECTION_COMMENTS = new LinkedHashMap<>();

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
                "RÃ¼ckmeldungen zum Wechsel des Spielmodus.");
        section("language",
                "Language selection and preferences.",
                "Sprachauswahl und Einstellungen.");
        section("menus",
                "Chat feedback triggered by menus (non-GUI text).",
                "Chat-RÃ¼ckmeldungen aus MenÃ¼s (keine GUI-Texte).");
        section("player",
                "Player management commands and status messages.",
                "Spieler-Management-Befehle und Statusmeldungen.");
        section("teleport",
                "Teleportation, homes, spawn, and warp messages.",
                "Teleportation, Homes, Spawn- und Warp-Nachrichten.");
        section("utility",
                "Utility commands and quick actions.",
                "Hilfsbefehle und Schnellaktionen.");

        // Core
        add("core.prefix", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8 &8➠ &e", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8 &8➠ &e");
        add("core.divider", "&6&l------------------------------", "&6&l------------------------------");
        add("core.no-permission", "{prefix}&eYou don't have permission to do that.", "{prefix}&eDafÃ¼r hast du keine Berechtigung.");
        add("core.no-permission-console", "&eYou don't have permission to do that.", "&eDu hast dafÃ¼r keine Berechtigung.");
        add("core.player-only", "{prefix}&eOnly players can run this command.", "{prefix}&eNur Spieler kÃ¶nnen diesen Befehl ausfÃ¼hren.");
        add("core.player-only-console", "&eOnly players can run this command.", "&eNur Spieler kÃ¶nnen diesen Befehl ausfÃ¼hren.");

        // Language selection
        add("language.already", "{prefix}&eYou are already using &6{language}&e.", "{prefix}&eDu verwendest bereits &6{language}&e.");
        add("language.changed", "{prefix}&eYour language is now set to &6{language}&e.", "{prefix}&eDeine Sprache wurde auf &6{language}&e gestellt.");
        add("language.changed-console", "&eLanguage set to &6{language}&e for this command.", "&eSprache fÃ¼r diesen Befehl auf &6{language}&e gesetzt.");
        add("language.console-only", "{prefix}&eConsole messages default to English.", "{prefix}&eDie Konsole nutzt standardmÃ¤ÃŸig Englisch.");
        add("language.current", "{prefix}&eCurrent language: &6{language}&e.", "{prefix}&eAktuelle Sprache: &6{language}&e.");
        add("language.invalid", "{prefix}&cUnknown language. Options: &eEnglish &7/ &eDeutsch", "{prefix}&cUnbekannte Sprache. Optionen: &eEnglisch &7/ &eDeutsch");
        add("language.usage", "{prefix}&eUsage: &f/language &7<&eEnglish&7|&eEN&7|&eGerman&7|&eDE&7>", "{prefix}&eBenutzung: &f/sprache &7<&eEnglisch&7|&eEN&7|&eDeutsch&7|&eDE&7>");

        // Gamemode
        add("gamemode.already", "{prefix}&eYou are already in &6{mode}&e.", "{prefix}&eDu bist bereits im Modus &6{mode}&e.");
        add("gamemode.changed-self", "{prefix}&eSet your gamemode to &6{mode}&e.", "{prefix}&eDein Spielmodus wurde auf &6{mode}&e gesetzt.");
        add("gamemode.changed-other", "{prefix}&eSet &6{target}&e to &6{mode}&e.", "{prefix}&eSpielmodus von &6{target}&e auf &6{mode}&e gesetzt.");
        add("gamemode.changed-other-console", "&eSet {target} to {mode}.", "&e{target} auf {mode} gesetzt.");
        add("gamemode.changed-target", "{prefix}&eYour gamemode was set to &6{mode}&e by &6{sender}&e.", "{prefix}&e{sender} hat deinen Spielmodus auf &6{mode}&e gesetzt.");
        add("gamemode.current", "{prefix}&eCurrent gamemode: &6{mode}&e.", "{prefix}&eAktueller Spielmodus: &6{mode}&e.");
        add("gamemode.unknown-mode-or-player", "{prefix}&cUnknown mode or player. Valid: &esurvival&7, &ecreative&7, &eadventure&7, &espectator", "{prefix}&cUnbekannter Modus oder Spieler. G\u00fcltig: &esurvival&7, &ecreative&7, &eadventure&7, &espectator");
        add("gamemode.usage.console", "&eUsage: /gamemode <mode> <player>", "&eBenutzung: /gamemode <Modus> <Spieler>");
        add("gamemode.usage.self", "{prefix}&eUsage: &f/gamemode &7<&esurvival|creative|adventure|spectator&7>", "{prefix}&eBenutzung: &f/gamemode &7<&esurvival|creative|adventure|spectator&7>");
        add("gamemode.usage.target", "{prefix}&eAdd a mode for &6{target}&e. Example: &f/gamemode creative {target}", "{prefix}&eGib einen Modus f\u00fcr &6{target}&e an. Beispiel: &f/gamemode creative {target}");

        // Teleport + homes + spawn
        add("teleport.back.missing", "{prefix}&eNo previous location recorded.", "{prefix}&eEs wurde kein vorheriger Ort gespeichert.");
        add("teleport.back.teleporting", "{prefix}&eTeleporting you back.", "{prefix}&eTeleportiere dich zurÃ¼ck.");
        add("teleport.top.usage", "{prefix}&eUsage: &f/top &7[<player>]", "{prefix}&eBenutzung: &f/top &7[<Spieler>]");
        add("teleport.top.usage-console", "&eUsage: /top <player>", "&eBenutzung: /top <Spieler>");
        add("teleport.top.no-safe", "{prefix}&cNo safe block found above &6{target}&c.", "{prefix}&cKein sicherer Block ueber &6{target}&c gefunden.");
        add("teleport.top.teleporting", "{prefix}&eTeleporting to the highest safe block.", "{prefix}&eTeleportiere zum hoechsten sicheren Block.");
        add("teleport.top.sent-other", "{prefix}&eTeleported &6{target}&e to the highest safe block.", "{prefix}&e&6{target}&e zum hoechsten sicheren Block teleportiert.");
        add("teleport.top.sent-target", "{prefix}&eYou were sent to the highest safe block by &6{sender}&e.", "{prefix}&eDu wurdest von &6{sender}&e zum hoechsten sicheren Block teleportiert.");
        add("teleport.top.target-not-found", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("teleport.home.limit", "{prefix}&eYou cannot create more than &6{limit} &ehomes.", "{prefix}&eDu kannst nicht mehr als &6{limit} &eHomes erstellen.");
        add("teleport.home.not-found", "{prefix}&eHome &6{home} &edoes not exist.", "{prefix}&eDas Home &6{home} &eexistiert nicht.");
        add("teleport.home.delete-usage", "{prefix}&eUsage: &f/{command} <name>", "{prefix}&eBenutzung: &f/{command} <Name>");
        add("teleport.home.removed", "{prefix}&eHome &6{home} &ehas been removed.", "{prefix}&eDas Home &6{home} &ewurde entfernt.");
        add("teleport.home.set", "{prefix}&eHome &6{home} &ehas been set.", "{prefix}&eDas Home &6{home} &ewurde gesetzt.");
        add("teleport.home.teleporting", "{prefix}&eTeleporting to home &6{home}&e.", "{prefix}&eTeleportiere zum Home &6{home}&e.");
        add("teleport.homes.list", "{prefix}&eHomes: &6{homes}", "{prefix}&eHomes: &6{homes}");
        add("teleport.homes.none", "none", "keine");
        add("teleport.spawn.delete-confirm", "{prefix}&eType &6confirm &eto delete spawn.", "{prefix}&eTippe &6confirm &eum den Spawn zu lÃ¶schen.");
        add("teleport.spawn.delete-confirm-needed", "{prefix}&cPlease confirm spawn deletion.", "{prefix}&cBitte bestÃ¤tige das LÃ¶schen des Spawns.");
        add("teleport.spawn.deleted", "{prefix}&eSpawn has been unset.", "{prefix}&eSpawn wurde entfernt.");
        add("teleport.spawn.info", Arrays.asList(
                "{prefix}&6Spawn Info:",
                "{prefix}&eWorld: &f{world}",
                "{prefix}&eXYZ: &f{x}, {y}, {z}",
                "{prefix}&eYaw/Pitch: &f{yaw}/{pitch}",
                "{prefix}&eSet by: &f{setBy}",
                "{prefix}&eSet at: &f{setAt}"
        ), Arrays.asList(
                "{prefix}&6Spawn-Info:",
                "{prefix}&eWelt: &f{world}",
                "{prefix}&eXYZ: &f{x}, {y}, {z}",
                "{prefix}&eYaw/Pitch: &f{yaw}/{pitch}",
                "{prefix}&eGesetzt von: &f{setBy}",
                "{prefix}&eGesetzt am: &f{setAt}"
        ));
        add("teleport.spawn.info-missing", "{prefix}&eSpawn is not set.", "{prefix}&eDer Spawn ist nicht gesetzt.");
        add("teleport.spawn.info-unknown", "unknown", "unbekannt");
        add("teleport.spawn.missing", "{prefix}&eSpawn has not been set yet.", "{prefix}&eDer Spawn wurde noch nicht gesetzt.");
        add("teleport.spawn.missing-op", "{prefix}&cSpawn is not set. Use &e/spawn set &cto create one.", "{prefix}&cSpawn ist nicht gesetzt. Nutze &e/spawn set &cum einen zu erstellen.");
        add("teleport.spawn.missing-op-hover", "&eClick to suggest /spawn set", "&eKlicke, um /spawn set vorzuschlagen");
        add("teleport.spawn.set", "{prefix}&eServer spawn updated.", "{prefix}&eServer-Spawn aktualisiert.");
        add("teleport.spawn.teleporting", "{prefix}&eTeleporting to spawn.", "{prefix}&eTeleportiere zum Spawn.");
        add("teleport.warmup.cancelled", "{prefix}&eTeleport cancelled.", "{prefix}&eTeleport abgebrochen.");
        add("teleport.warmup.start", "{prefix}&eTeleport commencing in &6{seconds} &eseconds. Do not move!", "{prefix}&eTeleport startet in &6{seconds} &eSekunden. Nicht bewegen!");
        add("teleport.warp.categories.list", "{prefix}&eWarp categories: &f{categories}", "{prefix}&eWarp-Kategorien: &f{categories}");
        add("teleport.warp.categories.none", "{prefix}&eNo warp categories available.", "{prefix}&eKeine Warp-Kategorien vorhanden.");
        add("teleport.warp.delete.confirm", "{prefix}&eType &6confirm &eto delete warp &6{warp}&e.", "{prefix}&eTippe &6confirm &eum den Warp &6{warp}&e zu lÃ¶schen.");
        add("teleport.warp.delete.confirm-needed", "{prefix}&cPlease confirm deletion of &6{warp}&c.", "{prefix}&cBitte bestÃ¤tige das LÃ¶schen von &6{warp}&c.");
        add("teleport.warp.deleted", "{prefix}&eWarp &6{warp} &ehas been deleted.", "{prefix}&eWarp &6{warp} &ewurde gelÃ¶scht.");
        add("teleport.warp.icon.cleared", "{prefix}&eCleared icon for warp &6{warp}&e.", "{prefix}&eIcon fÃ¼r Warp &6{warp}&e zurÃ¼ckgesetzt.");
        add("teleport.warp.icon.invalid", "{prefix}&cUnknown icon &6{icon}&c.", "{prefix}&cUnbekanntes Icon &6{icon}&c.");
        add("teleport.warp.icon.updated", "{prefix}&eSet icon for warp &6{warp}&e to &6{icon}&e.", "{prefix}&eIcon fÃ¼r Warp &6{warp}&e auf &6{icon}&e gesetzt.");
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
        add("teleport.warp.invalid-location", "{prefix}&cWarp &6{warp}&c has no valid location.", "{prefix}&cWarp &6{warp}&c hat keinen gÃ¼ltigen Standort.");
        add("teleport.warp.invalid-name", "{prefix}&cWarp names may only use letters, numbers, underscores, and dashes (max 32).", "{prefix}&cWarp-Namen dÃ¼rfen nur Buchstaben, Zahlen, Unterstrich und Bindestrich nutzen (max 32).");
        add("teleport.warp.list.empty", "{prefix}&eNo warps available.", "{prefix}&eKeine Warps verfÃ¼gbar.");
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
        add("teleport.warp.set.confirm", "{prefix}&eWarp &6{warp}&e already exists. Run again with confirm to overwrite.", "{prefix}&eWarp &6{warp}&e existiert bereits. Mit confirm Ã¼berschreiben.");
        add("teleport.warp.set.created", "{prefix}&eWarp &6{warp}&e created.", "{prefix}&eWarp &6{warp}&e erstellt.");
        add("teleport.warp.set.updated", "{prefix}&eWarp &6{warp}&e updated.", "{prefix}&eWarp &6{warp}&e aktualisiert.");
        add("teleport.warp.target-no-access", "{prefix}&c{target} cannot access warp &6{warp}&c.", "{prefix}&c{target} kann den Warp &6{warp}&c nicht nutzen.");
        add("teleport.warp.teleporting", "{prefix}&eTeleporting to warp &6{warp}&e.", "{prefix}&eTeleportiere zum Warp &6{warp}&e.");
        add("teleport.warp.unknown", "{prefix}&cUnknown warp or subcommand. Try &e/warp list&c.", "{prefix}&cUnbekannter Warp oder Unterbefehl. Versuche &e/warp list&c.");
        add("teleport.warp.usage.category", "{prefix}&eUsage: &f/warp category <name>", "{prefix}&eBenutzung: &f/warp category <Name>");
        add("teleport.warp.usage.console", "&eUsage: /warp <name|subcommand>", "&eBenutzung: /warp <Name|Unterbefehl>");
        add("teleport.warp.usage.delete", "{prefix}&eUsage: &f/warp delete <name>", "{prefix}&eBenutzung: &f/warp delete <Name>");
        add("teleport.warp.usage.icon", "{prefix}&eUsage: &f/warp icon <name> [material]", "{prefix}&eBenutzung: &f/warp icon <Name> [Material]");
        add("teleport.warp.usage.info", "{prefix}&eUsage: &f/warp info <name>", "{prefix}&eBenutzung: &f/warp info <Name>");
        add("teleport.warp.usage.move", "{prefix}&eUsage: &f/warp move <name>", "{prefix}&eBenutzung: &f/warp move <Name>");
        add("teleport.warp.usage.public", "{prefix}&eUsage: &f/warp public <name> &7[true|false]", "{prefix}&eBenutzung: &f/warp public <Name> &7[true|false]");
        add("teleport.warp.usage.rename", "{prefix}&eUsage: &f/warp rename <old> <new>", "{prefix}&eBenutzung: &f/warp rename <Alt> <Neu>");
        add("teleport.warp.usage.set", "{prefix}&eUsage: &f/warp set <name> &7[confirm]", "{prefix}&eBenutzung: &f/warp set <Name> &7[confirm]");
        add("teleport.warp.usage.tp", "{prefix}&eUsage: &f/warp tp <name> &7[player]", "{prefix}&eBenutzung: &f/warp tp <Name> &7[Spieler]");
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
        add("utility.heal.success", "{prefix}&eYou feel rejuvenated.", "{prefix}&eDu fÃ¼hlst dich erfrischt.");
        add("utility.heal.invalid-gamemode", "{prefix}&eYou can't use /heal in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst /heal im Kreativ- oder Zuschauermodus nicht nutzen. Wechsle in Survival oder Adventure.");
        add("utility.fly.invalid-gamemode", "{prefix}&eYou can't toggle fly while in Creative or Spectator. Try Survival or Adventure.", "{prefix}&eDu kannst Flug im Kreativ- oder Zuschauermodus nicht umschalten. Versuche Survival oder Adventure.");

        add("utility.craft.usage", "{prefix}&eUsage: &f/research &7[item]", "{prefix}&eBenutzung: &f/research &7[Item]");
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
        add("player.vital.usage", "{prefix}&eUsage: &f/vital &7[<player>]", "{prefix}&eBenutzung: &f/vital &7[<Spieler>]");
        add("player.vital.usage-console", "&eUsage: /vital <player>", "&eBenutzung: /vital <Spieler>");
        add("player.vital.success", "{prefix}&eYou feel fully restored.", "{prefix}&eDu fuehlst dich vollstaendig wiederhergestellt.");
        add("player.vital.success-other", "{prefix}&eRestored &6{target}&e's health and hunger.", "{prefix}&eGesundheit und Hunger von &6{target}&e wiederhergestellt.");
        add("player.vital.success-target", "{prefix}&eYour health and hunger were restored by &6{sender}&e.", "{prefix}&eDeine Gesundheit und dein Hunger wurden von &6{sender}&e wiederhergestellt.");
        add("player.vital.invalid-gamemode", "{prefix}&eYou can't use /vital in Creative or Spectator. Switch to Survival or Adventure.", "{prefix}&eDu kannst /vital im Kreativ- oder Zuschauermodus nicht nutzen. Wechsle zu Survival oder Adventure.");
        add("player.vital.invalid-gamemode-other", "{prefix}&eYou can't use /vital on &6{target}&e while they are in Creative or Spectator.", "{prefix}&eDu kannst /vital nicht auf &6{target}&e anwenden, solange der Spieler im Kreativ- oder Zuschauermodus ist.");
        add("player.vital.target-not-found", "{prefix}&cPlayer &6{player}&c is not online.", "{prefix}&cSpieler &6{player}&c ist nicht online.");
        add("player.god.usage", "{prefix}&eUsage: &f/god &7[<player>]", "{prefix}&eBenutzung: &f/god &7[<Spieler>]");
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
        add("player.invsee.usage", "{prefix}&eUsage: &f/invsee &7<player>", "{prefix}&eBenutzung: &f/invsee &7<Spieler>");
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
        add("player.moderation.usage.ban", "{prefix}&eUsage: &f/ban &7<player> [reason]", "{prefix}&eBenutzung: &f/ban &7<Spieler> [Grund]");
        add("player.moderation.usage.unban", "{prefix}&eUsage: &f/unban &7<player> [reason]", "{prefix}&eBenutzung: &f/unban &7<Spieler> [Grund]");
        add("player.moderation.usage.kick", "{prefix}&eUsage: &f/kick &7<player> [reason]", "{prefix}&eBenutzung: &f/kick &7<Spieler> [Grund]");
        add("player.moderation.usage.mute", "{prefix}&eUsage: &f/mute &7<player> [reason]", "{prefix}&eBenutzung: &f/mute &7<Spieler> [Grund]");
        add("player.moderation.usage.unmute", "{prefix}&eUsage: &f/unmute &7<player> [reason]", "{prefix}&eBenutzung: &f/unmute &7<Spieler> [Grund]");
        add("player.moderation.usage.tempban", "{prefix}&eUsage: &f/tempban &7<player> [reason]", "{prefix}&eBenutzung: &f/tempban &7<Spieler> [Grund]");
        add("player.moderation.usage.warn", "{prefix}&eUsage: &f/warn &7<player> [reason]", "{prefix}&eBenutzung: &f/warn &7<Spieler> [Grund]");
        add("player.moderation.usage.status", "{prefix}&eUsage: &f/status &7<player>", "{prefix}&eBenutzung: &f/status &7<Spieler>");
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
        add("player.moderation.status.header",
                "{prefix}&6Moderation Profile &8› &e{target}",
                "{prefix}&6Moderationsprofil &8› &e{target}");
        add("player.moderation.status.summary",
                " &8│ &7UUID: &f{uuid}  &8·  &7Profile: &f{profileType}  &8·  &7Session: &f{online}  &8·  &7Updated: &f{updated}",
                " &8│ &7UUID: &f{uuid}  &8·  &7Profil: &f{profileType}  &8·  &7Status: &f{online}  &8·  &7Aktualisiert: &f{updated}");
        add("player.moderation.status.ban-active",
                " &8│ &c● &7Ban  &8│  &cACTIVE  &8·  &f{type}  &8·  &7by &f{actor}  &8·  &7expires &f{expires}",
                " &8│ &c● &7Bann  &8│  &cAKTIV  &8·  &f{type}  &8·  &7von &f{actor}  &8·  &7endet &f{expires}");
        add("player.moderation.status.ban-clear",
                " &8│ &a● &7Ban  &8│  &aClear",
                " &8│ &a● &7Bann  &8│  &aFrei");
        add("player.moderation.status.ban-reason",
                " &8│     &8└ &7Reason: &f{reason}",
                " &8│     &8└ &7Grund: &f{reason}");
        add("player.moderation.status.mute-active",
                " &8│ &e● &7Mute  &8│  &eACTIVE  &8·  &7by &f{actor}  &8·  &7issued &f{issuedAt}",
                " &8│ &e● &7Mute  &8│  &eAKTIV  &8·  &7von &f{actor}  &8·  &7seit &f{issuedAt}");
        add("player.moderation.status.mute-clear",
                " &8│ &a● &7Mute  &8│  &aClear",
                " &8│ &a● &7Mute  &8│  &aFrei");
        add("player.moderation.status.mute-reason",
                " &8│     &8└ &7Reason: &f{reason}",
                " &8│     &8└ &7Grund: &f{reason}");
        add("player.moderation.status.counts-primary",
                " &8│ &6▸ &7Totals  &8│  &fBans {bans}  &8·  &fTempbans {tempbans}  &8·  &fKicks {kicks}  &8·  &fWarnings {warnings}",
                " &8│ &6▸ &7Summen  &8│  &fBanns {bans}  &8·  &fTempbanns {tempbans}  &8·  &fKicks {kicks}  &8·  &fWarnungen {warnings}");
        add("player.moderation.status.counts-secondary",
                " &8│           &fMutes {mutes}  &8·  &fUnmutes {unmutes}  &8·  &fUnbans {unbans}",
                " &8│           &fMutes {mutes}  &8·  &fEntstummt {unmutes}  &8·  &fEntbannt {unbans}");
        add("player.moderation.status.last-warning",
                " &8│ &e▸ &7Last Warning  &8│  &f{issuedAt}  &8·  &7by &f{actor}  &8·  &7Reason: &f{reason}",
                " &8│ &e▸ &7Letzte Warnung  &8│  &f{issuedAt}  &8·  &7von &f{actor}  &8·  &7Grund: &f{reason}");
        add("player.moderation.status.last-warning-none",
                " &8│ &7▸ Last Warning  &8│  &8None on record",
                " &8│ &7▸ Letzte Warnung  &8│  &8Keine gespeichert");
        add("player.moderation.status.last-action",
                " &8│ &6▸ &7Last Action  &8│  &f{action}  &8·  &f{issuedAt}  &8·  &7by &f{actor}  &8·  &7Reason: &f{reason}",
                " &8│ &6▸ &7Letzte Aktion  &8│  &f{action}  &8·  &f{issuedAt}  &8·  &7von &f{actor}  &8·  &7Grund: &f{reason}");
        add("player.moderation.status.last-action-none",
                " &8│ &7▸ Last Action  &8│  &8No actions recorded",
                " &8│ &7▸ Letzte Aktion  &8│  &8Keine Aktionen gespeichert");
        add("player.moderation.status.recent-header",
                " &8│ &6▸ &7Recent Actions  &8│  &e({count})",
                " &8│ &6▸ &7Letzte Aktionen  &8│  &e({count})");
        add("player.moderation.status.recent-entry",
                " &8│   &8• &f{action}  &8·  &7{issuedAt}  &8·  &7by &f{actor}  &8·  &f{reason}",
                " &8│   &8• &f{action}  &8·  &7{issuedAt}  &8·  &7von &f{actor}  &8·  &f{reason}");
        add("player.moderation.status.recent-none",
                " &8│   &8• &7No recent moderation history.",
                " &8│   &8• &7Keine aktuelle Moderationshistorie gespeichert.");
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
        add("admin.confirm.stop-prompt", "{prefix}&eClick again to confirm server stop.", "{prefix}&eKlicke erneut, um den Server-Stop zu bestÃ¤tigen.");
        add("admin.joinlock.enabled", "{prefix}&eJoin lock {state}.", "{prefix}&eJoin-Sperre {state}.");
        add("admin.joinlock.state.enabled", "enabled", "aktiviert");
        add("admin.joinlock.state.disabled", "disabled", "deaktiviert");
        add("admin.joinlock.kick-reason", "Server temporarily locked. Try again later.", "Server vorÃ¼bergehend gesperrt. Versuche es spÃ¤ter erneut.");
        add("admin.joinlock.locked-kick", "Server is temporarily locked. Please try again later.", "Der Server ist vorÃ¼bergehend gesperrt. Bitte spÃ¤ter erneut versuchen.");
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
        add("admin.performance.tps-missing", "{prefix}&cTPS not available on this server.", "{prefix}&cTPS sind auf diesem Server nicht verfÃ¼gbar.");
        add("admin.performance.cleared", "{prefix}&eCleared &6{removed} &eentities (&6{mode}&e).", "{prefix}&e&6{removed} &eEntitÃ¤ten gelÃ¶scht (&6{mode}&e).");
        add("admin.performance.mode.all", "all entities", "alle EntitÃ¤ten");
        add("admin.performance.mode.mobs", "mobs only", "nur Mobs");
        add("admin.performance.mode.items", "items only", "nur Items");
        add("admin.world.saved", "{prefix}&eSaved all worlds.", "{prefix}&eAlle Welten gespeichert.");
        add("admin.world.autosave.status", "{prefix}&eAuto-save {state} for all worlds.", "{prefix}&eAuto-Speichern fÃ¼r alle Welten {state}.");
        add("admin.world.autosave.state.enabled", "enabled", "aktiviert");
        add("admin.world.autosave.state.disabled", "disabled", "deaktiviert");
        add("admin.world.border-placeholder", "{prefix}&eWorld border controls are a placeholder.", "{prefix}&eWeltrÃ¤ndern-Steuerung ist noch ein Platzhalter.");
        add("admin.plugin.reloaded", "{prefix}&eReloaded SF-Core configs.", "{prefix}&eSF-Core-Konfigurationen neu geladen.");
        add("admin.plugin.reloaded-console", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8  &8➠ &6SF-Core &econfigs were reloaded by &6{player}&e.", "&6\uD835\uDDE6\uD835\uDDD9-\uD835\uDDD6\uD835\uDDE2\uD835\uDDE5\uD835\uDDD8  &8➠ &6SF-Core &eKonfigurationen wurden von &6{player}&e neu geladen.");
        add("admin.plugin.modules-placeholder", "{prefix}&eModule toggles are a placeholder.", "{prefix}&eModul-Umschalter sind noch ein Platzhalter.");
        add("admin.modules.joinleave.enabled", "{prefix}&eJoin/leave broadcasts &aenabled&e.", "{prefix}&eJoin/Leave-Nachrichten &aaktiviert&e.");
        add("admin.modules.joinleave.disabled", "{prefix}&eJoin/leave broadcasts &cdisabled&e.", "{prefix}&eJoin/Leave-Nachrichten &cdeaktiviert&e.");
        add("admin.modules.joinleave.status", "{prefix}&eJoin/leave broadcasts: &f{state}", "{prefix}&eJoin/Leave-Nachrichten: &f{state}");
        add("admin.modules.joinleave.usage", "{prefix}&eUsage: &f/sf joinleave &7<&eon|off|status&7>", "{prefix}&eBenutzung: &f/sf joinleave &7<&ean|aus|status&7>");
        add("admin.modules.joinmotd.enabled", "{prefix}&eIn-game join MOTD &aenabled&e.", "{prefix}&eIn-Game Join-MOTD &aaktiviert&e.");
        add("admin.modules.joinmotd.disabled", "{prefix}&eIn-game join MOTD &cdisabled&e.", "{prefix}&eIn-Game Join-MOTD &cdeaktiviert&e.");
        add("admin.modules.joinmotd.status", "{prefix}&eIn-game join MOTD: &f{state}", "{prefix}&eIn-Game Join-MOTD: &f{state}");
        add("admin.modules.joinmotd.usage", "{prefix}&eUsage: &f/sf joinmotd &7<&eon|off|status&7>", "{prefix}&eBenutzung: &f/sf joinmotd &7<&ean|aus|status&7>");
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
        add("commands.sf.help.header", "{prefix}&6SF-Core Help &7(Page {page}/{pages}{category})", "{prefix}&6SF-Core Hilfe &7(Seite {page}/{pages}{category})");
        add("commands.sf.help.header-category", " - {category}", " - {category}");
        add("commands.sf.help.detail.title", "{prefix}&6{usage}", "{prefix}&6{usage}");
        add("commands.sf.help.detail.description", "{prefix}&eDescription: &f{description}", "{prefix}&eBeschreibung: &f{description}");
        add("commands.sf.help.detail.category", "{prefix}&eCategory: &f{category}", "{prefix}&eKategorie: &f{category}");
        add("commands.sf.help.detail.permission", "{prefix}&ePermission: &f{permission}", "{prefix}&eBerechtigung: &f{permission}");
        add("commands.sf.help.line-suffix", " - {description}", " - {description}");
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
        add("commands.sf.commands.none", "{prefix}&cNo commands available for you in that category.", "{prefix}&cKeine Befehle in dieser Kategorie verfÃ¼gbar.");
        add("commands.sf.commands.header", "{prefix}&6Loaded commands:", "{prefix}&6Geladene Befehle:");
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
        add("commands.sf.version", "{prefix}&6SF-Core &e{version} &7[{tag}]", "{prefix}&6SF-Core &e{version} &7[{tag}]");
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
        add("commands.sf.reload.full.base", "{prefix}&eSF-Core fully reloaded. &7({duration})", "{prefix}&eSF-Core komplett neu geladen. &7({duration})");
        add("commands.sf.reload.full.hover", Arrays.asList(
                "&eReload scope:",
                "&7- config.yml + languages",
                "&7- motd.yml",
                "&7- moderation service",
                "&7- data service",
                "&7- MOTD service",
                "&7- teleport manager state",
                "&7Time: &f{duration}",
                "&7Tip: run /sf diagnostics for a health check."
        ), Arrays.asList(
                "&eNeuladung umfasst:",
                "&7- config.yml + Sprachdateien",
                "&7- motd.yml",
                "&7- Moderationsdienst",
                "&7- Datendienst",
                "&7- MOTD-Dienst",
                "&7- Teleport-Manager",
                "&7Zeit: &f{duration}",
                "&7Tipp: Nutze /sf diagnostics fÃ¼r einen Statuscheck."
        ));
        add("commands.sf.reload.file.missing", "{prefix}&cFile not found: &f{file}", "{prefix}&cDatei nicht gefunden: &f{file}");
        add("commands.sf.reload.file.success", "{prefix}&eReloaded file &6{file}&e.", "{prefix}&eDatei &6{file} &eneu geladen.");
        add("commands.sf.reload.file.hover", Arrays.asList(
                "&eReloaded file:",
                "&7Path: &f{path}",
                "&7Size: &f{size} bytes"
        ), Arrays.asList(
                "&eDatei neu geladen:",
                "&7Pfad: &f{path}",
                "&7GrÃ¶ÃŸe: &f{size} Bytes"
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
        add("commands.sf.updates.forced", "{prefix}&7Forced check triggered (simulated).", "{prefix}&7Erzwungener Check ausgelÃ¶st (simuliert).");
        add("commands.sf.updates.hint", "{prefix}&7Use /sf updates check to force a check or /sf updates notify to toggle notices.", "{prefix}&7Nutze /sf updates check fÃ¼r einen erzwungenen Check oder /sf updates notify zum Umschalten von Hinweisen.");
        add("commands.sf.updates.notify.enabled", "{prefix}&eUpdate notifications enabled.", "{prefix}&eUpdate-Hinweise aktiviert.");
        add("commands.sf.updates.notify.disabled", "{prefix}&eUpdate notifications disabled.", "{prefix}&eUpdate-Hinweise deaktiviert.");
        add("commands.sf.config.validation.header", "{prefix}&6Config validation &7(basic)", "{prefix}&6Config-Validierung &7(basis)");
        add("commands.sf.config.validation.config", "{prefix}&econfig.yml: &aok", "{prefix}&econfig.yml: &aok");
        add("commands.sf.config.validation.data", "{prefix}&edata.yml: {state}", "{prefix}&edata.yml: {state}");
        add("commands.sf.config.validation.data-missing", "&cmissing", "&cfehlt");
        add("commands.sf.config.validation.motd", "{prefix}&emotd.yml: {state}", "{prefix}&emotd.yml: {state}");
        add("commands.sf.config.validation.moderation", "{prefix}&emoderation.yml: {state}", "{prefix}&emoderation.yml: {state}");
        add("commands.sf.config.list.header", "{prefix}&6Loaded configs:", "{prefix}&6Geladene Configs:");
        add("commands.sf.config.list.config", "{prefix}&econfig.yml &7(settings, teleport, homes)", "{prefix}&econfig.yml &7(Einstellungen, Teleport, Homes)");
        add("commands.sf.config.list.data", "{prefix}&edata.yml &7(player data, spawn, homes)", "{prefix}&edata.yml &7(Spielerdaten, Spawn, Homes)");
        add("commands.sf.config.list.motd", "{prefix}&emotd.yml &7(server list MOTD, player counter, hover)", "{prefix}&emotd.yml &7(Serverliste-MOTD, Spielerzaehler, Hover)");
        add("commands.sf.config.list.moderation", "{prefix}&emoderation.yml &7(mutes, warnings, moderation metadata)", "{prefix}&emoderation.yml &7(Stummschaltungen, Verwarnungen, Moderationsdaten)");
        add("commands.sf.debug.toggled", "{prefix}&eDebug logging {state}.", "{prefix}&eDebug-Logging {state}.");
        add("commands.sf.debug.state.enabled", "enabled", "aktiviert");
        add("commands.sf.debug.state.disabled", "disabled", "deaktiviert");
        add("commands.sf.debug.status", "{prefix}&eDebug status: &f{state}", "{prefix}&eDebug-Status: &f{state}");
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
        add("commands.sf.entry.permissions.description", "Lists permission nodes for a command or category.", "Listet Berechtigungen fÃ¼r einen Befehl oder eine Kategorie auf.");
        add("commands.sf.entry.commands.usage", "/sf commands [category]", "/sf commands [Kategorie]");
        add("commands.sf.entry.commands.description", "Lists available commands filtered by category and permissions.", "Listet verfÃ¼gbare Befehle nach Kategorie und Rechten gefiltert auf.");

        add("commands.sf.entry.reload.usage", "/sf reload", "/sf reload");
        add("commands.sf.entry.reload.description", "Reloads SF-Core configs and reinitializes all feature modules safely.", "LÃ¤dt SF-Core Configs neu und initialisiert Module neu.");
        add("commands.sf.entry.reload-config.usage", "/sf reload config", "/sf reload config");
        add("commands.sf.entry.reload-config.description", "Reloads configuration files only (core.yml + feature configs).", "LÃ¤dt nur Konfigurationsdateien neu (core.yml + Feature-Configs).");
        add("commands.sf.entry.reload-file.usage", "/sf reload <file>", "/sf reload <Datei>");
        add("commands.sf.entry.reload-file.description", "Reloads a specific file from the plugin data folder.", "LÃ¤dt eine bestimmte Datei aus dem Plugin-Datenordner neu.");
        add("commands.sf.entry.diagnostics.usage", "/sf diagnostics", "/sf diagnostics");
        add("commands.sf.entry.diagnostics.description", "Runs a quick health check (config status, loaded modules, platform info).", "FÃ¼hrt einen schnellen Gesundheitscheck durch (Config-Status, Module, Plattforminfo).");
        add("commands.sf.entry.diagnostics-full.usage", "/sf diagnostics full", "/sf diagnostics full");
        add("commands.sf.entry.diagnostics-full.description", "Outputs extended diagnostics including services, hooks, and errors.", "Gibt erweiterte Diagnosedaten zu Diensten, Hooks und Fehlern aus.");

        add("commands.sf.entry.version.usage", "/sf version", "/sf version");
        add("commands.sf.entry.version.description", "Shows current SF-Core version and build tag.", "Zeigt die aktuelle SF-Core Version und den Build-Tag.");
        add("commands.sf.entry.updates.usage", "/sf updates", "/sf updates");
        add("commands.sf.entry.updates.description", "Checks for available updates and displays a summary.", "PrÃ¼ft auf verfÃ¼gbare Updates und zeigt eine Zusammenfassung.");
        add("commands.sf.entry.updates-check.usage", "/sf updates check", "/sf updates check");
        add("commands.sf.entry.updates-check.description", "Forces an update check (placeholder).", "Erzwingt einen Update-Check (Platzhalter).");
        add("commands.sf.entry.updates-notify.usage", "/sf updates notify", "/sf updates notify");
        add("commands.sf.entry.updates-notify.description", "Toggles update notifications for the executor.", "Schaltet Update-Benachrichtigungen um.");

        add("commands.sf.entry.config.usage", "/sf config", "/sf config");
        add("commands.sf.entry.config.description", "Displays loaded config files and their status.", "Zeigt geladene Configs und deren Status.");
        add("commands.sf.entry.config-validate.usage", "/sf config validate", "/sf config validate");
        add("commands.sf.entry.config-validate.description", "Validates config files and reports errors or deprecated keys.", "Validiert Config-Dateien und meldet Fehler oder veraltete SchlÃ¼ssel.");
        add("commands.sf.entry.debug.usage", "/sf debug", "/sf debug");
        add("commands.sf.entry.debug.description", "Shows debug status and active debug flags.", "Zeigt Debug-Status und aktive Debug-Flags.");
        add("commands.sf.entry.debug-enable.usage", "/sf debug enable", "/sf debug enable");
        add("commands.sf.entry.debug-enable.description", "Enables debug logging temporarily.", "Aktiviert temporÃ¤r Debug-Logging.");
        add("commands.sf.entry.debug-disable.usage", "/sf debug disable", "/sf debug disable");
        add("commands.sf.entry.debug-disable.description", "Disables debug logging.", "Deaktiviert Debug-Logging.");
        add("commands.sf.entry.debug-dump.usage", "/sf debug dump", "/sf debug dump");
        add("commands.sf.entry.debug-dump.description", "Dumps internal state to a log file for troubleshooting.", "Schreibt den internen Status in eine Log-Datei fÃ¼r Fehleranalyse.");
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
