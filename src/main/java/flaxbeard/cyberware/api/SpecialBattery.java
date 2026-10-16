package flaxbeard.cyberware.api;

import net.minecraft.world.item.ItemStack;

/**
 * Cyberware that stores power by itself instead of contributing to the shared capacity.
 */
public interface SpecialBattery {
    /**
     * @param battery the installed battery stack
     * @param source  the producer of the power, or {@link ItemStack#EMPTY} for external sources such as the charger
     * @return the amount actually added
     */
    int add(ItemStack battery, ItemStack source, int amount, boolean simulate);

    /**
     * @return the amount actually extracted
     */
    int extract(ItemStack battery, int amount, boolean simulate);

    int getStoredEnergy(ItemStack battery);

    int getCapacity(ItemStack battery);
}
