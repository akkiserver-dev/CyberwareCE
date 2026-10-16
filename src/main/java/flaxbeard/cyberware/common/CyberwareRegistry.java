package flaxbeard.cyberware.common;

import flaxbeard.cyberware.Cyberware;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class CyberwareRegistry {
    private static final List<DeferredRegister<?>> registers = new ArrayList<>();

    public static final DeferredRegister<Attribute> ATTRIBUTES = create(Registries.ATTRIBUTE);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = create(Registries.DATA_COMPONENT_TYPE);

    public static void init(IEventBus bus) {
        registers.forEach(bus::register);
    }

    private static <T> DeferredRegister<T> create(ResourceKey<Registry<T>> reg) {
        DeferredRegister<T> register = DeferredRegister.create(reg, Cyberware.MOD_ID);
        registers.add(register);
        return register;
    }
}
