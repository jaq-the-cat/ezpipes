package com.jaquiethecat.ezpipes.pipedata;

public record PipeTier(int itemTransfer, int fluidTransfer, int energyTransfer) {
    private static final int BUCKET = 1000;
    public static final PipeTier DEFAULT = new PipeTier(2, BUCKET*16, 16_000);
    public static final PipeTier T1 = new PipeTier(4, BUCKET*32, 64_000);
    public static final PipeTier T2 = new PipeTier(6, BUCKET*64, 256_000);
    public static final PipeTier T3 = new PipeTier(8, BUCKET*128, 1_024_000);
    public static final PipeTier T4 = new PipeTier(10, BUCKET*256, 4_096_000);
}
