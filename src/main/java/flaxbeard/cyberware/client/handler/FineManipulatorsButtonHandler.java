package flaxbeard.cyberware.client.handler;

import flaxbeard.cyberware.common.menu.FineManipulatorsMenu;
import flaxbeard.cyberware.common.network.OpenFineManipulatorsPayload;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds a button to the inventory that opens the crafting grid of the Crafting Hands.
 */
public final class FineManipulatorsButtonHandler {
    private static final int SIZE = 20;

    private FineManipulatorsButtonHandler() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new FineManipulatorsButton(screen));
        }
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        LocalPlayer player = Minecraft.getInstance().player;
        boolean visible = player != null && FineManipulatorsMenu.canUse(player);
        for (var child : screen.children()) {
            if (child instanceof FineManipulatorsButton button) {
                button.visible = visible;
                button.active = visible;
                // the screen moves when the recipe book is opened
                button.setPosition(screen.getGuiLeft() + screen.getXSize() - SIZE - 4, screen.getGuiTop() - SIZE);
            }
        }
    }

    private static class FineManipulatorsButton extends AbstractButton {
        private final ItemStack icon;

        private FineManipulatorsButton(InventoryScreen screen) {
            super(screen.getGuiLeft() + screen.getXSize() - SIZE - 4, screen.getGuiTop() - SIZE, SIZE, SIZE,
                    Component.translatable("cyberware.gui.fine_manipulators"));
            this.icon = new ItemStack(CyberwareItems.CRAFT_HANDS.get());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new OpenFineManipulatorsPayload());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.renderItem(icon, getX() + 2, getY() + 2);
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            // only the icon is shown
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
