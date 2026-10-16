package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only holds the client side state of the door animation.
 */
public class SurgeryChamberBlockEntity extends BlockEntity {
    public boolean lastOpen;
    public float openTicks;

    public SurgeryChamberBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.SURGERY_CHAMBER.get(), pos, state);
    }
}
