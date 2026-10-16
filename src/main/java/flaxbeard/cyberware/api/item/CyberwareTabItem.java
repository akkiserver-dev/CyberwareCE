package flaxbeard.cyberware.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Items that are sorted into categories in the Cyberware creative tab.
 */
public interface CyberwareTabItem {
    enum Category {
        BLOCKS,
        BODY_PARTS,
        EYES,
        CRANIUM,
        HEART,
        LUNGS,
        LOWER_ORGANS,
        SKIN,
        MUSCLE,
        BONE,
        ARM,
        HAND,
        LEG,
        FOOT;

        public static Category of(BodyRegion region) {
            return values()[region.ordinal() + 2];
        }
    }

    Category getCategory(ItemStack stack);
}
