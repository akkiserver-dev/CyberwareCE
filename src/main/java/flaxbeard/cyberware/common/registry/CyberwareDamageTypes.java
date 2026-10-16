package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Keys of the data driven damage types in {@code data/cyberware/damage_type}.
 */
public final class CyberwareDamageTypes {
    public static final ResourceKey<DamageType> BRAINLESS = key("brainless");
    public static final ResourceKey<DamageType> HEARTLESS = key("heartless");
    public static final ResourceKey<DamageType> SURGERY = key("surgery");
    public static final ResourceKey<DamageType> SPINELESS = key("spineless");
    public static final ResourceKey<DamageType> NO_MUSCLES = key("nomuscles");
    public static final ResourceKey<DamageType> NO_ESSENCE = key("noessence");
    public static final ResourceKey<DamageType> LOW_ESSENCE = key("lowessence");

    private CyberwareDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CyberwareAPI.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return level.damageSources().source(type);
    }

    /**
     * @return whether dying from this source means the body is broken, so cyberware drops even with keepCyberware
     */
    public static boolean isMissingEssential(DamageSource source) {
        return source.is(NO_ESSENCE) || source.is(HEARTLESS) || source.is(BRAINLESS) || source.is(NO_MUSCLES) || source.is(SPINELESS);
    }
}
