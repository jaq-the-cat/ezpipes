package com.jaquiethecat.ezpipes.blocks.pipe;

import com.google.common.collect.ImmutableMap;
import com.jaquiethecat.ezpipes.EPUtils;
import com.jaquiethecat.ezpipes.blocks.ModBlockEntities;
import com.jaquiethecat.ezpipes.blocks.pipe.network.PipeNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, EntityBlock {
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    public static final BooleanProperty IS_IO = BooleanProperty.create("is_io");

    public static final VoxelShape CORE_SHAPE = Block.box(4,4,4,12,12,12);
    public static final Map<Direction, VoxelShape> DIR_SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
            .put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12))
            .put(Direction.DOWN, Block.box(4, 0, 4, 12, 6, 12))
            .put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 6))
            .put(Direction.SOUTH, Block.box(4, 4, 10, 12, 12, 16))
            .put(Direction.EAST, Block.box(10, 4, 4, 16, 12, 12))
            .put(Direction.WEST, Block.box(0, 4, 4, 6, 12, 12))
            .build();

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return CORE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return CORE_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public PipeBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(0.5f, 5f));
        registerDefaultState(getStateDefinition().any()
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(IS_IO, false));
    }

    BooleanProperty getDirectionProperty(Direction dir) {
        return switch (dir) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST, IS_IO);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PIPE.get(), PipeBlockEntity::tick);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState,
                                  LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        return pState.setValue(
                getDirectionProperty(pDirection),
                canConnectTo(pDirection.getOpposite(), pNeighborState.getBlock(), pNeighborPos, pLevel));
    }

    protected boolean canConnectTo(Direction side, Block neighbor, BlockPos pNeighborPos, LevelAccessor pLevel) {
        return (neighbor instanceof PipeBlock) || EPUtils.entityIsStorage(
                pLevel.getBlockEntity(pNeighborPos), side);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState pState = super.getStateForPlacement(pContext);
        if (!pContext.canPlace()) return pState;
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState pNeighborState = level.getBlockState(neighborPos);
            // join pipe with other
            pState = updateShape(pState, dir, pNeighborState, level, pos, neighborPos);
            level.setBlockAndUpdate(neighborPos, pNeighborState.getBlock().updateShape(
                    pNeighborState, dir.getOpposite(), pState, level, neighborPos, pos));
        }
        return pState;
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pLevel.isClientSide() || pState == pOldState) return;
        var networks = PipeNetworks.getInstance(pLevel.getServer());
        networks.addOrCreateNetwork(pPos);

        for (var dir : Direction.values()) {
            var neighborPos = pPos.relative(dir);
            var neighborState = pLevel.getBlockState(neighborPos);
            var neighborEntity = pLevel.getBlockEntity(neighborPos);
            if (neighborState.getBlock() instanceof PipeBlock) {
                networks.merge(pPos, neighborPos, pLevel);
            } else if (EPUtils.entityIsStorage(neighborEntity, dir.getOpposite())) {
                pLevel.setBlockAndUpdate(pPos, pState.setValue(IS_IO, true));
            }
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pPos.relative(dir);
                BlockState pNeighborState = pLevel.getBlockState(neighborPos);
                if (pNeighborState.getBlock() instanceof PipeBlock) {
                    pLevel.setBlockAndUpdate(neighborPos, pNeighborState
                            .setValue(getDirectionProperty(dir.getOpposite()), false));
                }
            }
            if (!pLevel.isClientSide()) {
                var networks = PipeNetworks.getInstance(pLevel.getServer());
                networks.removeWhichContain(pPos, pLevel);
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader pLevel, BlockPos pos, BlockPos neighbor) {
        if (pLevel.isClientSide()) return;
        var level = (Level) pLevel;
        if (level.getBlockState(neighbor).getBlock() instanceof PipeBlock) return;

        if (EPUtils.areAnyNeighborsStorage(pos, pLevel)) {
            level.setBlockAndUpdate(pos, state.setValue(IS_IO, true));

        } else {
            level.setBlockAndUpdate(pos, state.setValue(IS_IO, false));
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide() && pHand == InteractionHand.MAIN_HAND) {
            if (pPlayer.getItemInHand(pHand).isEmpty()) {
                if (pPlayer.isShiftKeyDown()) {
                    var entity = pLevel.getBlockEntity(pHit.getBlockPos());
                    if (entity instanceof PipeBlockEntity pipeEntity) {
                        pipeEntity.channels.get(0).isPulling ^= true;
                        pPlayer.sendSystemMessage(Component.literal(pipeEntity.channels.get(0).toString()));
                    }
                } else {
                    var networks = PipeNetworks.getInstance(pLevel.getServer());
                    pPlayer.sendSystemMessage(Component.literal(networks.toString()));
                }
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(IS_IO))
            return new PipeBlockEntity(pos, state);
        return null;
    }
}
