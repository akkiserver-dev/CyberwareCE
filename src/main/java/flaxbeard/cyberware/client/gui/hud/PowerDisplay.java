package flaxbeard.cyberware.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.hud.AbstractHudElement;
import flaxbeard.cyberware.client.handler.HudHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * A battery icon with the stored, consumed and produced power.
 */
public class PowerDisplay extends AbstractHudElement {
    private static final float[] LOW_POWER_COLOR = {1.0F, 0.0F, 0.125F};
    private static final int LOW_POWER_COLOR_HEX = 0xFF0020;

    private float percentFull = 0;
    private int capacity = 0;
    private int stored = 0;
    private int production = 0;
    private int consumption = 0;
    private float[] hudColor = LOW_POWER_COLOR;
    private int hudColorHex = 0x00FFFF;

    public PowerDisplay() {
        super("cyberware:power");
        setDefaultX(5);
        setDefaultY(5);
        setHeight(25);
        setWidth(101);
    }

    @Override
    public void renderElement(GuiGraphics graphics, int x, int y, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick) {
        if (isHidden() || !hudjackAvailable) return;

        if (player.tickCount % 20 == 0) {
            CyberwareUserData data = CyberwareAPI.getUserData(player);
            if (data == null) return;
            percentFull = data.getPercentFull();
            capacity = data.getCapacity();
            stored = data.getStoredPower();
            production = data.getProduction();
            consumption = data.getConsumption();
            hudColor = data.getHudColor();
            hudColorHex = data.getHudColorHex();
        }
        if (capacity == 0) return;

        boolean isLowPower = percentFull <= 0.2F;
        boolean isCriticalPower = percentFull <= 0.05F;
        if (isCriticalPower && player.tickCount % 4 == 0) return;

        boolean rightAnchored = getHorizontalAnchor() == AnchorHorizontal.RIGHT;
        float[] color = isLowPower ? LOW_POWER_COLOR : hudColor;
        int colorHex = isLowPower ? LOW_POWER_COLOR_HEX : hudColorHex;
        Font font = Minecraft.getInstance().font;

        // battery icon
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);
        int u = isLowPower ? 39 : 0;
        int iconX = rightAnchored ? x + getWidth() - 13 : x;
        int fill = Math.round(21F * percentFull);
        // top part, background and foreground of the battery
        graphics.blit(HudHandler.HUD_TEXTURE, iconX, y, u, 0, 13, 2 + (21 - fill));
        graphics.blit(HudHandler.HUD_TEXTURE, iconX, y + 2 + (21 - fill), 13 + u, 2 + (21 - fill), 13, fill + 2);
        graphics.blit(HudHandler.HUD_TEXTURE, iconX, y + 2 + (21 - fill), 26 + u, 2 + (21 - fill), 13, fill + 2);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String storage = stored + " / " + capacity;
        int storageX = rightAnchored ? x + getWidth() - 15 - font.width(storage) : x + 15;
        graphics.drawString(font, storage, storageX, y + 4, colorHex);

        String flow = "-" + consumption + " / +" + production;
        int flowX = rightAnchored ? x + getWidth() - 15 - font.width(flow) : x + 15;
        graphics.drawString(font, flow, flowX, y + 14, colorHex);
    }
}
