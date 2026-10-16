package flaxbeard.cyberware.client.gui;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.menu.BlueprintArchiveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BlueprintArchiveScreen extends ChestLikeScreen<BlueprintArchiveMenu> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/blueprint_archive.png");

    public BlueprintArchiveScreen(BlueprintArchiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        renderBlueprintIcons(graphics, menu.getRows() * 9, index -> menu.getArchiveStack(index), 8, 18, 9);
    }

    /**
     * Draws the item a blueprint is for on top of each blueprint in a grid of slots.
     */
    static void renderBlueprintIcons(GuiGraphics graphics, int count, java.util.function.IntFunction<ItemStack> stacks, int x, int y, int columns) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200F);
        for (int index = 0; index < count; index++) {
            ItemStack stack = stacks.apply(index);
            if (stack.getItem() instanceof Blueprint blueprint) {
                graphics.renderItem(blueprint.getTarget(stack), x + 18 * (index % columns), y + 18 * (index / columns));
            }
        }
        graphics.pose().popPose();
    }
}
