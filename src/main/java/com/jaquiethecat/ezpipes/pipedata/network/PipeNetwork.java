package com.jaquiethecat.ezpipes.pipedata.network;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.jaquiethecat.ezpipes.blocks.pipe.PipeBlockEntity;
import com.jaquiethecat.ezpipes.enums.TransferType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

public class PipeNetwork {
    private String name = "Unnamed Network";
    private final MutableGraph<BlockPos> graph;
    public NetworkInventory inventory = new NetworkInventory();
    public HashMap<UUID, PipeChannel> channels = new HashMap<>();
    protected PipeNetwork() {
        graph = GraphBuilder.undirected().allowsSelfLoops(true).build();
        channels.put(UUID.randomUUID(), new PipeChannel(TransferType.Item));
    }

    public PipeNetwork(BlockPos initial) {
        this();
        graph.addNode(initial);
    }

    public PipeChannel getChannel(UUID id) {
        return channels.get(id);
    }

    protected PipeNetwork(MutableGraph<BlockPos> graph) {
        this.graph = graph;
        channels.put(UUID.randomUUID(), new PipeChannel(TransferType.Item));
    }

    public boolean contains(BlockPos pos) {
        return graph.nodes().contains(pos);
    }

    public ItemStack insertItem(ItemStack stack) {
        return inventory.addItem(stack);
    }
    public FluidStack insertFluid(FluidStack stack) {
        return inventory.addFluid(stack);
    }
    public int insertEnergy(int energy) {
        return inventory.addEnergy(energy);
    }
    public int extractEnergy(int transfer) {
        return inventory.extractEnergy(transfer);
    }
    public ItemStack extractFirstItemMatching(PipeFilter filter) {
        return inventory.extractFirstItemMatching(filter);
    }
    public FluidStack extractFirstFluidMatching(PipeFilter filter, int mult) {
        return inventory.extractFirstFluidMatching(filter, mult);
    }

    public void add(BlockPos pos, BlockPos otherPos) {
        graph.addNode(otherPos);
        graph.putEdge(pos, otherPos);
    }

    public void remove(BlockPos pos) {
        graph.removeNode(pos);
        for (PipeChannel channel : channels.values())
            channel.dist.remove(pos);
    }

    public Set<MutableGraph<BlockPos>> getSectionsFrom(BlockPos removed) {
        Set<MutableGraph<BlockPos>> sections = new HashSet<>();
        for (BlockPos adjacent : graph.adjacentNodes(removed)) {
            var section = getSubgraphFrom(removed, adjacent);
            sections.add(section);
        }
        return sections;
    }

    public MutableGraph<BlockPos> getSubgraphFrom(BlockPos removed, BlockPos pos) {
        MutableGraph<BlockPos> subGraph = GraphBuilder
                .undirected()
                .allowsSelfLoops(true)
                .build();
        subGraph.addNode(removed);
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(pos);
        while (!queue.isEmpty()) {
            BlockPos node = queue.remove();
            subGraph.addNode(node);
            for (var adjacent : graph.adjacentNodes(node))
                if (!subGraph.nodes().contains(adjacent)) {
                    queue.add(adjacent);
                    subGraph.putEdge(node, adjacent);
                }
        }
        subGraph.removeNode(removed);
        return subGraph;
    }

    public boolean isEmpty() {
        return graph.nodes().isEmpty();
    }

    public void mergeWith(PipeNetwork other, BlockPos connectedTo, BlockPos connecting) {
        for (BlockPos node : other.graph.nodes())
            graph.addNode(node);
        for (EndpointPair<BlockPos> edge : other.graph.edges())
            graph.putEdge(edge.nodeU(), edge.nodeV());
        inventory.addFrom(other.inventory);
        graph.putEdge(connectedTo, connecting);
        clearChannels();
    }
    public void clearChannels() {
        for (PipeChannel channel : this.channels.values()) {
            channel.dist.clear();
        }
    }
    // Go through each node, get their BlockEntity and update its network ID
    public void updateAllIDs(UUID newId, LevelAccessor level) {
        for (BlockPos node : graph.nodes()) {
            var entity = (PipeBlockEntity) level.getBlockEntity(node);
            if (entity != null) {
                entity.syncedChannels.clear();
                entity.networkId = newId;
            }
        }
    }
    public String toString() {
        StringBuilder str = new StringBuilder("PipeNetwork{");
        str.append("\n  Nodes {");
        for (BlockPos node : graph.nodes()) {
            str.append("\n      ").append(node);
        }
        str.append("\n  } Edges {");
        for (EndpointPair<BlockPos> edge : graph.edges()) {
            str.append("\n      ").append(edge);
        }
        str.append("\n  } Inventory {");
        str.append("\n      ").append(inventory);
        str.append("\n  }\n}");
        return str.toString();
    }
    private void addPosToList(List<Integer> tag, BlockPos pos) {
        tag.add(pos.getX());
        tag.add(pos.getY());
        tag.add(pos.getZ());
    }
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        List<BlockPos> nodes = graph.nodes().stream().toList();
        List<EndpointPair<BlockPos>> edges = graph.edges().stream().toList();
        // name
        nbt.putString("name", name);
        // nodes
        {
            var nodesTag = new ArrayList<Integer>(nodes.size() * 3);
            for (BlockPos node : nodes)
                addPosToList(nodesTag, node);
            nbt.putIntArray("nodes", nodesTag);
        }
        // edges
        {
            var edgesTag = new ArrayList<Integer>(graph.edges().size() * 2 * 3);
            for (var edge : edges) {
                addPosToList(edgesTag, edge.nodeU());
                addPosToList(edgesTag, edge.nodeV());
            }
            nbt.putIntArray("edges", edgesTag);
        }
        // inventory
        nbt.put("inventory", inventory.serializeNBT());
        // channels
        {
            ListTag channelsLT = new ListTag();
            channels.forEach((uuid, channel) -> {
                CompoundTag tag = new CompoundTag();
                tag.putUUID("uuid", uuid);
                tag.put("channel", channel.serializeNBT());
            });
            nbt.put("channels", channelsLT);
        }

        return nbt;
    }
    public static PipeNetwork deserializeNBT(CompoundTag nbt) {
        var network = new PipeNetwork();
        int[] nodes = nbt.getIntArray("nodes");
        int[] edges = nbt.getIntArray("edges");
        // name
        network.name = nbt.getString("name");
        // nodes
        for (int i = 0; i < nodes.length - 2; i += 3)
            network.graph.addNode(new BlockPos(nodes[i], nodes[i + 1], nodes[i + 2]));
        // edges
        for (int i = 0; i < edges.length - 5; i += 6)
            network.graph.putEdge(
                    new BlockPos(edges[i], edges[i + 1], edges[i + 2]),
                    new BlockPos(edges[i + 3], edges[i + 4], edges[i + 5]));
        // inventory
        network.inventory = NetworkInventory.deserializeNBT(nbt.get("inventory"));
        // channels
        ListTag channelsLT = (ListTag) nbt.get("channels");
        for (Tag tag : channelsLT) {
            if (tag instanceof CompoundTag cEntryNbt) {
                network.channels.put(
                    cEntryNbt.getUUID("uuid"),
                    PipeChannel.deserializeNBT((CompoundTag) cEntryNbt.get("channel"))
                );
            }
        }
        return network;
    }
}
