package flaxbeard.cyberware.common.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Cyberpunk clothing: mirror shades, biker jacket and the dyeable trench coat.
 */
public class CyberwareArmorItem extends ArmorItem {
    /** Color of an undyed trench coat. */
    public static final int DEFAULT_TRENCH_COAT_COLOR = 0x333333;

    public CyberwareArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
