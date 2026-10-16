package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.recipe.Deconstruction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the deconstructable items and the components they break into, read from the server's loot tables.
 */
public record DeconstructionSyncPayload(Map<Item, List<ItemStack>> previews) implements CustomPacketPayload {
    public static final Type<DeconstructionSyncPayload> TYPE = new Type<>(CyberwareAPI.id("deconstruction_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeconstructionSyncPayload> STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, Item, List<ItemStack>, Map<Item, List<ItemStack>>>map(LinkedHashMap::new,
                    ByteBufCodecs.registry(Registries.ITEM), ItemStack.LIST_STREAM_CODEC)
            .map(DeconstructionSyncPayload::new, DeconstructionSyncPayload::previews);

    @Override
    public Type<DeconstructionSyncPayload> type() {
        return TYPE;
    }

    public static void handle(DeconstructionSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Deconstruction.setPreviews(payload.previews()));
    }
}
