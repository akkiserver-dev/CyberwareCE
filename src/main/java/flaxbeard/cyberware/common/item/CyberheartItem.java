package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class CyberheartItem extends CyberwareItem {
    public CyberheartItem(Properties properties, Settings settings) {
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
        return CyberwareConstants.HEART_CONSUMPTION;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;

        CyberwareUserData data = event.getUserData();
        ItemStack cyberheart = data.getCyberware(CyberwareItems.CYBERHEART.get());
        if (cyberheart.isEmpty()) return;

        if (!data.usePower(cyberheart, CyberwareConstants.HEART_CONSUMPTION)) {
            entity.hurt(CyberwareDamageTypes.source(entity.level(), CyberwareDamageTypes.HEARTLESS), Float.MAX_VALUE);
        } else if (entity.hasEffect(MobEffects.WEAKNESS)) {
            entity.removeEffect(MobEffects.WEAKNESS);
        }
    }
}
