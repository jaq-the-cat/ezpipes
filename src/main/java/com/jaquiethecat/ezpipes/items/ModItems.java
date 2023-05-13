package com.jaquiethecat.ezpipes.items;

import com.jaquiethecat.ezpipes.EZPipes;
import com.jaquiethecat.ezpipes.pipedata.PipeTier;
import com.jaquiethecat.ezpipes.items.pipe.PipeUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EZPipes.MOD_ID);

    public static final RegistryObject<PipeUpgradeItem> PIPE_UP_T1 = ITEMS.register("pipe_upgrade_t1",
            () -> new PipeUpgradeItem(PipeTier.T1)); // 3s
    public static final RegistryObject<PipeUpgradeItem> PIPE_UP_T2 = ITEMS.register("pipe_upgrade_t2",
            () -> new PipeUpgradeItem(PipeTier.T2)); // 2s
    public static final RegistryObject<PipeUpgradeItem> PIPE_UP_T3 = ITEMS.register("pipe_upgrade_t3",
            () -> new PipeUpgradeItem(PipeTier.T3)); // 1s
    public static final RegistryObject<PipeUpgradeItem> PIPE_UP_T4 = ITEMS.register("pipe_upgrade_t4",
            () -> new PipeUpgradeItem(PipeTier.T4)); // 0.5s
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
