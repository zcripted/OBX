package dev.zcripted.obx.feature.economy;

import dev.zcripted.obx.api.economy.EconomyService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the money-safety guarantees of {@link EconomyService#sanitize(double)}:
 * NaN/Infinity/negative collapse to 0, values are rounded to 2 decimals, and the
 * hard cap is enforced — so no crafted amount can poison a stored balance.
 */
class EconomySanitizeTest {

    @Test
    @DisplayName("NaN and -Infinity collapse to 0 (+Infinity is capped, see capEnforced)")
    void nonFiniteIsZero() {
        assertEquals(0.0, EconomyService.sanitize(Double.NaN));
        assertEquals(0.0, EconomyService.sanitize(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("negatives and zero collapse to 0")
    void negativesAreZero() {
        assertEquals(0.0, EconomyService.sanitize(-1.0));
        assertEquals(0.0, EconomyService.sanitize(-0.01));
        assertEquals(0.0, EconomyService.sanitize(0.0));
    }

    @Test
    @DisplayName("values round to 2 decimals")
    void roundsToCents() {
        assertEquals(10.12, EconomyService.sanitize(10.124));
        assertEquals(10.13, EconomyService.sanitize(10.125));
        assertEquals(0.01, EconomyService.sanitize(0.005));
    }

    @Test
    @DisplayName("amounts above the cap (incl. +Infinity) clamp to MAX_BALANCE")
    void capEnforced() {
        assertEquals(EconomyService.MAX_BALANCE, EconomyService.sanitize(EconomyService.MAX_BALANCE + 1_000.0));
        assertEquals(EconomyService.MAX_BALANCE, EconomyService.sanitize(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("ordinary amounts pass through")
    void ordinaryPassThrough() {
        assertEquals(100.0, EconomyService.sanitize(100.0));
        assertEquals(1250.5, EconomyService.sanitize(1250.50));
    }
}
