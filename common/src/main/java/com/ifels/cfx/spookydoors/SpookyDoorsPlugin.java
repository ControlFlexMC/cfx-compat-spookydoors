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
 *   <li>the bundled compat config is exported into ControlFlex's {@code compat/mods} directory,
 *       which adds the aiming-at-a-door state entry (same defaults, rebindable per state);</li>
 *   <li>the three door actions are declared, grouped under {@code spookydoors} so they are listed
 *       on that adaptation card and nowhere else;</li>
 *   <li>the drag engine is switched on once the API is confirmed new enough to report analog
 *       magnitudes.</li>
 * </ol>
 */
public final class SpookyDoorsPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    /**
     * First API release with native action registration ({@code 0.9.2.2}), layer-scoped default
     * bindings and {@link com.ifels.controlflex.api.AnalogValues} ({@code 0.9.2.4}) — without the
     * last one a stick's travel cannot be read at all.
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
     * Exports the aiming-door state entry.
     *
     * <p>ControlFlex installs this before {@code onRegisterActions} and reloads the {@code mods}
     * layer afterwards, so the entry is active on the same launch that installed it.</p>
     */
    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        if (installer.install(COMPAT_RESOURCE, COMPAT_FILE_NAME)) {
            LOGGER.info("[{}] installed {} (aiming-door bindings)", SpookyAction.MOD_ID,
                    COMPAT_FILE_NAME);
        } else {
            LOGGER.warn("[{}] could not install {} — the three actions will be listed but stay "
                            + "unbound, because their defaults live in that config",
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
