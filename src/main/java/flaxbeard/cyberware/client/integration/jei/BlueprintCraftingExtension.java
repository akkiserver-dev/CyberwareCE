package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.recipe.BlueprintCraftingRecipe;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Shows {@link BlueprintCraftingRecipe} in the crafting category: a blank blueprint plus any deconstructable item,
 * which is kept, gives the blueprint of that item.
 */
public class BlueprintCraftingExtension implements ICraftingCategoryExtension<BlueprintCraftingRecipe> {
    @Override
    public void setRecipe(RecipeHolder<BlueprintCraftingRecipe> recipe, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
        List<ItemStack> items = DeconstructableItem.all().stream().map(DeconstructableItem::stack).toList();
        List<ItemStack> blueprints = items.stream().map(item -> BlueprintItem.createBlueprint(item.getItem())).toList();

        // the returned list holds the whole grid
        List<IRecipeSlotBuilder> grid = craftingGridHelper.createAndSetInputs(builder,
                List.of(List.of(new ItemStack(CyberwareItems.BLUEPRINT.get())), items), 2, 1);
        IRecipeSlotBuilder item = grid.get(craftingGridHelper.getCraftingIndex(1, 2, 1));
        item.addRichTooltipCallback((view, tooltip) -> tooltip.add(CyberwareJeiPlugin.notConsumed()));
        IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, blueprints);

        // show the blueprint of the shown item
        builder.createFocusLink(item, output);
        builder.setShapeless();
    }
}
