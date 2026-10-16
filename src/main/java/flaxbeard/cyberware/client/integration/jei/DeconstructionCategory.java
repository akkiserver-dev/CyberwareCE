package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Destroying cyberware in the Engineering Table: all the components it can break into, of which some are lost,
 * and with paper a chance of its blueprint.
 */
public class DeconstructionCategory extends AbstractRecipeCategory<DeconstructableItem> {
    public DeconstructionCategory(IGuiHelper guiHelper) {
        super(CyberwareJeiPlugin.DECONSTRUCTION, Component.translatable("jei.cyberware.deconstruction"),
                guiHelper.createDrawableItemLike(CyberwareBlocks.ENGINEERING_TABLE.get()), 133, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DeconstructableItem recipe, IFocusGroup focuses) {
        ItemStack toDestroy = recipe.stack();
        builder.addInputSlot(1, 1)
                .setStandardSlotBackground()
                .addItemStack(toDestroy);
        builder.addInputSlot(1, 19)
                .setStandardSlotBackground()
                .addIngredients(Ingredient.of(CyberwareTags.PAPER))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.cyberware.paper")));

        // the six crafting slots of the table receive the components
        List<ItemStack> components = recipe.components();
        for (int index = 0; index < 6; index++) {
            IRecipeSlotBuilder slot = builder.addOutputSlot(55 + index % 3 * 18, 1 + index / 3 * 18).setStandardSlotBackground();
            if (index < components.size()) {
                slot.addItemStack(components.get(index));
            }
        }
        builder.addOutputSlot(115, 10)
                .setStandardSlotBackground()
                .addItemStack(BlueprintItem.createBlueprint(recipe.item()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("cyberware.gui.destroy_chance",
                        CyberwareJeiPlugin.percent(EngineeringTableBlockEntity.getBlueprintChance(CyberwareJeiPlugin.level(), toDestroy)))));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DeconstructableItem recipe, IFocusGroup focuses) {
        Level level = CyberwareJeiPlugin.level();
        int lost = EngineeringTableBlockEntity.getComponentsLost(level == null ? Difficulty.NORMAL : level.getDifficulty());
        builder.addRecipeArrowWidget()
                .setPosition(24, 10)
                .setTooltip(List.of(
                        Component.translatable("jei.cyberware.components_lost", lost),
                        Component.translatable("jei.cyberware.components_lost.damaged")));
    }

    @Override
    public ResourceLocation getRegistryName(DeconstructableItem recipe) {
        return recipe.id("deconstruction/");
    }
}
