package dev.zcripted.obx.api.hub;

import java.util.List;

/**
 * Public hub surface used across features (the staff admin menu reads/toggles hub
 * state). Implemented by {@code feature.hub.service.HubService}.
 */
public interface HubApi {

    boolean isEnabled();

    void setEnabled(boolean value);

    boolean toggleEnabled();

    void reload();

    List<String> getHubWorlds();

    boolean addHubWorld(String worldName);

    boolean removeHubWorld(String worldName);

    boolean isItemEnabled(String itemId);

    int launchpadCooldownSeconds();
}
