package com.jaquiethecat.ezpipes.items.pipe;

import com.jaquiethecat.ezpipes.pipedata.PipeTier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class PipeUpgradeItem extends Item {
    public final PipeTier upgrade;

    public PipeUpgradeItem(PipeTier upgrade) {
        super(new Item.Properties()
                .stacksTo(1)
                .tab(CreativeModeTab.TAB_REDSTONE));
        this.upgrade = upgrade;
    }
}
