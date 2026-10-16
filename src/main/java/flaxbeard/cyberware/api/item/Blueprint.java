package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An item usable in the Engineering Table's blueprint slot.
 */
public interface Blueprint {
    /**
     * @param stack the blueprint
     * @param items the six crafting slots of the Engineering Table
     * @return the crafted result, or {@link ItemStack#EMPTY} if the requirements aren't met
     */
    ItemStack getResult(ItemStack stack, List<ItemStack> items);

    /**
     * @return the crafting slots after the requirements have been removed
     */
    List<ItemStack> consumeItems(ItemStack stack, List<ItemStack> items);

    default ItemStack getIconForDisplay(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    default List<ItemStack> getRequirementsForDisplay(ItemStack stack) {
        return List.of();
    }
}
