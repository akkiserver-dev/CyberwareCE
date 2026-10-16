package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * An item usable in the Engineering Table's blueprint slot. The table crafts the target with a
 * {@code cyberware:engineering} recipe whose result is that item.
 */
public interface Blueprint {
    /**
     * @return the item this blueprint lets the Engineering Table craft, or {@link ItemStack#EMPTY} if it is blank
     */
    ItemStack getTarget(ItemStack blueprint);
}
