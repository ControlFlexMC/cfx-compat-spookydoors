package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.BindingLayer;
import com.ifels.controlflex.api.ButtonName;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexAction;
import com.ifels.controlflex.api.TriggerMode;

import java.util.EnumMap;
import java.util.Map;

/**
 * Declares the three door actions and keeps their handles for per-tick polling.
 *
 * <p>Polling is used instead of {@code onActionTriggered} because a hold has three states this
 * bridge cares about — start, still held, ended — and the magnitude has to be read every tick
 * anyway.</p>
 *
 * <p>These are ControlFlex-native actions, not fake {@code KeyMapping}s: they never enter the
 * vanilla controls screen and never write global key state. {@code groupModId} points at
 * {@code spookydoors}, which is both the adaptation card they are listed on and the only context
 * in which they are visible.</p>
 */
public final class SpookyActions {

    private static final Map<SpookyAction, IControlFlexAction> HANDLES =
            new EnumMap<SpookyAction, IControlFlexAction>(SpookyAction.class);

    private SpookyActions() {
    }

    public static void register(IActionRegistrar registrar) {
        for (SpookyAction action : SpookyAction.values()) {
            IControlFlexAction handle = registrar.register(ActionRegistration
                    .builder(action.actionId())
                    .nameKey(action.nameKey())
                    .groupModId(SpookyAction.TARGET_MOD_ID)
                    // In-game only: the door is dragged while playing, never from a screen.
                    .layer(ActionLayer.IN_GAME)
                    .defaultBinding(holdDeclaration(action))
                    .build());
            HANDLES.put(action, handle);
        }
    }

    /** Never {@code null}; actions start unbound and inactive when registration was skipped. */
    public static boolean isActive(SpookyAction action) {
        IControlFlexAction handle = HANDLES.get(action);
        return handle != null && handle.isActive();
    }

    /**
     * Main-layer HOLD default, mirroring the aiming entry in {@code spookydoors.json}.
     *
     * <p>The compat config alone is enough on a core that latches a registered action by the mode
     * that actually fired and resolves state overrides when dispatching (control-flex
     * {@code 4058359b5}+). This declaration stays because released cores do neither: they read the
     * mode from this declaration and re-resolve the base layer before firing, so without it the
     * three actions never activate there.</p>
     *
     * <p>Sharing the left stick with movement is harmless: nothing here acts on the door until
     * the crosshair is on one, and push/pull are only integrated while a grab is held.</p>
     */
    static DefaultBinding holdDeclaration(SpookyAction action) {
        switch (action) {
            case GRAB:
                return DefaultBinding.of(ButtonName.LEFT_STICK_CLICK, TriggerMode.HOLD)
                        .inLayer(BindingLayer.MAIN);
            case PUSH:
                return DefaultBinding.of(ButtonName.LEFT_STICK_UP, TriggerMode.HOLD)
                        .inLayer(BindingLayer.MAIN);
            case PULL:
                return DefaultBinding.of(ButtonName.LEFT_STICK_DOWN, TriggerMode.HOLD)
                        .inLayer(BindingLayer.MAIN);
            default:
                return null;
        }
    }
}
