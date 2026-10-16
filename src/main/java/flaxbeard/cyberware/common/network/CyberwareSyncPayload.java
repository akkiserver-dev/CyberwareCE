package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sends the cyberware data of an entity to clients.
 */
public record CyberwareSyncPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<CyberwareSyncPayload> TYPE = new Type<>(CyberwareAPI.id("cyberware_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareSyncPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, CyberwareSyncPayload::entityId, ByteBufCodecs.COMPOUND_TAG, CyberwareSyncPayload::data, CyberwareSyncPayload::new);

    @Override
    public Type<CyberwareSyncPayload> type() {
        return TYPE;
    }

    public static void handle(CyberwareSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleCyberwareSync(payload));
    }
}
