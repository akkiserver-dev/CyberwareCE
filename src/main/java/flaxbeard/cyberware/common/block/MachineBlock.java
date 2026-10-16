package flaxbeard.cyberware.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Base of the machines with an inventory that is dropped when broken.
 */
public abstract class MachineBlock extends BaseEntityBlock {
    protected MachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * @return the inventory to drop when the block is broken, or {@code null} for none
     */
    @Nullable
    protected abstract IItemHandler getDroppedInventory(BlockEntity blockEntity);

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            IItemHandler inventory = blockEntity == null ? null : getDroppedInventory(blockEntity);
            if (inventory != null) {
                for (int index = 0; index < inventory.getSlots(); index++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(index));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
