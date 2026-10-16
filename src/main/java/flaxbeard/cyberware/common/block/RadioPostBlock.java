package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.RadioPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A fence-like Radio Tower Component. A 3x3x4 base topped by a 6 block pillar of posts forms a Radio Tower,
 * which is rendered as one big model; a Radio Beacon on top of it becomes a tier 3 transmitter.
 */
public class RadioPostBlock extends BaseEntityBlock {
    public static final MapCodec<RadioPostBlock> CODEC = simpleCodec(RadioPostBlock::new);
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    /** 0: single post, 1: part of a tower, 2: the master block (bottom center) of a tower. */
    public static final IntegerProperty TRANSFORMED = IntegerProperty.create("transformed", 0, 2);
    public static final int MASTER = 2;
    public static final int TOWER_HEIGHT = 10;
    private static final int BASE_HEIGHT = 4;

    private static final VoxelShape PILLAR = box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape ARM_NORTH = box(6, 0, 0, 10, 16, 6);
    private static final VoxelShape ARM_SOUTH = box(6, 0, 10, 10, 16, 16);
    private static final VoxelShape ARM_WEST = box(0, 0, 6, 6, 16, 10);
    private static final VoxelShape ARM_EAST = box(10, 0, 6, 16, 16, 10);

    public RadioPostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TRANSFORMED, 0)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, TRANSFORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(TRANSFORMED) > 0 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = PILLAR;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        return shape;
    }

    // ---- connections ----

    private boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RadioPostBlock
                || state.getBlock() instanceof FenceGateBlock
                || (!isExceptionForConnection(state) && state.isFaceSturdy(level, pos, direction.getOpposite()));
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, connectsTo(level, pos.north(), Direction.NORTH))
                .setValue(EAST, connectsTo(level, pos.east(), Direction.EAST))
                .setValue(SOUTH, connectsTo(level, pos.south(), Direction.SOUTH))
                .setValue(WEST, connectsTo(level, pos.west(), Direction.WEST));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighborPos, direction));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ---- tower multiblock ----

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        for (int y = -(TOWER_HEIGHT - 1); y <= 0; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (tryComplete(level, pos.offset(x, y, z))) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean isTowerPosition(int x, int y, int z) {
        return y < BASE_HEIGHT || (x == 0 && z == 0);
    }

    private boolean tryComplete(Level level, BlockPos start) {
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!isTowerPosition(x, y, z)) continue;
                    BlockState state = level.getBlockState(start.offset(x, y, z));
                    if (!state.is(this) || state.getValue(TRANSFORMED) != 0) {
                        return false;
                    }
                }
            }
        }

        for (int y = 0; y < TOWER_HEIGHT; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!isTowerPosition(x, y, z)) continue;
                    BlockPos pos = start.offset(x, y, z);
                    boolean isMaster = pos.equals(start);
                    level.setBlock(pos, level.getBlockState(pos).setValue(TRANSFORMED, isMaster ? MASTER : 1), Block.UPDATE_CLIENTS);
                    if (level.getBlockEntity(pos) instanceof RadioPostBlockEntity post) {
                        post.setMaster(isMaster ? null : start);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Turns all parts of the tower whose master is at {@code start} back into single posts.
     */
    public static void destruct(Level level, BlockPos start) {
        if (!(level.getBlockEntity(start) instanceof RadioPostBlockEntity master) || master.destructing) return;
        master.destructing = true;
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!isTowerPosition(x, y, z)) continue;
                    BlockPos pos = start.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof RadioPostBlock && state.getValue(TRANSFORMED) > 0) {
                        level.setBlock(pos, state.setValue(TRANSFORMED, 0), Block.UPDATE_CLIENTS);
                        if (level.getBlockEntity(pos) instanceof RadioPostBlockEntity post) {
                            post.setMaster(null);
                        }
                    }
                }
            }
        }
        master.destructing = false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(TRANSFORMED) > 0
                && level.getBlockEntity(pos) instanceof RadioPostBlockEntity post) {
            BlockPos masterPos = state.getValue(TRANSFORMED) == MASTER ? pos : post.getMaster();
            if (masterPos != null) {
                destruct(level, masterPos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return LeadItem.bindPlayerMobs(player, level, pos);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(TRANSFORMED) > 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioPostBlockEntity(pos, state);
    }
}
