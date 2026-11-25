package com.ifels.cfx.spookydoors;

import com.ifels.cfx.spookydoors.mixin.MouseHandlerAccessor;
import com.ifels.controlflex.api.AnalogValues;
import com.ifels.controlflex.api.ButtonName;
import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionBindingInfo;
import com.ifels.controlflex.api.IActionBindingProvider;
import com.ifels.controlflex.api.IControllerState;
import com.ifels.controlflex.api.IInputProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Turns the grab hold into Spooky Doors' own mouse gesture.
 *
 * <p>Spooky Doors has no API for "operate this door": it listens on {@code MouseHandler.onPress}
 * and {@code MouseHandler.onMove}. So this bridge does exactly what a player's mouse would do —
 * press use, accumulate horizontal displacement while held, release — and lets the door mod own
 * the state, sound and server sync. Nothing here touches the door itself.</p>
 *
 * <p>The displacement is horizontal only, because that is the axis Spooky Doors integrates. Its
 * own code flips the sign based on the player's side and the door's hinge, so pushing the stick
 * forward always means "away from me", from either side of the door.</p>
 *
 * <p>What a direction does depends on how it is bound, and the two can be mixed: a stick or
 * trigger integrates its travel as speed, while a face button, bumper or keyboard key — which
 * has no travel to read — moves the door one fixed step per press. See {@link DragRate}.</p>
 */
public final class DoorDrag {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    /** Re-trying immediately after a declined grab would spam a use every tick. */
    private static final int DECLINED_GRAB_COOLDOWN_TICKS = 10;

    /** Inputs whose deflection is a real 0..1 magnitude rather than a pressed state. */
    private static final Set<String> ANALOG_BUTTONS = Set.of(
            ButtonName.LEFT_STICK_UP, ButtonName.LEFT_STICK_DOWN,
            ButtonName.LEFT_STICK_LEFT, ButtonName.LEFT_STICK_RIGHT,
            ButtonName.RIGHT_STICK_UP, ButtonName.RIGHT_STICK_DOWN,
            ButtonName.RIGHT_STICK_LEFT, ButtonName.RIGHT_STICK_RIGHT,
            ButtonName.LEFT_TRIGGER, ButtonName.RIGHT_TRIGGER);

    private static boolean enabled;
    private static boolean dragging;
    private static int useButton = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    private static double virtualX;
    private static double virtualY;
    private static float pushBaseline;
    private static float pullBaseline;
    private static boolean pushWasDown;
    private static boolean pullWasDown;
    private static int cooldown;

    private DoorDrag() {
    }

    /** Switched off when ControlFlex ships an API too old to read analog magnitudes. */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            release(Minecraft.getInstance());
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            release(mc);
            cooldown = 0;
            return;
        }

        boolean held = SpookyActions.isActive(SpookyAction.GRAB);
        if (dragging) {
            if (!held) {
                release(mc);
            } else {
                step(mc);
            }
        } else if (held && cooldown == 0 && mc.screen == null) {
            grab(mc);
        }
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /**
     * Presses use once and checks whether Spooky Doors swallowed it.
     *
     * <p>A declined press means the door would not accept the drag; it is released in the same
     * tick so vanilla use does not start, and the gesture is retried after a short cooldown.</p>
     */
    private static void grab(Minecraft mc) {
        if (DoorTargeting.target(mc) == null) {
            return;
        }
        InputConstants.Key useKey = mc.options.keyUse.getKey();
        if (useKey.getType() != InputConstants.Type.MOUSE) {
            // Use is bound to a keyboard key: there is no mouse button to simulate.
            return;
        }
        useButton = useKey.getValue();

        MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
        virtualX = mouse.cfxsd$getXpos();
        virtualY = mouse.cfxsd$getYpos();

        long window = mc.getWindow().getWindow();
        mouse.cfxsd$invokeOnPress(window, useButton, GLFW.GLFW_PRESS, 0);
        if (mc.options.keyUse.isDown()) {
            mouse.cfxsd$invokeOnPress(window, useButton, GLFW.GLFW_RELEASE, 0);
            cooldown = DECLINED_GRAB_COOLDOWN_TICKS;
            return;
        }

        dragging = true;
        pushBaseline = baselineOf(SpookyAction.PUSH);
        pullBaseline = baselineOf(SpookyAction.PULL);
        // A button already held when the door was grabbed has already been counted; only a new
        // press on top of the grab may nudge a digital binding.
        pushWasDown = SpookyActions.isActive(SpookyAction.PUSH);
        pullWasDown = SpookyActions.isActive(SpookyAction.PULL);
        LOGGER.debug("[{}] grabbed a door, baselines push={} pull={}",
                SpookyAction.MOD_ID, pushBaseline, pullBaseline);
    }

    /**
     * One tick of dragging: analog deflection integrates, digital presses nudge once.
     *
     * <p>Each direction is resolved on its own so one analog stick and one digital button can be
     * mixed — for example a stick for pushing and a face button for pulling back.</p>
     */
    private static void step(Minecraft mc) {
        IControllerState state = controllerState();
        String pushButton = boundButton(SpookyAction.PUSH);
        String pullButton = boundButton(SpookyAction.PULL);
        boolean pushAnalog = isAnalog(pushButton);
        boolean pullAnalog = isAnalog(pullButton);

        boolean pushDown = SpookyActions.isActive(SpookyAction.PUSH);
        boolean pullDown = SpookyActions.isActive(SpookyAction.PULL);

        double pushUnits = DragRate.oneTick(pushAnalog,
                DragRate.delta(magnitudeOf(state, pushButton), pushBaseline),
                pushDown, pushWasDown);
        double pullUnits = DragRate.oneTick(pullAnalog,
                DragRate.delta(magnitudeOf(state, pullButton), pullBaseline),
                pullDown, pullWasDown);

        pushWasDown = pushDown;
        pullWasDown = pullDown;

        double displacement = DragRate.signed(pushUnits, pullUnits);
        if (displacement == 0.0) {
            return;
        }
        virtualX += displacement;
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
        mouse.cfxsd$invokeOnMove(mc.getWindow().getWindow(), virtualX, virtualY);
    }

    private static void release(Minecraft mc) {
        if (!dragging) {
            return;
        }
        dragging = false;
        pushWasDown = false;
        pullWasDown = false;
        if (mc == null) {
            return;
        }
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
        mouse.cfxsd$invokeOnPress(mc.getWindow().getWindow(), useButton,
                GLFW.GLFW_RELEASE, 0);
    }

    /**
     * Only a stick direction or a trigger has travel worth subtracting. A face button or a
     * keyboard key is binary, so its baseline stays zero and it works by discrete presses.
     */
    private static boolean isAnalog(String button) {
        return button != null && ANALOG_BUTTONS.contains(button);
    }

    private static float baselineOf(SpookyAction action) {
        String button = boundButton(action);
        return isAnalog(button) ? magnitudeOf(controllerState(), button) : 0.0f;
    }

    /** 0..1 deflection of one bound button, or zero when it cannot report travel. */
    private static float magnitudeOf(IControllerState state, String button) {
        return state == null || button == null ? 0.0f : AnalogValues.magnitude(state, button);
    }

    /** The action's own button, ignoring a shift modifier listed before it. */
    private static String boundButton(SpookyAction action) {
        IActionBindingProvider bindings = ControlFlexApi.getActionBindingProvider();
        if (bindings == null) {
            return null;
        }
        IActionBindingInfo info = bindings.getActiveBinding(action.actionId());
        if (info == null || !info.isBound()) {
            return null;
        }
        List<String> buttons = info.getButtons();
        return buttons.isEmpty() ? null : buttons.get(buttons.size() - 1);
    }

    private static IControllerState controllerState() {
        IInputProvider inputs = ControlFlexApi.getInputProvider();
        return inputs == null ? null : inputs.getControllerState();
    }
}
