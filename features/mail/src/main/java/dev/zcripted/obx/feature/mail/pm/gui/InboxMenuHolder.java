package dev.zcripted.obx.feature.mail.pm.gui;

import dev.zcripted.obx.feature.mail.pm.InboxMessage;
import dev.zcripted.obx.core.gui.MenuHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for the inbox GUI. Maps raw inventory slots to the {@link InboxMessage} shown
 * there so the click listener can open the exact message clicked (no index drift).
 * Inventory plumbing is inherited from {@link MenuHolder}.
 */
public final class InboxMenuHolder extends MenuHolder {

    private final Map<Integer, InboxMessage> bySlot = new HashMap<Integer, InboxMessage>();

    void map(int slot, InboxMessage message) {
        bySlot.put(slot, message);
    }

    public InboxMessage forSlot(int slot) {
        return bySlot.get(slot);
    }
}
