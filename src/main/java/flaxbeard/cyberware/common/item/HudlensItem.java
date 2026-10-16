package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.Hudjack;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * A HUD for people without cybereyes.
 */
public class HudlensItem extends CyberwareItem implements MenuItem, Hudjack {
    public HudlensItem(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return other.is(CyberwareItems.CYBEREYES.get());
    }

    @Override
    public boolean hasMenu(ItemStack stack) {
        return true;
    }

    @Override
    public void use(Entity entity, ItemStack stack) {
        EnableDisableHelper.toggle(stack);
    }

    @Override
    public String getMenuLabel(ItemStack stack) {
        return EnableDisableHelper.getMenuLabel(stack);
    }

    @Override
    public float[] getColor(ItemStack stack) {
        return EnableDisableHelper.getMenuColor(stack);
    }

    @Override
    public boolean isActive(ItemStack stack) {
        return EnableDisableHelper.isEnabled(stack);
    }
}
