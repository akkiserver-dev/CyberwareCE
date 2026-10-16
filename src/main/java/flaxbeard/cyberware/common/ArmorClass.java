package flaxbeard.cyberware.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * How heavy the armor of an entity is. Some cyberware (dodging, spikes) doesn't work with heavy armor.
 */
public enum ArmorClass {
    NONE,
    LIGHT,
    HEAVY;

    private static final int MAX_TOTAL_ARMOR = 10;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    /** Maximum armor of a light piece, indexed like {@link #ARMOR_SLOTS}. */
    private static final double[] MAX_PART_ARMOR = {1.5D, 3.0D, 4.0D, 1.5D};

    public static boolean isWearingLightOrNone(LivingEntity entity) {
        return get(entity) != HEAVY;
    }

    public static ArmorClass get(LivingEntity entity) {
        // fast check for heavy armor
        if (entity.getArmorValue() > MAX_TOTAL_ARMOR) {
            return HEAVY;
        }

        // slower check per armor piece
        boolean hasNoArmor = true;
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            EquipmentSlot slot = ARMOR_SLOTS[index];
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            hasNoArmor = false;
            double maxPartArmor = MAX_PART_ARMOR[index];

            // caps on vanilla armor materials, judged by their chestplate
            if (stack.getItem() instanceof ArmorItem armorItem
                    && armorItem.getMaterial().value().getDefense(ArmorItem.Type.CHESTPLATE) > MAX_PART_ARMOR[2]) {
                return HEAVY;
            }

            // caps on attributes; modded armor can change attributes without being an ArmorItem
            double[] armor = {0};
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (attribute.is(Attributes.ARMOR)) {
                    armor[0] = Math.max(armor[0], modifier.amount());
                }
            });
            if (armor[0] > maxPartArmor) {
                return HEAVY;
            }
        }
        return hasNoArmor ? NONE : LIGHT;
    }
}
