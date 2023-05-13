package com.jaquiethecat.ezpipes.blocks;

import com.jaquiethecat.ezpipes.EZPipes;
import com.jaquiethecat.ezpipes.blocks.pipe.PipeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EZPipes.MOD_ID);

    public static final RegistryObject<BlockEntityType<PipeBlockEntity>> PIPE =
            BLOCK_ENTITY.register("pipe", () -> BlockEntityType.Builder.of(
                    PipeBlockEntity::new, ModBlocks.PIPE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY.register(eventBus);
    }
}
