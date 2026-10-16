package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.SpecialBattery;
import flaxbeard.cyberware.api.item.Quality;
import net.minecraft.world.item.ItemStack;

/**
 * An infinite power source, installed into every CyberZombie.
 */
public class CreativeBatteryItem extends CyberwareItem implements SpecialBattery {
    private static final int INFINITE = 999999;

    public CreativeBatteryItem(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    public int add(ItemStack battery, ItemStack source, int amount, boolean simulate) {
        return amount;
    }

    @Override
    public int extract(ItemStack battery, int amount, boolean simulate) {
        return amount;
    }

    @Override
    public int getStoredEnergy(ItemStack battery) {
        return INFINITE;
    }

    @Override
    public int getCapacity(ItemStack battery) {
        return INFINITE;
    }

    @Override
    public boolean canHoldQuality(ItemStack stack, Quality quality) {
        return quality == CyberwareAPI.QUALITY_MANUFACTURED;
    }
}
