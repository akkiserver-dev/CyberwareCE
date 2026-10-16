package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Switches the archive or component box shown by the open Engineering Table.
 */
public record EngineeringSwitchPayload(boolean forward, boolean isComponentBox) implements CustomPacketPayload {
    public static final Type<EngineeringSwitchPayload> TYPE = new Type<>(CyberwareAPI.id("engineering_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EngineeringSwitchPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, EngineeringSwitchPayload::forward, ByteBufCodecs.BOOL, EngineeringSwitchPayload::isComponentBox, EngineeringSwitchPayload::new);

    @Override
    public Type<EngineeringSwitchPayload> type() {
        return TYPE;
    }

    public static void handle(EngineeringSwitchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleEngineeringSwitch(payload, context));
    }
}
