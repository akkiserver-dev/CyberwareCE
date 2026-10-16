package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.ArmorClass;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.common.registry.CyberwareEffects;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class SkinUpgradeItem extends CyberwareItem {
    private static final Map<LivingEntity, Collection<LastEffect>> LAST_EFFECTS = new WeakHashMap<>();

    private final Kind kind;

    public SkinUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return kind == Kind.IMMUNOSUPPRESSANT ? CyberwareConstants.IMMUNO_CONSUMPTION : 0;
    }

    @Override
    public int getPowerProduction(ItemStack stack) {
        return kind == Kind.SOLARSKIN ? CyberwareConstants.SOLAR_PRODUCTION : 0;
    }

    // ---- Solarskin ----

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;

        float lightFactor = getLightFactor(entity);
        if (lightFactor <= 0.0F) return;

        CyberwareUserData data = event.getUserData();
        ItemStack solarskin = data.getCyberware(CyberwareItems.SOLARSKIN.get());
        if (!solarskin.isEmpty()) {
            int power = Math.max(0, Math.round(CyberwareItems.SOLARSKIN.get().getPowerProduction(solarskin) * lightFactor));
            data.addPower(power, solarskin);
        }
    }

    private static float getLightFactor(LivingEntity entity) {
        Level level = entity.level();
        // the world must have a sun
        if (!level.dimensionType().hasSkyLight()) return 0.0F;
        // the current position must see the sun
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ());
        if (!level.canSeeSky(pos)) return 0.0F;
        // the sun must not be shaded
        if (level.getBrightness(LightLayer.SKY, pos) < 15) return 0.0F;

        // it must be day time (see the vanilla daylight detector)
        float sunAngle = level.getSunAngle(1.0F);
        float offset = sunAngle < (float) Math.PI ? 0.0F : (float) Math.PI * 2.0F;
        sunAngle += (offset - sunAngle) * 0.2F;
        return Mth.cos(sunAngle);
    }

    // ---- Immunosuppressant ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleImmunosuppressant(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();

        ItemStack immuno = data.getCyberware(CyberwareItems.IMMUNOSUPPRESSANT.get());
        if (immuno.isEmpty()) {
            if (entity.tickCount % 20 == 0) {
                LAST_EFFECTS.remove(entity);
            }
            return;
        }

        // consume power every second, apply damage every 5 seconds unless powered or Neuropozyne is active
        if (entity instanceof Player && entity.tickCount % 20 == 0) {
            boolean powered = data.usePower(immuno, CyberwareConstants.IMMUNO_CONSUMPTION);
            if (!powered && entity.tickCount % 100 == 0 && !entity.hasEffect(CyberwareEffects.NEUROPOZYNE)) {
                entity.hurt(CyberwareDamageTypes.source(entity.level(), CyberwareDamageTypes.LOW_ESSENCE), 2.0F);
            }
        }

        // increase poison and hunger duration
        if (entity.level().isClientSide()) return;
        Collection<LastEffect> lastEffects = LAST_EFFECTS.get(entity);
        if (lastEffects == null) {
            // first time seeing this entity: remember the current poison and hunger effects
            lastEffects = new ArrayList<>(2);
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                if (isAugmentedEffect(effect.getEffect())) {
                    lastEffects.add(new LastEffect(effect));
                }
            }
            LAST_EFFECTS.put(entity, lastEffects);
            return;
        }

        lastEffects.forEach(lastEffect -> lastEffect.found = false);
        List<MobEffectInstance> toAdd = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!isAugmentedEffect(effect.getEffect())) continue;

            boolean found = false;
            for (LastEffect lastEffect : lastEffects) {
                if (lastEffect.effect.equals(effect.getEffect())
                        && lastEffect.amplifier == effect.getAmplifier()
                        && lastEffect.duration >= effect.getDuration()) {
                    lastEffect.found = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                toAdd.add(new MobEffectInstance(effect.getEffect(), (int) (effect.getDuration() * 1.8F),
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
            }
        }
        for (MobEffectInstance augmented : toAdd) {
            entity.addEffect(augmented);
            LastEffect lastEffect = new LastEffect(augmented);
            lastEffect.found = true;
            lastEffects.add(lastEffect);
        }

        lastEffects.forEach(lastEffect -> lastEffect.duration--);
        lastEffects.removeIf(lastEffect -> lastEffect.duration < 0 || !lastEffect.found);
    }

    private static boolean isAugmentedEffect(Holder<MobEffect> effect) {
        return effect.equals(MobEffects.POISON) || effect.equals(MobEffects.HUNGER);
    }

    // ---- Subdermal Spikes ----

    @SubscribeEvent
    public static void handleHurt(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.SUBDERMAL_SPIKES.get())) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || !source.isDirect()) return;
        if (ArmorClass.get(entity) == ArmorClass.HEAVY) return;

        RandomSource random = entity.getRandom();
        // same odds and damage as thorns III
        if (random.nextFloat() < 0.15F * 3) {
            attacker.hurt(entity.damageSources().thorns(entity), 1 + random.nextInt(4));
        }
    }

    private static class LastEffect {
        private final Holder<MobEffect> effect;
        private final int amplifier;
        private int duration;
        private boolean found;

        private LastEffect(MobEffectInstance instance) {
            effect = instance.getEffect();
            amplifier = instance.getAmplifier();
            duration = instance.getDuration();
        }
    }

    public enum Kind {
        SOLARSKIN,
        SUBDERMAL_SPIKES,
        SYNTHETIC_SKIN,
        IMMUNOSUPPRESSANT
    }
}
