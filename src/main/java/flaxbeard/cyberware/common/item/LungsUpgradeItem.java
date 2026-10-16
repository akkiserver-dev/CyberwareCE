package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LungsUpgradeItem extends CyberwareItem {
    public static final int MAX_STORED_AIR = 900;
    private static final String AIR_KEY = "air";

    private static final Set<LivingEntity> OXYGEN_POWERED = Collections.newSetFromMap(new WeakHashMap<>());

    private final Kind kind;

    public LungsUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return kind == Kind.HYPEROXYGENATION_BOOST ? 3 : 1;
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return kind == Kind.HYPEROXYGENATION_BOOST ? CyberwareConstants.HYPEROXYGENATION_CONSUMPTION * stack.getCount() : 0;
    }

    /**
     * @return the air stored in a Compressed Oxygen Implant
     */
    public static int getStoredAir(ItemStack stack) {
        var data = CyberwareAPI.getFunctionData(stack);
        return data.contains(AIR_KEY) ? data.getInt(AIR_KEY) : MAX_STORED_AIR;
    }

    private static void setStoredAir(ItemStack stack, int air) {
        CyberwareAPI.updateFunctionData(stack, tag -> tag.putInt(AIR_KEY, air));
    }

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        int maxAir = entity.getMaxAirSupply();

        ItemStack compressedOxygen = data.getCyberware(CyberwareItems.COMPRESSED_OXYGEN.get());
        if (!compressedOxygen.isEmpty()) {
            int air = getStoredAir(compressedOxygen);
            if (entity.getAirSupply() < maxAir && air > 0) {
                int toAdd = Math.min(maxAir - entity.getAirSupply(), air);
                entity.setAirSupply(entity.getAirSupply() + toAdd);
                setStoredAir(compressedOxygen, air - toAdd);
            } else if (entity.getAirSupply() == maxAir && air < MAX_STORED_AIR) {
                setStoredAir(compressedOxygen, air + 1);
            }
        }

        ItemStack boost = data.getCyberware(CyberwareItems.HYPEROXYGENATION_BOOST.get());
        if (!boost.isEmpty()
                && (entity.isSprinting() || entity instanceof Monster)
                && !entity.isInWater()
                && entity.onGround()) {
            boolean wasPowered = OXYGEN_POWERED.contains(entity);
            boolean isPowered = entity.tickCount % 20 == 0
                    ? data.usePower(boost, CyberwareItems.HYPEROXYGENATION_BOOST.get().getPowerConsumption(boost))
                    : wasPowered;
            if (isPowered) {
                Vec3 motion = entity.getDeltaMovement();
                if (Math.abs(entity.xxa) + Math.abs(entity.zza) > 0.0F && Math.abs(motion.x) + Math.abs(motion.z) > 0.0F) {
                    // increase maximum horizontal motion
                    float boostAmount = 0.21F * boost.getCount();
                    entity.moveRelative(0.075F, new Vec3(entity.xxa * boostAmount, 0.0F, entity.zza * boostAmount));
                }
                OXYGEN_POWERED.add(entity);
            } else if (entity.tickCount % 20 == 0) {
                OXYGEN_POWERED.remove(entity);
            }
        }
    }

    public enum Kind {
        COMPRESSED_OXYGEN,
        HYPEROXYGENATION_BOOST
    }
}
