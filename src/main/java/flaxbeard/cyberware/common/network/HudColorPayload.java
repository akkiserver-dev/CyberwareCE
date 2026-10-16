package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Saves the HUD color of a player.
 */
public record HudColorPayload(int color) implements CustomPacketPayload {
    public static final Type<HudColorPayload> TYPE = new Type<>(CyberwareAPI.id("hud_color"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HudColorPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, HudColorPayload::color, HudColorPayload::new);

    @Override
    public Type<HudColorPayload> type() {
        return TYPE;
    }

    public static void handle(HudColorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleHudColor(payload, context));
    }
}
