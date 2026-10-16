package flaxbeard.cyberware.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import flaxbeard.cyberware.common.registry.CyberwareRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crafting in the Engineering Table: a blueprint of the result plus the components, e.g.
 * <pre>{"type": "cyberware:engineering", "result": {"id": "cyberware:cybereyes"},
 *  "ingredients": [{"item": "cyberware:component_plating", "count": 1}, ...]}</pre>
 * The blueprint is not consumed.
 */
public record EngineeringRecipe(ItemStack result, List<SizedIngredient> ingredients) implements Recipe<EngineeringRecipe.Input> {
    public static final MapCodec<EngineeringRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(EngineeringRecipe::result),
            SizedIngredient.FLAT_CODEC.listOf(1, 6).fieldOf("ingredients").forGetter(EngineeringRecipe::ingredients)
    ).apply(instance, EngineeringRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineeringRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, EngineeringRecipe::result,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), EngineeringRecipe::ingredients,
            EngineeringRecipe::new);

    /**
     * @param target the item the blueprint is for
     * @param items  the crafting slots of the Engineering Table
     */
    public record Input(ItemStack target, List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    /**
     * @return the recipe crafting the target of a blueprint from the given items
     */
    public static Optional<RecipeHolder<EngineeringRecipe>> find(Level level, ItemStack target, List<ItemStack> items) {
        if (target.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(CyberwareRecipes.ENGINEERING_TYPE.get(), new Input(target, items), level);
    }

    /**
     * @return the first recipe crafting the target of a blueprint, regardless of the available items
     */
    public static Optional<EngineeringRecipe> findFor(RecipeManager recipes, ItemStack target) {
        if (target.isEmpty()) return Optional.empty();
        return recipes.getAllRecipesFor(CyberwareRecipes.ENGINEERING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> ItemStack.isSameItem(recipe.result(), target))
                .findFirst();
    }

    /**
     * @return how many more items of each ingredient are needed, in the order of the ingredients (0 when satisfied)
     */
    public int[] getMissing(List<ItemStack> items) {
        int[] missing = new int[ingredients.size()];
        for (int index = 0; index < ingredients.size(); index++) {
            SizedIngredient ingredient = ingredients.get(index);
            int count = ingredient.count();
            for (ItemStack item : items) {
                if (!item.isEmpty() && ingredient.ingredient().test(item)) {
                    count -= item.getCount();
                }
                if (count <= 0) break;
            }
            missing[index] = Math.max(0, count);
        }
        return missing;
    }

    /**
     * @return the items after the ingredients have been removed
     */
    public List<ItemStack> consume(List<ItemStack> items) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : items) {
            remaining.add(item.copy());
        }
        for (SizedIngredient ingredient : ingredients) {
            int missing = ingredient.count();
            for (int index = 0; index < remaining.size() && missing > 0; index++) {
                ItemStack item = remaining.get(index);
                if (!item.isEmpty() && ingredient.ingredient().test(item)) {
                    int toSubtract = Math.min(missing, item.getCount());
                    missing -= toSubtract;
                    item.shrink(toSubtract);
                    if (item.isEmpty()) {
                        remaining.set(index, ItemStack.EMPTY);
                    }
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!ItemStack.isSameItem(input.target(), result)) return false;
        for (int missing : getMissing(input.items())) {
            if (missing > 0) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    // not part of the recipe book
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CyberwareRecipes.ENGINEERING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CyberwareRecipes.ENGINEERING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<EngineeringRecipe> {
        @Override
        public MapCodec<EngineeringRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EngineeringRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
