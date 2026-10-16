package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.block.TwoHighBlockHelper;
import flaxbeard.cyberware.common.recipe.Deconstruction;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Block drops, deconstruction results and the loot added to vanilla chests (see {@link CyberwareLootModifierProvider}).
 */
public final class CyberwareLootTables {
    /** Added to dungeon, mineshaft, stronghold and temple chests. */
    static final ResourceKey<LootTable> NEUROPOZYNE_CHEST = ResourceKey.create(Registries.LOOT_TABLE, CyberwareAPI.id("chests/neuropozyne"));
    /** Added to Nether fortress chests. */
    static final ResourceKey<LootTable> NETHER_BRIDGE_CHEST = ResourceKey.create(Registries.LOOT_TABLE, CyberwareAPI.id("chests/nether_bridge"));

    private CyberwareLootTables() {
    }

    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(provider -> new DeconstructionLoot(), Deconstruction.PARAMS),
                new LootTableProvider.SubProviderEntry(provider -> new ChestLoot(), LootContextParamSets.CHEST)
        ), registries);
    }

    private static class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(CyberwareBlocks.SURGERY.get());
            dropSelf(CyberwareBlocks.CHARGER.get());
            dropSelf(CyberwareBlocks.SCANNER.get());
            dropSelf(CyberwareBlocks.BLUEPRINT_ARCHIVE.get());
            dropSelf(CyberwareBlocks.RADIO.get());
            dropSelf(CyberwareBlocks.RADIO_BEACON.get());
            dropSelf(CyberwareBlocks.RADIO_POST.get());
            // two block tall machines drop from their lower half only
            add(CyberwareBlocks.SURGERY_CHAMBER.get(), block -> createSinglePropConditionTable(block, TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER));
            add(CyberwareBlocks.ENGINEERING_TABLE.get(), block -> createSinglePropConditionTable(block, TwoHighBlockHelper.HALF, DoubleBlockHalf.LOWER));
            // the Component Box keeps its contents and name
            Block box = CyberwareBlocks.COMPONENT_BOX.get();
            add(box, LootTable.lootTable().withPool(applyExplosionCondition(box, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(box).apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                            .include(DataComponents.CONTAINER)
                            .include(DataComponents.CUSTOM_NAME))))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return CyberwareBlocks.BLOCKS.getEntries().stream().map(Holder::value).toList();
        }
    }

    /**
     * One pool per component, so the loot table always gives all of them; the Engineering Table loses some at random.
     */
    private static class DeconstructionLoot implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            EngineeringParts.get().forEach((item, parts) -> {
                LootTable.Builder table = LootTable.lootTable();
                for (ItemStack part : parts) {
                    table.withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(part.getItem())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(part.getCount())))));
                }
                output.accept(Deconstruction.lootTable(item.asItem()), table);
            });
        }
    }

    private static class ChestLoot implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(NEUROPOZYNE_CHEST, LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(CyberwareItems.NEUROPOZYNE.get()).setWeight(15)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(16, 64))))
                    .add(EmptyLootItem.emptyItem().setWeight(85))));
            output.accept(NETHER_BRIDGE_CHEST, LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(CyberwareItems.SURGERY.get()).setWeight(15))
                    .add(EmptyLootItem.emptyItem().setWeight(60))));
        }
    }
}
