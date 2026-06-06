package dev.zcripted.obx.util.update;

import dev.zcripted.obx.core.ObxPlugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the latest released OBX version, compares it against the running plugin version, and
 * surfaces the <strong>BuiltByBit resource page</strong> as the download link.
 *
 * <p><b>Version source:</b> the public {@code zcripted/OBX} GitHub repository's latest release
 * tag, which is kept in sync with each BuiltByBit release. BuiltByBit itself is intentionally not
 * polled: its API requires a private token (which must never be embedded in a distributed jar)
 * and its resource page is bot-protected, so a seller-controlled source — the GitHub release tag —
 * is queried for the version, and the user is pointed to BuiltByBit ({@link #DOWNLOAD_URL}) to
 * download. The private source repository is never contacted.
 *
 * <p>All network I/O is performed off the main thread via the plugin's
 * {@link dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter} ({@code runAsync});
 * the {@link Result} is delivered back on the main/global thread
 * ({@code runNow}) so it is always safe to send chat messages from the
 * callback, on Bukkit/Spigot/Paper/Purpur and Folia alike. The check is
 * dependency-free: GitHub's JSON response is scanned for the {@code "tag_name"}
 * field with a small regex rather than pulling in a JSON library.
 */
public final class UpdateChecker {

    /** Public-facing repository that hosts OBX release tags (owner/name) — the version SOURCE. */
    public static final String REPO = "zcripted/OBX";

    /** Latest-release API endpoint on the public repo (the version source, not the download). */
    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    /** BuiltByBit resource page — the DOWNLOAD link surfaced in the "update available" message. */
    public static final String DOWNLOAD_URL =
            "https://builtbybit.com/resources/obx-obsidian-extended.111131/";

    /** Latest-release page on the public repo — release NOTES for the newest version. */
    public static final String RELEASE_NOTES_URL = "https://github.com/" + REPO + "/releases/latest";

    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final int TIMEOUT_MS = 5000;

    private final ObxPlugin plugin;

    public UpdateChecker(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /** Outcome of a check, delivered to the caller on the main thread. */
    public enum Status { UP_TO_DATE, UPDATE_AVAILABLE, FAILED }

    public static final class Result {
        private final Status status;
        private final String currentVersion;
        private final String latestVersion;

        Result(Status status, String currentVersion, String latestVersion) {
            this.status = status;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
        }

        public Status status() {
            return status;
        }

        public String currentVersion() {
            return currentVersion;
        }

        /** Latest version string from GitHub, or {@code null} when the check failed. */
        public String latestVersion() {
            return latestVersion;
        }
    }

    /**
     * Runs the check asynchronously and invokes {@code callback} on the main
     * thread with the {@link Result}. Never throws; a network/parse failure is
     * reported as {@link Status#FAILED}.
     */
    public void checkAsync(Consumer<Result> callback) {
        final String current = normalize(plugin.getDescription().getVersion());
        plugin.getSchedulerAdapter().runAsync(() -> {
            Result result = fetch(current);
            if (callback != null) {
                plugin.getSchedulerAdapter().runNow(() -> callback.accept(result));
            }
        });
    }

    private Result fetch(String current) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "OBX-UpdateChecker");
            int code = connection.getResponseCode();
            if (code != 200) {
                // 404 = no releases published yet; anything else = transient error.
                return new Result(Status.FAILED, current, null);
            }
            String body = read(connection.getInputStream());
            Matcher matcher = TAG_NAME.matcher(body);
            if (!matcher.find()) {
                return new Result(Status.FAILED, current, null);
            }
            String latest = normalize(matcher.group(1));
            if (latest.isEmpty()) {
                return new Result(Status.FAILED, current, null);
            }
            boolean newer = isNewer(latest, current);
            return new Result(newer ? Status.UPDATE_AVAILABLE : Status.UP_TO_DATE, current, latest);
        } catch (Throwable t) {
            return new Result(Status.FAILED, current, null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws Exception {
        // Defensive cap: the GitHub releases JSON is a few KB, so stop after 1 MB rather than buffer
        // an unbounded stream from a hostile proxy/MITM (the 5s read timeout is the other backstop).
        final int maxBytes = 1024 * 1024;
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        int n;
        int total = 0;
        while ((n = in.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
            total += n;
            if (total >= maxBytes) {
                break;
            }
        }
        return builder.toString();
    }

    /**
     * Strips a leading {@code v} and any build/qualifier suffix (e.g.
     * {@code -SNAPSHOT}, {@code +build.3}) so {@code v1.2.0} and
     * {@code 1.2.0-SNAPSHOT} both normalise to {@code 1.2.0}.
     */
    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        int cut = value.indexOf('-');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        cut = value.indexOf('+');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        return value.trim();
    }

    /**
     * Numeric, dot-segmented version comparison: returns {@code true} when
     * {@code latest} is strictly greater than {@code current}. Missing segments
     * count as 0 ({@code 1.2} == {@code 1.2.0}); non-numeric segments are
     * treated as 0 so a malformed tag never spuriously reports an update.
     */
    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("\\.");
        String[] b = current.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int la = i < a.length ? parse(a[i]) : 0;
            int lb = i < b.length ? parse(b[i]) : 0;
            if (la != lb) {
                return la > lb;
            }
        }
        return false;
    }

    private static int parse(String segment) {
        try {
            return Integer.parseInt(segment.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
