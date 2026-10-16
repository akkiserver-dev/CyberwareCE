package flaxbeard.cyberware.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.hud.CyberwareHudDataEvent;
import flaxbeard.cyberware.api.hud.CyberwareHudEvent;
import flaxbeard.cyberware.api.hud.HudElement;
import flaxbeard.cyberware.api.hud.HudSaveData;
import flaxbeard.cyberware.client.ClientCyberware;
import flaxbeard.cyberware.client.handler.HudHandler;
import flaxbeard.cyberware.common.network.SyncHudDataPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Lets the player drag the HUD elements around, anchor them to screen edges and hide them.
 */
public class HudConfigurationScreen extends Screen {
    @Nullable
    private HudElement dragging = null;
    @Nullable
    private HudElement hovered = null;
    private int offsetX = 0;
    private int offsetY = 0;
    private boolean clicked = false;

    public HudConfigurationScreen() {
        super(Component.translatable("cyberware.gui.hud_configuration"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // show the HUD over the world
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        CyberwareUserData data = ClientCyberware.getLocalData();
        boolean active = data != null && HudHandler.isHudjackAvailable(data);
        List<HudElement> elements = NeoForge.EVENT_BUS.post(new CyberwareHudEvent(width, height, active)).getElements();

        hovered = null;
        if (dragging == null) {
            for (HudElement element : elements) {
                int elementX = HudHandler.getAbsoluteX(width, element);
                int elementY = HudHandler.getAbsoluteY(height, element);
                if (isInRegion(elementX, elementY, element.getWidth(), element.getHeight(), mouseX, mouseY)) {
                    hovered = element;
                    offsetX = mouseX - elementX;
                    offsetY = mouseY - elementY;
                    break;
                }
            }
        }

        RenderSystem.enableBlend();
        for (HudElement element : elements) {
            drawBox(graphics, element);
            drawButtons(graphics, element, mouseX, mouseY);
        }
        for (HudElement element : elements) {
            handleButtons(graphics, element, mouseX, mouseY);
        }

        if (dragging != null) {
            HudHandler.setXFromAbsolute(width, dragging, mouseX - offsetX);
            HudHandler.setYFromAbsolute(height, dragging, mouseY - offsetY);
            if (hasShiftDown()) {
                dragging.setX(Math.round(dragging.getX() / 5F) * 5);
                dragging.setY(Math.round(dragging.getY() / 5F) * 5);
            }
            graphics.renderTooltip(font, Component.literal(dragging.getX() + ", " + dragging.getY()), mouseX, mouseY);
        } else if (hovered != null) {
            graphics.renderTooltip(font, Component.literal(hovered.getX() + ", " + hovered.getY()), mouseX, mouseY);
        }
        clicked = false;
    }

    private void blitHud(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        if (width > 0 && height > 0) {
            graphics.blit(HudHandler.HUD_TEXTURE, x, y, u, v, width, height);
        }
    }

    /**
     * Draws a dashed frame around the element and, for the hovered element, guides to its anchor edges.
     */
    private void drawBox(GuiGraphics graphics, HudElement element) {
        int elementX = HudHandler.getAbsoluteX(width, element) - 1;
        int elementY = HudHandler.getAbsoluteY(height, element) - 1;
        float[] color = ClientCyberware.getHudColor();
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);

        if (element == dragging || element == hovered) {
            boolean right = element.getHorizontalAnchor() == HudElement.AnchorHorizontal.RIGHT;
            boolean bottom = element.getVerticalAnchor() == HudElement.AnchorVertical.BOTTOM;

            int remaining = element.getX();
            int position = right ? HudHandler.getAbsoluteX(width, element) + element.getWidth() : 0;
            int lineY = Math.min(height - 2, Math.max(1, bottom ? elementY + element.getHeight() + 1 : elementY));
            while (remaining >= 2) {
                blitHud(graphics, position, lineY, 255, 0, 1, 1);
                position += 2;
                remaining -= 2;
            }
            blitHud(graphics, position, lineY, 255, 0, remaining, 1);

            remaining = element.getY();
            position = bottom ? HudHandler.getAbsoluteY(height, element) + element.getHeight() : 0;
            int lineX = Math.min(width - 2, Math.max(1, right ? elementX + element.getWidth() + 1 : elementX));
            while (remaining >= 2) {
                blitHud(graphics, lineX, position, 255, 0, 1, 1);
                position += 2;
                remaining -= 2;
            }
            blitHud(graphics, lineX, position, 255, 0, 1, remaining);
        }

        // marching ants
        boolean shift = (minecraft.player.tickCount / 4) % 2 == 0;
        int one = shift ? 254 : 255;
        int two = shift ? 255 : 254;
        int boxWidth = element.getWidth() + 2;
        int position = 0;
        while (boxWidth >= 2) {
            blitHud(graphics, elementX + position, elementY, one, 0, 1, 1);
            blitHud(graphics, elementX + position + 1, elementY, two, 0, 1, 1);
            blitHud(graphics, elementX + position, elementY + element.getHeight() + 1, one, 0, 1, 1);
            blitHud(graphics, elementX + position + 1, elementY + element.getHeight() + 1, two, 0, 1, 1);
            position += 2;
            boxWidth -= 2;
        }
        int boxHeight = element.getHeight() + 2;
        position = 0;
        while (boxHeight >= 2) {
            blitHud(graphics, elementX, elementY + position, one, 0, 1, 1);
            blitHud(graphics, elementX, elementY + position + 1, two, 0, 1, 1);
            blitHud(graphics, elementX + element.getWidth() + 1, elementY + position, one, 0, 1, 1);
            blitHud(graphics, elementX + element.getWidth() + 1, elementY + position + 1, two, 0, 1, 1);
            position += 2;
            boxHeight -= 2;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int getButtonsY(HudElement element) {
        int elementY = HudHandler.getAbsoluteY(height, element) - 1;
        return elementY + element.getHeight() + 10 > height ? elementY - 11 : elementY + element.getHeight() + 4;
    }

    private int getButtonsX(HudElement element) {
        return HudHandler.getAbsoluteX(width, element) - 1 + 5;
    }

    private void drawButtons(GuiGraphics graphics, HudElement element, int mouseX, int mouseY) {
        int buttonsX = getButtonsX(element);
        int buttonsY = getButtonsY(element);
        if (element.canHide()) {
            boolean hover = isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY);
            blitHud(graphics, buttonsX, buttonsY, hover ^ element.isHidden() ? 125 : 116, 0, 9, 9);
            buttonsX += 11;
        }
        boolean down = element.getVerticalAnchor() != HudElement.AnchorVertical.BOTTOM;
        blitHud(graphics, buttonsX, buttonsY, down ^ isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY) ? 80 : 89, 0, 9, 9);
        buttonsX += 11;
        boolean right = element.getHorizontalAnchor() != HudElement.AnchorHorizontal.RIGHT;
        blitHud(graphics, buttonsX, buttonsY, right ^ isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY) ? 98 : 107, 0, 9, 9);
        buttonsX += 11;
        blitHud(graphics, buttonsX, buttonsY, 134, 0, 9, 9);
    }

    private void handleButtons(GuiGraphics graphics, HudElement element, int mouseX, int mouseY) {
        int buttonsX = getButtonsX(element);
        int buttonsY = getButtonsY(element);
        if (element.canHide()) {
            if (isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable(element.isHidden() ? "cyberware.gui.show" : "cyberware.gui.hide"), mouseX, mouseY);
                if (clicked) element.setHidden(!element.isHidden());
            }
            buttonsX += 11;
        }
        if (isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY)) {
            boolean down = element.getVerticalAnchor() != HudElement.AnchorVertical.BOTTOM;
            graphics.renderTooltip(font, Component.translatable(down ? "cyberware.gui.stick_down" : "cyberware.gui.stick_up"), mouseX, mouseY);
            if (clicked) flipVertical(element);
        }
        buttonsX += 11;
        if (isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY)) {
            boolean right = element.getHorizontalAnchor() != HudElement.AnchorHorizontal.RIGHT;
            graphics.renderTooltip(font, Component.translatable(right ? "cyberware.gui.stick_right" : "cyberware.gui.stick_left"), mouseX, mouseY);
            if (clicked) flipHorizontal(element);
        }
        buttonsX += 11;
        if (isInRegion(buttonsX, buttonsY, 9, 9, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.reset_hud"), mouseX, mouseY);
            if (clicked) element.reset();
        }
    }

    private static boolean isInRegion(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX >= x - 1 && pointX < x + width + 1 && pointY >= y - 1 && pointY < y + height + 1;
    }

    private void flipVertical(HudElement element) {
        int y = HudHandler.getAbsoluteY(height, element);
        element.setVerticalAnchor(element.getVerticalAnchor() == HudElement.AnchorVertical.BOTTOM ? HudElement.AnchorVertical.TOP : HudElement.AnchorVertical.BOTTOM);
        HudHandler.setYFromAbsolute(height, element, y);
    }

    private void flipHorizontal(HudElement element) {
        int x = HudHandler.getAbsoluteX(width, element);
        element.setHorizontalAnchor(element.getHorizontalAnchor() == HudElement.AnchorHorizontal.RIGHT ? HudElement.AnchorHorizontal.LEFT : HudElement.AnchorHorizontal.RIGHT);
        HudHandler.setXFromAbsolute(width, element, x);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (dragging == null) {
                dragging = hovered;
            }
            clicked = true;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && hovered != null) {
            flipVertical(hovered);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        CompoundTag layout = new CompoundTag();
        for (HudElement element : NeoForge.EVENT_BUS.post(new CyberwareHudDataEvent()).getElements()) {
            HudSaveData elementData = new HudSaveData();
            element.save(elementData);
            layout.put(element.getUniqueName(), elementData.tag());
        }
        CyberwareUserData data = ClientCyberware.getLocalData();
        if (data != null) {
            data.setHudData(layout);
        }
        PacketDistributor.sendToServer(new SyncHudDataPayload(layout));
        super.removed();
    }
}
