package dev.sergeantfuzzy.sfcore.hologram.api;

import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.packet.InteractDecoder;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when {@code InteractionDispatcher} resolves a click into a CText
 * action. Downstream plugins can cancel the event to suppress the
 * configured command, or modify state on the hologram before letting it
 * propagate.
 *
 * <p>Part of the public Phase 7 dev API (plan §J).
 */
public final class HologramInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Hologram hologram;
    private final Player player;
    private final InteractDecoder.Action action;
    private boolean cancelled;

    public HologramInteractEvent(Hologram hologram, Player player, InteractDecoder.Action action) {
        this.hologram = hologram;
        this.player = player;
        this.action = action;
    }

    public Hologram getHologram() {
        return hologram;
    }

    public Player getPlayer() {
        return player;
    }

    public InteractDecoder.Action getAction() {
        return action;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
