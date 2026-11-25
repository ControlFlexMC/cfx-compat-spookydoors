package com.ifels.cfx.spookydoors;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Answers "is the crosshair on a door Spooky Doors would let us drag".
 *
 * <p>Every condition mirrors the mod's own grab check so this bridge never injects a press that
 * the door mod would decline — a declined press falls through to vanilla use and would place a
 * block or eat food instead of grabbing. The two data-driven checks (tags) are read directly;
 * the per-door {@code spooky} flag only exists in Spooky Doors' own state, so it is read by
 * reflection and the bridge switches itself off if that ever stops working.</p>
 */
public final class DoorTargeting {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyAction.MOD_ID);

    private static final TagKey<Block> DOORS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("spookydoors", "doors"));
    private static final TagKey<Block> EXCLUDED_DOORS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("spookydoors", "excluded_doors"));
    private static final TagKey<Item> HAUNTS_DOORS =
            TagKey.create(Registries.ITEM, new ResourceLocation("spookydoors", "haunts_doors"));
    private static final TagKey<Item> EXORCISES_DOORS =
            TagKey.create(Registries.ITEM, new ResourceLocation("spookydoors", "exorcises_doors"));

    private static Method providerGet;
    private static Method providerAt;
    private static Method doorSpooky;
    private static boolean reflectionBroken;

    private DoorTargeting() {
    }

    /** The door's lower half when the crosshair is on a door we may drag, otherwise {@code null}. */
    public static BlockPos target(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(DOORS) || state.is(EXCLUDED_DOORS)) {
            return null;
        }
        if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) {
            return null;
        }

        BlockPos base = basePos(pos, state);
        if (!isSpooky(mc.level, base)) {
            return null;
        }
        if (bypassesDrag(mc.player, true)) {
            return null;
        }
        return base;
    }

    /** Mirror of {@code SpookyDoorUtils.getBasePos}: always work on the lower half. */
    private static BlockPos basePos(BlockPos pos, BlockState state) {
        if (state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** Mirror of {@code shouldBypassDragging}'s item branches. */
    private static boolean bypassesDrag(Player player, boolean spooky) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!spooky && stack.is(HAUNTS_DOORS)) {
                return true;
            }
            if (spooky && stack.is(EXORCISES_DOORS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpooky(Level level, BlockPos base) {
        if (!initReflection()) {
            return false;
        }
        try {
            Object provider = providerGet.invoke(null, level);
            if (provider == null) {
                return false;
            }
            Object door = providerAt.invoke(provider, base);
            if (door == null) {
                return false;
            }
            return Boolean.TRUE.equals(doorSpooky.invoke(door));
        } catch (Throwable t) {
            reflectionBroken = true;
            LOGGER.warn("[{}] stopping door targeting: Spooky Doors state could not be read ({})",
                    SpookyAction.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean initReflection() {
        if (reflectionBroken) {
            return false;
        }
        if (providerGet != null) {
            return true;
        }
        try {
            Class<?> providerClass =
                    Class.forName("net.blay09.mods.spookydoors.core.SpookyDoorProvider");
            Class<?> doorClass =
                    Class.forName("net.blay09.mods.spookydoors.core.SpookyDoor");
            providerGet = providerClass.getMethod("get", Level.class);
            providerAt = providerClass.getMethod("at", BlockPos.class);
            doorSpooky = doorClass.getMethod("spooky");
            return true;
        } catch (Throwable t) {
            reflectionBroken = true;
            LOGGER.warn("[{}] stopping door targeting: Spooky Doors classes not found ({})",
                    SpookyAction.MOD_ID, t.toString());
            return false;
        }
    }
}
