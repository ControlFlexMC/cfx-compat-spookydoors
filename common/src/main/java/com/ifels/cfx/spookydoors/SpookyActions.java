package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionBindingProvider;
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
 *
 * <p><b>They are registered unbound on purpose.</b> Bindings belong to the player: the three
 * actions are meant to be bound in one layer whose left stick is otherwise free, which is what
 * stops the stick from driving movement and the door at the same time. Nothing here seeds a
 * default. That relies on the core latching a registered action by the mode that fired — a HOLD
 * binding made in the binding screen has to keep {@code isActive()} true while the button is
 * down — see {@link SpookyDoorsPlugin#MIN_API_VERSION}.</p>
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
     * Whether the binding in effect right now is a hold-style trigger.
     *
     * <p>{@code isActive()} already tells us how long the action lasts — latched for HOLD, one
     * tick for PRESS — but not how far one step should be, and the two want different steps. The
     * mode comes from the resolved binding (per layer, per state), so this follows whatever the
     * player configured instead of assuming HOLD.</p>
     *
     * <p>Unbound and API-not-ready both answer {@code false}, which only means "one-shot"; the
     * action cannot be active in either case, so the step is never spent.</p>
     */
    static boolean isBoundAsHold(SpookyAction action) {
        IActionBindingProvider provider = ControlFlexApi.getActionBindingProvider();
        if (provider == null) {
            return false;
        }
        return isHoldLike(provider.getActiveBinding(action.actionId()).getTriggerMode());
    }

    /** Modes that stay active while the button is down (or latched on, for the toggles). */
    static boolean isHoldLike(TriggerMode mode) {
        return mode == TriggerMode.HOLD
                || mode == TriggerMode.DELAY_HOLD
                || mode == TriggerMode.LONG_HOLD
                || mode == TriggerMode.DOUBLE_HOLD
                || mode == TriggerMode.TOGGLE
                || mode == TriggerMode.DOUBLE_TOGGLE;
    }

    /**
     * The declaration for one action — no {@code defaultBinding}, deliberately.
     *
     * <p>A default would put the door on a button before the player chose a layer for it, and
     * whichever layer shipped it would then collide with that layer's own bindings. The bundled
     * compat config seeds nothing either, so the binding screen is the single source of truth.
     * Package-private so the test can assert that no default sneaks back in.</p>
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
