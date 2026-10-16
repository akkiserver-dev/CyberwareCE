package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.network.CyberlegFallPayload;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CyberlimbItem extends CyberwareItem implements SidedLimb {
    private static final String ACTIVE_KEY = "active";

    private final Side side;

    public CyberlimbItem(Properties properties, Settings settings, Side side) {
        super(properties, settings);
        this.side = side;
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        Cyberware otherWare = CyberwareAPI.getCyberware(other);
        return otherWare instanceof SidedLimb sidedLimb
                && otherWare.isEssential(other)
                && sidedLimb.getSide(other) == side;
    }

    @Override
    public Side getSide(ItemStack stack) {
        return side;
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return CyberwareConstants.LIMB_CONSUMPTION;
    }

    /**
     * @return whether the installed cyberlimb had power during the last second
     */
    public static boolean isPowered(ItemStack stack) {
        var data = CyberwareAPI.getFunctionData(stack);
        return !data.contains(ACTIVE_KEY) || data.getBoolean(ACTIVE_KEY);
    }

    /**
     * @return the number of installed cyberlegs
     */
    public static int countLegs(CyberwareUserData data) {
        int legs = 0;
        if (data.isCyberwareInstalled(CyberwareItems.CYBERLEG_LEFT.get())) legs++;
        if (data.isCyberwareInstalled(CyberwareItems.CYBERLEG_RIGHT.get())) legs++;
        return legs;
    }

    public static boolean hasBothLegs(CyberwareUserData data) {
        return countLegs(data) == 2;
    }

    /**
     * Players landing on two cyberlegs make a metallic sound instead of the normal hurt sound.
     * The hurt sound is played by the client, so the client is told about the fall first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handleFallDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.isCanceled() || entity.level().isClientSide() || !(entity instanceof Player) || !event.getSource().is(DamageTypeTags.IS_FALL)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data != null && hasBothLegs(data)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new CyberlegFallPayload(entity.getId()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;

        CyberwareUserData data = event.getUserData();
        for (CyberlimbItem limb : List.of(CyberwareItems.CYBERARM_LEFT.get(), CyberwareItems.CYBERARM_RIGHT.get(),
                CyberwareItems.CYBERLEG_LEFT.get(), CyberwareItems.CYBERLEG_RIGHT.get())) {
            ItemStack installed = data.getCyberware(limb);
            if (!installed.isEmpty()) {
                boolean powered = data.usePower(installed, limb.getPowerConsumption(installed));
                if (powered != isPowered(installed)) {
                    CyberwareAPI.updateFunctionData(installed, tag -> tag.putBoolean(ACTIVE_KEY, powered));
                }
            }
        }
    }
}
