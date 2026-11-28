package com.ifels.cfx.spookydoors.fabric;

import com.ifels.cfx.spookydoors.DoorDrag;
import com.ifels.cfx.spookydoors.SpookyAction;
import com.ifels.cfx.spookydoors.SpookyDoorsPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric entry point (MC 1.20.1).
 *
 * <p>Mirror of the Forge entry: it only wires the end-of-client-tick hook. Everything else
 * happens through the ControlFlex SPI plugin ({@link SpookyDoorsPlugin}) once ControlFlex is
 * ready, so no door logic runs while the controller stack is still loading.</p>
 *
 * <p>{@code fabric.mod.json} declares {@code "environment": "client"}, so this initializer never
 * runs on a dedicated server.</p>
 */
@Environment(EnvType.CLIENT)
public final class SpookyDoorsFabric implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> DoorDrag.tick());
        LOGGER.info("[{}] Fabric entry ready — door drag arms when ControlFlex is ready",
                SpookyAction.MOD_ID);
    }
}
