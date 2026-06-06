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

    /**
     * Welcome-MOTD lines localized to the given language code (e.g. {@code "en"}, {@code "de"},
     * {@code "es"}), so a player who set {@code /language} sees the MOTD in their own language.
     * Falls back to the default (EN) catalog for an unknown/blank code. Default impl returns the
     * EN lines for backwards compatibility.
     */
    default List<String> getJoinMotdLines(String languageCode) {
        return getJoinMotdLines();
    }

    /** First-join welcome-MOTD lines localized to {@code languageCode}; see {@link #getJoinMotdLines(String)}. */
    default List<String> getFirstJoinMotdLines(String languageCode) {
        return getFirstJoinMotdLines();
    }
}
