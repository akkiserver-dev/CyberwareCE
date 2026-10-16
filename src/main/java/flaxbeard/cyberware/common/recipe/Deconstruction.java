package flaxbeard.cyberware.common.recipe;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.network.DeconstructionSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What items break into when destroyed in the Engineering Table. Each item has a loot table at
 * {@code <namespace>:deconstruction/<path>} of the item id; items without such a table (or with an empty one)
 * can't be deconstructed or scanned. The table lists all the components, the Engineering Table then loses some
 * of them at random like in the original mod.
 * <p>
 * Loot tables only exist on the server, so the server sends the deconstructable items with a preview of their
 * components to the clients.
 */
public final class Deconstruction {
    /** Parameters of the deconstruction loot tables: the destroyed item is the tool. */
    public static final LootContextParamSet PARAMS = LootContextParamSet.builder()
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.TOOL)
            .optional(LootContextParams.THIS_ENTITY)
            .build();
    public static final ResourceLocation PARAMS_ID = CyberwareAPI.id("deconstruction");

    /** Rolls used to find all the possible components of a table for the preview. */
    private static final int PREVIEW_ROLLS = 16;

    private static volatile Map<Item, List<ItemStack>> previews = Map.of();

    private Deconstruction() {
    }

    /**
     * Makes {@code "type": "cyberware:deconstruction"} usable in loot tables. Called from the mod constructor.
     */
    public static void registerParams() {
        if (!LootContextParamSets.REGISTRY.containsKey(PARAMS_ID)) {
            LootContextParamSets.REGISTRY.put(PARAMS_ID, PARAMS);
        }
    }

    public static ResourceKey<LootTable> lootTable(Item item) {
        return ResourceKey.create(Registries.LOOT_TABLE, BuiltInRegistries.ITEM.getKey(item).withPrefix("deconstruction/"));
    }

    public static boolean canDeconstruct(ItemStack stack) {
        return !stack.isEmpty() && previews.containsKey(stack.getItem());
    }

    /**
     * @return all the components the item can break into, for display
     */
    public static List<ItemStack> getPreview(ItemStack stack) {
        return stack.isEmpty() ? List.of() : previews.getOrDefault(stack.getItem(), List.of());
    }

    /**
     * @return every deconstructable item with the preview of its components
     */
    public static Map<Item, List<ItemStack>> getPreviews() {
        return previews;
    }

    /**
     * Sets the previews received from the server, or clears them with an empty map.
     */
    public static void setPreviews(Map<Item, List<ItemStack>> received) {
        previews = Collections.unmodifiableMap(new LinkedHashMap<>(received));
    }

    /**
     * Rolls the deconstruction loot table of the item. Server side only.
     */
    public static List<ItemStack> rollComponents(ServerLevel level, BlockPos pos, ItemStack toDestroy, @Nullable Entity entity) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(lootTable(toDestroy.getItem()));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, toDestroy.copyWithCount(1))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
                .create(PARAMS);
        return new ArrayList<>(table.getRandomItems(params));
    }

    private static Map<Item, List<ItemStack>> computePreviews(MinecraftServer server) {
        Map<Item, List<ItemStack>> result = new LinkedHashMap<>();
        ServerLevel level = server.overworld();
        for (ResourceLocation id : server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
            if (!id.getPath().startsWith("deconstruction/")) continue;
            ResourceLocation itemId = id.withPath(id.getPath().substring("deconstruction/".length()));
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) continue;

            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    .withParameter(LootContextParams.TOOL, new ItemStack(item))
                    .create(PARAMS);
            // the union of several rolls, with the highest count seen for each component
            List<ItemStack> preview = new ArrayList<>();
            for (int roll = 0; roll < PREVIEW_ROLLS; roll++) {
                List<ItemStack> rolled = new ArrayList<>();
                for (ItemStack stack : table.getRandomItems(params, roll)) {
                    merge(rolled, stack, true);
                }
                for (ItemStack stack : rolled) {
                    merge(preview, stack, false);
                }
            }
            if (!preview.isEmpty()) {
                result.put(item, List.copyOf(preview));
            }
        }
        return result;
    }

    private static void merge(List<ItemStack> stacks, ItemStack stack, boolean add) {
        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                existing.setCount(add ? existing.getCount() + stack.getCount() : Math.max(existing.getCount(), stack.getCount()));
                return;
            }
        }
        stacks.add(stack.copy());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        previews = Collections.unmodifiableMap(computePreviews(event.getServer()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            // the data packs were reloaded
            previews = Collections.unmodifiableMap(computePreviews(event.getPlayerList().getServer()));
        }
        DeconstructionSyncPayload payload = new DeconstructionSyncPayload(previews);
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        previews = Map.of();
    }
}
