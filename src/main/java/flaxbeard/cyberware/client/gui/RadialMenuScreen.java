package flaxbeard.cyberware.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.HotkeyHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.client.ClientCyberware;
import flaxbeard.cyberware.client.KeyBinds;
import flaxbeard.cyberware.client.handler.HudHandler;
import flaxbeard.cyberware.common.network.SyncHotkeyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The radial menu of active cyberware, opened while the menu key is held. Releasing the key uses the selected
 * item; clicking a segment allows binding a hotkey to it. The icons on the right open the HUD configuration
 * and the HUD color selection.
 */
public class RadialMenuScreen extends Screen {
    private static final float RADIUS = 100F;
    private static final float INNER_RADIUS = 40F;
    private static final int ROW_SIZE = 5;
    private static final float[][] COLOR_OPTIONS = {
            {0.0F, 1.0F, 1.0F},
            {76F / 255F, 1.0F, 0.0F},
            {1.0F, 216F / 255F, 0.0F},
            {1.0F, 182F / 255F, 66F / 255F},
            {212F / 255F, 119F / 255F, 1.0F},
            {1.0F, 0.0F, 0.0F},
            {61F / 255F, 174F / 255F, 1.0F},
            {1.0F, 89F / 255F, 232F / 255F},
            {28F / 255F, 1.0F, 156F / 255F},
            {1.0F, 1.0F, 1.0F}
    };

    private boolean movedWheel = false;
    private int selectedPart = -1;
    private int lastMousedOverPart = -1;
    private boolean editing = false;
    private boolean colorMode = false;
    private boolean close = false;
    private double scroll = 0;
    private EditBox hex;

    public RadialMenuScreen() {
        super(Component.translatable("cyberware.gui.menu"));
    }

    @Override
    protected void init() {
        int rows = (COLOR_OPTIONS.length + ROW_SIZE - 1) / ROW_SIZE;
        hex = new EditBox(font, width / 2 - 70, height / 2 - 100 + 30 * rows, 140, 20, Component.empty());
        hex.setMaxLength(6);
        hex.setValue(toHex(ClientCyberware.getHudColorHex()));
        hex.setFilter(text -> text.matches("[0-9A-Fa-f]*"));
        hex.setResponder(text -> {
            if (text.length() == 6) {
                ClientCyberware.setHudColor(Integer.parseInt(text, 16));
            }
        });
        hex.setVisible(colorMode);
        addRenderableWidget(hex);
    }

    private static String toHex(int color) {
        StringBuilder text = new StringBuilder(Integer.toHexString(color).toUpperCase());
        while (text.length() < 6) {
            text.insert(0, "0");
        }
        return text.toString();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // the menu is drawn over the world without a background
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        CyberwareUserData data = ClientCyberware.getLocalData();
        if (data == null) return;

        int wheel = (int) Math.signum(scroll);
        scroll = 0;
        if (!movedWheel && wheel != 0 && !editing && !colorMode) {
            movedWheel = true;
            if (selectedPart == -1 && wheel > 0) {
                wheel = 0;
            }
        }

        // the three icons on the right
        float[] mainColor = ClientCyberware.getHudColor();
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F + RADIUS, height / 2F - RADIUS, 0F);
        graphics.pose().scale(2F, 2F, 2F);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(mainColor[0], mainColor[1], mainColor[2], 1.0F);
        graphics.blit(HudHandler.HUD_TEXTURE, 1, 0, 81, 10, 7, 8);
        graphics.blit(HudHandler.HUD_TEXTURE, 0, 9, 88, 10, 8, 8);
        graphics.blit(HudHandler.HUD_TEXTURE, 0, 18, 97, 10, 8, 8);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();

        if (colorMode) {
            for (int index = 0; index < COLOR_OPTIONS.length; index++) {
                float[] color = COLOR_OPTIONS[index];
                RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);
                graphics.blit(HudHandler.HUD_TEXTURE, width / 2 + colorOffsetX(index), height / 2 + colorOffsetY(index), 0, 236, 20, 20);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            renderPie(graphics, data, mouseX, mouseY, wheel, mainColor);
        }

        int iconsX = (int) (width / 2F + RADIUS);
        int iconsY = (int) (height / 2F - RADIUS);
        if (isInRegion(iconsX + 2, iconsY, 12, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("cyberware.gui.keybind.0"),
                    Component.translatable("cyberware.gui.keybind.1"),
                    Component.translatable("cyberware.gui.keybind.2"),
                    Component.translatable("cyberware.gui.keybind.3"),
                    Component.translatable("cyberware.gui.keybind.4")), mouseX, mouseY);
        } else if (isInRegion(iconsX, iconsY + 18, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.open_hud_config"), mouseX, mouseY);
        } else if (isInRegion(iconsX, iconsY + 36, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.open_color_change"), mouseX, mouseY);
        }
    }

    private void renderPie(GuiGraphics graphics, CyberwareUserData data, int mouseX, int mouseY, int wheel, float[] mainColor) {
        List<ItemStack> items = data.getActiveItems();
        int pieces = items.size();
        if (pieces == 0) return;
        int centerX = width / 2;
        int centerY = height / 2;

        if (movedWheel && !editing) {
            selectedPart = Math.floorMod(selectedPart - wheel, pieces);
        }

        float degreesPerPiece = 360F / pieces;
        float mouseDistance = (float) Math.sqrt((centerX - mouseX) * (centerX - mouseX) + (centerY - mouseY) * (centerY - mouseY));
        float mouseAngle = (float) ((Math.atan2(mouseY - centerY, mouseX - centerX) * 180F / Math.PI) + 360F) % 360F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int piece = 0; piece < pieces; piece++) {
            float rotation = (degreesPerPiece * piece + 270) % 360;
            if (!editing) {
                if (mouseDistance > INNER_RADIUS) {
                    movedWheel = false;
                    if (pieces == 1 || (mouseAngle > rotation && mouseAngle < rotation + degreesPerPiece && lastMousedOverPart != piece)) {
                        lastMousedOverPart = piece;
                        selectedPart = piece;
                    }
                } else {
                    lastMousedOverPart = -1;
                    if (!movedWheel) {
                        selectedPart = -1;
                    }
                }
            }

            boolean selected = piece == selectedPart;
            float radius = RADIUS + (selected ? 10 : 0);
            float innerRadius = INNER_RADIUS + (selected ? 10 : 0);
            ItemStack stack = items.get(piece);
            float[] itemColor = ((MenuItem) stack.getItem()).getColor(stack);
            float[] color = itemColor == null ? mainColor : itemColor;
            float alpha = selected ? 0.8F : 0.5F;

            // a ring segment, drawn as a strip between the inner and outer arcs
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            int steps = Math.max(1, (int) Math.ceil(degreesPerPiece));
            for (int step = 0; step <= steps; step++) {
                double radians = Math.toRadians(rotation + degreesPerPiece * step / steps);
                float cos = (float) Math.cos(radians);
                float sin = (float) Math.sin(radians);
                buffer.addVertex(matrix, centerX + cos * innerRadius, centerY + sin * innerRadius, 0).setColor(color[0], color[1], color[2], alpha);
                buffer.addVertex(matrix, centerX + cos * radius, centerY + sin * radius, 0).setColor(color[0], color[1], color[2], alpha);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        float scale = pieces > 8 ? (pieces > 16 ? .5F : 1F) : 2F;
        float itemRadiusBase = INNER_RADIUS + (RADIUS - INNER_RADIUS) / 2F;
        for (int piece = 0; piece < pieces; piece++) {
            ItemStack stack = items.get(piece);
            float itemRadius = piece == selectedPart ? itemRadiusBase + 10F : itemRadiusBase;
            double radians = Math.toRadians((degreesPerPiece * (piece + .5F) + 270) % 360);
            float offset = 8F * scale;
            int boundKey = HotkeyHelper.getHotkey(stack);
            float yOffset = boundKey == HotkeyHelper.NONE ? font.lineHeight / 2F : 0;
            float x = centerX + (float) Math.cos(radians) * itemRadius;
            float y = centerY + (float) Math.sin(radians) * itemRadius;

            graphics.pose().pushPose();
            graphics.pose().translate(x - offset, y - offset + yOffset, 0);
            graphics.pose().scale(scale, scale, scale);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();

            String keyText = null;
            if (piece == selectedPart && editing) {
                if ((minecraft.player.tickCount / 4) % 2 == 0) keyText = "__";
            } else if (boundKey != HotkeyHelper.NONE) {
                keyText = getKeyName(boundKey);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            if (keyText != null) {
                graphics.drawString(font, keyText, (int) (x - font.width(keyText) / 2F), (int) (y + offset), 0xFFFFFF);
            }
            String label = I18n.get(((MenuItem) stack.getItem()).getMenuLabel(stack));
            graphics.drawString(font, label, (int) (x - font.width(label) / 2F), (int) (y - offset + yOffset - font.lineHeight), 0xFFFFFF);
            graphics.pose().popPose();
        }

        if (selectedPart >= 0 && selectedPart < pieces) {
            Component name = items.get(selectedPart).getHoverName();
            graphics.drawString(font, name, (width - font.width(name)) / 2, (int) (height / 2F - 30 - RADIUS), 0xFFFFFF);
        }
    }

    /**
     * @return the display name of a hotkey code, see {@link HotkeyHelper}
     */
    public static String getKeyName(int key) {
        if (HotkeyHelper.isMouseButton(key)) {
            return InputConstants.Type.MOUSE.getOrCreate(key - HotkeyHelper.MOUSE_OFFSET).getDisplayName().getString();
        }
        if (HotkeyHelper.isShifted(key)) {
            return "SHIFT + " + InputConstants.Type.KEYSYM.getOrCreate(key - HotkeyHelper.SHIFT_OFFSET).getDisplayName().getString();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    private static int colorOffsetX(int index) {
        return -10 + (index % ROW_SIZE - ROW_SIZE / 2) * 30;
    }

    private static int colorOffsetY(int index) {
        return -100 + 30 * (index / ROW_SIZE);
    }

    private static boolean isInRegion(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX >= x - 1 && pointX < x + width + 1 && pointY >= y - 1 && pointY < y + height + 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll += scrollY;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorMode && hex.mouseClicked(mouseX, mouseY, button)) {
            setFocused(hex);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !editing) {
            int iconsX = (int) (width / 2F + RADIUS);
            int iconsY = (int) (height / 2F - RADIUS);
            if (isInRegion(iconsX, iconsY + 18, 16, 16, mouseX, mouseY)) {
                minecraft.setScreen(new HudConfigurationScreen());
                return true;
            }
            if (isInRegion(iconsX, iconsY + 36, 16, 16, mouseX, mouseY)) {
                colorMode = !colorMode;
                hex.setVisible(colorMode);
                return true;
            }
            if (colorMode) {
                for (int index = 0; index < COLOR_OPTIONS.length; index++) {
                    if (isInRegion(width / 2 + colorOffsetX(index), height / 2 + colorOffsetY(index), 20, 20, mouseX, mouseY)) {
                        ClientCyberware.setHudColor(COLOR_OPTIONS[index]);
                        hex.setValue(toHex(ClientCyberware.getHudColorHex()));
                        return true;
                    }
                }
            } else {
                editing = true;
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (editing) {
                editing = false;
            } else if (selectedPart != -1) {
                CyberwareUserData data = ClientCyberware.getLocalData();
                if (data != null && selectedPart < data.getActiveItems().size()) {
                    HotkeyHelper.removeHotkey(data, data.getActiveItems().get(selectedPart));
                    PacketDistributor.sendToServer(new SyncHotkeyPayload(selectedPart, SyncHotkeyPayload.REMOVE));
                }
            }
            return true;
        }
        if (button > GLFW.GLFW_MOUSE_BUTTON_RIGHT && editing && selectedPart != -1) {
            assignHotkey(button + HotkeyHelper.MOUSE_OFFSET);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void assignHotkey(int code) {
        CyberwareUserData data = ClientCyberware.getLocalData();
        if (data != null && selectedPart >= 0 && selectedPart < data.getActiveItems().size()) {
            HotkeyHelper.removeHotkey(data, code);
            HotkeyHelper.assignHotkey(data, data.getActiveItems().get(selectedPart), code);
            PacketDistributor.sendToServer(new SyncHotkeyPayload(selectedPart, code));
        }
        editing = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editing) {
                editing = false;
            } else {
                close = true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || KeyBinds.MENU.matches(keyCode, scanCode)) {
            return true;
        }
        if (selectedPart != -1 && editing) {
            assignHotkey(keyCode + (hasShiftDown() ? HotkeyHelper.SHIFT_OFFSET : 0));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (close) {
            onClose();
            return;
        }
        CyberwareUserData data = ClientCyberware.getLocalData();
        long window = minecraft.getWindow().getWindow();
        boolean menuKeyDown = KeyBinds.MENU.isUnbound() || InputConstants.isKeyDown(window, KeyBinds.MENU.getKey().getValue());
        if ((!menuKeyDown && !editing && !colorMode) || (data != null && data.getNumActiveItems() < 1)) {
            if (data != null && selectedPart >= 0 && selectedPart < data.getActiveItems().size() && !editing) {
                ClientCyberware.useActiveItem(minecraft.player, data.getActiveItems().get(selectedPart));
            }
            onClose();
            return;
        }

        // keep moving while the menu is open
        for (KeyMapping key : new KeyMapping[]{minecraft.options.keyUp, minecraft.options.keyLeft, minecraft.options.keyDown,
                minecraft.options.keyRight, minecraft.options.keyShift, minecraft.options.keySprint, minecraft.options.keyJump}) {
            InputConstants.Key input = key.getKey();
            boolean down = false;
            if (!editing) {
                down = input.getType() == InputConstants.Type.MOUSE
                        ? GLFW.glfwGetMouseButton(window, input.getValue()) == GLFW.GLFW_PRESS
                        : input.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, input.getValue());
            }
            KeyMapping.set(input, down);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ClientCyberware.syncHudColor();
        super.removed();
    }
}
