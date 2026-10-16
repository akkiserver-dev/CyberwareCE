package flaxbeard.cyberware;

import com.mojang.logging.LogUtils;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.block.TwoHighBlockHelper;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.command.ClearCyberwareCommand;
import flaxbeard.cyberware.common.handler.CyberwareDataHandler;
import flaxbeard.cyberware.common.handler.EssentialsMissingHandler;
import flaxbeard.cyberware.common.handler.LootHandler;
import flaxbeard.cyberware.common.item.ArmUpgradeItem;
import flaxbeard.cyberware.common.item.BoneUpgradeItem;
import flaxbeard.cyberware.common.item.BrainUpgradeItem;
import flaxbeard.cyberware.common.item.CyberheartItem;
import flaxbeard.cyberware.common.item.CybereyeUpgradeItem;
import flaxbeard.cyberware.common.item.CybereyesItem;
import flaxbeard.cyberware.common.item.CyberlimbItem;
import flaxbeard.cyberware.common.item.CyberwareArmorItem;
import flaxbeard.cyberware.common.item.FootUpgradeItem;
import flaxbeard.cyberware.common.item.HandUpgradeItem;
import flaxbeard.cyberware.common.item.HeartUpgradeItem;
import flaxbeard.cyberware.common.item.LegUpgradeItem;
import flaxbeard.cyberware.common.item.LowerOrgansUpgradeItem;
import flaxbeard.cyberware.common.item.LungsUpgradeItem;
import flaxbeard.cyberware.common.item.MuscleUpgradeItem;
import flaxbeard.cyberware.common.item.SkinUpgradeItem;
import flaxbeard.cyberware.common.item.SpiderEyeCyberware;
import flaxbeard.cyberware.common.network.CyberwareNetwork;
import flaxbeard.cyberware.common.network.SyncConfigPayload;
import flaxbeard.cyberware.common.registry.CyberwareArmorMaterials;
import flaxbeard.cyberware.common.registry.CyberwareAttachments;
import flaxbeard.cyberware.common.registry.CyberwareAttributes;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import flaxbeard.cyberware.common.registry.CyberwareCreativeTabs;
import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import flaxbeard.cyberware.common.registry.CyberwareEffects;
import flaxbeard.cyberware.common.registry.CyberwareEntities;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import flaxbeard.cyberware.common.registry.CyberwareRecipes;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(CyberwareMod.MOD_ID)
public class CyberwareMod {
    public static final String MOD_ID = "cyberware";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CyberwareMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CyberwareConfig.SPEC);

        CyberwareBlocks.BLOCKS.register(modBus);
        CyberwareItems.ITEMS.register(modBus);
        CyberwareBlockEntities.BLOCK_ENTITIES.register(modBus);
        CyberwareMenus.MENUS.register(modBus);
        CyberwareEntities.ENTITY_TYPES.register(modBus);
        CyberwareEffects.EFFECTS.register(modBus);
        CyberwareAttributes.ATTRIBUTES.register(modBus);
        CyberwareDataComponents.COMPONENTS.register(modBus);
        CyberwareAttachments.ATTACHMENT_TYPES.register(modBus);
        CyberwareArmorMaterials.ARMOR_MATERIALS.register(modBus);
        CyberwareCreativeTabs.CREATIVE_TABS.register(modBus);
        CyberwareRecipes.RECIPE_SERIALIZERS.register(modBus);
        CyberwareRecipes.CONDITION_CODECS.register(modBus);
        CyberwareRecipes.BIOME_MODIFIER_SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(CyberwareNetwork::register);
        modBus.addListener(CyberwareMod::registerCapabilities);
        modBus.addListener(CyberwareMod::createAttributes);
        modBus.addListener(CyberwareMod::modifyAttributes);
        modBus.addListener(CyberwareMod::registerSpawnPlacements);
        modBus.addListener(CyberwareMod::onConfigLoad);
        modBus.addListener(CyberwareMod::onConfigReload);

        for (Class<?> handler : new Class<?>[]{
                CyberwareDataHandler.class, EssentialsMissingHandler.class, LootHandler.class,
                CybereyesItem.class, CybereyeUpgradeItem.class, SpiderEyeCyberware.class, BrainUpgradeItem.class,
                CyberheartItem.class, HeartUpgradeItem.class, LungsUpgradeItem.class, LowerOrgansUpgradeItem.class,
                SkinUpgradeItem.class, MuscleUpgradeItem.class, BoneUpgradeItem.class, ArmUpgradeItem.class,
                HandUpgradeItem.class, LegUpgradeItem.class, FootUpgradeItem.class, CyberlimbItem.class}) {
            NeoForge.EVENT_BUS.register(handler);
        }
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ClearCyberwareCommand.register(event.getDispatcher()));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CyberwareDataHandler.registerGameRules();
            CyberwareAPI.linkCyberware(Items.SPIDER_EYE, new SpiderEyeCyberware());

            // washing a dyed trench coat in a cauldron restores its default color
            CauldronInteraction.WATER.map().put(CyberwareItems.TRENCH_COAT.get(), (state, level, pos, player, hand, stack) -> {
                DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
                if (color == null || color.rgb() == CyberwareArmorItem.DEFAULT_TRENCH_COAT_COLOR) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide()) {
                    stack.set(DataComponents.DYED_COLOR, new DyedItemColor(CyberwareArmorItem.DEFAULT_TRENCH_COAT_COLOR, true));
                    player.awardStat(Stats.CLEAN_ARMOR);
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            });
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CyberwareBlockEntities.CHARGER.get(), (charger, side) -> charger.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CyberwareBlockEntities.SCANNER.get(), (scanner, side) -> scanner.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CyberwareBlockEntities.BLUEPRINT_ARCHIVE.get(), (archive, side) -> archive.slots);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CyberwareBlockEntities.COMPONENT_BOX.get(), (box, side) -> box.slots);
        // both halves of the Engineering Table expose the inventory of the upper half
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            if (level.getBlockEntity(TwoHighBlockHelper.getUpperPos(state, pos)) instanceof EngineeringTableBlockEntity table) {
                return table.getItemHandler(side);
            }
            return null;
        }, CyberwareBlocks.ENGINEERING_TABLE.get());
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(CyberwareEntities.CYBER_ZOMBIE.get(), Zombie.createAttributes().build());
    }

    private static void modifyAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends net.minecraft.world.entity.LivingEntity> type : event.getTypes()) {
            if (!event.has(type, CyberwareAttributes.TOLERANCE)) {
                event.add(type, CyberwareAttributes.TOLERANCE);
            }
        }
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(CyberwareEntities.CYBER_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CyberwareConfig.SPEC) {
            CyberwareConfig.invalidateCaches();
        }
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CyberwareConfig.SPEC) {
            CyberwareConfig.invalidateCaches();
            // the config file was edited while the server runs: update its clients
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> PacketDistributor.sendToAllPlayers(new SyncConfigPayload(CyberwareConfig.Synced.fromConfig())));
            }
        }
    }
}
