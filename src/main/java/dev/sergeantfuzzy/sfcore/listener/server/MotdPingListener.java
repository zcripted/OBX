package dev.sergeantfuzzy.sfcore.listener.server;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.MotdService;
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
 * Server-list ping handler that injects SF-Core's configured MOTD, displayed counts,
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

    private final Main plugin;

    public MotdPingListener(Main plugin) {
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
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "MOTD",
                    "Registered Paper ping listener for " + paperEventClass.getName()
                            + " (enables the player-count hover).");
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
        // Log once per distinct event class: INFO when the hover was applied,
        // WARNING when it couldn't be. Helps confirm the sample reached the wire
        // (any remaining "no hover" is then client-side cache or a proxy stripping
        // the sample, not SF-Core).
        if (!LOGGED_PING_CLASSES.add(event.getClass().getName())) {
            return;
        }
        String message = "ping handled: event=" + event.getClass().getName()
                + ", hoverLines=" + hoverLineCount + ", sample=" + sampleResult;
        if (sampleResult != null && sampleResult.startsWith("ok")) {
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "MOTD", message);
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
        String applyHoverSample(Main plugin, ServerListPingEvent event, List<String> hoverLines) {
            if (sampleGetter == MISSING_METHOD && sampleSetter == MISSING_METHOD) {
                return "fail:no-sample-api on " + event.getClass().getName();
            }
            StringBuilder tried = new StringBuilder();
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
                    return outcome + " type=" + type.getSimpleName();
                }
                tried.append(type.getSimpleName()).append(':').append(outcome).append(' ');
            }
            return "fail:no-candidate-accepted [" + tried.toString().trim() + "]";
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
         * Applies the built profiles via the setter (if any) or by mutating the
         * getter's list. On a type mismatch ({@link ClassCastException}) the list
         * is restored to its original contents so a failed attempt doesn't wipe a
         * real player sample. Returns {@code "ok:..."} on success.
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
                return "immutable";
            } catch (ClassCastException cce) {
                restore(list, backup);
                return "cce";
            } catch (Throwable t) {
                restore(list, backup);
                return "addAll-" + t.getClass().getSimpleName();
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

        static ProfileFactory resolve(Main plugin, Class<?> sampleType) {
            if (sampleType == null) {
                return NONE;
            }
            String typeName = sampleType.getName();
            if ("org.bukkit.profile.PlayerProfile".equals(typeName) || "com.destroystokyo.paper.profile.PlayerProfile".equals(typeName)) {
                // PRIMARY: wrap a non-validating com.mojang.authlib.GameProfile into a
                // CraftPlayerProfile. The public createProfile / createPlayerProfile
                // factories validate the profile NAME (≤16 chars, [A-Za-z0-9_]) and so
                // reject the coloured MOTD hover text — that rejection is the
                // "PlayerProfile:no-build" failure. GameProfile performs no name
                // validation, and a CraftPlayerProfile built from it is accepted by the
                // sample list (it implements PlayerProfile).
                ProfileFactory wrap = resolveGameProfileWrap(plugin, sampleType);
                if (wrap != null) {
                    return wrap;
                }
                // FALLBACK (older platforms / non-Paper): the validating factory methods.
                ProfileSource source = ProfileSource.detect(plugin, sampleType);
                return source == null ? NONE : new ProfileFactory(Strategy.PLAYER_PROFILE, sampleType, null, source);
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
        private static ProfileFactory resolveGameProfileWrap(Main plugin, Class<?> sampleType) {
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
        private static Class<?> findCraftPlayerProfileClass(Main plugin) {
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

        List<Object> buildAll(Main plugin, List<String> hoverLines) {
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

        private Object build(Main plugin, UUID uuid, String value) {
            try {
                switch (strategy) {
                    case GAME_PROFILE:
                        return constructor.newInstance(uuid, value);
                    case GAME_PROFILE_WRAP: {
                        // Build the GameProfile with a name-safe stub so any
                        // validation in either the authlib ctor or the
                        // CraftPlayerProfile wrapper accepts it. The coloured
                        // MOTD hover text is what newer Paper rejects in the
                        // wrapper — that rejection is the "PlayerProfile:
                        // no-build(GAME_PROFILE_WRAP+ctor+wrapCtor)" failure.
                        // After wrapping, overwrite the underlying
                        // GameProfile.name field directly so the rendered
                        // sample carries the actual hover text. The wrapper
                        // delegates getName() to the underlying profile, so
                        // the post-construction write is what the client
                        // ultimately sees.
                        String stub = stubName(uuid);
                        Object gameProfile = constructor.newInstance(uuid, stub);
                        Object wrapped;
                        if (wrapperCtor != null) {
                            wrapped = wrapperCtor.newInstance(gameProfile);
                        } else {
                            wrapped = wrapperMethod.invoke(null, gameProfile);
                        }
                        injectGameProfileName(gameProfile, value);
                        return wrapped;
                    }
                    case PLAYER_PROFILE:
                        return profileSource.create(plugin, uuid, value);
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
         * Reflectively overwrites the {@code name} String field on the
         * {@code com.mojang.authlib.GameProfile} (or any superclass) so the
         * rendered hover sample shows the actual MOTD text rather than the
         * stub used to satisfy validation at wrapper-construction time.
         * Throws on failure so the outer build() catch records the cause.
         */
        private static void injectGameProfileName(Object gameProfile, String name) throws Exception {
            Class<?> current = gameProfile.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.getType() == String.class && "name".equals(field.getName())) {
                        field.setAccessible(true);
                        field.set(gameProfile, name);
                        return;
                    }
                }
                current = current.getSuperclass();
            }
            throw new NoSuchFieldException("com.mojang.authlib.GameProfile.name");
        }

        private Object[] buildArguments(Main plugin, Class<?>[] paramTypes, UUID uuid, String value) {
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

        static ProfileSource detect(Main plugin, Class<?> expectedType) {
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

        Object create(Main plugin, UUID uuid, String value) {
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
