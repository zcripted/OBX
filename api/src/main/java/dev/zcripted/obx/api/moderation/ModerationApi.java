package dev.zcripted.obx.api.moderation;

/**
 * Public moderation surface used across features (mute checks for chat, status
 * reads for staff). Implemented by {@code feature.moderation.service.ModerationService}.
 */
public interface ModerationApi {

    boolean isMuted(String playerName);

    String getMuteReason(String playerName);

    /** UUID-keyed mute check — uses the join-loaded cache, avoiding a per-call name->UUID DB lookup. */
    boolean isMuted(java.util.UUID uuid);

    /** UUID-keyed mute reason — uses the join-loaded cache. */
    String getMuteReason(java.util.UUID uuid);

    void reload();
}
