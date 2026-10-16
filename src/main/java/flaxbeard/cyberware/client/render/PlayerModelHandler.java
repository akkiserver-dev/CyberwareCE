package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.client.CyberwareClientConfig;
import flaxbeard.cyberware.common.handler.EssentialsMissingHandler;
import flaxbeard.cyberware.common.item.HandUpgradeItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Changes how players are rendered: missing limbs are hidden, cyberlimbs use robot textures (see {@link CyberlimbLayer}),
 * players without legs are lower, and the main hand moves to the remaining arm.
 */
public final class PlayerModelHandler {
    private static final float LEGLESS_OFFSET = 11F / 16F;

    /** Items hidden during the render of a player, to restore afterwards. */
    private static final Map<Player, Map<EquipmentSlot, ItemStack>> HIDDEN_ITEMS = new HashMap<>();
    private static final Map<Player, Boolean> LOWERED = new HashMap<>();

    @Nullable
    private static HumanoidArm previousMainArm = null;
    private static boolean missingArm = false;
    private static boolean missingSecondArm = false;
    @Nullable
    private static PlayerModel<AbstractClientPlayer> wideArmModel;
    @Nullable
    private static ModelPart claws;

    private PlayerModelHandler() {
    }

    // ---- third person ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!CyberwareClientConfig.ENABLE_CUSTOM_PLAYER_MODEL.get()) return;
        Player player = event.getEntity();
        PlayerCyberwareState state = PlayerCyberwareState.of(player);
        if (state == null || !state.isModified()) return;

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        if (!state.hasSkin()) {
            model.head.visible = false;
            model.hat.visible = false;
            model.body.visible = false;
            model.jacket.visible = false;
        }
        hideIfReplaced(model.leftArm, model.leftSleeve, state.hasLeftArm(), state.leftArm(), state.hasSkin());
        hideIfReplaced(model.rightArm, model.rightSleeve, state.hasRightArm(), state.rightArm(), state.hasSkin());
        hideIfReplaced(model.leftLeg, model.leftPants, state.hasLeftLeg(), state.leftLeg(), state.hasSkin());
        hideIfReplaced(model.rightLeg, model.rightPants, state.hasRightLeg(), state.rightLeg(), state.hasSkin());

        Map<EquipmentSlot, ItemStack> hidden = new HashMap<>();
        if (state.hasNoLegs()) {
            // no legs: no pants and shoes, and the body is lower
            hideItem(player, EquipmentSlot.LEGS, hidden);
            hideItem(player, EquipmentSlot.FEET, hidden);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0, -LEGLESS_OFFSET, 0);
            LOWERED.put(player, true);
        }
        // no arm: nothing is held on that side
        HumanoidArm mainArm = player.getMainArm();
        if (!state.hasLeftArm()) {
            hideItem(player, mainArm == HumanoidArm.LEFT ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, hidden);
        }
        if (!state.hasRightArm()) {
            hideItem(player, mainArm == HumanoidArm.RIGHT ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, hidden);
        }
        if (!hidden.isEmpty()) {
            HIDDEN_ITEMS.put(player, hidden);
        }
    }

    private static void hideIfReplaced(ModelPart part, ModelPart overlay, boolean present, PlayerCyberwareState.LimbLook look, boolean hasSkin) {
        if (!present || CyberlimbLayer.getLimbTexture(look, hasSkin) != null) {
            part.visible = false;
            overlay.visible = false;
        }
    }

    private static void hideItem(Player player, EquipmentSlot slot, Map<EquipmentSlot, ItemStack> hidden) {
        if (hidden.containsKey(slot)) return;
        ItemStack stack = getSlot(player, slot);
        if (!stack.isEmpty()) {
            hidden.put(slot, stack);
            setSlot(player, slot, ItemStack.EMPTY);
        }
    }

    // the inventory is changed directly so that no equipment change is triggered
    private static ItemStack getSlot(Player player, EquipmentSlot slot) {
        Inventory inventory = player.getInventory();
        return switch (slot) {
            case MAINHAND -> Inventory.isHotbarSlot(inventory.selected) ? inventory.items.get(inventory.selected) : ItemStack.EMPTY;
            case OFFHAND -> inventory.offhand.get(0);
            default -> inventory.armor.get(slot.getIndex());
        };
    }

    private static void setSlot(Player player, EquipmentSlot slot, ItemStack stack) {
        Inventory inventory = player.getInventory();
        switch (slot) {
            case MAINHAND -> inventory.items.set(inventory.selected, stack);
            case OFFHAND -> inventory.offhand.set(0, stack);
            default -> inventory.armor.set(slot.getIndex(), stack);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Map<EquipmentSlot, ItemStack> hidden = HIDDEN_ITEMS.remove(player);
        if (hidden != null) {
            hidden.forEach((slot, stack) -> setSlot(player, slot, stack));
        }
        if (LOWERED.remove(player) != null) {
            event.getPoseStack().popPose();
        }
    }

    // ---- first person ----

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!CyberwareClientConfig.ENABLE_CUSTOM_PLAYER_MODEL.get()) return;
        if ((event.getHand() == InteractionHand.MAIN_HAND && missingSecondArm) || (event.getHand() == InteractionHand.OFF_HAND && missingArm)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (!CyberwareClientConfig.ENABLE_CUSTOM_PLAYER_MODEL.get()) return;
        AbstractClientPlayer player = event.getPlayer();
        PlayerCyberwareState state = PlayerCyberwareState.of(player);
        if (state == null) return;

        HumanoidArm arm = event.getArm();
        boolean present = arm == HumanoidArm.LEFT ? state.hasLeftArm() : state.hasRightArm();
        if (!present) {
            event.setCanceled(true);
            return;
        }
        PlayerCyberwareState.LimbLook look = arm == HumanoidArm.LEFT ? state.leftArm() : state.rightArm();
        if (look == PlayerCyberwareState.LimbLook.NORMAL) return;

        event.setCanceled(true);
        PlayerModel<AbstractClientPlayer> model = getWideArmModel();
        PoseStack poseStack = event.getPoseStack();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart armPart = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
        ModelPart sleeve = arm == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
        armPart.visible = true;
        sleeve.visible = true;
        armPart.xRot = 0.0F;
        sleeve.xRot = 0.0F;
        armPart.render(poseStack, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(CyberlimbLayer.ROBOT)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        sleeve.render(poseStack, event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(CyberlimbLayer.ROBOT)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);

        renderClaws(event, player, arm);
    }

    private static void renderClaws(RenderArmEvent event, AbstractClientPlayer player, HumanoidArm arm) {
        if (player.getMainArm() != arm || !player.getMainHandItem().isEmpty()) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;
        ItemStack clawsStack = data.getCyberware(CyberwareItems.CLAWS.get());
        if (clawsStack.isEmpty() || !EnableDisableHelper.isEnabled(clawsStack) || !HandUpgradeItem.hasPrimaryCyberarm(player, data)) return;

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float percent = (player.tickCount + partialTick - HandUpgradeItem.clawsTime) / 4F;
        percent = (float) Math.sin(Math.max(0F, Math.min(1.0F, percent)) * Math.PI / 2F);

        ModelPart claw = getClaws();
        claw.setPos(-5.0F, -5.0F + 7F * percent, 0.0F);
        claw.xRot = 0.0F;
        claw.yRot = 0.0F;
        claw.zRot = 0.07F;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (arm == HumanoidArm.LEFT) {
            // the claws model is made for the right arm
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
        claw.render(poseStack, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(CyberlimbLayer.ROBOT)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static PlayerModel<AbstractClientPlayer> getWideArmModel() {
        if (wideArmModel == null) {
            wideArmModel = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return wideArmModel;
    }

    private static ModelPart getClaws() {
        if (claws == null) {
            claws = Minecraft.getInstance().getEntityModels().bakeLayer(CyberwareModelLayers.CLAWS).getChild("claw1");
        }
        return claws;
    }

    /**
     * Clears the cached models when resources are reloaded.
     */
    public static void clearModels() {
        wideArmModel = null;
        claws = null;
    }

    // ---- main hand ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;

        boolean hasRightArm = !isArmDisabled(player, data, HumanoidArm.RIGHT);
        boolean hasLeftArm = !isArmDisabled(player, data, HumanoidArm.LEFT);
        boolean stillMissingArm = false;
        boolean stillMissingSecondArm = false;
        if (!hasRightArm) {
            setMainArm(minecraft, HumanoidArm.LEFT);
            missingArm = stillMissingArm = true;
            if (!hasLeftArm) {
                missingSecondArm = stillMissingSecondArm = true;
            }
        } else if (!hasLeftArm) {
            setMainArm(minecraft, HumanoidArm.RIGHT);
            missingArm = stillMissingArm = true;
        }

        if (!stillMissingArm) {
            missingArm = false;
            restoreMainArm(minecraft);
        }
        if (!stillMissingSecondArm) {
            missingSecondArm = false;
        }
    }

    private static boolean isArmDisabled(Player player, CyberwareUserData data, HumanoidArm arm) {
        InteractionHand hand = player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return EssentialsMissingHandler.isHandDisabled(player, data, hand);
    }

    private static void setMainArm(Minecraft minecraft, HumanoidArm arm) {
        if (minecraft.options.mainHand().get() != arm) {
            if (previousMainArm == null) {
                previousMainArm = minecraft.options.mainHand().get();
            }
            minecraft.options.mainHand().set(arm);
            minecraft.options.broadcastOptions();
        }
    }

    private static void restoreMainArm(Minecraft minecraft) {
        if (previousMainArm != null) {
            minecraft.options.mainHand().set(previousMainArm);
            minecraft.options.broadcastOptions();
            previousMainArm = null;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        missingArm = false;
        missingSecondArm = false;
        if (previousMainArm != null) {
            Minecraft.getInstance().options.mainHand().set(previousMainArm);
            previousMainArm = null;
        }
    }
}
