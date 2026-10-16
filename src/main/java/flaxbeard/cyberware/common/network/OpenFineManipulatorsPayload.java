package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Opens the crafting grid of the Crafting Hands.
 */
public record OpenFineManipulatorsPayload() implements CustomPacketPayload {
    public static final Type<OpenFineManipulatorsPayload> TYPE = new Type<>(CyberwareAPI.id("open_fine_manipulators"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFineManipulatorsPayload> STREAM_CODEC = StreamCodec.unit(new OpenFineManipulatorsPayload());

    @Override
    public Type<OpenFineManipulatorsPayload> type() {
        return TYPE;
    }

    public static void handle(OpenFineManipulatorsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleOpenFineManipulators(context));
    }
}
