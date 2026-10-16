package flaxbeard.cyberware.client.handler;

import net.minecraft.core.Holder;
import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.item.CybereyeUpgradeItem;
import flaxbeard.cyberware.common.item.CybereyesItem;
import flaxbeard.cyberware.common.item.LungsUpgradeItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client side visual effects of cyberware.
 */
public final class CyberwareClientEffects {
    private static final ResourceLocation SPIDER_SHADER = ResourceLocation.withDefaultNamespace("shaders/post/spider.json");
    private static final ResourceLocation AIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/air");
    private static final ResourceLocation AIR_BURSTING_SPRITE = ResourceLocation.withDefaultNamespace("hud/air_bursting");
    private static final float HIGHLIGHT_RANGE = 25F;
    private static final float[] ZOOM_DIVISORS = {1F, 2F, 5F, 12F};

    private static final List<LivingEntity> HIGHLIGHTED = new ArrayList<>();
    private static int highlightTick = -1;
    private static boolean highlighting = false;

    /** Entity id -> game time of falls on cyberlegs whose hurt sound should be replaced. */
    private static final Map<Integer, Long> CYBERLEG_FALLS = new HashMap<>();

    private CyberwareClientEffects() {
    }

    private static CyberwareUserData localData() {
        return CyberwareAPI.getUserData(Minecraft.getInstance().player);
    }

    // ---- black overlays: missing eyes, unpowered cybereyes, surgery ----

    /**
     * GUI layer rendered below all others.
     */
    public static void renderBlackout(GuiGraphics graphics, DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (data != null && !player.isCreative() && (!data.hasEssential(BodyRegion.EYES) || CybereyesItem.isBlind(player))) {
            graphics.fill(0, 0, width, height, 0xE6000000);
        }

        if (SurgeryBlockEntity.workingOnPlayer) {
            float ticks = SurgeryBlockEntity.playerProgressTicks + deltaTracker.getGameTimeDeltaPartialTick(false);
            float alpha = 1.0F;
            if (ticks < 20F) {
                alpha = ticks / 20F;
            } else if (ticks > 60F) {
                alpha = (80F - ticks) / 20F;
            }
            int alphaByte = Mth.clamp((int) (alpha * 255), 0, 255);
            graphics.fill(0, 0, width, height, alphaByte << 24);
        }
    }

    // ---- Liquid Refraction Calibrator ----

    @SubscribeEvent
    public static void handleFog(ViewportEvent.RenderFog event) {
        CyberwareUserData data = localData();
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.UNDERWATER_VISION.get())) return;
        if (event.getType() == FogType.WATER) {
            event.setNearPlaneDistance(-8.0F);
            event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), 128.0F));
            event.setCanceled(true);
        } else if (event.getType() == FogType.LAVA) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(5.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleLiquidOverlay(RenderBlockScreenEffectEvent event) {
        CyberwareUserData data = CyberwareAPI.getUserData(event.getPlayer());
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.UNDERWATER_VISION.get())) return;
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER
                || (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE && event.getPlayer().isInLava())) {
            event.setCanceled(true);
        }
    }

    // ---- Targeting Overlay: nearby entities glow while rendering ----

    @SubscribeEvent
    public static void startHighlight(RenderFrameEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (player.tickCount != highlightTick) {
            highlightTick = player.tickCount;
            CyberwareUserData data = CyberwareAPI.getUserData(player);
            ItemStack targeting = data == null ? ItemStack.EMPTY : data.getCyberware(CyberwareItems.TARGETING.get());
            highlighting = !targeting.isEmpty() && EnableDisableHelper.isEnabled(targeting);
        }
        if (!highlighting) return;

        HIGHLIGHTED.clear();
        double rangeSquared = HIGHLIGHT_RANGE * HIGHLIGHT_RANGE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HIGHLIGHT_RANGE))) {
            if (entity != player && !entity.isCurrentlyGlowing() && player.distanceToSqr(entity) <= rangeSquared) {
                entity.setGlowingTag(true);
                HIGHLIGHTED.add(entity);
            }
        }
    }

    @SubscribeEvent
    public static void endHighlight(RenderFrameEvent.Post event) {
        for (LivingEntity entity : HIGHLIGHTED) {
            entity.setGlowingTag(false);
        }
        HIGHLIGHTED.clear();
    }

    // ---- Distance Enhancer ----

    private static boolean isZooming() {
        Minecraft minecraft = Minecraft.getInstance();
        CyberwareUserData data = localData();
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.ZOOM.get())) {
            CybereyeUpgradeItem.zoomLevel = 0;
            return false;
        }
        return CybereyeUpgradeItem.zoomLevel > 0 && minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
    }

    @SubscribeEvent
    public static void handleZoomFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov() && isZooming()) {
            event.setFOV((event.getFOV() + 5.0) / ZOOM_DIVISORS[CybereyeUpgradeItem.zoomLevel]);
        }
    }

    @SubscribeEvent
    public static void handleZoomSensitivity(CalculatePlayerTurnEvent event) {
        if (isZooming()) {
            event.setMouseSensitivity(event.getMouseSensitivity() / Math.sqrt(ZOOM_DIVISORS[CybereyeUpgradeItem.zoomLevel]));
        }
    }

    // ---- spider eyes ----

    @SubscribeEvent
    public static void handleSpiderVision(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        CyberwareUserData data = CyberwareAPI.getUserData(player);
        PostChain effect = minecraft.gameRenderer.currentEffect();
        if (data != null && data.isCyberwareInstalled(Items.SPIDER_EYE)) {
            if (effect == null) {
                minecraft.gameRenderer.loadEffect(SPIDER_SHADER);
            }
        } else if (!player.isSpectator() && effect != null && effect.getName().equals(SPIDER_SHADER.toString())) {
            minecraft.gameRenderer.shutdownEffect();
        }
    }

    @SubscribeEvent
    public static void offsetCrosshairPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && hasSpiderEyes()) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, event.getGuiGraphics().guiHeight() / 5F, 0);
        }
    }

    @SubscribeEvent
    public static void offsetCrosshairPost(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && hasSpiderEyes()) {
            event.getGuiGraphics().pose().popPose();
        }
    }

    private static boolean hasSpiderEyes() {
        CyberwareUserData data = localData();
        return data != null && data.isCyberwareInstalled(Items.SPIDER_EYE);
    }

    // ---- Compressed Oxygen Implant: extra air bubbles ----

    @SubscribeEvent
    public static void renderStoredAir(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isCreative() || !player.isEyeInFluid(FluidTags.WATER)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;
        ItemStack oxygen = data.getCyberware(CyberwareItems.COMPRESSED_OXYGEN.get());
        if (oxygen.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int air = LungsUpgradeItem.getStoredAir(oxygen);
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - (minecraft.gui.rightHeight - 10) - 8;
        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        RenderSystem.enableBlend();
        while (air > 0) {
            red = Math.min(1.0F, red + 1.0F);
            blue -= 0.25F;
            green = Math.min(1.0F, green + 0.25F);
            RenderSystem.setShaderColor(red, green, Math.max(0F, blue), 1.0F);
            int drawAir = Math.min(300, air);
            int full = Mth.ceil((drawAir - 2) * 10.0D / 300.0D);
            int partial = Mth.ceil(drawAir * 10.0D / 300.0D) - full;
            for (int index = 0; index < full + partial; index++) {
                graphics.blitSprite(index < full ? AIR_SPRITE : AIR_BURSTING_SPRITE, right - index * 8 - 9, top, 9, 9);
            }
            air -= 300;
            top -= 8;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ---- metallic landing sound of cyberlegs ----

    public static void markCyberlegFall(int entityId) {
        if (Minecraft.getInstance().level != null) {
            CYBERLEG_FALLS.put(entityId, Minecraft.getInstance().level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void handleHurtSound(PlayLevelSoundEvent.AtPosition event) {
        if (CYBERLEG_FALLS.isEmpty() || event.getSound() == null || !event.getSound().value().equals(SoundEvents.PLAYER_HURT)) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        CYBERLEG_FALLS.entrySet().removeIf(entry -> level.getGameTime() - entry.getValue() > 20);
        for (Map.Entry<Integer, Long> entry : CYBERLEG_FALLS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entity.position().distanceToSqr(event.getPosition()) < 0.01) {
                event.setSound(Holder.direct(SoundEvents.IRON_GOLEM_HURT));
                event.setNewPitch(event.getNewPitch() + 1F);
                CYBERLEG_FALLS.remove(entry.getKey());
                return;
            }
        }
    }
}
