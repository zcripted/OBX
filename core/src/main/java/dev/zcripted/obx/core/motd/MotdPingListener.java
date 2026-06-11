package dev.zcripted.obx.core.motd;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.motd.MotdService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-list ping handler that injects OBX's configured MOTD, displayed counts,
 * and hover sample lines.
 *
 * <p>Server-list pings are an attacker-driven hot path (botnets routinely flood pings
 * to fingerprint a server) so all reflection is resolved <strong>once per event class
 * and once per profile sample type</strong> and cached on static maps. After the first
 * ping for a given event class, the handler dispatches almost entirely through cached
 * {@link Method}, {@link Field}, and {@link Constructor} handles with no further class-
 * scanning. Cache misses are recorded with sentinel values so absent methods aren't
 * re-resolved on every ping.
 */
public class MotdPingListener implements Listener {

    private static final String PAPER_LISTED_PLAYER_INFO = "com.destroystokyo.paper.event.server.PaperServerListPingEvent$ListedPlayerInfo";

    private static final ConcurrentHashMap<Class<?>, EventReflection> EVENT_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ProfileFactory> PROFILE_CACHE = new ConcurrentHashMap<>();

    /** Event class names already logged by the one-time ping diagnostic. */
    private static final java.util.Set<String> LOGGED_PING_CLASSES = ConcurrentHashMap.newKeySet();

    private static final Method MISSING_METHOD = sentinelMethod();
    private static final Field MISSING_FIELD = sentinelField();
    private static final Class<?> MISSING_CLASS = MissingClassMarker.class;

    private final ObxPlugin plugin;

    public MotdPingListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers an additional handler for Paper's {@code PaperServerListPingEvent}
     * when that class is present on the runtime.
     *
     * <p>This is required for the server-list hover (player sample) to work on
     * Paper and its forks. {@code PaperServerListPingEvent} declares its OWN
     * {@link org.bukkit.event.HandlerList}, so a listener registered only for the
     * base {@link ServerListPingEvent} receives the plain Bukkit event — which
     * exposes {@code setMotd}/{@code setMaxPlayers} (so the MOTD and counter work)
     * but has no player-sample API, leaving the hover silently unset. Listening
     * for the Paper event too means {@link #onServerListPing} runs against an
     * instance whose class exposes {@code getPlayerSample()} / {@code setHidePlayers()},
     * so the cached reflection in {@link EventReflection} can populate the hover.
     *
     * <p>On non-Paper platforms (base Spigot/CraftBukkit) the class is absent and
     * this is a no-op; the base {@link ServerListPingEvent} listener still drives
     * the MOTD and counter there.
     */
    @SuppressWarnings("unchecked")
    public void registerPaperPingListener() {
        Class<?> paperEventClass;
        try {
            paperEventClass = Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
        } catch (Throwable notPaper) {
            return;
        }
        if (!org.bukkit.event.Event.class.isAssignableFrom(paperEventClass)
                || !ServerListPingEvent.class.isAssignableFrom(paperEventClass)) {
            return;
        }
        try {
            // Use a named EventExecutor (not a lambda) so the dispatch is fully
            // predictable under obfuscation/shrinking.
            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends org.bukkit.event.Event>) paperEventClass,
                    this, EventPriority.HIGHEST, new PaperPingExecutor(this), plugin, true);
            if (plugin.getConfig().getBoolean("debug", false)) {
                dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "MOTD",
                        "Registered Paper ping listener for " + paperEventClass.getName()
                                + " (enables the player-count hover).");
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[MOTD] Could not register Paper ping listener; "
                    + "server-list hover may be unavailable: " + throwable.getMessage());
        }
    }

    /**
     * Bridges {@code PaperServerListPingEvent} (resolved only at runtime) into
     * {@link #onServerListPing(ServerListPingEvent)}. A concrete class rather
     * than a lambda so nothing about the dispatch depends on invokedynamic
     * surviving obfuscation.
     */
    private static final class PaperPingExecutor implements org.bukkit.plugin.EventExecutor {
        private final MotdPingListener listener;

        PaperPingExecutor(MotdPingListener listener) {
            this.listener = listener;
        }

        @Override
        public void execute(org.bukkit.event.Listener registered, org.bukkit.event.Event event) {
            if (event instanceof ServerListPingEvent) {
                listener.onServerListPing((ServerListPingEvent) event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerListPing(ServerListPingEvent event) {
        MotdService motdService = plugin.getMotdService();
        if (motdService == null || !motdService.isEnabled()) {
            return;
        }

        EventReflection eventOps = EVENT_CACHE.computeIfAbsent(event.getClass(), this::resolveEventReflection);

        int realOnline = Bukkit.getOnlinePlayers().size();
        int realMax = plugin.getServer().getMaxPlayers();
        int displayOnline = motdService.getDisplayedOnline(realOnline);
        int displayMax = Math.max(displayOnline, motdService.getDisplayedMax(realMax));

        event.setMotd(motdService.buildMotd(realOnline, realMax));
        event.setMaxPlayers(displayMax);
        eventOps.applyDisplayedOnline(event, displayOnline);

        List<String> effectiveLines = resolveHoverLines(motdService, realOnline, realMax);
        if (effectiveLines == null || effectiveLines.isEmpty()) {
            logPingOnce(event, 0, "skipped:no-hover-lines");
            return;
        }
        eventOps.applyHidePlayers(event, false);
        String sampleResult = eventOps.applyHoverSample(plugin, event, effectiveLines);
        logPingOnce(event, effectiveLines.size(), sampleResult);
    }

    /**
     * Logs the outcome of the first ping seen for each distinct event class. On
     * Paper two classes fire (the base {@code ServerListPingEvent} and
     * {@code PaperServerListPingEvent}); seeing both lines makes it obvious which
     * event carries the player-sample API and whether the hover was actually set.
     * After the first ping per class this is a single set lookup — safe on the
     * ping hot path.
     */
    private void logPingOnce(ServerListPingEvent event, int hoverLineCount, String sampleResult) {
        boolean ok = sampleResult != null && sampleResult.startsWith("ok");
        // Success is routine diagnostic noise on the ping hot path — only surface it
        // when debug mode is on (/obx debug). FAILURES always log, even with debug
        // off, so a broken player-count hover is always troubleshootable. When debug
        // is off and the ping succeeded we return WITHOUT consuming the once-per-class
        // slot, so enabling debug later still produces the confirmation line.
        if (ok && !plugin.getConfig().getBoolean("debug", false)) {
            return;
        }
        // Log at most once per distinct event class (pings are an attacker-floodable
        // path, so this keeps the console from being spammed).
        if (!LOGGED_PING_CLASSES.add(event.getClass().getName())) {
            return;
        }
        String message = "ping handled: event=" + event.getClass().getName()
                + ", hoverLines=" + hoverLineCount + ", sample=" + sampleResult;
        if (ok) {
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "MOTD", message);
        } else {
            plugin.getLogger().warning("[MOTD] " + message + " (no custom hover shown for this ping).");
        }
    }

    private List<String> resolveHoverLines(MotdService motdService, int realOnline, int realMax) {
        if (motdService.useProfileNamesForHover()) {
            List<String> sampleLines = motdService.getSampleProfiles();
            if (sampleLines != null && !sampleLines.isEmpty()) {
                return sampleLines;
            }
        }
        return motdService.getHoverLines(realOnline, realMax);
    }

    /** First-touch resolution for a given event class. Allocates only on cold path. */
    private EventReflection resolveEventReflection(Class<?> eventClass) {
        Method setNumPlayers = findMethod(eventClass, "setNumPlayers", int.class);
        Field numPlayersField = setNumPlayers == MISSING_METHOD ? findField(eventClass, "numPlayers") : MISSING_FIELD;
        Method setHidePlayers = findMethod(eventClass, "setHidePlayers", boolean.class);

        Class<?> sampleType = detectSampleType(eventClass);

        Method sampleSetter = MISSING_METHOD;
        for (String name : new String[]{"setPlayerSample", "setSample", "setListedPlayers"}) {
            sampleSetter = findMethod(eventClass, name, List.class);
            if (sampleSetter != MISSING_METHOD) {
                break;
            }
        }
        Method sampleGetter = MISSING_METHOD;
        for (String name : new String[]{"getPlayerSample", "getListedPlayers"}) {
            sampleGetter = findMethod(eventClass, name);
            if (sampleGetter != MISSING_METHOD) {
                break;
            }
        }

        return new EventReflection(setNumPlayers, numPlayersField, setHidePlayers, sampleType, sampleSetter, sampleGetter);
    }

    private Class<?> detectSampleType(Class<?> eventClass) {
        // Detect the ACTUAL element type the sample list holds, from the getter /
        // setter generics — FIRST. Modern Paper's getPlayerSample() returns
        // List<PlayerProfile> (com.destroystokyo.paper.profile.PlayerProfile);
        // older APIs used List<ListedPlayerInfo>. The list is element-type-checked,
        // so building the wrong type makes addAll(...) throw ClassCastException
        // (exactly the StandardPaperServerListPingEventImpl failure). Only fall back
        // to the hardcoded ListedPlayerInfo class when the generics can't be read.
        Class<?> resolved = sampleTypeFromMethod(findMethod(eventClass, "getPlayerSample"));
        if (resolved != null) {
            return resolved;
        }
        resolved = sampleTypeFromParam(findMethod(eventClass, "setPlayerSample", List.class), 0);
        if (resolved != null) {
            return resolved;
        }
        resolved = sampleTypeFromMethod(findMethod(eventClass, "getListedPlayers"));
        if (resolved != null) {
            return resolved;
        }
        resolved = sampleTypeFromParam(findMethod(eventClass, "setListedPlayers", List.class), 0);
        if (resolved != null) {
            return resolved;
        }
        Class<?> paperListedPlayerInfo = findClass(PAPER_LISTED_PLAYER_INFO);
        if (paperListedPlayerInfo != null && paperListedPlayerInfo != MISSING_CLASS) {
            return paperListedPlayerInfo;
        }
        return null;
    }

    private static Class<?> sampleTypeFromMethod(Method method) {
        if (method == null || method == MISSING_METHOD) {
            return null;
        }
        return detectGenericListType(method.getGenericReturnType());
    }

    private static Class<?> sampleTypeFromParam(Method method, int parameterIndex) {
        if (method == null || method == MISSING_METHOD) {
            return null;
        }
        Type[] types = method.getGenericParameterTypes();
        if (parameterIndex < 0 || parameterIndex >= types.length) {
            return null;
        }
        return detectGenericListType(types[parameterIndex]);
    }

    private static Class<?> detectGenericListType(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
        if (arguments.length == 0) {
            return null;
        }
        Type argument = arguments[0];
        return argument instanceof Class<?> ? (Class<?>) argument : null;
    }

    /** Cached per-event-class operations. Hot path uses pre-resolved Method/Field handles. */
    private static final class EventReflection {
        private final Method setNumPlayers;
        private final Field numPlayersField;
        private final Method setHidePlayers;
        private final Class<?> sampleType;
        private final Method sampleSetter;
        private final Method sampleGetter;

        EventReflection(Method setNumPlayers, Field numPlayersField, Method setHidePlayers,
                        Class<?> sampleType, Method sampleSetter, Method sampleGetter) {
            this.setNumPlayers = setNumPlayers;
            this.numPlayersField = numPlayersField;
            this.setHidePlayers = setHidePlayers;
            this.sampleType = sampleType;
            this.sampleSetter = sampleSetter;
            this.sampleGetter = sampleGetter;
        }

        void applyDisplayedOnline(ServerListPingEvent event, int online) {
            if (setNumPlayers != MISSING_METHOD) {
                try {
                    setNumPlayers.invoke(event, online);
                    return;
                } catch (Exception ignored) {
                    // fall through to direct field write
                }
            }
            if (numPlayersField != MISSING_FIELD) {
                try {
                    if (!numPlayersField.isAccessible()) {
                        numPlayersField.setAccessible(true);
                    }
                    numPlayersField.setInt(event, online);
                } catch (Exception ignored) {
                    // platform doesn't expose a writable count
                }
            }
        }

        void applyHidePlayers(ServerListPingEvent event, boolean hidden) {
            if (setHidePlayers == MISSING_METHOD) {
                return;
            }
            try {
                setHidePlayers.invoke(event, hidden);
            } catch (Exception ignored) {
                // method missing on this fork
            }
        }

        /**
         * Populates the server-list player-sample (the hover tooltip over the
         * player count). The list returned by {@code getPlayerSample()} is
         * element-type-checked and that type differs across platforms/versions
         * (modern Paper: {@code PlayerProfile}; older: {@code ListedPlayerInfo};
         * some: {@code GameProfile}). Building the wrong type makes
         * {@code addAll(...)} throw {@link ClassCastException} — the failure seen
         * on {@code StandardPaperServerListPingEventImpl}. So we TRY each candidate
         * element type until the list accepts one, restoring the original contents
         * between failed attempts. Returns a short outcome tag for the diagnostic.
         */
        String applyHoverSample(ObxPlugin plugin, ServerListPingEvent event, List<String> hoverLines) {
            StringBuilder tried = new StringBuilder();
            // 1) Official API path (setter, else mutate / field-replace the getter's
            //    list). Sufficient on platforms whose sample list is mutable.
            String apiOutcome = "none";
            if (sampleGetter != MISSING_METHOD || sampleSetter != MISSING_METHOD) {
                for (Class<?> type : candidateSampleTypes()) {
                    ProfileFactory factory = PROFILE_CACHE.computeIfAbsent(type, t -> ProfileFactory.resolve(plugin, t));
                    List<Object> profiles = factory.buildAll(plugin, hoverLines);
                    if (profiles.isEmpty()) {
                        // Include the resolved factory shape so a lingering failure is
                        // diagnosable (e.g. distinguishes "no wrapper found" -> NONE from
                        // "wrapper threw" -> GAME_PROFILE_WRAP+wrapCtor).
                        tried.append(type.getSimpleName()).append(":no-build(").append(factory.describe()).append(") ");
                        continue;
                    }
                    String outcome = trySetSample(event, profiles);
                    if (outcome.startsWith("ok")) {
                        apiOutcome = outcome + "/" + type.getSimpleName();
                        break;
                    }
                    tried.append(type.getSimpleName()).append(':').append(outcome).append(' ');
                }
            }
            // 2) Comprehensive field injection. On modern Paper the list the server
            //    actually SERIALISES onto the status packet is frequently a DIFFERENT
            //    internal field (commonly List<GameProfile>) than the one
            //    getPlayerSample() mirrors — so even sample=ok via the API path can
            //    leave the wire empty and no hover shows. Fill EVERY profile-typed
            //    List field, building each with the factory for ITS OWN element type,
            //    so whichever field is serialised carries the coloured hover names.
            int fieldsWritten = injectProfileListFields(plugin, event, hoverLines);
            int getterNow = currentSampleSize(event);
            boolean ok = apiOutcome.startsWith("ok") || fieldsWritten > 0 || getterNow == hoverLines.size();
            String summary = "api=" + apiOutcome + " fields=" + fieldsWritten + " getterNow=" + getterNow;
            return ok ? "ok:" + summary : "fail:no-candidate-accepted [" + tried.toString().trim() + "] " + summary;
        }

        /**
         * Fills <em>every</em> profile-typed {@code List} field on the event (and
         * its superclasses) with a mutable list of freshly-built, coloured sample
         * entries — each field populated with objects matching its <em>own</em>
         * generic element type: {@code List<GameProfile>} gets raw
         * {@code GameProfile}s (authlib accepts arbitrary names), {@code
         * List<PlayerProfile>} gets {@code CraftPlayerProfile}s,
         * {@code List<ListedPlayerInfo>} gets {@code ListedPlayerInfo}s. This is the
         * workaround for modern Paper, whose serialised sample field differs from
         * the {@code getPlayerSample()} view. Returns the number of fields written.
         */
        private int injectProfileListFields(ObxPlugin plugin, ServerListPingEvent event, List<String> hoverLines) {
            int written = 0;
            Class<?> current = event.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || !List.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    Class<?> element = detectGenericListType(field.getGenericType());
                    if (element == null || !isProfileType(element)) {
                        continue;
                    }
                    ProfileFactory factory = PROFILE_CACHE.computeIfAbsent(element, t -> ProfileFactory.resolve(plugin, t));
                    List<Object> profiles = factory.buildAll(plugin, hoverLines);
                    if (profiles.isEmpty()) {
                        continue;
                    }
                    if (writeListField(event, field, new ArrayList<>(profiles))) {
                        written++;
                    }
                }
                current = current.getSuperclass();
            }
            return written;
        }

        /** True if {@code type} is one of the known server-list sample element types. */
        private static boolean isProfileType(Class<?> type) {
            String name = type.getName();
            return "com.destroystokyo.paper.profile.PlayerProfile".equals(name)
                    || "org.bukkit.profile.PlayerProfile".equals(name)
                    || "com.mojang.authlib.GameProfile".equals(name)
                    || PAPER_LISTED_PLAYER_INFO.equals(name);
        }

        /** Current size reported by the sample getter, or {@code -1} when unavailable. */
        private int currentSampleSize(ServerListPingEvent event) {
            if (sampleGetter == MISSING_METHOD) {
                return -1;
            }
            try {
                Object now = sampleGetter.invoke(event);
                return now instanceof List ? ((List<?>) now).size() : -1;
            } catch (Throwable t) {
                return -1;
            }
        }

        /**
         * Element types to attempt, in order: the type detected from the event's
         * generics first, then the common cross-version sample types. Duplicates
         * and absent classes are skipped.
         */
        private List<Class<?>> candidateSampleTypes() {
            List<Class<?>> types = new ArrayList<>(5);
            addCandidate(types, sampleType);
            addCandidate(types, findClass("com.destroystokyo.paper.profile.PlayerProfile"));
            addCandidate(types, findClass("org.bukkit.profile.PlayerProfile"));
            addCandidate(types, findClass("com.mojang.authlib.GameProfile"));
            addCandidate(types, findClass(PAPER_LISTED_PLAYER_INFO));
            return types;
        }

        private static void addCandidate(List<Class<?>> types, Class<?> type) {
            if (type != null && type != MISSING_CLASS && !types.contains(type)) {
                types.add(type);
            }
        }

        /**
         * Applies the built profiles via the setter (if any), then by mutating the
         * getter's list, and finally — for modern Paper, whose
         * {@code getPlayerSample()} returns an <em>unmodifiable</em> list with no
         * setter — by overwriting the event's backing {@code List} field with a
         * mutable copy. On a type mismatch ({@link ClassCastException}) the
         * getter's list is restored so a failed attempt doesn't wipe a real
         * sample. Returns {@code "ok:..."} on success.
         */
        private String trySetSample(ServerListPingEvent event, List<Object> profiles) {
            if (sampleSetter != MISSING_METHOD) {
                try {
                    sampleSetter.invoke(event, profiles);
                    return "ok:setter x" + profiles.size();
                } catch (Throwable ignored) {
                    // fall through to getter mutation
                }
            }
            if (sampleGetter == MISSING_METHOD) {
                return "no-method";
            }
            Object current;
            try {
                current = sampleGetter.invoke(event);
            } catch (Throwable t) {
                return "getter-threw-" + t.getClass().getSimpleName();
            }
            if (!(current instanceof List)) {
                return "non-list";
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) current;
            List<Object> backup = new ArrayList<>(list);
            try {
                list.clear();
                list.addAll(profiles);
                return "ok:mutate x" + profiles.size();
            } catch (UnsupportedOperationException immutable) {
                // Modern Paper: the sample list is unmodifiable. Swap the event's
                // backing field for a mutable copy carrying our profiles.
                String replaced = overwriteSampleField(event, current, profiles);
                return replaced != null ? replaced : "immutable";
            } catch (ClassCastException cce) {
                restore(list, backup);
                return "cce";
            } catch (Throwable t) {
                restore(list, backup);
                return "addAll-" + t.getClass().getSimpleName();
            }
        }

        /**
         * Overwrites the {@code List} field on {@code event} that currently holds
         * {@code currentSample} (the object the getter returned) with a fresh
         * mutable {@link ArrayList} of {@code profiles}. Matching the field by its
         * <em>current value</em> (identity) rather than by name makes this robust
         * across Paper versions and obfuscation. A plain {@code ArrayList} performs
         * no element-type check, so it sidesteps both the immutability and the
         * element-cast issues of the API-returned list. Returns an {@code "ok:..."}
         * tag on success, or {@code null} when no matching field could be written.
         */
        private String overwriteSampleField(ServerListPingEvent event, Object currentSample, List<Object> profiles) {
            if (currentSample == null) {
                return null;
            }
            // Pass 1 — identity match: the getter returned the backing field itself
            // (just as an unmodifiable view of the same object). Most precise.
            String byIdentity = overwriteListFields(event, profiles, currentSample, true);
            if (byIdentity != null) {
                return byIdentity;
            }
            // Pass 2 — the getter returns a FRESH unmodifiable copy each call, so no
            // field is identity-equal to it. Write each List field, then confirm via
            // the getter that the sample now reflects our profiles; restore otherwise.
            return overwriteListFields(event, profiles, null, false);
        }

        /**
         * Iterates the event's {@code List}-typed instance fields and writes a
         * mutable copy of {@code profiles}. When {@code requireIdentity} is set,
         * only the field whose current value {@code ==} {@code identityTarget} is
         * written. Otherwise every List field is tried and the write is kept only
         * if {@link #sampleReflects} confirms the getter now returns our profiles
         * (the field is restored when it doesn't, so an unrelated List isn't
         * clobbered). Returns an {@code "ok:..."} tag, or {@code null}.
         */
        private String overwriteListFields(ServerListPingEvent event, List<Object> profiles,
                                           Object identityTarget, boolean requireIdentity) {
            Class<?> current = event.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || !List.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    Object previous;
                    try {
                        field.setAccessible(true);
                        previous = field.get(event);
                    } catch (Throwable notReadable) {
                        continue;
                    }
                    if (requireIdentity && previous != identityTarget) {
                        continue;
                    }
                    if (!writeListField(event, field, new ArrayList<>(profiles))) {
                        continue;
                    }
                    if (requireIdentity || sampleReflects(event, profiles.size())) {
                        return "ok:field-replace x" + profiles.size();
                    }
                    // Write took no visible effect on the getter — undo it so an
                    // unrelated List field isn't left clobbered, and keep scanning.
                    writeListField(event, field, previous);
                }
                current = current.getSuperclass();
            }
            return null;
        }

        /** True when the sample getter now reports {@code expectedSize} entries. */
        private boolean sampleReflects(ServerListPingEvent event, int expectedSize) {
            if (sampleGetter == MISSING_METHOD) {
                return false;
            }
            try {
                Object now = sampleGetter.invoke(event);
                return now instanceof List && ((List<?>) now).size() == expectedSize;
            } catch (Throwable t) {
                return false;
            }
        }

        /** Writes {@code value} to {@code field}, falling back to Unsafe when reflection is blocked. */
        private static boolean writeListField(Object instance, Field field, Object value) {
            try {
                field.setAccessible(true);
                field.set(instance, value);
                return true;
            } catch (Throwable reflectionBlocked) {
                try {
                    ProfileFactory.UnsafeFieldWriter.set(instance, field, value);
                    return true;
                } catch (Throwable unsafeFailed) {
                    return false;
                }
            }
        }

        private static void restore(List<Object> list, List<Object> backup) {
            try {
                list.clear();
                list.addAll(backup);
            } catch (Throwable ignored) {
                // best effort
            }
        }
    }

    /** Builds sample profiles using a per-sample-type strategy resolved once. */
    private static final class ProfileFactory {
        private static final ProfileFactory NONE = new ProfileFactory(null, null, null, null);

        private final Strategy strategy;
        private final Class<?> sampleType;
        private final Constructor<?> constructor;
        private final ProfileSource profileSource;
        /** GAME_PROFILE_WRAP only: how a raw GameProfile becomes a CraftPlayerProfile. */
        private final Constructor<?> wrapperCtor;
        private final Method wrapperMethod;
        /** First failure captured from build(), so subsequent diagnostics carry the cause. */
        private volatile String firstFailure;

        private ProfileFactory(Strategy strategy, Class<?> sampleType, Constructor<?> constructor, ProfileSource profileSource) {
            this(strategy, sampleType, constructor, profileSource, null, null);
        }

        private ProfileFactory(Strategy strategy, Class<?> sampleType, Constructor<?> constructor,
                               ProfileSource profileSource, Constructor<?> wrapperCtor, Method wrapperMethod) {
            this.strategy = strategy;
            this.sampleType = sampleType;
            this.constructor = constructor;
            this.profileSource = profileSource;
            this.wrapperCtor = wrapperCtor;
            this.wrapperMethod = wrapperMethod;
        }

        /** Compact description used by the one-time ping diagnostic. */
        String describe() {
            String base = (strategy == null ? "NONE" : strategy.name())
                    + (constructor != null ? "+ctor" : "")
                    + (profileSource != null ? "+src" : "")
                    + (wrapperCtor != null ? "+wrapCtor" : "")
                    + (wrapperMethod != null ? "+wrapFn" : "");
            return firstFailure == null ? base : base + "; first-fail=" + firstFailure;
        }

        static ProfileFactory resolve(ObxPlugin plugin, Class<?> sampleType) {
            if (sampleType == null) {
                return NONE;
            }
            String typeName = sampleType.getName();
            if ("org.bukkit.profile.PlayerProfile".equals(typeName) || "com.destroystokyo.paper.profile.PlayerProfile".equals(typeName)) {
                // The server-list sample wants the platform's PlayerProfile
                // implementation (CraftPlayerProfile). The hover "names" are
                // arbitrary coloured text, which the public createProfile /
                // createPlayerProfile factories may reject (≤16 chars,
                // [A-Za-z0-9_]). We resolve BOTH paths and let build() try them
                // in order at runtime (see buildPlayerProfile):
                //   1. a (UUID,String) factory (createProfileExact preferred — it
                //      skips name validation on platforms that expose it);
                //   2. wrapping a raw com.mojang.authlib.GameProfile (which never
                //      validates names) into a CraftPlayerProfile, then overwriting
                //      the wrapper's own name afterwards if its ctor sanitised it.
                // Carrying both makes the hover robust across Paper versions: older
                // builds keep a GameProfile field on CraftPlayerProfile; modern
                // builds (1.20.5+/1.21.x) store the name as a plain String field and
                // build the GameProfile on demand — so a single fixed strategy breaks
                // on one or the other.
                ProfileSource source = ProfileSource.detect(plugin, sampleType);
                ProfileFactory wrap = resolveGameProfileWrap(plugin, sampleType);
                if (source == null && wrap == null) {
                    return NONE;
                }
                Constructor<?> gpCtor = wrap == null ? null : wrap.constructor;
                Constructor<?> wrapCtor = wrap == null ? null : wrap.wrapperCtor;
                Method wrapMethod = wrap == null ? null : wrap.wrapperMethod;
                return new ProfileFactory(Strategy.PLAYER_PROFILE, sampleType, gpCtor, source, wrapCtor, wrapMethod);
            }
            if ("com.mojang.authlib.GameProfile".equals(typeName)) {
                Constructor<?> ctor = findConstructor(sampleType, UUID.class, String.class);
                return ctor == null ? NONE : new ProfileFactory(Strategy.GAME_PROFILE, sampleType, ctor, null);
            }
            if (PAPER_LISTED_PLAYER_INFO.equals(typeName)) {
                ProfileSource source = ProfileSource.detect(plugin, null);
                Constructor<?> chosen = chooseListedPlayerInfoConstructor(sampleType, source);
                if (chosen != null) {
                    return new ProfileFactory(Strategy.LISTED_PLAYER_INFO, sampleType, chosen, source);
                }
                return NONE;
            }
            return NONE;
        }

        /**
         * Resolves a builder that wraps a raw {@code com.mojang.authlib.GameProfile}
         * (which does NOT validate names) into the platform's {@code CraftPlayerProfile}
         * — the concrete type the server-list sample accepts — bypassing the name
         * validation that the public profile factories enforce. Returns {@code null}
         * when GameProfile or a suitable wrapper can't be found, so the caller falls
         * back to the public factory.
         */
        private static ProfileFactory resolveGameProfileWrap(ObxPlugin plugin, Class<?> sampleType) {
            Class<?> gpClass = findClass("com.mojang.authlib.GameProfile");
            if (gpClass == null || gpClass == MISSING_CLASS) {
                return null;
            }
            Constructor<?> gpCtor = findConstructor(gpClass, UUID.class, String.class);
            if (gpCtor == null) {
                return null;
            }
            Class<?> craftClass = findCraftPlayerProfileClass(plugin);
            if (craftClass == null || !sampleType.isAssignableFrom(craftClass)) {
                return null;
            }
            // Prefer a constructor taking a single GameProfile (CraftPlayerProfile(GameProfile)).
            for (Constructor<?> candidate : craftClass.getDeclaredConstructors()) {
                Class<?>[] params = candidate.getParameterTypes();
                if (params.length == 1 && params[0] == gpClass) {
                    candidate.setAccessible(true);
                    return new ProfileFactory(Strategy.GAME_PROFILE_WRAP, sampleType, gpCtor, null, candidate, null);
                }
            }
            // Otherwise a static factory taking a single GameProfile (asBukkitCopy / asBukkitMirror).
            for (Method method : craftClass.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == gpClass && sampleType.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return new ProfileFactory(Strategy.GAME_PROFILE_WRAP, sampleType, gpCtor, null, null, method);
                }
            }
            return null;
        }

        /** Locates the runtime {@code CraftPlayerProfile} implementation class. */
        private static Class<?> findCraftPlayerProfileClass(ObxPlugin plugin) {
            // Derive the CraftBukkit base package from the live server class, covering
            // both the modern unversioned package (1.20.5+) and the older versioned
            // org.bukkit.craftbukkit.v1_xx_Rn form.
            String base = plugin.getServer().getClass().getPackage().getName();
            Class<?> found = findClass(base + ".profile.CraftPlayerProfile");
            if (found != null && found != MISSING_CLASS) {
                return found;
            }
            found = findClass("org.bukkit.craftbukkit.profile.CraftPlayerProfile");
            return found == null || found == MISSING_CLASS ? null : found;
        }

        private static Constructor<?> chooseListedPlayerInfoConstructor(Class<?> sampleType, ProfileSource source) {
            for (Constructor<?> constructor : sampleType.getDeclaredConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (canSatisfy(types, source)) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
            return null;
        }

        private static boolean canSatisfy(Class<?>[] paramTypes, ProfileSource source) {
            for (Class<?> type : paramTypes) {
                if (type == UUID.class || type == String.class || CharSequence.class.isAssignableFrom(type)) {
                    continue;
                }
                if (type.isPrimitive() || type.isEnum()) {
                    continue;
                }
                if (source != null && source.profileType != null && type.isAssignableFrom(source.profileType)) {
                    continue;
                }
                if ("com.mojang.authlib.GameProfile".equals(type.getName())) {
                    continue;
                }
                return false;
            }
            return true;
        }

        List<Object> buildAll(ObxPlugin plugin, List<String> hoverLines) {
            if (strategy == null || hoverLines == null || hoverLines.isEmpty()) {
                return new ArrayList<>(0);
            }
            List<Object> profiles = new ArrayList<>(hoverLines.size());
            for (int i = 0; i < hoverLines.size(); i++) {
                String value = hoverLines.get(i) == null ? "" : hoverLines.get(i);
                UUID uuid = new UUID(0L, i + 1L);
                Object profile = build(plugin, uuid, value);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
            return profiles;
        }

        private Object build(ObxPlugin plugin, UUID uuid, String value) {
            try {
                switch (strategy) {
                    case GAME_PROFILE:
                        return constructor.newInstance(uuid, value);
                    case PLAYER_PROFILE:
                        return buildPlayerProfile(plugin, uuid, value);
                    case LISTED_PLAYER_INFO:
                        return constructor.newInstance(buildArguments(plugin, constructor.getParameterTypes(), uuid, value));
                    default:
                        return null;
                }
            } catch (Throwable t) {
                if (firstFailure == null) {
                    Throwable root = t;
                    while (root instanceof java.lang.reflect.InvocationTargetException && root.getCause() != null) {
                        root = root.getCause();
                    }
                    String message = root.getMessage();
                    firstFailure = root.getClass().getSimpleName()
                            + (message == null ? "" : ":" + (message.length() > 80 ? message.substring(0, 80) + "…" : message));
                }
                return null;
            }
        }

        /**
         * Returns a Minecraft-name-shaped stub (≤16 chars, {@code [A-Za-z0-9_]})
         * derived from the synthesised UUID. Used as a placeholder when
         * building the GameProfile before wrapping into a CraftPlayerProfile;
         * the wrapper validates against this clean name and only the real
         * hover text — written into the underlying GameProfile via
         * {@link #injectGameProfileName} after the wrapper succeeds — is
         * what gets rendered.
         */
        private static String stubName(UUID uuid) {
            long n = (uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits()) & 0x7FFFFFFFL;
            return "MOTD_" + n; // "MOTD_" (5) + up to 10 digits = up to 15 chars
        }

        /**
         * Builds one {@code PlayerProfile} (CraftPlayerProfile) carrying a
         * coloured hover line as its name, trying the available strategies in
         * order until one yields a profile whose {@code getName()} actually
         * returns the coloured value:
         *
         * <ol>
         *   <li><b>Factory method</b> — {@code createProfileExact} /
         *       {@code createProfile} / {@code createPlayerProfile}. On platforms
         *       whose factory does not validate the name this is the clean path,
         *       no field surgery needed.</li>
         *   <li><b>Wrap a coloured GameProfile</b> — {@code new GameProfile(uuid,
         *       value)} (authlib's ctor accepts arbitrary strings) wrapped via the
         *       {@code CraftPlayerProfile(GameProfile)} ctor / {@code asBukkit*}
         *       factory. If the wrapper copies the name verbatim, done.</li>
         *   <li><b>Stub + overwrite</b> — if the wrapper ctor sanitised/validated
         *       the name, rebuild it with a clean stub name and then overwrite the
         *       wrapper's own name (see {@link #setProfileName}). This is what
         *       makes modern Paper (1.20.5+/1.21.x) work: there {@code
         *       CraftPlayerProfile} stores the name as a plain {@code String}
         *       field and has no {@code GameProfile} field to swap.</li>
         * </ol>
         *
         * <p>Returns {@code null} (recording a diagnostic) only when every
         * strategy fails.
         */
        private Object buildPlayerProfile(ObxPlugin plugin, UUID uuid, String value) {
            // 1) Factory method (createProfileExact preferred).
            if (profileSource != null) {
                Object profile = profileSource.create(plugin, uuid, value);
                if (profile != null && nameEquals(profile, value)) {
                    return profile;
                }
            }
            // 2) + 3) Wrap a GameProfile, overwriting the name if the ctor sanitised it.
            if (constructor != null && (wrapperCtor != null || wrapperMethod != null)) {
                // 2) Direct: wrap a GameProfile already carrying the coloured name.
                try {
                    Object coloured = constructor.newInstance(uuid, value);
                    Object wrapped = wrapImpl(coloured);
                    if (wrapped != null) {
                        if (nameEquals(wrapped, value) || setProfileName(wrapped, uuid, value)) {
                            return wrapped;
                        }
                    }
                } catch (Throwable validatedInCtor) {
                    recordFailure(validatedInCtor);
                    // ctor validated the coloured name — fall through to stub+overwrite
                }
                // 3) Stub name to satisfy a validating ctor, then overwrite with colour.
                try {
                    Object safe = constructor.newInstance(uuid, stubName(uuid));
                    Object wrapped = wrapImpl(safe);
                    if (wrapped != null && setProfileName(wrapped, uuid, value)) {
                        return wrapped;
                    }
                } catch (Throwable t) {
                    recordFailure(t);
                }
            }
            if (firstFailure == null) {
                firstFailure = "PlayerProfile:all-strategies-failed(source=" + (profileSource != null)
                        + ",wrapCtor=" + (wrapperCtor != null) + ",wrapFn=" + (wrapperMethod != null) + ")";
            }
            return null;
        }

        /** Invokes the wrapper ctor (or static {@code asBukkit*} factory) for {@code gameProfile}. */
        private Object wrapImpl(Object gameProfile) throws Exception {
            if (wrapperCtor != null) {
                return wrapperCtor.newInstance(gameProfile);
            }
            return wrapperMethod.invoke(null, gameProfile);
        }

        /**
         * Overwrites the name carried by a freshly-built {@code CraftPlayerProfile}
         * wrapper so it returns the coloured hover text, handling both wrapper
         * shapes seen across versions:
         *
         * <ul>
         *   <li><b>Older Paper</b> — the wrapper keeps a backing
         *       {@code com.mojang.authlib.GameProfile} field; we replace it with a
         *       coloured {@code GameProfile} (authlib accepts the name; the field is
         *       on a regular class so the write is allowed, with an Unsafe
         *       fallback for {@code final}).</li>
         *   <li><b>Modern Paper (1.20.5+/1.21.x)</b> — the wrapper stores the name
         *       as a plain {@code String} field and builds the GameProfile on
         *       demand. We find that field by matching its <em>current</em> value
         *       (the stub the wrapper was just built with), which survives field
         *       renames and obfuscation, then overwrite it. A field literally named
         *       {@code "name"} is the last-resort fallback.</li>
         * </ul>
         *
         * @return {@code true} once {@code getName()} reports the coloured value.
         */
        private boolean setProfileName(Object wrapper, UUID uuid, String value) {
            // (a) Replace a backing com.mojang.authlib.GameProfile field.
            Class<?> gpClass = findClass("com.mojang.authlib.GameProfile");
            if (constructor != null && gpClass != null && gpClass != MISSING_CLASS) {
                Class<?> current = wrapper.getClass();
                while (current != null) {
                    for (Field field : current.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        if (!field.getType().isAssignableFrom(gpClass)) {
                            continue;
                        }
                        try {
                            Object colouredProfile = constructor.newInstance(uuid, value);
                            if (writeField(wrapper, field, colouredProfile) && nameEquals(wrapper, value)) {
                                return true;
                            }
                        } catch (Throwable ignored) {
                            // try the next candidate field
                        }
                    }
                    current = current.getSuperclass();
                }
            }
            // (b) Overwrite the String field that currently holds the stub name.
            String stub = stubName(uuid);
            Class<?> current = wrapper.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        if (stub.equals(field.get(wrapper)) && writeField(wrapper, field, value) && nameEquals(wrapper, value)) {
                            return true;
                        }
                    } catch (Throwable ignored) {
                        // try the next candidate field
                    }
                }
                current = current.getSuperclass();
            }
            // (c) Last resort: a String field literally named "name".
            current = wrapper.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || field.getType() != String.class || !"name".equals(field.getName())) {
                        continue;
                    }
                    if (writeField(wrapper, field, value) && nameEquals(wrapper, value)) {
                        return true;
                    }
                }
                current = current.getSuperclass();
            }
            return false;
        }

        /** Writes {@code value} to {@code field}, falling back to Unsafe when reflection is blocked. */
        private static boolean writeField(Object instance, Field field, Object value) {
            try {
                field.setAccessible(true);
                field.set(instance, value);
                return true;
            } catch (Throwable reflectionBlocked) {
                try {
                    UnsafeFieldWriter.set(instance, field, value);
                    return true;
                } catch (Throwable unsafeFailed) {
                    return false;
                }
            }
        }

        /** Best-effort check that {@code profile.getName()} equals {@code expected}. */
        private static boolean nameEquals(Object profile, String expected) {
            try {
                Method getName = profile.getClass().getMethod("getName");
                return expected.equals(getName.invoke(profile));
            } catch (Throwable t) {
                return false;
            }
        }

        /** Records the first underlying failure (unwrapping reflection wrappers) for diagnostics. */
        private void recordFailure(Throwable t) {
            if (firstFailure != null) {
                return;
            }
            Throwable root = t;
            while (root instanceof java.lang.reflect.InvocationTargetException && root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getMessage();
            firstFailure = root.getClass().getSimpleName()
                    + (message == null ? "" : ":" + (message.length() > 80 ? message.substring(0, 80) + "…" : message));
        }

        /**
         * Sets a {@code final} instance field via {@code sun.misc.Unsafe},
         * the universal bypass for JDK 17+'s reflective-write guard. The
         * {@code Unsafe.putObject} intrinsic is a raw memory store — the
         * JVM never re-checks final-ness — so the value lands regardless
         * of the field's modifiers. Resolved reflectively so the class
         * file itself doesn't reference {@code sun.misc.Unsafe} at
         * compile time, keeping cross-JDK loads clean.
         */
        private static final class UnsafeFieldWriter {
            private static final Object UNSAFE;
            private static final Method PUT_OBJECT;
            private static final Method OBJECT_FIELD_OFFSET;

            static {
                Object unsafe = null;
                Method put = null;
                Method offset = null;
                try {
                    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    unsafe = theUnsafe.get(null);
                    put = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
                    offset = unsafeClass.getMethod("objectFieldOffset", Field.class);
                } catch (Throwable unavailable) {
                    // sun.misc.Unsafe absent (very stripped JRE) — set() will
                    // throw and the caller's build() catch records the cause.
                }
                UNSAFE = unsafe;
                PUT_OBJECT = put;
                OBJECT_FIELD_OFFSET = offset;
            }

            static void set(Object instance, Field field, Object value) throws Exception {
                if (UNSAFE == null) {
                    throw new IllegalStateException("sun.misc.Unsafe unavailable");
                }
                long fieldOffset = (Long) OBJECT_FIELD_OFFSET.invoke(UNSAFE, field);
                PUT_OBJECT.invoke(UNSAFE, instance, fieldOffset, value);
            }
        }

        private Object[] buildArguments(ObxPlugin plugin, Class<?>[] paramTypes, UUID uuid, String value) {
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> type = paramTypes[i];
                if (type == UUID.class) {
                    args[i] = uuid;
                } else if (type == String.class || CharSequence.class.isAssignableFrom(type)) {
                    args[i] = value;
                } else if (type.isPrimitive()) {
                    args[i] = primitiveZero(type);
                } else if (type.isEnum()) {
                    Object[] constants = type.getEnumConstants();
                    args[i] = constants.length > 0 ? constants[0] : null;
                } else if (profileSource != null && profileSource.profileType != null && type.isAssignableFrom(profileSource.profileType)) {
                    args[i] = profileSource.create(plugin, uuid, value);
                } else if ("com.mojang.authlib.GameProfile".equals(type.getName())) {
                    Class<?> gpClass = findClass("com.mojang.authlib.GameProfile");
                    Constructor<?> gpCtor = gpClass == null || gpClass == MISSING_CLASS ? null : findConstructor(gpClass, UUID.class, String.class);
                    args[i] = gpCtor == null ? null : safeNewInstance(gpCtor, uuid, value);
                } else {
                    args[i] = null;
                }
            }
            return args;
        }

        private static Object safeNewInstance(Constructor<?> ctor, Object... args) {
            try {
                return ctor.newInstance(args);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static Object primitiveZero(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            return null;
        }

        private enum Strategy {
            GAME_PROFILE,
            GAME_PROFILE_WRAP,
            PLAYER_PROFILE,
            LISTED_PLAYER_INFO
        }
    }

    /** Resolved once: which factory method (if any) builds a Bukkit/Paper PlayerProfile. */
    private static final class ProfileSource {
        final Method method;
        final boolean isStatic;
        final Class<?> profileType;

        private ProfileSource(Method method, boolean isStatic, Class<?> profileType) {
            this.method = method;
            this.isStatic = isStatic;
            this.profileType = profileType;
        }

        static ProfileSource detect(ObxPlugin plugin, Class<?> expectedType) {
            Class<?> serverClass = plugin.getServer().getClass();
            // createProfileExact performs NO name validation. The MOTD hover
            // "names" are arbitrary coloured text (e.g. "§eOnline: 5/20"), which
            // the validating createProfile / createPlayerProfile reject on modern
            // Paper — that rejection is the StandardPaperServerListPingEventImpl
            // "PlayerProfile:no-build" failure. Prefer the exact factory so the
            // sample profiles actually build; only fall back to the validating
            // factories on older platforms that lack it (those were lenient
            // about names anyway).
            ProfileSource source = tryFactory(Bukkit.class, "createProfileExact", true, expectedType);
            if (source == null) {
                source = tryFactory(serverClass, "createProfileExact", false, expectedType);
            }
            if (source == null) {
                source = tryFactory(Bukkit.class, "createProfile", true, expectedType);
            }
            if (source == null) {
                source = tryFactory(serverClass, "createPlayerProfile", false, expectedType);
            }
            if (source == null) {
                source = tryFactory(serverClass, "createProfile", false, expectedType);
            }
            return source;
        }

        /**
         * Resolves a {@code (UUID, String) -> PlayerProfile} factory method on the
         * given holder, returning {@code null} when it is absent or its return
         * type isn't assignable to {@code expectedType}.
         */
        private static ProfileSource tryFactory(Class<?> holder, String methodName, boolean isStatic, Class<?> expectedType) {
            Method method = findMethod(holder, methodName, UUID.class, String.class);
            if (method == MISSING_METHOD) {
                return null;
            }
            if (expectedType != null && !expectedType.isAssignableFrom(method.getReturnType())) {
                return null;
            }
            return new ProfileSource(method, isStatic, method.getReturnType());
        }

        Object create(ObxPlugin plugin, UUID uuid, String value) {
            try {
                Object target = isStatic ? null : plugin.getServer();
                return method.invoke(target, uuid, value);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        if (type == null) {
            return MISSING_METHOD;
        }
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            // fall through to declared search up the inheritance chain
        }
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameters);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return MISSING_METHOD;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return MISSING_FIELD;
    }

    private static Constructor<?> findConstructor(Class<?> type, Class<?>... parameters) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return MISSING_CLASS;
        }
    }

    private static Method sentinelMethod() {
        try {
            return Object.class.getMethod("toString");
        } catch (NoSuchMethodException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Field sentinelField() {
        // Use a stable JDK field rather than reflecting on one of THIS class's
        // own fields by name: under obfuscation our field names are renamed, so
        // a getDeclaredField("MISSING_METHOD") would only survive if ProGuard
        // happens to adapt the string literal. java.lang.Integer.MAX_VALUE is in
        // the JDK (never obfuscated) and always present — it's only ever used as
        // a unique non-null sentinel reference, never read.
        try {
            return Integer.class.getDeclaredField("MAX_VALUE");
        } catch (NoSuchFieldException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Sentinel marker used so a class-not-found result is cacheable. */
    private static final class MissingClassMarker {
    }
}