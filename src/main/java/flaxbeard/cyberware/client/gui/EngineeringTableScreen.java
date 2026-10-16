package flaxbeard.cyberware.client.gui;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.block.entity.BlueprintArchiveBlockEntity;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.menu.EngineeringTableMenu;
import flaxbeard.cyberware.common.network.EngineeringDestroyPayload;
import flaxbeard.cyberware.common.network.EngineeringSwitchPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class EngineeringTableScreen extends AbstractContainerScreen<EngineeringTableMenu> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/engineering.png");
    private static final int GHOST_OVERLAY = 0x30FFFFFF;

    private final int offset;
    private PageButton previousArchive;
    private PageButton nextArchive;
    private PageButton previousBox;
    private PageButton nextBox;

    public EngineeringTableScreen(EngineeringTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.offset = menu.getOffset();
        this.imageWidth = 176 + (menu.hasArchive() ? EngineeringTableMenu.PANEL_WIDTH : 0) + offset;
        this.titleLabelX = offset + 8;
        this.inventoryLabelX = offset + 8;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new SmashButton(leftPos + offset + 39, topPos + 34));
        previousArchive = addRenderableWidget(new PageButton(leftPos + offset + 180, topPos + 131, false, () -> switchPanel(false, false)));
        nextArchive = addRenderableWidget(new PageButton(leftPos + offset + 216, topPos + 131, true, () -> switchPanel(true, false)));
        previousBox = addRenderableWidget(new PageButton(leftPos + 7, topPos + 131, false, () -> switchPanel(false, true)));
        nextBox = addRenderableWidget(new PageButton(leftPos + 43, topPos + 131, true, () -> switchPanel(true, true)));
        updateButtons();
    }

    private void updateButtons() {
        previousArchive.visible = nextArchive.visible = menu.hasArchive() && menu.canSwitchArchive();
        previousBox.visible = nextBox.visible = menu.hasComponentBox() && menu.canSwitchBox();
    }

    private void switchPanel(boolean forward, boolean isComponentBox) {
        PacketDistributor.sendToServer(new EngineeringSwitchPayload(forward, isComponentBox));
        if (isComponentBox) {
            menu.switchBox(forward);
        } else {
            menu.switchArchive(forward);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        menu.stillValid(minecraft.player);
        updateButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        EngineeringTableBlockEntity table = menu.getTable();
        if (isHovering(offset + 39, 34, 21, 21, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("cyberware.gui.destroy"));
            if (!table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_PAPER).isEmpty()) {
                float chance = EngineeringTableBlockEntity.getBlueprintChance(minecraft.level, table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_DESTROY));
                tooltip.add(Component.translatable("cyberware.gui.destroy_chance", Math.round(chance * 100F) / 100F + "%"));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else if (isHovering(offset + 15, 20, 16, 16, mouseX, mouseY) && table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_DESTROY).isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.to_destroy"), mouseX, mouseY);
        } else if (isHovering(offset + 15, 53, 16, 16, mouseX, mouseY) && table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_PAPER).isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.paper"), mouseX, mouseY);
        } else if (isHovering(offset + 115, 53, 16, 16, mouseX, mouseY) && table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_BLUEPRINT).isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.blueprint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        BlueprintArchiveBlockEntity archive = menu.getArchive();
        if (menu.hasArchive() && archive != null) {
            graphics.drawString(font, truncate(archive.getName().getString()), offset + 180, 10, 0x404040, false);
            BlueprintArchiveScreen.renderBlueprintIcons(graphics, EngineeringTableMenu.ARCHIVE_SLOTS,
                    index -> menu.getSlot(EngineeringTableMenu.ARCHIVE_START + index).getItem(), offset + 181, 22, 3);
        }
        if (menu.hasComponentBox()) {
            graphics.drawString(font, truncate(menu.getComponentBoxName().getString()), 7, 10, 0x404040, false);
        }

        renderMissingRequirements(graphics);
    }

    /**
     * Shows the components still needed by the loaded blueprint as faded items in the empty crafting slots.
     */
    private void renderMissingRequirements(GuiGraphics graphics) {
        EngineeringTableBlockEntity table = menu.getTable();
        ItemStack blueprintStack = table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_BLUEPRINT);
        if (!(blueprintStack.getItem() instanceof Blueprint blueprint)) return;

        List<ItemStack> missing = new ArrayList<>();
        for (ItemStack required : blueprint.getRequirementsForDisplay(blueprintStack)) {
            int count = required.getCount();
            for (int slot = EngineeringTableBlockEntity.SLOT_CRAFTING_START; slot < EngineeringTableBlockEntity.SLOT_CRAFTING_END; slot++) {
                ItemStack crafting = table.slots.getStackInSlot(slot);
                if (ItemStack.isSameItem(crafting, required)
                        && (required.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(required, crafting))) {
                    count -= crafting.getCount();
                }
            }
            if (count > 0) {
                missing.add(required.copyWithCount(count));
            }
        }

        int index = 0;
        for (int slot = EngineeringTableBlockEntity.SLOT_CRAFTING_START; slot < EngineeringTableBlockEntity.SLOT_CRAFTING_END && index < missing.size(); slot++) {
            if (!table.slots.getStackInSlot(slot).isEmpty()) continue;
            ItemStack stack = missing.get(index++);
            int x = offset + 71 + 18 * (slot % 2);
            int y = -1 + 18 * (slot / 2);
            graphics.renderFakeItem(stack, x, y);
            graphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, GHOST_OVERLAY);
            graphics.renderItemDecorations(font, stack, x, y, "+" + stack.getCount());
        }
    }

    private static String truncate(String name) {
        if (name.length() <= 11) return name;
        return name.substring(0, 9).trim() + "...";
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos + offset, topPos, 0, 0, menu.hasArchive() ? 241 : 176, imageHeight);
        if (menu.hasComponentBox()) {
            graphics.blit(ComponentBoxScreen.TEXTURE, leftPos, topPos, 176, 0, EngineeringTableMenu.PANEL_WIDTH, imageHeight);
        }
    }

    private class SmashButton extends AbstractButton {
        private SmashButton(int x, int y) {
            super(x, y, 21, 21, Component.translatable("cyberware.gui.destroy"));
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new EngineeringDestroyPayload(menu.getTable().getBlockPos()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean pressed = isHovered() && minecraft.mouseHandler.isLeftPressed();
            graphics.blit(TEXTURE, getX(), getY(), pressed ? 0 : 39, pressed ? 166 : 34, 21, 21);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static class PageButton extends AbstractButton {
        private final boolean isForward;
        private final Runnable action;

        private PageButton(int x, int y, boolean isForward, Runnable action) {
            super(x, y, 23, 13, Component.empty());
            this.isForward = isForward;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(TEXTURE, getX(), getY(), isHovered() ? 44 : 21, isForward ? 166 : 179, 23, 13);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
