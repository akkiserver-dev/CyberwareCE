package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.item.Deconstructable;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

import java.util.List;

public class KatanaItem extends SwordItem implements Deconstructable {
    public KatanaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean canDestroy(ItemStack stack) {
        return true;
    }

    @Override
    public List<ItemStack> getComponents(ItemStack stack) {
        return List.of(
                new ItemStack(Items.IRON_INGOT, 2),
                new ItemStack(CyberwareItems.TITANIUM.get(), 1),
                new ItemStack(CyberwareItems.PLATING.get(), 1));
    }
}
