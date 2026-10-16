package flaxbeard.cyberware.common.registry;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.recipe.BlueprintCraftingRecipe;
import flaxbeard.cyberware.common.recipe.ConfigCondition;
import flaxbeard.cyberware.common.world.CyberZombieSpawnModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Serializers for the data driven content: recipes, recipe conditions and biome modifiers.
 */
public final class CyberwareRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, CyberwareMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, CyberwareMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, CyberwareMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BlueprintCraftingRecipe>> BLUEPRINT_CRAFTING = RECIPE_SERIALIZERS.register("blueprint_crafting",
            () -> new SimpleCraftingRecipeSerializer<>(BlueprintCraftingRecipe::new));

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> CONFIG_CONDITION = CONDITION_CODECS.register("config",
            () -> ConfigCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<CyberZombieSpawnModifier>> CYBER_ZOMBIE_SPAWNS = BIOME_MODIFIER_SERIALIZERS.register("cyberzombie_spawns",
            () -> CyberZombieSpawnModifier.CODEC);

    private CyberwareRecipes() {
    }
}
