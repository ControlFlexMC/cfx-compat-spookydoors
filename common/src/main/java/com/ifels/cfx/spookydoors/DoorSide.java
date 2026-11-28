package com.ifels.cfx.spookydoors;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Sign the synthetic cursor displacement has to carry so that "push" always moves the door panel
 * away from the player.
 *
 * <p>Spooky Doors does not take a push/pull intent; it takes a horizontal cursor delta and folds
 * two flips into it before touching {@code percentOpen}
 * ({@code SpookyDoorsClient#onMoveMouse}, read from its bytecode):</p>
 *
 * <pre>
 *   d = xpos - lastMouseX;
 *   if (player is on the side the door faces) d = -d;   // door.facing + player position
 *   if (hinge == LEFT)                       d = -d;   // door.hinge
 *   percentOpen += d * 0.005;
 * </pre>
 *
 * <p>Only the <b>hinge</b> flip is compensated here, and the asymmetry is the whole point:</p>
 * <ul>
 *   <li>The hinge flip makes two otherwise identical doors react oppositely to the same delta, so
 *       it has to be pre-applied — otherwise push opens right-hinged doors and closes left-hinged
 *       ones.</li>
 *   <li>The facing flip is left to the door mod on purpose. It already makes the same delta do
 *       opposite things depending on which side of the door the player stands on, which is exactly
 *       what pushing means physically: from behind the door, pushing the panel away from you
 *       closes it. Pre-applying that flip too cancels the mod's own compensation and push stops
 *       depending on the side at all.</li>
 * </ul>
 */
final class DoorSide {

    private DoorSide() {
    }

    /** {@code +1} for a right hinge, {@code -1} for a left one; {@code +1} when state is not a door. */
    static int pushSign(BlockState state) {
        if (state == null || !state.hasProperty(DoorBlock.HINGE)) {
            return 1;
        }
        return pushSign(state.getValue(DoorBlock.HINGE));
    }

    static int pushSign(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? -1 : 1;
    }
}
