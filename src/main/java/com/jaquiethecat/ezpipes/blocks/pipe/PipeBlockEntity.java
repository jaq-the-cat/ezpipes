package com.jaquiethecat.ezpipes.blocks.pipe;

import com.jaquiethecat.ezpipes.blocks.ModBlockEntities;
import com.jaquiethecat.ezpipes.pipedata.network.ChannelReference;
import com.jaquiethecat.ezpipes.pipedata.network.PipeNetwork;
import com.jaquiethecat.ezpipes.pipedata.network.PipeNetworks;
import com.jaquiethecat.ezpipes.pipedata.network.TransferManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PipeBlockEntity extends BlockEntity implements MenuProvider {
    public UUID networkId;
    public Set<ChannelReference> syncedChannels = new HashSet<>();
    public static final int TICKS_TO_TRANSFER = 20*2; // 2 seconds
    private int ticksRemaining;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE.get(), pos, state);
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
        if (entity.ticksRemaining <= 0) {
            entity.ticksRemaining = TICKS_TO_TRANSFER;

            MinecraftServer server = level.getServer();
            UUID netId = entity.getNetwork(server);
            PipeNetwork net = PipeNetworks.getInstance(server).getNetwork(netId);

            entity.syncAllChannels(net); // TODO: Remove this
            TransferManager.transfer(entity.syncedChannels, net, pos, level);
        }
    }

    private void syncAllChannels(PipeNetwork net) {
        if (syncedChannels.isEmpty()) { // TODO: Remove this if statement too
//            syncedChannels.clear();
            net.channels.forEach((uuid, channel) ->
                    syncedChannels.add(new ChannelReference(uuid, false)));
        }
    }
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }
    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ezpipes.warped_pipe");
    }
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putUUID("networkId", networkId);
        ListTag channelsLT = new ListTag();
        for (ChannelReference syncedChannel : syncedChannels) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", syncedChannel.id);
            tag.putBoolean("isInput", syncedChannel.isInput);
        }
        nbt.put("channels", channelsLT);
        super.saveAdditional(nbt);
    }
    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        networkId = nbt.getUUID("networkId");
        var channelsLT = (ListTag) nbt.get("channels");
        if (channelsLT == null) return;
        syncedChannels = new HashSet<>(channelsLT.size());
        for (Tag tag : channelsLT)
            if (tag instanceof CompoundTag channelTag)
                syncedChannels.add(new ChannelReference(
                        channelTag.getUUID("uuid"),
                        channelTag.getBoolean("isInput")));
    }
}
