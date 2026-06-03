package dev.zcripted.obx.hologram.text;

import dev.zcripted.obx.hologram.model.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Single entry point for converting a {@code HologramLine.TextLine} template
 * into a render-ready legacy-coded string. Pipeline:
 *
 * <ol>
 *   <li><strong>Pages.</strong> If the template contains {@code !nextpage!},
 *       split on the delimiter and pick the current page for the viewer
 *       (server-default page 0 when no viewer is supplied).</li>
 *   <li><strong>Placeholders.</strong> If PlaceholderAPI is installed, run
 *       the segment through it with the viewer's player context.</li>
 *   <li><strong>MiniMessage.</strong> If Adventure is on the classpath, parse
 *       MiniMessage tags ({@code <gradient>}, {@code <hex>}, etc.) and
 *       serialize back to legacy section-code form.</li>
 *   <li><strong>Filler.</strong> Expand {@code %filler%} to width-fitting
 *       whitespace using the hologram's {@code line-width} as the target.</li>
 *   <li><strong>Legacy.</strong> Translate any remaining {@code &} colour
 *       codes to {@code §} so the renderer's String setter handles them.</li>
 * </ol>
 *
 * <p>Every step degrades gracefully — missing PAPI, missing Adventure, or a
 * thrown exception inside one of the bridges all fall through to the next
 * step without raising.
 */
public final class HologramTextResolver {

    private static final String PAGE_DELIMITER = "!nextpage!";

    private HologramTextResolver() {
    }

    /** Resolve with no viewer context — used by Phase 1 / Phase 2 paths where a viewer is unknown. */
    public static String resolve(String template, Hologram hologram) {
        return resolve(template, hologram, null);
    }

    /** Full resolve pipeline. */
    public static String resolve(String template, Hologram hologram, Player viewer) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String segment = template;

        if (segment.contains(PAGE_DELIMITER)) {
            String[] pages = segment.split(PAGE_DELIMITER, -1);
            int pageIndex = 0;
            if (viewer != null && hologram != null) {
                pageIndex = PageState.current(viewer.getUniqueId(), hologram.getId());
                if (pageIndex >= pages.length) {
                    pageIndex = 0;
                }
            }
            segment = pages[pageIndex];
        }

        if (PlaceholderBridge.AVAILABLE && segment.indexOf('%') >= 0 && viewer != null) {
            segment = PlaceholderBridge.apply(viewer, segment);
        }

        if (MiniMessageBridge.available() && (segment.indexOf('<') >= 0 || segment.indexOf('#') >= 0)) {
            segment = MiniMessageBridge.apply(segment);
        }

        if (segment.contains("%filler%")) {
            int target = hologram == null ? 200 : hologram.getSettings().getLineWidth();
            segment = FillerExpander.expand(segment, target);
        }

        return ChatColor.translateAlternateColorCodes('&', segment);
    }

    /** Count of page segments in the template, used by {@code PageSub}. */
    public static int pageCount(String template) {
        if (template == null || !template.contains(PAGE_DELIMITER)) {
            return 1;
        }
        return template.split(PAGE_DELIMITER, -1).length;
    }
}
