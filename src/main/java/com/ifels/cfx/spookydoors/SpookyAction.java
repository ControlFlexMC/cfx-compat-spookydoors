package com.ifels.cfx.spookydoors;

/**
 * The three actions this bridge exposes to ControlFlex.
 *
 * <p>They are registered into the in-game layer only: the door is dragged while the player is
 * playing, never from a screen, so no per-screen table is involved and no {@code compat} JSON
 * has to be installed.</p>
 *
 * <p>Ids are permanent — they are written into the user's binding file, so renaming one loses
 * that binding.</p>
 */
public enum SpookyAction {

    /** Hold to grab the door the crosshair is on. Injects the {@code use} press and release. */
    GRAB("grab"),

    /** Push the grabbed door away. Magnitude comes from the bound axis, not from this action. */
    PUSH("push"),

    /** Pull the grabbed door towards the player. Same magnitude rule as {@link #PUSH}. */
    PULL("pull");

    public static final String MOD_ID = "cfx_compat_spookydoors";

    /** The mod these actions belong to, used to group them on its adaptation card. */
    public static final String TARGET_MOD_ID = "spookydoors";

    private final String key;

    SpookyAction(String key) {
        this.key = key;
    }

    /** Translation key, also the display name key on the adaptation card. */
    public String nameKey() {
        return "key." + MOD_ID + "." + key;
    }

    /** Stable ControlFlex action id. */
    public String actionId() {
        return MOD_ID + ":" + key;
    }
}
