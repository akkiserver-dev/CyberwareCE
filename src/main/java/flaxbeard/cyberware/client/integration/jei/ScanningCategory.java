package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * Scanning cyberware in the Scanner: the item is kept, and each scan has a chance of using paper for its blueprint.
 */
public class ScanningCategory extends AbstractRecipeCategory<DeconstructableItem> {
    public ScanningCategory(IGuiHelper guiHelper) {
        super(CyberwareJeiPlugin.SCANNING, Component.translatable("jei.cyberware.scanning"),
                guiHelper.createDrawableItemLike(CyberwareBlocks.SCANNER.get()), 73, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DeconstructableItem recipe, IFocusGroup focuses) {
        ItemStack toScan = recipe.stack();
        builder.addInputSlot(1, 1)
                .setStandardSlotBackground()
                .addItemStack(toScan)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(CyberwareJeiPlugin.notConsumed()));
        builder.addInputSlot(1, 19)
                .setStandardSlotBackground()
                .addIngredients(Ingredient.of(CyberwareTags.PAPER));
        builder.addOutputSlot(55, 10)
                .setStandardSlotBackground()
                .addItemStack(BlueprintItem.createBlueprint(recipe.item()))
                .addRichTooltipCallback((view, tooltip) -> addChance(tooltip, toScan));
    }

    private static void addChance(ITooltipBuilder tooltip, ItemStack toScan) {
        Level level = CyberwareJeiPlugin.level();
        tooltip.add(Component.translatable("cyberware.gui.destroy_chance",
                CyberwareJeiPlugin.percent(ScannerBlockEntity.getChance(level, toScan))));
        float additional = CyberwareConfig.get(level).scannerChanceAdditional();
        if (!toScan.isDamageableItem() && additional > 0F) {
            tooltip.add(Component.translatable("jei.cyberware.chance_per_item",
                    CyberwareJeiPlugin.percent(additional), CyberwareJeiPlugin.percent(ScannerBlockEntity.MAX_CHANCE)));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DeconstructableItem recipe, IFocusGroup focuses) {
        int time = Math.max(1, CyberwareConfig.get(CyberwareJeiPlugin.level()).scannerTime());
        builder.addAnimatedRecipeArrowWidget(time)
                .setPosition(24, 10)
                .setTooltip(Component.translatable("jei.cyberware.scan_time", time / 1200, time % 1200 / 20));
    }

    @Override
    public ResourceLocation getRegistryName(DeconstructableItem recipe) {
        return recipe.id("scanning/");
    }
}
