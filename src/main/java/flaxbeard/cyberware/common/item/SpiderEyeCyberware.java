package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.Quality;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a vanilla spider eye be installed as eyes, giving spider vision.
 * The post processing shader and crosshair offset are applied on the client.
 */
public class SpiderEyeCyberware implements Cyberware {
    /** Amplifier used to recognize the night vision effect given by spider eyes. */
    public static final int NIGHT_VISION_AMPLIFIER = -53;

    @Override
    public BodyRegion getBodyRegion(ItemStack stack) {
        return BodyRegion.EYES;
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        return List.of();
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return CyberwareAPI.getCyberware(other).isEssential(other);
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return true;
    }

    @Override
    public List<Component> getInfo(ItemStack stack) {
        List<Component> info = new ArrayList<>();
        for (String line : CyberwareItem.splitTranslation("cyberware.tooltip.spider_eye")) {
            info.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return info;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return 0;
    }

    @Override
    public Quality getQuality(ItemStack stack) {
        return null;
    }

    @Override
    public ItemStack setQuality(ItemStack stack, Quality quality) {
        return stack;
    }

    @Override
    public boolean canHoldQuality(ItemStack stack, Quality quality) {
        return false;
    }

    @Override
    public void onAdded(LivingEntity entity, ItemStack stack) {
        // no operation
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        // no operation
    }

    @Override
    public int getEssenceCost(ItemStack stack) {
        return 5;
    }

    @SubscribeEvent
    public static void handleSpiderNightVision(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;

        if (event.getUserData().isCyberwareInstalled(Items.SPIDER_EYE)) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, NIGHT_VISION_AMPLIFIER, true, false));
        } else {
            MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getAmplifier() == NIGHT_VISION_AMPLIFIER) {
                entity.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
