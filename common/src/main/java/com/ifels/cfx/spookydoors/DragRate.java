package com.ifels.cfx.spookydoors;

/**
 * Converts the push/pull input into the mouse displacement Spooky Doors expects.
 *
 * <p>Spooky Doors integrates {@code openness += dx * 0.005} on every {@code MouseHandler.onMove},
 * so {@value #FULL_TRAVEL_UNITS} units of {@code dx} travel the whole 0..1 range.</p>
 *
 * <p>How far one tick moves the door depends on what the action is bound to and how it triggers:</p>
 * <ul>
 *   <li><b>Analog</b> (stick direction, trigger): the deflection above the grab-time baseline is a
 *       speed. Pushing halfway opens the door at half pace and letting go leaves it where it
 *       stopped — the same "hold and drag" feel the mouse has.</li>
 *   <li><b>Digital, hold-style</b> (face button, bumper, key bound as HOLD): there is no travel to
 *       read, so a held button advances {@value #HOLD_STEP_FRACTION} of the travel every tick —
 *       a full swing takes about a second, slow enough to stop where you want.</li>
 *   <li><b>Digital, one-shot</b> (PRESS, TAP, …): the action lasts a single tick, and that tick is
 *       worth {@value #TAP_STEP_FRACTION} of the travel. One press is one visible nudge, and the
 *       size stays put no matter how the hold rate is tuned.</li>
 * </ul>
 *
 * <p>For the analog side the baseline is the stick position captured when the door was grabbed.
 * Subtracting it means grabbing a door while already walking does not count that existing
 * deflection as a push, and clamping at zero means returning the stick to its old position stops
 * the door instead of pulling it back. A digital button needs no baseline: holding it is the
 * intent to push, whether or not it was already down when the grab happened.</p>
 */
final class DragRate {

    /** {@code 1 / 0.005}: the displacement that takes the door from closed to fully open. */
    static final double FULL_TRAVEL_UNITS = 200.0;

    /** Full analog deflection is meant to slam the door in about a third of a second. */
    static final double MAX_UNITS_PER_SECOND = 600.0;

    /** Below 1 this would make small deflections faster; above it, fine control near centre. */
    static final double CURVE_EXPONENT = 1.8;

    /** One tick of displacement at 20 ticks per second. */
    static final double TICK_SECONDS = 0.05;

    /** Per-tick stride of a held digital button: 5% of the travel, so a second end to end. */
    static final double HOLD_STEP_FRACTION = 0.05;

    static final double HOLD_STEP_UNITS = FULL_TRAVEL_UNITS * HOLD_STEP_FRACTION;

    /** What a one-shot binding moves on its single tick; deliberately bigger than a hold tick. */
    static final double TAP_STEP_FRACTION = 0.15;

    static final double TAP_STEP_UNITS = FULL_TRAVEL_UNITS * TAP_STEP_FRACTION;

    private DragRate() {
    }

    /** Deflection beyond the grab-time baseline, never negative. */
    static float delta(float magnitude, float baseline) {
        float d = magnitude - Math.max(0.0f, baseline);
        return d > 0.0f ? Math.min(d, 1.0f) : 0.0f;
    }

    /**
     * Unsigned displacement one direction contributes on this tick.
     *
     * @param analog   whether the bound button reports 0..1 travel
     * @param delta    deflection above the grab baseline; ignored for digital
     * @param down     whether the binding is pressed now; ignored for analog
     * @param holdLike whether the binding stays active while held; ignored for analog
     */
    static double oneTick(boolean analog, float delta, boolean down, boolean holdLike) {
        if (analog) {
            return curve(delta) * TICK_SECONDS;
        }
        if (!down) {
            return 0.0;
        }
        return holdLike ? HOLD_STEP_UNITS : TAP_STEP_UNITS;
    }

    /** Signed displacement: pushing is positive, pulling is negative, both cancel out. */
    static double signed(double pushUnits, double pullUnits) {
        return pushUnits - pullUnits;
    }

    /** {@code 0..600} units per second; the low end is deliberately very slow. */
    static double curve(float delta) {
        if (delta <= 0.0f) {
            return 0.0;
        }
        double clamped = Math.min(delta, 1.0);
        return MAX_UNITS_PER_SECOND * Math.pow(clamped, CURVE_EXPONENT);
    }
}
