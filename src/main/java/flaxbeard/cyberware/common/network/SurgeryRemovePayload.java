package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Marks (or unmarks) installed cyberware for removal in a Robosurgeon.
 */
public record SurgeryRemovePayload(BlockPos pos, int slot, boolean discard) implements CustomPacketPayload {
    public static final Type<SurgeryRemovePayload> TYPE = new Type<>(CyberwareAPI.id("surgery_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SurgeryRemovePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SurgeryRemovePayload::pos, ByteBufCodecs.VAR_INT, SurgeryRemovePayload::slot, ByteBufCodecs.BOOL, SurgeryRemovePayload::discard, SurgeryRemovePayload::new);

    @Override
    public Type<SurgeryRemovePayload> type() {
        return TYPE;
    }

    public static void handle(SurgeryRemovePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleSurgeryRemove(payload, context));
    }
}
