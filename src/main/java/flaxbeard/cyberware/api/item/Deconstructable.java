package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An item that can be destroyed in the Engineering Table or scanned in the Scanner.
 */
public interface Deconstructable {
    boolean canDestroy(ItemStack stack);

    List<ItemStack> getComponents(ItemStack stack);
}
