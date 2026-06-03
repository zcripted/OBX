package dev.zcripted.obx.hologram.interact;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for the {@code <T>label</T><C>command</C><H>hover text</H>} markup
 * used by interactive hologram lines. A line may contain any number of
 * {@code <T>…</T>} blocks; each one becomes a {@link Segment} with the
 * embedded command + optional hover text.
 *
 * <p>Plain text outside {@code <T>} blocks renders as static label text with
 * no command attached — so the same parser handles both clickable and
 * non-clickable lines without a special case at the caller.
 *
 * <p>This is intentionally a small hand-rolled parser (no regex backtracking,
 * no allocation per character) so it's cheap to call per-line per-tick when
 * the renderer decides whether a click should fire.
 */
public final class CTextParser {

    public static final class Segment {
        public final String label;
        public final String command;
        public final String hover;

        Segment(String label, String command, String hover) {
            this.label = label;
            this.command = command;
            this.hover = hover;
        }

        public boolean isClickable() {
            return command != null && !command.isEmpty();
        }
    }

    private CTextParser() {
    }

    public static List<Segment> parse(String template) {
        List<Segment> out = new ArrayList<>();
        if (template == null || template.isEmpty()) {
            return out;
        }
        int index = 0;
        StringBuilder buffer = new StringBuilder();
        while (index < template.length()) {
            int tStart = template.indexOf("<T>", index);
            if (tStart < 0) {
                buffer.append(template.substring(index));
                break;
            }
            buffer.append(template, index, tStart);
            int tEnd = template.indexOf("</T>", tStart + 3);
            if (tEnd < 0) {
                // unterminated — treat the rest as plain text
                buffer.append(template.substring(tStart));
                break;
            }
            // Flush plain prefix as a non-clickable segment if non-empty.
            if (buffer.length() > 0) {
                out.add(new Segment(buffer.toString(), null, null));
                buffer.setLength(0);
            }
            String label = template.substring(tStart + 3, tEnd);
            index = tEnd + 4;
            String command = null;
            String hover = null;
            // After </T> we look for <C>…</C> and <H>…</H>, in any order.
            for (int probe = 0; probe < 2; probe++) {
                if (index < template.length() && template.startsWith("<C>", index)) {
                    int cEnd = template.indexOf("</C>", index + 3);
                    if (cEnd < 0) {
                        break;
                    }
                    command = template.substring(index + 3, cEnd);
                    index = cEnd + 4;
                } else if (index < template.length() && template.startsWith("<H>", index)) {
                    int hEnd = template.indexOf("</H>", index + 3);
                    if (hEnd < 0) {
                        break;
                    }
                    hover = template.substring(index + 3, hEnd);
                    index = hEnd + 4;
                } else {
                    break;
                }
            }
            out.add(new Segment(label, command, hover));
        }
        if (buffer.length() > 0) {
            out.add(new Segment(buffer.toString(), null, null));
        }
        return out;
    }

    /** Concatenate every segment's label, dropping the markup. Used for rendering. */
    public static String stripMarkup(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(template.length());
        for (Segment segment : parse(template)) {
            builder.append(segment.label);
        }
        return builder.toString();
    }
}
