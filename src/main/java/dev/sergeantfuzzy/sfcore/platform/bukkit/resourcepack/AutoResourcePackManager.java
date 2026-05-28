package dev.sergeantfuzzy.sfcore.platform.bukkit.resourcepack;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Handles installing the bundled pack, hosting it, and applying it to players.
 */
public class AutoResourcePackManager {

    private static final int FAILURE_THRESHOLD = 2;

    private final Main plugin;
    private final File packFile;
    private final Map<UUID, Integer> failures = new ConcurrentHashMap<>();
    private final AtomicBoolean warnedOnce = new AtomicBoolean(false);
    private final Logger logger;

    private byte[] packHash = new byte[0];
    private volatile String packUrl;
    private volatile String lastComputedUrl;
    private volatile boolean usePackHash;
    private volatile boolean hashFromConfig;
    private volatile boolean enabled;

    public AutoResourcePackManager(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.packFile = new File(plugin.getDataFolder(), "resourcepack/sf-core-pack.zip");
        refreshConfig();
    }

    public void refreshConfig() {
        this.enabled = plugin.getConfig().getBoolean("resource-pack.enabled", false);
        this.packUrl = null;
        this.lastComputedUrl = null;
        this.usePackHash = false;
        this.hashFromConfig = false;
    }

    public void installBundledPack() {
        if (!enabled) {
            return;
        }
        String configured = trim(plugin.getConfig().getString("resource-pack.public-url", ""));
        // If no URL is provided, skip generating the local pack (first startup with blank config).
        if (configured == null || configured.isEmpty()) {
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin,
                    "Skipping bundled pack install; resource-pack.public-url is blank.");
            return;
        }
        try (InputStream bundled = plugin.getResource("bundled-resourcepack/sf-core-gui-pack.zip")) {
            if (bundled == null) {
                logger.warning("Bundled resource pack missing from jar.");
                return;
            }
            byte[] bundledBytes = toByteArray(bundled);
            byte[] bundledSha = sha1(bundledBytes);
            ensureParentExists(packFile);

            byte[] existingSha = packFile.isFile() ? sha1(packFile) : null;
            if (existingSha == null || !Arrays.equals(existingSha, bundledSha)) {
                java.nio.file.Files.write(packFile.toPath(), bundledBytes);
                dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin,
                        "Installed resource pack to " + packFile.getAbsolutePath());
            }
            packHash = bundledSha;
        } catch (IOException exception) {
            logger.warning("Failed to install bundled resource pack: " + exception.getMessage());
        }
    }

    /**
     * Prepares the URL used for player downloads. Prefers the configured public URL; otherwise uploads to mc-packs.
     */
    public void prepareHosting() {
        if (!enabled) {
            return;
        }
        packUrl = computeConfiguredUrl();
        usePackHash = false;
        hashFromConfig = false;
        if (packUrl != null) {
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "Resource pack URL set to " + packUrl);
        } else {
            warnAutodetectOnce(null);
        }
    }

    public void applyPackOnJoin(Player player) {
        if (!enabled) {
            return;
        }
        String url = computePackUrlForClients();
        if (url == null || url.isEmpty()) {
            warnAutodetectOnce(url);
            return;
        }
        lastComputedUrl = url;
        try {
            if (usePackHash && packHash != null && packHash.length > 0) {
                player.setResourcePack(url, packHash);
            } else {
                player.setResourcePack(url);
            }
        } catch (Throwable throwable) {
            logger.warning("Failed to send resource pack to " + player.getName() + ": " + throwable.getMessage());
        }
    }

    public void handlePackStatus(PlayerResourcePackStatusEvent event) {
        if (!enabled) {
            return;
        }
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin,
                "Resource pack status for " + event.getPlayer().getName() + ": " + status.name());
        if (status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD || status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            int count = failures.merge(event.getPlayer().getUniqueId(), 1, Integer::sum);
            if (count >= FAILURE_THRESHOLD) {
                warnAutodetectOnce(lastComputedUrl != null ? lastComputedUrl : computePackUrlForClients());
            }
            return;
        }
        if (status == PlayerResourcePackStatusEvent.Status.ACCEPTED || status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            failures.remove(event.getPlayer().getUniqueId());
        }
    }

    private void warnAutodetectOnce(String url) {
        if (warnedOnce.compareAndSet(false, true)) {
            logger.warning("Resource pack URL may not be reachable: " + (url == null || url.isEmpty() ? "unavailable" : url));
            logger.warning("Set resource-pack.public-url in config.yml to a public HTTPS/HTTP URL if your server is behind NAT/proxy.");
        }
    }

    private String computePackUrlForClients() {
        if (!enabled) {
            return null;
        }
        if (packUrl != null && !packUrl.isEmpty()) {
            lastComputedUrl = packUrl;
            return packUrl;
        }
        return null;
    }

    private String computeConfiguredUrl() {
        if (!enabled) {
            return null;
        }
        String configured = trim(plugin.getConfig().getString("resource-pack.public-url", ""));
        String normalizedConfig = normalizeConfiguredUrl(configured);
        if (normalizedConfig != null) {
            lastComputedUrl = normalizedConfig;
            // If the URL embeds a SHA-1 (e.g., mc-packs), use it.
            if (!hashFromConfig && tryExtractHashFromUrl(normalizedConfig)) {
                usePackHash = true;
            } else {
                usePackHash = false; // external URL; hash may not match bundled hash
            }
        }
        return normalizedConfig;
    }

    private String normalizeConfiguredUrl(String configured) {
        if (configured == null || configured.isEmpty()) {
            return null;
        }
        String value = configured.trim();
        if (value.isEmpty()) {
            return null;
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            lastComputedUrl = value;
            return value;
        }
        boolean hasPath = value.contains("/");
        String url = hasPath ? "http://" + value : "http://" + value + "/sf-core-pack.zip";
        lastComputedUrl = url;
        return url;
    }

    private boolean tryExtractHashFromUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        int packIdx = lower.indexOf("/pack/");
        if (packIdx == -1) {
            return false;
        }
        int start = packIdx + "/pack/".length();
        int end = lower.indexOf(".zip", start);
        if (end == -1) {
            return false;
        }
        String candidate = lower.substring(start, end);
        if (candidate.length() != 40 || !candidate.matches("[a-f0-9]{40}")) {
            return false;
        }
        try {
            byte[] hashBytes = hexToBytes(candidate);
            if (hashBytes != null && hashBytes.length == 20) {
                packHash = hashBytes;
                hashFromConfig = true;
                return true;
            }
        } catch (Exception ignored) {
            // ignore parsing errors
        }
        return false;
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                return null;
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static void ensureParentExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Unable to create directory " + parent.getAbsolutePath());
            }
        }
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static byte[] sha1(File file) throws IOException {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            return sha1(inputStream);
        }
    }

    private static byte[] sha1(byte[] data) throws IOException {
        return sha1(new java.io.ByteArrayInputStream(data));
    }

    private static byte[] sha1(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return digest.digest();
        } catch (Exception exception) {
            throw new IOException("Unable to compute SHA-1: " + exception.getMessage(), exception);
        } finally {
            inputStream.close();
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
