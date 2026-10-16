package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.SpecialBattery;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.item.ItemStack;

/**
 * A large battery that can only be charged externally (by the Cyberware Charger).
 */
public class DenseBatteryItem extends CyberwareItem implements SpecialBattery {
    private static final String POWER_KEY = "power";

    public DenseBatteryItem(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return other.is(CyberwareItems.BATTERY.get());
    }

    @Override
    public int add(ItemStack battery, ItemStack source, int amount, boolean simulate) {
        if (!source.isEmpty()) return 0;
        int toAdd = Math.min(getCapacity(battery) - getStoredEnergy(battery), amount);
        if (!simulate) {
            int stored = getStoredEnergy(battery);
            CyberwareAPI.updateFunctionData(battery, tag -> tag.putInt(POWER_KEY, stored + toAdd));
        }
        return toAdd;
    }

    @Override
    public int extract(ItemStack battery, int amount, boolean simulate) {
        int stored = getStoredEnergy(battery);
        int toExtract = Math.min(stored, amount);
        if (!simulate) {
            CyberwareAPI.updateFunctionData(battery, tag -> tag.putInt(POWER_KEY, stored - toExtract));
        }
        return toExtract;
    }

    @Override
    public int getStoredEnergy(ItemStack battery) {
        return CyberwareAPI.getFunctionData(battery).getInt(POWER_KEY);
    }

    @Override
    public int getCapacity(ItemStack battery) {
        return CyberwareConstants.DENSE_BATTERY_CAPACITY;
    }
}
