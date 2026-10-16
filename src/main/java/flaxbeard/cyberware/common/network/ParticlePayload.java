package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Spawns the particles of the heart upgrades.
 */
public record ParticlePayload(int effect, float x, float y, float z) implements CustomPacketPayload {
    public static final int HEAL = 0;
    public static final int DEFIBRILLATOR = 1;

    public static final Type<ParticlePayload> TYPE = new Type<>(CyberwareAPI.id("particle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParticlePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ParticlePayload::effect, ByteBufCodecs.FLOAT, ParticlePayload::x, ByteBufCodecs.FLOAT, ParticlePayload::y, ByteBufCodecs.FLOAT, ParticlePayload::z, ParticlePayload::new);

    @Override
    public Type<ParticlePayload> type() {
        return TYPE;
    }

    public static void handle(ParticlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handleParticles(payload));
    }
}
