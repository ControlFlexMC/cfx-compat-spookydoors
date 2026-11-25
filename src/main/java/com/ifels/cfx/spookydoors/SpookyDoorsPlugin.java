package com.ifels.cfx.spookydoors;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlFlex SPI entry point.
 *
 * <p>Two things happen here, in this order: the three door actions are declared (so they show up
 * on the Spooky Doors adaptation card and can be rebound), and then the drag engine is switched
 * on once the API is confirmed new enough to report analog magnitudes.</p>
 */
public final class SpookyDoorsPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    /** {@code AnalogValues} — without it a stick's travel cannot be read at all. */
    private static final String MIN_API_VERSION = "0.9.3.0";

    @Override
    public String getModId() {
        return SpookyAction.MOD_ID;
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
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
