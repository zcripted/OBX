package dev.zcripted.obx.api.economy;

import dev.zcripted.obx.OBX;
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

    public static boolean register(OBX plugin, EconomyService economy) {
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
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Registered as Vault economy provider.");
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to register Vault economy provider: " + throwable.getMessage());
            return false;
        }
    }

    private static final class EconomyHandler implements InvocationHandler {
        private final OBX plugin;
        private final EconomyService economy;

        EconomyHandler(OBX plugin, EconomyService economy) {
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
                    boolean ok = p != null && economy.withdraw(p.getUniqueId(), p.getName(), amount);
                    return buildEconomyResponse(method.getReturnType(), economy.getBalance(p == null ? null : p.getUniqueId()), amount, ok ? "SUCCESS" : "FAILURE", ok ? null : "Insufficient funds");
                }
                case "depositPlayer": {
                    OfflinePlayer p = resolveOfflinePlayer(args[0]);
                    double amount = ((Number) args[args.length - 1]).doubleValue();
                    if (p != null) economy.deposit(p.getUniqueId(), p.getName(), amount);
                    return buildEconomyResponse(method.getReturnType(), economy.getBalance(p == null ? null : p.getUniqueId()), amount, "SUCCESS", null);
                }
                case "createPlayerAccount": return true;
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
