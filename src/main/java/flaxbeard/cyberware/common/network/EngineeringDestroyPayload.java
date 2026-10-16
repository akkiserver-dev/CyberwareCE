package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The destroy button of an Engineering Table was pressed.
 */
public record EngineeringDestroyPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<EngineeringDestroyPayload> TYPE = new Type<>(CyberwareAPI.id("engineering_destroy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EngineeringDestroyPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EngineeringDestroyPayload::pos, EngineeringDestroyPayload::new);

    @Override
    public Type<EngineeringDestroyPayload> type() {
        return TYPE;
    }

    public static void handle(EngineeringDestroyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleEngineeringDestroy(payload, context));
    }
}
