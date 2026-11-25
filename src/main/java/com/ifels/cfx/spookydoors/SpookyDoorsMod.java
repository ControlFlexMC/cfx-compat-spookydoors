package com.ifels.cfx.spookydoors;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Mod entry point.
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
public final class SpookyDoorsMod {

    public SpookyDoorsMod() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                TickEvent.ClientTickEvent.class, DoorDrag::onClientTick);
    }
}
