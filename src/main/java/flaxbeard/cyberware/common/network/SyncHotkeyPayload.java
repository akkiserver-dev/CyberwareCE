package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Assigns a hotkey to an active item; {@link #REMOVE} as key removes it.
 */
public record SyncHotkeyPayload(int selectedPart, int key) implements CustomPacketPayload {
    public static final int REMOVE = Integer.MAX_VALUE;

    public static final Type<SyncHotkeyPayload> TYPE = new Type<>(CyberwareAPI.id("sync_hotkey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHotkeyPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncHotkeyPayload::selectedPart, ByteBufCodecs.INT, SyncHotkeyPayload::key, SyncHotkeyPayload::new);

    @Override
    public Type<SyncHotkeyPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHotkeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleSyncHotkey(payload, context));
    }
}
