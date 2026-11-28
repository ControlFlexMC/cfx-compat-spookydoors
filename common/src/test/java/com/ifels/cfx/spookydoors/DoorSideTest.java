package com.ifels.cfx.spookydoors;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sign that keeps "push" meaning "away from me".
 *
 * <p>Spooky Doors folds two flips into the cursor delta it receives: one when the player stands on
 * the side the door faces, one for a left hinge. Only the hinge flip was missing here, so
 * right-hinged doors looked correct (their flip is the identity unless the player is also on the
 * facing side) while left-hinged ones pushed the wrong way.</p>
 */
class DoorSideTest {

    private static final double FRONT = -1.0;
    private static final double BACK = 1.0;

    @Test
    void rightHingedDoorFromTheBackPushesPositive() {
        // The configuration that looked right before the fix: this is the anchor the report gives.
        assertEquals(1, DoorSide.pushSign(Direction.NORTH, DoorHingeSide.RIGHT, 0.0, BACK));
    }

    @Test
    void leftHingedDoorFromTheSameSpotFlips() {
        // The reported bug: same player position, same facing, only the hinge differs.
        assertEquals(-1, DoorSide.pushSign(Direction.NORTH, DoorHingeSide.LEFT, 0.0, BACK));
    }

    @Test
    void facingSideFlipsAsWell() {
        // Standing on the side the door faces inverts the pair again — this is the flip a real
        // mouse absorbs by simply being dragged the other way.
        assertEquals(-1, DoorSide.pushSign(Direction.NORTH, DoorHingeSide.RIGHT, 0.0, FRONT));
        assertEquals(1, DoorSide.pushSign(Direction.NORTH, DoorHingeSide.LEFT, 0.0, FRONT));
    }

    @Test
    void everyFacingUsesItsOwnAxis() {
        // NORTH/SOUTH compare Z around the door, EAST/WEST compare X — the same four branches
        // Spooky Doors takes, so the "facing" side is the side the door looks at.
        assertEquals(1, DoorSide.pushSign(Direction.SOUTH, DoorHingeSide.RIGHT, 0.0, FRONT));
        assertEquals(-1, DoorSide.pushSign(Direction.SOUTH, DoorHingeSide.RIGHT, 0.0, BACK));
        assertEquals(1, DoorSide.pushSign(Direction.WEST, DoorHingeSide.RIGHT, BACK, 0.0));
        assertEquals(-1, DoorSide.pushSign(Direction.WEST, DoorHingeSide.RIGHT, FRONT, 0.0));
        assertEquals(1, DoorSide.pushSign(Direction.EAST, DoorHingeSide.RIGHT, FRONT, 0.0));
        assertEquals(-1, DoorSide.pushSign(Direction.EAST, DoorHingeSide.RIGHT, BACK, 0.0));
    }

    @Test
    void theTwoFlipsAreIndependentNegations() {
        double[][] offsets = {{0.0, -2.0}, {0.0, 2.0}, {-2.0, 0.0}, {2.0, 0.0}};
        for (Direction facing : new Direction[]{
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            for (double[] offset : offsets) {
                int right = DoorSide.pushSign(facing, DoorHingeSide.RIGHT, offset[0], offset[1]);
                int left = DoorSide.pushSign(facing, DoorHingeSide.LEFT, offset[0], offset[1]);
                assertEquals(-right, left, facing + " @" + offset[0] + "," + offset[1]);
                assertEquals(1, Math.abs(right));
            }
        }
    }

    @Test
    void aNonDoorStateFallsBackToTheOldBehaviour() {
        assertEquals(1, DoorSide.pushSign(null, 0.0, 0.0));
    }
}
