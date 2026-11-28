package com.ifels.cfx.spookydoors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragRateTest {

    private static final float EPS = 1.0e-4f;

    @Test
    void baselineIsSubtracted() {
        assertEquals(0.3f, DragRate.delta(0.8f, 0.5f), EPS);
    }

    @Test
    void returningTowardsTheBaselineDoesNotReverseTheDoor() {
        assertEquals(0.0f, DragRate.delta(0.4f, 0.5f), EPS);
        assertEquals(0.0f, DragRate.delta(0.0f, 0.5f), EPS);
    }

    @Test
    void centredBaselineEqualsPlainDeflection() {
        assertEquals(0.42f, DragRate.delta(0.42f, 0.0f), EPS);
    }

    @Test
    void pushAndPullHaveOppositeSigns() {
        double push = DragRate.oneTick(true, 0.4f, false, false);
        double idle = DragRate.oneTick(true, 0.0f, false, false);
        assertTrue(DragRate.signed(push, idle) > 0.0);
        assertTrue(DragRate.signed(idle, push) < 0.0);
        assertEquals(0.0, DragRate.signed(idle, idle), EPS);
    }

    @Test
    void holdingBothDirectionsCancelsOut() {
        double both = DragRate.oneTick(true, 0.6f, false, false);
        assertEquals(0.0, DragRate.signed(both, both), EPS);
    }

    @Test
    void analogTickIsTheRateTimesOneTick() {
        assertEquals(DragRate.curve(0.3f) * DragRate.TICK_SECONDS,
                DragRate.oneTick(true, 0.3f, true, false), EPS);
    }

    @Test
    void fullDeflectionReachesTheConfiguredSpeed() {
        assertEquals(DragRate.MAX_UNITS_PER_SECOND, DragRate.curve(1.0f), EPS);
        assertTrue(DragRate.curve(1.0f) > DragRate.curve(0.5f));
    }

    @Test
    void lowEndIsFinerThanLinear() {
        // 20% deflection must not feel like 20% of full speed, or peeking is impossible.
        assertTrue(DragRate.curve(0.2f) < 0.2 * DragRate.MAX_UNITS_PER_SECOND);
    }

    @Test
    void valuesAboveOneAreClamped() {
        assertEquals(DragRate.MAX_UNITS_PER_SECOND, DragRate.curve(1.7f), EPS);
        assertEquals(1.0f, DragRate.delta(1.9f, 0.0f), EPS);
    }

    @Test
    void fullTravelTakesAboutAThirdOfASecond() {
        double seconds = DragRate.FULL_TRAVEL_UNITS / DragRate.MAX_UNITS_PER_SECOND;
        assertTrue(seconds > 0.2 && seconds < 0.5, "was " + seconds);
    }

    // ===== digital fallback: one press, one fixed nudge =====

    @Test
    void digitalPressMovesTheDoorByAFixedAmount() {
        assertEquals(DragRate.DIGITAL_STEP_UNITS, DragRate.digitalStep(true, false), EPS);
    }

    @Test
    void holdingADigitalButtonDoesNotKeepPushing() {
        assertEquals(0.0, DragRate.digitalStep(true, true), EPS);
        assertEquals(0.0, DragRate.digitalStep(false, true), EPS);
        assertEquals(0.0, DragRate.digitalStep(false, false), EPS);
    }

    @Test
    void digitalStepIsATwelfthToAnEighthOfTheTravel() {
        // Coarse enough to see the door move, fine enough to peek in one or two taps.
        double fraction = DragRate.DIGITAL_STEP_UNITS / DragRate.FULL_TRAVEL_UNITS;
        assertTrue(fraction >= 1.0 / 12.0 && fraction <= 1.0 / 8.0, "was " + fraction);
    }

    @Test
    void digitalStepIsQuotedInDoorOpenness() {
        // Spooky Doors multiplies dx by 0.005, so the step must survive that conversion.
        assertEquals(DragRate.DIGITAL_STEP_FRACTION, DragRate.DIGITAL_STEP_UNITS * 0.005, EPS);
    }

    @Test
    void digitalBindingIgnoresTheFullDeflectionItReports() {
        // A pressed face button reads as magnitude 1.0; integrating that as analog would slam the
        // door at full speed on every tap, which is exactly what the fixed step exists to avoid.
        assertEquals(DragRate.DIGITAL_STEP_UNITS, DragRate.oneTick(false, 1.0f, true, false), EPS);
    }
}
