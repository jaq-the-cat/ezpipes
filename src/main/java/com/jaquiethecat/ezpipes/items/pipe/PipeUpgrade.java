package com.jaquiethecat.ezpipes.items.pipe;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class PipeUpgrade extends Item {
    public final int ticksToTransfer;

    public PipeUpgrade(int ticksToTransfer) {
        super(new Item.Properties()
                .stacksTo(1)
                .tab(CreativeModeTab.TAB_REDSTONE));
        this.ticksToTransfer = ticksToTransfer;
    }
}
