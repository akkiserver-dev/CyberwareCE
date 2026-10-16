package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.effect.CyberwareMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CyberwareMod.MOD_ID);

    /** Suppresses cyberware rejection. */
    public static final DeferredHolder<MobEffect, MobEffect> NEUROPOZYNE = EFFECTS.register("neuropozyne",
            () -> new CyberwareMobEffect(MobEffectCategory.BENEFICIAL, 0x47453d, 0));

    /** Shown while the body rejects cyberware because of low essence. */
    public static final DeferredHolder<MobEffect, MobEffect> REJECTION = EFFECTS.register("rejection",
            () -> new CyberwareMobEffect(MobEffectCategory.HARMFUL, 0xFF0000, 1));

    private CyberwareEffects() {
    }
}
