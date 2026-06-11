package dev.zcripted.obx.core.module;

import dev.zcripted.obx.core.ObxPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers every feature {@link Module} and drives its lifecycle.
 *
 * <p>Modules are enabled in dependency order (a topological sort over
 * {@link Module#dependsOn()}) and disabled in the reverse order. Per-module
 * on/off state is read from {@code config.yml} under {@code modules.<id>} and can
 * be flipped at runtime via {@link #setEnabled(String, boolean)} — which the
 * staff "Toggle Modules" menu calls — with the new state written back to config so
 * it survives a restart.
 */
public final class ModuleManager {

    private final ObxPlugin plugin;
    /** Insertion-ordered: registration order, used as the tie-breaker within a dependency level. */
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> active = new ConcurrentHashMap<>();
    private List<Module> enableOrder;

    public ModuleManager(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /** Register a module. Call for every feature before {@link #enableAll()}. */
    public ModuleManager register(Module module) {
        if (modules.containsKey(module.id())) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
        modules.put(module.id(), module);
        return this;
    }

    /** Enables every module whose config toggle resolves to {@code true}, in dependency order. */
    public void enableAll() {
        enableOrder = topologicalOrder();
        for (Module module : enableOrder) {
            boolean wanted = plugin.getConfig().getBoolean("modules." + module.id(), module.enabledByDefault());
            active.put(module.id(), false);
            if (wanted) {
                enable(module);
            }
        }
    }

    /** Disables every active module in reverse dependency order. */
    public void disableAll() {
        if (enableOrder == null) {
            return;
        }
        List<Module> reverse = new ArrayList<>(enableOrder);
        Collections.reverse(reverse);
        for (Module module : reverse) {
            if (Boolean.TRUE.equals(active.get(module.id()))) {
                disable(module);
            }
        }
    }

    /** Reloads config for every active module (called by {@code /obx reload}). */
    public void reloadAll() {
        if (enableOrder == null) {
            return;
        }
        for (Module module : enableOrder) {
            if (Boolean.TRUE.equals(active.get(module.id()))) {
                try {
                    module.reload(plugin);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[" + module.id() + "] reload failed: " + t.getMessage());
                }
            }
        }
    }

    /**
     * Flips a module on or off at runtime and persists the choice to config.
     * Returns {@code true} if the state changed. Enabling respects dependency order
     * (dependencies are enabled first); disabling cascades to dependents.
     */
    public boolean setEnabled(String id, boolean enabled) {
        Module module = modules.get(id);
        if (module == null || Boolean.valueOf(enabled).equals(active.get(id))) {
            return false;
        }
        if (enabled) {
            for (String dep : module.dependsOn()) {
                setEnabled(dep, true);
            }
            enable(module);
        } else {
            // Disable anything that depends on this module first.
            for (Module other : modules.values()) {
                if (Boolean.TRUE.equals(active.get(other.id()))) {
                    for (String dep : other.dependsOn()) {
                        if (dep.equals(id)) {
                            setEnabled(other.id(), false);
                        }
                    }
                }
            }
            disable(module);
        }
        plugin.getConfig().set("modules." + id, enabled);
        plugin.saveConfig();
        return true;
    }

    public boolean isEnabled(String id) {
        return Boolean.TRUE.equals(active.get(id));
    }

    public boolean isRegistered(String id) {
        return modules.containsKey(id);
    }

    /** Snapshot of every module's id → live enabled state, for the toggle UI. */
    public Map<String, Boolean> states() {
        Map<String, Boolean> snapshot = new LinkedHashMap<>();
        for (Module module : modules.values()) {
            snapshot.put(module.id(), isEnabled(module.id()));
        }
        return snapshot;
    }

    public Module get(String id) {
        return modules.get(id);
    }

    public List<Module> modules() {
        return new ArrayList<>(modules.values());
    }

    // ── internals ──────────────────────────────────────────────────────────

    private void enable(Module module) {
        try {
            module.enable(plugin);
            active.put(module.id(), true);
        } catch (Throwable t) {
            active.put(module.id(), false);
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[" + module.id() + "] failed to enable: " + t.getMessage(), t);
            // Roll back any partial registrations (listeners/commands/services/tasks) made before the
            // module threw, so a failed enable doesn't leak handlers or double-register on a later retry.
            try {
                module.disable(plugin);
            } catch (Throwable cleanup) {
                plugin.getLogger().warning("[" + module.id() + "] cleanup after a failed enable also failed: "
                        + cleanup.getMessage());
            }
        }
    }

    private void disable(Module module) {
        try {
            module.disable(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("[" + module.id() + "] error during disable: " + t.getMessage());
        } finally {
            active.put(module.id(), false);
        }
    }

    /**
     * Kahn-style topological sort over {@link Module#dependsOn()}. Registration
     * order breaks ties so the result is deterministic. Unknown dependencies are
     * ignored (with a warning); a dependency cycle falls back to registration order.
     */
    private List<Module> topologicalOrder() {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> dependents = new LinkedHashMap<>();
        for (String id : modules.keySet()) {
            indegree.put(id, 0);
            dependents.put(id, new ArrayList<>());
        }
        for (Module module : modules.values()) {
            for (String dep : module.dependsOn()) {
                if (!modules.containsKey(dep)) {
                    plugin.getLogger().warning("[" + module.id() + "] declares unknown dependency '" + dep + "'");
                    continue;
                }
                indegree.put(module.id(), indegree.get(module.id()) + 1);
                dependents.get(dep).add(module.id());
            }
        }
        List<Module> order = new ArrayList<>();
        List<String> ready = new ArrayList<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        while (!ready.isEmpty()) {
            String id = ready.remove(0);
            order.add(modules.get(id));
            for (String dependent : dependents.get(id)) {
                int d = indegree.get(dependent) - 1;
                indegree.put(dependent, d);
                if (d == 0) {
                    ready.add(dependent);
                }
            }
        }
        if (order.size() != modules.size()) {
            plugin.getLogger().warning("Module dependency cycle detected; falling back to registration order.");
            return new ArrayList<>(modules.values());
        }
        return order;
    }
}