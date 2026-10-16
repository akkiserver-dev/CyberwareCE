package flaxbeard.cyberware.common.registry;

import com.mojang.serialization.Codec;
import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.item.Quality;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CyberwareMod.MOD_ID);

    /**
     * Functional data of installed cyberware (stored power, air, toggles, hotkeys...), cleared when it is removed.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> FUNCTION_DATA = COMPONENTS.registerComponentType("function_data",
            builder -> builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Quality>> QUALITY = COMPONENTS.registerComponentType("quality",
            builder -> builder.persistent(Quality.CODEC).networkSynchronized(Quality.STREAM_CODEC));

    /** The item a blueprint was made from. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> BLUEPRINT_ITEM = COMPONENTS.registerComponentType("blueprint_item",
            builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    /** The experience stored in an Experience Capsule. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXPERIENCE = COMPONENTS.registerComponentType("experience",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private CyberwareDataComponents() {
    }
}
