package flaxbeard.cyberware.client.gui;

import flaxbeard.cyberware.common.menu.ChestLikeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * A chest style screen whose texture has the container rows on top and the player inventory at v = 126.
 */
public abstract class ChestLikeScreen<T extends ChestLikeMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;

    protected ChestLikeScreen(T menu, Inventory playerInventory, Component title, ResourceLocation texture) {
        super(menu, playerInventory, title);
        this.texture = texture;
        this.imageHeight = 114 + menu.getRows() * 18;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int rowsHeight = menu.getRows() * 18 + 17;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, rowsHeight);
        graphics.blit(texture, leftPos, topPos + rowsHeight, 0, 126, imageWidth, 96);
    }
}
