package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sends the config values the client displays (tolerance, machine chances, scan time) from the server.
 */
public record SyncConfigPayload(CyberwareConfig.Synced values) implements CustomPacketPayload {
    public static final Type<SyncConfigPayload> TYPE = new Type<>(CyberwareAPI.id("sync_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = CyberwareConfig.Synced.STREAM_CODEC
            .map(SyncConfigPayload::new, SyncConfigPayload::values).cast();

    @Override
    public Type<SyncConfigPayload> type() {
        return TYPE;
    }

    public static void handle(SyncConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CyberwareConfig.setFromServer(payload.values()));
    }
}
