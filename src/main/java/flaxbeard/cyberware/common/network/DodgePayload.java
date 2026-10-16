package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Shows the dodge effect of the Threat Matrix Calculator.
 */
public record DodgePayload(int entityId) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(CyberwareAPI.id("dodge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DodgePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DodgePayload::entityId, DodgePayload::new);

    @Override
    public Type<DodgePayload> type() {
        return TYPE;
    }

    public static void handle(DodgePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleDodge(payload));
    }
}
