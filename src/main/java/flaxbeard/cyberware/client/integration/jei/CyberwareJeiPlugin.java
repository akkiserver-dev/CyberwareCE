package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.gui.ScannerScreen;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.recipe.BlueprintCraftingRecipe;
import flaxbeard.cyberware.common.recipe.EngineeringRecipe;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shows the Engineering Table and the Scanner in JEI: assembling cyberware from blueprints, deconstructing it into
 * components and scanning it into blueprints. Also shows the crafting of blueprints and tells blueprints apart.
 */
@JeiPlugin
public class CyberwareJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<EngineeringRecipe>> ENGINEERING = RecipeType.createRecipeHolderType(CyberwareAPI.id("engineering"));
    public static final RecipeType<DeconstructableItem> DECONSTRUCTION = RecipeType.create(CyberwareMod.MOD_ID, "deconstruction", DeconstructableItem.class);
    public static final RecipeType<DeconstructableItem> SCANNING = RecipeType.create(CyberwareMod.MOD_ID, "scanning", DeconstructableItem.class);

    @Override
    public ResourceLocation getPluginUid() {
        return CyberwareAPI.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(CyberwareItems.BLUEPRINT.get(), new ISubtypeInterpreter<>() {
            @Override
            @Nullable
            public Object getSubtypeData(ItemStack blueprint, UidContext context) {
                ItemStack stored = BlueprintItem.getBlueprintItem(blueprint);
                return stored.isEmpty() ? null : stored.getItem();
            }

            // still required by JEI, for data saved by older versions
            @Override
            @Deprecated
            public String getLegacyStringSubtypeInfo(ItemStack blueprint, UidContext context) {
                ItemStack stored = BlueprintItem.getBlueprintItem(blueprint);
                return stored.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stored.getItem()).toString();
            }
        });
        // the creative tab has trench coats in several colors
        registration.registerSubtypeInterpreter(CyberwareItems.TRENCH_COAT.get(), new ISubtypeInterpreter<>() {
            @Override
            @Nullable
            public Object getSubtypeData(ItemStack coat, UidContext context) {
                DyedItemColor color = coat.get(DataComponents.DYED_COLOR);
                return color == null ? null : color.rgb();
            }

            // still required by JEI, for data saved by older versions
            @Override
            @Deprecated
            public String getLegacyStringSubtypeInfo(ItemStack coat, UidContext context) {
                DyedItemColor color = coat.get(DataComponents.DYED_COLOR);
                return color == null ? "" : Integer.toHexString(color.rgb());
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new EngineeringCategory(guiHelper),
                new DeconstructionCategory(guiHelper),
                new ScanningCategory(guiHelper));
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(BlueprintCraftingRecipe.class, new BlueprintCraftingExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = level();
        if (level != null) {
            registration.addRecipes(ENGINEERING, level.getRecipeManager().getAllRecipesFor(CyberwareRecipes.ENGINEERING_TYPE.get()));
        }
        // sent by the server before the recipes, so they are known by now
        List<DeconstructableItem> deconstructable = DeconstructableItem.all();
        registration.addRecipes(DECONSTRUCTION, deconstructable);
        registration.addRecipes(SCANNING, deconstructable);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(ENGINEERING, CyberwareItems.ENGINEERING_TABLE.get());
        registration.addRecipeCatalysts(DECONSTRUCTION, CyberwareItems.ENGINEERING_TABLE.get());
        registration.addRecipeCatalysts(SCANNING, CyberwareItems.SCANNER.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // the progress bar
        registration.addRecipeClickArea(ScannerScreen.class, 5, 32, 162, 9, SCANNING);
    }

    /**
     * The level of the client, for the synced config and the difficulty.
     */
    @Nullable
    static Level level() {
        return Minecraft.getInstance().level;
    }

    /**
     * @return a chance in percent formatted like in the machines
     */
    static String percent(float chance) {
        return Math.round(chance * 100F) / 100F + "%";
    }

    static Component notConsumed() {
        return Component.translatable("jei.cyberware.not_consumed");
    }
}
