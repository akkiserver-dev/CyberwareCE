package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.block.entity.BlueprintArchiveBlockEntity;
import flaxbeard.cyberware.common.menu.BlueprintArchiveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public class BlueprintArchiveBlock extends HorizontalMachineBlock {
    public static final MapCodec<BlueprintArchiveBlock> CODEC = simpleCodec(BlueprintArchiveBlock::new);

    public BlueprintArchiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintArchiveBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BlueprintArchiveBlockEntity archive)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> new BlueprintArchiveMenu(id, inventory, archive), archive.getName()), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        return blockEntity instanceof BlueprintArchiveBlockEntity archive ? archive.slots : null;
    }
}
