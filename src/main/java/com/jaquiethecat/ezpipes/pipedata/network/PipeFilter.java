package com.jaquiethecat.ezpipes.pipedata.network;

import com.jaquiethecat.ezpipes.EPUtils;
import com.jaquiethecat.ezpipes.enums.FilterType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

public class PipeFilter {
    public FilterType filterType;
    public String filter;

    public PipeFilter() {
    }

    public PipeFilter(FilterType filterType, String filter) {
        this.filterType = filterType;
        this.filter = filter;
    }

    @Override
    public String toString() {
        return "PipeFilter{ type: " + filterType + " filter: " + filter + "}";
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("filter", filter);
        tag.putString("filterType", filterType.name());
        return tag;
    }

    public static PipeFilter deserializeNBT(CompoundTag nbt) {
        PipeFilter filter = new PipeFilter();
        filter.filter = nbt.getString("filter");
        filter.filterType = FilterType.valueOf(nbt.getString("filterType"));
        return filter;
    }

    public boolean match(Item item) {
        return match(EPUtils.getItemId(item));
    }

    public boolean match(Fluid fluid) {
        return match(EPUtils.getFluidId(fluid));
    }

    public boolean match(String id) {
        boolean matches = FilenameUtils.wildcardMatch(id, filter, IOCase.INSENSITIVE);
        if (filterType == FilterType.Whitelist)
            return matches;
        else
            return !matches;
    }
}
