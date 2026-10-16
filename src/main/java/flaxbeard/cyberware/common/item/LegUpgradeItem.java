package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

public class LegUpgradeItem extends CyberwareItem {
    private final Kind kind;

    public LegUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        return List.of(List.of(
                new ItemStack(CyberwareItems.CYBERLEG_LEFT.get()),
                new ItemStack(CyberwareItems.CYBERLEG_RIGHT.get())));
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return kind == Kind.JUMP_BOOST ? CyberwareConstants.JUMPBOOST_CONSUMPTION : 0;
    }

    @SubscribeEvent
    public static void handleJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null) return;

        LegUpgradeItem jumpBoostItem = CyberwareItems.JUMP_BOOST.get();
        ItemStack jumpBoost = data.getCyberware(jumpBoostItem);
        if (jumpBoost.isEmpty() || !data.usePower(jumpBoost, jumpBoostItem.getPowerConsumption(jumpBoost))) return;

        int legs = CyberlimbItem.countLegs(data);
        Vec3 motion = entity.getDeltaMovement();
        if (entity.isShiftKeyDown()) {
            // leap forwards
            Vec3 look = entity.getViewVector(0.5F);
            double total = Math.abs(look.z + look.x);
            double y = Math.max(look.y, total);
            entity.setDeltaMovement(motion.add(look.x * legs, legs * y / 3F, look.z * legs));
        } else {
            entity.setDeltaMovement(motion.add(0, legs * (0.2750000059604645D / 2D), 0));
        }
    }

    @SubscribeEvent
    public static void handleFallDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        if (data != null
                && data.isCyberwareInstalled(CyberwareItems.FALL_DAMAGE_UPGRADE.get())
                && CyberlimbItem.hasBothLegs(data)) {
            event.setCanceled(true);
        }
    }

    public enum Kind {
        JUMP_BOOST,
        FALL_DAMAGE
    }
}
