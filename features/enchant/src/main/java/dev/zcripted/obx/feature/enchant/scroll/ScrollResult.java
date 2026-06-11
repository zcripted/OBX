package dev.zcripted.obx.feature.enchant.scroll;

/**
 * Outcome of attempting to use a scroll on an item. The listener that owns the
 * physical items uses this to decide whether to consume the scroll and/or
 * destroy the target.
 */
public enum ScrollResult {

    /** Validation failed (wrong type / conflict / cap / duplicate). Nothing consumed. */
    REJECTED(false, false),
    /** Player lacked the XP cost. Nothing consumed. */
    NO_XP(false, false),
    /** Enchant applied. Consume the scroll. */
    APPLIED(true, false),
    /** Existing level upgraded. Consume the scroll. */
    UPGRADED(true, false),
    /** Roll failed but the item survived (destroy disabled). Consume the scroll. */
    FAILED_KEPT(true, false),
    /** Roll failed; a Protection imbue saved the item. Consume the scroll. */
    FAILED_PROTECTED(true, false),
    /** Roll failed and the item is destroyed. Consume the scroll AND the target. */
    FAILED_DESTROYED(true, true),
    /** A utility action (extract / transmute / imbue) succeeded. Consume the scroll. */
    UTILITY_OK(true, false),
    /** A utility action could not be performed (e.g. no enchants to extract). Nothing consumed. */
    UTILITY_FAILED(false, false);

    private final boolean consumeScroll;
    private final boolean destroyTarget;

    ScrollResult(boolean consumeScroll, boolean destroyTarget) {
        this.consumeScroll = consumeScroll;
        this.destroyTarget = destroyTarget;
    }

    public boolean consumesScroll() {
        return consumeScroll;
    }

    public boolean destroysTarget() {
        return destroyTarget;
    }
}