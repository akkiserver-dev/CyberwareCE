package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adds the mod's loot to vanilla chests, see {@link CyberwareLootTables}.
 */
public class CyberwareLootModifierProvider extends GlobalLootModifierProvider {
    private static final List<ResourceKey<LootTable>> NEUROPOZYNE_CHESTS = List.of(
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.STRONGHOLD_CROSSING,
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE);

    public CyberwareLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CyberwareMod.MOD_ID);
    }

    @Override
    protected void start() {
        AnyOfCondition.Builder anyChest = AnyOfCondition.anyOf();
        for (ResourceKey<LootTable> chest : NEUROPOZYNE_CHESTS) {
            anyChest.or(LootTableIdCondition.builder(chest.location()));
        }
        add("neuropozyne_in_chests", new AddTableLootModifier(
                new LootItemCondition[]{anyChest.build()}, CyberwareLootTables.NEUROPOZYNE_CHEST));
        add("surgery_in_nether_bridge", new AddTableLootModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(BuiltInLootTables.NETHER_BRIDGE.location()).build()},
                CyberwareLootTables.NETHER_BRIDGE_CHEST));
    }
}
