package flaxbeard.cyberware.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import flaxbeard.cyberware.common.menu.ScannerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScannerScreen extends AbstractContainerScreen<ScannerMenu> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/scanner.png");
    private static final String[] DOTS = {"", ".", "..", "...", "....", "....."};
    private static final int TEXT_COLOR = 0x1DA9C1;

    private int messageNumber = -1;
    private boolean resetLast = false;

    public ScannerScreen(ScannerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        ScannerBlockEntity scanner = menu.getScanner();
        if (isHovering(35, 53, 16, 16, mouseX, mouseY) && scanner.slots.getStackInSlot(ScannerBlockEntity.SLOT_INPUT).isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.to_scan"), mouseX, mouseY);
        }
        if (isHovering(15, 53, 16, 16, mouseX, mouseY) && scanner.slots.getStackInSlot(ScannerBlockEntity.SLOT_PAPER).isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.paper"), mouseX, mouseY);
        }
        int ticks = menu.getTicks();
        if (ticks > 0 && isHovering(5, 32, 162, 9, mouseX, mouseY)) {
            int ticksLeft = CyberwareConfig.get(minecraft.level).scannerTime() - ticks;
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.time_left", ticksLeft / 1200, (ticksLeft % 1200) / 20), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        ScannerBlockEntity scanner = menu.getScanner();
        ItemStack toScan = scanner.slots.getStackInSlot(ScannerBlockEntity.SLOT_INPUT);
        if (!toScan.isEmpty() && scanner.slots.getStackInSlot(ScannerBlockEntity.SLOT_OUTPUT).isEmpty()) {
            int maxMessage = getMessageCount();
            int dots = (minecraft.player.tickCount / 10) % DOTS.length;
            if ((dots == 0 && !resetLast) || messageNumber == -1 || messageNumber >= maxMessage) {
                if (dots == 0) resetLast = true;
                messageNumber = minecraft.player.getRandom().nextInt(Math.max(1, maxMessage));
            }
            if (dots != 0) resetLast = false;
            graphics.drawString(font, I18n.get("cyberware.gui.scanner_saying." + messageNumber) + DOTS[dots], 6, 20, 0x1F6D7C, false);
        }

        graphics.drawString(font, title, 6, 7, TEXT_COLOR, false);

        float chance = ScannerBlockEntity.getChance(minecraft.level, toScan);
        String percent = I18n.get("cyberware.gui.percent", Math.round(chance * 100F) / 100F + "%");
        graphics.drawString(font, percent, imageWidth - 6 - font.width(percent), 7, TEXT_COLOR, false);

        int progress = (int) Math.ceil(menu.getTicks() / (float) Math.max(1, CyberwareConfig.get(minecraft.level).scannerTime()) * 162);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.6F);
        graphics.blit(TEXTURE, 5, 32, 0, 175, progress, 9);
        graphics.blit(TEXTURE, 5 + progress, 32, progress, 166, 162 - progress, 9);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int getMessageCount() {
        try {
            return Integer.parseInt(I18n.get("cyberware.gui.scanner_saying.count")) - 1;
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
