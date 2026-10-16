package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.block.BlueprintArchiveBlock;
import flaxbeard.cyberware.common.block.ChargerBlock;
import flaxbeard.cyberware.common.block.ComponentBoxBlock;
import flaxbeard.cyberware.common.block.EngineeringTableBlock;
import flaxbeard.cyberware.common.block.RadioBeaconBlock;
import flaxbeard.cyberware.common.block.RadioBlock;
import flaxbeard.cyberware.common.block.RadioPostBlock;
import flaxbeard.cyberware.common.block.ScannerBlock;
import flaxbeard.cyberware.common.block.SurgeryBlock;
import flaxbeard.cyberware.common.block.SurgeryChamberBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CyberwareMod.MOD_ID);

    public static final DeferredBlock<SurgeryBlock> SURGERY = BLOCKS.register("surgery", () -> new SurgeryBlock(machine()));
    public static final DeferredBlock<SurgeryChamberBlock> SURGERY_CHAMBER = BLOCKS.register("surgery_chamber",
            () -> new SurgeryChamberBlock(machine().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<ChargerBlock> CHARGER = BLOCKS.register("charger", () -> new ChargerBlock(machine()));
    public static final DeferredBlock<EngineeringTableBlock> ENGINEERING_TABLE = BLOCKS.register("engineering_table",
            () -> new EngineeringTableBlock(machine().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<ScannerBlock> SCANNER = BLOCKS.register("scanner", () -> new ScannerBlock(machine().noOcclusion()));
    public static final DeferredBlock<BlueprintArchiveBlock> BLUEPRINT_ARCHIVE = BLOCKS.register("blueprint_archive",
            () -> new BlueprintArchiveBlock(machine()));
    public static final DeferredBlock<ComponentBoxBlock> COMPONENT_BOX = BLOCKS.register("component_box",
            () -> new ComponentBoxBlock(machine().noOcclusion()));
    public static final DeferredBlock<RadioBlock> RADIO = BLOCKS.register("beacon", () -> new RadioBlock(machine().noOcclusion()));
    public static final DeferredBlock<RadioBeaconBlock> RADIO_BEACON = BLOCKS.register("beacon_large",
            () -> new RadioBeaconBlock(machine().noOcclusion()));
    public static final DeferredBlock<RadioPostBlock> RADIO_POST = BLOCKS.register("radio_post",
            () -> new RadioPostBlock(machine().noOcclusion()));

    private CyberwareBlocks() {
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 10.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
