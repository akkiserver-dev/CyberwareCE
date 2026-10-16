package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.item.ArmUpgradeItem;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.item.BodyPartItem;
import flaxbeard.cyberware.common.item.BoneUpgradeItem;
import flaxbeard.cyberware.common.item.BrainUpgradeItem;
import flaxbeard.cyberware.common.item.ComponentBoxItem;
import flaxbeard.cyberware.common.item.CreativeBatteryItem;
import flaxbeard.cyberware.common.item.CyberheartItem;
import flaxbeard.cyberware.common.item.CybereyeUpgradeItem;
import flaxbeard.cyberware.common.item.CybereyesItem;
import flaxbeard.cyberware.common.item.CyberlimbItem;
import flaxbeard.cyberware.common.item.CyberwareArmorItem;
import flaxbeard.cyberware.common.item.CyberwareItem;
import flaxbeard.cyberware.common.item.DenseBatteryItem;
import flaxbeard.cyberware.common.item.DescribedBlockItem;
import flaxbeard.cyberware.common.item.ExperienceCapsuleItem;
import flaxbeard.cyberware.common.item.FootUpgradeItem;
import flaxbeard.cyberware.common.item.HandUpgradeItem;
import flaxbeard.cyberware.common.item.HeartUpgradeItem;
import flaxbeard.cyberware.common.item.HudlensItem;
import flaxbeard.cyberware.common.item.KatanaItem;
import flaxbeard.cyberware.common.item.LegUpgradeItem;
import flaxbeard.cyberware.common.item.LowerOrgansUpgradeItem;
import flaxbeard.cyberware.common.item.LungsUpgradeItem;
import flaxbeard.cyberware.common.item.MuscleUpgradeItem;
import flaxbeard.cyberware.common.item.NeuropozyneItem;
import flaxbeard.cyberware.common.item.SkinUpgradeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static flaxbeard.cyberware.common.CyberwareConstants.COMMON;
import static flaxbeard.cyberware.common.CyberwareConstants.RARE;
import static flaxbeard.cyberware.common.CyberwareConstants.UNCOMMON;
import static flaxbeard.cyberware.common.CyberwareConstants.VERY_COMMON;
import static flaxbeard.cyberware.common.item.CyberwareItem.settings;

/**
 * All items. Each piece of cyberware that used to be a metadata variant is its own item; registry names keep the
 * original model names (e.g. {@code cybereye_upgrades_night_vision}) and tooltips keep the original translation keys.
 */
public final class CyberwareItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberwareMod.MOD_ID);

    private static final List<DeferredItem<? extends Item>> CYBERWARE = new ArrayList<>();

    // ---- components ----

    public static final DeferredItem<Item> ACTUATOR = ITEMS.registerSimpleItem("component_actuator");
    public static final DeferredItem<Item> REACTOR = ITEMS.registerSimpleItem("component_reactor");
    public static final DeferredItem<Item> TITANIUM = ITEMS.registerSimpleItem("component_titanium");
    public static final DeferredItem<Item> SSC = ITEMS.registerSimpleItem("component_ssc");
    public static final DeferredItem<Item> PLATING = ITEMS.registerSimpleItem("component_plating");
    public static final DeferredItem<Item> FIBER_OPTICS = ITEMS.registerSimpleItem("component_fiberoptics");
    public static final DeferredItem<Item> FULLERENE = ITEMS.registerSimpleItem("component_fullerene");
    public static final DeferredItem<Item> SYNTH_NERVES = ITEMS.registerSimpleItem("component_synthnerves");
    public static final DeferredItem<Item> STORAGE = ITEMS.registerSimpleItem("component_storage");
    public static final DeferredItem<Item> MICROELECTRIC = ITEMS.registerSimpleItem("component_microelectric");

    // ---- body parts ----

    public static final DeferredItem<BodyPartItem> BODY_PART_EYES = bodyPart("eyes", 0, BodyRegion.EYES, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_BRAIN = bodyPart("brain", 1, BodyRegion.CRANIUM, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_HEART = bodyPart("heart", 2, BodyRegion.HEART, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_LUNGS = bodyPart("lungs", 3, BodyRegion.LUNGS, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_STOMACH = bodyPart("stomach", 4, BodyRegion.LOWER_ORGANS, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_SKIN = bodyPart("skin", 5, BodyRegion.SKIN, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_MUSCLES = bodyPart("muscles", 6, BodyRegion.MUSCLE, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_BONES = bodyPart("bones", 7, BodyRegion.BONE, null);
    public static final DeferredItem<BodyPartItem> BODY_PART_ARM_LEFT = bodyPart("arm_left", 8, BodyRegion.ARM, SidedLimb.Side.LEFT);
    public static final DeferredItem<BodyPartItem> BODY_PART_ARM_RIGHT = bodyPart("arm_right", 9, BodyRegion.ARM, SidedLimb.Side.RIGHT);
    public static final DeferredItem<BodyPartItem> BODY_PART_LEG_LEFT = bodyPart("leg_left", 10, BodyRegion.LEG, SidedLimb.Side.LEFT);
    public static final DeferredItem<BodyPartItem> BODY_PART_LEG_RIGHT = bodyPart("leg_right", 11, BodyRegion.LEG, SidedLimb.Side.RIGHT);

    // ---- eyes ----

    public static final DeferredItem<CybereyesItem> CYBEREYES = cyberware("cybereyes", CybereyesItem::new,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereyes").essence(8).weight(UNCOMMON));
    public static final DeferredItem<CybereyeUpgradeItem> NIGHT_VISION = cybereyeUpgrade("night_vision", CybereyeUpgradeItem.Kind.NIGHT_VISION,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereye_upgrades.0").essence(2).weight(UNCOMMON));
    public static final DeferredItem<CybereyeUpgradeItem> UNDERWATER_VISION = cybereyeUpgrade("underwater_vision", CybereyeUpgradeItem.Kind.UNDERWATER_VISION,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereye_upgrades.1").essence(2).weight(UNCOMMON));
    public static final DeferredItem<CybereyeUpgradeItem> HUDJACK = cybereyeUpgrade("hudjack", CybereyeUpgradeItem.Kind.HUDJACK,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereye_upgrades.2").essence(1).weight(UNCOMMON));
    public static final DeferredItem<CybereyeUpgradeItem> TARGETING = cybereyeUpgrade("targeting", CybereyeUpgradeItem.Kind.TARGETING,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereye_upgrades.3").essence(1).weight(UNCOMMON));
    public static final DeferredItem<CybereyeUpgradeItem> ZOOM = cybereyeUpgrade("zoom", CybereyeUpgradeItem.Kind.ZOOM,
            settings(BodyRegion.EYES, "cyberware.tooltip.cybereye_upgrades.4").essence(1).weight(UNCOMMON));
    public static final DeferredItem<HudlensItem> HUDLENS = cyberware("eye_upgrades_hudlens", HudlensItem::new,
            settings(BodyRegion.EYES, "cyberware.tooltip.eye_upgrades.0").essence(1).weight(VERY_COMMON));

    // ---- cranium ----

    public static final DeferredItem<BrainUpgradeItem> CORTICAL_STACK = brainUpgrade("cortical_stack", BrainUpgradeItem.Kind.CORTICAL_STACK,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.0").essence(3).weight(RARE));
    public static final DeferredItem<BrainUpgradeItem> ENDER_JAMMER = brainUpgrade("ender_jammer", BrainUpgradeItem.Kind.ENDER_JAMMER,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.1").essence(10).weight(UNCOMMON));
    public static final DeferredItem<BrainUpgradeItem> CONSCIOUSNESS_TRANSMITTER = brainUpgrade("consciousness_transmitter", BrainUpgradeItem.Kind.CONSCIOUSNESS_TRANSMITTER,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.2").essence(2).weight(UNCOMMON));
    public static final DeferredItem<BrainUpgradeItem> NEURAL_CONTEXTUALIZER = brainUpgrade("neural_contextualizer", BrainUpgradeItem.Kind.NEURAL_CONTEXTUALIZER,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.3").essence(2).weight(COMMON));
    public static final DeferredItem<BrainUpgradeItem> THREAT_MATRIX = brainUpgrade("matrix", BrainUpgradeItem.Kind.THREAT_MATRIX,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.4").essence(8).weight(UNCOMMON));
    public static final DeferredItem<BrainUpgradeItem> CRANIAL_BROADCASTER = brainUpgrade("radio", BrainUpgradeItem.Kind.RADIO,
            settings(BodyRegion.CRANIUM, "cyberware.tooltip.brain_upgrades.5").essence(2).weight(UNCOMMON));

    // ---- heart ----

    public static final DeferredItem<CyberheartItem> CYBERHEART = cyberware("cyberheart", CyberheartItem::new,
            settings(BodyRegion.HEART, "cyberware.tooltip.cyberheart").essence(5).weight(COMMON));
    public static final DeferredItem<HeartUpgradeItem> INTERNAL_DEFIBRILLATOR = heartUpgrade("defibrillator", HeartUpgradeItem.Kind.INTERNAL_DEFIBRILLATOR,
            settings(BodyRegion.HEART, "cyberware.tooltip.heart_upgrades.0").essence(10).weight(COMMON));
    public static final DeferredItem<HeartUpgradeItem> PLATELET_DISPATCHER = heartUpgrade("platelets", HeartUpgradeItem.Kind.PLATELET_DISPATCHER,
            settings(BodyRegion.HEART, "cyberware.tooltip.heart_upgrades.1").essence(5).weight(UNCOMMON));
    public static final DeferredItem<HeartUpgradeItem> STEM_CELL_SYNTHESIZER = heartUpgrade("medkit", HeartUpgradeItem.Kind.STEM_CELL_SYNTHESIZER,
            settings(BodyRegion.HEART, "cyberware.tooltip.heart_upgrades.2").essence(15).weight(UNCOMMON));
    public static final DeferredItem<HeartUpgradeItem> CARDIOVASCULAR_COUPLER = heartUpgrade("coupler", HeartUpgradeItem.Kind.CARDIOVASCULAR_COUPLER,
            settings(BodyRegion.HEART, "cyberware.tooltip.heart_upgrades.3").essence(10).weight(VERY_COMMON));

    // ---- lungs ----

    public static final DeferredItem<LungsUpgradeItem> COMPRESSED_OXYGEN = lungsUpgrade("oxygen", LungsUpgradeItem.Kind.COMPRESSED_OXYGEN,
            settings(BodyRegion.LUNGS, "cyberware.tooltip.lungs_upgrades.0").essence(15).weight(UNCOMMON));
    public static final DeferredItem<LungsUpgradeItem> HYPEROXYGENATION_BOOST = lungsUpgrade("hyperoxygenation", LungsUpgradeItem.Kind.HYPEROXYGENATION_BOOST,
            settings(BodyRegion.LUNGS, "cyberware.tooltip.lungs_upgrades.1").essence(2, 4, 5).weight(COMMON));

    // ---- lower organs ----

    public static final DeferredItem<LowerOrgansUpgradeItem> LIVER_FILTER = lowerOrgansUpgrade("liver_filter", LowerOrgansUpgradeItem.Kind.LIVER_FILTER,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.lower_organs_upgrades.0").essence(5).weight(UNCOMMON));
    public static final DeferredItem<LowerOrgansUpgradeItem> METABOLIC_GENERATOR = lowerOrgansUpgrade("metabolic", LowerOrgansUpgradeItem.Kind.METABOLIC_GENERATOR,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.lower_organs_upgrades.1").essence(5).weight(COMMON));
    public static final DeferredItem<LowerOrgansUpgradeItem> BATTERY = lowerOrgansUpgrade("battery", LowerOrgansUpgradeItem.Kind.BATTERY,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.lower_organs_upgrades.2").essence(5, 7, 9, 11).weight(VERY_COMMON));
    public static final DeferredItem<LowerOrgansUpgradeItem> ADRENALINE_PUMP = lowerOrgansUpgrade("adrenaline", LowerOrgansUpgradeItem.Kind.ADRENALINE_PUMP,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.lower_organs_upgrades.3").essence(5).weight(UNCOMMON));
    public static final DeferredItem<DenseBatteryItem> DENSE_BATTERY = cyberware("dense_battery", DenseBatteryItem::new,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.dense_battery").essence(15).weight(RARE));
    public static final DeferredItem<CreativeBatteryItem> CREATIVE_BATTERY = cyberware("creative_battery", CreativeBatteryItem::new,
            settings(BodyRegion.LOWER_ORGANS, "cyberware.tooltip.creative_battery").essence(0));

    // ---- skin ----

    public static final DeferredItem<SkinUpgradeItem> SOLARSKIN = skinUpgrade("solar_skin", SkinUpgradeItem.Kind.SOLARSKIN,
            settings(BodyRegion.SKIN, "cyberware.tooltip.skin_upgrades.0").essence(12).weight(VERY_COMMON));
    public static final DeferredItem<SkinUpgradeItem> SUBDERMAL_SPIKES = skinUpgrade("subdermal_spikes", SkinUpgradeItem.Kind.SUBDERMAL_SPIKES,
            settings(BodyRegion.SKIN, "cyberware.tooltip.skin_upgrades.1").essence(12).weight(UNCOMMON));
    public static final DeferredItem<SkinUpgradeItem> SYNTHETIC_SKIN = skinUpgrade("fake_skin", SkinUpgradeItem.Kind.SYNTHETIC_SKIN,
            settings(BodyRegion.SKIN, "cyberware.tooltip.skin_upgrades.2").essence(0).weight(UNCOMMON));
    public static final DeferredItem<SkinUpgradeItem> IMMUNOSUPPRESSANT = skinUpgrade("immuno", SkinUpgradeItem.Kind.IMMUNOSUPPRESSANT,
            settings(BodyRegion.SKIN, "cyberware.tooltip.skin_upgrades.3").essence(-25).weight(RARE));

    // ---- muscle ----

    public static final DeferredItem<MuscleUpgradeItem> WIRED_REFLEXES = muscleUpgrade("wired_reflexes", MuscleUpgradeItem.Kind.WIRED_REFLEXES,
            settings(BodyRegion.MUSCLE, "cyberware.tooltip.muscle_upgrades.0").essence(9, 10, 11).weight(UNCOMMON));
    public static final DeferredItem<MuscleUpgradeItem> MUSCLE_REPLACEMENTS = muscleUpgrade("muscle_replacements", MuscleUpgradeItem.Kind.MUSCLE_REPLACEMENTS,
            settings(BodyRegion.MUSCLE, "cyberware.tooltip.muscle_upgrades.1").essence(15).weight(RARE));

    // ---- bone ----

    public static final DeferredItem<BoneUpgradeItem> BONE_LACING = boneUpgrade("bonelacing", BoneUpgradeItem.Kind.LACING,
            settings(BodyRegion.BONE, "cyberware.tooltip.bone_upgrades.0").essence(3, 6, 9, 12, 15).weight(UNCOMMON));
    public static final DeferredItem<BoneUpgradeItem> BONE_FLEX = boneUpgrade("boneflex", BoneUpgradeItem.Kind.FLEX,
            settings(BodyRegion.BONE, "cyberware.tooltip.bone_upgrades.1").essence(5).weight(RARE));
    public static final DeferredItem<BoneUpgradeItem> BONE_BATTERY = boneUpgrade("bonebattery", BoneUpgradeItem.Kind.BATTERY,
            settings(BodyRegion.BONE, "cyberware.tooltip.bone_upgrades.2").essence(2, 3, 4, 5).weight(UNCOMMON));

    // ---- limbs ----

    public static final DeferredItem<CyberlimbItem> CYBERARM_LEFT = cyberlimb("cyberarm_left", 0, BodyRegion.ARM, SidedLimb.Side.LEFT);
    public static final DeferredItem<CyberlimbItem> CYBERARM_RIGHT = cyberlimb("cyberarm_right", 1, BodyRegion.ARM, SidedLimb.Side.RIGHT);
    public static final DeferredItem<CyberlimbItem> CYBERLEG_LEFT = cyberlimb("cyberleg_left", 2, BodyRegion.LEG, SidedLimb.Side.LEFT);
    public static final DeferredItem<CyberlimbItem> CYBERLEG_RIGHT = cyberlimb("cyberleg_right", 3, BodyRegion.LEG, SidedLimb.Side.RIGHT);

    public static final DeferredItem<ArmUpgradeItem> BOW_STRING_RETRACTOR = cyberware("arm_upgrades_bow", ArmUpgradeItem::new,
            settings(BodyRegion.ARM, "cyberware.tooltip.arm_upgrades.0").essence(3).weight(RARE));

    public static final DeferredItem<HandUpgradeItem> CRAFT_HANDS = handUpgrade("craft_hands", HandUpgradeItem.Kind.CRAFT_HANDS,
            settings(BodyRegion.HAND, "cyberware.tooltip.hand_upgrades.0").essence(2).weight(RARE));
    public static final DeferredItem<HandUpgradeItem> CLAWS = handUpgrade("claws", HandUpgradeItem.Kind.CLAWS,
            settings(BodyRegion.HAND, "cyberware.tooltip.hand_upgrades.1").essence(2).weight(RARE));
    public static final DeferredItem<HandUpgradeItem> MINING_FIST = handUpgrade("mining", HandUpgradeItem.Kind.MINING,
            settings(BodyRegion.HAND, "cyberware.tooltip.hand_upgrades.2").essence(1).weight(RARE));

    public static final DeferredItem<LegUpgradeItem> JUMP_BOOST = legUpgrade("jump_boost", LegUpgradeItem.Kind.JUMP_BOOST,
            settings(BodyRegion.LEG, "cyberware.tooltip.leg_upgrades.0").essence(3).weight(RARE));
    public static final DeferredItem<LegUpgradeItem> FALL_DAMAGE_UPGRADE = legUpgrade("fall_damage", LegUpgradeItem.Kind.FALL_DAMAGE,
            settings(BodyRegion.LEG, "cyberware.tooltip.leg_upgrades.1").essence(2).weight(RARE));

    public static final DeferredItem<FootUpgradeItem> SPURS = footUpgrade("spurs", FootUpgradeItem.Kind.SPURS,
            settings(BodyRegion.FOOT, "cyberware.tooltip.foot_upgrades.0").essence(1).weight(UNCOMMON));
    public static final DeferredItem<FootUpgradeItem> AQUA = footUpgrade("aqua", FootUpgradeItem.Kind.AQUA,
            settings(BodyRegion.FOOT, "cyberware.tooltip.foot_upgrades.1").essence(2).weight(RARE));
    public static final DeferredItem<FootUpgradeItem> WHEELS = footUpgrade("wheels", FootUpgradeItem.Kind.WHEELS,
            settings(BodyRegion.FOOT, "cyberware.tooltip.foot_upgrades.2").essence(3).weight(UNCOMMON));

    // ---- other items ----

    public static final DeferredItem<NeuropozyneItem> NEUROPOZYNE = ITEMS.registerItem("neuropozyne", NeuropozyneItem::new);
    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<ExperienceCapsuleItem> EXP_CAPSULE = ITEMS.registerItem("exp_capsule", ExperienceCapsuleItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<KatanaItem> KATANA = ITEMS.register("katana", () -> new KatanaItem(CyberwareTiers.KATANA,
            new Item.Properties().attributes(SwordItem.createAttributes(CyberwareTiers.KATANA, 3, -2.4F))));

    public static final DeferredItem<CyberwareArmorItem> SHADES = ITEMS.register("shades", () -> new CyberwareArmorItem(
            CyberwareArmorMaterials.SHADES, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))));
    public static final DeferredItem<CyberwareArmorItem> SHADES2 = ITEMS.register("shades2", () -> new CyberwareArmorItem(
            CyberwareArmorMaterials.SHADES2, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))));
    public static final DeferredItem<CyberwareArmorItem> JACKET = ITEMS.register("jacket", () -> new CyberwareArmorItem(
            CyberwareArmorMaterials.JACKET, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(5))));
    public static final DeferredItem<CyberwareArmorItem> TRENCH_COAT = ITEMS.register("trenchcoat", () -> new CyberwareArmorItem(
            CyberwareArmorMaterials.TRENCH_COAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()
            .durability(ArmorItem.Type.CHESTPLATE.getDurability(5))
            .component(DataComponents.DYED_COLOR, new DyedItemColor(CyberwareArmorItem.DEFAULT_TRENCH_COAT_COLOR, true))));

    // ---- block items ----

    public static final DeferredItem<DescribedBlockItem> SURGERY = blockItem("surgery", CyberwareBlocks.SURGERY,
            "cyberware.tooltip.surgery.0", "cyberware.tooltip.surgery.1");
    public static final DeferredItem<DescribedBlockItem> SURGERY_CHAMBER = blockItem("surgery_chamber", CyberwareBlocks.SURGERY_CHAMBER,
            "cyberware.tooltip.surgery_chamber.0", "cyberware.tooltip.surgery_chamber.1");
    public static final DeferredItem<DescribedBlockItem> CHARGER = blockItem("charger", CyberwareBlocks.CHARGER,
            "cyberware.tooltip.charger.0", "cyberware.tooltip.charger.1");
    public static final DeferredItem<DescribedBlockItem> ENGINEERING_TABLE = blockItem("engineering_table", CyberwareBlocks.ENGINEERING_TABLE,
            "cyberware.tooltip.engineering_table");
    public static final DeferredItem<DescribedBlockItem> SCANNER = blockItem("scanner", CyberwareBlocks.SCANNER,
            "cyberware.tooltip.scanner");
    public static final DeferredItem<DescribedBlockItem> BLUEPRINT_ARCHIVE = blockItem("blueprint_archive", CyberwareBlocks.BLUEPRINT_ARCHIVE,
            "cyberware.tooltip.blueprint_archive.0", "cyberware.tooltip.blueprint_archive.1");
    public static final DeferredItem<ComponentBoxItem> COMPONENT_BOX = ITEMS.register("component_box",
            () -> new ComponentBoxItem(CyberwareBlocks.COMPONENT_BOX.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DescribedBlockItem> RADIO = blockItem("beacon", CyberwareBlocks.RADIO,
            "cyberware.tooltip.beacon");
    public static final DeferredItem<DescribedBlockItem> RADIO_BEACON = blockItem("beacon_large", CyberwareBlocks.RADIO_BEACON,
            "cyberware.tooltip.beacon_large");
    public static final DeferredItem<DescribedBlockItem> RADIO_POST = blockItem("radio_post", CyberwareBlocks.RADIO_POST,
            "cyberware.tooltip.beacon_post.0", "cyberware.tooltip.beacon_post.1", "cyberware.tooltip.beacon_post.2");

    private CyberwareItems() {
    }

    /**
     * @return every registered cyberware item, in registration order
     */
    public static List<DeferredItem<? extends Item>> getCyberware() {
        return Collections.unmodifiableList(CYBERWARE);
    }


    private static <T extends CyberwareItem> DeferredItem<T> cyberware(String name, BiFunction<Item.Properties, CyberwareItem.Settings, T> factory, CyberwareItem.Settings settings) {
        DeferredItem<T> item = ITEMS.register(name, () -> factory.apply(new Item.Properties(), settings));
        CYBERWARE.add(item);
        return item;
    }

    private static DeferredItem<BodyPartItem> bodyPart(String name, int index, BodyRegion region, SidedLimb.Side side) {
        return cyberware("body_part_" + name, (properties, settings) -> new BodyPartItem(properties, settings, side),
                settings(region, "cyberware.tooltip.body_part." + index));
    }

    private static DeferredItem<CyberlimbItem> cyberlimb(String name, int index, BodyRegion region, SidedLimb.Side side) {
        return cyberware("cyberlimbs_" + name, (properties, settings) -> new CyberlimbItem(properties, settings, side),
                settings(region, "cyberware.tooltip.cyberlimbs." + index).essence(15));
    }

    private static DeferredItem<CybereyeUpgradeItem> cybereyeUpgrade(String name, CybereyeUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("cybereye_upgrades_" + name, (properties, s) -> new CybereyeUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<BrainUpgradeItem> brainUpgrade(String name, BrainUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("brain_upgrades_" + name, (properties, s) -> new BrainUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<HeartUpgradeItem> heartUpgrade(String name, HeartUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("heart_upgrades_" + name, (properties, s) -> new HeartUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<LungsUpgradeItem> lungsUpgrade(String name, LungsUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("lungs_upgrades_" + name, (properties, s) -> new LungsUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<LowerOrgansUpgradeItem> lowerOrgansUpgrade(String name, LowerOrgansUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("lower_organs_upgrades_" + name, (properties, s) -> new LowerOrgansUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<SkinUpgradeItem> skinUpgrade(String name, SkinUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("skin_upgrades_" + name, (properties, s) -> new SkinUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<MuscleUpgradeItem> muscleUpgrade(String name, MuscleUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("muscle_upgrades_" + name, (properties, s) -> new MuscleUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<BoneUpgradeItem> boneUpgrade(String name, BoneUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("bone_upgrades_" + name, (properties, s) -> new BoneUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<HandUpgradeItem> handUpgrade(String name, HandUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("hand_upgrades_" + name, (properties, s) -> new HandUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<LegUpgradeItem> legUpgrade(String name, LegUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("leg_upgrades_" + name, (properties, s) -> new LegUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<FootUpgradeItem> footUpgrade(String name, FootUpgradeItem.Kind kind, CyberwareItem.Settings settings) {
        return cyberware("foot_upgrades_" + name, (properties, s) -> new FootUpgradeItem(properties, s, kind), settings);
    }

    private static DeferredItem<DescribedBlockItem> blockItem(String name, Supplier<? extends Block> block, String... tooltipKeys) {
        return ITEMS.register(name, () -> new DescribedBlockItem(block.get(), new Item.Properties(), tooltipKeys));
    }
}
