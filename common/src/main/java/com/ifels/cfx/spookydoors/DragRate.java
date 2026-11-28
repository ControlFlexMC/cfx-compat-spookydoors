package com.ifels.cfx.spookydoors;

/**
 * Converts the push/pull input into the mouse displacement Spooky Doors expects.
 *
 * <p>Spooky Doors integrates {@code openness += dx * 0.005} on every {@code MouseHandler.onMove},
 * so {@value #FULL_TRAVEL_UNITS} units of {@code dx} travel the whole 0..1 range.</p>
 *
 * <p>There are two input models, chosen by what the action is bound to:</p>
 * <ul>
 *   <li><b>Analog</b> (stick direction, trigger): the deflection above the grab-time baseline
 *       is a speed, integrated every tick. Pushing halfway opens the door at half pace and
 *       letting go leaves it where it stopped — the same "hold and drag" feel the mouse has.</li>
 *   <li><b>Digital</b> (face button, bumper, keyboard key): there is no travel to read, so each
 *       <i>press</i> becomes one fixed nudge of {@value #DIGITAL_STEP_FRACTION} of the door.
 *       Holding the button does not repeat, otherwise it would degenerate into the analog
 *       behaviour at a wildly different speed and overshoot on every tap.</li>
 * </ul>
 *
 * <p>For the analog side the baseline is the stick position captured when the door was grabbed.
 * Subtracting it means grabbing a door while already walking does not count that existing
 * deflection as a push, and clamping at zero means returning the stick to its old position stops
 * the door instead of pulling it back.</p>
 */
final class DragRate {

    /** {@code 1 / 0.005}: the displacement that takes the door from closed to fully open. */
    static final double FULL_TRAVEL_UNITS = 200.0;

    /** Full deflection is meant to slam the door in about a third of a second. */
    static final double MAX_UNITS_PER_SECOND = 600.0;

    /** Below 1 this would make small deflections faster; above it, fine control near centre. */
    static final double CURVE_EXPONENT = 1.8;

    /** One digital press moves the door an eighth of its travel: eight taps end to end. */
    static final double DIGITAL_STEP_FRACTION = 0.125;

    static final double DIGITAL_STEP_UNITS = FULL_TRAVEL_UNITS * DIGITAL_STEP_FRACTION;

    /** One tick of displacement at 20 ticks per second. */
    static final double TICK_SECONDS = 0.05;

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
     * <p>An analog binding integrates its deflection, so a light press creeps and a full press
     * moves a third of the door per second. A digital binding has no deflection to integrate,
     * so it produces its whole fixed step on the press edge and nothing afterwards.</p>
     *
     * @param analog  whether the bound button reports 0..1 travel
     * @param delta   deflection above the grab baseline; ignored for digital
     * @param down    whether the binding is pressed now; ignored for analog
     * @param wasDown whether it was pressed on the previous tick; ignored for analog
     */
    static double oneTick(boolean analog, float delta, boolean down, boolean wasDown) {
        return analog ? curve(delta) * TICK_SECONDS : digitalStep(down, wasDown);
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

    /**
     * Displacement produced by a digital binding on this tick: the full step on the press edge,
     * nothing while it stays down or after it is released.
     */
    static double digitalStep(boolean down, boolean wasDown) {
        return down && !wasDown ? DIGITAL_STEP_UNITS : 0.0;
    }
}
