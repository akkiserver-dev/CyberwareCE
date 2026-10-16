package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Plays the effects of an Engineering Table destroying an item.
 */
public record EngineeringSmashPayload(BlockPos pos, ItemStack stack) implements CustomPacketPayload {
    public static final Type<EngineeringSmashPayload> TYPE = new Type<>(CyberwareAPI.id("engineering_smash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EngineeringSmashPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EngineeringSmashPayload::pos, ItemStack.OPTIONAL_STREAM_CODEC, EngineeringSmashPayload::stack, EngineeringSmashPayload::new);

    @Override
    public Type<EngineeringSmashPayload> type() {
        return TYPE;
    }

    public static void handle(EngineeringSmashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleEngineeringSmash(payload));
    }
}
