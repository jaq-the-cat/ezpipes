package com.jaquiethecat.ezpipes.pipedata.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NetworkInventory {
    SimpleContainer inventory;
    long battery;
    List<FluidTank> tanks;
    static final int MAX_STACKS = 128;
    static final int MAX_TANKS = 32;
    static final int MAX_FLUID_PER_TANk = 10_000_000; // 10k buckets
    static final long MAX_ENERGY = 128_000_000_000L; // 128GFE

    NetworkInventory() {
        this.inventory = new SimpleContainer(MAX_STACKS);
        this.battery = 0;
        this.tanks = new ArrayList<>();
    }

    public ItemStack addItem(@NotNull ItemStack stack) {
        return inventory.addItem(stack);
    }

    private static FluidStack fill(FluidStack fluid, FluidTank tank) {
        int initialAmount = fluid.getAmount();
        var filled = tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        var remaining = initialAmount - filled;
        if (remaining <= 0) return FluidStack.EMPTY;
        fluid.setAmount(remaining);
        return fluid;
    }

    public FluidStack addFluid(@NotNull FluidStack fluid) {
        for (FluidTank tank : tanks)
            fluid = fill(fluid, tank);
        if (fluid.isEmpty()) return FluidStack.EMPTY;
        if (tanks.size() < MAX_TANKS) {
            var newTank = new FluidTank(MAX_FLUID_PER_TANk);
            fluid = fill(fluid, newTank);
        }
        return fluid;
    }

    public int addEnergy(long energy) {
        int remaining = 0;
        if (battery < MAX_ENERGY) {
            battery += energy;
        }
        if (battery > MAX_ENERGY) {
            remaining = (int) (MAX_ENERGY - battery);
            battery = MAX_ENERGY;
        }
        return remaining;
    }

    public @NotNull ItemStack extractFirstItemMatching(PipeFilter filter) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (filter == null || filter.match(itemStack.getItem()))
                return inventory.removeItem(i, itemStack.getCount());
        }
        return ItemStack.EMPTY;
    }

    public @NotNull FluidStack extractFirstFluidMatching(PipeFilter filter, int transfer) {
        for (int tankIndex = 0; tankIndex < tanks.size(); tankIndex++)
            if (filter == null || filter.match(tanks.get(tankIndex).getFluid().getFluid())) {
                var stack = tanks.get(tankIndex).drain(transfer, IFluidHandler.FluidAction.EXECUTE);
                if (stack.isEmpty())
                    tanks.remove(tankIndex);
                return stack;
            }
        return FluidStack.EMPTY;
    }

    public int extractEnergy(int transfer) {
        if (battery > transfer) {
            battery -= transfer;
            return transfer;
        }
        int val = (int) battery;
        battery = 0;
        return val;
    }

    public void addFrom(NetworkInventory other) {
        for (int slot = 0; slot < other.inventory.getContainerSize(); slot++)
            addItem(other.inventory.getItem(slot));
        for (FluidTank tank : other.tanks)
            addFluid(tank.getFluid());
        addEnergy(other.battery);
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        // add items
        nbt.put("inventory", inventory.createTag());
        // add fluids
        ListTag tanksTag = new ListTag();
        for (FluidTank tank : tanks) {
            CompoundTag tankTag = new CompoundTag();
            tanksTag.add(tank.writeToNBT(tankTag));
        }
        nbt.put("tanks", tanksTag);
        // add battery
        nbt.putLong("battery", battery);
        return nbt;
    }

    public static NetworkInventory deserializeNBT(Tag tag) {
        var inv = new NetworkInventory();
        if (tag instanceof CompoundTag nbt) {
            // get items
            inv.inventory.fromTag((ListTag) nbt.get("inventory"));
            // get fluids
            var tanksTag = (ListTag) nbt.get("tanks");
            for (Tag tankTag : tanksTag)
                if (tankTag instanceof CompoundTag tankNbt)
                    inv.tanks.add(new FluidTank(MAX_FLUID_PER_TANk).readFromNBT(tankNbt));
            // get battery
            inv.battery = nbt.getLong("battery");
        }
        return inv;
    }

    @Override
    public String toString() {
        return "NetworkInventory{" +
                "invSize=" + inventory.getContainerSize() + "\n" + inventory + "\n" +
                "tankSize=" + tanks.size() + "\n" + tanks + "\n" +
                "battery=" + battery + " FE" + "\n" +
            "}";
    }
}
