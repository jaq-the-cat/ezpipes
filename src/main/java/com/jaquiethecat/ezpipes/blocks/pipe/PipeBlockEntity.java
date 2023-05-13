package com.jaquiethecat.ezpipes.blocks.pipe;

import com.jaquiethecat.ezpipes.EPUtils;
import com.jaquiethecat.ezpipes.blocks.ModBlockEntities;
import com.jaquiethecat.ezpipes.blocks.pipe.network.PipeChannel;
import com.jaquiethecat.ezpipes.blocks.pipe.network.PipeNetwork;
import com.jaquiethecat.ezpipes.blocks.pipe.network.PipeNetworks;
import com.jaquiethecat.ezpipes.blocks.pipe.network.TransferManager;
import com.jaquiethecat.ezpipes.enums.TransferType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class PipeBlockEntity extends BlockEntity implements MenuProvider {
    public UUID networkId;
    public List<PipeChannel> channels;
    protected int defaultTicksToTransfer = 20*4; // 4 seconds
    private int ticksRemaining;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE.get(), pos, state);
        channels = new ArrayList<>();
        channels.add(new PipeChannel(false, TransferType.Energy));
    }

    public UUID getNetwork(ServerLevel level) {
        var networks = PipeNetworks.getInstance(level);
        networkId = networks.thatContains(getBlockPos());
        if (networkId == null) networkId = networks.addOrCreateNetwork(getBlockPos());
        return networkId;
    }

    public UUID getNetwork(MinecraftServer server) {
        return getNetwork(server.overworld());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.ticksRemaining -= 1;
        EPUtils.checkNeighboringUpdateCountdown(pos, level);
        if (entity.ticksRemaining <= 0) {
            UUID netId = entity.getNetwork(level.getServer());
            PipeNetwork net = PipeNetworks.getInstance(level.getServer()).getNetwork(netId);
            entity.ticksRemaining = net.upgrade == null ? entity.defaultTicksToTransfer : net.upgrade.ticksToTransfer;
            // transfer stuff
            TransferManager.transfer(entity.channels, net, pos, level);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.literal("Pipe");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putUUID("networkId", networkId);
        nbt.put("channels", PipeChannel.serializeChannels(channels));
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        networkId = nbt.getUUID("networkId");
        channels = PipeChannel.deserializeChannels(nbt.get("channels"));
    }
}
