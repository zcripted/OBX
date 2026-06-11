package dev.zcripted.obx.feature.mail.pm;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists private messages queued for offline players to {@code messages.yml}.
 * Each recipient keeps a capped, newest-last list of {@link InboxMessage}s stored as
 * plain YAML maps under {@code inbox.<uuid>}.
 *
 * <p>Every public method is {@code synchronized} so the shared {@link YamlConfiguration}'s
 * read-modify-write-save cycle can't be interleaved by two threads (e.g. a main-thread {@code /msg}
 * and an async delivery), which would otherwise corrupt {@code messages.yml} or drop messages. The
 * monitor is reentrant, so the nested {@code add → get → writeAll → save} calls are safe.
 */
public final class MessageStore {

    /** Max messages kept per player (oldest dropped beyond this). */
    private static final int CAP = 28;

    private final ObxPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public MessageStore(ObxPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
    }

    public synchronized void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private synchronized YamlConfiguration config() {
        if (config == null) {
            load();
        }
        return config;
    }

    public synchronized void save() {
        if (config == null) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save messages.yml: " + exception.getMessage());
        }
    }

    public synchronized List<InboxMessage> get(UUID recipient) {
        List<InboxMessage> result = new ArrayList<InboxMessage>();
        List<Map<?, ?>> raw = config().getMapList("inbox." + recipient);
        for (Map<?, ?> entry : raw) {
            InboxMessage message = InboxMessage.fromMap(entry);
            if (message != null) {
                result.add(message);
            }
        }
        return result;
    }

    public synchronized int count(UUID recipient) {
        return config().getMapList("inbox." + recipient).size();
    }

    public synchronized void add(UUID recipient, InboxMessage message) {
        List<InboxMessage> messages = get(recipient);
        messages.add(message);
        while (messages.size() > CAP) {
            messages.remove(0);
        }
        writeAll(recipient, messages);
    }

    public synchronized void clear(UUID recipient) {
        config().set("inbox." + recipient, null);
        save();
    }

    /** Marks the message with {@code id} as read. No-op if it isn't found. */
    public synchronized void markRead(UUID recipient, String id) {
        if (id == null) {
            return;
        }
        List<InboxMessage> messages = get(recipient);
        boolean changed = false;
        for (InboxMessage message : messages) {
            if (id.equals(message.getId()) && !message.isRead()) {
                message.setRead(true);
                changed = true;
            }
        }
        if (changed) {
            writeAll(recipient, messages);
        }
    }

    /** Sets the bookmark flag on the message with {@code id}. No-op if it isn't found. */
    public synchronized void setBookmarked(UUID recipient, String id, boolean bookmarked) {
        if (id == null) {
            return;
        }
        List<InboxMessage> messages = get(recipient);
        boolean changed = false;
        for (InboxMessage message : messages) {
            if (id.equals(message.getId()) && message.isBookmarked() != bookmarked) {
                message.setBookmarked(bookmarked);
                changed = true;
            }
        }
        if (changed) {
            writeAll(recipient, messages);
        }
    }

    /** Removes the message with {@code id}. */
    public synchronized void delete(UUID recipient, String id) {
        if (id == null) {
            return;
        }
        List<InboxMessage> messages = get(recipient);
        boolean changed = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (id.equals(messages.get(i).getId())) {
                messages.remove(i);
                changed = true;
            }
        }
        if (changed) {
            writeAll(recipient, messages);
        }
    }

    /** Removes every non-bookmarked message and returns how many were cleared. */
    public synchronized int clearNonBookmarked(UUID recipient) {
        List<InboxMessage> messages = get(recipient);
        int before = messages.size();
        List<InboxMessage> kept = new ArrayList<InboxMessage>();
        for (InboxMessage message : messages) {
            if (message.isBookmarked()) {
                kept.add(message);
            }
        }
        int removed = before - kept.size();
        if (removed > 0) {
            writeAll(recipient, kept);
        }
        return removed;
    }

    private synchronized void writeAll(UUID recipient, List<InboxMessage> messages) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (InboxMessage message : messages) {
            serialized.add(message.toMap());
        }
        config().set("inbox." + recipient, serialized);
        save();
    }
}