package flaxbeard.cyberware.common;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.item.CyberwareData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import static flaxbeard.cyberware.common.CyberwareRegistry.*;

public class CyberwareComponents {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CyberwareData>> CYBERWARE_DATA =
            COMPONENTS.register("cyberware_data", () ->
                    DataComponentType.<CyberwareData>builder()
                            .persistent(CyberwareData.CODEC)
                            .build());
}