package com.ifels.cfx.spookydoors;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Sign the synthetic cursor displacement has to carry so that "push" always swings the door away
 * from the player.
 *
 * <p>Spooky Doors does not take a push/pull intent; it takes a horizontal cursor delta and folds
 * two flips of its own into it before touching {@code percentOpen}
 * ({@code SpookyDoorsClient#onMoveMouse}):</p>
 *
 * <pre>
 *   d = xpos - lastMouseX;
 *   if (player is on the side the door faces) d = -d;   // door.facing
 *   if (hinge == LEFT)                       d = -d;   // door.hinge
 *   percentOpen += d * 0.005;
 * </pre>
 *
 * <p>A real mouse does not care — the player sees which way the panel moves and drags that way.
 * Our cursor is synthetic, so the bridge has to pre-multiply by the same two flips. Missing the
 * hinge one is what made push/pull swap on left-hinged doors while right-hinged doors behaved:
 * with a right hinge the door's flip is a no-op, so only those matched.</p>
 *
 * <p>The flips are computed from state the mod itself reads, so this stays correct from either
 * side of the door and for every hinge. When the state is not a door any more the sign falls back
 * to {@code +1}, which is the behaviour before the hinge fix.</p>
 */
final class DoorSide {

    private DoorSide() {
    }

    /** Sign for the given door state; {@code dxToDoor}/{@code dzToDoor} are player minus door. */
    static int pushSign(BlockState state, double dxToDoor, double dzToDoor) {
        if (state == null
                || !state.hasProperty(DoorBlock.FACING)
                || !state.hasProperty(DoorBlock.HINGE)) {
            return 1;
        }
        return pushSign(state.getValue(DoorBlock.FACING), state.getValue(DoorBlock.HINGE),
                dxToDoor, dzToDoor);
    }

    /** The two flips from {@code onMoveMouse}, applied to our own displacement in advance. */
    static int pushSign(Direction facing, DoorHingeSide hinge,
                        double dxToDoor, double dzToDoor) {
        // "The player stands on the side the door faces" — same four comparisons the mod makes.
        boolean facingUs = switch (facing) {
            case NORTH -> dzToDoor < 0.0;
            case SOUTH -> dzToDoor > 0.0;
            case WEST -> dxToDoor < 0.0;
            case EAST -> dxToDoor > 0.0;
            default -> false;
        };
        boolean leftHinge = hinge == DoorHingeSide.LEFT;
        // Two independent negations: they cancel when both apply.
        return facingUs == leftHinge ? 1 : -1;
    }
}
