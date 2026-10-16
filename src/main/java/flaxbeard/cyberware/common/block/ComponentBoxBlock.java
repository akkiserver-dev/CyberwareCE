package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.ComponentBoxBlockEntity;
import flaxbeard.cyberware.common.menu.ComponentBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Sneak-use picks the box up with its contents; otherwise opens it.
 */
public class ComponentBoxBlock extends HorizontalMachineBlock {
    public static final MapCodec<ComponentBoxBlock> CODEC = simpleCodec(ComponentBoxBlock::new);

    private static final VoxelShape SHAPE_EAST_WEST = box(1, 0, 4, 15, 10, 12);
    private static final VoxelShape SHAPE_NORTH_SOUTH = box(4, 0, 1, 12, 10, 15);

    public ComponentBoxBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComponentBoxBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ComponentBoxBlockEntity box)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(box.collectComponents());
            box.setChanged();
            // empty the box first so nothing is dropped when it's removed
            for (int index = 0; index < box.slots.getSlots(); index++) {
                box.slots.setStackInSlot(index, ItemStack.EMPTY);
            }
            level.removeBlock(pos, false);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } else {
            player.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> ComponentBoxMenu.forBlock(id, inventory, box), box.getName()),
                    buffer -> {
                        buffer.writeBoolean(false);
                        buffer.writeBlockPos(pos);
                    });
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        // the contents are kept on the dropped item, see the loot table
        return null;
    }
}
