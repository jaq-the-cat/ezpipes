package com.jaquiethecat.ezpipes.items;

import com.jaquiethecat.ezpipes.EZPipes;
import com.jaquiethecat.ezpipes.items.pipe.PipeUpgrade;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EZPipes.MOD_ID);

    public static final RegistryObject<PipeUpgrade> PIPE_UP_T1 = ITEMS.register("pipe_upgrade_t1",
            () -> new PipeUpgrade(20*3)); // 3s
    public static final RegistryObject<PipeUpgrade> PIPE_UP_T2 = ITEMS.register("pipe_upgrade_t2",
            () -> new PipeUpgrade(20*2)); // 2s
    public static final RegistryObject<PipeUpgrade> PIPE_UP_T3 = ITEMS.register("pipe_upgrade_t3",
            () -> new PipeUpgrade(20)); // 1s
    public static final RegistryObject<PipeUpgrade> PIPE_UP_T4 = ITEMS.register("pipe_upgrade_t4",
            () -> new PipeUpgrade(10)); // 0.5s
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}