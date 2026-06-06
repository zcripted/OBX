package dev.zcripted.obx.feature.economy;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/**
 * Registers OBX's economy as a Vault provider — but only if the Vault plugin
 * is installed on the server. To keep the JAR free of compile-time references to
 * Vault (an optional dependency), the provider class is implemented as a JDK
 * dynamic proxy that satisfies the {@code net.milkbowl.vault.economy.Economy}
 * contract reflectively. If Vault is missing the registration is a no-op.
 */
public final class VaultEconomyProvider {

    private VaultEconomyProvider() {}

    public static boolean register(ObxPlugin plugin, EconomyService economy) {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null) {
            return false;
        }
        try {
            Class<?> economyInterface = Class.forName("net.milkbowl.vault.economy.Economy");
            Object proxy = Proxy.newProxyInstance(
                    economyInterface.getClassLoader(),
                    new Class<?>[]{economyInterface},
                    new EconomyHandler(plugin, economy));
            // Reflective register() — the typed Bukkit signature is
            // <T> register(Class<T>, T, Plugin, ServicePriority), which the
            // compiler can't satisfy when the interface is loaded reflectively.
            Object services = Bukkit.getServicesManager();
            java.lang.reflect.Method registerMethod = services.getClass().getMethod(
                    "register", Class.class, Object.class, Plugin.class, org.bukkit.plugin.ServicePriority.class);
            registerMethod.invoke(services, economyInterface, proxy, plugin, org.bukkit.plugin.ServicePriority.Normal);
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Registered §d§lOBX§r§7 as the economy system provider.");
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to register Vault economy provider: " + throwable.getMessage());
            return false;
        }
    }

    /**
     * Removes every Vault service registration owned by this plugin. Called on module
     * disable so a runtime disable/re-enable cycle can't leave a stale proxy (pointing
     * at an old EconomyService) registered or stack a duplicate on re-enable.
     */
    public static void unregister(ObxPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Object services = Bukkit.getServicesManager();
            java.lang.reflect.Method unregisterAll = services.getClass().getMethod("unregisterAll", Plugin.class);
            unregisterAll.invoke(services, plugin);
        } catch (Throwable ignored) {
            // Vault gone or API shape changed — Bukkit clears our registrations on full disable anyway.
        }
    }

    private static final class EconomyHandler implements InvocationHandler {
        private final ObxPlugin plugin;
        private final EconomyService economy;

        EconomyHandler(ObxPlugin plugin, EconomyService economy) {
            this.plugin = plugin;
            this.economy = economy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            Class<?>[] params = method.getParameterTypes();
            switch (name) {
                case "isEnabled": return true;
                case "getName": return "OBX";
                case "hasBankSupport": return false;
                case "fractionalDigits": return 2;
                case "format": return economy.format(args == null ? 0.0 : ((Number) args[0]).doubleValue());
                case "currencyNamePlural": return economy.getCurrencyNamePlural();
                case "currencyNameSingular": return economy.getCurrencyName();
                case "hasAccount":
                    return resolveOfflinePlayer(args[0]) != null;
                case "getBalance":
                    return economy.getBalance(resolveUuid(args[0]));
                case "has": {
                    double balance = economy.getBalance(resolveUuid(args[0]));
                    double amount = ((Number) args[args.length - 1]).doubleValue();
                    return balance >= amount;
                }
                case "withdrawPlayer": {
                    OfflinePlayer p = resolveOfflinePlayer(args[0]);
                    double amount = ((Number) args[args.length - 1]).doubleValue();
                    if (p == null) {
                        return buildEconomyResponse(method.getReturnType(), 0.0, amount, "FAILURE", "Unknown player");
                    }
                    // Reject non-positive / non-finite up front: withdraw() sanitizes a negative to 0
                    // and returns a no-op true, which would otherwise report SUCCESS to a Vault caller.
                    if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                        return buildEconomyResponse(method.getReturnType(), economy.getBalance(p.getUniqueId()), amount, "FAILURE", "Cannot withdraw a non-positive amount");
                    }
                    boolean ok = economy.withdraw(p.getUniqueId(), p.getName(), amount);
                    return buildEconomyResponse(method.getReturnType(), economy.getBalance(p.getUniqueId()), amount, ok ? "SUCCESS" : "FAILURE", ok ? null : "Insufficient funds");
                }
                case "depositPlayer": {
                    OfflinePlayer p = resolveOfflinePlayer(args[0]);
                    double amount = ((Number) args[args.length - 1]).doubleValue();
                    if (p == null) {
                        return buildEconomyResponse(method.getReturnType(), 0.0, amount, "FAILURE", "Unknown player");
                    }
                    if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                        return buildEconomyResponse(method.getReturnType(), economy.getBalance(p.getUniqueId()), amount, "FAILURE", "Cannot deposit a non-positive amount");
                    }
                    // deposit() is void and silently clamps at the max balance, so measure the actual
                    // delta and report FAILURE if nothing moved — Vault callers trust the response type.
                    double before = economy.getBalance(p.getUniqueId());
                    economy.deposit(p.getUniqueId(), p.getName(), amount);
                    double after = economy.getBalance(p.getUniqueId());
                    boolean moved = after > before;
                    return buildEconomyResponse(method.getReturnType(), after, after - before, moved ? "SUCCESS" : "FAILURE", moved ? null : "Deposit had no effect");
                }
                case "createPlayerAccount": {
                    OfflinePlayer p = resolveOfflinePlayer(args[0]);
                    if (p == null) {
                        return false;
                    }
                    economy.ensureAccount(p.getUniqueId(), p.getName());
                    return true;
                }
                case "getBanks": return Collections.emptyList();
                case "toString": return "OBX EconomyProvider";
                case "equals": return proxy == args[0];
                case "hashCode": return System.identityHashCode(proxy);
                default:
                    // Bank methods + any unknown — return a generic failure response.
                    if (method.getReturnType().getName().equals("net.milkbowl.vault.economy.EconomyResponse")) {
                        return buildEconomyResponse(method.getReturnType(), 0.0, 0.0, "NOT_IMPLEMENTED", "Bank features not supported");
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == double.class) return 0.0;
                    if (method.getReturnType() == int.class) return 0;
                    if (List.class.isAssignableFrom(method.getReturnType())) return Collections.emptyList();
                    return null;
            }
        }

        private OfflinePlayer resolveOfflinePlayer(Object arg) {
            if (arg == null) return null;
            if (arg instanceof OfflinePlayer) return (OfflinePlayer) arg;
            return Bukkit.getOfflinePlayer(arg.toString());
        }

        private java.util.UUID resolveUuid(Object arg) {
            OfflinePlayer p = resolveOfflinePlayer(arg);
            return p == null ? null : p.getUniqueId();
        }

        private Object buildEconomyResponse(Class<?> returnType, double newBalance, double amount, String typeName, String errorMessage) {
            try {
                Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
                Class<?> typeEnum = Class.forName("net.milkbowl.vault.economy.EconomyResponse$ResponseType");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object type = Enum.valueOf((Class<Enum>) typeEnum, typeName);
                return responseClass
                        .getConstructor(double.class, double.class, typeEnum, String.class)
                        .newInstance(amount, newBalance, type, errorMessage);
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Vault response construction failed: " + throwable.getMessage());
                return null;
            }
        }
    }
}
