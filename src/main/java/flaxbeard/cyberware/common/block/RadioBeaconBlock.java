package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.RadioBeaconBlockEntity;
import flaxbeard.cyberware.common.block.entity.RadioBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * The Radio Beacon, placed on top of a Radio Tower.
 */
public class RadioBeaconBlock extends HorizontalMachineBlock {
    public static final MapCodec<RadioBeaconBlock> CODEC = simpleCodec(RadioBeaconBlock::new);

    private static final VoxelShape SHAPE_EAST_WEST = box(5, 0, 3, 11, 16, 13);
    private static final VoxelShape SHAPE_NORTH_SOUTH = box(3, 0, 5, 13, 16, 11);
    private static final VoxelShape COLLISION = box(6.5, 0, 6.5, 9.5, 16, 9.5);

    public RadioBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing == Direction.NORTH || facing == Direction.SOUTH ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBeaconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CyberwareBlockEntities.RADIO_BEACON.get(), RadioBlockEntity::tick);
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        return null;
    }
}
