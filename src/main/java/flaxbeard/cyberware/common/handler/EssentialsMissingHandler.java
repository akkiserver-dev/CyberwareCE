package flaxbeard.cyberware.common.handler;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.item.CyberlimbItem;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.common.registry.CyberwareEffects;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Consequences of missing essential body parts and low essence, and the per tick {@link CyberwareUpdateEvent}.
 */
public final class EssentialsMissingHandler {
    private static final ResourceLocation MISSING_LEG_SPEED = CyberwareAPI.id("missing_leg_speed");
    /** How much shorter a player without legs is. */
    private static final float LEGLESS_HEIGHT_REDUCTION = 10F / 16F;

    private static final Map<LivingEntity, Integer> LUNGS_TIMES = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> LEGLESS = new WeakHashMap<>();
    private static final Map<Player, Integer> HUNGER_BEFORE_EATING = new WeakHashMap<>();
    private static final Map<Player, Float> SATURATION_BEFORE_EATING = new WeakHashMap<>();

    private EssentialsMissingHandler() {
    }

    @SubscribeEvent
    public static void triggerCyberwareEvent(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            CyberwareUserData data = CyberwareAPI.getUserData(entity);
            if (data != null) {
                NeoForge.EVENT_BUS.post(new CyberwareUpdateEvent(entity, data));
            }
        }
    }

    /**
     * @return the number of legs that are missing or unpowered cyberlegs
     */
    private static int countMissingLegs(CyberwareUserData data, boolean includeUnpowered) {
        int missing = 0;
        if (!data.hasEssential(BodyRegion.LEG, SidedLimb.Side.LEFT)) missing++;
        if (!data.hasEssential(BodyRegion.LEG, SidedLimb.Side.RIGHT)) missing++;
        if (includeUnpowered) {
            ItemStack left = data.getCyberware(CyberwareItems.CYBERLEG_LEFT.get());
            if (!left.isEmpty() && !CyberlimbItem.isPowered(left)) missing++;
            ItemStack right = data.getCyberware(CyberwareItems.CYBERLEG_RIGHT.get());
            if (!right.isEmpty() && !CyberlimbItem.isPowered(right)) missing++;
        }
        return missing;
    }

    private static DamageSource source(LivingEntity entity, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        return CyberwareDamageTypes.source(entity.level(), type);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handleMissingEssentials(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        boolean isSecond = entity.tickCount % 20 == 0;
        boolean isServer = !entity.level().isClientSide();

        if (isSecond) {
            data.resetBuffer();
        }

        if (isServer && !data.hasEssential(BodyRegion.CRANIUM)) {
            entity.hurt(source(entity, CyberwareDamageTypes.BRAINLESS), Float.MAX_VALUE);
        }

        if (isServer && entity instanceof Player && isSecond) {
            int tolerance = data.getTolerance(entity);
            if (tolerance <= 0) {
                entity.hurt(source(entity, CyberwareDamageTypes.NO_ESSENCE), Float.MAX_VALUE);
            }
            if (tolerance < CyberwareConfig.CRITICAL_ESSENCE.get() && entity.tickCount % 100 == 0 && !entity.hasEffect(CyberwareEffects.NEUROPOZYNE)) {
                entity.addEffect(new MobEffectInstance(CyberwareEffects.REJECTION, 110, 0, true, false));
                entity.hurt(source(entity, CyberwareDamageTypes.LOW_ESSENCE), 2F);
            }
            if (!data.hasEssential(BodyRegion.EYES)) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40));
            }
        }

        // a player without visible legs is shorter
        if (entity instanceof Player) {
            boolean legless = countMissingLegs(data, false) == 2;
            if (LEGLESS.getOrDefault(entity, false) != legless) {
                LEGLESS.put(entity, legless);
                entity.refreshDimensions();
            }
        }

        // missing or unpowered legs make moving on the ground impossible
        int missingLegs = countMissingLegs(data, true);
        if (missingLegs >= 1 && entity.onGround()) {
            AttributeHelper.applyTransient(entity, Attributes.MOVEMENT_SPEED, MISSING_LEG_SPEED, -100.0);
        } else if (missingLegs >= 1 || isSecond) {
            AttributeHelper.remove(entity, Attributes.MOVEMENT_SPEED, MISSING_LEG_SPEED);
        }

        if (isServer) {
            if (!data.hasEssential(BodyRegion.HEART)) {
                entity.hurt(source(entity, CyberwareDamageTypes.HEARTLESS), Float.MAX_VALUE);
            }
            if (!data.hasEssential(BodyRegion.BONE)) {
                entity.hurt(source(entity, CyberwareDamageTypes.SPINELESS), Float.MAX_VALUE);
            }
            if (!data.hasEssential(BodyRegion.MUSCLE)) {
                entity.hurt(source(entity, CyberwareDamageTypes.NO_MUSCLES), Float.MAX_VALUE);
            }
        }

        if (!data.hasEssential(BodyRegion.LUNGS)) {
            int start = LUNGS_TIMES.computeIfAbsent(entity, e -> e.tickCount);
            if (entity.tickCount - start >= 20) {
                LUNGS_TIMES.put(entity, entity.tickCount);
                if (isServer) {
                    entity.hurt(entity.damageSources().drown(), 2F);
                }
            }
        } else if (isSecond) {
            LUNGS_TIMES.remove(entity);
        }
    }

    @SubscribeEvent
    public static void handleSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && LEGLESS.getOrDefault(player, false)
                && event.getPose() != Pose.SLEEPING && event.getPose() != Pose.DYING) {
            EntityDimensions size = event.getNewSize();
            event.setNewSize(EntityDimensions.scalable(size.width(), size.height() - LEGLESS_HEIGHT_REDUCTION)
                    .withEyeHeight(size.eyeHeight() - LEGLESS_HEIGHT_REDUCTION));
        }
    }

    @SubscribeEvent
    public static void handleJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data != null && countMissingLegs(data, true) == 2) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, 0.2F, motion.z);
        }
    }

    // ---- missing lower organs: food has no effect ----

    @SubscribeEvent
    public static void handleEatFoodTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (event.getItem().getUseAnimation() == UseAnim.EAT && data != null && !data.hasEssential(BodyRegion.LOWER_ORGANS)) {
            HUNGER_BEFORE_EATING.put(player, player.getFoodData().getFoodLevel());
            SATURATION_BEFORE_EATING.put(player, player.getFoodData().getSaturationLevel());
        } else {
            HUNGER_BEFORE_EATING.remove(player);
            SATURATION_BEFORE_EATING.remove(player);
        }
    }

    @SubscribeEvent
    public static void handleEatFoodEnd(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || event.getItem().getUseAnimation() != UseAnim.EAT) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null || data.hasEssential(BodyRegion.LOWER_ORGANS)) return;

        FoodData food = player.getFoodData();
        Integer hunger = HUNGER_BEFORE_EATING.remove(player);
        if (hunger != null) {
            food.setFoodLevel(hunger);
        }
        Float saturation = SATURATION_BEFORE_EATING.remove(player);
        if (saturation != null) {
            food.setSaturation(saturation);
        }
    }

    // ---- missing skin: more damage ----

    @SubscribeEvent
    public static void handleMissingSkin(LivingIncomingDamageEvent event) {
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        DamageSource source = event.getSource();
        if (data != null && !data.hasEssential(BodyRegion.SKIN)
                && (!source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.IS_FALL))) {
            event.setAmount(event.getAmount() * 3F);
        }
    }

    // ---- missing arms: the corresponding hand can't be used ----

    /**
     * @return whether the arm holding the given hand is missing or an unpowered cyberarm
     */
    public static boolean isHandDisabled(Player player, CyberwareUserData data, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        SidedLimb.Side side = arm == HumanoidArm.RIGHT ? SidedLimb.Side.RIGHT : SidedLimb.Side.LEFT;
        if (!data.hasEssential(BodyRegion.ARM, side)) return true;
        ItemStack cyberarm = data.getCyberware(side == SidedLimb.Side.RIGHT ? CyberwareItems.CYBERARM_RIGHT.get() : CyberwareItems.CYBERARM_LEFT.get());
        return !cyberarm.isEmpty() && !CyberlimbItem.isPowered(cyberarm);
    }

    private static void cancelIfHandDisabled(PlayerInteractEvent event) {
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        if (data != null && isHandDisabled(event.getEntity(), data, event.getHand()) && event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfHandDisabled(event);
    }

    @SubscribeEvent
    public static void handleLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfHandDisabled(event);
    }

    @SubscribeEvent
    public static void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfHandDisabled(event);
    }

    @SubscribeEvent
    public static void handleRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfHandDisabled(event);
    }
}
