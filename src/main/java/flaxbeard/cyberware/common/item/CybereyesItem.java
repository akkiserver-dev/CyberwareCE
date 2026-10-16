package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class CybereyesItem extends CyberwareItem {
    /** Entities whose cybereyes ran out of power; kept per side so the client can render the black overlay. */
    private static final Set<LivingEntity> BLIND = Collections.newSetFromMap(new WeakHashMap<>());

    public CybereyesItem(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return CyberwareAPI.getCyberware(other).isEssential(other);
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return CyberwareConstants.CYBEREYES_CONSUMPTION;
    }

    /**
     * @return whether the cybereyes of this entity are unpowered
     */
    public static boolean isBlind(LivingEntity entity) {
        return BLIND.contains(entity);
    }

    @SubscribeEvent
    public static void handleBlindnessImmunity(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(MobEffects.BLINDNESS)) return;
        if (event.getUserData().isCyberwareInstalled(CyberwareItems.CYBEREYES.get())) {
            entity.removeEffect(MobEffects.BLINDNESS);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleMissingEssentials(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;

        CyberwareUserData data = event.getUserData();
        ItemStack cybereyes = data.getCyberware(CyberwareItems.CYBEREYES.get());
        if (!cybereyes.isEmpty()) {
            boolean powered = data.usePower(cybereyes, ((CyberwareItem) cybereyes.getItem()).getPowerConsumption(cybereyes));
            if (powered) {
                BLIND.remove(entity);
            } else {
                BLIND.add(entity);
            }
        } else {
            BLIND.remove(entity);
        }
        // note: the original also applied blindness here, which handleBlindnessImmunity removed again right away;
        // unpowered cybereyes only darken the screen (see the client overlay)
    }
}
