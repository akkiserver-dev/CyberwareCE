package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.network.SwitchHeldItemPayload;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MuscleUpgradeItem extends CyberwareItem implements MenuItem {
    private static final ResourceLocation SPEED_MODIFIER = CyberwareAPI.id("muscle_speed_upgrade");
    private static final ResourceLocation DAMAGE_MODIFIER = CyberwareAPI.id("muscle_damage_upgrade");

    private static final Set<LivingEntity> STRENGTH_POWERED = Collections.newSetFromMap(new WeakHashMap<>());

    private final Kind kind;

    public MuscleUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public void onAdded(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.WIRED_REFLEXES) {
            AttributeHelper.applyTransient(entity, Attributes.ATTACK_SPEED, SPEED_MODIFIER, 1.5);
        } else {
            AttributeHelper.applyTransient(entity, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, 3.0);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.WIRED_REFLEXES) {
            AttributeHelper.remove(entity, Attributes.ATTACK_SPEED, SPEED_MODIFIER);
        } else {
            AttributeHelper.remove(entity, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER);
        }
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return kind == Kind.WIRED_REFLEXES ? 3 : 1;
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return kind == Kind.WIRED_REFLEXES ? CyberwareConstants.REFLEXES_CONSUMPTION : CyberwareConstants.REPLACEMENTS_CONSUMPTION;
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return kind == Kind.MUSCLE_REPLACEMENTS;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return kind == Kind.MUSCLE_REPLACEMENTS && CyberwareAPI.getCyberware(other).isEssential(other);
    }

    @SubscribeEvent
    public static void handleHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;

        ItemStack reflexes = data.getCyberware(CyberwareItems.WIRED_REFLEXES.get());
        int rank = reflexes.getCount();
        if (rank <= 1 || !EnableDisableHelper.isEnabled(reflexes) || !STRENGTH_POWERED.contains(player)) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || !source.isDirect()) return;
        if (player.tickCount - player.getCombatTracker().lastDamageTime <= 120) return;

        Inventory inventory = player.getInventory();
        int weaponSlot = -1;
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && (player.getUseItemRemainingTicks() > 0
                || mainHand.getItem() instanceof SwordItem
                || getAttackDamage(mainHand) != null)) {
            weaponSlot = inventory.selected;
        }

        if (weaponSlot == -1) {
            double mostDamage = 0;
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                if (slot == inventory.selected) continue;
                Double damage = getAttackDamage(inventory.getItem(slot));
                if (damage != null && (damage > mostDamage || weaponSlot == -1)) {
                    mostDamage = damage;
                    weaponSlot = slot;
                }
            }
        }

        if (weaponSlot != -1) {
            inventory.selected = weaponSlot;
            int attackerId = rank > 2 ? attacker.getId() : -1;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SwitchHeldItemPayload(weaponSlot, player.getId(), attackerId));
        }
    }

    /**
     * @return the attack damage bonus of a weapon in the main hand, or {@code null} if it has none
     */
    private static Double getAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return null;
        double[] damage = {0};
        boolean[] found = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                found[0] = true;
                damage[0] += modifier.amount();
            }
        });
        return found[0] ? damage[0] : null;
    }

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        boolean isSecond = entity.tickCount % 20 == 0;

        MuscleUpgradeItem replacementsItem = CyberwareItems.MUSCLE_REPLACEMENTS.get();
        ItemStack replacements = data.getCyberware(replacementsItem);
        if (!replacements.isEmpty()) {
            boolean wasPowered = STRENGTH_POWERED.contains(entity);
            boolean isPowered = isSecond ? data.usePower(replacements, replacementsItem.getPowerConsumption(replacements)) : wasPowered;
            if (isPowered) {
                Vec3 motion = entity.getDeltaMovement();
                if (!entity.isInWater() && entity.onGround()
                        && Math.abs(entity.xxa) + Math.abs(entity.zza) > 0.0F
                        && Math.abs(motion.x) + Math.abs(motion.z) > 0.0F) {
                    // increase maximum horizontal motion by 70% (0.118 -> 0.200)
                    float boost = 0.51F;
                    entity.moveRelative(0.075F, new Vec3(entity.xxa * boost, 0.0F, entity.zza * boost));
                }
                if (isSecond) {
                    replacementsItem.onAdded(entity, replacements);
                }
                STRENGTH_POWERED.add(entity);
            } else if (isSecond) {
                replacementsItem.onRemoved(entity, replacements);
                STRENGTH_POWERED.remove(entity);
            }
        } else if (isSecond) {
            replacementsItem.onRemoved(entity, replacements);
            STRENGTH_POWERED.remove(entity);
        }

        if (isSecond) {
            MuscleUpgradeItem reflexesItem = CyberwareItems.WIRED_REFLEXES.get();
            ItemStack reflexes = data.getCyberware(reflexesItem);
            if (!reflexes.isEmpty() && EnableDisableHelper.isEnabled(reflexes)
                    && data.usePower(reflexes, reflexesItem.getPowerConsumption(reflexes))) {
                reflexesItem.onAdded(entity, reflexes);
            } else {
                reflexesItem.onRemoved(entity, reflexes);
            }
        }
    }

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind == Kind.WIRED_REFLEXES;
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
        WIRED_REFLEXES,
        MUSCLE_REPLACEMENTS
    }
}
