package com.jaquiethecat.ezpipes.pipedata.network;

import com.google.common.graph.MutableGraph;
import com.jaquiethecat.ezpipes.EZPipes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PipeNetworks extends SavedData {
    protected PipeNetworks() {}
    private final Map<UUID, PipeNetwork> networks = new HashMap<>();
    public String toString() {
        return "PipeNetworks [\n" + networks + "\n]";
    }
    protected void add(UUID id, PipeNetwork network) {
        networks.put(id, network);
        this.setDirty();
    }
    public UUID addOrCreateNetwork(BlockPos pos) {
        UUID contains = thatContains(pos);
        if (contains != null) return contains;
        UUID id = UUID.randomUUID();
        networks.put(id, new PipeNetwork(pos));
        this.setDirty();
        return id;
    }
    protected UUID newNetwork(MutableGraph<BlockPos> graph) {
        UUID id = UUID.randomUUID();
        networks.put(id, new PipeNetwork(graph));
        this.setDirty();
        return id;
    }
    public PipeNetwork getNetwork(UUID id) {
        return networks.get(id);
    }
    public UUID thatContains(BlockPos pos) {
        for (UUID netId : networks.keySet()) {
            if (getNetwork(netId).contains(pos)) return netId;
        }
        return null;
    }
    public void merge(BlockPos newPipe, BlockPos neighborPipe, LevelAccessor level) {
        // newPipe always in a network
        var neighborNetId = thatContains(neighborPipe);
        var newNetId = thatContains(newPipe);
        if (neighborNetId == newNetId) return;
        var neighborNet = getNetwork(neighborNetId);
        var newNet = getNetwork(newNetId);

        if (neighborNetId == null) {
            newNet.add(neighborPipe, newPipe);
        } else {
            neighborNet.mergeWith(newNet, neighborPipe, newPipe);
            newNet.updateAllIDs(neighborNetId, level);
            networks.remove(newNetId);
        }
        this.setDirty();
    }
    public void removeWhichContain(BlockPos pos, Level level) {
        var id = thatContains(pos);
        if (id == null) return;
        var net = getNetwork(id);
        var netInv = net.inventory;
        var channels = net.channels;
        
        Set<MutableGraph<BlockPos>> sections = net.getSectionsFrom(pos);
        if (sections != null && sections.size() > 1) {
            unmergeInto(id, sections, netInv, channels, level);
            return;
        }
        net.remove(pos);
        if (net.isEmpty()) {
            networks.remove(id);
        } else {
        }

        this.setDirty();
    }
    public void unmergeInto(UUID id, Set<MutableGraph<BlockPos>> sections,
                            NetworkInventory netInv, HashMap<UUID, PipeChannel> channels, LevelAccessor level) {
        boolean transferInventory = true;
        for (MutableGraph<BlockPos> section : sections) {
            if (section.nodes().isEmpty()) continue;
            var newId = newNetwork(section);
            var net = getNetwork(newId);
            net.updateAllIDs(newId, level);
            channels.forEach((channelId, channel) -> net.channels.put(channelId, channel.copy()));
            net.clearChannels();
            if (transferInventory) {
                net.inventory = netInv;
                transferInventory = false;
            }
        }
        networks.remove(id);
        this.setDirty();
    }
    public static PipeNetworks getInstance(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PipeNetworks::load, PipeNetworks::new,
                EZPipes.MOD_ID + "_pipe_networks");
    }
    public static PipeNetworks getInstance(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PipeNetworks::load, PipeNetworks::new,
                EZPipes.MOD_ID + "_pipe_networks");
    }
    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        var networksListTag = new ListTag();
        networks.forEach((id, network) -> {
            var networkNbt = new CompoundTag();
            networkNbt.putUUID("id", id);
            networkNbt.put("network", network.serializeNBT());
            networksListTag.add(networkNbt);
        });
        nbt.put("networks", networksListTag);
        return nbt;
    }
    public static PipeNetworks load(CompoundTag nbt) {
        var networks = new PipeNetworks();
        var networksListTag = (ListTag) nbt.get("networks");
        for (Tag tag : networksListTag) {
            if (tag instanceof CompoundTag netNbt) {
                networks.add(
                        netNbt.getUUID("id"),
                        PipeNetwork.deserializeNBT((CompoundTag) netNbt.get("network"))
                );
            }
        }
        return networks;
    }
}