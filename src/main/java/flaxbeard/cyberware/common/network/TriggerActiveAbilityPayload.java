package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Uses an active item from the radial menu or a hotkey.
 */
public record TriggerActiveAbilityPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<TriggerActiveAbilityPayload> TYPE = new Type<>(CyberwareAPI.id("trigger_active_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerActiveAbilityPayload> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, TriggerActiveAbilityPayload::stack, TriggerActiveAbilityPayload::new);

    @Override
    public Type<TriggerActiveAbilityPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerActiveAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ServerPayloadHandler.handleTriggerActiveAbility(payload, context));
    }
}
