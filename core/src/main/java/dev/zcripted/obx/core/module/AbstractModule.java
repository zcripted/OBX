package dev.zcripted.obx.core.module;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Convenience base for {@link Module} implementations.
 *
 * <p>Subclasses implement {@link #onEnable(ObxPlugin)} and use the {@code listener(...)},
 * {@code command(...)}, {@code service(...)} and {@code onDisable(...)} helpers to
 * wire the feature up. This base records everything that was registered and tears
 * it down automatically in {@link #disable(ObxPlugin)} — unregistering listeners,
 * cancelling tracked work, restoring a "disabled" stub on commands, and dropping
 * services from the registry — so runtime toggling works without per-module
 * teardown code.
 */
public abstract class AbstractModule implements Module {

    protected ObxPlugin plugin;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<String> commandNames = new ArrayList<>();
    private final List<Class<?>> services = new ArrayList<>();
    private final List<Runnable> teardown = new ArrayList<>();

    /** Feature wiring goes here. Called by {@link #enable(ObxPlugin)} after {@code plugin} is set. */
    protected abstract void onEnable(ObxPlugin plugin);

    @Override
    public final void enable(ObxPlugin plugin) {
        this.plugin = plugin;
        onEnable(plugin);
    }

    @Override
    public void disable(ObxPlugin plugin) {
        // Run feature-specific teardown first (save state, stop tasks), newest first.
        for (int i = teardown.size() - 1; i >= 0; i--) {
            try {
                teardown.get(i).run();
            } catch (Throwable t) {
                plugin.getLogger().warning("[" + id() + "] teardown step failed: " + t.getMessage());
            }
        }
        teardown.clear();

        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();

        for (String name : commandNames) {
            PluginCommand command = plugin.getCommand(name);
            if (command != null) {
                command.setExecutor(DISABLED);
                command.setTabCompleter(null);
            }
        }
        commandNames.clear();

        for (Class<?> type : services) {
            plugin.getServiceRegistry().unregister(type);
        }
        services.clear();
    }

    /** Register a Bukkit event listener; auto-unregistered on disable. */
    protected void listener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    /**
     * Bind a command executor to a {@code plugin.yml} command and remember it so a
     * "disabled" stub is restored on teardown. Also wires the tab completer when the
     * executor implements one — mirroring the old {@code OBX#bind} behaviour.
     */
    protected void command(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("[" + id() + "] command '" + name + "' missing from plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter) executor);
        }
        commandNames.add(name);
    }

    /** Register a service in the shared registry and return it; auto-dropped on disable. */
    protected <T> T service(Class<T> type, T instance) {
        plugin.getServiceRegistry().register(type, instance);
        services.add(type);
        return instance;
    }

    /** Record an arbitrary teardown action (e.g. {@code task::cancel}, {@code svc::save}). */
    protected void onDisable(Runnable action) {
        teardown.add(action);
    }

    /** Read-only view of this module's registered listeners (for diagnostics/tests). */
    protected List<Listener> registeredListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /** Shared executor installed on a command whose owning module is disabled. */
    private static final CommandExecutor DISABLED = (sender, command, label, args) -> {
        sender.sendMessage("§8[§5OBX§8] §7This feature is currently disabled.");
        return true;
    };
}