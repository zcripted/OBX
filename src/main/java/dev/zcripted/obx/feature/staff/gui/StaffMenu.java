package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.CustomHeadUtil;
import dev.zcripted.obx.feature.warp.gui.WarpMenuStyling;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.core.language.LanguageRegistry;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.feature.moderation.service.ModerationService.ModerationStatusProfile;
import dev.zcripted.obx.feature.staff.service.StaffSessionTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * OBX {@code /staff} GUI: a 6-row inventory listing every online player
 * (alphabetical, self excluded) as a player-skull head. The bottom row is
 * filler glass plus a search head and a custom red-X close head.
 *
 * <p>Each player skull's hover lore carries a moderation report card and
 * profile summary (first join, total active time, current session,
 * country, language, and counts of warnings / mutes / kicks / tempbans /
 * bans). The lore is sourced from the language file so EN/DE viewers see
 * their own translation; placeholders are filled per player.
 */
public final class StaffMenu {

    public static final int INVENTORY_SIZE = 54;
    public static final int CONTENT_END_EXCLUSIVE = 45; // first 5 rows (45 slots) for player heads
    public static final int PAGE_SIZE = 45; // matches CONTENT_END_EXCLUSIVE — one page = first 5 rows
    public static final int PREV_SLOT = 45;
    public static final int VIEWER_SLOT = 47;
    public static final int SEARCH_SLOT = 49;
    public static final int NEXT_SLOT = 51;
    public static final int CLOSE_SLOT = 53;

    /**
     * Base64 texture for a red-X custom head (minecraft-heads.com #56785).
     * The string is the value of the {@code textures} property in a Mojang
     * {@code GameProfile}: a base64-encoded JSON document pointing at a
     * Mojang sessionserver texture URL. If the reflective injection fails
     * on a particular server build, {@link CustomHeadUtil#customHead}
     * silently falls back to a barrier so the close slot is still clickable.
     */
    private static final String RED_X_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3Rl"
                    + "eHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAz"
                    + "MTljMWVkMWU4NGQ4OTA2NSJ9fX0=";

    /**
     * Base64 texture for a magnifying-glass custom head (minecraft-heads.com
     * #27534, "Loupe") used as the search button. Same
     * falls-back-to-barrier semantics as the red-X texture.
     */
    private static final String SEARCH_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3Rl"
                    + "eHR1cmUvZmMzNWU4Njg0YzdmNzc2YmVmZWRjNDMxOWQwODE0OGM1NGJlYTM5MzIxZTFiZDVk"
                    + "ZWY3YTU1Yjg5ZmRhYTA5OSJ9fX0=";

    private static final SimpleDateFormat JOINED_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH);

    private StaffMenu() {
    }

    public static void open(ObxPlugin plugin, Player viewer) {
        open(plugin, viewer, 0);
    }

    /**
     * Opens the staff overview at {@code requestedPage} (0-based). The page
     * is clamped into {@code [0, totalPages)}; pagination buttons render only
     * when there is an adjacent page in that direction. Online-player count
     * is unbounded — {@code totalPages = ceil(onlineExcludingSelf / 45)} —
     * so a server with hundreds of players gets as many pages as needed.
     *
     * <p>Bottom-row layout (slots 45 – 53):
     * <pre>
     *  45  46  47  48  49  50  51  52  53
     *  ◀   ·   me  ·   ?   ·   ▶   ·   ✖
     * </pre>
     * where {@code ◀} / {@code ▶} appear only when a previous / next page
     * exists, {@code me} is the viewer's own player head (accountability
     * marker — shows who is operating this management interface), {@code ?}
     * is the search head, and {@code ✖} is the red-X close head.
     */
    public static void open(ObxPlugin plugin, Player viewer, int requestedPage) {
        LanguageManager languages = plugin.getLanguageManager();
        LanguageRegistry registry = languages.getLanguage(viewer.getUniqueId());

        // Build the alphabetical online-player list, excluding the viewer.
        List<Player> onlineSorted = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            onlineSorted.add(online);
        }
        onlineSorted.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));

        // Page math. Always at least one page so an empty server still
        // renders the bottom-row controls and the viewer's own head.
        int totalPages = Math.max(1, (onlineSorted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int pageStart = page * PAGE_SIZE;
        int pageEnd = Math.min(pageStart + PAGE_SIZE, onlineSorted.size());
        boolean hasPrev = page > 0;
        boolean hasNext = pageEnd < onlineSorted.size();
        int prevSlot = hasPrev ? PREV_SLOT : StaffMenuHolder.NO_SLOT;
        int nextSlot = hasNext ? NEXT_SLOT : StaffMenuHolder.NO_SLOT;

        // playerSlots[slot] = uuid of the head rendered at that slot, or null.
        // Pre-sized for the full inventory so the click handler can index by raw slot.
        List<UUID> playerSlots = new ArrayList<>(Collections.<UUID>nCopies(INVENTORY_SIZE, null));

        StaffMenuHolder holder = new StaffMenuHolder(playerSlots, SEARCH_SLOT, CLOSE_SLOT,
                VIEWER_SLOT, prevSlot, nextSlot, page, totalPages);

        Map<String, String> titleReplacements = new LinkedHashMap<>();
        titleReplacements.put("page", String.valueOf(page + 1));
        titleReplacements.put("pages", String.valueOf(totalPages));
        // Single-page servers use the original title key so existing
        // language overrides stay in effect; multi-page servers use a
        // dedicated key so translators can choose how to render the
        // "(p/n)" suffix without affecting the unpaginated label.
        String titleKey = totalPages > 1 ? "admin.staff.menu.title-paginated" : "admin.staff.menu.title";
        String title = languages.get(viewer, titleKey, titleReplacements);
        if (title == null || title.isEmpty()) {
            title = WarpMenuStyling.gradientTitle(totalPages > 1
                    ? "Staff Menu (" + (page + 1) + "/" + totalPages + ")"
                    : "Staff Menu");
        }
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);

        ItemStack filler = WarpMenuStyling.createFiller();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        // Player heads for the current page occupy slots 0..(pageEnd-pageStart-1)
        // in alphabetical order. Pagination handles overflow — no per-page
        // cap beyond the 45-slot content area.
        for (int i = pageStart; i < pageEnd; i++) {
            Player target = onlineSorted.get(i);
            ItemStack head = buildPlayerHead(plugin, viewer, registry, target, "admin.staff.player-head");
            int slot = i - pageStart;
            inventory.setItem(slot, head);
            playerSlots.set(slot, target.getUniqueId());
        }

        inventory.setItem(SEARCH_SLOT, buildSearchHead(viewer, languages));
        inventory.setItem(CLOSE_SLOT, buildCloseHead(viewer, languages));

        // Viewer's own head — accountability marker showing who is operating
        // this management interface (the "desktop screen" concept).
        inventory.setItem(VIEWER_SLOT, buildPlayerHead(plugin, viewer, registry, viewer, "admin.staff.viewer-head"));

        if (hasPrev) {
            inventory.setItem(prevSlot, buildPrevPageItem(viewer, languages, page, totalPages));
        }
        if (hasNext) {
            inventory.setItem(nextSlot, buildNextPageItem(viewer, languages, page, totalPages));
        }

        viewer.openInventory(inventory);
    }

    /**
     * Re-renders the current page's player heads and the viewer's own head into
     * the EXISTING inventory (no re-open, so the cursor and scroll position are
     * untouched). Keeps the online-player list current and the viewer head's
     * total-active / current-session time ticking live. Driven by
     * {@link AdminMenuRefreshTask} several times a second.
     */
    public static void refresh(ObxPlugin plugin, Player viewer, StaffMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) {
            return;
        }
        LanguageManager languages = plugin.getLanguageManager();
        LanguageRegistry registry = languages.getLanguage(viewer.getUniqueId());

        List<Player> onlineSorted = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            onlineSorted.add(online);
        }
        onlineSorted.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));

        int page = holder.getCurrentPage();
        int pageStart = page * PAGE_SIZE;
        int pageEnd = Math.min(pageStart + PAGE_SIZE, onlineSorted.size());
        ItemStack filler = WarpMenuStyling.createFiller();
        for (int slot = 0; slot < CONTENT_END_EXCLUSIVE; slot++) {
            int idx = pageStart + slot;
            if (idx >= pageStart && idx < pageEnd) {
                Player target = onlineSorted.get(idx);
                inv.setItem(slot, buildPlayerHead(plugin, viewer, registry, target, "admin.staff.player-head"));
                holder.setPlayerAt(slot, target.getUniqueId());
            } else {
                inv.setItem(slot, filler.clone());
                holder.setPlayerAt(slot, null);
            }
        }
        // Viewer's own head — live total-active + current-session time.
        inv.setItem(VIEWER_SLOT, buildPlayerHead(plugin, viewer, registry, viewer, "admin.staff.viewer-head"));
    }

    public static ItemStack buildCloseHead(Player viewer, LanguageManager languages) {
        String name = languages.get(viewer, "admin.staff.close.name");
        if (name == null || name.isEmpty()) {
            name = "§cClose";
        }
        List<String> lore = languages.list(viewer, "admin.staff.close.lore", null);
        return CustomHeadUtil.customHead(RED_X_TEXTURE, colorize(name), colorizeLines(lore));
    }

    private static ItemStack buildSearchHead(Player viewer, LanguageManager languages) {
        String name = languages.get(viewer, "admin.staff.search.name");
        if (name == null || name.isEmpty()) {
            name = "§eSearch Player...";
        }
        List<String> lore = languages.list(viewer, "admin.staff.search.lore", null);
        return CustomHeadUtil.customHead(SEARCH_TEXTURE, colorize(name), colorizeLines(lore));
    }

    /**
     * Renders the alphabetical-list head for an online {@code subject}.
     * {@code keyPrefix} switches between the per-target head
     * ({@code admin.staff.player-head}) and the viewer's own self-head
     * ({@code admin.staff.viewer-head}); both share the same placeholder
     * surface so a single profile lookup feeds either lore template.
     */
    private static ItemStack buildPlayerHead(ObxPlugin plugin, Player viewer, LanguageRegistry registry,
                                             Player subject, String keyPrefix) {
        LanguageManager languages = plugin.getLanguageManager();
        ModerationService moderation = plugin.getModerationService();
        StaffSessionTracker tracker = plugin.getStaffSessionTracker();

        String firstJoined = formatJoinDate(subject);
        long activeMillis = activePlayMillis(subject);
        long sessionMillis = tracker == null ? 0L : tracker.getSessionDuration(subject.getUniqueId());

        String activeStr = formatLongDuration(activeMillis);
        String sessionStr = formatShortDuration(sessionMillis);
        String country = resolveCountryName(subject);
        String language = resolvePlayerLanguage(plugin, subject);

        ModerationStatusProfile profile = moderation == null ? null : moderation.getStatusProfile(subject.getName());
        int warnCount = profile == null ? 0 : profile.getActionCount("warn");
        int muteCount = profile == null ? 0 : profile.getActionCount("mute");
        int kickCount = profile == null ? 0 : profile.getActionCount("kick");
        int tempbanCount = profile == null ? 0 : profile.getActionCount("tempban");
        int banCount = profile == null ? 0 : profile.getActionCount("ban");

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("player", subject.getName());
        replacements.put("uuid", subject.getUniqueId().toString());
        replacements.put("joined", firstJoined);
        replacements.put("active", activeStr);
        replacements.put("session", sessionStr);
        replacements.put("country", country);
        replacements.put("language", language);
        replacements.put("warnings", String.valueOf(warnCount));
        replacements.put("mutes", String.valueOf(muteCount));
        replacements.put("kicks", String.valueOf(kickCount));
        replacements.put("tempbans", String.valueOf(tempbanCount));
        replacements.put("bans", String.valueOf(banCount));

        String displayName = languages.get(viewer, keyPrefix + ".name", replacements);
        if (displayName == null || displayName.isEmpty()) {
            displayName = "§5§l" + subject.getName();
        }
        List<String> lore = languages.list(viewer, keyPrefix + ".lore", replacements);
        return CustomHeadUtil.playerHead(subject, colorize(displayName), colorizeLines(lore));
    }

    private static ItemStack buildPrevPageItem(Player viewer, LanguageManager languages, int page, int totalPages) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("page", String.valueOf(page + 1));
        r.put("pages", String.valueOf(totalPages));
        r.put("target", String.valueOf(page));
        String name = languages.get(viewer, "admin.staff.prev-page.name", r);
        if (name == null || name.isEmpty()) {
            name = "&a◀ Previous Page";
        }
        List<String> lore = languages.list(viewer, "admin.staff.prev-page.lore", r);
        Material material = WarpMenuStyling.resolveMaterial("ARROW");
        return WarpMenuStyling.item(material, colorize(name), colorizeLines(lore));
    }

    private static ItemStack buildNextPageItem(Player viewer, LanguageManager languages, int page, int totalPages) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("page", String.valueOf(page + 1));
        r.put("pages", String.valueOf(totalPages));
        r.put("target", String.valueOf(page + 2));
        String name = languages.get(viewer, "admin.staff.next-page.name", r);
        if (name == null || name.isEmpty()) {
            name = "&aNext Page ▶";
        }
        List<String> lore = languages.list(viewer, "admin.staff.next-page.lore", r);
        Material material = WarpMenuStyling.resolveMaterial("ARROW");
        return WarpMenuStyling.item(material, colorize(name), colorizeLines(lore));
    }

    private static long activePlayMillis(Player player) {
        // The all-time play timer is exposed as a {@code Statistic} enum
        // constant whose name has shifted across API versions:
        // 1.8 – 1.16 expose {@code PLAY_ONE_TICK}; 1.17+ rename it to
        // {@code PLAY_ONE_MINUTE}; 1.20+ adds {@code PLAY_TIME} as the
        // canonical alias. The plugin compiles against the 1.12.2 API
        // baseline, which only carries {@code PLAY_ONE_TICK}, so we resolve
        // the constant reflectively at runtime and try each known name in
        // priority order. The value is a tick count (50 ms / tick) that
        // accumulates across sessions and never resets.
        Statistic stat = resolveStatistic("PLAY_TIME", "PLAY_ONE_MINUTE", "PLAY_ONE_TICK");
        if (stat == null) {
            return 0L;
        }
        try {
            int ticks = player.getStatistic(stat);
            return Math.max(0L, ticks * 50L);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static Statistic resolveStatistic(String... candidates) {
        for (String name : candidates) {
            try {
                return Statistic.valueOf(name);
            } catch (Throwable ignored) {
                // try next candidate
            }
        }
        return null;
    }

    private static String formatJoinDate(OfflinePlayer player) {
        long first = player.getFirstPlayed();
        if (first <= 0L) {
            return "Unknown";
        }
        return JOINED_FORMAT.format(new Date(first));
    }

    /**
     * Long-form duration: years, months, days, hours, minutes, seconds. The
     * year/month/day fields are dropped when zero so a freshly-joined player
     * shows {@code "12m 33s"} rather than {@code "0y 0mo 0d 0h 12m 33s"}.
     */
    private static String formatLongDuration(long millis) {
        if (millis <= 0L) {
            return "0s";
        }
        long totalSeconds = millis / 1000L;
        long years = totalSeconds / (365L * 24L * 3600L);
        totalSeconds %= 365L * 24L * 3600L;
        long months = totalSeconds / (30L * 24L * 3600L);
        totalSeconds %= 30L * 24L * 3600L;
        long days = totalSeconds / (24L * 3600L);
        totalSeconds %= 24L * 3600L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append("y ");
        if (months > 0) sb.append(months).append("mo ");
        if (days > 0) sb.append(days).append("d ");
        sb.append(hours).append("h ");
        sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    /**
     * Short-form duration: days, hours, minutes, seconds (year/month omitted
     * by definition since this is only a session length, but day is also
     * dropped when zero per the spec).
     */
    private static String formatShortDuration(long millis) {
        if (millis <= 0L) {
            return "0s";
        }
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / (24L * 3600L);
        totalSeconds %= 24L * 3600L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        sb.append(hours).append("h ");
        sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    /**
     * Best-effort country name from the player's client locale. The locale
     * is reported by the Minecraft client on language-pack load; we extract
     * the country segment ({@code en_us → US}) and translate a small set of
     * common ISO-3166 codes to display names. Anything unknown is returned
     * as the raw two-letter code (or {@code "Unknown"} when the locale
     * lacks a country segment). Per CLAUDE.md, no network lookup is
     * performed — that would need an offline GeoIP database.
     */
    private static String resolveCountryName(Player player) {
        String locale = readLocale(player);
        if (locale == null || locale.isEmpty()) {
            return "Unknown";
        }
        // Normalize separators ("en_us" / "en-us" → "en_us"), uppercase
        // country, then look up. Some clients report just "en" with no
        // country — treat as unknown.
        String normalized = locale.replace('-', '_');
        int sep = normalized.indexOf('_');
        if (sep < 0 || sep == normalized.length() - 1) {
            return "Unknown";
        }
        String code = normalized.substring(sep + 1).toUpperCase(Locale.ROOT);
        String mapped = COUNTRY_NAMES.get(code);
        return mapped == null ? code : mapped;
    }

    private static String readLocale(Player player) {
        // Player.getLocale() exists on 1.12+; 1.8 – 1.11 only exposes it via
        // Player.spigot().getLocale(). Reflective lookup picks whichever is
        // present so the single jar stays cross-version.
        try {
            return (String) Player.class.getMethod("getLocale").invoke(player);
        } catch (Throwable ignoredModern) {
            try {
                Object spigot = Player.class.getMethod("spigot").invoke(player);
                return (String) spigot.getClass().getMethod("getLocale").invoke(spigot);
            } catch (Throwable ignoredLegacy) {
                return null;
            }
        }
    }

    private static String resolvePlayerLanguage(ObxPlugin plugin, Player player) {
        try {
            LanguageRegistry registry = plugin.getLanguageManager().getLanguage(player.getUniqueId());
            if (registry == null) {
                return "Default (English)";
            }
            switch (registry) {
                case DE:
                    return "Deutsch";
                case EN:
                default:
                    return "English";
            }
        } catch (Throwable ignored) {
            return "Default (English)";
        }
    }

    private static String nameOf(Player player) {
        return player == null || player.getName() == null ? "" : player.getName();
    }

    private static List<String> colorizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(colorize(line));
        }
        return colored;
    }

    private static String colorize(String line) {
        if (line == null) {
            return "";
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', line);
    }

    private static final Map<String, String> COUNTRY_NAMES;
    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("US", "United States");
        map.put("CA", "Canada");
        map.put("MX", "Mexico");
        map.put("GB", "United Kingdom");
        map.put("UK", "United Kingdom");
        map.put("IE", "Ireland");
        map.put("DE", "Germany");
        map.put("AT", "Austria");
        map.put("CH", "Switzerland");
        map.put("FR", "France");
        map.put("NL", "Netherlands");
        map.put("BE", "Belgium");
        map.put("LU", "Luxembourg");
        map.put("ES", "Spain");
        map.put("PT", "Portugal");
        map.put("IT", "Italy");
        map.put("DK", "Denmark");
        map.put("SE", "Sweden");
        map.put("NO", "Norway");
        map.put("FI", "Finland");
        map.put("IS", "Iceland");
        map.put("PL", "Poland");
        map.put("CZ", "Czechia");
        map.put("SK", "Slovakia");
        map.put("HU", "Hungary");
        map.put("RO", "Romania");
        map.put("BG", "Bulgaria");
        map.put("GR", "Greece");
        map.put("RU", "Russia");
        map.put("UA", "Ukraine");
        map.put("BY", "Belarus");
        map.put("TR", "Türkiye");
        map.put("AU", "Australia");
        map.put("NZ", "New Zealand");
        map.put("JP", "Japan");
        map.put("KR", "South Korea");
        map.put("CN", "China");
        map.put("TW", "Taiwan");
        map.put("HK", "Hong Kong");
        map.put("SG", "Singapore");
        map.put("MY", "Malaysia");
        map.put("ID", "Indonesia");
        map.put("PH", "Philippines");
        map.put("TH", "Thailand");
        map.put("VN", "Vietnam");
        map.put("IN", "India");
        map.put("PK", "Pakistan");
        map.put("BD", "Bangladesh");
        map.put("AE", "United Arab Emirates");
        map.put("SA", "Saudi Arabia");
        map.put("IL", "Israel");
        map.put("EG", "Egypt");
        map.put("ZA", "South Africa");
        map.put("NG", "Nigeria");
        map.put("KE", "Kenya");
        map.put("MA", "Morocco");
        map.put("BR", "Brazil");
        map.put("AR", "Argentina");
        map.put("CL", "Chile");
        map.put("CO", "Colombia");
        map.put("PE", "Peru");
        map.put("VE", "Venezuela");
        map.put("UY", "Uruguay");
        map.put("EC", "Ecuador");
        COUNTRY_NAMES = Collections.unmodifiableMap(map);
    }

}
