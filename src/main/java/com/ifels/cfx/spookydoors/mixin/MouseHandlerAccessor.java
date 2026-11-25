package com.ifels.cfx.spookydoors.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches the mouse pipeline Spooky Doors listens on.
 *
 * <p>Spooky Doors mixes into {@code MouseHandler.onPress} and {@code MouseHandler.onMove}, so the
 * only way to drive a door without touching its code is to replay the same calls a real mouse
 * would produce. Both are private to the bridge: no other mod is expected to call them.</p>
 *
 * <p>{@code onMove} takes <b>absolute</b> cursor coordinates, not a delta — the callee subtracts
 * the previous position itself. The bridge therefore keeps its own virtual cursor and feeds it
 * forward, seeded from the real {@code xpos}/{@code ypos} at grab time so the first move does not
 * produce a jump.</p>
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("xpos")
    double cfxsd$getXpos();

    @Accessor("ypos")
    double cfxsd$getYpos();

    @Invoker("onPress")
    void cfxsd$invokeOnPress(long window, int button, int action, int modifiers);

    @Invoker("onMove")
    void cfxsd$invokeOnMove(long window, double x, double y);
}
