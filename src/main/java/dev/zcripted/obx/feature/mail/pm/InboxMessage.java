package dev.zcripted.obx.feature.mail.pm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * One stored private message held for an offline recipient until they read it from the
 * inbox. Persisted as a plain YAML map (sender / name / time / text) so the body can
 * contain any characters without delimiter issues.
 */
public final class InboxMessage {

    /** Sender id used when the console sends a message. */
    public static final String CONSOLE = "CONSOLE";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final String id;
    private final String senderId;
    private final String senderName;
    private final String text;
    private final long time;
    private boolean read;
    private boolean bookmarked;

    /** New incoming message — fresh id, unread, not bookmarked. */
    public InboxMessage(String senderId, String senderName, String text, long time) {
        this(UUID.randomUUID().toString(), senderId, senderName, text, time, false, false);
    }

    /** Full constructor used when loading from storage. */
    public InboxMessage(String id, String senderId, String senderName, String text, long time, boolean read, boolean bookmarked) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.time = time;
        this.read = read;
        this.bookmarked = bookmarked;
    }

    public String getId() {
        return id;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public long getTime() {
        return time;
    }

    public boolean isFromConsole() {
        return CONSOLE.equals(senderId);
    }

    /** Local-time date, e.g. {@code May 27, 2026}. */
    public String dateLabel() {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DATE);
    }

    /** Local time, e.g. {@code 7:57 PM}. */
    public String timeLabel() {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(TIME);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("sender", senderId);
        map.put("name", senderName);
        map.put("time", time);
        map.put("text", text == null ? "" : text);
        map.put("read", read);
        map.put("bookmarked", bookmarked);
        return map;
    }

    public static InboxMessage fromMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        Object id = map.get("id");
        Object sender = map.get("sender");
        Object name = map.get("name");
        Object text = map.get("text");
        Object time = map.get("time");
        long when;
        if (time instanceof Number) {
            when = ((Number) time).longValue();
        } else {
            try {
                when = Long.parseLong(String.valueOf(time));
            } catch (NumberFormatException ex) {
                when = System.currentTimeMillis();
            }
        }
        return new InboxMessage(
                id == null ? null : String.valueOf(id),
                sender == null ? CONSOLE : String.valueOf(sender),
                name == null ? "Unknown" : String.valueOf(name),
                text == null ? "" : String.valueOf(text),
                when,
                truthy(map.get("read")),
                truthy(map.get("bookmarked")));
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
