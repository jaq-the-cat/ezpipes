package com.jaquiethecat.ezpipes.pipedata.network;

import com.jaquiethecat.ezpipes.EPUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.UUID;

public class TransferManager {
    private TransferManager() {}

    public static void transfer(HashMap<UUID, ChannelReference> channels, PipeNetwork net, BlockPos pos,
                                Level level) {
        var neighbors = EPUtils.getNeighboringStorage(pos, level);
        if (neighbors.isEmpty()) return;
        for (BlockEntity neighbor : neighbors) {
            var side = Direction.fromNormal(pos.subtract(neighbor.getBlockPos()));
            channels.forEach((id, ref) -> {
                var channel = net.getChannel(id);
                switch (channel.transferType) {
                    case Item -> transferItem(ref.isInput, channel, net, pos, side, neighbor);
                    case Fluid -> transferFluids(ref.isInput, channel, net, pos, side, neighbor);
                    case Energy -> transferEnergy(ref.isInput, channel, net, side, neighbor);
                    case None -> { }
                }
            });
        }
    }
    public static void transferItem(boolean isInput, PipeChannel channel, PipeNetwork net, BlockPos pos, Direction side,
                                    BlockEntity neighbor) {
        var inventory = EPUtils.getCapability(ForgeCapabilities.ITEM_HANDLER, side, neighbor);
        if (inventory == null) return;
        boolean goToNext = false;
        var upgrade = channel.getUpgrade();
        var dist = channel.dist;
        for (PipeFilter filter : channel.filters) {
            if (isInput) {
                dist.remove(pos);
                for (int i = 0; i < upgrade.itemTransfer(); i++)
                    extractItem(net, filter, inventory);
            } else {
                if (!dist.contains(pos)) dist.add(pos);
                if (dist.amNext(pos)) {
                    for (int i = 0; i < upgrade.itemTransfer(); i++) {
                        if (instertItem(net, filter, inventory))
                            goToNext = true;
                    }
                }
            }
            if (goToNext) dist.next();
        }
    }
    private static void transferFluids(boolean isInput, PipeChannel channel, PipeNetwork net,
                                       BlockPos pos, Direction side,
                                          BlockEntity neighbor) {
        var fluidHandler = EPUtils.getCapability(ForgeCapabilities.FLUID_HANDLER, side, neighbor);
        if (fluidHandler == null) return;
        boolean goToNext = false;
        var upgrade = channel.getUpgrade();
        var dist = channel.dist;
        for (PipeFilter filter : channel.filters) {
            if (isInput) {
                dist.remove(pos);
                extractFluid(upgrade.fluidTransfer(), net, filter, fluidHandler);
            } else {
                if (insertFluid(upgrade.fluidTransfer(), net, filter, fluidHandler))
                    goToNext = true;
            }
        }
        if (goToNext) dist.next();
    }
    public static void transferEnergy(boolean isInput, PipeChannel channel, PipeNetwork net, Direction side,
                                         BlockEntity neighbor) {
        var battery = EPUtils.getCapability(ForgeCapabilities.ENERGY, side, neighbor);
        if (battery == null) return;
        if (isInput) {
            int energy = battery.extractEnergy(
                    channel.getUpgrade().energyTransfer(),false);
            int remaining = net.insertEnergy(energy);
            battery.receiveEnergy(remaining, false);
        } else {
            var dist = channel.dist;
            int energy = net.extractEnergy(channel.getUpgrade().energyTransfer());
            int wasAccepted = battery.receiveEnergy(energy, false);
            int remaining = energy - wasAccepted;
            if (energy != remaining)
                dist.next();
            net.insertEnergy(remaining);
        }
    }
    public static void extractItem(PipeNetwork net, PipeFilter filter, IItemHandler inventory) {
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
    public static boolean instertItem(PipeNetwork net, PipeFilter filter, IItemHandler inventory) {
        var stack = net.extractFirstItemMatching(filter);
        if (stack.isEmpty()) return false;
        int initialCount = stack.getCount();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stack = inventory.insertItem(slot, stack, false);
        }
        if (!stack.isEmpty())
            net.insertItem(stack);
        return initialCount != stack.getCount();
    }
    public static void extractFluid(int transfer, PipeNetwork net, PipeFilter filter, IFluidHandler fluidHandler) {
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            var inTank = fluidHandler.getFluidInTank(tank);
            if (inTank.isEmpty()) continue;
            if (filter == null || filter.match(inTank.getFluid())) {
                inTank.setAmount(transfer);
                var stack = fluidHandler.drain(inTank, IFluidHandler.FluidAction.EXECUTE);
                var remaining = net.insertFluid(stack);
                if (!remaining.isEmpty())
                    fluidHandler.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                break;
            }
        }
    }
    public static boolean insertFluid(int transfer, PipeNetwork net, PipeFilter filter, IFluidHandler tanks) {
        var stack = net.extractFirstFluidMatching(filter, transfer);
        if (stack.isEmpty()) return false;
        int initialAmount = stack.getAmount(); // has 100mB
        int filled = tanks.fill(stack, IFluidHandler.FluidAction.EXECUTE); // filled 60mB
        stack.setAmount(initialAmount - filled); // remaining: 100-60 = 40mB
        if (!stack.isEmpty())
            net.insertFluid(stack);
        return initialAmount != stack.getAmount();
    }
}
