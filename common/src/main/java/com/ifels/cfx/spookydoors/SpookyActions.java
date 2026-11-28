package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexAction;

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
 *
 * <p><b>They are registered unbound on purpose.</b> The only source of their bindings is the
 * aiming-at-a-door entry in the bundled {@code spookydoors.json}, so the left stick stays
 * dedicated to movement until the crosshair actually rests on a door. That relies on the core
 * latching a registered action by the mode that fired and honouring state overrides when
 * dispatching — see {@link SpookyDoorsPlugin#MIN_API_VERSION}.</p>
 */
public final class SpookyActions {

    private static final Map<SpookyAction, IControlFlexAction> HANDLES =
            new EnumMap<SpookyAction, IControlFlexAction>(SpookyAction.class);

    private SpookyActions() {
    }

    public static void register(IActionRegistrar registrar) {
        for (SpookyAction action : SpookyAction.values()) {
            HANDLES.put(action, registrar.register(registration(action)));
        }
    }

    /** Never {@code null}; actions start unbound and inactive when registration was skipped. */
    public static boolean isActive(SpookyAction action) {
        IControlFlexAction handle = HANDLES.get(action);
        return handle != null && handle.isActive();
    }

    /**
     * The declaration for one action — no {@code defaultBinding}, deliberately.
     *
     * <p>Bindings come from the compat config's aiming entry instead of registration, so they
     * exist only while the crosshair is on a door. Package-private so the test can assert that no
     * default sneaks back in.</p>
     */
    static ActionRegistration registration(SpookyAction action) {
        return ActionRegistration.builder(action.actionId())
                .nameKey(action.nameKey())
                .groupModId(SpookyAction.TARGET_MOD_ID)
                // In-game only: the door is dragged while playing, never from a screen.
                .layer(ActionLayer.IN_GAME)
                .build();
    }
}
