package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
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
                    .layer(ActionLayer.IN_GAME)
                    .defaultBinding(defaultBinding(action))
                    .build());
            HANDLES.put(action, handle);
        }
    }

    /** Never {@code null}; actions start unbound and inactive when registration was skipped. */
    public static boolean isActive(SpookyAction action) {
        IControlFlexAction handle = HANDLES.get(action);
        return handle != null && handle.isActive();
    }

    private static DefaultBinding defaultBinding(SpookyAction action) {
        switch (action) {
            case GRAB:
                return DefaultBinding.of(ButtonName.LEFT_STICK_CLICK, TriggerMode.HOLD);
            case PUSH:
                return DefaultBinding.of(ButtonName.LEFT_STICK_UP, TriggerMode.HOLD);
            case PULL:
                return DefaultBinding.of(ButtonName.LEFT_STICK_DOWN, TriggerMode.HOLD);
            default:
                return null;
        }
    }
}
