package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.recipe.EngineeringRecipe;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * Assembling cyberware in the Engineering Table: the components plus the blueprint, which is kept.
 */
public class EngineeringCategory extends AbstractRecipeCategory<RecipeHolder<EngineeringRecipe>> {
    public EngineeringCategory(IGuiHelper guiHelper) {
        super(CyberwareJeiPlugin.ENGINEERING, Component.translatable("jei.cyberware.engineering"),
                guiHelper.createDrawableItemLike(CyberwareBlocks.ENGINEERING_TABLE.get()), 133, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EngineeringRecipe> holder, IFocusGroup focuses) {
        EngineeringRecipe recipe = holder.value();
        List<SizedIngredient> ingredients = recipe.ingredients();
        // the six crafting slots of the table
        for (int index = 0; index < 6; index++) {
            IRecipeSlotBuilder slot = builder.addInputSlot(1 + index % 3 * 18, 1 + index / 3 * 18).setStandardSlotBackground();
            if (index < ingredients.size()) {
                slot.addItemStacks(List.of(ingredients.get(index).getItems()));
            }
        }
        builder.addInputSlot(61, 10)
                .setStandardSlotBackground()
                .addItemStack(BlueprintItem.createBlueprint(recipe.result().getItem()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(CyberwareJeiPlugin.notConsumed()));
        builder.addOutputSlot(115, 10)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<EngineeringRecipe> holder, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(84, 10);
    }
}
