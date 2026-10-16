package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Wired Reflexes switched the held weapon of a player, optionally facing the attacker.
 */
public record SwitchHeldItemPayload(int slot, int entityId, int attackerId) implements CustomPacketPayload {
    public static final Type<SwitchHeldItemPayload> TYPE = new Type<>(CyberwareAPI.id("switch_held_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchHeldItemPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SwitchHeldItemPayload::slot, ByteBufCodecs.VAR_INT, SwitchHeldItemPayload::entityId, ByteBufCodecs.INT, SwitchHeldItemPayload::attackerId, SwitchHeldItemPayload::new);

    @Override
    public Type<SwitchHeldItemPayload> type() {
        return TYPE;
    }

    public static void handle(SwitchHeldItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleSwitchHeldItem(payload));
    }
}
