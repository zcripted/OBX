package dev.zcripted.obx.feature.enchant.service;

import dev.zcripted.obx.feature.enchant.model.CustomEnchant;

/**
 * Immutable description of an {@link EnchantService#apply} attempt, carrying
 * everything the feedback layer needs to render the right action-bar + chat
 * message and sound for both success and every rejection case.
 */
public final class ApplyResult {

    private final ApplyStatus status;
    private final CustomEnchant enchant;
    private final int level;
    private final int previousLevel;
    private final CustomEnchant conflicting;
    private final int cap;

    private ApplyResult(ApplyStatus status, CustomEnchant enchant, int level, int previousLevel,
                        CustomEnchant conflicting, int cap) {
        this.status = status;
        this.enchant = enchant;
        this.level = level;
        this.previousLevel = previousLevel;
        this.conflicting = conflicting;
        this.cap = cap;
    }

    public static ApplyResult of(ApplyStatus status, CustomEnchant enchant, int level) {
        return new ApplyResult(status, enchant, level, 0, null, 0);
    }

    public static ApplyResult upgraded(CustomEnchant enchant, int level, int previousLevel) {
        return new ApplyResult(ApplyStatus.UPGRADED, enchant, level, previousLevel, null, 0);
    }

    public static ApplyResult conflict(CustomEnchant enchant, int level, CustomEnchant conflicting) {
        return new ApplyResult(ApplyStatus.CONFLICT, enchant, level, 0, conflicting, 0);
    }

    public static ApplyResult cap(CustomEnchant enchant, int level, int cap) {
        return new ApplyResult(ApplyStatus.SLOT_CAP_REACHED, enchant, level, 0, null, cap);
    }

    public ApplyStatus getStatus() {
        return status;
    }

    public CustomEnchant getEnchant() {
        return enchant;
    }

    public int getLevel() {
        return level;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public CustomEnchant getConflicting() {
        return conflicting;
    }

    public int getCap() {
        return cap;
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }
}
