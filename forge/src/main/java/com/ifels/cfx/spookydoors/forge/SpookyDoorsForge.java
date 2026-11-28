package com.ifels.cfx.spookydoors.forge;

import com.ifels.cfx.spookydoors.DoorDrag;
import com.ifels.cfx.spookydoors.SpookyAction;
import com.ifels.cfx.spookydoors.SpookyDoorsPlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entry point (MC 1.20.1).
 *
 * <p>It does nothing but wire the client tick: everything else happens through the ControlFlex
 * SPI plugin ({@link SpookyDoorsPlugin}) once ControlFlex is ready, so no door logic runs while
 * the controller stack is still loading.</p>
 *
 * <p>The mod is declared {@code side="CLIENT"} in {@code mods.toml}; the distance check here is a
 * second guard so a dedicated server never touches {@link DoorDrag}, which references client
 * classes.</p>
 */
@Mod(SpookyAction.MOD_ID)
public final class SpookyDoorsForge {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    public SpookyDoorsForge() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                TickEvent.ClientTickEvent.class, SpookyDoorsForge::onClientTick);
        LOGGER.info("[{}] Forge entry ready — door drag arms when ControlFlex is ready",
                SpookyAction.MOD_ID);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DoorDrag.tick();
        }
    }
}
