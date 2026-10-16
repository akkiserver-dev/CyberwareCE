package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.item.CyberwareArmorItem;
import flaxbeard.cyberware.common.item.KatanaItem;
import flaxbeard.cyberware.common.recipe.BlueprintCraftingRecipe;
import flaxbeard.cyberware.common.recipe.ConfigCondition;
import flaxbeard.cyberware.common.recipe.EngineeringRecipe;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The crafting recipes of the machines and the Engineering Table recipes of the cyberware.
 */
public class CyberwareRecipeProvider extends RecipeProvider {
    public CyberwareRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        buildMachineRecipes(output);
        buildBlueprintRecipes(output);
        buildEngineeringRecipes(output);
    }

    private static void buildMachineRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.SURGERY_CHAMBER.get())
                .pattern("III").pattern("IBI").pattern("IDI")
                .define('I', Tags.Items.INGOTS_IRON).define('B', Tags.Items.STORAGE_BLOCKS_IRON).define('D', Items.IRON_DOOR)
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.CHARGER.get())
                .pattern("IFI").pattern("IRI").pattern("III")
                .define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE).define('F', Items.IRON_BARS)
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.SCANNER.get())
                .pattern("IEI").pattern("IDI").pattern("III")
                .define('I', Tags.Items.INGOTS_IRON).define('D', Tags.Items.GEMS_DIAMOND).define('E', CyberwareItems.CYBEREYES.get())
                .unlockedBy("has_cybereyes", has(CyberwareItems.CYBEREYES.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.ENGINEERING_TABLE.get())
                .pattern(" PI").pattern("III").pattern("ICI")
                .define('I', Tags.Items.INGOTS_IRON).define('P', Items.PISTON).define('C', Items.CRAFTING_TABLE)
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.BLUEPRINT_ARCHIVE.get())
                .pattern("PPP").pattern("AAA").pattern("PPP")
                .define('P', Tags.Items.INGOTS_IRON).define('A', CyberwareTags.PAPER)
                .unlockedBy("has_blueprint", has(CyberwareItems.BLUEPRINT.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.COMPONENT_BOX.get())
                .pattern(" O ").pattern("ICI").pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON).define('C', Tags.Items.CHESTS_WOODEN).define('O', CyberwareTags.COMPONENTS)
                .unlockedBy("has_component", has(CyberwareTags.COMPONENTS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.RADIO.get())
                .pattern("F  ").pattern("III").pattern("ICI")
                .define('I', Tags.Items.INGOTS_IRON).define('F', CyberwareItems.FIBER_OPTICS.get()).define('C', CyberwareItems.SSC.get())
                .unlockedBy("has_component", has(CyberwareTags.COMPONENTS))
                .save(output, CyberwareAPI.id("radio"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.RADIO_BEACON.get())
                .pattern("IEI").pattern("PRP").pattern("ICI")
                .define('I', Tags.Items.INGOTS_IRON).define('R', CyberwareItems.RADIO.get()).define('E', Items.ENDER_EYE)
                .define('P', CyberwareItems.TITANIUM.get()).define('C', CyberwareItems.SSC.get())
                .unlockedBy("has_radio", has(CyberwareItems.RADIO.get()))
                .save(output, CyberwareAPI.id("radio_large"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.RADIO_POST.get(), 6)
                .pattern("B B").pattern("BFB").pattern("BPB")
                .define('B', Items.IRON_BARS).define('P', CyberwareItems.PLATING.get()).define('F', CyberwareItems.FIBER_OPTICS.get())
                .unlockedBy("has_component", has(CyberwareTags.COMPONENTS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CyberwareItems.SURGERY.get())
                .pattern("III").pattern("IRI").pattern("PSA")
                .define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('P', Items.DIAMOND_PICKAXE).define('S', Items.DIAMOND_SWORD).define('A', Items.DIAMOND_AXE)
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output.withConditions(new ConfigCondition(ConfigCondition.Option.SURGERY_CRAFTING)));
    }

    private static void buildBlueprintRecipes(RecipeOutput output) {
        // an item and a blank blueprint make a blueprint of the item, the item is kept
        SpecialRecipeBuilder.special(BlueprintCraftingRecipe::new).save(output, CyberwareAPI.id("blueprint_crafting"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.PAPER)
                .requires(CyberwareItems.BLUEPRINT.get())
                .unlockedBy("has_blueprint", has(CyberwareItems.BLUEPRINT.get()))
                .save(output, CyberwareAPI.id("paper_from_blueprint"));
    }

    private static void buildEngineeringRecipes(RecipeOutput output) {
        for (Map.Entry<ItemLike, List<ItemStack>> entry : EngineeringParts.get().entrySet()) {
            Item item = entry.getKey().asItem();
            List<SizedIngredient> ingredients = entry.getValue().stream()
                    .map(part -> SizedIngredient.of(part.getItem(), part.getCount()))
                    .toList();
            RecipeOutput target = output;
            if (item instanceof KatanaItem) {
                target = output.withConditions(new ConfigCondition(ConfigCondition.Option.KATANA));
            } else if (item instanceof CyberwareArmorItem) {
                target = output.withConditions(new ConfigCondition(ConfigCondition.Option.CLOTHES));
            }
            target.accept(BuiltInRegistries.ITEM.getKey(item).withPrefix("engineering/"), new EngineeringRecipe(new ItemStack(item), ingredients), null);
        }
    }
}
