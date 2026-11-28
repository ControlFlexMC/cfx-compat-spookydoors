package com.ifels.cfx.spookydoors;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Only the hinge half of Spooky Doors' sign handling is compensated.
 *
 * <p>Its other flip — "player stands on the side the door faces" — is what makes push and pull
 * swap when you walk around the door, and that is the wanted behaviour: from behind, pushing the
 * panel moves it into the doorway. Compensating both cancels it, which is how push ended up
 * opening the door from either side.</p>
 */
class DoorSideTest {

    @Test
    void rightHingedDoorPushesPositive() {
        // The configuration both reports agree on: push opens.
        assertEquals(1, DoorSide.pushSign(DoorHingeSide.RIGHT));
    }

    @Test
    void leftHingedDoorFlips() {
        // Without this, a left-hinged door opened on push and closed on pull (first report).
        assertEquals(-1, DoorSide.pushSign(DoorHingeSide.LEFT));
    }

    @Test
    void theTwoHingesAreMirrorImages() {
        // Deliberate: the sign depends on the hinge only. The door mod's other flip (player side)
        // is left intact, so push means "panel away from me" — open from the front, close from
        // behind. Adding a player-side flip here cancelled that and push opened from both sides
        // (the second report).
        assertEquals(-DoorSide.pushSign(DoorHingeSide.RIGHT),
                DoorSide.pushSign(DoorHingeSide.LEFT));
        assertEquals(1, Math.abs(DoorSide.pushSign(DoorHingeSide.LEFT)));
    }

    @Test
    void aNonDoorStateFallsBackToPositive() {
        assertEquals(1, DoorSide.pushSign((BlockState) null));
    }
}
