package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Cyberware that, while active, allows the cyberware HUD to be displayed.
 */
public interface Hudjack {
    boolean isActive(ItemStack stack);
}
