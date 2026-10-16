package flaxbeard.cyberware.common.handler;

import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.Set;

/**
 * Adds Neuropozyne to dungeon chests and the Robosurgeon to Nether fortress chests.
 */
public final class LootHandler {
    private static final Set<ResourceKey<LootTable>> NEUROPOZYNE_TABLES = Set.of(
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.STRONGHOLD_CROSSING,
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE);

    private LootHandler() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (NEUROPOZYNE_TABLES.stream().anyMatch(key -> key.location().equals(event.getName()))) {
            event.getTable().addPool(LootPool.lootPool()
                    .name("cyberware:neuropozyne")
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(CyberwareItems.NEUROPOZYNE.get()).setWeight(15)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(16, 64))))
                    .add(EmptyLootItem.emptyItem().setWeight(85))
                    .build());
        }
        if (BuiltInLootTables.NETHER_BRIDGE.location().equals(event.getName())) {
            event.getTable().addPool(singleItemPool("cyberware:surgery_apparatus", CyberwareItems.SURGERY.get(), 15, 60));
        }
    }

    private static LootPool singleItemPool(String name, ItemLike item, int weight, int emptyWeight) {
        return LootPool.lootPool()
                .name(name)
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).setWeight(weight))
                .add(EmptyLootItem.emptyItem().setWeight(emptyWeight))
                .build();
    }
}
