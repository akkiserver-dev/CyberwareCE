package flaxbeard.cyberware.client.handler;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.registry.CyberwareCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Adds buttons next to the creative cyberware tab to switch between scavenged and manufactured cyberware.
 */
public final class CreativeMenuHandler {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/creative_expansion.png");
    private static final int PANEL_WIDTH = 29;
    private static final int PANEL_HEIGHT = 129;

    private CreativeMenuHandler() {
    }

    private static boolean isCyberwareTab(Screen screen) {
        return screen instanceof CreativeModeInventoryScreen && CreativeModeInventoryScreen.selectedTab == CyberwareCreativeTabs.CYBERWARE.get();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen screen) {
            event.addListener(new QualityButton(screen, true));
            event.addListener(new QualityButton(screen, false));
        }
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        boolean visible = isCyberwareTab(screen);
        for (var child : screen.children()) {
            if (child instanceof QualityButton button) {
                button.visible = visible;
                button.active = visible;
                // follow the screen, its position changes with the window size
                button.setPosition(screen.getGuiLeft() + screen.getXSize() + 4, screen.getGuiTop() + 8 + button.offset() * 23);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        if (!isCyberwareTab(event.getContainerScreen())) return;
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) event.getContainerScreen();
        event.getGuiGraphics().blit(TEXTURE, screen.getGuiLeft() + screen.getXSize(), screen.getGuiTop(), 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    /**
     * Moves the effect list so it does not cover the panel.
     */
    @SubscribeEvent
    public static void onRenderEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (isCyberwareTab(event.getScreen())) {
            event.addHorizontalOffset(PANEL_WIDTH);
        }
    }

    private static void select(CreativeModeInventoryScreen screen, boolean scavenged) {
        if (CyberwareCreativeTabs.showScavenged == scavenged) return;
        CyberwareCreativeTabs.showScavenged = scavenged;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        CreativeModeTab tab = CyberwareCreativeTabs.CYBERWARE.get();
        boolean hasPermissions = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(player.connection.enabledFeatures(), hasPermissions, player.level().registryAccess()));
        screen.selectTab(tab);
    }

    private static class QualityButton extends AbstractButton {
        private final CreativeModeInventoryScreen screen;
        private final boolean scavenged;

        private QualityButton(CreativeModeInventoryScreen screen, boolean scavenged) {
            super(0, 0, 18, 18, Component.empty());
            this.screen = screen;
            this.scavenged = scavenged;
            setTooltip(Tooltip.create(Component.translatable(
                    (scavenged ? CyberwareAPI.QUALITY_SCAVENGED : CyberwareAPI.QUALITY_MANUFACTURED).getTranslationKey())));
        }

        private int offset() {
            return scavenged ? 0 : 1;
        }

        @Override
        public void onPress() {
            select(screen, scavenged);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean down = CyberwareCreativeTabs.showScavenged == scavenged || (isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed());
            int u = down ? 29 : 4;
            int v = (down ? 0 : 8) + offset() * (down ? 18 : 23);
            guiGraphics.blit(TEXTURE, getX(), getY(), u, v, 18, 18);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
