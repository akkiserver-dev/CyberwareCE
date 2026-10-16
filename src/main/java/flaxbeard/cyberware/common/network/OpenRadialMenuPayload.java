package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The player opened the radial menu, so the prompt to open it is no longer shown.
 */
public record OpenRadialMenuPayload() implements CustomPacketPayload {
    public static final Type<OpenRadialMenuPayload> TYPE = new Type<>(CyberwareAPI.id("open_radial_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRadialMenuPayload> STREAM_CODEC = StreamCodec.unit(new OpenRadialMenuPayload());

    @Override
    public Type<OpenRadialMenuPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRadialMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleOpenRadialMenu(context));
    }
}
