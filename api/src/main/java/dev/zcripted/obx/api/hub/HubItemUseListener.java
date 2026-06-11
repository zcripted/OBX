package dev.zcripted.obx.api.hub;

/**
 * Public surface of the hub item-use listener — the visibility/vanish reset hooks
 * other features (staff) call. Implemented by {@code feature.hub.listener.HubItemUseListenerImpl}.
 */
public interface HubItemUseListener {

    void clearAllVanishState();

    void resetVisibilityForAll();
}