package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.block.entity.BlueprintArchiveBlockEntity;
import flaxbeard.cyberware.common.block.entity.ChargerBlockEntity;
import flaxbeard.cyberware.common.block.entity.ComponentBoxBlockEntity;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.block.entity.RadioBeaconBlockEntity;
import flaxbeard.cyberware.common.block.entity.RadioBlockEntity;
import flaxbeard.cyberware.common.block.entity.RadioPostBlockEntity;
import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.block.entity.SurgeryChamberBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CyberwareMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurgeryBlockEntity>> SURGERY = BLOCK_ENTITIES.register("surgery",
            () -> BlockEntityType.Builder.of(SurgeryBlockEntity::new, CyberwareBlocks.SURGERY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurgeryChamberBlockEntity>> SURGERY_CHAMBER = BLOCK_ENTITIES.register("surgery_chamber",
            () -> BlockEntityType.Builder.of(SurgeryChamberBlockEntity::new, CyberwareBlocks.SURGERY_CHAMBER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER = BLOCK_ENTITIES.register("charger",
            () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, CyberwareBlocks.CHARGER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineeringTableBlockEntity>> ENGINEERING_TABLE = BLOCK_ENTITIES.register("engineering_table",
            () -> BlockEntityType.Builder.of(EngineeringTableBlockEntity::new, CyberwareBlocks.ENGINEERING_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScannerBlockEntity>> SCANNER = BLOCK_ENTITIES.register("scanner",
            () -> BlockEntityType.Builder.of(ScannerBlockEntity::new, CyberwareBlocks.SCANNER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintArchiveBlockEntity>> BLUEPRINT_ARCHIVE = BLOCK_ENTITIES.register("blueprint_archive",
            () -> BlockEntityType.Builder.of(BlueprintArchiveBlockEntity::new, CyberwareBlocks.BLUEPRINT_ARCHIVE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComponentBoxBlockEntity>> COMPONENT_BOX = BLOCK_ENTITIES.register("component_box",
            () -> BlockEntityType.Builder.of(ComponentBoxBlockEntity::new, CyberwareBlocks.COMPONENT_BOX.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO = BLOCK_ENTITIES.register("beacon",
            () -> BlockEntityType.Builder.of(RadioBlockEntity::new, CyberwareBlocks.RADIO.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBeaconBlockEntity>> RADIO_BEACON = BLOCK_ENTITIES.register("beacon_large",
            () -> BlockEntityType.Builder.of(RadioBeaconBlockEntity::new, CyberwareBlocks.RADIO_BEACON.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioPostBlockEntity>> RADIO_POST = BLOCK_ENTITIES.register("radio_post",
            () -> BlockEntityType.Builder.of(RadioPostBlockEntity::new, CyberwareBlocks.RADIO_POST.get()).build(null));

    private CyberwareBlockEntities() {
    }
}
