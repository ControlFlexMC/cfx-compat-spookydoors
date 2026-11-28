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
        double push = DragRate.oneTick(true, 0.4f, false);
        double idle = DragRate.oneTick(true, 0.0f, false);
        assertTrue(DragRate.signed(push, idle) > 0.0);
        assertTrue(DragRate.signed(idle, push) < 0.0);
        assertEquals(0.0, DragRate.signed(idle, idle), EPS);
    }

    @Test
    void holdingBothDirectionsCancelsOut() {
        double both = DragRate.oneTick(true, 0.6f, false);
        assertEquals(0.0, DragRate.signed(both, both), EPS);
    }

    @Test
    void analogTickIsTheRateTimesOneTick() {
        assertEquals(DragRate.curve(0.3f) * DragRate.TICK_SECONDS,
                DragRate.oneTick(true, 0.3f, true), EPS);
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

    // ===== digital binding: held means full deflection =====

    @Test
    void aHeldDigitalButtonMovesTheDoorEveryTick() {
        // The whole point of a HOLD binding: the door keeps travelling until it is released,
        // instead of taking one nudge and stopping (which is what a PRESS binding would do).
        double oneTick = DragRate.oneTick(false, 0.0f, true);
        assertTrue(oneTick > 0.0, "a held button must advance on every tick");

        double tenTicks = 0.0;
        for (int i = 0; i < 10; i++) {
            tenTicks += DragRate.oneTick(false, 0.0f, true);
        }
        assertEquals(oneTick * 10.0, tenTicks, EPS);
    }

    @Test
    void aReleasedDigitalButtonStops() {
        assertEquals(0.0, DragRate.oneTick(false, 0.0f, false), EPS);
        // The reported magnitude is irrelevant for a digital binding — only "held" counts.
        assertEquals(0.0, DragRate.oneTick(false, 1.0f, false), EPS);
    }

    @Test
    void aHeldDigitalButtonPushesAsFastAsAFullyDeflectedStick() {
        assertEquals(DragRate.oneTick(true, 1.0f, false),
                DragRate.oneTick(false, 0.0f, true), EPS);
    }

    @Test
    void aSingleTickTapIsAVisibleButSmallNudge() {
        // Coarse enough to see the door move, fine enough to peek with a tap.
        double fraction = DragRate.DIGITAL_FRACTION_OF_TRAVEL;
        assertTrue(fraction >= 0.08 && fraction <= 0.2, "was " + fraction);
    }

    @Test
    void digitalTravelIsQuotedInDoorOpenness() {
        // Spooky Doors multiplies dx by 0.005, so the per-tick step must survive that conversion.
        assertEquals(DragRate.DIGITAL_FRACTION_OF_TRAVEL,
                DragRate.DIGITAL_UNITS_PER_TICK * 0.005, EPS);
    }

    @Test
    void aHeldDigitalButtonOpensTheDoorInAboutAThirdOfASecond() {
        double ticks = DragRate.FULL_TRAVEL_UNITS / DragRate.DIGITAL_UNITS_PER_TICK;
        double seconds = ticks * DragRate.TICK_SECONDS;
        assertTrue(seconds > 0.2 && seconds < 0.5, "was " + seconds);
    }
}
