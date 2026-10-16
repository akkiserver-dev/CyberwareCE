package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Saves the HUD layout of a player.
 */
public record SyncHudDataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncHudDataPayload> TYPE = new Type<>(CyberwareAPI.id("sync_hud_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHudDataPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, SyncHudDataPayload::data, SyncHudDataPayload::new);

    @Override
    public Type<SyncHudDataPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHudDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleSyncHudData(payload, context));
    }
}
