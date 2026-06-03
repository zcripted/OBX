package dev.zcripted.obx.api.playerinfo;

import java.util.List;

/** Public join/leave + welcome-MOTD configuration API. Implemented by {@code feature.playerinfo.service.JoinLeaveServiceImpl}. */
public interface JoinLeaveService {

    void reload();

    boolean isJoinLeaveEnabled();

    void setJoinLeaveEnabled(boolean enabled);

    boolean suppressVanillaJoinMessage();

    boolean isFirstJoinMessageEnabled();

    String getJoinMessage();

    String getLeaveMessage();

    String getFirstJoinMessage();

    boolean isJoinMotdEnabled();

    void setJoinMotdEnabled(boolean enabled);

    boolean isFirstJoinMotdEnabled();

    List<String> getJoinMotdLines();

    List<String> getFirstJoinMotdLines();
}
