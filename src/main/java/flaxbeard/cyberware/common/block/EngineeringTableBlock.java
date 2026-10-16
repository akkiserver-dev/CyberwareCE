package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.menu.EngineeringTableMenu;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * A two block tall table: the lower half is a plain machine block, the upper half holds the press and the inventory.
 */
public class EngineeringTableBlock extends HorizontalMachineBlock {
    public static final MapCodec<EngineeringTableBlock> CODEC = simpleCodec(EngineeringTableBlock::new);

    private static final VoxelShape UPPER_NORTH = box(4, 0, 0, 12, 16, 12);
    private static final VoxelShape UPPER_SOUTH = box(4, 0, 4, 12, 16, 16);
    private static final VoxelShape UPPER_EAST = box(4, 0, 4, 16, 16, 12);
    private static final VoxelShape UPPER_WEST = box(0, 0, 4, 12, 16, 12);

    public EngineeringTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TwoHighBlockHelper.HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!TwoHighBlockHelper.isUpper(state)) return Shapes.block();
        return switch (state.getValue(FACING)) {
            case SOUTH -> UPPER_SOUTH;
            case EAST -> UPPER_EAST;
            case WEST -> UPPER_WEST;
            default -> UPPER_NORTH;
        };
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return TwoHighBlockHelper.isUpper(state) ? Shapes.empty() : Shapes.block();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!TwoHighBlockHelper.canPlace(context)) return null;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        TwoHighBlockHelper.placeUpperHalf(level, pos, state);
        // the name of the item applies to the upper half, which holds the inventory
        if (level.getBlockEntity(pos.above()) instanceof EngineeringTableBlockEntity table) {
            table.applyComponentsFromItemStack(stack);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = TwoHighBlockHelper.updateShape(this, state, direction, neighborState);
        return updated.isAir() ? updated : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TwoHighBlockHelper.preventCreativeDrop(this, level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos upperPos = TwoHighBlockHelper.getUpperPos(state, pos);
        if (!(level.getBlockEntity(upperPos) instanceof EngineeringTableBlockEntity table)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            EngineeringTableMenu.Layout layout = EngineeringTableMenu.Layout.find(table, serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> new EngineeringTableMenu(id, inventory, table, layout),
                    table.getName()), buffer -> {
                buffer.writeBlockPos(upperPos);
                layout.write(buffer);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TwoHighBlockHelper.isUpper(state) ? new EngineeringTableBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CyberwareBlockEntities.ENGINEERING_TABLE.get(), EngineeringTableBlockEntity::tick);
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        return blockEntity instanceof EngineeringTableBlockEntity table ? table.slots : null;
    }
}
