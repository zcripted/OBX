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

    private static final Method MISSING_METHOD = sentinelMethod();
    private static final Field MISSING_FIELD = sentinelField();
    private static final Class<?> MISSING_CLASS = MissingClassMarker.class;

    private final Main plugin;

    public MotdPingListener(Main plugin) {
        this.plugin = plugin;
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
            return;
        }
        eventOps.applyHidePlayers(event, false);
        eventOps.applyHoverSample(plugin, event, effectiveLines);
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
        Class<?> paperListedPlayerInfo = findClass(PAPER_LISTED_PLAYER_INFO);
        if (paperListedPlayerInfo != null && paperListedPlayerInfo != MISSING_CLASS) {
            return paperListedPlayerInfo;
        }
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
        return sampleTypeFromParam(findMethod(eventClass, "setListedPlayers", List.class), 0);
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

        void applyHoverSample(Main plugin, ServerListPingEvent event, List<String> hoverLines) {
            ProfileFactory factory = sampleType == null
                    ? null
                    : PROFILE_CACHE.computeIfAbsent(sampleType, type -> ProfileFactory.resolve(plugin, type));
            List<Object> profiles = factory == null
                    ? new ArrayList<>(0)
                    : factory.buildAll(plugin, hoverLines);
            if (profiles.isEmpty()) {
                return;
            }
            if (sampleSetter != MISSING_METHOD) {
                try {
                    sampleSetter.invoke(event, profiles);
                    return;
                } catch (Exception ignored) {
                    // fall through to in-place mutation
                }
            }
            if (sampleGetter != MISSING_METHOD) {
                try {
                    Object current = sampleGetter.invoke(event);
                    if (current instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) current;
                        list.clear();
                        list.addAll(profiles);
                    }
                } catch (Exception ignored) {
                    // best-effort
                }
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

        private ProfileFactory(Strategy strategy, Class<?> sampleType, Constructor<?> constructor, ProfileSource profileSource) {
            this.strategy = strategy;
            this.sampleType = sampleType;
            this.constructor = constructor;
            this.profileSource = profileSource;
        }

        static ProfileFactory resolve(Main plugin, Class<?> sampleType) {
            if (sampleType == null) {
                return NONE;
            }
            String typeName = sampleType.getName();
            if ("org.bukkit.profile.PlayerProfile".equals(typeName) || "com.destroystokyo.paper.profile.PlayerProfile".equals(typeName)) {
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
                    case PLAYER_PROFILE:
                        return profileSource.create(plugin, uuid, value);
                    case LISTED_PLAYER_INFO:
                        return constructor.newInstance(buildArguments(plugin, constructor.getParameterTypes(), uuid, value));
                    default:
                        return null;
                }
            } catch (Throwable ignored) {
                return null;
            }
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
            Method bukkitCreate = findMethod(Bukkit.class, "createProfile", UUID.class, String.class);
            if (bukkitCreate != MISSING_METHOD && (expectedType == null || expectedType.isAssignableFrom(bukkitCreate.getReturnType()))) {
                return new ProfileSource(bukkitCreate, true, bukkitCreate.getReturnType());
            }
            Class<?> serverClass = plugin.getServer().getClass();
            Method serverPlayerProfile = findMethod(serverClass, "createPlayerProfile", UUID.class, String.class);
            if (serverPlayerProfile != MISSING_METHOD && (expectedType == null || expectedType.isAssignableFrom(serverPlayerProfile.getReturnType()))) {
                return new ProfileSource(serverPlayerProfile, false, serverPlayerProfile.getReturnType());
            }
            Method serverProfile = findMethod(serverClass, "createProfile", UUID.class, String.class);
            if (serverProfile != MISSING_METHOD && (expectedType == null || expectedType.isAssignableFrom(serverProfile.getReturnType()))) {
                return new ProfileSource(serverProfile, false, serverProfile.getReturnType());
            }
            return null;
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
        try {
            return MotdPingListener.class.getDeclaredField("MISSING_METHOD");
        } catch (NoSuchFieldException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Sentinel marker used so a class-not-found result is cacheable. */
    private static final class MissingClassMarker {
    }
}
