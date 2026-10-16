package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.block.entity.SurgeryChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A two block tall chamber with a double door, closing it with an entity inside starts the surgery of the
 * Robosurgeon placed on top of it.
 */
public class SurgeryChamberBlock extends BaseEntityBlock {
    public static final MapCodec<SurgeryChamberBlock> CODEC = simpleCodec(SurgeryChamberBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final VoxelShape TOP = box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape BOTTOM = box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape WALL_NORTH = box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape WALL_SOUTH = box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape WALL_WEST = box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WALL_EAST = box(15, 0, 0, 16, 16, 16);

    public SurgeryChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TwoHighBlockHelper.HALF, FACING, OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        // the door is on the side the chamber was placed from
        VoxelShape shape = TwoHighBlockHelper.isUpper(state) ? TOP : BOTTOM;
        if (!open || facing != Direction.SOUTH) shape = Shapes.or(shape, WALL_NORTH);
        if (!open || facing != Direction.NORTH) shape = Shapes.or(shape, WALL_SOUTH);
        if (!open || facing != Direction.EAST) shape = Shapes.or(shape, WALL_WEST);
        if (!open || facing != Direction.WEST) shape = Shapes.or(shape, WALL_EAST);
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!TwoHighBlockHelper.canPlace(context)) return null;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(OPEN, true)
                .setValue(TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (TwoHighBlockHelper.isUpper(state)) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && !TwoHighBlockHelper.isUpper(below);
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        TwoHighBlockHelper.placeUpperHalf(level, pos, state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = TwoHighBlockHelper.updateShape(this, state, direction, neighborState);
        if (updated.isAir()) return updated;
        if (direction == Direction.DOWN && !TwoHighBlockHelper.isUpper(state) && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TwoHighBlockHelper.preventCreativeDrop(this, level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        boolean top = TwoHighBlockHelper.isUpper(state);
        BlockPos upperPos = top ? pos : pos.above();
        if (canOpen(upperPos, level)) {
            toggleDoor(top, state, pos, level);
            notifySurgeon(upperPos, level);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public void toggleDoor(boolean top, BlockState state, BlockPos pos, Level level) {
        level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_CLIENTS);
        BlockPos otherPos = top ? pos.below() : pos.above();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.is(this)) {
            level.setBlock(otherPos, otherState.cycle(OPEN), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean canOpen(BlockPos upperPos, Level level) {
        return !(level.getBlockEntity(upperPos.above()) instanceof SurgeryBlockEntity surgery) || surgery.canOpen();
    }

    private static void notifySurgeon(BlockPos upperPos, Level level) {
        if (level.getBlockEntity(upperPos.above()) instanceof SurgeryBlockEntity surgery) {
            surgery.notifyChange();
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // only the upper half renders the animated doors
        return TwoHighBlockHelper.isUpper(state) ? new SurgeryChamberBlockEntity(pos, state) : null;
    }
}
