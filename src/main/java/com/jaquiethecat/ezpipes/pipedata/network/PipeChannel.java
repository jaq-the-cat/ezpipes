package com.jaquiethecat.ezpipes.pipedata.network;

import com.jaquiethecat.ezpipes.enums.FilterType;
import com.jaquiethecat.ezpipes.enums.TransferType;
import com.jaquiethecat.ezpipes.items.pipe.PipeUpgradeItem;
import com.jaquiethecat.ezpipes.pipedata.PipeTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

class Distributer {

    protected Distributer() {
    }

    public boolean canTransfer(BlockPos pos) {
        return true;
    }

    @Override
    public String toString() {
        return "Distributer{ " + " }";
    }
}

public class PipeChannel {
    public TransferType transferType;
    public PipeUpgradeItem upgradeItem;
    public Set<PipeFilter> filters = new HashSet<>();
    public final Distributer distributer = new Distributer();

    public PipeTier getUpgrade() {
        if (upgradeItem == null)
            return PipeTier.DEFAULT;
        return upgradeItem.upgrade;
    }

    public PipeChannel(TransferType transferType) {
        this.transferType = transferType;
        filters.add(new PipeFilter(FilterType.Whitelist, "*"));
    }

    public PipeChannel() {
        this(TransferType.None);
    }

    public boolean canTransfer(boolean isInput, BlockPos pos) {
        if (isInput) {
            return true;
        }
        return distributer.canTransfer(pos);
    }

    private ListTag serializeFiltersNBT() {
        ListTag list = new ListTag();
        filters.forEach(filter -> list.add(filter.serializeNBT()));
        return list;
    }
    private static Set<PipeFilter> deserializeFiltersNBT(ListTag filtersList) {
        Set<PipeFilter> filters = new HashSet<>();
        for (Tag tag : filtersList)
            if (tag instanceof CompoundTag nbt)
                filters.add(PipeFilter.deserializeNBT(nbt));
        return filters;
    }
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("transferType", transferType.name());
        nbt.put("filters", serializeFiltersNBT());
        return nbt;
    }
    public static PipeChannel deserializeNBT(CompoundTag nbt) {
        var channel = new PipeChannel();
        channel.transferType = TransferType.valueOf(nbt.getString("transferType"));
        channel.filters = deserializeFiltersNBT((ListTag) nbt.get("filters"));
        return channel;
    }

    @Override
    public String toString() {
        return "PipeChannel{ " + transferType + " :: " +
                filters + " :: " +
                distributer + " }";
    }

    public PipeChannel copy() {
        var newChannel = new PipeChannel();
        for (PipeFilter filter : filters)
            newChannel.filters.add(new PipeFilter(filter.filterType, filter.filter));
        newChannel.transferType = transferType;
        return newChannel;
    }
}
