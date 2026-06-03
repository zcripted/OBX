package dev.zcripted.obx.api.moderation;

/**
 * Public moderation surface used across features (mute checks for chat, status
 * reads for staff). Implemented by {@code feature.moderation.service.ModerationService}.
 */
public interface ModerationApi {

    boolean isMuted(String playerName);

    String getMuteReason(String playerName);

    void reload();
}
