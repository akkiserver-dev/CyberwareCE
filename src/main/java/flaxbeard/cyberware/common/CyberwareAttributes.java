package flaxbeard.cyberware.common;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;

import static flaxbeard.cyberware.common.CyberwareRegistry.*;

public class CyberwareAttributes {
    /**
     * Maximum Tolerance, per-player
     */
    public static final DeferredHolder<Attribute, Attribute> TOLERANCE = ATTRIBUTES.register("tolerance",
            () -> new RangedAttribute(
                    "cyberware.tolerance",
                    CyberwareConfig.ESSENCE.get(),
                    0.0,
                    Double.MAX_VALUE
            ).setSyncable(true));
}
