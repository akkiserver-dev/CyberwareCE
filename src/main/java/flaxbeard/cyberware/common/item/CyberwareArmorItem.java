package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.item.Deconstructable;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cyberpunk clothing: mirror shades, biker jacket and the dyeable trench coat.
 */
public class CyberwareArmorItem extends ArmorItem implements Deconstructable {
    /** Color of an undyed trench coat. */
    public static final int DEFAULT_TRENCH_COAT_COLOR = 0x333333;

    private final Supplier<List<ItemStack>> components;

    public CyberwareArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<List<ItemStack>> components) {
        super(material, type, properties);
        this.components = components;
    }

    @Override
    public boolean canDestroy(ItemStack stack) {
        return true;
    }

    @Override
    public List<ItemStack> getComponents(ItemStack stack) {
        return components.get();
    }
}
