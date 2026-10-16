package flaxbeard.cyberware.common;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.BodyRegion;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Common (gameplay) configuration. Values are read through the static {@link ModConfigSpec.ConfigValue} fields.
 */
public final class CyberwareConfig {
    public static final ModConfigSpec SPEC;

    // Essence
    public static final ModConfigSpec.IntValue ESSENCE;
    public static final ModConfigSpec.IntValue CRITICAL_ESSENCE;

    // Machines
    public static final ModConfigSpec.DoubleValue ENGINEERING_CHANCE;
    public static final ModConfigSpec.DoubleValue SCANNER_CHANCE;
    public static final ModConfigSpec.DoubleValue SCANNER_CHANCE_ADDL;
    public static final ModConfigSpec.IntValue SCANNER_TIME;

    // Mobs
    public static final ModConfigSpec.BooleanValue MOBS_ENABLE_CYBER_ZOMBIES;
    public static final ModConfigSpec.IntValue MOBS_CYBER_ZOMBIE_WEIGHT;
    public static final ModConfigSpec.IntValue MOBS_CYBER_ZOMBIE_MIN_PACK;
    public static final ModConfigSpec.IntValue MOBS_CYBER_ZOMBIE_MAX_PACK;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOBS_DIMENSIONS;
    public static final ModConfigSpec.BooleanValue MOBS_IS_DIMENSION_BLACKLIST;
    public static final ModConfigSpec.BooleanValue MOBS_APPLY_DIMENSION_TO_SPAWNING;
    public static final ModConfigSpec.BooleanValue MOBS_APPLY_DIMENSION_TO_BEACON;
    public static final ModConfigSpec.BooleanValue MOBS_ADD_CLOTHES;
    public static final ModConfigSpec.DoubleValue MOBS_CYBER_ZOMBIE_DROP_RARITY;
    public static final ModConfigSpec.DoubleValue MOBS_CLOTH_DROP_RARITY;

    // Gamerules
    public static final ModConfigSpec.BooleanValue DEFAULT_DROP;
    public static final ModConfigSpec.BooleanValue DEFAULT_KEEP;
    public static final ModConfigSpec.DoubleValue DROP_CHANCE;

    // Other
    public static final ModConfigSpec.BooleanValue SURGERY_CRAFTING;
    public static final ModConfigSpec.IntValue FE_PER_POWER;
    public static final ModConfigSpec.BooleanValue ENABLE_KATANA;
    public static final ModConfigSpec.BooleanValue ENABLE_CLOTHES;
    public static final ModConfigSpec.ConfigValue<String> FIST_MINING_TOOL_NAME;

    // Defaults
    private static final Map<BodyRegion, ModConfigSpec.ConfigValue<List<? extends String>>> STARTING_ITEMS = new EnumMap<>(BodyRegion.class);

    private static Map<BodyRegion, NonNullList<ItemStack>> startingStacks;
    private static Item fistMiningTool;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("essence");
        ESSENCE = builder.comment("Maximum Essence").defineInRange("maxEssence", 100, 0, Integer.MAX_VALUE);
        CRITICAL_ESSENCE = builder.comment("Critical Essence value, where rejection begins").defineInRange("criticalEssence", 25, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("machines");
        ENGINEERING_CHANCE = builder.comment("Chance of blueprint from Engineering Table").defineInRange("engineeringChance", 15.0, 0.0, 100.0);
        SCANNER_CHANCE = builder.comment("Chance of blueprint from Scanner").defineInRange("scannerChance", 10.0, 0.0, 50.0);
        SCANNER_CHANCE_ADDL = builder.comment("Additive chance for Scanner per extra item").defineInRange("scannerChanceAdditional", 10.0, 0.0, 100.0);
        SCANNER_TIME = builder.comment("Ticks taken per Scanner operation", "24000 is one Minecraft day, 1200 is one real-life minute").defineInRange("scannerTime", 12000, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("mobs");
        MOBS_ENABLE_CYBER_ZOMBIES = builder.comment("CyberZombies are enabled").define("cyberZombiesEnabled", true);
        MOBS_CYBER_ZOMBIE_WEIGHT = builder.comment("CyberZombies spawning weight", "Vanilla Zombie = 100, Enderman = 10, Witch = 5").defineInRange("cyberZombieWeight", 15, 0, Integer.MAX_VALUE);
        MOBS_CYBER_ZOMBIE_MIN_PACK = builder.comment("CyberZombies minimum pack size", "Vanilla Zombie = 4, Enderman = 1, Witch = 1").defineInRange("cyberZombieMinPack", 1, 0, Integer.MAX_VALUE);
        MOBS_CYBER_ZOMBIE_MAX_PACK = builder.comment("CyberZombies maximum pack size", "Vanilla Zombie = 4, Enderman = 4, Witch = 1").defineInRange("cyberZombieMaxPack", 1, 0, Integer.MAX_VALUE);
        MOBS_DIMENSIONS = builder.comment("Dimension ids (e.g. minecraft:the_nether)").defineListAllowEmpty("dimensions", List.of(), () -> "minecraft:overworld", CyberwareConfig::isResourceLocation);
        MOBS_IS_DIMENSION_BLACKLIST = builder.comment("Dimension ids is a blacklist?").define("dimensionsIsBlacklist", true);
        MOBS_APPLY_DIMENSION_TO_SPAWNING = builder.comment("Dimension ids applies to natural spawning?").define("dimensionsApplyToSpawning", true);
        MOBS_APPLY_DIMENSION_TO_BEACON = builder.comment("Dimension ids applies to beacon, radio & cranial broadcaster?").define("dimensionsApplyToBeacon", true);
        MOBS_ADD_CLOTHES = builder.comment("Add Cyberware clothing to mobs").define("addClothes", true);
        MOBS_CYBER_ZOMBIE_DROP_RARITY = builder.comment("Percent chance a CyberZombie drops a cyberware").defineInRange("cyberZombieDropRarity", 50.0, 0.0, 100.0);
        MOBS_CLOTH_DROP_RARITY = builder.comment("Percent chance a Cyberware clothing is dropped").defineInRange("clothDropRarity", 50.0, 0.0, 100.0);
        builder.pop();

        builder.push("gamerules");
        DEFAULT_DROP = builder.comment("Default for gamerule cyberware_dropCyberware",
                "Determines if players drop their Cyberware on death. Does not change settings on existing worlds, use /gamerule for that. Overridden if cyberware_keepCyberware is true")
                .define("defaultDrop", false);
        DEFAULT_KEEP = builder.comment("Default for gamerule cyberware_keepCyberware",
                "Determines if players keep their Cyberware between lives. Does not change settings on existing worlds, use /gamerule for that.")
                .define("defaultKeep", false);
        DROP_CHANCE = builder.comment("Chance of successful drop", "If dropCyberware enabled, chance for a piece of Cyberware to successfully drop instead of being destroyed.")
                .defineInRange("dropChance", 100.0, 0.0, 100.0);
        builder.pop();

        builder.push("other");
        SURGERY_CRAFTING = builder.comment("Enable crafting recipe for Robosurgeon", "Normally only found in Nether fortresses").define("surgeryCrafting", false);
        FE_PER_POWER = builder.comment("FE per internal power unit consumed by the Cyberware Charger").defineInRange("fePerPower", 1, 0, Integer.MAX_VALUE);
        ENABLE_KATANA = builder.comment("Enable Katana").define("enableKatana", true);
        ENABLE_CLOTHES = builder.comment("Enable Trench Coat, Mirror Shades, and Biker Jacket").define("enableClothes", true);
        FIST_MINING_TOOL_NAME = builder.comment("Registry name of the mining tool equivalent to the reinforced fist").define("fistMiningTool", "minecraft:iron_pickaxe");
        builder.pop();

        builder.comment("Cyberware installed in new bodies. Use format 'id' or 'id amount'").push("defaults");
        for (BodyRegion region : BodyRegion.values()) {
            STARTING_ITEMS.put(region, builder.defineListAllowEmpty(region.getName(), defaultStartingItems(region), () -> "cyberware:body_part_eyes", o -> o instanceof String));
        }
        builder.pop();

        SPEC = builder.build();
    }

    @Nullable
    private static Synced fromServer;

    private CyberwareConfig() {
    }

    /**
     * Values the client needs to show the same numbers the server uses. The server sends them to its clients,
     * so a client never uses its own config for them.
     */
    public record Synced(int essence, int criticalEssence, float engineeringChance, float scannerChance,
                         float scannerChanceAdditional, int scannerTime) {
        public static final StreamCodec<ByteBuf, Synced> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Synced::essence,
                ByteBufCodecs.VAR_INT, Synced::criticalEssence,
                ByteBufCodecs.FLOAT, Synced::engineeringChance,
                ByteBufCodecs.FLOAT, Synced::scannerChance,
                ByteBufCodecs.FLOAT, Synced::scannerChanceAdditional,
                ByteBufCodecs.VAR_INT, Synced::scannerTime,
                Synced::new);

        public static Synced fromConfig() {
            return new Synced(ESSENCE.get(), CRITICAL_ESSENCE.get(), ENGINEERING_CHANCE.get().floatValue(),
                    SCANNER_CHANCE.get().floatValue(), SCANNER_CHANCE_ADDL.get().floatValue(), SCANNER_TIME.get());
        }
    }

    /**
     * @return the synced values for the given side: the own config on the server, the server's values on the client
     */
    public static Synced get(@Nullable Level level) {
        Synced synced = fromServer;
        return level != null && level.isClientSide() && synced != null ? synced : Synced.fromConfig();
    }

    /**
     * Sets the values received from the server, or clears them with {@code null} when leaving it.
     */
    public static void setFromServer(@Nullable Synced synced) {
        fromServer = synced;
    }

    private static boolean isResourceLocation(Object object) {
        return object instanceof String string && ResourceLocation.tryParse(string) != null;
    }

    private static List<String> defaultStartingItems(BodyRegion region) {
        return switch (region) {
            case EYES -> List.of("cyberware:body_part_eyes 1");
            case CRANIUM -> List.of("cyberware:body_part_brain 1");
            case HEART -> List.of("cyberware:body_part_heart 1");
            case LUNGS -> List.of("cyberware:body_part_lungs 1");
            case LOWER_ORGANS -> List.of("cyberware:body_part_stomach 1");
            case SKIN -> List.of("cyberware:body_part_skin 1");
            case MUSCLE -> List.of("cyberware:body_part_muscles 1");
            case BONE -> List.of("cyberware:body_part_bones 1");
            case ARM -> List.of("cyberware:body_part_arm_left 1", "cyberware:body_part_arm_right 1");
            case LEG -> List.of("cyberware:body_part_leg_left 1", "cyberware:body_part_leg_right 1");
            case HAND, FOOT -> List.of();
        };
    }

    /**
     * Called when the config is (re)loaded to clear values derived from it.
     */
    public static void invalidateCaches() {
        startingStacks = null;
        fistMiningTool = null;
    }

    /**
     * @return the configured starting cyberware of a region, padded to {@link CyberwareConstants#WARE_PER_SLOT}.
     * Callers must copy the stacks before modifying them.
     */
    public static NonNullList<ItemStack> getStartingItems(BodyRegion region) {
        if (startingStacks == null) {
            startingStacks = parseStartingItems();
        }
        return startingStacks.get(region);
    }

    private static Map<BodyRegion, NonNullList<ItemStack>> parseStartingItems() {
        Map<BodyRegion, NonNullList<ItemStack>> result = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            NonNullList<ItemStack> stacks = NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY);
            List<ItemStack> parsed = new ArrayList<>();
            for (String entry : STARTING_ITEMS.get(region).get()) {
                ItemStack stack = parseStartingItem(region, entry);
                if (!stack.isEmpty()) {
                    parsed.add(stack);
                }
            }
            if (parsed.size() > CyberwareConstants.WARE_PER_SLOT) {
                CyberwareMod.LOGGER.error("Cyberware configuration error! Too many items for slot {}", region.getName());
            }
            for (int index = 0; index < Math.min(parsed.size(), stacks.size()); index++) {
                stacks.set(index, parsed.get(index));
            }
            result.put(region, stacks);
        }
        return result;
    }

    private static ItemStack parseStartingItem(BodyRegion region, String entry) {
        String[] params = entry.trim().split("\\s+");
        if (params.length == 0 || params.length > 2) {
            CyberwareMod.LOGGER.error("Cyberware configuration error! Item '{}' for {} slot has an invalid format", entry, region.getName());
            return ItemStack.EMPTY;
        }
        int count = 1;
        if (params.length == 2) {
            try {
                count = Integer.parseInt(params[1]);
            } catch (NumberFormatException exception) {
                CyberwareMod.LOGGER.error("Cyberware configuration error! Item '{}' for {} slot has an invalid quantity", entry, region.getName());
                return ItemStack.EMPTY;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(params[0]);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            CyberwareMod.LOGGER.error("Cyberware configuration error! Item '{}' for {} slot doesn't exist", entry, region.getName());
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        if (!CyberwareAPI.isCyberware(stack)) {
            CyberwareMod.LOGGER.error("Cyberware configuration error! {} is not a valid piece of cyberware", id);
            return ItemStack.EMPTY;
        }
        if (CyberwareAPI.getCyberware(stack).getBodyRegion(stack) != region) {
            CyberwareMod.LOGGER.error("Cyberware configuration error! {} will not fit in slot {}", id, region.getName());
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public static Item getFistMiningTool() {
        if (fistMiningTool == null) {
            ResourceLocation id = ResourceLocation.tryParse(FIST_MINING_TOOL_NAME.get());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                fistMiningTool = BuiltInRegistries.ITEM.get(id);
            } else {
                CyberwareMod.LOGGER.error("Unable to find item with id {}, check your configuration. Defaulting fist mining tool to Iron pickaxe.", FIST_MINING_TOOL_NAME.get());
                fistMiningTool = Items.IRON_PICKAXE;
            }
        }
        return fistMiningTool;
    }

    public static boolean isValidDimension(ResourceLocation dimension) {
        boolean isListed = MOBS_DIMENSIONS.get().contains(dimension.toString());
        return MOBS_IS_DIMENSION_BLACKLIST.get() != isListed;
    }
}
