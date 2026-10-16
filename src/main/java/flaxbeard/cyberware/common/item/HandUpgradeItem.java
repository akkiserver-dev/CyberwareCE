package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class HandUpgradeItem extends CyberwareItem implements MenuItem {
    private static final ResourceLocation CLAWS_MODIFIER = CyberwareAPI.id("claws_damage_upgrade");

    private static final Set<LivingEntity> CLAWS_OUT = Collections.newSetFromMap(new WeakHashMap<>());

    /** Client side time (ticks + partial ticks) at which the claws animation of the local player starts. */
    public static float clawsTime;

    private final Kind kind;

    public HandUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        return List.of(List.of(
                new ItemStack(CyberwareItems.CYBERARM_LEFT.get()),
                new ItemStack(CyberwareItems.CYBERARM_RIGHT.get())));
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return other.getItem() instanceof HandUpgradeItem;
    }

    /**
     * @return whether a cyberarm is installed on the side of the main hand
     */
    public static boolean hasPrimaryCyberarm(LivingEntity entity, CyberwareUserData data) {
        return entity.getMainArm() == HumanoidArm.RIGHT
                ? data.isCyberwareInstalled(CyberwareItems.CYBERARM_RIGHT.get())
                : data.isCyberwareInstalled(CyberwareItems.CYBERARM_LEFT.get());
    }

    // ---- Retractable Claws ----

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();

        ItemStack claws = data.getCyberware(CyberwareItems.CLAWS.get());
        boolean wasOut = CLAWS_OUT.contains(entity);
        boolean isOut = !claws.isEmpty()
                && entity.getMainHandItem().isEmpty()
                && hasPrimaryCyberarm(entity, data)
                && EnableDisableHelper.isEnabled(claws);

        if (isOut) {
            if (!wasOut || entity.tickCount % 20 == 0) {
                AttributeHelper.applyTransient(entity, Attributes.ATTACK_DAMAGE, CLAWS_MODIFIER, 5.5);
                CLAWS_OUT.add(entity);
            }
            if (!wasOut && isLocalPlayer(entity)) {
                startClawsAnimation(entity, true);
            }
        } else if (wasOut || entity.tickCount % 20 == 0) {
            AttributeHelper.remove(entity, Attributes.ATTACK_DAMAGE, CLAWS_MODIFIER);
            CLAWS_OUT.remove(entity);
        }
    }

    private static boolean isLocalPlayer(Entity entity) {
        return entity instanceof Player player && player.isLocalPlayer();
    }

    private static void startClawsAnimation(LivingEntity entity, boolean delay) {
        clawsTime = entity.tickCount + (delay ? 5 : 0);
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        if (kind == Kind.CLAWS) {
            AttributeHelper.remove(entity, Attributes.ATTACK_DAMAGE, CLAWS_MODIFIER);
            CLAWS_OUT.remove(entity);
        }
    }

    // ---- Reinforced Fist ----

    private static boolean canMineWithFist(Player player) {
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        return data != null
                && hasPrimaryCyberarm(player, data)
                && data.isCyberwareInstalled(CyberwareItems.MINING_FIST.get())
                && player.getMainHandItem().isEmpty();
    }

    @SubscribeEvent
    public static void handleHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (canMineWithFist(event.getEntity())
                && new ItemStack(CyberwareConfig.getFistMiningTool()).isCorrectToolForDrops(event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void handleBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (canMineWithFist(event.getEntity())) {
            ItemStack tool = new ItemStack(CyberwareConfig.getFistMiningTool());
            event.setNewSpeed(event.getNewSpeed() * tool.getDestroySpeed(event.getState()));
        }
    }

    // ---- menu ----

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind == Kind.CLAWS;
    }

    @Override
    public void use(Entity entity, ItemStack stack) {
        EnableDisableHelper.toggle(stack);
        if (entity instanceof LivingEntity livingEntity && isLocalPlayer(entity)) {
            startClawsAnimation(livingEntity, false);
        }
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
        CRAFT_HANDS,
        CLAWS,
        MINING
    }
}
