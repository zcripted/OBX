package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.gui.MenuHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marker holder used to identify the hub server-selector inventory in click
 * listeners (mirrors the existing {@link MainMenuHolder} / {@link WarpMenuHolder}
 * idiom).
 *
 * <p>Holds a mapping of inventory slot → server id so the click handler
 * can dispatch without re-parsing item NBT.
 */
public final class ServerSelectorHolder extends MenuHolder {

    private final Map<Integer, String> serverBySlot = new HashMap<>();
    private int closeSlot = -1;

    public void bindSlot(int slot, String serverId) {
        if (serverId == null) {
            return;
        }
        serverBySlot.put(slot, serverId);
    }

    public String serverFor(int slot) {
        return serverBySlot.get(slot);
    }

    public void setCloseSlot(int slot) {
        this.closeSlot = slot;
    }

    public boolean isCloseSlot(int slot) {
        return closeSlot >= 0 && slot == closeSlot;
    }
}
