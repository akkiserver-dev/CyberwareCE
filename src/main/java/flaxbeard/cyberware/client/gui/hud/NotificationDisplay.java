package flaxbeard.cyberware.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.hud.AbstractHudElement;
import flaxbeard.cyberware.api.hud.Notification;
import flaxbeard.cyberware.api.hud.NotificationInstance;
import flaxbeard.cyberware.client.ClientCyberware;
import flaxbeard.cyberware.client.handler.HudHandler;
import flaxbeard.cyberware.common.ArmorClass;
import flaxbeard.cyberware.common.block.entity.RadioNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * A bar of small icons notifying about armor weight changes, radio range and dodges.
 */
public class NotificationDisplay extends AbstractHudElement {
    private static final List<NotificationInstance> EXAMPLES = List.of(
            new NotificationInstance(0, new ArmorNotification(true)),
            new NotificationInstance(0, new ArmorNotification(false)),
            new NotificationInstance(0, new ArmorNotification(true)),
            new NotificationInstance(0, new ArmorNotification(false)));

    private int cachedTick = -1;
    private int radioTier = RadioNetwork.NONE;
    private boolean wearingLightArmor = false;

    public NotificationDisplay() {
        super("cyberware:notification");
        setDefaultX(5);
        setDefaultY(5 - 20);
        setWidth(5 * 18);
        setHeight(14 + 20 + 4);
        setDefaultVerticalAnchor(AnchorVertical.BOTTOM);
    }

    @Override
    public void renderElement(GuiGraphics graphics, int x, int y, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick) {
        if (isHidden() || !hudjackAvailable) return;

        boolean topAnchored = getVerticalAnchor() == AnchorVertical.TOP;
        boolean rightAnchored = getHorizontalAnchor() == AnchorHorizontal.RIGHT;
        float currentTime = player.tickCount + partialTick;

        if (player.tickCount != cachedTick) {
            cachedTick = player.tickCount;
            boolean wasWearingLightArmor = wearingLightArmor;
            wearingLightArmor = ArmorClass.isWearingLightOrNone(player);
            if (wearingLightArmor != wasWearingLightArmor) {
                HudHandler.addNotification(new NotificationInstance(currentTime, new ArmorNotification(wearingLightArmor)));
            }
            int previousTier = radioTier;
            radioTier = RadioNetwork.getTierInRange(player.level(), player.getX(), player.getY(), player.getZ());
            if (radioTier != previousTier) {
                HudHandler.addNotification(new NotificationInstance(currentTime, new RadioNotification(radioTier)));
            }
        }

        if (isConfigOpen) {
            // placeholders so that the player can see what it will look like
            for (int index = 0; index < EXAMPLES.size(); index++) {
                double percentVisible = index == 0 ? (player.tickCount % 40F) / 40F : 0F;
                renderNotification(graphics, EXAMPLES.get(index).notification(), EXAMPLES.size() - 1 - index, percentVisible, x, y, topAnchored, rightAnchored);
            }
            return;
        }

        List<NotificationInstance> elapsed = new ArrayList<>();
        List<NotificationInstance> notifications = HudHandler.NOTIFICATIONS;
        for (int index = 0; index < notifications.size(); index++) {
            NotificationInstance instance = notifications.get(index);
            Notification notification = instance.notification();
            if (currentTime - instance.createdTime() < notification.getDuration() + 25) {
                double percentVisible = Math.max(0F, (currentTime - instance.createdTime() - notification.getDuration()) / 30F);
                renderNotification(graphics, notification, notifications.size() - 1 - index, percentVisible, x, y, topAnchored, rightAnchored);
            } else {
                elapsed.add(instance);
            }
        }
        notifications.removeAll(elapsed);
    }

    private void renderNotification(GuiGraphics graphics, Notification notification, int index, double percentVisible, int x, int y, boolean topAnchored, boolean rightAnchored) {
        float yOffset = (float) (20F * Math.sin(percentVisible * Math.PI / 2F));
        graphics.pose().pushPose();
        graphics.pose().translate(0F, topAnchored ? -yOffset : yOffset, 0F);
        int notificationX = rightAnchored ? x + getWidth() - (index + 1) * 18 : x + index * 18;
        notification.render(graphics, notificationX, y + (topAnchored ? 20 : 0));
        graphics.pose().popPose();
    }

    /**
     * Draws a part of the HUD texture tinted with the HUD color.
     */
    static void blitHudColored(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        float[] color = ClientCyberware.getHudColor();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);
        graphics.blit(HudHandler.HUD_TEXTURE, x, y, u, v, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private record ArmorNotification(boolean light) implements Notification {
        @Override
        public void render(GuiGraphics graphics, int x, int y) {
            blitHudColored(graphics, x, y + 1, 0, 25, 15, 14);
            if (light) {
                graphics.blit(HudHandler.HUD_TEXTURE, x + 9, y + 1 + 7, 15, 25, 7, 9);
            } else {
                graphics.blit(HudHandler.HUD_TEXTURE, x + 8, y + 1 + 7, 22, 25, 8, 9);
            }
        }

        @Override
        public int getDuration() {
            return 20;
        }
    }

    private record RadioNotification(int tier) implements Notification {
        @Override
        public void render(GuiGraphics graphics, int x, int y) {
            if (tier > 0) {
                blitHudColored(graphics, x, y + 1, 13, 39, 15, 14);
                String text = tier == RadioNetwork.TIER_PLAYER ? I18n.get("cyberware.gui.radio_internal") : Integer.toString(tier - 1);
                Font font = Minecraft.getInstance().font;
                graphics.drawString(font, text, x + 15 - font.width(text), y + 9, 0xFFFFFF);
            } else {
                blitHudColored(graphics, x, y + 1, 28, 39, 15, 14);
            }
        }

        @Override
        public int getDuration() {
            return 40;
        }
    }

    /**
     * Shown when the Threat Matrix Calculator dodged an attack.
     */
    public record DodgeNotification() implements Notification {
        @Override
        public void render(GuiGraphics graphics, int x, int y) {
            blitHudColored(graphics, x + 1, y + 1, 0, 39, 15, 14);
        }

        @Override
        public int getDuration() {
            return 5;
        }
    }
}
