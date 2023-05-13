package com.jaquiethecat.ezpipes.pipedata.network;

import com.jaquiethecat.ezpipes.enums.FilterType;
import com.jaquiethecat.ezpipes.enums.TransferType;
import com.jaquiethecat.ezpipes.pipedata.PipeTier;
import com.jaquiethecat.ezpipes.items.pipe.PipeUpgradeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class PipeChannel {
    public boolean isPulling;
    public TransferType transferType;
    public PipeUpgradeItem upgradeItem;
    public Set<PipeFilter> filters;

    public PipeTier getUpgrade() {
        if (upgradeItem == null)
            return PipeTier.DEFAULT;
        return upgradeItem.upgrade;
    }

    public PipeChannel(boolean isPulling, TransferType transferType) {
        this.isPulling = isPulling;
        this.transferType = transferType;
        filters = new HashSet<>();
        filters.add(new PipeFilter(FilterType.Whitelist, "*"));
    }

    public PipeChannel() {
        this(false, TransferType.None);
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
        nbt.putBoolean("isPulling", isPulling);
        nbt.putString("transferType", transferType.name());
        nbt.put("filters", serializeFiltersNBT());
        return nbt;
    }

    public static PipeChannel deserializeNBT(CompoundTag nbt) {
        var channel = new PipeChannel();
        channel.isPulling = nbt.getBoolean("isPulling");
        channel.transferType = TransferType.valueOf(nbt.getString("transferType"));
        channel.filters = deserializeFiltersNBT((ListTag) nbt.get("filters"));
        return channel;
    }

    public static ListTag serializeChannels(Collection<PipeChannel> channels) {
        ListTag channelsList = new ListTag();
        for (var channel : channels)
            channelsList.add(channel.serializeNBT());
        return channelsList;

    }

    public static List<PipeChannel> deserializeChannels(Tag channelsTag) {
        List<PipeChannel> channels = new ArrayList<>();
        if (channelsTag instanceof ListTag channelsListTag) {
            for (Tag channelTag : channelsListTag)
                if (channelTag instanceof CompoundTag channelNbt)
                    channels.add(deserializeNBT(channelNbt));
        }
        return channels;
    }

    @Override
    public String toString() {
        return "PipeChannel{ " + isPulling + " | " + transferType + " | " + filters + " }";
    }
}
