package flaxbeard.cyberware.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.hud.CyberwareHudDataEvent;
import flaxbeard.cyberware.api.hud.CyberwareHudEvent;
import flaxbeard.cyberware.api.hud.HudElement;
import flaxbeard.cyberware.api.hud.HudSaveData;
import flaxbeard.cyberware.api.hud.NotificationInstance;
import flaxbeard.cyberware.api.item.Hudjack;
import flaxbeard.cyberware.client.CyberwareClientConfig;
import flaxbeard.cyberware.client.KeyBinds;
import flaxbeard.cyberware.client.gui.HudConfigurationScreen;
import flaxbeard.cyberware.client.gui.hud.MissingPowerDisplay;
import flaxbeard.cyberware.client.gui.hud.NotificationDisplay;
import flaxbeard.cyberware.client.gui.hud.PowerDisplay;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the cyberware HUD (power, missing power and notifications) while a Hudjack or Hudlens is active.
 */
public final class HudHandler {
    public static final ResourceLocation HUD_TEXTURE = CyberwareAPI.id("textures/gui/hud.png");
    private static final int MAX_NOTIFICATIONS = 5;

    public static final List<NotificationInstance> NOTIFICATIONS = new ArrayList<>();

    private static final PowerDisplay POWER_DISPLAY = new PowerDisplay();
    private static final MissingPowerDisplay MISSING_POWER_DISPLAY = new MissingPowerDisplay();
    private static final NotificationDisplay NOTIFICATION_DISPLAY = new NotificationDisplay();

    private static int cachedTick = -1;
    private static float floatingFactor = 0.0F;
    private static List<HudElement> hudElements = new ArrayList<>();
    private static boolean hudjackAvailable = false;
    private static boolean promptToOpenMenu = false;
    private static int hudColor = 0x00FFFF;

    private static int lastTick = 0;
    private static double lastVelY = 0;
    private static double lastLastVelY = 0;

    private HudHandler() {
    }

    public static void addNotification(NotificationInstance notification) {
        while (NOTIFICATIONS.size() >= MAX_NOTIFICATIONS) {
            NOTIFICATIONS.remove(0);
        }
        NOTIFICATIONS.add(notification);
    }

    @SubscribeEvent
    public static void addHudElements(CyberwareHudEvent event) {
        if (event.isHudjackAvailable()) {
            event.addElement(POWER_DISPLAY);
            event.addElement(MISSING_POWER_DISPLAY);
            event.addElement(NOTIFICATION_DISPLAY);
        }
    }

    @SubscribeEvent
    public static void saveHudElements(CyberwareHudDataEvent event) {
        event.addElement(POWER_DISPLAY);
        event.addElement(MISSING_POWER_DISPLAY);
        event.addElement(NOTIFICATION_DISPLAY);
    }

    /**
     * @return whether an active Hudjack or Hudlens is installed
     */
    public static boolean isHudjackAvailable(CyberwareUserData data) {
        for (ItemStack stack : data.getHudjackItems()) {
            if (((Hudjack) CyberwareAPI.getCyberware(stack)).isActive(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies the saved HUD layout of the local player to all HUD elements.
     */
    public static void loadHudLayout(CompoundTag layout) {
        CyberwareHudDataEvent event = NeoForge.EVENT_BUS.post(new CyberwareHudDataEvent());
        for (HudElement element : event.getElements()) {
            if (layout.contains(element.getUniqueName())) {
                element.load(new HudSaveData(layout.getCompound(element.getUniqueName())));
            }
        }
    }

    /**
     * The GUI layer drawing the HUD, rendered just below the chat.
     */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (player.tickCount != cachedTick) {
            cachedTick = player.tickCount;
            CyberwareUserData data = CyberwareAPI.getUserData(player);
            if (data == null) return;

            floatingFactor = 0.0F;
            boolean available = false;
            for (ItemStack stack : data.getHudjackItems()) {
                if (((Hudjack) CyberwareAPI.getCyberware(stack)).isActive(stack)) {
                    available = true;
                    if (CyberwareClientConfig.ENABLE_FLOAT.get()) {
                        floatingFactor = (stack.is(CyberwareItems.HUDLENS.get())
                                ? CyberwareClientConfig.HUDLENS_FLOAT.get() : CyberwareClientConfig.HUDJACK_FLOAT.get()).floatValue();
                    }
                    break;
                }
            }

            CyberwareHudEvent event = NeoForge.EVENT_BUS.post(new CyberwareHudEvent(width, height, available));
            hudElements = event.getElements();
            hudjackAvailable = event.isHudjackAvailable();
            promptToOpenMenu = !data.getActiveItems().isEmpty() && !data.hasOpenedRadialMenu();
            hudColor = data.getHudColorHex();
        }

        graphics.pose().pushPose();

        // let the HUD float a little with the movement of the camera
        double accelLastY = lastVelY - lastLastVelY;
        double accelY = player.getDeltaMovement().y - lastVelY;
        double accelPitch = accelLastY + (accelY - accelLastY) * (partialTick + player.tickCount - lastTick) / 2F;
        double pitchMove = floatingFactor * ((player.xBobO + (player.xBob - player.xBobO) * partialTick) - player.getXRot());
        double yawMove = floatingFactor * ((player.yBobO + (player.yBob - player.yBobO) * partialTick) - player.getYRot());
        graphics.pose().translate(yawMove, pitchMove + accelPitch * 50F * floatingFactor, 0);
        if (player.tickCount > lastTick + 1) {
            lastTick = player.tickCount;
            lastLastVelY = lastVelY;
            lastVelY = player.getDeltaMovement().y;
        }

        boolean configOpen = minecraft.screen instanceof HudConfigurationScreen;
        for (HudElement element : hudElements) {
            clampToScreen(element, width, height);
            element.render(graphics, player, width, height, hudjackAvailable, configOpen, partialTick);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // prompt the player to open the radial menu if they haven't yet
        if (promptToOpenMenu) {
            Font font = minecraft.font;
            Component text = Component.translatable("cyberware.gui.open_menu", KeyBinds.MENU.getTranslatedKeyMessage());
            graphics.drawString(font, text, width - font.width(text) - 5, 5, hudColor);
        }

        graphics.pose().popPose();
    }

    /**
     * Keeps a few pixels of every element on screen.
     */
    private static void clampToScreen(HudElement element, int width, int height) {
        if (element.getHeight() + getAbsoluteY(height, element) <= 3) {
            setYFromAbsolute(height, element, 4 - element.getHeight());
        }
        if (getAbsoluteY(height, element) >= height - 3) {
            setYFromAbsolute(height, element, height - 4);
        }
        if (element.getWidth() + getAbsoluteX(width, element) <= 3) {
            setXFromAbsolute(width, element, 4 - element.getWidth());
        }
        if (getAbsoluteX(width, element) >= width - 3) {
            setXFromAbsolute(width, element, width - 4);
        }
    }

    public static int getAbsoluteX(int width, HudElement element) {
        return element.getHorizontalAnchor() == HudElement.AnchorHorizontal.RIGHT ? width - element.getX() - element.getWidth() : element.getX();
    }

    public static int getAbsoluteY(int height, HudElement element) {
        return element.getVerticalAnchor() == HudElement.AnchorVertical.BOTTOM ? height - element.getY() - element.getHeight() : element.getY();
    }

    public static void setXFromAbsolute(int width, HudElement element, int x) {
        element.setX(element.getHorizontalAnchor() == HudElement.AnchorHorizontal.RIGHT ? width - x - element.getWidth() : x);
    }

    public static void setYFromAbsolute(int height, HudElement element, int y) {
        element.setY(element.getVerticalAnchor() == HudElement.AnchorVertical.BOTTOM ? height - y - element.getHeight() : y);
    }
}
