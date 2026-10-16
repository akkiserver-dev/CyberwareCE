package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Cyberware that belongs to a specific side of the body, such as a left arm.
 */
public interface SidedLimb {
    Side getSide(ItemStack stack);

    enum Side {
        LEFT,
        RIGHT
    }
}
