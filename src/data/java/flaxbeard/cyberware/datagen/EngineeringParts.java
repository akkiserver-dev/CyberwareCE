package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The components each item is made of. The Engineering Table crafting recipes and the deconstruction loot tables
 * are generated from this table, the values are the ones of the original mod.
 */
public final class EngineeringParts {
    private EngineeringParts() {
    }

    /**
     * @return the components of every item that can be crafted and deconstructed, in registration order
     */
    public static Map<ItemLike, List<ItemStack>> get() {
        Map<ItemLike, List<ItemStack>> parts = new LinkedHashMap<>();
        add(parts, CyberwareItems.CYBEREYES, part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 2), part(CyberwareItems.SYNTH_NERVES.get(), 2));
        add(parts, CyberwareItems.NIGHT_VISION, part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 2), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.UNDERWATER_VISION, part(CyberwareItems.FIBER_OPTICS.get(), 2), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.HUDJACK, part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 2));
        add(parts, CyberwareItems.TARGETING, part(CyberwareItems.SSC.get(), 2), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.ZOOM, part(CyberwareItems.FIBER_OPTICS.get(), 2), part(CyberwareItems.SYNTH_NERVES.get(), 4));
        add(parts, CyberwareItems.HUDLENS, part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 2));
        add(parts, CyberwareItems.CORTICAL_STACK, part(CyberwareItems.REACTOR.get(), 2), part(CyberwareItems.SYNTH_NERVES.get(), 1), part(CyberwareItems.STORAGE.get(), 2));
        add(parts, CyberwareItems.ENDER_JAMMER, part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 2));
        add(parts, CyberwareItems.CONSCIOUSNESS_TRANSMITTER, part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 3));
        add(parts, CyberwareItems.NEURAL_CONTEXTUALIZER, part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 3));
        add(parts, CyberwareItems.THREAT_MATRIX, part(CyberwareItems.SSC.get(), 3), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 2));
        add(parts, CyberwareItems.CRANIAL_BROADCASTER, part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.CYBERHEART, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.INTERNAL_DEFIBRILLATOR, part(CyberwareItems.ACTUATOR.get(), 1), part(CyberwareItems.FULLERENE.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 2));
        add(parts, CyberwareItems.PLATELET_DISPATCHER, part(CyberwareItems.ACTUATOR.get(), 1), part(CyberwareItems.REACTOR.get(), 2), part(CyberwareItems.STORAGE.get(), 1));
        add(parts, CyberwareItems.STEM_CELL_SYNTHESIZER, part(CyberwareItems.REACTOR.get(), 3), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.CARDIOVASCULAR_COUPLER, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 2));
        add(parts, CyberwareItems.COMPRESSED_OXYGEN, part(CyberwareItems.REACTOR.get(), 2), part(CyberwareItems.STORAGE.get(), 2));
        add(parts, CyberwareItems.HYPEROXYGENATION_BOOST, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.STORAGE.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.LIVER_FILTER, part(CyberwareItems.REACTOR.get(), 3), part(CyberwareItems.STORAGE.get(), 2));
        add(parts, CyberwareItems.METABOLIC_GENERATOR, part(CyberwareItems.REACTOR.get(), 3), part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.BATTERY, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.STORAGE.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 3));
        add(parts, CyberwareItems.ADRENALINE_PUMP, part(CyberwareItems.ACTUATOR.get(), 1), part(CyberwareItems.REACTOR.get(), 2));
        add(parts, CyberwareItems.DENSE_BATTERY, part(CyberwareItems.FULLERENE.get(), 3), part(CyberwareItems.MICROELECTRIC.get(), 4));
        add(parts, CyberwareItems.SOLARSKIN, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.SUBDERMAL_SPIKES, part(CyberwareItems.ACTUATOR.get(), 1), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.SYNTHETIC_SKIN, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.PLATING.get(), 3), part(CyberwareItems.FIBER_OPTICS.get(), 2));
        add(parts, CyberwareItems.IMMUNOSUPPRESSANT, part(CyberwareItems.REACTOR.get(), 3), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1), part(CyberwareItems.STORAGE.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.WIRED_REFLEXES, part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 3));
        add(parts, CyberwareItems.MUSCLE_REPLACEMENTS, part(CyberwareItems.ACTUATOR.get(), 3), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 2));
        add(parts, CyberwareItems.BONE_LACING, part(CyberwareItems.REACTOR.get(), 1), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.FULLERENE.get(), 2));
        add(parts, CyberwareItems.BONE_FLEX, part(CyberwareItems.REACTOR.get(), 3), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.STORAGE.get(), 2));
        add(parts, CyberwareItems.BONE_BATTERY, part(CyberwareItems.REACTOR.get(), 2), part(CyberwareItems.STORAGE.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.CYBERARM_LEFT, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 2), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.CYBERARM_RIGHT, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 2), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.CYBERLEG_LEFT, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 2), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.CYBERLEG_RIGHT, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 2), part(CyberwareItems.FIBER_OPTICS.get(), 1), part(CyberwareItems.SYNTH_NERVES.get(), 1));
        add(parts, CyberwareItems.BOW_STRING_RETRACTOR, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.PLATING.get(), 2));
        add(parts, CyberwareItems.CRAFT_HANDS, part(CyberwareItems.ACTUATOR.get(), 4), part(CyberwareItems.SSC.get(), 1), part(CyberwareItems.PLATING.get(), 1));
        add(parts, CyberwareItems.CLAWS, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.TITANIUM.get(), 2), part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FULLERENE.get(), 1), part(CyberwareItems.STORAGE.get(), 2));
        add(parts, CyberwareItems.MINING_FIST, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FULLERENE.get(), 2));
        add(parts, CyberwareItems.JUMP_BOOST, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.TITANIUM.get(), 2));
        add(parts, CyberwareItems.FALL_DAMAGE_UPGRADE, part(CyberwareItems.TITANIUM.get(), 3), part(CyberwareItems.PLATING.get(), 1), part(CyberwareItems.FIBER_OPTICS.get(), 1));
        add(parts, CyberwareItems.SPURS, part(CyberwareItems.ACTUATOR.get(), 1), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.PLATING.get(), 1));
        add(parts, CyberwareItems.AQUA, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.MICROELECTRIC.get(), 1));
        add(parts, CyberwareItems.WHEELS, part(CyberwareItems.ACTUATOR.get(), 2), part(CyberwareItems.MICROELECTRIC.get(), 2));
        add(parts, CyberwareItems.KATANA, part(Items.IRON_INGOT, 2), part(CyberwareItems.TITANIUM.get(), 1), part(CyberwareItems.PLATING.get(), 1));
        add(parts, CyberwareItems.SHADES, part(Blocks.BLACK_STAINED_GLASS, 4), part(CyberwareItems.PLATING.get(), 1));
        add(parts, CyberwareItems.SHADES2, part(Blocks.BLACK_STAINED_GLASS, 4), part(CyberwareItems.PLATING.get(), 1));
        add(parts, CyberwareItems.JACKET, part(CyberwareItems.TITANIUM.get(), 1), part(Items.LEATHER, 8), part(Items.BLACK_DYE, 1));
        add(parts, CyberwareItems.TRENCH_COAT, part(CyberwareItems.TITANIUM.get(), 2), part(Items.LEATHER, 12), part(Items.BLACK_DYE, 1));
        return parts;
    }

    private static void add(Map<ItemLike, List<ItemStack>> parts, ItemLike item, ItemStack... components) {
        parts.put(item, List.of(components));
    }

    private static ItemStack part(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
