package flaxbeard.cyberware.common.recipe;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A blank blueprint crafted with a deconstructable item becomes the blueprint of that item; the item is kept.
 */
public class BlueprintCraftingRecipe extends CustomRecipe {
    public BlueprintCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * @return the index of the item to copy, or -1 if the input doesn't match
     */
    private static int findItemToCopy(CraftingInput input) {
        int itemIndex = -1;
        boolean hasBlankBlueprint = false;
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) continue;
            if (CyberwareAPI.canDeconstruct(stack) && stack.getCount() == 1 && !stack.is(CyberwareItems.BLUEPRINT.get())) {
                if (itemIndex != -1) return -1;
                itemIndex = index;
            } else if (stack.is(CyberwareItems.BLUEPRINT.get()) && BlueprintItem.isBlank(stack)) {
                if (hasBlankBlueprint) return -1;
                hasBlankBlueprint = true;
            } else {
                return -1;
            }
        }
        return hasBlankBlueprint ? itemIndex : -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findItemToCopy(input) != -1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int index = findItemToCopy(input);
        return index == -1 ? ItemStack.EMPTY : BlueprintItem.getBlueprintForItem(input.getItem(index));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        int index = findItemToCopy(input);
        if (index != -1) {
            remaining.set(index, input.getItem(index).copy());
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CyberwareRecipes.BLUEPRINT_CRAFTING.get();
    }
}
