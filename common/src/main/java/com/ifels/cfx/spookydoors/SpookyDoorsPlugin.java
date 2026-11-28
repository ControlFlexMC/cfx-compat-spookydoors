package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexPlugin;
import com.ifels.controlflex.api.ICompatAssetInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlFlex SPI entry point.
 *
 * <p>Three things happen here, in this order:</p>
 * <ol>
 *   <li>the bundled compat config is exported into ControlFlex's {@code compat/mods} directory —
 *       it carries no bindings, only the {@code spookydoors} attribution that puts the three
 *       actions on that adaptation card and gates the card on this bridge being installed;</li>
 *   <li>the three door actions are declared unbound, grouped under {@code spookydoors} so they
 *       are listed on that card and nowhere else;</li>
 *   <li>the drag engine is switched on once the API is confirmed new enough to report analog
 *       magnitudes.</li>
 * </ol>
 *
 * <p>Bindings are entirely the player's: they pick a layer whose left stick is free (see the
 * README), which is what keeps the stick from doing double duty. Nothing here seeds a default.</p>
 */
public final class SpookyDoorsPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    /**
     * First API release with native action registration ({@code 0.9.2.2}), layer-scoped default
     * bindings and {@link com.ifels.controlflex.api.AnalogValues} ({@code 0.9.2.4}) — without the
     * last one a stick's travel cannot be read at all.
     *
     * <p>The API version cannot express the other half of the requirement: the core must latch a
     * registered action by the mode that fired (control-flex {@code 4058359b5}+). Older cores
     * read that mode from the registration instead, so an action the player bound as HOLD in the
     * binding screen would still report {@code isActive()} for a single tick — and this mod ships
     * no declared default to work around it.</p>
     */
    public static final String MIN_API_VERSION = "0.9.2.4";

    /** Installed into {@code config/controlflex/compat/mods/}; named after the target mod. */
    static final String COMPAT_FILE_NAME = "spookydoors.json";
    private static final String COMPAT_RESOURCE =
            "/assets/cfx_compat_spookydoors/compat/" + COMPAT_FILE_NAME;

    @Override
    public String getModId() {
        return SpookyAction.MOD_ID;
    }

    /**
     * Exports the attribution file.
     *
     * <p>ControlFlex installs this before {@code onRegisterActions} and reloads the {@code mods}
     * layer afterwards, so the card is there on the same launch that installed it.</p>
     */
    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        if (installer.install(COMPAT_RESOURCE, COMPAT_FILE_NAME)) {
            LOGGER.info("[{}] installed {} (spookydoors adaptation card)", SpookyAction.MOD_ID,
                    COMPAT_FILE_NAME);
        } else {
            LOGGER.warn("[{}] could not install {} — the three actions may not be grouped under "
                            + "Spooky Doors, and would then have no card to be bound on",
                    SpookyAction.MOD_ID, COMPAT_FILE_NAME);
        }
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
        // Registering against an older API would produce never-active handles (and the action
        // classes would be missing at runtime), so skip the whole set instead.
        if (!requireApiVersion(MIN_API_VERSION)) {
            LOGGER.warn("[{}] ControlFlex API {}+ required for native actions, found {} — "
                            + "door actions not registered",
                    SpookyAction.MOD_ID, MIN_API_VERSION, ControlFlexApi.getApiVersion());
            return;
        }
        SpookyActions.register(registrar);
    }

    @Override
    public void onControlFlexReady() {
        if (!ControlFlexApi.isAvailable()) {
            LOGGER.warn("[{}] ControlFlex API unavailable — door drag disabled", SpookyAction.MOD_ID);
            return;
        }
        if (!requireApiVersion(MIN_API_VERSION)) {
            LOGGER.warn("[{}] ControlFlex API {}+ required (analog magnitudes), found {} — "
                            + "door drag disabled",
                    SpookyAction.MOD_ID, MIN_API_VERSION, ControlFlexApi.getApiVersion());
            return;
        }
        DoorDrag.setEnabled(true);
        LOGGER.info("[{}] door drag ready", SpookyAction.MOD_ID);
    }
}
