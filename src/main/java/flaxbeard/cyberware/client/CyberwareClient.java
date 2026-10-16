package flaxbeard.cyberware.client;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.gui.BlueprintArchiveScreen;
import flaxbeard.cyberware.client.gui.ComponentBoxScreen;
import flaxbeard.cyberware.client.gui.EngineeringTableScreen;
import flaxbeard.cyberware.client.gui.ScannerScreen;
import flaxbeard.cyberware.client.gui.SurgeryScreen;
import flaxbeard.cyberware.client.handler.CreativeMenuHandler;
import flaxbeard.cyberware.client.handler.CyberwareClientEffects;
import flaxbeard.cyberware.client.handler.FineManipulatorsButtonHandler;
import flaxbeard.cyberware.client.handler.HudHandler;
import flaxbeard.cyberware.client.handler.RadialMenuHandler;
import flaxbeard.cyberware.client.handler.TooltipHandler;
import flaxbeard.cyberware.client.render.CyberZombieRenderer;
import flaxbeard.cyberware.client.render.CyberlimbLayer;
import flaxbeard.cyberware.client.render.CyberwareModelLayers;
import flaxbeard.cyberware.client.render.EngineeringTableRenderer;
import flaxbeard.cyberware.client.render.PlayerModelHandler;
import flaxbeard.cyberware.client.render.RadioTowerRenderer;
import flaxbeard.cyberware.client.render.ScannerRenderer;
import flaxbeard.cyberware.client.render.SurgeryChamberRenderer;
import flaxbeard.cyberware.client.render.TrenchCoatModel;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.effect.CyberwareMobEffect;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.item.CyberwareArmorItem;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareEffects;
import flaxbeard.cyberware.common.registry.CyberwareEntities;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

@Mod(value = CyberwareMod.MOD_ID, dist = Dist.CLIENT)
public class CyberwareClient {
    private static final ResourceLocation POTIONS_TEXTURE = CyberwareAPI.id("textures/gui/potions.png");

    public CyberwareClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CyberwareClientConfig.SPEC);

        modBus.addListener(CyberwareClient::clientSetup);
        modBus.addListener(CyberwareClient::registerScreens);
        modBus.addListener(CyberwareClient::registerRenderers);
        modBus.addListener(CyberwareClient::addLayers);
        modBus.addListener(CyberwareModelLayers::register);
        modBus.addListener(KeyBinds::register);
        modBus.addListener(CyberwareClient::registerItemColors);
        modBus.addListener(CyberwareClient::registerGuiLayers);
        modBus.addListener(CyberwareClient::registerClientExtensions);
        modBus.addListener(CyberwareClient::registerReloadListeners);

        NeoForge.EVENT_BUS.register(HudHandler.class);
        NeoForge.EVENT_BUS.register(CyberwareClientEffects.class);
        NeoForge.EVENT_BUS.register(RadialMenuHandler.class);
        NeoForge.EVENT_BUS.register(PlayerModelHandler.class);
        NeoForge.EVENT_BUS.register(TooltipHandler.class);
        NeoForge.EVENT_BUS.register(CreativeMenuHandler.class);
        NeoForge.EVENT_BUS.register(FineManipulatorsButtonHandler.class);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> CyberwareConfig.setFromServer(null));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation scavenged = CyberwareAPI.id("scavenged");
            for (var holder : CyberwareItems.getCyberware()) {
                ItemProperties.register(holder.get(), scavenged, (stack, level, entity, seed) ->
                        CyberwareAPI.isCyberware(stack) && CyberwareAPI.getCyberware(stack).getQuality(stack) == CyberwareAPI.QUALITY_SCAVENGED ? 1.0F : 0.0F);
            }
            ItemProperties.register(CyberwareItems.BLUEPRINT.get(), CyberwareAPI.id("blank"), (stack, level, entity, seed) ->
                    BlueprintItem.isBlank(stack) ? 1.0F : 0.0F);
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CyberwareMenus.SURGERY.get(), SurgeryScreen::new);
        event.register(CyberwareMenus.ENGINEERING_TABLE.get(), EngineeringTableScreen::new);
        event.register(CyberwareMenus.SCANNER.get(), ScannerScreen::new);
        event.register(CyberwareMenus.BLUEPRINT_ARCHIVE.get(), BlueprintArchiveScreen::new);
        event.register(CyberwareMenus.COMPONENT_BOX.get(), ComponentBoxScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CyberwareBlockEntities.SURGERY_CHAMBER.get(), SurgeryChamberRenderer::new);
        event.registerBlockEntityRenderer(CyberwareBlockEntities.SCANNER.get(), ScannerRenderer::new);
        event.registerBlockEntityRenderer(CyberwareBlockEntities.ENGINEERING_TABLE.get(), EngineeringTableRenderer::new);
        event.registerBlockEntityRenderer(CyberwareBlockEntities.RADIO_POST.get(), RadioTowerRenderer::new);
        event.registerEntityRenderer(CyberwareEntities.CYBER_ZOMBIE.get(), CyberZombieRenderer::new);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new CyberlimbLayer(renderer));
            }
        }
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex > 0 ? -1 : DyedItemColor.getOrDefault(stack, 0xFF000000 | CyberwareArmorItem.DEFAULT_TRENCH_COAT_COLOR),
                CyberwareItems.TRENCH_COAT.get());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(CyberwareAPI.id("blackout"), CyberwareClientEffects::renderBlackout);
        event.registerBelow(VanillaGuiLayers.CHAT, CyberwareAPI.id("hud"), HudHandler::render);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> PlayerModelHandler.clearModels());
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (var effect : new CyberwareMobEffect[]{(CyberwareMobEffect) CyberwareEffects.NEUROPOZYNE.get(), (CyberwareMobEffect) CyberwareEffects.REJECTION.get()}) {
            event.registerMobEffect(new MobEffectIcon(effect.getIconIndex()), effect);
        }

        event.registerItem(new IClientItemExtensions() {
            @Nullable
            private TrenchCoatModel<LivingEntity> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) {
                    model = new TrenchCoatModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(CyberwareModelLayers.TRENCH_COAT));
                }
                return model;
            }

            @Override
            public int getDefaultDyeColor(ItemStack stack) {
                return 0xFF000000 | CyberwareArmorItem.DEFAULT_TRENCH_COAT_COLOR;
            }
        }, CyberwareItems.TRENCH_COAT.get());
    }

    /**
     * Draws the effect icon from the mod's potion sheet, as the original did.
     */
    private record MobEffectIcon(int index) implements IClientMobEffectExtensions {
        private void blit(GuiGraphics guiGraphics, int x, int y) {
            guiGraphics.blit(POTIONS_TEXTURE, x, y, index * 18, 198, 18, 18);
        }

        @Override
        public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
            blit(guiGraphics, x, y + 7);
            return true;
        }

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            blit(guiGraphics, x + 3, y + 3);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            return true;
        }
    }
}
