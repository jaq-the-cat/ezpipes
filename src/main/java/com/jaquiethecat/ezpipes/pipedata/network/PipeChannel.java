package com.jaquiethecat.ezpipes.pipedata.network;

import com.jaquiethecat.ezpipes.enums.FilterType;
import com.jaquiethecat.ezpipes.enums.TransferType;
import com.jaquiethecat.ezpipes.items.pipe.PipeUpgradeItem;
import com.jaquiethecat.ezpipes.pipedata.PipeTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Distributer {
    private ArrayList<BlockPos> outputs;
    private int index;

    protected Distributer() {
        outputs = new ArrayList<>();
        index = -1;
    }
    public boolean contains(BlockPos pos) {
        return outputs.contains(pos);
    }
    public void add(BlockPos pos) {
        outputs.add(pos);
    }
    public void remove(BlockPos pos) {
        outputs.remove(pos);
    }
    public void clear() {
        outputs.clear();
        index = -1;
    }
    public boolean amNext(BlockPos pos) {
        if (outputs.isEmpty()) return false;
        int next = index+1;
        if (next >= outputs.size())
            next = 0;
        return outputs.get(next) == pos;
    }
    public void fixIndex() {
        if (outputs.isEmpty())
            index = -1;
        else if (index < 0)
            index = outputs.size()-1;
        else if (index >= outputs.size())
            index = 0;
    }
    public void next() {
        index += 1;
        fixIndex();
    }
    @Override
    public String toString() {
        return "Distributer{ " +
                outputs + " [" + index + "/" + (outputs.size()-1) + "] "
                + "}";
    }
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("index", index);
        List<Integer> outputsTag = new ArrayList<>(outputs.size()*3);
        for (BlockPos output : outputs) {
            outputsTag.add(output.getX());
            outputsTag.add(output.getY());
            outputsTag.add(output.getZ());
        }
        nbt.putIntArray("outputs", outputsTag);
        return nbt;
    }
    public void loadFromNBT(CompoundTag nbt) {
        index = nbt.getInt("index");
        var outputsTag = nbt.getIntArray("outputs");
        outputs.ensureCapacity(outputsTag.length/3);
        for (int i = 0; i < outputsTag.length - 2; i += 3)
            outputs.add(new BlockPos(outputsTag[i], outputsTag[i + 1], outputsTag[i + 2]));
    }
}

public class PipeChannel {
    public TransferType transferType;
    public PipeUpgradeItem upgradeItem;
    public Set<PipeFilter> filters = new HashSet<>();
    public final Distributer dist = new Distributer();

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

    public void clear() {
        dist.clear();
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
        nbt.put("dist", dist.serializeNBT());
        return nbt;
    }
    public static PipeChannel deserializeNBT(CompoundTag nbt) {
        var channel = new PipeChannel();
        channel.transferType = TransferType.valueOf(nbt.getString("transferType"));
        channel.filters = deserializeFiltersNBT((ListTag) nbt.get("filters"));
        channel.dist.loadFromNBT((CompoundTag) nbt.get("dist"));
        return channel;
    }

    @Override
    public String toString() {
        return "PipeChannel{ " + transferType + " :: " +
                filters + " :: " +
                dist + " }";
    }

    public PipeChannel copy() {
        var newChannel = new PipeChannel();
        for (PipeFilter filter : filters)
            newChannel.filters.add(new PipeFilter(filter.filterType, filter.filter));
        newChannel.transferType = transferType;
        return newChannel;
    }
}
