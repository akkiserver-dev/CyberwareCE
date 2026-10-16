package flaxbeard.cyberware.common.block;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.menu.SurgeryMenu;
import flaxbeard.cyberware.common.registry.CyberwareAttributes;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

/**
 * The Robosurgeon, placed on top of a Surgery Chamber.
 */
public class SurgeryBlock extends MachineBlock {
    public static final MapCodec<SurgeryBlock> CODEC = simpleCodec(SurgeryBlock::new);

    public SurgeryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SurgeryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CyberwareBlockEntities.SURGERY.get(), SurgeryBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SurgeryBlockEntity surgery)) {
            return InteractionResult.PASS;
        }
        // make sure the base tolerance follows config changes
        AttributeInstance tolerance = player.getAttribute(CyberwareAttributes.TOLERANCE);
        if (tolerance != null) {
            tolerance.setBaseValue(CyberwareConfig.ESSENCE.get());
        }
        surgery.updatePlayerSlots(player, CyberwareAPI.getUserData(player));
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new SurgeryMenu(containerId, inventory, surgery),
                    Component.translatable("block.cyberware.surgery")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    protected IItemHandler getDroppedInventory(BlockEntity blockEntity) {
        return blockEntity instanceof SurgeryBlockEntity surgery ? surgery.slots : null;
    }
}
