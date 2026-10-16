package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.ArmorClass;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.network.DodgePayload;
import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public class BrainUpgradeItem extends CyberwareItem implements MenuItem {
    private static final float JAMMER_RANGE = 25F;

    private static final Map<LivingEntity, Boolean> CONTEXTUALIZER_WORKING = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> MATRIX_WORKING = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> RADIO_WORKING = new WeakHashMap<>();

    private final Kind kind;

    public BrainUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return kind == Kind.CORTICAL_STACK && other.is(CyberwareItems.CONSCIOUSNESS_TRANSMITTER.get());
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return switch (kind) {
            case NEURAL_CONTEXTUALIZER -> CyberwareConstants.CONTEXTUALIZER_CONSUMPTION;
            case THREAT_MATRIX -> CyberwareConstants.MATRIX_CONSUMPTION;
            case RADIO -> CyberwareConstants.RADIO_CONSUMPTION;
            default -> 0;
        };
    }

    // ---- Ender Jammer ----

    @SubscribeEvent
    public static void handleEnderEntityTeleport(EntityTeleportEvent.EnderEntity event) {
        if (!isTeleportationAllowed(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void handleEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        if (!isTeleportationAllowed(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void handleChorusFruitTeleport(EntityTeleportEvent.ChorusFruit event) {
        if (!isTeleportationAllowed(event.getEntityLiving())) event.setCanceled(true);
    }

    public static boolean isTeleportationAllowed(@Nullable LivingEntity entity) {
        if (entity == null) return true;
        if (hasActiveJammer(entity)) return false;

        AABB area = entity.getBoundingBox().inflate(JAMMER_RANGE);
        for (LivingEntity other : entity.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.distanceToSqr(other) <= JAMMER_RANGE * JAMMER_RANGE && hasActiveJammer(other)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasActiveJammer(LivingEntity entity) {
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null) return false;
        ItemStack jammer = data.getCyberware(CyberwareItems.ENDER_JAMMER.get());
        return !jammer.isEmpty() && EnableDisableHelper.isEnabled(jammer);
    }

    // ---- Cortical Stack & Consciousness Transmitter ----

    @SubscribeEvent
    public static void handleClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player original = event.getOriginal();
        if (original.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        CyberwareUserData data = CyberwareAPI.getUserData(original);
        if (data == null) return;

        if (data.isCyberwareInstalled(CyberwareItems.CORTICAL_STACK.get())) {
            if (!original.level().isClientSide()) {
                ItemStack capsule = new ItemStack(CyberwareItems.EXP_CAPSULE.get());
                capsule.set(CyberwareDataComponents.EXPERIENCE, original.totalExperience);
                original.level().addFreshEntity(new ItemEntity(original.level(), original.getX(), original.getY(), original.getZ(), capsule));
            }
        } else if (data.isCyberwareInstalled(CyberwareItems.CONSCIOUSNESS_TRANSMITTER.get())) {
            event.getEntity().giveExperiencePoints((int) (Math.min(100, original.experienceLevel * 7) * .9F));
        }
    }

    @SubscribeEvent
    public static void handleExperienceDrop(LivingExperienceDropEvent event) {
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        if (data == null) return;
        if (data.isCyberwareInstalled(CyberwareItems.CORTICAL_STACK.get())
                || data.isCyberwareInstalled(CyberwareItems.CONSCIOUSNESS_TRANSMITTER.get())) {
            event.setCanceled(true);
        }
    }

    // ---- Neural Contextualizer ----

    @SubscribeEvent
    public static void handleMining(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;

        ItemStack contextualizer = data.getCyberware(CyberwareItems.NEURAL_CONTEXTUALIZER.get());
        if (contextualizer.isEmpty()
                || !EnableDisableHelper.isEnabled(contextualizer)
                || !CONTEXTUALIZER_WORKING.getOrDefault(player, false)
                || player.isShiftKeyDown()) {
            return;
        }

        BlockState state = event.getState();
        Inventory inventory = player.getInventory();
        ItemStack tool = player.getMainHandItem();
        if (!tool.isEmpty() && (tool.getItem() instanceof SwordItem || tool.getItem().getDescriptionId().contains("sword"))) {
            return;
        }
        if (isToolEffective(tool, state)) return;

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (slot != inventory.selected && isToolEffective(inventory.getItem(slot), state)) {
                inventory.selected = slot;
                return;
            }
        }
    }

    private static boolean isToolEffective(ItemStack tool, BlockState state) {
        return !tool.isEmpty() && tool.getDestroySpeed(state) > 1.0F;
    }

    // ---- power ----

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;
        CyberwareUserData data = event.getUserData();

        CONTEXTUALIZER_WORKING.put(entity, useToggledPower(data, CyberwareItems.NEURAL_CONTEXTUALIZER.get(), true));
        MATRIX_WORKING.put(entity, useToggledPower(data, CyberwareItems.THREAT_MATRIX.get(), false));
        RADIO_WORKING.put(entity, useToggledPower(data, CyberwareItems.CRANIAL_BROADCASTER.get(), true));
    }

    private static boolean useToggledPower(CyberwareUserData data, BrainUpgradeItem item, boolean toggleable) {
        ItemStack stack = data.getCyberware(item);
        if (stack.isEmpty() || (toggleable && !EnableDisableHelper.isEnabled(stack))) {
            return false;
        }
        return data.usePower(stack, item.getPowerConsumption(stack));
    }

    /**
     * @return whether the Cranial Broadcaster of this entity is turned on and powered
     */
    public static boolean isRadioWorking(LivingEntity entity) {
        return RADIO_WORKING.getOrDefault(entity, false);
    }

    // ---- Threat Matrix ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !MATRIX_WORKING.getOrDefault(entity, false)) return;

        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.THREAT_MATRIX.get())) return;

        DamageSource source = event.getSource();
        if (source.getEntity() == null && source.getDirectEntity() == null) return;

        ArmorClass armorClass = ArmorClass.get(entity);
        if (armorClass == ArmorClass.HEAVY) return;

        if (entity.invulnerableTime <= 10) {
            float chance = armorClass == ArmorClass.LIGHT ? CyberwareConstants.DODGE_ARMOR : CyberwareConstants.DODGE_NO_ARMOR;
            if (entity.getRandom().nextFloat() < chance) {
                event.setCanceled(true);
                entity.invulnerableTime = 20;
                entity.hurtTime = entity.hurtDuration = 10;
                entity.lastHurt = 9999F;
                PacketDistributor.sendToPlayersNear((ServerLevel) entity.level(), null,
                        entity.getX(), entity.getY(), entity.getZ(), 50, new DodgePayload(entity.getId()));
            }
        }
    }

    // ---- menu ----

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind == Kind.ENDER_JAMMER || kind == Kind.NEURAL_CONTEXTUALIZER || kind == Kind.RADIO;
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
        CORTICAL_STACK,
        ENDER_JAMMER,
        CONSCIOUSNESS_TRANSMITTER,
        NEURAL_CONTEXTUALIZER,
        THREAT_MATRIX,
        RADIO
    }
}
