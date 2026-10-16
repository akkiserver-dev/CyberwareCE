package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import flaxbeard.cyberware.common.menu.ScannerMenu;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public class ScannerBlock extends MachineBlock {
    public static final MapCodec<ScannerBlock> CODEC = simpleCodec(ScannerBlock::new);

    public ScannerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScannerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CyberwareBlockEntities.SCANNER.get(), ScannerBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ScannerBlockEntity scanner)) return InteractionResult.PASS;
        if (player.isCreative() && player.isShiftKeyDown()) {
            // debug shortcut: almost finish the current scan
            scanner.ticks = CyberwareConfig.SCANNER_TIME.get() - 200;
        }
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> new ScannerMenu(id, inventory, scanner), scanner.getName()), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        return blockEntity instanceof ScannerBlockEntity scanner ? scanner.slots : null;
    }
}
