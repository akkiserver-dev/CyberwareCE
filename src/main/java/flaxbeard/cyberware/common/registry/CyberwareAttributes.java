package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, CyberwareMod.MOD_ID);

    /**
     * Maximum tolerance (essence) of an entity. The base value follows the {@code maxEssence} config option.
     */
    public static final DeferredHolder<Attribute, Attribute> TOLERANCE = ATTRIBUTES.register("tolerance",
            () -> new RangedAttribute("attribute.cyberware.tolerance", 100.0, 0.0, Double.MAX_VALUE).setSyncable(true));

    private CyberwareAttributes() {
    }
}
