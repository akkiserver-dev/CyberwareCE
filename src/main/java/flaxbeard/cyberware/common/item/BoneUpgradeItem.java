package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class BoneUpgradeItem extends CyberwareItem {
    public static final int MAX_STACK_SIZE_LACING = 5;
    private static final ResourceLocation HEALTH_MODIFIER = CyberwareAPI.id("bone_health_upgrade");

    private final Kind kind;

    public BoneUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public void onAdded(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.LACING) {
            AttributeHelper.applyPermanent(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, 4.0 * stack.getCount());
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.LACING) {
            AttributeHelper.remove(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        }
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return other.getItem() instanceof BoneUpgradeItem;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return kind == Kind.BATTERY ? CyberwareConstants.BONE_BATTERY_CAPACITY * stack.getCount() : 0;
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return switch (kind) {
            case LACING -> MAX_STACK_SIZE_LACING;
            case BATTERY -> 4;
            default -> 1;
        };
    }

    @SubscribeEvent
    public static void handleJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null) return;

        BoneUpgradeItem lacingItem = CyberwareItems.BONE_LACING.get();
        ItemStack lacing = data.getCyberware(lacingItem);
        if (!lacing.isEmpty()) {
            lacingItem.onAdded(entity, lacing);
        } else {
            lacingItem.onRemoved(entity, lacing);
        }
    }

    @SubscribeEvent
    public static void handleFallDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        if (data != null && data.isCyberwareInstalled(CyberwareItems.BONE_FLEX.get())) {
            event.setAmount(event.getAmount() * .3333F);
        }
    }

    public enum Kind {
        LACING,
        FLEX,
        BATTERY
    }
}
