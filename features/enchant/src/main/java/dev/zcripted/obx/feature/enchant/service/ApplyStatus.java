package dev.zcripted.obx.feature.enchant.service;

/**
 * Outcome of attempting to apply an enchantment to an item. Maps directly onto
 * the admin-GUI / command error matrix (action-bar + chat feedback). The two
 * {@code *_OK} values are successes; everything else is a rejection.
 */
public enum ApplyStatus {

    APPLIED(true),
    UPGRADED(true),
    EMPTY_HAND(false),
    WRONG_TYPE(false),
    INVALID_LEVEL(false),
    ALREADY_APPLIED(false),
    CONFLICT(false),
    SLOT_CAP_REACHED(false),
    DISABLED(false);

    private final boolean success;

    ApplyStatus(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}