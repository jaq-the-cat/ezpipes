package com.jaquiethecat.ezpipes.blocks.pipe.network;

import com.jaquiethecat.ezpipes.EPUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Collection;

public class TransferManager {
    private TransferManager() {}

    public static void transfer(Collection<PipeChannel> channels, PipeNetwork net, BlockPos pos, Level level) {
        for (var neighbor : EPUtils.getNeighboringStorage(pos, level)) {
            var side = Direction.fromNormal(pos.subtract(neighbor.getBlockPos()));
            for (PipeChannel channel : channels) {
                switch (channel.transferType) {
                    case Item -> transferItem(channel, net, side, neighbor);
                    case Fluid -> transferFluids(channel, net, side, neighbor);
                    case Energy -> transferEnergy(channel, net, side, neighbor);
                    case None -> { }
                }
            }
        }
    }

    public static void transferEnergy(PipeChannel channel, PipeNetwork net, Direction side, BlockEntity neighbor) {
        var battery = EPUtils.getCapability(ForgeCapabilities.ENERGY, side, neighbor);
        if (battery != null) {
            if (channel.isPulling) {
                int energy = battery.extractEnergy(NetworkInventory.TRANSFER_ENERGY, false);
                int remaining = net.insertEnergy(energy);
                battery.receiveEnergy(remaining, false);
            } else {
                int energy = net.extractEnergy();
                int wasAccepted = battery.receiveEnergy(energy, false);
                int remaining = energy - wasAccepted;
                net.insertEnergy(remaining);
            }
        }
    }
    public static void transferItem(PipeChannel channel, PipeNetwork net, Direction side, BlockEntity neighbor) {
        var inventory = EPUtils.getCapability(ForgeCapabilities.ITEM_HANDLER, side, neighbor);
        if (inventory != null) {
            for (PipeFilter filter : channel.filters) {
                if (channel.isPulling)
                    insertItem(net, filter, inventory);
                else
                    extractItem(net, filter, inventory);
            }
        }
    }
    public static void insertItem(PipeNetwork net, PipeFilter filter, IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var inSlot = inventory.getStackInSlot(slot);
            if (inSlot.isEmpty()) continue;
            if (filter == null || filter.match(inSlot.getItem())) {
                var stack = inventory.extractItem(slot, inSlot.getCount(), false);
                var remaining = net.insertItem(stack);
                if (!remaining.isEmpty())
                    inventory.insertItem(slot, remaining, false);
                break;
            }
        }
    }
    public static void extractItem(PipeNetwork net, PipeFilter filter, IItemHandler inventory) {
        var stack = net.extractFirstItemMatching(filter);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stack = inventory.insertItem(slot, stack, false);
        }
        if (!stack.isEmpty())
            net.insertItem(stack);
    }
    private static void transferFluids(PipeChannel channel, PipeNetwork net, Direction side, BlockEntity neighbor) {
        var fluidHandler = EPUtils.getCapability(ForgeCapabilities.FLUID_HANDLER, side, neighbor);
        if (fluidHandler != null) {
            for (PipeFilter filter : channel.filters) {
                if (channel.isPulling)
                    insertFluid(net, filter, fluidHandler);
                else
                    extractFluid(net, filter, fluidHandler);
            }
        }
    }
    public static void insertFluid(PipeNetwork net, PipeFilter filter, IFluidHandler fluidHandler) {
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            var inTank = fluidHandler.getFluidInTank(tank);
            if (inTank.isEmpty()) continue;
            if (filter == null || filter.match(inTank.getFluid())) {
                inTank.setAmount(NetworkInventory.TRANSFER_FLUID);
                var stack = fluidHandler.drain(inTank, IFluidHandler.FluidAction.EXECUTE);
                var remaining = net.insertFluid(stack);
                if (!remaining.isEmpty())
                    fluidHandler.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                break;
            }
        }
    }
    public static void extractFluid(PipeNetwork net, PipeFilter filter, IFluidHandler tanks) {
        var stack = net.extractFirstFluidMatching(filter);
        if (stack.isEmpty()) return;
        int inStack = stack.getAmount(); // has 100mB
        int filled = tanks.fill(stack, IFluidHandler.FluidAction.EXECUTE); // filled 60mB
        stack.setAmount(inStack - filled); // remaining: 100-60 = 40mB
        if (!stack.isEmpty())
            net.insertFluid(stack);
    }
}
