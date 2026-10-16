package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class FootUpgradeItem extends CyberwareItem implements MenuItem {
    private static final ResourceLocation SPURS_MODIFIER = CyberwareAPI.id("spurs_speed");
    private static final ResourceLocation WHEELS_MODIFIER = CyberwareAPI.id("wheels_step_height");
    /** Same speed bonus as the Speed VI effect the original applied to the horse every tick. */
    private static final AttributeModifier SPURS_SPEED = new AttributeModifier(SPURS_MODIFIER, 1.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final Set<LivingEntity> AQUA_POWERED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LivingEntity> WHEELS_POWERED = Collections.newSetFromMap(new WeakHashMap<>());

    private final Kind kind;

    public FootUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        if (kind != Kind.AQUA) return List.of();
        return List.of(List.of(
                new ItemStack(CyberwareItems.CYBERLEG_LEFT.get()),
                new ItemStack(CyberwareItems.CYBERLEG_RIGHT.get())));
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return switch (kind) {
            case AQUA -> CyberwareConstants.AQUA_CONSUMPTION;
            case WHEELS -> CyberwareConstants.WHEEL_CONSUMPTION;
            default -> 0;
        };
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.WHEELS) {
            AttributeHelper.remove(entity, Attributes.STEP_HEIGHT, WHEELS_MODIFIER);
            WHEELS_POWERED.remove(entity);
        }
    }

    // ---- Spurs ----

    @SubscribeEvent
    public static void handleHorseMove(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide()) return;

        boolean hasSpurs = false;
        for (Entity passenger : horse.getPassengers()) {
            CyberwareUserData data = CyberwareAPI.getUserData(passenger);
            if (data != null && data.isCyberwareInstalled(CyberwareItems.SPURS.get())) {
                hasSpurs = true;
                break;
            }
        }

        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (hasSpurs && !speed.hasModifier(SPURS_MODIFIER)) {
            speed.addTransientModifier(SPURS_SPEED);
        } else if (!hasSpurs && speed.hasModifier(SPURS_MODIFIER)) {
            speed.removeModifier(SPURS_MODIFIER);
        }
    }

    // ---- Aqua & Wheels ----

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        boolean isSecond = entity.tickCount % 20 == 0;

        if (!entity.onGround() && entity.isInWater()) {
            FootUpgradeItem aquaItem = CyberwareItems.AQUA.get();
            ItemStack aqua = data.getCyberware(aquaItem);
            if (!aqua.isEmpty()) {
                boolean wasPowered = AQUA_POWERED.contains(entity);
                boolean isPowered = isSecond ? data.usePower(aqua, aquaItem.getPowerConsumption(aqua)) : wasPowered;
                if (isPowered) {
                    Vec3 motion = entity.getDeltaMovement();
                    if (Math.abs(entity.xxa) + Math.abs(entity.zza) > 0.0F && Math.abs(motion.x) + Math.abs(motion.z) > 0.0F) {
                        // increase maximum horizontal motion
                        float boost = CyberlimbItem.countLegs(data) * 0.4F;
                        entity.moveRelative(0.075F, new Vec3(entity.xxa * boost, 0.26F, entity.zza * boost));
                    }
                    AQUA_POWERED.add(entity);
                } else if (isSecond) {
                    AQUA_POWERED.remove(entity);
                }
            }
        } else if (isSecond) {
            AQUA_POWERED.remove(entity);
        }

        FootUpgradeItem wheelsItem = CyberwareItems.WHEELS.get();
        ItemStack wheels = data.getCyberware(wheelsItem);
        if (!wheels.isEmpty()) {
            boolean wasPowered = WHEELS_POWERED.contains(entity);
            boolean isPowered = EnableDisableHelper.isEnabled(wheels)
                    && (isSecond ? data.usePower(wheels, wheelsItem.getPowerConsumption(wheels)) : wasPowered);
            if (isPowered) {
                // raise the step height to a full block
                double bonus = Math.max(0.0, 1.0 - entity.getAttributeBaseValue(Attributes.STEP_HEIGHT));
                AttributeHelper.applyTransient(entity, Attributes.STEP_HEIGHT, WHEELS_MODIFIER, bonus);
                WHEELS_POWERED.add(entity);
            } else {
                wheelsItem.onRemoved(entity, wheels);
            }
        } else if (WHEELS_POWERED.contains(entity)) {
            wheelsItem.onRemoved(entity, wheels);
        }
    }

    // ---- menu ----

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind == Kind.WHEELS;
    }

    @Override
    public void use(Entity entity, ItemStack stack) {
        EnableDisableHelper.toggle(stack);
    }

    @Override
    public String getMenuLabel(ItemStack stack) {
        return EnableDisableHelper.getMenuLabel(stack);
    }

    @Override
    public float[] getColor(ItemStack stack) {
        return EnableDisableHelper.getMenuColor(stack);
    }

    public enum Kind {
        SPURS,
        AQUA,
        WHEELS
    }
}
