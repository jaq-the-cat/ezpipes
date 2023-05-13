package com.jaquiethecat.ezpipes.pipedata;

import com.jaquiethecat.ezpipes.blocks.pipe.PipeBlockEntity;

public record PipeTier(int itemTransfer, int fluidTransfer, int energyTransfer) {
    private static final int BUCKET = 1000;

    // Fluid and Energy are input per tick
    @Override
    public int fluidTransfer() {
        return fluidTransfer * PipeBlockEntity.TICKS_TO_TRANSFER;
    }
    @Override
    public int energyTransfer() {
        return energyTransfer * PipeBlockEntity.TICKS_TO_TRANSFER;
    }

    public static final PipeTier DEFAULT = new PipeTier(2, BUCKET*8, 16_000);
    public static final PipeTier T1 = new PipeTier(4, BUCKET*16, 64_000);
    public static final PipeTier T2 = new PipeTier(6, BUCKET*32, 256_000);
    public static final PipeTier T3 = new PipeTier(8, BUCKET*64, 1_024_000);
    public static final PipeTier T4 = new PipeTier(10, BUCKET*128, 4_096_000);
}
