package com.jaquiethecat.ezpipes;

import com.jaquiethecat.ezpipes.blocks.pipe.PipeBlock;
import com.jaquiethecat.ezpipes.enums.TransferType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class EPUtils {
    private EPUtils() {}

    public static boolean entityIsStorage(BlockEntity neighborEntity, Direction side) {
        return neighborEntity != null && !(neighborEntity.getBlockState().getBlock() instanceof PipeBlock)
                && (
                neighborEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent() ||
                neighborEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent() ||
                neighborEntity.getCapability(ForgeCapabilities.ENERGY, side).isPresent());
    }

    public static boolean areAnyNeighborsStorage(BlockPos pos, LevelReader level) {
        for (var dir : Direction.values()) {
            if (entityIsStorage(level.getBlockEntity(pos.relative(dir)), dir.getOpposite()))
                return true;
        }
        return false;
    }

    private static final HashMap<BlockPos, Set<BlockEntity>> neighboringStorageCache = new HashMap<>();
    private static final int COUNTDOWN_MAX = 40;
    private static int neighboringStorageCountdown = COUNTDOWN_MAX;
    public static void updateNeighboringStorage(BlockPos pos, LevelReader level) {
        Set<BlockEntity> neighbors = new HashSet<>();
        for (var dir : Direction.values()) {
            var entity = level.getBlockEntity(pos.relative(dir));
            if (entityIsStorage(entity, dir.getOpposite())) neighbors.add(entity);
        }
        neighboringStorageCache.put(pos, neighbors);
    }
    public static void checkNeighboringUpdateCountdown(BlockPos pos, LevelReader level) {
        neighboringStorageCountdown -= 1;
        if (neighboringStorageCountdown <= 0) {
            updateNeighboringStorage(pos, level);
            neighboringStorageCountdown = COUNTDOWN_MAX;
        }
    }

    public static Set<BlockEntity> getNeighboringStorage(BlockPos pos, LevelReader level) {
        var value = neighboringStorageCache.get(pos);
        if (value == null || value.isEmpty()) {
            updateNeighboringStorage(pos, level);
        }
        return neighboringStorageCache.get(pos);
    }

    public static @Nullable <T> T getCapability(Capability<T> cap, Direction side, BlockEntity entity){
        var capability = entity.getCapability(cap, side).resolve();
        return capability.orElse(null);
    }

    public static String getItemId(ItemLike item) {
        return item.asItem().getCreatorModId(item.asItem().getDefaultInstance())
                + ":" + item.asItem();
    }

    public static String getFluidId(Fluid fluid) {
        var id = fluid.getFluidType().toString();
        EZPipes.LOGGER.debug("------------------------------------------------------");
        EZPipes.LOGGER.debug("------------------------------------------------------");
        EZPipes.LOGGER.debug("------------------------------------------------------");
        EZPipes.LOGGER.debug(id);
        EZPipes.LOGGER.debug("------------------------------------------------------");
        EZPipes.LOGGER.debug("------------------------------------------------------");
        EZPipes.LOGGER.debug("------------------------------------------------------");
        return id;
    }

    public static Capability<?> transferToCapability(TransferType transferType) {
        return switch (transferType) {
            case None -> null;
            case Item -> ForgeCapabilities.ITEM_HANDLER;
            case Fluid -> ForgeCapabilities.FLUID_HANDLER;
            case Energy -> ForgeCapabilities.ENERGY;
        };
    }
}
