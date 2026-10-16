package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A player with two cyberlegs is about to take fall damage, so their hurt sound becomes metallic.
 */
public record CyberlegFallPayload(int entityId) implements CustomPacketPayload {
    public static final Type<CyberlegFallPayload> TYPE = new Type<>(CyberwareAPI.id("cyberleg_fall"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CyberlegFallPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, CyberlegFallPayload::entityId, CyberlegFallPayload::new);

    @Override
    public Type<CyberlegFallPayload> type() {
        return TYPE;
    }

    public static void handle(CyberlegFallPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleCyberlegFall(payload));
    }
}
